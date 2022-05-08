/**
 * Efficient cache or osmnodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.codec.WaypointMatcher;
import cgeo.geocaching.brouter.expressions.BExpressionContextWay;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.Log;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.IOUtils;

public final class NodesCache implements Closeable {
    public OsmNodesMap nodesMap;
    public WaypointMatcher waypointMatcher;
    public boolean firstFileAccessFailed = false;
    public String firstFileAccessName;
    private final BExpressionContextWay expCtxWay;
    private final int lookupVersion;
    private String currentFileName;
    private final HashMap<String, PhysicalFile> fileCache;
    private final DataBuffers dataBuffers;
    private final OsmFile[][] fileRows;
    private long cacheSum = 0;
    private long maxmemtiles;
    private final boolean detailed;

    private boolean garbageCollectionEnabled = false;
    private boolean ghostCleaningDone = false;


    private long cacheSumClean = 0;
    private long ghostSum = 0;
    private long ghostWakeup = 0;

    private final boolean directWeaving = !Boolean.getBoolean("disableDirectWeaving");

    private static final Hashtable<String, FileInformationCacheEntry> folderInfoCache = new Hashtable<>();

    private static class FileInformationCacheEntry {
        public ContentStorage.FileInformation fi;
        public long fiTimestamp;

        FileInformationCacheEntry(final ContentStorage.FileInformation fi) {
            this.fi = fi;
            this.fiTimestamp = System.currentTimeMillis();
        }
    }

    public NodesCache(final BExpressionContextWay ctxWay, final long maxmem, final NodesCache oldCache, final boolean detailed) {
        this.maxmemtiles = maxmem / 8;
        this.nodesMap = new OsmNodesMap();
        this.nodesMap.maxmem = (2L * maxmem) / 3L;
        this.expCtxWay = ctxWay;
        this.lookupVersion = ctxWay.meta.lookupVersion;
        this.detailed = detailed;

        if (ctxWay != null) {
            ctxWay.setDecodeForbidden(detailed);
        }

        firstFileAccessFailed = false;
        firstFileAccessName = null;

        if (oldCache != null) {
            fileCache = oldCache.fileCache;
            dataBuffers = oldCache.dataBuffers;

            // re-use old, virgin caches (if same detail-mode)
            if (oldCache.detailed == detailed) {
                fileRows = oldCache.fileRows;
                for (OsmFile[] fileRow : fileRows) {
                    if (fileRow == null) {
                        continue;
                    }
                    for (OsmFile osmf : fileRow) {
                        cacheSum += osmf.setGhostState();
                    }
                }
            } else {
                fileRows = new OsmFile[180][];
            }
        } else {
            fileCache = new HashMap<>(4);
            fileRows = new OsmFile[180][];
            dataBuffers = new DataBuffers();
        }
        ghostSum = cacheSum;
    }

    public String formatStatus() {
        return "collecting=" + garbageCollectionEnabled + " noGhosts=" + ghostCleaningDone + " cacheSum=" + cacheSum + " cacheSumClean=" + cacheSumClean + " ghostSum=" + ghostSum + " ghostWakeup=" + ghostWakeup;
    }

    public void clean(final boolean all) {
        for (OsmFile[] fileRow : fileRows) {
            if (fileRow == null) {
                continue;
            }
            for (OsmFile osmf : fileRow) {
                osmf.clean(all);
            }
        }
    }

    // if the cache sum exceeded a threshold,
    // clean all ghosts and enable garbage collection
    private void checkEnableCacheCleaning() {
        if (cacheSum < maxmemtiles) {
            return;
        }

        for (int i = 0; i < fileRows.length; i++) {
            final OsmFile[] fileRow = fileRows[i];
            if (fileRow == null) {
                continue;
            }
            for (OsmFile osmf : fileRow) {
                if (garbageCollectionEnabled && !ghostCleaningDone) {
                    cacheSum -= osmf.cleanGhosts();
                } else {
                    cacheSum -= osmf.collectAll();
                }
            }
        }

        if (garbageCollectionEnabled) {
            ghostCleaningDone = true;
            maxmemtiles *= 2;
        } else {
            cacheSumClean = cacheSum;
            garbageCollectionEnabled = true;
        }
    }

    public int loadSegmentFor(final int ilon, final int ilat) {
        final MicroCache mc = getSegmentFor(ilon, ilat);
        return mc == null ? 0 : mc.getSize();
    }

    public MicroCache getSegmentFor(final int ilon, final int ilat) {
        try {
            final int lonDegree = ilon / 1000000;
            final int latDegree = ilat / 1000000;
            OsmFile osmf = null;
            final OsmFile[] fileRow = fileRows[latDegree];
            final int ndegrees = fileRow == null ? 0 : fileRow.length;
            for (int i = 0; i < ndegrees; i++) {
                if (fileRow[i].lonDegree == lonDegree) {
                    osmf = fileRow[i];
                    break;
                }
            }
            if (osmf == null) {
                osmf = fileForSegment(lonDegree, latDegree);
                final OsmFile[] newFileRow = new OsmFile[ndegrees + 1];
                for (int i = 0; i < ndegrees; i++) {
                    newFileRow[i] = fileRow[i];
                }
                newFileRow[ndegrees] = osmf;
                fileRows[latDegree] = newFileRow;
            }
            currentFileName = osmf.filename;

            if (!osmf.hasData()) {
                return null;
            }

            MicroCache segment = osmf.getMicroCache(ilon, ilat);
            if (segment == null) {
                checkEnableCacheCleaning();
                segment = osmf.createMicroCache(ilon, ilat, dataBuffers, expCtxWay, waypointMatcher, directWeaving ? nodesMap : null);

                cacheSum += segment.getDataSize();
            } else if (segment.ghost) {
                segment.unGhost();
                ghostWakeup += segment.getDataSize();
            }
            return segment;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("error reading datafile " + currentFileName + ": " + e, e);
        }
    }

    /**
     * make sure the given node is non-hollow,
     * which means it contains not just the id,
     * but also the actual data
     *
     * @return true if successfull, false if node is still hollow
     */
    public boolean obtainNonHollowNode(final OsmNode node) {
        if (!node.isHollow()) {
            return true;
        }

        final MicroCache segment = getSegmentFor(node.ilon, node.ilat);
        if (segment == null) {
            return false;
        }
        if (!node.isHollow()) {
            return true; // direct weaving...
        }

        final long id = node.getIdFromPos();
        if (segment.getAndClear(id)) {
            node.parseNodeBody(segment, nodesMap, expCtxWay);
        }

        if (garbageCollectionEnabled) { // garbage collection
            cacheSum -= segment.collect(segment.getSize() >> 1); // threshold = 1/2 of size is deleted
        }

        return !node.isHollow();
    }


    /**
     * make sure all link targets of the given node are non-hollow
     */
    public void expandHollowLinkTargets(final OsmNode n) {
        for (OsmLink link = n.firstlink; link != null; link = link.getNext(n)) {
            obtainNonHollowNode(link.getTarget(n));
        }
    }

    /**
     * make sure all link targets of the given node are non-hollow
     */
    public boolean hasHollowLinkTargets(final OsmNode n) {
        for (OsmLink link = n.firstlink; link != null; link = link.getNext(n)) {
            if (link.getTarget(n).isHollow()) {
                return true;
            }
        }
        return false;
    }

    /**
     * get a node for the given id with all link-targets also non-hollow
     * <p>
     * It is required that an instance of the start-node does not yet
     * exist, not even a hollow instance, so getStartNode should only
     * be called once right after resetting the cache
     *
     * @param id the id of the node to load
     * @return the fully expanded node for id, or null if it was not found
     */
    public OsmNode getStartNode(final long id) {
        // initialize the start-node
        final OsmNode n = new OsmNode(id);
        n.setHollow();
        nodesMap.put(n);
        if (!obtainNonHollowNode(n)) {
            return null;
        }
        expandHollowLinkTargets(n);
        return n;
    }

    public OsmNode getGraphNode(final OsmNode template) {
        final OsmNode graphNode = new OsmNode(template.ilon, template.ilat);
        graphNode.setHollow();
        final OsmNode existing = nodesMap.put(graphNode);
        if (existing == null) {
            return graphNode;
        }
        nodesMap.put(existing);
        return existing;
    }

    public void matchWaypointsToNodes(final List<MatchedWaypoint> unmatchedWaypoints, final double maxDistance, final OsmNodePairSet islandNodePairs) {
        waypointMatcher = new WaypointMatcherImpl(unmatchedWaypoints, maxDistance, islandNodePairs);
        for (MatchedWaypoint mwp : unmatchedWaypoints) {
            preloadPosition(mwp.waypoint);
        }

        if (firstFileAccessFailed) {
            throw new IllegalArgumentException("datafile " + firstFileAccessName + " not found");
        }
        for (MatchedWaypoint mwp : unmatchedWaypoints) {
            if (mwp.crosspoint == null) {
                throw new IllegalArgumentException(mwp.name + "-position not mapped in existing datafile");
            }
        }
    }

    private void preloadPosition(final OsmNode n) {
        final int d = 12500;
        firstFileAccessFailed = false;
        firstFileAccessName = null;
        loadSegmentFor(n.ilon, n.ilat);
        if (firstFileAccessFailed) {
            throw new IllegalArgumentException("datafile " + firstFileAccessName + " not found");
        }
        for (int idxLat = -1; idxLat <= 1; idxLat++) {
            for (int idxLon = -1; idxLon <= 1; idxLon++) {
                if (idxLon != 0 || idxLat != 0) {
                    loadSegmentFor(n.ilon + d * idxLon, n.ilat + d * idxLat);
                }
            }
        }
    }

    private OsmFile fileForSegment(final int lonDegree, final int latDegree) throws Exception {
        final int lonMod5 = lonDegree % 5;
        final int latMod5 = latDegree % 5;

        final int lon = lonDegree - 180 - lonMod5;
        final String slon = lon < 0 ? "W" + (-lon) : "E" + lon;
        final int lat = latDegree - 90 - latMod5;

        final String slat = lat < 0 ? "S" + (-lat) : "N" + lat;
        final String filenameBase = slon + "_" + slat;

        currentFileName = filenameBase + BRouterConstants.BROUTER_TILE_FILEEXTENSION;

        PhysicalFile ra = null;
        if (!fileCache.containsKey(filenameBase)) {

            final FileInformationCacheEntry fice = folderInfoCache.get(filenameBase);
            final ContentStorage.FileInformation fi;
            if (fice != null && (System.currentTimeMillis() - fice.fiTimestamp) < 60000) {
                fi = fice.fi;
            } else {
                fi = ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_TILES.getFolder(), filenameBase + BRouterConstants.BROUTER_TILE_FILEEXTENSION);
                folderInfoCache.put(filenameBase, new FileInformationCacheEntry(fi));
            }

            if (fi != null && !fi.isDirectory) {
                currentFileName = fi.name;

                final InputStream is = ContentStorage.get().openForRead(fi.uri);
                if (is instanceof FileInputStream) {
                    ra = new PhysicalFile(fi.name, (FileInputStream) is, dataBuffers, lookupVersion);
                } else {
                    Log.w("Problem opening tile file " + fi + ", is = " + is);
                    IOUtils.closeQuietly(is);
                }
            }
            fileCache.put(filenameBase, ra);
        }
        ra = fileCache.get(filenameBase);
        final OsmFile osmf = new OsmFile(ra, lonDegree, latDegree, dataBuffers);

        if (firstFileAccessName == null) {
            firstFileAccessName = currentFileName;
            firstFileAccessFailed = osmf.filename == null;
        }

        return osmf;
    }

    @Override
    public void close() {
        for (PhysicalFile f : fileCache.values()) {
            if (f != null) {
                f.close();
            }
        }
    }
}
