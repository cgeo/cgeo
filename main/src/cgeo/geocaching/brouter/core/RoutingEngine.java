package cgeo.geocaching.brouter.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cgeo.geocaching.brouter.mapaccess.MatchedWaypoint;
import cgeo.geocaching.brouter.mapaccess.NodesCache;
import cgeo.geocaching.brouter.mapaccess.OsmLink;
import cgeo.geocaching.brouter.mapaccess.OsmLinkHolder;
import cgeo.geocaching.brouter.mapaccess.OsmNode;
import cgeo.geocaching.brouter.mapaccess.OsmNodePairSet;
import cgeo.geocaching.brouter.util.SortedHeap;
import cgeo.geocaching.brouter.util.StackSampler;

public class RoutingEngine extends Thread {
    public double airDistanceCostFactor;
    public SearchBoundary boundary;
    public boolean quite = false;
    protected List<OsmNodeNamed> waypoints = null;
    protected List<MatchedWaypoint> matchedWaypoints;
    protected OsmTrack foundTrack = new OsmTrack();
    protected String errorMessage = null;
    protected String segmentDir;
    protected RoutingContext routingContext;
    private NodesCache nodesCache;
    private final SortedHeap<OsmPath> openSet = new SortedHeap<OsmPath>();
    private boolean finished = false;
    private int linksProcessed = 0;
    private int nodeLimit; // used for target island search
    private static final int MAXNODES_ISLAND_CHECK = 500;
    private final OsmNodePairSet islandNodePairs = new OsmNodePairSet(MAXNODES_ISLAND_CHECK);
    private OsmTrack foundRawTrack = null;
    private int alternativeIndex = 0;
    private volatile boolean terminated;
    private final String outfileBase;
    private final String logfileBase;
    private final boolean infoLogEnabled;
    private Writer infoLogWriter;
    private StackSampler stackSampler;
    private OsmTrack guideTrack;
    private OsmPathElement matchPath;
    private long startTime;
    private long maxRunningTime;
    private Object[] extract;

    private final boolean directWeaving = !Boolean.getBoolean("disableDirectWeaving");

    public RoutingEngine(final String outfileBase, final String logfileBase, final String segmentDir,
                         final List<OsmNodeNamed> waypoints, final RoutingContext rc) {
        this.segmentDir = segmentDir;
        this.outfileBase = outfileBase;
        this.logfileBase = logfileBase;
        this.waypoints = waypoints;
        this.infoLogEnabled = outfileBase != null;
        this.routingContext = rc;

        final boolean cachedProfile = ProfileCache.parseProfile(rc);
        if (hasInfo()) {
            logInfo("parsed profile " + rc.profileFilename + " cached=" + cachedProfile);
        }
    }

    private boolean hasInfo() {
        return infoLogEnabled || infoLogWriter != null;
    }

    private void logInfo(String s) {
        if (infoLogEnabled) {
            System.out.println(s);
        }
        if (infoLogWriter != null) {
            try {
                infoLogWriter.write(s);
                infoLogWriter.write('\n');
                infoLogWriter.flush();
            } catch (IOException io) {
                infoLogWriter = null;
            }
        }
    }

    private void logThrowable(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        logInfo(sw.toString());
    }

    public void run() {
        doRun(0);
    }

    public void doRun(long maxRunningTime) {
        try {
            // delete nogos with waypoints in them
            routingContext.cleanNogolist(waypoints);

            startTime = System.currentTimeMillis();
            long startTime0 = startTime;
            this.maxRunningTime = maxRunningTime;
            int nsections = waypoints.size() - 1;
            OsmTrack[] refTracks = new OsmTrack[nsections]; // used ways for alternatives
            OsmTrack[] lastTracks = new OsmTrack[nsections];
            OsmTrack track = null;
            ArrayList<String> messageList = new ArrayList<String>();
            for (int i = 0; ; i++) {
                track = findTrack(refTracks, lastTracks);
                track.message = "track-length = " + track.distance + " filtered ascend = " + track.ascend
                    + " plain-ascend = " + track.plainAscend + " cost=" + track.cost;
                if (track.energy != 0) {
                    track.message += " energy=" + track.getFormattedEnergy() + " time=" + track.getFormattedTime2();
                }
                track.name = "brouter_" + routingContext.getProfileName() + "_" + i;

                messageList.add(track.message);
                track.messageList = messageList;
                if (outfileBase != null) {
                    String filename = outfileBase + i + ".gpx";
                    OsmTrack oldTrack = new OsmTrack();
                    oldTrack.readGpx(filename);
                    if (track.equalsTrack(oldTrack)) {
                        continue;
                    }
                    oldTrack = null;
                    track.writeGpx(filename);
                    foundTrack = track;
                    alternativeIndex = i;
                } else {
                    if (i == routingContext.getAlternativeIdx(0, 3)) {
                        if ("CSV".equals(System.getProperty("reportFormat"))) {
                            track.dumpMessages(null, routingContext);
                        } else {
                            if (!quite) {
                                System.out.println(track.formatAsGpx());
                            }
                        }
                        foundTrack = track;
                    } else {
                        continue;
                    }
                }
                if (logfileBase != null) {
                    String logfilename = logfileBase + i + ".csv";
                    track.dumpMessages(logfilename, routingContext);
                }
                break;
            }
            long endTime = System.currentTimeMillis();
            logInfo("execution time = " + (endTime - startTime0) / 1000. + " seconds");
        } catch (IllegalArgumentException e) {
            logException(e);
        } catch (Exception e) {
            logException(e);
            logThrowable(e);
        } catch (Error e) {
            cleanOnOOM();
            logException(e);
            logThrowable(e);
        } finally {
            if (hasInfo() && routingContext.expctxWay != null) {
                logInfo("expression cache stats=" + routingContext.expctxWay.cacheStats());
            }

            ProfileCache.releaseProfile(routingContext);

            if (nodesCache != null) {
                if (hasInfo() && nodesCache != null) {
                    logInfo("NodesCache status before close=" + nodesCache.formatStatus());
                }
                nodesCache.close();
                nodesCache = null;
            }
            openSet.clear();
            finished = true; // this signals termination to outside

            if (infoLogWriter != null) {
                try {
                    infoLogWriter.close();
                } catch (Exception e) {
                }
                infoLogWriter = null;
            }

            if (stackSampler != null) {
                try {
                    stackSampler.close();
                } catch (Exception e) {
                }
                stackSampler = null;
            }

        }
    }

    private void logException(Throwable t) {
        errorMessage = t instanceof IllegalArgumentException ? t.getMessage() : t.toString();
        logInfo("Error (linksProcessed=" + linksProcessed + " open paths: " + openSet.getSize() + "): " + errorMessage);
    }


    public void doSearch() {
        try {
            MatchedWaypoint seedPoint = new MatchedWaypoint();
            seedPoint.waypoint = waypoints.get(0);
            List<MatchedWaypoint> listOne = new ArrayList<MatchedWaypoint>();
            listOne.add(seedPoint);
            matchWaypointsToNodes(listOne);

            findTrack("seededSearch", seedPoint, null, null, null, false);
        } catch (IllegalArgumentException e) {
            logException(e);
        } catch (Exception e) {
            logException(e);
            logThrowable(e);
        } catch (Error e) {
            cleanOnOOM();
            logException(e);
            logThrowable(e);
        } finally {
            ProfileCache.releaseProfile(routingContext);
            if (nodesCache != null) {
                nodesCache.close();
                nodesCache = null;
            }
            openSet.clear();
            finished = true; // this signals termination to outside

            if (infoLogWriter != null) {
                try {
                    infoLogWriter.close();
                } catch (Exception e) {
                }
                infoLogWriter = null;
            }
        }
    }

    public void cleanOnOOM() {
        terminate();
    }

    private OsmTrack findTrack(OsmTrack[] refTracks, OsmTrack[] lastTracks) {
        for (; ; ) {
            try {
                return tryFindTrack(refTracks, lastTracks);
            } catch (RoutingIslandException rie) {
                islandNodePairs.freezeTempPairs();
                nodesCache.clean(true);
                matchedWaypoints = null;
            }
        }
    }

    private OsmTrack tryFindTrack(OsmTrack[] refTracks, OsmTrack[] lastTracks) {
        OsmTrack totaltrack = new OsmTrack();
        int nUnmatched = waypoints.size();

        if (hasInfo()) {
            for (OsmNodeNamed wp : waypoints) {
                logInfo("wp=" + wp);
            }
        }

        // check for a track for that target
        OsmTrack nearbyTrack = null;
        if (lastTracks[waypoints.size() - 2] == null) {
            StringBuilder debugInfo = hasInfo() ? new StringBuilder() : null;
            nearbyTrack = OsmTrack.readBinary(routingContext.rawTrackPath, waypoints.get(waypoints.size() - 1), routingContext.getNogoChecksums(), routingContext.profileTimestamp, debugInfo);
            if (nearbyTrack != null) {
                nUnmatched--;
            }
            if (hasInfo()) {
                boolean found = nearbyTrack != null;
                boolean dirty = found && nearbyTrack.isDirty;
                logInfo("read referenceTrack, found=" + found + " dirty=" + dirty + " " + debugInfo);
            }
        }

        if (matchedWaypoints == null) // could exist from the previous alternative level
        {
            matchedWaypoints = new ArrayList<MatchedWaypoint>();
            for (int i = 0; i < nUnmatched; i++) {
                MatchedWaypoint mwp = new MatchedWaypoint();
                mwp.waypoint = waypoints.get(i);
                mwp.name = waypoints.get(i).name;
                matchedWaypoints.add(mwp);
            }
            matchWaypointsToNodes(matchedWaypoints);

            // detect target islands: restricted search in inverse direction
            routingContext.inverseDirection = !routingContext.inverseRouting;
            airDistanceCostFactor = 0.;
            for (int i = 0; i < matchedWaypoints.size() - 1; i++) {
                nodeLimit = MAXNODES_ISLAND_CHECK;
                if (routingContext.inverseRouting) {
                    OsmTrack seg = findTrack("start-island-check", matchedWaypoints.get(i), matchedWaypoints.get(i + 1), null, null, false);
                    if (seg == null && nodeLimit > 0) {
                        throw new IllegalArgumentException("start island detected for section " + i);
                    }
                } else {
                    OsmTrack seg = findTrack("target-island-check", matchedWaypoints.get(i + 1), matchedWaypoints.get(i), null, null, false);
                    if (seg == null && nodeLimit > 0) {
                        throw new IllegalArgumentException("target island detected for section " + i);
                    }
                }
            }
            routingContext.inverseDirection = false;
            nodeLimit = 0;

            if (nearbyTrack != null) {
                matchedWaypoints.add(nearbyTrack.endPoint);
            }
        }

        for (int i = 0; i < matchedWaypoints.size() - 1; i++) {
            if (lastTracks[i] != null) {
                if (refTracks[i] == null)
                    refTracks[i] = new OsmTrack();
                refTracks[i].addNodes(lastTracks[i]);
            }

            OsmTrack seg;
            if (routingContext.inverseRouting) {
                routingContext.inverseDirection = true;
                seg = searchTrack(matchedWaypoints.get(i + 1), matchedWaypoints.get(i), null, refTracks[i]);
                routingContext.inverseDirection = false;
            } else {
                seg = searchTrack(matchedWaypoints.get(i), matchedWaypoints.get(i + 1), i == matchedWaypoints.size() - 2 ? nearbyTrack : null, refTracks[i]);
            }

            if (seg == null)
                return null;
            totaltrack.appendTrack(seg);
            lastTracks[i] = seg;
        }
        if (routingContext.poipoints != null)
            totaltrack.pois = routingContext.poipoints;
        totaltrack.matchedWaypoints = matchedWaypoints;
        return totaltrack;
    }

    // geometric position matching finding the nearest routable way-section
    private void matchWaypointsToNodes(List<MatchedWaypoint> unmatchedWaypoints) {
        resetCache(false);
        nodesCache.matchWaypointsToNodes(unmatchedWaypoints, 250., islandNodePairs);
    }

    private OsmTrack searchTrack(MatchedWaypoint startWp, MatchedWaypoint endWp, OsmTrack nearbyTrack, OsmTrack refTrack) {
        OsmTrack track = null;
        double[] airDistanceCostFactors = new double[]{routingContext.pass1coefficient, routingContext.pass2coefficient};
        boolean isDirty = false;
        IllegalArgumentException dirtyMessage = null;

        if (nearbyTrack != null) {
            airDistanceCostFactor = 0.;
            try {
                track = findTrack("re-routing", startWp, endWp, nearbyTrack, refTrack, true);
            } catch (IllegalArgumentException iae) {
                if (terminated)
                    throw iae;

                // fast partial recalcs: if that timed out, but we had a match,
                // build the concatenation from the partial and the nearby track
                if (matchPath != null) {
                    track = mergeTrack(matchPath, nearbyTrack);
                    isDirty = true;
                    dirtyMessage = iae;
                    logInfo("using fast partial recalc");
                }
                if (maxRunningTime > 0) {
                    maxRunningTime += System.currentTimeMillis() - startTime; // reset timeout...
                }
            }
        }

        if (track == null) {
            for (int cfi = 0; cfi < airDistanceCostFactors.length; cfi++) {
                airDistanceCostFactor = airDistanceCostFactors[cfi];

                if (airDistanceCostFactor < 0.) {
                    continue;
                }

                OsmTrack t;
                try {
                    t = findTrack(cfi == 0 ? "pass0" : "pass1", startWp, endWp, track, refTrack, false);
                } catch (IllegalArgumentException iae) {
                    if (!terminated && matchPath != null) // timeout, but eventually prepare a dirty ref track
                    {
                        logInfo("supplying dirty reference track after timeout");
                        foundRawTrack = mergeTrack(matchPath, track);
                        foundRawTrack.endPoint = endWp;
                        foundRawTrack.nogoChecksums = routingContext.getNogoChecksums();
                        foundRawTrack.profileTimestamp = routingContext.profileTimestamp;
                        foundRawTrack.isDirty = true;
                    }
                    throw iae;
                }

                if (t == null && track != null && matchPath != null) {
                    // ups, didn't find it, use a merge
                    t = mergeTrack(matchPath, track);
                    logInfo("using sloppy merge cause pass1 didn't reach destination");
                }
                if (t != null) {
                    track = t;
                } else {
                    throw new IllegalArgumentException("no track found at pass=" + cfi);
                }
            }
        }
        if (track == null)
            throw new IllegalArgumentException("no track found");

        boolean wasClean = nearbyTrack != null && !nearbyTrack.isDirty;
        if (refTrack == null && !(wasClean && isDirty)) // do not overwrite a clean with a dirty track
        {
            logInfo("supplying new reference track, dirty=" + isDirty);
            track.endPoint = endWp;
            track.nogoChecksums = routingContext.getNogoChecksums();
            track.profileTimestamp = routingContext.profileTimestamp;
            track.isDirty = isDirty;
            foundRawTrack = track;
        }

        if (!wasClean && isDirty) {
            throw dirtyMessage;
        }

        // final run for verbose log info and detail nodes
        airDistanceCostFactor = 0.;
        guideTrack = track;
        startTime = System.currentTimeMillis(); // reset timeout...
        try {
            OsmTrack tt = findTrack("re-tracking", startWp, endWp, null, refTrack, false);
            if (tt == null)
                throw new IllegalArgumentException("error re-tracking track");
            return tt;
        } finally {
            guideTrack = null;
        }
    }


    private void resetCache(boolean detailed) {
        if (hasInfo() && nodesCache != null) {
            logInfo("NodesCache status before reset=" + nodesCache.formatStatus());
        }
        long maxmem = routingContext.memoryclass * 1024L * 1024L; // in MB

        nodesCache = new NodesCache(segmentDir, routingContext.expctxWay, routingContext.forceSecondaryData, maxmem, nodesCache, detailed);
        islandNodePairs.clearTempPairs();
    }

    private OsmPath getStartPath(OsmNode n1, OsmNode n2, MatchedWaypoint mwp, OsmNodeNamed endPos, boolean sameSegmentSearch) {
        if (endPos != null) {
            endPos.radius = 1.5;
        }
        OsmPath p = getStartPath(n1, n2, new OsmNodeNamed(mwp.crosspoint), endPos, sameSegmentSearch);

        // special case: start+end on same segment
        if (p.cost >= 0 && sameSegmentSearch && endPos != null && endPos.radius < 1.5) {
            p.treedepth = 0; // hack: mark for the final-check
        }
        return p;
    }


    private OsmPath getStartPath(OsmNode n1, OsmNode n2, OsmNodeNamed wp, OsmNodeNamed endPos, boolean sameSegmentSearch) {
        try {
            routingContext.setWaypoint(wp, sameSegmentSearch ? endPos : null, false);
            OsmPath bestPath = null;
            OsmLink bestLink = null;
            OsmLink startLink = new OsmLink(null, n1);
            OsmPath startPath = routingContext.createPath(startLink);
            startLink.addLinkHolder(startPath, null);
            double minradius = 1e10;
            for (OsmLink link = n1.firstlink; link != null; link = link.getNext(n1)) {
                OsmNode nextNode = link.getTarget(n1);
                if (nextNode.isHollow())
                    continue; // border node?
                if (nextNode.firstlink == null)
                    continue; // don't care about dead ends
                if (nextNode == n1)
                    continue; // ?
                if (nextNode != n2)
                    continue; // just that link

                wp.radius = 1.5;
                OsmPath testPath = routingContext.createPath(startPath, link, null, guideTrack != null);
                testPath.airdistance = endPos == null ? 0 : nextNode.calcDistance(endPos);
                if (wp.radius < minradius) {
                    bestPath = testPath;
                    minradius = wp.radius;
                    bestLink = link;
                }
            }
            if (bestLink != null) {
                bestLink.addLinkHolder(bestPath, n1);
            }
            bestPath.treedepth = 1;

            return bestPath;
        } finally {
            routingContext.unsetWaypoint();
        }
    }

    private OsmTrack findTrack(String operationName, MatchedWaypoint startWp, MatchedWaypoint endWp, OsmTrack costCuttingTrack, OsmTrack refTrack, boolean fastPartialRecalc) {
        try {
            boolean detailed = guideTrack != null;
            resetCache(detailed);
            nodesCache.nodesMap.cleanupMode = detailed ? 0 : (routingContext.considerTurnRestrictions ? 2 : 1);
            return _findTrack(operationName, startWp, endWp, costCuttingTrack, refTrack, fastPartialRecalc);
        } finally {
            nodesCache.clean(false); // clean only non-virgin caches
        }
    }


    private OsmTrack _findTrack(String operationName, MatchedWaypoint startWp, MatchedWaypoint endWp, OsmTrack costCuttingTrack, OsmTrack refTrack, boolean fastPartialRecalc) {
        boolean verbose = guideTrack != null;

        int maxTotalCost = guideTrack != null ? guideTrack.cost + 5000 : 1000000000;
        int firstMatchCost = 1000000000;

        logInfo("findtrack with airDistanceCostFactor=" + airDistanceCostFactor);
        if (costCuttingTrack != null)
            logInfo("costCuttingTrack.cost=" + costCuttingTrack.cost);

        matchPath = null;
        int nodesVisited = 0;

        long startNodeId1 = startWp.node1.getIdFromPos();
        long startNodeId2 = startWp.node2.getIdFromPos();
        long endNodeId1 = endWp == null ? -1L : endWp.node1.getIdFromPos();
        long endNodeId2 = endWp == null ? -1L : endWp.node2.getIdFromPos();
        OsmNode end1 = null;
        OsmNode end2 = null;
        OsmNodeNamed endPos = null;

        boolean sameSegmentSearch = false;
        OsmNode start1 = nodesCache.getGraphNode(startWp.node1);
        OsmNode start2 = nodesCache.getGraphNode(startWp.node2);
        if (endWp != null) {
            end1 = nodesCache.getGraphNode(endWp.node1);
            end2 = nodesCache.getGraphNode(endWp.node2);
            nodesCache.nodesMap.endNode1 = end1;
            nodesCache.nodesMap.endNode2 = end2;
            endPos = new OsmNodeNamed(endWp.crosspoint);
            sameSegmentSearch = (start1 == end1 && start2 == end2) || (start1 == end2 && start2 == end1);
        }
        if (!nodesCache.obtainNonHollowNode(start1)) {
            return null;
        }
        nodesCache.expandHollowLinkTargets(start1);
        if (!nodesCache.obtainNonHollowNode(start2)) {
            return null;
        }
        nodesCache.expandHollowLinkTargets(start2);


        routingContext.startDirectionValid = routingContext.forceUseStartDirection || fastPartialRecalc;
        routingContext.startDirectionValid &= routingContext.startDirection != null && !routingContext.inverseDirection;
        if (routingContext.startDirectionValid) {
            logInfo("using start direction " + routingContext.startDirection);
        }

        OsmPath startPath1 = getStartPath(start1, start2, startWp, endPos, sameSegmentSearch);
        OsmPath startPath2 = getStartPath(start2, start1, startWp, endPos, sameSegmentSearch);

        // check for an INITIAL match with the cost-cutting-track
        if (costCuttingTrack != null) {
            OsmPathElement pe1 = costCuttingTrack.getLink(startNodeId1, startNodeId2);
            if (pe1 != null) {
                logInfo("initialMatch pe1.cost=" + pe1.cost);
                int c = startPath1.cost - pe1.cost;
                if (c < 0)
                    c = 0;
                if (c < firstMatchCost)
                    firstMatchCost = c;
            }

            OsmPathElement pe2 = costCuttingTrack.getLink(startNodeId2, startNodeId1);
            if (pe2 != null) {
                logInfo("initialMatch pe2.cost=" + pe2.cost);
                int c = startPath2.cost - pe2.cost;
                if (c < 0)
                    c = 0;
                if (c < firstMatchCost)
                    firstMatchCost = c;
            }

            if (firstMatchCost < 1000000000)
                logInfo("firstMatchCost from initial match=" + firstMatchCost);
        }

        synchronized (openSet) {
            openSet.clear();
            addToOpenset(startPath1);
            addToOpenset(startPath2);
        }
        ArrayList<OsmPath> openBorderList = new ArrayList<OsmPath>(4096);
        boolean memoryPanicMode = false;
        boolean needNonPanicProcessing = false;

        for (; ; ) {
            if (terminated) {
                throw new IllegalArgumentException("operation killed by thread-priority-watchdog after " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
            }

            if (maxRunningTime > 0) {
                long timeout = (matchPath == null && fastPartialRecalc) ? maxRunningTime / 3 : maxRunningTime;
                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new IllegalArgumentException(operationName + " timeout after " + (timeout / 1000) + " seconds");
                }
            }

            synchronized (openSet) {

                OsmPath path = openSet.popLowestKeyValue();
                if (path == null) {
                    if (openBorderList.isEmpty()) {
                        break;
                    }
                    for (OsmPath p : openBorderList) {
                        openSet.add(p.cost + (int) (p.airdistance * airDistanceCostFactor), p);
                    }
                    openBorderList.clear();
                    memoryPanicMode = false;
                    needNonPanicProcessing = true;
                    continue;
                }

                if (path.airdistance == -1) {
                    path.unregisterUpTree(routingContext);
                    continue;
                }

                if (directWeaving && nodesCache.hasHollowLinkTargets(path.getTargetNode())) {
                    if (!memoryPanicMode) {
                        if (!nodesCache.nodesMap.isInMemoryBounds(openSet.getSize(), false)) {
// System.out.println( "collecting..." );
                            int nodesBefore = nodesCache.nodesMap.nodesCreated;
                            int pathsBefore = openSet.getSize();

                            nodesCache.nodesMap.collectOutreachers();
                            for (; ; ) {
                                OsmPath p3 = openSet.popLowestKeyValue();
                                if (p3 == null)
                                    break;
                                if (p3.airdistance != -1 && nodesCache.nodesMap.canEscape(p3.getTargetNode())) {
                                    openBorderList.add(p3);
                                }
                            }
                            nodesCache.nodesMap.clearTemp();
                            for (OsmPath p : openBorderList) {
                                openSet.add(p.cost + (int) (p.airdistance * airDistanceCostFactor), p);
                            }
                            openBorderList.clear();
                            logInfo("collected, nodes/paths before=" + nodesBefore + "/" + pathsBefore + " after=" + nodesCache.nodesMap.nodesCreated + "/" + openSet.getSize() + " maxTotalCost=" + maxTotalCost);
                            if (!nodesCache.nodesMap.isInMemoryBounds(openSet.getSize(), true)) {
                                if (maxTotalCost < 1000000000 || needNonPanicProcessing || fastPartialRecalc) {
                                    throw new IllegalArgumentException("memory limit reached");
                                }
                                memoryPanicMode = true;
                                logInfo("************************ memory limit reached, enabled memory panic mode *************************");
                            }
                        }
                    }
                    if (memoryPanicMode) {
                        openBorderList.add(path);
                        continue;
                    }
                }
                needNonPanicProcessing = false;


                if (fastPartialRecalc && matchPath != null && path.cost > 30L * firstMatchCost && !costCuttingTrack.isDirty) {
                    logInfo("early exit: firstMatchCost=" + firstMatchCost + " path.cost=" + path.cost);

                    // use an early exit, unless there's a realistc chance to complete within the timeout
                    if (path.cost > maxTotalCost / 2 && System.currentTimeMillis() - startTime < maxRunningTime / 3) {
                        logInfo("early exit supressed, running for completion, resetting timeout");
                        startTime = System.currentTimeMillis();
                        fastPartialRecalc = false;
                    } else {
                        throw new IllegalArgumentException("early exit for a close recalc");
                    }
                }

                if (nodeLimit > 0) // check node-limit for target island search
                {
                    if (--nodeLimit == 0) {
                        return null;
                    }
                }

                nodesVisited++;
                linksProcessed++;

                OsmLink currentLink = path.getLink();
                OsmNode sourceNode = path.getSourceNode();
                OsmNode currentNode = path.getTargetNode();

                if (currentLink.isLinkUnused()) {
                    path.unregisterUpTree(routingContext);
                    continue;
                }

                long currentNodeId = currentNode.getIdFromPos();
                long sourceNodeId = sourceNode.getIdFromPos();

                if (!path.didEnterDestinationArea()) {
                    islandNodePairs.addTempPair(sourceNodeId, currentNodeId);
                }

                if (path.treedepth != 1) {
                    if (path.treedepth == 0) // hack: sameSegment Paths marked treedepth=0 to pass above check
                    {
                        path.treedepth = 1;
                    }

                    if ((sourceNodeId == endNodeId1 && currentNodeId == endNodeId2)
                        || (sourceNodeId == endNodeId2 && currentNodeId == endNodeId1)) {
                        // track found, compile
                        logInfo("found track at cost " + path.cost + " nodesVisited = " + nodesVisited);
                        OsmTrack t = compileTrack(path, verbose);
                        t.showspeed = routingContext.showspeed;
                        return t;
                    }

                    // check for a match with the cost-cutting-track
                    if (costCuttingTrack != null) {
                        OsmPathElement pe = costCuttingTrack.getLink(sourceNodeId, currentNodeId);
                        if (pe != null) {
                            // remember first match cost for fast termination of partial recalcs
                            int parentcost = path.originElement == null ? 0 : path.originElement.cost;

                            // hitting start-element of costCuttingTrack?
                            int c = path.cost - parentcost - pe.cost;
                            if (c > 0)
                                parentcost += c;

                            if (parentcost < firstMatchCost)
                                firstMatchCost = parentcost;

                            int costEstimate = path.cost
                                + path.elevationCorrection(routingContext)
                                + (costCuttingTrack.cost - pe.cost);
                            if (costEstimate <= maxTotalCost) {
                                matchPath = OsmPathElement.create(path, routingContext.countTraffic);
                            }
                            if (costEstimate < maxTotalCost) {
                                logInfo("maxcost " + maxTotalCost + " -> " + costEstimate);
                                maxTotalCost = costEstimate;
                            }
                        }
                    }
                }

                int keepPathAirdistance = path.airdistance;
                OsmLinkHolder firstLinkHolder = currentLink.getFirstLinkHolder(sourceNode);
                for (OsmLinkHolder linkHolder = firstLinkHolder; linkHolder != null; linkHolder = linkHolder.getNextForLink()) {
                    ((OsmPath) linkHolder).airdistance = -1; // invalidate the entry in the open set;
                }

                if (path.treedepth > 1) {
                    boolean isBidir = currentLink.isBidirectional();
                    sourceNode.unlinkLink(currentLink);

                    // if the counterlink is alive and does not yet have a path, remove it
                    if (isBidir && currentLink.getFirstLinkHolder(currentNode) == null && !routingContext.considerTurnRestrictions) {
                        currentNode.unlinkLink(currentLink);
                    }
                }

                // recheck cutoff before doing expensive stuff
                if (path.cost + path.airdistance > maxTotalCost + 100) {
                    path.unregisterUpTree(routingContext);
                    continue;
                }

                nodesCache.nodesMap.currentMaxCost = maxTotalCost;
                nodesCache.nodesMap.currentPathCost = path.cost;
                nodesCache.nodesMap.destination = endPos;

                routingContext.firstPrePath = null;

                for (OsmLink link = currentNode.firstlink; link != null; link = link.getNext(currentNode)) {
                    OsmNode nextNode = link.getTarget(currentNode);

                    if (!nodesCache.obtainNonHollowNode(nextNode)) {
                        continue; // border node?
                    }
                    if (nextNode.firstlink == null) {
                        continue; // don't care about dead ends
                    }
                    if (nextNode == sourceNode) {
                        continue; // border node?
                    }

                    OsmPrePath prePath = routingContext.createPrePath(path, link);
                    if (prePath != null) {
                        prePath.next = routingContext.firstPrePath;
                        routingContext.firstPrePath = prePath;
                    }
                }

                for (OsmLink link = currentNode.firstlink; link != null; link = link.getNext(currentNode)) {
                    OsmNode nextNode = link.getTarget(currentNode);

                    if (!nodesCache.obtainNonHollowNode(nextNode)) {
                        continue; // border node?
                    }
                    if (nextNode.firstlink == null) {
                        continue; // don't care about dead ends
                    }
                    if (nextNode == sourceNode) {
                        continue; // border node?
                    }

                    if (guideTrack != null) {
                        int gidx = path.treedepth + 1;
                        if (gidx >= guideTrack.nodes.size()) {
                            continue;
                        }
                        OsmPathElement guideNode = guideTrack.nodes.get(routingContext.inverseRouting ? guideTrack.nodes.size() - 1 - gidx : gidx);
                        long nextId = nextNode.getIdFromPos();
                        if (nextId != guideNode.getIdFromPos()) {
                            // not along the guide-track, discard, but register for voice-hint processing
                            if (routingContext.turnInstructionMode > 0) {
                                OsmPath detour = routingContext.createPath(path, link, refTrack, true);
                                if (detour.cost >= 0. && nextId != startNodeId1 && nextId != startNodeId2) {
                                    guideTrack.registerDetourForId(currentNode.getIdFromPos(), OsmPathElement.create(detour, false));
                                }
                            }
                            continue;
                        }
                    }

                    OsmPath bestPath = null;

                    boolean isFinalLink = false;
                    long targetNodeId = nextNode.getIdFromPos();
                    if (currentNodeId == endNodeId1 || currentNodeId == endNodeId2) {
                        if (targetNodeId == endNodeId1 || targetNodeId == endNodeId2) {
                            isFinalLink = true;
                        }
                    }

                    for (OsmLinkHolder linkHolder = firstLinkHolder; linkHolder != null; linkHolder = linkHolder.getNextForLink()) {
                        OsmPath otherPath = (OsmPath) linkHolder;
                        try {
                            if (isFinalLink) {
                                endPos.radius = 1.5; // 1.5 meters is the upper limit that will not change the unit-test result..
                                routingContext.setWaypoint(endPos, true);
                            }
                            OsmPath testPath = routingContext.createPath(otherPath, link, refTrack, guideTrack != null);
                            if (testPath.cost >= 0 && (bestPath == null || testPath.cost < bestPath.cost)) {
                                bestPath = testPath;
                            }
                        } finally {
                            if (isFinalLink) {
                                routingContext.unsetWaypoint();
                            }
                        }
                    }
                    if (bestPath != null) {
                        boolean trafficSim = endPos == null;

                        bestPath.airdistance = trafficSim ? keepPathAirdistance : (isFinalLink ? 0 : nextNode.calcDistance(endPos));

                        boolean inRadius = boundary == null || boundary.isInBoundary(nextNode, bestPath.cost);

                        if (inRadius && (isFinalLink || bestPath.cost + bestPath.airdistance <= maxTotalCost + 100)) {
                            // add only if this may beat an existing path for that link
                            OsmLinkHolder dominator = link.getFirstLinkHolder(currentNode);
                            while (!trafficSim && dominator != null) {
                                OsmPath dp = (OsmPath) dominator;
                                if (dp.airdistance != -1 && bestPath.definitlyWorseThan(dp, routingContext)) {
                                    break;
                                }
                                dominator = dominator.getNextForLink();
                            }

                            if (dominator == null) {
                                if (trafficSim && boundary != null && path.cost == 0 && bestPath.cost > 0) {
                                    bestPath.airdistance += boundary.getBoundaryDistance(nextNode);
                                }
                                bestPath.treedepth = path.treedepth + 1;
                                link.addLinkHolder(bestPath, currentNode);
                                addToOpenset(bestPath);
                            }
                        }
                    }
                }

                path.unregisterUpTree(routingContext);
            }
        }

        if (nodesVisited < MAXNODES_ISLAND_CHECK && islandNodePairs.getFreezeCount() < 5) {
            throw new RoutingIslandException();
        }

        return null;
    }

    private void addToOpenset(OsmPath path) {
        if (path.cost >= 0) {
            openSet.add(path.cost + (int) (path.airdistance * airDistanceCostFactor), path);
            path.registerUpTree();
        }
    }

    private OsmTrack compileTrack(OsmPath path, boolean verbose) {
        OsmPathElement element = OsmPathElement.create(path, false);

        // for final track, cut endnode
        if (guideTrack != null) {
            element = element.origin;
        }

        float totalTime = element.getTime();
        float totalEnergy = element.getEnergy();

        OsmTrack track = new OsmTrack();
        track.cost = path.cost;
        track.energy = (int) path.getTotalEnergy();

        int distance = 0;
        double ascend = 0;
        double ehb = 0.;

        short ele_start = Short.MIN_VALUE;
        short ele_end = Short.MIN_VALUE;

        double eleFactor = routingContext.inverseRouting ? -0.25 : 0.25;
        while (element != null) {
            if (guideTrack != null && element.message == null) {
                element.message = new MessageData();
            }

            if (routingContext.inverseRouting) {
                element.setTime(totalTime - element.getTime());
                element.setEnergy(totalEnergy - element.getEnergy());
                track.nodes.add(element);
            } else {
                track.nodes.add(0, element);
            }

            OsmPathElement nextElement = element.origin;

            short ele = element.getSElev();
            if (ele != Short.MIN_VALUE)
                ele_start = ele;
            if (ele_end == Short.MIN_VALUE)
                ele_end = ele;

            if (nextElement != null) {
                distance += element.calcDistance(nextElement);
                short ele_next = nextElement.getSElev();
                if (ele_next != Short.MIN_VALUE) {
                    ehb = ehb + (ele - ele_next) * eleFactor;
                }
                if (ehb > 10.) {
                    ascend += ehb - 10.;
                    ehb = 10.;
                } else if (ehb < 0.) {
                    ehb = 0.;
                }
            }
            element = nextElement;
        }
        ascend += ehb;
        track.distance = distance;
        track.ascend = (int) ascend;
        track.plainAscend = (int) ((ele_end - ele_start) * eleFactor + 0.5);
        logInfo("track-length = " + track.distance);
        logInfo("filtered ascend = " + track.ascend);
        track.buildMap();

        // for final track..
        if (guideTrack != null) {
            track.copyDetours(guideTrack);
            track.processVoiceHints(routingContext);
            track.prepareSpeedProfile(routingContext);
        }
        return track;
    }

    private OsmTrack mergeTrack(OsmPathElement match, OsmTrack oldTrack) {
        logInfo("**************** merging match=" + match.cost + " with oldTrack=" + oldTrack.cost);
        OsmPathElement element = match;
        OsmTrack track = new OsmTrack();
        track.cost = oldTrack.cost;

        while (element != null) {
            track.addNode(element);
            element = element.origin;
        }
        long lastId = 0;
        long id1 = match.getIdFromPos();
        long id0 = match.origin == null ? 0 : match.origin.getIdFromPos();
        boolean appending = false;
        for (OsmPathElement n : oldTrack.nodes) {
            if (appending) {
                track.nodes.add(n);
            }

            long id = n.getIdFromPos();
            if (id == id1 && lastId == id0) {
                appending = true;
            }
            lastId = id;
        }


        track.buildMap();
        return track;
    }

    public int getPathPeak() {
        synchronized (openSet) {
            return openSet.getPeakSize();
        }
    }

    public int[] getOpenSet() {
        if (extract == null) {
            extract = new Object[500];
        }

        synchronized (openSet) {
            if (guideTrack != null) {
                ArrayList<OsmPathElement> nodes = guideTrack.nodes;
                int[] res = new int[nodes.size() * 2];
                int i = 0;
                for (OsmPathElement n : nodes) {
                    res[i++] = n.getILon();
                    res[i++] = n.getILat();
                }
                return res;
            }

            int size = openSet.getExtract(extract);
            int[] res = new int[size * 2];
            for (int i = 0, j = 0; i < size; i++) {
                OsmPath p = (OsmPath) extract[i];
                extract[i] = null;
                OsmNode n = p.getTargetNode();
                res[j++] = n.ilon;
                res[j++] = n.ilat;
            }
            return res;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public int getLinksProcessed() {
        return linksProcessed;
    }

    public int getDistance() {
        return foundTrack.distance;
    }

    public int getAscend() {
        return foundTrack.ascend;
    }

    public int getPlainAscend() {
        return foundTrack.plainAscend;
    }

    public String getTime() {
        return foundTrack.getFormattedTime2();
    }

    public OsmTrack getFoundTrack() {
        return foundTrack;
    }

    public int getAlternativeIndex() {
        return alternativeIndex;
    }

    public OsmTrack getFoundRawTrack() {
        return foundRawTrack;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void terminate() {
        terminated = true;
    }

    public boolean isTerminated() {
        return terminated;
    }

}
