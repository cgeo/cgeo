package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.MatchedWaypoint;
import cgeo.geocaching.brouter.mapaccess.NodesCache;
import cgeo.geocaching.brouter.mapaccess.OsmLink;
import cgeo.geocaching.brouter.mapaccess.OsmLinkHolder;
import cgeo.geocaching.brouter.mapaccess.OsmNode;
import cgeo.geocaching.brouter.mapaccess.OsmNodePairSet;
import cgeo.geocaching.brouter.util.SortedHeap;
import cgeo.geocaching.utils.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class RoutingEngine extends Thread {
    public double airDistanceCostFactor;
    public SearchBoundary boundary;
    protected List<OsmNodeNamed> waypoints = null;
    protected List<MatchedWaypoint> matchedWaypoints;
    protected OsmTrack foundTrack = new OsmTrack();
    protected String errorMessage = null;
    protected RoutingContext routingContext;
    private NodesCache nodesCache;
    private final SortedHeap<OsmPath> openSet = new SortedHeap<>();
    private boolean finished = false;
    private int linksProcessed = 0;
    private int nodeLimit; // used for target island search
    private static final int MAXNODES_ISLAND_CHECK = 500;
    private final OsmNodePairSet islandNodePairs = new OsmNodePairSet(MAXNODES_ISLAND_CHECK);
    private OsmTrack foundRawTrack = null;
    private volatile boolean terminated;
    private OsmTrack guideTrack;
    private OsmPathElement matchPath;
    private long startTime;
    private long maxRunningTime;

    private final boolean directWeaving = !Boolean.getBoolean("disableDirectWeaving");

    public RoutingEngine(final List<OsmNodeNamed> waypoints, final RoutingContext rc) {
        this.waypoints = waypoints;
        this.routingContext = rc;

        ProfileCache.parseProfile(rc);
    }

    private boolean hasInfo() {
        return Log.isEnabled(Log.LogLevel.INFO);
    }

    private void logInfo(final String s) {
        Log.i(s);
    }

    private void logThrowable(final Throwable t) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        logInfo(sw.toString());
    }

    public void run() {
        doRun(0);
    }

    public void doRun(final long maxRunningTime) {
        try {
            startTime = System.currentTimeMillis();
            final long startTime0 = startTime;
            this.maxRunningTime = maxRunningTime;
            final int nsections = waypoints.size() - 1;
            final OsmTrack[] refTracks = new OsmTrack[nsections]; // used ways for alternatives
            final OsmTrack[] lastTracks = new OsmTrack[nsections];
            OsmTrack track = null;
            final ArrayList<String> messageList = new ArrayList<>();
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
                if (i == routingContext.getAlternativeIdx(0, 3)) {
                    if ("CSV".equals(System.getProperty("reportFormat"))) {
                        track.dumpMessages(null, routingContext);
                    }
                    foundTrack = track;
                } else {
                    continue;
                }
                break;
            }
            final long endTime = System.currentTimeMillis();
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
        }
    }

    private void logException(final Throwable t) {
        errorMessage = t instanceof IllegalArgumentException ? t.getMessage() : t.toString();
        logInfo("Error (linksProcessed=" + linksProcessed + " open paths: " + openSet.getSize() + "): " + errorMessage);
    }


    public void cleanOnOOM() {
        terminate();
    }

    private OsmTrack findTrack(final OsmTrack[] refTracks, final OsmTrack[] lastTracks) {
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

    private OsmTrack tryFindTrack(final OsmTrack[] refTracks, final OsmTrack[] lastTracks) {
        final OsmTrack totaltrack = new OsmTrack();
        int nUnmatched = waypoints.size();

        if (hasInfo()) {
            for (OsmNodeNamed wp : waypoints) {
                logInfo("wp=" + wp);
            }
        }

        // check for a track for that target
        OsmTrack nearbyTrack = null;
        if (lastTracks[waypoints.size() - 2] == null) {
            final StringBuilder debugInfo = hasInfo() ? new StringBuilder() : null;
            nearbyTrack = OsmTrack.readBinary(routingContext.rawTrackPath, waypoints.get(waypoints.size() - 1), routingContext.getNogoChecksums(), routingContext.profileTimestamp, debugInfo);
            if (nearbyTrack != null) {
                nUnmatched--;
            }
            if (hasInfo()) {
                final boolean found = nearbyTrack != null;
                final boolean dirty = found && nearbyTrack.isDirty;
                logInfo("read referenceTrack, found=" + found + " dirty=" + dirty + " " + debugInfo);
            }
        }

        if (matchedWaypoints == null) { // could exist from the previous alternative level
            matchedWaypoints = new ArrayList<>();
            for (int i = 0; i < nUnmatched; i++) {
                final MatchedWaypoint mwp = new MatchedWaypoint();
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
                    final OsmTrack seg = findTrack("start-island-check", matchedWaypoints.get(i), matchedWaypoints.get(i + 1), null, null, false);
                    if (seg == null && nodeLimit > 0) {
                        throw new IllegalArgumentException("start island detected for section " + i);
                    }
                } else {
                    final OsmTrack seg = findTrack("target-island-check", matchedWaypoints.get(i + 1), matchedWaypoints.get(i), null, null, false);
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
                if (refTracks[i] == null) {
                    refTracks[i] = new OsmTrack();
                }
                refTracks[i].addNodes(lastTracks[i]);
            }

            final OsmTrack seg;
            if (routingContext.inverseRouting) {
                routingContext.inverseDirection = true;
                seg = searchTrack(matchedWaypoints.get(i + 1), matchedWaypoints.get(i), null, refTracks[i]);
                routingContext.inverseDirection = false;
            } else {
                seg = searchTrack(matchedWaypoints.get(i), matchedWaypoints.get(i + 1), i == matchedWaypoints.size() - 2 ? nearbyTrack : null, refTracks[i]);
            }

            if (seg == null) {
                return null;
            }
            totaltrack.appendTrack(seg);
            lastTracks[i] = seg;
        }
        if (routingContext.poipoints != null) {
            totaltrack.pois = routingContext.poipoints;
        }
        totaltrack.matchedWaypoints = matchedWaypoints;
        return totaltrack;
    }

    // geometric position matching finding the nearest routable way-section
    private void matchWaypointsToNodes(final List<MatchedWaypoint> unmatchedWaypoints) {
        resetCache(false);
        nodesCache.matchWaypointsToNodes(unmatchedWaypoints, routingContext.waypointCatchingRange, islandNodePairs);
    }

    private OsmTrack searchTrack(final MatchedWaypoint startWp, final MatchedWaypoint endWp, final OsmTrack nearbyTrack, final OsmTrack refTrack) {
        // remove nogos with waypoints inside
        try {
            final List<OsmNode> wpts2 = new ArrayList<>();
            wpts2.add(startWp.waypoint);
            wpts2.add(endWp.waypoint);
            final boolean calcBeeline = routingContext.allInOneNogo(wpts2);

            if (!calcBeeline) {
                return searchRoutedTrack(startWp, endWp, nearbyTrack, refTrack);
            }

            // we want a beeline-segment
            OsmPath path = routingContext.createPath(new OsmLink(null, startWp.crosspoint));
            path = routingContext.createPath(path, new OsmLink(startWp.crosspoint, endWp.crosspoint), null, false);
            return compileTrack(path);
        } finally {
            routingContext.restoreNogoList();
        }
    }

    @SuppressWarnings("PMD.NPathComplexity") // external code, do not refactor
    private OsmTrack searchRoutedTrack(MatchedWaypoint startWp, MatchedWaypoint endWp, OsmTrack nearbyTrack, OsmTrack refTrack) {
        OsmTrack track = null;
        final double[] airDistanceCostFactors = new double[]{routingContext.pass1coefficient, routingContext.pass2coefficient};
        boolean isDirty = false;
        IllegalArgumentException dirtyMessage = null;

        if (nearbyTrack != null) {
            airDistanceCostFactor = 0.;
            try {
                track = findTrack("re-routing", startWp, endWp, nearbyTrack, refTrack, true);
            } catch (IllegalArgumentException iae) {
                if (terminated) {
                    throw iae;
                }

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
                    if (!terminated && matchPath != null) { // timeout, but eventually prepare a dirty ref track
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
        if (track == null) {
            throw new IllegalArgumentException("no track found");
        }

        final boolean wasClean = nearbyTrack != null && !nearbyTrack.isDirty;
        if (refTrack == null && !(wasClean && isDirty)) { // do not overwrite a clean with a dirty track
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
            final OsmTrack tt = findTrack("re-tracking", startWp, endWp, null, refTrack, false);
            if (tt == null) {
                throw new IllegalArgumentException("error re-tracking track");
            }
            return tt;
        } finally {
            guideTrack = null;
        }
    }


    private void resetCache(final boolean detailed) {
        if (hasInfo() && nodesCache != null) {
            logInfo("NodesCache status before reset=" + nodesCache.formatStatus());
        }
        final long maxmem = routingContext.memoryclass * 1024L * 1024L; // in MB

        nodesCache = new NodesCache(routingContext.expctxWay, maxmem, nodesCache, detailed);
        islandNodePairs.clearTempPairs();
    }

    private OsmPath getStartPath(final OsmNode n1, final OsmNode n2, final MatchedWaypoint mwp, final OsmNodeNamed endPos, final boolean sameSegmentSearch) {
        if (endPos != null) {
            endPos.radius = 1.5;
        }
        final OsmPath p = getStartPath(n1, n2, new OsmNodeNamed(mwp.crosspoint), endPos, sameSegmentSearch);

        // special case: start+end on same segment
        if (p.cost >= 0 && sameSegmentSearch && endPos != null && endPos.radius < 1.5) {
            p.treedepth = 0; // hack: mark for the final-check
        }
        return p;
    }


    private OsmPath getStartPath(final OsmNode n1, final OsmNode n2, final OsmNodeNamed wp, final OsmNodeNamed endPos, final boolean sameSegmentSearch) {
        try {
            routingContext.setWaypoint(wp, sameSegmentSearch ? endPos : null, false);
            OsmPath bestPath = null;
            OsmLink bestLink = null;
            final OsmLink startLink = new OsmLink(null, n1);
            final OsmPath startPath = routingContext.createPath(startLink);
            startLink.addLinkHolder(startPath, null);
            double minradius = 1e10;
            for (OsmLink link = n1.firstlink; link != null; link = link.getNext(n1)) {
                final OsmNode nextNode = link.getTarget(n1);
                if (nextNode.isHollow()) {
                    continue; // border node?
                }
                if (nextNode.firstlink == null) {
                    continue; // don't care about dead ends
                }
                if (nextNode == n1) {
                    continue; // ?
                }
                if (nextNode != n2) {
                    continue; // just that link
                }

                wp.radius = 1.5;
                final OsmPath testPath = routingContext.createPath(startPath, link, null, guideTrack != null);
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

    private OsmTrack findTrack(final String operationName, final MatchedWaypoint startWp, final MatchedWaypoint endWp, final OsmTrack costCuttingTrack, final OsmTrack refTrack, final boolean fastPartialRecalc) {
        try {
            final List<OsmNode> wpts2 = new ArrayList<OsmNode>();
            if (startWp != null) {
                wpts2.add(startWp.waypoint);
            }
            if (endWp != null) {
                wpts2.add(endWp.waypoint);
            }
            routingContext.cleanNogoList(wpts2);

            final boolean detailed = guideTrack != null;
            resetCache(detailed);
            nodesCache.nodesMap.cleanupMode = detailed ? 0 : (routingContext.considerTurnRestrictions ? 2 : 1);
            return findTrackHelper(operationName, startWp, endWp, costCuttingTrack, refTrack, fastPartialRecalc);
        } finally {
            routingContext.restoreNogoList();
            nodesCache.clean(false); // clean only non-virgin caches
        }
    }


    private OsmTrack findTrackHelper(final String operationName, final MatchedWaypoint startWp, final MatchedWaypoint endWp, final OsmTrack costCuttingTrack, final OsmTrack refTrack, boolean fastPartialRecalc) {
        final boolean verbose = guideTrack != null;

        int maxTotalCost = guideTrack != null ? guideTrack.cost + 5000 : 1000000000;
        int firstMatchCost = 1000000000;

        logInfo("findtrack with airDistanceCostFactor=" + airDistanceCostFactor);
        if (costCuttingTrack != null) {
            logInfo("costCuttingTrack.cost=" + costCuttingTrack.cost);
        }

        matchPath = null;
        int nodesVisited = 0;

        final long startNodeId1 = startWp.node1.getIdFromPos();
        final long startNodeId2 = startWp.node2.getIdFromPos();
        final long endNodeId1 = endWp == null ? -1L : endWp.node1.getIdFromPos();
        final long endNodeId2 = endWp == null ? -1L : endWp.node2.getIdFromPos();
        OsmNode end1 = null;
        OsmNode end2 = null;
        OsmNodeNamed endPos = null;

        boolean sameSegmentSearch = false;
        final OsmNode start1 = nodesCache.getGraphNode(startWp.node1);
        final OsmNode start2 = nodesCache.getGraphNode(startWp.node2);
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

        final OsmPath startPath1 = getStartPath(start1, start2, startWp, endPos, sameSegmentSearch);
        final OsmPath startPath2 = getStartPath(start2, start1, startWp, endPos, sameSegmentSearch);

        // check for an INITIAL match with the cost-cutting-track
        if (costCuttingTrack != null) {
            final OsmPathElement pe1 = costCuttingTrack.getLink(startNodeId1, startNodeId2);
            if (pe1 != null) {
                logInfo("initialMatch pe1.cost=" + pe1.cost);
                int c = startPath1.cost - pe1.cost;
                if (c < 0) {
                    c = 0;
                }
                if (c < firstMatchCost) {
                    firstMatchCost = c;
                }
            }

            final OsmPathElement pe2 = costCuttingTrack.getLink(startNodeId2, startNodeId1);
            if (pe2 != null) {
                logInfo("initialMatch pe2.cost=" + pe2.cost);
                int c = startPath2.cost - pe2.cost;
                if (c < 0) {
                    c = 0;
                }
                if (c < firstMatchCost) {
                    firstMatchCost = c;
                }
            }

            if (firstMatchCost < 1000000000) {
                logInfo("firstMatchCost from initial match=" + firstMatchCost);
            }
        }

        synchronized (openSet) {
            openSet.clear();
            addToOpenset(startPath1);
            addToOpenset(startPath2);
        }
        final ArrayList<OsmPath> openBorderList = new ArrayList<>(4096);
        boolean memoryPanicMode = false;
        boolean needNonPanicProcessing = false;

        for (; ; ) {
            if (terminated) {
                throw new IllegalArgumentException("operation killed by thread-priority-watchdog after " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
            }

            if (maxRunningTime > 0) {
                final long timeout = (matchPath == null && fastPartialRecalc) ? maxRunningTime / 3 : maxRunningTime;
                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new IllegalArgumentException(operationName + " timeout after " + (timeout / 1000) + " seconds");
                }
            }

            synchronized (openSet) {

                final OsmPath path = openSet.popLowestKeyValue();
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
                    if (!memoryPanicMode && !nodesCache.nodesMap.isInMemoryBounds(openSet.getSize(), false)) {
                        final int nodesBefore = nodesCache.nodesMap.nodesCreated;
                        final int pathsBefore = openSet.getSize();

                        nodesCache.nodesMap.collectOutreachers();
                        for (; ; ) {
                            final OsmPath p3 = openSet.popLowestKeyValue();
                            if (p3 == null) {
                                break;
                            }
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

                if (nodeLimit > 0 && --nodeLimit == 0) { // check node-limit for target island search
                    return null;
                }

                nodesVisited++;
                linksProcessed++;

                final OsmLink currentLink = path.getLink();
                final OsmNode sourceNode = path.getSourceNode();
                final OsmNode currentNode = path.getTargetNode();

                if (currentLink.isLinkUnused()) {
                    path.unregisterUpTree(routingContext);
                    continue;
                }

                final long currentNodeId = currentNode.getIdFromPos();
                final long sourceNodeId = sourceNode.getIdFromPos();

                if (!path.didEnterDestinationArea()) {
                    islandNodePairs.addTempPair(sourceNodeId, currentNodeId);
                }

                if (path.treedepth != 1) {
                    if (path.treedepth == 0) { // hack: sameSegment Paths marked treedepth=0 to pass above check
                        path.treedepth = 1;
                    }

                    if ((sourceNodeId == endNodeId1 && currentNodeId == endNodeId2)
                            || (sourceNodeId == endNodeId2 && currentNodeId == endNodeId1)) {
                        // track found, compile
                        logInfo("found track at cost " + path.cost + " nodesVisited = " + nodesVisited);
                        final OsmTrack t = compileTrack(path);
                        t.showspeed = routingContext.showspeed;
                        t.showSpeedProfile = routingContext.showSpeedProfile;
                        return t;
                    }

                    // check for a match with the cost-cutting-track
                    if (costCuttingTrack != null) {
                        final OsmPathElement pe = costCuttingTrack.getLink(sourceNodeId, currentNodeId);
                        if (pe != null) {
                            // remember first match cost for fast termination of partial recalcs
                            int parentcost = path.originElement == null ? 0 : path.originElement.cost;

                            // hitting start-element of costCuttingTrack?
                            final int c = path.cost - parentcost - pe.cost;
                            if (c > 0) {
                                parentcost += c;
                            }

                            if (parentcost < firstMatchCost) {
                                firstMatchCost = parentcost;
                            }

                            final int costEstimate = path.cost
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

                final int keepPathAirdistance = path.airdistance;
                final OsmLinkHolder firstLinkHolder = currentLink.getFirstLinkHolder(sourceNode);
                for (OsmLinkHolder linkHolder = firstLinkHolder; linkHolder != null; linkHolder = linkHolder.getNextForLink()) {
                    ((OsmPath) linkHolder).airdistance = -1; // invalidate the entry in the open set;
                }

                if (path.treedepth > 1) {
                    final boolean isBidir = currentLink.isBidirectional();
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
                    final OsmNode nextNode = link.getTarget(currentNode);

                    if (!nodesCache.obtainNonHollowNode(nextNode)) {
                        continue; // border node?
                    }
                    if (nextNode.firstlink == null) {
                        continue; // don't care about dead ends
                    }
                    if (nextNode == sourceNode) {
                        continue; // border node?
                    }

                    final OsmPrePath prePath = routingContext.createPrePath(path, link);
                    if (prePath != null) {
                        prePath.next = routingContext.firstPrePath;
                        routingContext.firstPrePath = prePath;
                    }
                }

                for (OsmLink link = currentNode.firstlink; link != null; link = link.getNext(currentNode)) {
                    final OsmNode nextNode = link.getTarget(currentNode);

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
                        final int gidx = path.treedepth + 1;
                        if (gidx >= guideTrack.nodes.size()) {
                            continue;
                        }
                        final OsmPathElement guideNode = guideTrack.nodes.get(routingContext.inverseRouting ? guideTrack.nodes.size() - 1 - gidx : gidx);
                        final long nextId = nextNode.getIdFromPos();
                        if (nextId != guideNode.getIdFromPos()) {
                            // not along the guide-track, discard, but register for voice-hint processing
                            if (routingContext.turnInstructionMode > 0) {
                                final OsmPath detour = routingContext.createPath(path, link, refTrack, true);
                                if (detour.cost >= 0. && nextId != startNodeId1 && nextId != startNodeId2) {
                                    guideTrack.registerDetourForId(currentNode.getIdFromPos(), OsmPathElement.create(detour, false));
                                }
                            }
                            continue;
                        }
                    }

                    OsmPath bestPath = null;

                    boolean isFinalLink = false;
                    final long targetNodeId = nextNode.getIdFromPos();
                    if ((currentNodeId == endNodeId1 || currentNodeId == endNodeId2) && (targetNodeId == endNodeId1 || targetNodeId == endNodeId2)) {
                        isFinalLink = true;
                    }

                    for (OsmLinkHolder linkHolder = firstLinkHolder; linkHolder != null; linkHolder = linkHolder.getNextForLink()) {
                        final OsmPath otherPath = (OsmPath) linkHolder;
                        try {
                            if (isFinalLink) {
                                endPos.radius = 1.5; // 1.5 meters is the upper limit that will not change the unit-test result..
                                routingContext.setWaypoint(endPos, true);
                            }
                            final OsmPath testPath = routingContext.createPath(otherPath, link, refTrack, guideTrack != null);
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
                        final boolean trafficSim = endPos == null;

                        bestPath.airdistance = trafficSim ? keepPathAirdistance : (isFinalLink ? 0 : nextNode.calcDistance(endPos));

                        final boolean inRadius = boundary == null || boundary.isInBoundary(nextNode, bestPath.cost);

                        if (inRadius && (isFinalLink || bestPath.cost + bestPath.airdistance <= maxTotalCost + 100)) {
                            // add only if this may beat an existing path for that link
                            OsmLinkHolder dominator = link.getFirstLinkHolder(currentNode);
                            while (!trafficSim && dominator != null) {
                                final OsmPath dp = (OsmPath) dominator;
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

    private void addToOpenset(final OsmPath path) {
        if (path.cost >= 0) {
            openSet.add(path.cost + (int) (path.airdistance * airDistanceCostFactor), path);
            path.registerUpTree();
        }
    }

    private OsmTrack compileTrack(final OsmPath path) {
        OsmPathElement element = OsmPathElement.create(path, false);

        // for final track, cut endnode
        if (guideTrack != null) {
            element = element.origin;
        }

        final float totalTime = element.getTime();
        final float totalEnergy = element.getEnergy();

        final OsmTrack track = new OsmTrack();
        track.cost = path.cost;
        track.energy = (int) path.getTotalEnergy();

        int distance = 0;
        double ascend = 0;
        double ehb = 0.;

        short eleStart = Short.MIN_VALUE;
        short eleEnd = Short.MIN_VALUE;

        final double eleFactor = routingContext.inverseRouting ? -0.25 : 0.25;
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

            final OsmPathElement nextElement = element.origin;

            final short ele = element.getSElev();
            if (ele != Short.MIN_VALUE) {
                eleStart = ele;
            }
            if (eleEnd == Short.MIN_VALUE) {
                eleEnd = ele;
            }

            if (nextElement != null) {
                distance += element.calcDistance(nextElement);
                final short eleNext = nextElement.getSElev();
                if (eleNext != Short.MIN_VALUE) {
                    ehb = ehb + (ele - eleNext) * eleFactor;
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
        track.plainAscend = (int) ((eleEnd - eleStart) * eleFactor + 0.5);
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

    private OsmTrack mergeTrack(final OsmPathElement match, final OsmTrack oldTrack) {
        logInfo("**************** merging match=" + match.cost + " with oldTrack=" + oldTrack.cost);
        OsmPathElement element = match;
        final OsmTrack track = new OsmTrack();
        track.cost = oldTrack.cost;

        while (element != null) {
            track.addNode(element);
            element = element.origin;
        }
        long lastId = 0;
        final long id1 = match.getIdFromPos();
        final long id0 = match.origin == null ? 0 : match.origin.getIdFromPos();
        boolean appending = false;
        for (OsmPathElement n : oldTrack.nodes) {
            if (appending) {
                track.nodes.add(n);
            }

            final long id = n.getIdFromPos();
            if (id == id1 && lastId == id0) {
                appending = true;
            }
            lastId = id;
        }


        track.buildMap();
        return track;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getDistance() {
        return foundTrack.distance;
    }

    public String getTime() {
        return foundTrack.getFormattedTime2();
    }

    public OsmTrack getFoundTrack() {
        return foundTrack;
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

}
