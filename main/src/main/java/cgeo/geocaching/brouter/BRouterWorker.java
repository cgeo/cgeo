package cgeo.geocaching.brouter;

import cgeo.geocaching.brouter.core.FormatGpx;
import cgeo.geocaching.brouter.core.FormatJson;
import cgeo.geocaching.brouter.core.FormatKml;
import cgeo.geocaching.brouter.core.OsmNodeNamed;
import cgeo.geocaching.brouter.core.OsmTrack;
import cgeo.geocaching.brouter.core.RoutingContext;
import cgeo.geocaching.brouter.core.RoutingEngine;
import cgeo.geocaching.brouter.core.RoutingParamCollector;

import android.os.Bundle;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BRouterWorker {
    private static final int OUTPUT_FORMAT_GPX = 0;
    private static final int OUTPUT_FORMAT_KML = 1;
    private static final int OUTPUT_FORMAT_JSON = 2;

    public String profileFilename;
    public String rawTrackPath;
    public List<OsmNodeNamed> waypoints;
    public List<OsmNodeNamed> nogoList;

    // external code, do not refactor
    @SuppressWarnings({"PMD.ExcessiveMethodLength"})
    public String getTrackFromParams(final Bundle params) {
        int engineMode = 0;
        if (params.containsKey("engineMode")) {
            engineMode = params.getInt("engineMode", 0);
        }

        final RoutingContext rc = new RoutingContext();
        rc.rawTrackPath = rawTrackPath;
        rc.profileFilename = profileFilename;

        final RoutingParamCollector routingParamCollector = new RoutingParamCollector();

        // parameter pre control
        if (params.containsKey("lonlats")) {
            waypoints = routingParamCollector.getWayPointList(params.getString("lonlats"));
            params.remove("lonlats");
        }
        if (params.containsKey("lats")) {
            final double[] lats = params.getDoubleArray("lats");
            final double[] lons = params.getDoubleArray("lons");
            waypoints = routingParamCollector.readPositions(lons, lats);
            params.remove("lons");
            params.remove("lats");
        }

        if (waypoints == null) {
            throw new IllegalArgumentException("no points!");
        }
        if (engineMode == 0) {
            if (waypoints.size() < 2) {
                throw new IllegalArgumentException("we need two lat/lon points at least!");
            }
        } else {
            if (waypoints.isEmpty()) {
                throw new IllegalArgumentException("we need two lat/lon points at least!");
            }
        }

        if (nogoList != null && !nogoList.isEmpty()) {
            // forward already read nogos from filesystem
            if (rc.nogopoints == null) {
                rc.nogopoints = nogoList;
            } else {
                rc.nogopoints.addAll(nogoList);
            }

        }

        final Map<String, String> theParams = new HashMap<>();
        for (String key : params.keySet()) {
            final Object value = params.get(key);
            if (value instanceof double[]) {
                String s = Arrays.toString(params.getDoubleArray(key));
                s = s.replace("[", "").replace("]", "");
                theParams.put(key, s);
            } else {
                theParams.put(key, value.toString());
            }
        }
        routingParamCollector.setParams(rc, waypoints, theParams);

        if (params.containsKey("extraParams")) {
            Map<String, String> profileparams = null;
            try {
                profileparams = routingParamCollector.getUrlParams(params.getString("extraParams"));
                routingParamCollector.setProfileParams(rc, profileparams);
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }

        long maxRunningTime = 60000;
        final String sMaxRunningTime = params.getString("maxRunningTime");
        if (sMaxRunningTime != null) {
            maxRunningTime = Integer.parseInt(sMaxRunningTime) * 1000L;
        }

        final RoutingEngine cr = new RoutingEngine(waypoints, rc, engineMode);
        cr.doRun(maxRunningTime);

        if (engineMode == RoutingEngine.BROUTER_ENGINEMODE_ROUTING) {
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

            int writeFromat = OUTPUT_FORMAT_GPX;
            if (rc.outputFormat != null) {
                if ("kml".equals(rc.outputFormat)) {
                    writeFromat = OUTPUT_FORMAT_KML;
                }
                if ("json".equals(rc.outputFormat)) {
                    writeFromat = OUTPUT_FORMAT_JSON;
                }
            }

            final OsmTrack track = cr.getFoundTrack();

            if (track != null) {
                track.exportWaypoints = rc.exportWaypoints;
                switch (writeFromat) {
                    case OUTPUT_FORMAT_KML:
                        return new FormatKml(rc).format(track);
                    case OUTPUT_FORMAT_JSON:
                        return new FormatJson(rc).format(track);
                    case OUTPUT_FORMAT_GPX:
                    default:
                        return new FormatGpx(rc).format(track);
                }
            }
        } else {    // get other infos
            if (cr.getErrorMessage() != null) {
                return cr.getErrorMessage();
            }
            return cr.getFoundInfo();
        }
        return null;
    }

}
