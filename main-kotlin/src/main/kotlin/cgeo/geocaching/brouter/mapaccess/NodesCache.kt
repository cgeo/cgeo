// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

/**
 * Efficient cache or osmnodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess

import cgeo.geocaching.brouter.BRouterConstants
import cgeo.geocaching.brouter.codec.DataBuffers
import cgeo.geocaching.brouter.codec.MicroCache
import cgeo.geocaching.brouter.codec.WaypointMatcher
import cgeo.geocaching.brouter.expressions.BExpressionContextWay
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.Log

import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.HashMap
import java.util.Hashtable
import java.util.List
import java.util.Map

import org.apache.commons.io.IOUtils

class NodesCache : Closeable {
    public OsmNodesMap nodesMap
    public WaypointMatcher waypointMatcher
    var firstFileAccessFailed: Boolean = false
    public String firstFileAccessName
    private final BExpressionContextWay expCtxWay
    private final Int lookupVersion
    private String currentFileName
    private final Map<String, PhysicalFile> fileCache
    private final DataBuffers dataBuffers
    private final OsmFile[][] fileRows
    private var cacheSum: Long = 0
    private Long maxmemtiles
    private final Boolean detailed; // NOPMD used in constructor

    private var garbageCollectionEnabled: Boolean = false
    private var ghostCleaningDone: Boolean = false


    private var cacheSumClean: Long = 0
    private var ghostSum: Long = 0
    private var ghostWakeup: Long = 0

    private val directWeaving: Boolean = !Boolean.getBoolean("disableDirectWeaving")

    private static val folderInfoCache: Hashtable<String, FileInformationCacheEntry> = Hashtable<>()

    private static class FileInformationCacheEntry {
        public ContentStorage.FileInformation fi
        public Long fiTimestamp

        FileInformationCacheEntry(final ContentStorage.FileInformation fi) {
            this.fi = fi
            this.fiTimestamp = System.currentTimeMillis()
        }
    }

    public NodesCache(final BExpressionContextWay ctxWay, final Long maxmem, final NodesCache oldCache, final Boolean detailed) {
        this.maxmemtiles = maxmem / 8
        this.nodesMap = OsmNodesMap()
        this.nodesMap.maxmem = (2L * maxmem) / 3L
        this.expCtxWay = ctxWay
        this.lookupVersion = ctxWay.meta.lookupVersion
        this.detailed = detailed

        if (ctxWay != null) {
            ctxWay.setDecodeForbidden(detailed)
        }

        firstFileAccessFailed = false
        firstFileAccessName = null

        if (oldCache != null) {
            fileCache = oldCache.fileCache
            dataBuffers = oldCache.dataBuffers

            // re-use old, virgin caches (if same detail-mode)
            if (oldCache.detailed == detailed) {
                fileRows = oldCache.fileRows
                for (OsmFile[] fileRow : fileRows) {
                    if (fileRow == null) {
                        continue
                    }
                    for (OsmFile osmf : fileRow) {
                        cacheSum += osmf.setGhostState()
                    }
                }
            } else {
                fileRows = OsmFile[180][]
            }
        } else {
            fileCache = HashMap<>(4)
            fileRows = OsmFile[180][]
            dataBuffers = DataBuffers()
        }
        ghostSum = cacheSum
    }

    public String formatStatus() {
        return "collecting=" + garbageCollectionEnabled + " noGhosts=" + ghostCleaningDone + " cacheSum=" + cacheSum + " cacheSumClean=" + cacheSumClean + " ghostSum=" + ghostSum + " ghostWakeup=" + ghostWakeup
    }

    public Unit clean(final Boolean all) {
        for (OsmFile[] fileRow : fileRows) {
            if (fileRow == null) {
                continue
            }
            for (OsmFile osmf : fileRow) {
                osmf.clean(all)
            }
        }
    }

    // if the cache sum exceeded a threshold,
    // clean all ghosts and enable garbage collection
    private Unit checkEnableCacheCleaning() {
        if (cacheSum < maxmemtiles) {
            return
        }

        for (final OsmFile[] fileRow : fileRows) {
            if (fileRow == null) {
                continue
            }
            for (OsmFile osmf : fileRow) {
                if (garbageCollectionEnabled && !ghostCleaningDone) {
                    cacheSum -= osmf.cleanGhosts()
                } else {
                    cacheSum -= osmf.collectAll()
                }
            }
        }

        if (garbageCollectionEnabled) {
            ghostCleaningDone = true
            maxmemtiles *= 2
        } else {
            cacheSumClean = cacheSum
            garbageCollectionEnabled = true
        }
    }

    public Int loadSegmentFor(final Int ilon, final Int ilat) {
        val mc: MicroCache = getSegmentFor(ilon, ilat)
        return mc == null ? 0 : mc.getSize()
    }

    public MicroCache getSegmentFor(final Int ilon, final Int ilat) {
        try {
            val lonDegree: Int = ilon / 1000000
            val latDegree: Int = ilat / 1000000
            OsmFile osmf = null
            final OsmFile[] fileRow = fileRows[latDegree]
            val ndegrees: Int = fileRow == null ? 0 : fileRow.length
            for (Int i = 0; i < ndegrees; i++) {
                if (fileRow[i].lonDegree == lonDegree) {
                    osmf = fileRow[i]
                    break
                }
            }
            if (osmf == null) {
                osmf = fileForSegment(lonDegree, latDegree)
                final OsmFile[] newFileRow = OsmFile[ndegrees + 1]
                for (Int i = 0; i < ndegrees; i++) {
                    newFileRow[i] = fileRow[i]
                }
                newFileRow[ndegrees] = osmf
                fileRows[latDegree] = newFileRow
            }
            currentFileName = osmf.filename

            if (!osmf.hasData()) {
                return null
            }

            MicroCache segment = osmf.getMicroCache(ilon, ilat)
            if (segment == null) {
                checkEnableCacheCleaning()
                segment = osmf.createMicroCache(ilon, ilat, dataBuffers, expCtxWay, waypointMatcher, directWeaving ? nodesMap : null)

                cacheSum += segment.getDataSize()
            } else if (segment.ghost) {
                segment.unGhost()
                ghostWakeup += segment.getDataSize()
            }
            return segment
        } catch (IOException re) {
            throw RuntimeException(re.getMessage(), re); // NOPMD
        } catch (RuntimeException re) {
            throw re
        } catch (Exception e) {
            throw RuntimeException("error reading datafile " + currentFileName + ": " + e, e)
        }
    }

    /**
     * make sure the given node is non-hollow,
     * which means it contains not just the id,
     * but also the actual data
     *
     * @return true if successfull, false if node is still hollow
     */
    public Boolean obtainNonHollowNode(final OsmNode node) {
        if (!node.isHollow()) {
            return true
        }

        val segment: MicroCache = getSegmentFor(node.ilon, node.ilat)
        if (segment == null) {
            return false
        }
        if (!node.isHollow()) {
            return true; // direct weaving...
        }

        val id: Long = node.getIdFromPos()
        if (segment.getAndClear(id)) {
            node.parseNodeBody(segment, nodesMap, expCtxWay)
        }

        if (garbageCollectionEnabled) { // garbage collection
            cacheSum -= segment.collect(segment.getSize() >> 1); // threshold = 1/2 of size is deleted
        }

        return !node.isHollow()
    }


    /**
     * make sure all link targets of the given node are non-hollow
     */
    public Unit expandHollowLinkTargets(final OsmNode n) {
        for (OsmLink link = n.firstlink; link != null; link = link.getNext(n)) {
            obtainNonHollowNode(link.getTarget(n))
        }
    }

    /**
     * make sure all link targets of the given node are non-hollow
     */
    public Boolean hasHollowLinkTargets(final OsmNode n) {
        for (OsmLink link = n.firstlink; link != null; link = link.getNext(n)) {
            if (link.getTarget(n).isHollow()) {
                return true
            }
        }
        return false
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
    public OsmNode getStartNode(final Long id) {
        // initialize the start-node
        val n: OsmNode = OsmNode(id)
        n.setHollow()
        nodesMap.put(n)
        if (!obtainNonHollowNode(n)) {
            return null
        }
        expandHollowLinkTargets(n)
        return n
    }

    public OsmNode getGraphNode(final OsmNode template) {
        val graphNode: OsmNode = OsmNode(template.ilon, template.ilat)
        graphNode.setHollow()
        val existing: OsmNode = nodesMap.put(graphNode)
        if (existing == null) {
            return graphNode
        }
        nodesMap.put(existing)
        return existing
    }

    public Unit matchWaypointsToNodes(final List<MatchedWaypoint> unmatchedWaypoints, final Double maxDistance, final OsmNodePairSet islandNodePairs) {
        waypointMatcher = WaypointMatcherImpl(unmatchedWaypoints, maxDistance, islandNodePairs)
        for (MatchedWaypoint mwp : unmatchedWaypoints) {
            Int cellsize = 12500
            preloadPosition(mwp.waypoint, cellsize)
            // get a second chance
            if (mwp.crosspoint == null) {
                cellsize = 1000000 / 32
                preloadPosition(mwp.waypoint, cellsize)
            }
        }

        if (firstFileAccessFailed) {
            throw IllegalArgumentException("datafile " + firstFileAccessName + " not found")
        }
        val len: Int = unmatchedWaypoints.size()
        for (Int i = 0; i < len; i++) {
            val mwp: MatchedWaypoint = unmatchedWaypoints.get(i)
            if (mwp.crosspoint == null) {
                if (unmatchedWaypoints.size() > 1 && i == unmatchedWaypoints.size() - 1 && unmatchedWaypoints.get(i - 1).direct) {
                    mwp.crosspoint = OsmNode(mwp.waypoint.ilon, mwp.waypoint.ilat)
                    mwp.direct = true
                } else {
                    throw IllegalArgumentException(mwp.name + "-position not mapped in existing datafile")
                }
            }
            if (unmatchedWaypoints.size() > 1 && i == unmatchedWaypoints.size() - 1 && unmatchedWaypoints.get(i - 1).direct) {
                mwp.crosspoint = OsmNode(mwp.waypoint.ilon, mwp.waypoint.ilat)
                mwp.direct = true
            }
        }
    }

    private Unit preloadPosition(final OsmNode n, final Int d) {
        firstFileAccessFailed = false
        firstFileAccessName = null
        loadSegmentFor(n.ilon, n.ilat)
        if (firstFileAccessFailed) {
            throw IllegalArgumentException("datafile " + firstFileAccessName + " not found")
        }
        for (Int idxLat = -1; idxLat <= 1; idxLat++) {
            for (Int idxLon = -1; idxLon <= 1; idxLon++) {
                if (idxLon != 0 || idxLat != 0) {
                    loadSegmentFor(n.ilon + d * idxLon, n.ilat + d * idxLat)
                }
            }
        }
    }

    private OsmFile fileForSegment(final Int lonDegree, final Int latDegree) throws Exception {
        val lonMod5: Int = lonDegree % 5
        val latMod5: Int = latDegree % 5

        val lon: Int = lonDegree - 180 - lonMod5
        val slon: String = lon < 0 ? "W" + (-lon) : "E" + lon
        val lat: Int = latDegree - 90 - latMod5

        val slat: String = lat < 0 ? "S" + (-lat) : "N" + lat
        val filenameBase: String = slon + "_" + slat

        currentFileName = filenameBase + BRouterConstants.BROUTER_TILE_FILEEXTENSION

        PhysicalFile ra = null
        if (!fileCache.containsKey(filenameBase)) {

            val fice: FileInformationCacheEntry = folderInfoCache.get(filenameBase)
            final ContentStorage.FileInformation fi
            if (fice != null && (System.currentTimeMillis() - fice.fiTimestamp) < 60000) {
                fi = fice.fi
            } else {
                fi = ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_TILES.getFolder(), filenameBase + BRouterConstants.BROUTER_TILE_FILEEXTENSION)
                folderInfoCache.put(filenameBase, FileInformationCacheEntry(fi))
            }

            if (fi != null && !fi.isDirectory) {
                currentFileName = fi.name

                val is: InputStream = ContentStorage.get().openForRead(fi.uri)
                if (is is FileInputStream) {
                    ra = PhysicalFile(fi.name, (FileInputStream) is, dataBuffers, lookupVersion)
                } else {
                    Log.w("Problem opening tile file " + fi + ", is = " + is)
                    IOUtils.closeQuietly(is)
                }
            }
            fileCache.put(filenameBase, ra)
        }
        ra = fileCache.get(filenameBase)
        val osmf: OsmFile = OsmFile(ra, lonDegree, latDegree, dataBuffers)

        if (firstFileAccessName == null) {
            firstFileAccessName = currentFileName
            firstFileAccessFailed = osmf.filename == null
        }

        return osmf
    }

    override     public Unit close() {
        for (PhysicalFile f : fileCache.values()) {
            if (f != null) {
                f.close()
            }
        }
    }

    public Int getElevationType(final Int ilon, final Int ilat) {
        val lonDegree: Int = ilon / 1000000
        val latDegree: Int = ilat / 1000000
        final OsmFile[] fileRow = fileRows[latDegree]
        val ndegrees: Int = fileRow == null ? 0 : fileRow.length
        for (Int i = 0; i < ndegrees; i++) {
            if (fileRow[i].lonDegree == lonDegree) {
                val osmf: OsmFile = fileRow[i]
                if (osmf != null) {
                    return osmf.elevationType
                }
                break
            }
        }
        return 3
    }
}
