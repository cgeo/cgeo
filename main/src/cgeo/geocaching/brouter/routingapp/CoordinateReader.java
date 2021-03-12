package cgeo.geocaching.brouter.routingapp;

import cgeo.geocaching.brouter.core.OsmNodeNamed;
import cgeo.geocaching.brouter.core.RoutingHelper;
import cgeo.geocaching.utils.Log;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Read coordinates from a gpx-file
 */
public abstract class CoordinateReader {
    protected static String[] posnames
        = new String[]{"from", "via1", "via2", "via3", "via4", "via5", "via6", "via7", "via8", "via9", "to"};
    public List<OsmNodeNamed> waypoints;
    public List<OsmNodeNamed> nogopoints;
    public String basedir;
    public String rootdir;
    public String tracksdir;
    public List<OsmNodeNamed> allpoints;
    private boolean nogosOnly;
    private Map<String, Map<String, OsmNodeNamed>> allpointsMap;
    private HashMap<String, OsmNodeNamed> pointmap;

    public CoordinateReader(final String basedir) {
        this.basedir = basedir;
    }

    public static CoordinateReader obtainValidReader(final String basedir, final String segmentDir) throws Exception {
        return obtainValidReader(basedir, segmentDir, false);
    }

    public static CoordinateReader obtainValidReader(final String basedir, final String segmentDir, final boolean nogosOnly) throws Exception {
        CoordinateReader cor = null;
        final ArrayList<CoordinateReader> rl = new ArrayList<CoordinateReader>();

        Log.i("adding standard maptool-base: " + basedir);
        rl.add(new CoordinateReaderOsmAnd(basedir));
        rl.add(new CoordinateReaderLocus(basedir));
        rl.add(new CoordinateReaderOrux(basedir));

        // eventually add standard-sd
        final File standardbase = Environment.getExternalStorageDirectory();
        if (standardbase != null) {
            final String base2 = standardbase.getAbsolutePath();
            if (!base2.equals(basedir)) {
                Log.i("adding internal sd maptool-base: " + base2);
                rl.add(new CoordinateReaderOsmAnd(base2));
                rl.add(new CoordinateReaderLocus(base2));
                rl.add(new CoordinateReaderOrux(base2));
            }
        }

        // eventually add explicit directory
        final File additional = RoutingHelper.getAdditionalMaptoolDir(segmentDir);
        if (additional != null) {
            final String base3 = additional.getAbsolutePath();

            Log.i("adding maptool-base from storage-config: " + base3);

            rl.add(new CoordinateReaderOsmAnd(base3));
            rl.add(new CoordinateReaderOsmAnd(base3, true));
            rl.add(new CoordinateReaderLocus(base3));
            rl.add(new CoordinateReaderOrux(base3));
        }

        long tmax = 0;
        for (CoordinateReader r : rl) {
            final long t = r.getTimeStamp();

            if (t > tmax) {
                tmax = t;
                cor = r;
            }
        }
        if (cor == null) {
            cor = new CoordinateReaderNone();
        }
        cor.nogosOnly = nogosOnly;
        cor.readFromTo();
        return cor;
    }

    public abstract long getTimeStamp() throws Exception;

    public abstract int getTurnInstructionMode();

    public void readAllPoints() throws Exception {
        allpointsMap = new TreeMap<String, Map<String, OsmNodeNamed>>();
        readFromTo();
        allpoints = new ArrayList<OsmNodeNamed>();
        final Set<String> names = new HashSet<String>();
        for (String category : allpointsMap.keySet()) {
            final Map<String, OsmNodeNamed> cat = allpointsMap.get(category);
            if (cat.size() < 101) {
                for (OsmNodeNamed wp : cat.values()) {
                    if (names.add(wp.name)) {
                        allpoints.add(wp);
                    }
                }
            } else {
                final OsmNodeNamed nocatHint = new OsmNodeNamed();
                nocatHint.name = "<big category " + category + " supressed>";
                allpoints.add(nocatHint);
            }
        }
    }

    /*
     * read the from, to and via-positions from a gpx-file
     */
    public void readFromTo() throws Exception {
        pointmap = new HashMap<String, OsmNodeNamed>();
        waypoints = new ArrayList<OsmNodeNamed>();
        nogopoints = new ArrayList<OsmNodeNamed>();
        readPointmap();
        boolean fromToMissing = false;
        for (int i = 0; i < posnames.length; i++) {
            final String name = posnames[i];
            final OsmNodeNamed n = pointmap.get(name);
            if (n != null) {
                waypoints.add(n);
            } else {
                if ("from".equals(name)) {
                    fromToMissing = true;
                }
                if ("to".equals(name)) {
                    fromToMissing = true;
                }
            }
        }
        if (fromToMissing) {
            waypoints.clear();
        }
    }

    protected void checkAddPoint(final String categoryIn, final OsmNodeNamed n) {
        String category = categoryIn;
        if (allpointsMap != null) {
            if (category == null) {
                category = "";
            }
            Map<String, OsmNodeNamed> cat = allpointsMap.get(category);
            if (cat == null) {
                cat = new TreeMap<String, OsmNodeNamed>();
                allpointsMap.put(category, cat);
            }
            if (cat.size() < 101) {
                cat.put(n.name, n);
            }
            return;
        }

        boolean isKnown = false;
        for (int i = 0; i < posnames.length; i++) {
            if (posnames[i].equals(n.name)) {
                isKnown = true;
                break;
            }
        }

        if (isKnown) {
            if (pointmap.put(n.name, n) != null && !nogosOnly) {
                throw new IllegalArgumentException("multiple " + n.name + "-positions!");
            }
        } else if (n.name != null && n.name.startsWith("nogo")) {
            n.isNogo = true;
            n.nogoWeight = Double.NaN;
            nogopoints.add(n);
        }

    }

    protected abstract void readPointmap() throws Exception;
}
