/**
 * Container for routig configs
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.brouter.expressions.BExpressionContext;
import cgeo.geocaching.brouter.expressions.BExpressionContextNode;
import cgeo.geocaching.brouter.expressions.BExpressionContextWay;
import cgeo.geocaching.brouter.mapaccess.GeometryDecoder;
import cgeo.geocaching.brouter.mapaccess.OsmLink;
import cgeo.geocaching.brouter.mapaccess.OsmNode;
import cgeo.geocaching.brouter.util.CheapAngleMeter;
import cgeo.geocaching.brouter.util.CheapRulerHelper;

import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RoutingContext {
    public int alternativeIdx = 0;
    public String profileFilename;
    public long profileTimestamp;
    public Map<String, String> keyValues;
    public String rawTrackPath;
    public BExpressionContextWay expctxWay;
    public BExpressionContextNode expctxNode;
    public GeometryDecoder geometryDecoder = new GeometryDecoder();
    public int memoryclass = 64;
    public int downhillcostdiv;
    public int downhillcutoff;
    public int uphillcostdiv;
    public int uphillcutoff;
    public boolean carMode;
    public boolean bikeMode;
    public boolean footMode;
    public boolean considerTurnRestrictions;
    public boolean processUnusedTags;
    public double pass1coefficient;
    public double pass2coefficient;
    public int elevationpenaltybuffer;
    public int elevationmaxbuffer;
    public int elevationbufferreduce;
    public double cost1speed;
    public double additionalcostfactor;
    public double changetime;
    public double buffertime;
    public double waittimeadjustment;
    public double inittimeadjustment;
    public double starttimeoffset;
    public boolean transitonly;
    public double waypointCatchingRange;
    public List<OsmNodeNamed> poipoints;
    public List<OsmNodeNamed> nogopoints = null;
    private List<OsmNodeNamed> nogopointsAll = null; // full list not filtered for wayoints-in-nogos
    public Integer startDirection;
    public boolean startDirectionValid;
    public boolean forceUseStartDirection;
    public CheapAngleMeter anglemeter = new CheapAngleMeter();
    public double nogoCost = 0.;
    public boolean isEndpoint = false;
    public boolean shortestmatch = false;
    public double wayfraction;
    public int ilatshortest;
    public int ilonshortest;
    public boolean countTraffic;
    public boolean inverseDirection;
    public DataOutput trafficOutputStream;
    public double farTrafficWeight;
    public double nearTrafficWeight;
    public double farTrafficDecayLength;
    public double nearTrafficDecayLength;
    public double trafficDirectionFactor;
    public double trafficSourceExponent;
    public double trafficSourceMinDist;
    public boolean showspeed;
    public boolean showSpeedProfile;
    public boolean inverseRouting;
    public OsmPrePath firstPrePath;
    public int turnInstructionMode; // 0=none, 1=auto, 2=locus, 3=osmand, 4=comment-style, 5=gpsies-style
    public double turnInstructionCatchingRange;
    public boolean turnInstructionRoundabouts;
    // Speed computation model (for bikes)
    public double totalMass;
    public double maxSpeed;
    public double sCX;
    public double defaultCR;
    public double bikerPower;
    public OsmPathModel pm;
    private List<OsmNodeNamed> keepnogopoints = null;
    private OsmNodeNamed pendingEndpoint = null;

    public static void prepareNogoPoints(final List<OsmNodeNamed> nogos) {
        for (OsmNodeNamed nogo : nogos) {
            if (nogo instanceof OsmNogoPolygon) {
                continue;
            }
            String s = nogo.name;
            final int idx = s.indexOf(' ');
            if (idx > 0) {
                s = s.substring(0, idx);
            }
            int ir = 20; // default radius
            if (s.length() > 4) {
                try {
                    ir = Integer.parseInt(s.substring(4));
                } catch (Exception e) { /* ignore */ }
            }
            // Radius of the nogo point in meters
            nogo.radius = ir;
        }
    }

    /**
     * restore the full nogolist previously saved by cleanNogoList
     */
    public void restoreNogoList() {
        nogopoints = nogopointsAll;
    }

    /**
     * clean the nogolist (previoulsy saved by saveFullNogolist())
     * by removing nogos with waypoints within
     *
     * @return true if all wayoints are all in the same (full-weigth) nogo area (triggering bee-line-mode)
     */
    public void cleanNogoList(final List<OsmNode> waypoints) {
        nogopointsAll = nogopoints;
        if (nogopoints == null) {
            return;
        }
        final List<OsmNodeNamed> nogos = new ArrayList<>();
        for (OsmNodeNamed nogo : nogopoints) {
            boolean goodGuy = true;
            for (OsmNode wp : waypoints) {
                if (wp.calcDistance(nogo) < nogo.radius
                        && (!(nogo instanceof OsmNogoPolygon)
                        || (((OsmNogoPolygon) nogo).isClosed
                        ? ((OsmNogoPolygon) nogo).isWithin(wp.ilon, wp.ilat)
                        : ((OsmNogoPolygon) nogo).isOnPolyline(wp.ilon, wp.ilat)))) {
                    goodGuy = false;
                }
            }
            if (goodGuy) {
                nogos.add(nogo);
            }
        }
        nogopoints = nogos.isEmpty() ? null : nogos;
    }

    public boolean allInOneNogo(List<OsmNode> waypoints) {
        if (nogopoints == null) {
            return false;
        }
        boolean allInTotal = false;
        for (OsmNodeNamed nogo : nogopoints) {
            boolean allIn = Double.isNaN(nogo.nogoWeight);
            for (OsmNode wp : waypoints) {
                final int dist = wp.calcDistance(nogo);
                if (dist < nogo.radius
                        && (!(nogo instanceof OsmNogoPolygon)
                        || (((OsmNogoPolygon) nogo).isClosed
                        ? ((OsmNogoPolygon) nogo).isWithin(wp.ilon, wp.ilat)
                        : ((OsmNogoPolygon) nogo).isOnPolyline(wp.ilon, wp.ilat)))) {
                    continue;
                }
                allIn = false;
            }
            allInTotal |= allIn;
        }
        return allInTotal;
    }

    public void setAlternativeIdx(final int idx) {
        alternativeIdx = idx;
    }

    public int getAlternativeIdx(final int min, final int max) {
        return alternativeIdx < min ? min : (alternativeIdx > max ? max : alternativeIdx);
    }

    public String getProfileName() {
        String name = profileFilename == null ? "unknown" : profileFilename;
        if (name.endsWith(BRouterConstants.BROUTER_PROFILE_FILEEXTENSION)) {
            name = name.substring(0, profileFilename.length() - BRouterConstants.BROUTER_PROFILE_FILEEXTENSION.length());
        }
        return name;
    }

    private void setModel(final boolean useKinematicModel) {
        try {
            pm = useKinematicModel ? new KinematicModel() : new StdModel();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create path-model: " + e);
        }
        initModel();
    }

    public void initModel() {
        pm.init(expctxWay, expctxNode, keyValues);
    }

    public long getKeyValueChecksum() {
        long s = 0L;
        if (keyValues != null) {
            for (Map.Entry<String, String> e : keyValues.entrySet()) {
                s += e.getKey().hashCode() + e.getValue().hashCode();
            }
        }
        return s;
    }

    public void readGlobalConfig() {
        final BExpressionContext expctxGlobal = expctxWay; // just one of them...

        if (keyValues != null) {
            // add parameter to context
            for (Map.Entry<String, String> e : keyValues.entrySet()) {
                final float f = Float.parseFloat(e.getValue());
                expctxWay.setVariableValue(e.getKey(), f, false);
                expctxNode.setVariableValue(e.getKey(), f, false);
            }
        }

        setModel(expctxGlobal.useKinematicModel);

        downhillcostdiv = (int) expctxGlobal.getVariableValue("downhillcost", 0.f);
        downhillcutoff = (int) (expctxGlobal.getVariableValue("downhillcutoff", 0.f) * 10000);
        uphillcostdiv = (int) expctxGlobal.getVariableValue("uphillcost", 0.f);
        uphillcutoff = (int) (expctxGlobal.getVariableValue("uphillcutoff", 0.f) * 10000);
        if (downhillcostdiv != 0) {
            downhillcostdiv = 1000000 / downhillcostdiv;
        }
        if (uphillcostdiv != 0) {
            uphillcostdiv = 1000000 / uphillcostdiv;
        }
        carMode = 0.f != expctxGlobal.getVariableValue("validForCars", 0.f);
        bikeMode = 0.f != expctxGlobal.getVariableValue("validForBikes", 0.f);
        footMode = 0.f != expctxGlobal.getVariableValue("validForFoot", 0.f);

        waypointCatchingRange = expctxGlobal.getVariableValue("waypointCatchingRange", 250.f);

        // turn-restrictions used per default for car profiles
        considerTurnRestrictions = 0.f != expctxGlobal.getVariableValue("considerTurnRestrictions", 1.f);

        // process tags not used in the profile (to have them in the data-tab)
        processUnusedTags = 0.f != expctxGlobal.getVariableValue("processUnusedTags", 0.f);

        // forceSecondaryData = 0.f != expctxGlobal.getVariableValue( "forceSecondaryData", 0.f );
        pass1coefficient = expctxGlobal.getVariableValue("pass1coefficient", 1.5f);
        pass2coefficient = expctxGlobal.getVariableValue("pass2coefficient", 0.f);
        elevationpenaltybuffer = (int) (expctxGlobal.getVariableValue("elevationpenaltybuffer", 5.f) * 1000000);
        elevationmaxbuffer = (int) (expctxGlobal.getVariableValue("elevationmaxbuffer", 10.f) * 1000000);
        elevationbufferreduce = (int) (expctxGlobal.getVariableValue("elevationbufferreduce", 0.f) * 10000);

        cost1speed = expctxGlobal.getVariableValue("cost1speed", 22.f);
        additionalcostfactor = expctxGlobal.getVariableValue("additionalcostfactor", 1.5f);
        changetime = expctxGlobal.getVariableValue("changetime", 180.f);
        buffertime = expctxGlobal.getVariableValue("buffertime", 120.f);
        waittimeadjustment = expctxGlobal.getVariableValue("waittimeadjustment", 0.9f);
        inittimeadjustment = expctxGlobal.getVariableValue("inittimeadjustment", 0.2f);
        starttimeoffset = expctxGlobal.getVariableValue("starttimeoffset", 0.f);
        transitonly = expctxGlobal.getVariableValue("transitonly", 0.f) != 0.f;

        farTrafficWeight = expctxGlobal.getVariableValue("farTrafficWeight", 2.f);
        nearTrafficWeight = expctxGlobal.getVariableValue("nearTrafficWeight", 2.f);
        farTrafficDecayLength = expctxGlobal.getVariableValue("farTrafficDecayLength", 30000.f);
        nearTrafficDecayLength = expctxGlobal.getVariableValue("nearTrafficDecayLength", 3000.f);
        trafficDirectionFactor = expctxGlobal.getVariableValue("trafficDirectionFactor", 0.9f);
        trafficSourceExponent = expctxGlobal.getVariableValue("trafficSourceExponent", -0.7f);
        trafficSourceMinDist = expctxGlobal.getVariableValue("trafficSourceMinDist", 3000.f);

        showspeed = 0.f != expctxGlobal.getVariableValue("showspeed", 0.f);
        showSpeedProfile = 0.f != expctxGlobal.getVariableValue("showSpeedProfile", 0.f);
        inverseRouting = 0.f != expctxGlobal.getVariableValue("inverseRouting", 0.f);

        final int tiMode = (int) expctxGlobal.getVariableValue("turnInstructionMode", 0.f);
        if (tiMode != 1) { // automatic selection from coordinate source
            turnInstructionMode = tiMode;
        }
        turnInstructionCatchingRange = expctxGlobal.getVariableValue("turnInstructionCatchingRange", 40.f);
        turnInstructionRoundabouts = expctxGlobal.getVariableValue("turnInstructionRoundabouts", 1.f) != 0.f;

        // Speed computation model (for bikes)
        // Total mass (biker + bike + luggages or hiker), in kg
        totalMass = expctxGlobal.getVariableValue("totalMass", 90.f);
        // Max speed (before braking), in km/h in profile and m/s in code
        if (footMode) {
            maxSpeed = expctxGlobal.getVariableValue("maxSpeed", 6.f) / 3.6;
        } else {
            maxSpeed = expctxGlobal.getVariableValue("maxSpeed", 45.f) / 3.6;
        }
        // Equivalent surface for wind, S * C_x, F = -1/2 * S * C_x * v^2 = - S_C_x * v^2
        sCX = expctxGlobal.getVariableValue("S_C_x", 0.5f * 0.45f);
        // Default resistance of the road, F = - m * g * C_r (for good quality road)
        defaultCR = expctxGlobal.getVariableValue("C_r", 0.01f);
        // Constant power of the biker (in W)
        bikerPower = expctxGlobal.getVariableValue("bikerPower", 100.f);
    }

    public void cleanNogolist(final List<OsmNodeNamed> waypoints) {
        if (nogopoints == null) {
            return;
        }
        final List<OsmNodeNamed> nogos = new ArrayList<>();
        for (OsmNodeNamed nogo : nogopoints) {
            boolean goodGuy = true;
            for (OsmNodeNamed wp : waypoints) {
                if (wp.calcDistance(nogo) < nogo.radius
                        && (!(nogo instanceof OsmNogoPolygon)
                        || (((OsmNogoPolygon) nogo).isClosed
                        ? ((OsmNogoPolygon) nogo).isWithin(wp.ilon, wp.ilat)
                        : ((OsmNogoPolygon) nogo).isOnPolyline(wp.ilon, wp.ilat)))) {
                    goodGuy = false;
                    break;
                }
            }
            if (goodGuy) {
                nogos.add(nogo);
            }
        }
        nogopoints = nogos.isEmpty() ? null : nogos;
    }

    public long[] getNogoChecksums() {
        final long[] cs = new long[3];
        final int n = nogopoints == null ? 0 : nogopoints.size();
        for (int i = 0; i < n; i++) {
            final OsmNodeNamed nogo = nogopoints.get(i);
            cs[0] += nogo.ilon;
            cs[1] += nogo.ilat;
            // 10 is an arbitrary constant to get sub-integer precision in the checksum
            cs[2] += (long) (nogo.radius * 10.);
        }
        return cs;
    }

    public void setWaypoint(final OsmNodeNamed wp, final boolean endpoint) {
        setWaypoint(wp, null, endpoint);
    }

    public void setWaypoint(final OsmNodeNamed wp, final OsmNodeNamed pendingEndpoint, final boolean endpoint) {
        keepnogopoints = nogopoints;
        nogopoints = new ArrayList<>();
        nogopoints.add(wp);
        if (keepnogopoints != null) {
            nogopoints.addAll(keepnogopoints);
        }
        isEndpoint = endpoint;
        this.pendingEndpoint = pendingEndpoint;
    }

    public boolean checkPendingEndpoint() {
        if (pendingEndpoint != null) {
            isEndpoint = true;
            nogopoints.set(0, pendingEndpoint);
            pendingEndpoint = null;
            return true;
        }
        return false;
    }

    public void unsetWaypoint() {
        nogopoints = keepnogopoints;
        pendingEndpoint = null;
        isEndpoint = false;
    }

    public int calcDistance(int lon1, int lat1, int lon2, int lat2) {
        final double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales((lat1 + lat2) >> 1);
        final double dlon2m = lonlat2m[0];
        final double dlat2m = lonlat2m[1];
        double dx = (lon2 - lon1) * dlon2m;
        double dy = (lat2 - lat1) * dlat2m;
        double d = Math.sqrt(dy * dy + dx * dx);

        shortestmatch = false;

        if (nogopoints != null && !nogopoints.isEmpty() && d > 0.) {
            for (int ngidx = 0; ngidx < nogopoints.size(); ngidx++) {
                final OsmNodeNamed nogo = nogopoints.get(ngidx);
                final double x1 = (lon1 - nogo.ilon) * dlon2m;
                final double y1 = (lat1 - nogo.ilat) * dlat2m;
                final double x2 = (lon2 - nogo.ilon) * dlon2m;
                final double y2 = (lat2 - nogo.ilat) * dlat2m;
                final double r12 = x1 * x1 + y1 * y1;
                final double r22 = x2 * x2 + y2 * y2;
                double radius = Math.abs(r12 < r22 ? y1 * dx - x1 * dy : y2 * dx - x2 * dy) / d;

                if (radius < nogo.radius) { // 20m
                    double s1 = x1 * dx + y1 * dy;
                    double s2 = x2 * dx + y2 * dy;


                    if (s1 < 0.) {
                        s1 = -s1;
                        s2 = -s2;
                    }
                    if (s2 > 0.) {
                        radius = Math.sqrt(s1 < s2 ? r12 : r22);
                        if (radius > nogo.radius) {
                            continue;
                        }
                    }
                    if (nogo.isNogo) {
                        if (!(nogo instanceof OsmNogoPolygon)) {  // nogo is a circle
                            if (Double.isNaN(nogo.nogoWeight)) {
                                // default nogo behaviour (ignore completely)
                                nogoCost = -1;
                            } else {
                                // nogo weight, compute distance within the circle
                                nogoCost = nogo.distanceWithinRadius(lon1, lat1, lon2, lat2, d) * nogo.nogoWeight;
                            }
                        } else if (((OsmNogoPolygon) nogo).intersects(lon1, lat1, lon2, lat2)) {
                            // nogo is a polyline/polygon, we have to check there is indeed
                            // an intersection in this case (radius check is not enough).
                            if (Double.isNaN(nogo.nogoWeight)) {
                                // default nogo behaviour (ignore completely)
                                nogoCost = -1;
                            } else {
                                if (((OsmNogoPolygon) nogo).isClosed) {
                                    // compute distance within the polygon
                                    nogoCost = ((OsmNogoPolygon) nogo).distanceWithinPolygon(lon1, lat1, lon2, lat2) * nogo.nogoWeight;
                                } else {
                                    // for a polyline, just add a constant penalty
                                    nogoCost = nogo.nogoWeight;
                                }
                            }
                        }
                    } else {
                        shortestmatch = true;
                        nogo.radius = radius; // shortest distance to way
                        // calculate remaining distance
                        if (s2 < 0.) {
                            wayfraction = -s2 / (d * d);
                            final double xm = x2 - wayfraction * dx;
                            final double ym = y2 - wayfraction * dy;
                            ilonshortest = (int) (xm / dlon2m + nogo.ilon);
                            ilatshortest = (int) (ym / dlat2m + nogo.ilat);
                        } else if (s1 > s2) {
                            wayfraction = 0.;
                            ilonshortest = lon2;
                            ilatshortest = lat2;
                        } else {
                            wayfraction = 1.;
                            ilonshortest = lon1;
                            ilatshortest = lat1;
                        }

                        // here it gets nasty: there can be nogo-points in the list
                        // *after* the shortest distance point. In case of a shortest-match
                        // we use the reduced way segment for nogo-matching, in order not
                        // to cut our escape-way if we placed a nogo just in front of where we are
                        if (isEndpoint) {
                            wayfraction = 1. - wayfraction;
                            lon2 = ilonshortest;
                            lat2 = ilatshortest;
                        } else {
                            nogoCost = 0.;
                            lon1 = ilonshortest;
                            lat1 = ilatshortest;
                        }
                        dx = (lon2 - lon1) * dlon2m;
                        dy = (lat2 - lat1) * dlat2m;
                        d = Math.sqrt(dy * dy + dx * dx);
                    }
                }
            }
        }
        return (int) (d + 1.0);
    }

    public OsmPrePath createPrePath(final OsmPath origin, final OsmLink link) {
        final OsmPrePath p = pm.createPrePath();
        if (p != null) {
            p.init(origin, link, this);
        }
        return p;
    }

    public OsmPath createPath(final OsmLink link) {
        final OsmPath p = pm.createPath();
        p.init(link);
        return p;
    }

    public OsmPath createPath(final OsmPath origin, final OsmLink link, final OsmTrack refTrack, final boolean detailMode) {
        final OsmPath p = pm.createPath();
        p.init(origin, link, refTrack, detailMode, this);
        return p;
    }

}
