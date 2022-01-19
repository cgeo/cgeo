package cgeo.geocaching.brouter;

import cgeo.geocaching.brouter.core.OsmNodeNamed;
import cgeo.geocaching.brouter.core.OsmTrack;
import cgeo.geocaching.brouter.core.RoutingContext;
import cgeo.geocaching.brouter.core.RoutingEngine;
import cgeo.geocaching.utils.Log;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class BRouterWorker {
    // public String baseDir;
    public String profileFilename;
    public String rawTrackPath;
    public List<OsmNodeNamed> waypoints;
    public List<OsmNodeNamed> nogoList;

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

        if (params.containsKey("direction")) {
            rc.startDirection = params.getInt("direction");
        }

        readNogos(params); // add interface provided nogos
        RoutingContext.prepareNogoPoints(nogoList);
        rc.nogopoints = nogoList;

        waypoints = readPositions(params);

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
        final boolean writeKml = "kml".equals(format);

        final OsmTrack track = cr.getFoundTrack();
        return track == null ? null : writeKml ? track.formatAsKml() : track.formatAsGpx();
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

    private void readNogos(final Bundle params) {
        final double[] lats = params.getDoubleArray("nogoLats");
        final double[] lons = params.getDoubleArray("nogoLons");
        final double[] radi = params.getDoubleArray("nogoRadi");

        if (lats == null || lons == null || radi == null) {
            return;
        }

        for (int i = 0; i < lats.length && i < lons.length && i < radi.length; i++) {
            final OsmNodeNamed n = new OsmNodeNamed();
            n.name = "nogo" + (int) radi[i];
            n.ilon = (int) ((lons[i] + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((lats[i] + 90.) * 1000000. + 0.5);
            n.isNogo = true;
            n.nogoWeight = Double.NaN;
            Log.i("added interface provided nogo: " + n);
            nogoList.add(n);
        }
    }
}
