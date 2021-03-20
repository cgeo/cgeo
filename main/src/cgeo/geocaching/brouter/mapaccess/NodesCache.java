/**
 * Efficient cache or osmnodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.codec.WaypointMatcher;
import cgeo.geocaching.brouter.expressions.BExpressionContextWay;

public final class NodesCache {
    public OsmNodesMap nodesMap;
    public WaypointMatcher waypointMatcher;
    public boolean first_file_access_failed = false;
    public String first_file_access_name;
    private final File segmentDir;
    private File secondarySegmentsDir = null;
    private final BExpressionContextWay expCtxWay;
    private final int lookupVersion;
    private final int lookupMinorVersion;
    private final boolean forceSecondaryData;
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

    public NodesCache(String segmentDir, BExpressionContextWay ctxWay, boolean forceSecondaryData, long maxmem, NodesCache oldCache, boolean detailed) {
        this.maxmemtiles = maxmem / 8;
        this.segmentDir = new File(segmentDir);
        this.nodesMap = new OsmNodesMap();
        this.nodesMap.maxmem = (2L * maxmem) / 3L;
        this.expCtxWay = ctxWay;
        this.lookupVersion = ctxWay.meta.lookupVersion;
        this.lookupMinorVersion = ctxWay.meta.lookupMinorVersion;
        this.forceSecondaryData = forceSecondaryData;
        this.detailed = detailed;

        if (ctxWay != null) {
            ctxWay.setDecodeForbidden(detailed);
        }

        first_file_access_failed = false;
        first_file_access_name = null;

        if (!this.segmentDir.isDirectory())
            throw new RuntimeException("segment directory " + segmentDir + " does not exist");

        if (oldCache != null) {
            fileCache = oldCache.fileCache;
            dataBuffers = oldCache.dataBuffers;
            secondarySegmentsDir = oldCache.secondarySegmentsDir;

            // re-use old, virgin caches (if same detail-mode)
            if (oldCache.detailed == detailed) {
                fileRows = oldCache.fileRows;
                for (OsmFile[] fileRow : fileRows) {
                    if (fileRow == null)
                        continue;
                    for (OsmFile osmf : fileRow) {
                        cacheSum += osmf.setGhostState();
                    }
                }
            } else {
                fileRows = new OsmFile[180][];
            }
        } else {
            fileCache = new HashMap<String, PhysicalFile>(4);
            fileRows = new OsmFile[180][];
            dataBuffers = new DataBuffers();
            secondarySegmentsDir = StorageConfigHelper.getSecondarySegmentDir(segmentDir);
        }
        ghostSum = cacheSum;
    }

    public String formatStatus() {
        return "collecting=" + garbageCollectionEnabled + " noGhosts=" + ghostCleaningDone + " cacheSum=" + cacheSum + " cacheSumClean=" + cacheSumClean + " ghostSum=" + ghostSum + " ghostWakeup=" + ghostWakeup;
    }

    public void clean(boolean all) {
        for (OsmFile[] fileRow : fileRows) {
            if (fileRow == null)
                continue;
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
            OsmFile[] fileRow = fileRows[i];
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

    public int loadSegmentFor(int ilon, int ilat) {
        MicroCache mc = getSegmentFor(ilon, ilat);
        return mc == null ? 0 : mc.getSize();
    }

    public MicroCache getSegmentFor(int ilon, int ilat) {
        try {
            int lonDegree = ilon / 1000000;
            int latDegree = ilat / 1000000;
            OsmFile osmf = null;
            OsmFile[] fileRow = fileRows[latDegree];
            int ndegrees = fileRow == null ? 0 : fileRow.length;
            for (int i = 0; i < ndegrees; i++) {
                if (fileRow[i].lonDegree == lonDegree) {
                    osmf = fileRow[i];
                    break;
                }
            }
            if (osmf == null) {
                osmf = fileForSegment(lonDegree, latDegree);
                OsmFile[] newFileRow = new OsmFile[ndegrees + 1];
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
    public boolean obtainNonHollowNode(OsmNode node) {
        if (!node.isHollow())
            return true;

        MicroCache segment = getSegmentFor(node.ilon, node.ilat);
        if (segment == null) {
            return false;
        }
        if (!node.isHollow()) {
            return true; // direct weaving...
        }

        long id = node.getIdFromPos();
        if (segment.getAndClear(id)) {
            node.parseNodeBody(segment, nodesMap, expCtxWay);
        }

        if (garbageCollectionEnabled) // garbage collection
        {
            cacheSum -= segment.collect(segment.getSize() >> 1); // threshold = 1/2 of size is deleted
        }

        return !node.isHollow();
    }


    /**
     * make sure all link targets of the given node are non-hollow
     */
    public void expandHollowLinkTargets(OsmNode n) {
        for (OsmLink link = n.firstlink; link != null; link = link.getNext(n)) {
            obtainNonHollowNode(link.getTarget(n));
        }
    }

    /**
     * make sure all link targets of the given node are non-hollow
     */
    public boolean hasHollowLinkTargets(OsmNode n) {
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
    public OsmNode getStartNode(long id) {
        // initialize the start-node
        OsmNode n = new OsmNode(id);
        n.setHollow();
        nodesMap.put(n);
        if (!obtainNonHollowNode(n)) {
            return null;
        }
        expandHollowLinkTargets(n);
        return n;
    }

    public OsmNode getGraphNode(OsmNode template) {
        OsmNode graphNode = new OsmNode(template.ilon, template.ilat);
        graphNode.setHollow();
        OsmNode existing = nodesMap.put(graphNode);
        if (existing == null) {
            return graphNode;
        }
        nodesMap.put(existing);
        return existing;
    }

    public void matchWaypointsToNodes(List<MatchedWaypoint> unmatchedWaypoints, double maxDistance, OsmNodePairSet islandNodePairs) {
        waypointMatcher = new WaypointMatcherImpl(unmatchedWaypoints, 250., islandNodePairs);
        for (MatchedWaypoint mwp : unmatchedWaypoints) {
            preloadPosition(mwp.waypoint);
        }

        if (first_file_access_failed) {
            throw new IllegalArgumentException("datafile " + first_file_access_name + " not found");
        }
        for (MatchedWaypoint mwp : unmatchedWaypoints) {
            if (mwp.crosspoint == null) {
                throw new IllegalArgumentException(mwp.name + "-position not mapped in existing datafile");
            }
        }
    }

    private void preloadPosition(OsmNode n) {
        int d = 12500;
        first_file_access_failed = false;
        first_file_access_name = null;
        loadSegmentFor(n.ilon, n.ilat);
        if (first_file_access_failed) {
            throw new IllegalArgumentException("datafile " + first_file_access_name + " not found");
        }
        for (int idxLat = -1; idxLat <= 1; idxLat++)
            for (int idxLon = -1; idxLon <= 1; idxLon++) {
                if (idxLon != 0 || idxLat != 0) {
                    loadSegmentFor(n.ilon + d * idxLon, n.ilat + d * idxLat);
                }
            }
    }

    private OsmFile fileForSegment(int lonDegree, int latDegree) throws Exception {
        int lonMod5 = lonDegree % 5;
        int latMod5 = latDegree % 5;

        int lon = lonDegree - 180 - lonMod5;
        String slon = lon < 0 ? "W" + (-lon) : "E" + lon;
        int lat = latDegree - 90 - latMod5;

        String slat = lat < 0 ? "S" + (-lat) : "N" + lat;
        String filenameBase = slon + "_" + slat;

        currentFileName = filenameBase + ".rd5";

        PhysicalFile ra = null;
        if (!fileCache.containsKey(filenameBase)) {
            File f = null;
            if (!forceSecondaryData) {
                File primary = new File(segmentDir, filenameBase + ".rd5");
                if (primary.exists()) {
                    f = primary;
                }
            }
            if (f == null) {
                File secondary = new File(secondarySegmentsDir, filenameBase + ".rd5");
                if (secondary.exists()) {
                    f = secondary;
                }
            }
            if (f != null) {
                currentFileName = f.getName();
                ra = new PhysicalFile(f, dataBuffers, lookupVersion, lookupMinorVersion);
            }
            fileCache.put(filenameBase, ra);
        }
        ra = fileCache.get(filenameBase);
        OsmFile osmf = new OsmFile(ra, lonDegree, latDegree, dataBuffers);

        if (first_file_access_name == null) {
            first_file_access_name = currentFileName;
            first_file_access_failed = osmf.filename == null;
        }

        return osmf;
    }

    public void close() {
        for (PhysicalFile f : fileCache.values()) {
            try {
                if (f != null)
                    f.ra.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
    }
}
