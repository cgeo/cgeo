package cgeo.geocaching.brouter;

import cgeo.geocaching.brouter.core.OsmNodeNamed;
import cgeo.geocaching.brouter.core.OsmTrack;
import cgeo.geocaching.brouter.core.RoutingContext;
import cgeo.geocaching.brouter.core.RoutingEngine;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class BRouterWorker {
    private static final int OUTPUT_FORMAT_GPX = 0;
    private static final int OUTPUT_FORMAT_KML = 1;
    private static final int OUTPUT_FORMAT_JSON = 2;

    public String profileFilename;
    public String rawTrackPath;
    public List<OsmNodeNamed> waypoints;
    public List<OsmNodeNamed> nogoList;
    public List<OsmNodeNamed> nogoPolygonsList;

    // external code, do not refactor
    @SuppressWarnings({"PMD.ExcessiveMethodLength", "DuplicateBranchesInSwitch"})
    public String getTrackFromParams(final Bundle params) {
        long maxRunningTime = 60000;
        final String sMaxRunningTime = params.getString("maxRunningTime");
        if (sMaxRunningTime != null) {
            maxRunningTime = Integer.parseInt(sMaxRunningTime) * 1000;
        }

        final RoutingContext rc = new RoutingContext();
        rc.rawTrackPath = rawTrackPath;
        rc.profileFilename = profileFilename;

        final String tiFormat = params.getString("turnInstructionFormat");
        if (tiFormat != null) {
            if ("osmand".equalsIgnoreCase(tiFormat)) {
                rc.turnInstructionMode = 3;
            } else if ("locus".equalsIgnoreCase(tiFormat)) {
                rc.turnInstructionMode = 2;
            }
        }
        if (params.containsKey("timode")) {
            rc.turnInstructionMode = params.getInt("timode");
        }

        if (params.containsKey("direction")) {
            rc.startDirection = params.getInt("direction");
        }
        if (params.containsKey("alternativeidx")) {
            rc.alternativeIdx = params.getInt("alternativeidx");
        }

        if (nogoList != null) {
            RoutingContext.prepareNogoPoints(nogoList);
            if (rc.nogopoints == null) {
                rc.nogopoints = nogoList;
            } else {
                rc.nogopoints.addAll(nogoList);
            }
        }
        if (rc.nogopoints == null) {
            rc.nogopoints = nogoPolygonsList;
        } else if (nogoPolygonsList != null) {
            rc.nogopoints.addAll(nogoPolygonsList);
        }
        final List<OsmNodeNamed> poisList = readPoisList(params);
        rc.poipoints = poisList;

        if (params.containsKey("lats")) {
            waypoints = readPositions(params);
        }
        if (params.containsKey("lonlats")) {
            waypoints = readLonlats(params);
        }

        if (waypoints == null) {
            return "no pts ";
        }

        if (params.containsKey("extraParams")) {  // add user params
            final String extraParams = params.getString("extraParams");
            if (rc.keyValues == null) {
                rc.keyValues = new HashMap<String, String>();
            }
            final StringTokenizer tk = new StringTokenizer(extraParams, "?&");
            while (tk.hasMoreTokens()) {
                final String t = tk.nextToken();
                final StringTokenizer tk2 = new StringTokenizer(t, "=");
                if (tk2.hasMoreTokens()) {
                    final String key = tk2.nextToken();
                    if (tk2.hasMoreTokens()) {
                        final String value = tk2.nextToken();
                        rc.keyValues.put(key, value);
                    }
                }
            }
        }

        final RoutingEngine cr = new RoutingEngine(waypoints, rc);
        cr.doRun(maxRunningTime);

        // store new reference track if any
        // (can exist for timed-out search)
        if (cr.getFoundRawTrack() != null) {
            try {
                cr.getFoundRawTrack().writeBinary(rawTrackPath);
            } catch (Exception ignored) {
            }
        }

        if (cr.getErrorMessage() != null) {
            return cr.getErrorMessage();
        }

        final String format = params.getString("trackFormat");
        int writeFromat = OUTPUT_FORMAT_GPX;
        if (format != null) {
            if ("kml".equals(format)) {
                writeFromat = OUTPUT_FORMAT_KML;
            }
            if ("json".equals(format)) {
                writeFromat = OUTPUT_FORMAT_JSON;
            }
        }

        final OsmTrack track = cr.getFoundTrack();

        if (track != null) {
            if (params.containsKey("exportWaypoints")) {
                track.exportWaypoints = (params.getInt("exportWaypoints", 0) == 1);
            }
            switch (writeFromat) {
                case OUTPUT_FORMAT_GPX:
                    return track.formatAsGpx();
                case OUTPUT_FORMAT_KML:
                    return track.formatAsKml();
                case OUTPUT_FORMAT_JSON:
                    return track.formatAsGeoJson();
                default:
                    return track.formatAsGpx();
            }
        }
        return null;
    }

    private List<OsmNodeNamed> readPositions(final Bundle params) {
        final List<OsmNodeNamed> wplist = new ArrayList<>();

        final double[] lats = params.getDoubleArray("lats");
        final double[] lons = params.getDoubleArray("lons");

        if (lats == null || lats.length < 2 || lons == null || lons.length < 2) {
            throw new IllegalArgumentException("we need two lat/lon points at least!");
        }

        for (int i = 0; i < lats.length && i < lons.length; i++) {
            final OsmNodeNamed n = new OsmNodeNamed();
            n.name = "via" + i;
            n.ilon = (int) ((lons[i] + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((lats[i] + 90.) * 1000000. + 0.5);
            wplist.add(n);
        }
        wplist.get(0).name = "from";
        wplist.get(wplist.size() - 1).name = "to";

        return wplist;
    }

    private List<OsmNodeNamed> readLonlats(final Bundle params) {
        final List<OsmNodeNamed> wplist = new ArrayList<>();

        final String lonLats = params.getString("lonlats");
        if (lonLats == null) {
            throw new IllegalArgumentException("lonlats parameter not set");
        }

        final String[] coords = lonLats.split("\\|");
        if (coords.length < 2) {
            throw new IllegalArgumentException("we need two lat/lon points at least!");
        }

        for (int i = 0; i < coords.length; i++) {
            final String[] lonLat = coords[i].split(",");
            if (lonLat.length < 2) {
                throw new IllegalArgumentException("we need two lat/lon points at least!");
            }
            wplist.add(readPosition(lonLat[0], lonLat[1], "via" + i));
        }

        wplist.get(0).name = "from";
        wplist.get(wplist.size() - 1).name = "to";

        return wplist;
    }

    private static OsmNodeNamed readPosition(final String vlon, final String vlat, final String name) {
        if (vlon == null) {
            throw new IllegalArgumentException("lon " + name + " not found in input");
        }
        if (vlat == null) {
            throw new IllegalArgumentException("lat " + name + " not found in input");
        }

        return readPosition(Double.parseDouble(vlon), Double.parseDouble(vlat), name);
    }

    private static OsmNodeNamed readPosition(final double lon, final double lat, final String name) {
        final OsmNodeNamed n = new OsmNodeNamed();
        n.name = name;
        n.ilon = (int) ((lon + 180.) * 1000000. + 0.5);
        n.ilat = (int) ((lat + 90.) * 1000000. + 0.5);
        return n;
    }

    private List<OsmNodeNamed> readPoisList(Bundle params) {
        // lon,lat,name|...
        final String pois = params.getString("pois");
        if (pois == null) {
            return null;
        }

        final String[] lonLatNameList = pois.split("\\|");

        final List<OsmNodeNamed> poisList = new ArrayList<OsmNodeNamed>();
        for (String s : lonLatNameList) {
            final String[] lonLatName = s.split(",");

            final OsmNodeNamed n = new OsmNodeNamed();
            n.ilon = (int) ((Double.parseDouble(lonLatName[0]) + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((Double.parseDouble(lonLatName[1]) + 90.) * 1000000. + 0.5);
            n.name = lonLatName[2];
            poisList.add(n);
        }

        return poisList;
    }

}
