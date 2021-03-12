package cgeo.geocaching.brouter.routingapp;

import cgeo.geocaching.brouter.core.OsmNodeNamed;
import cgeo.geocaching.brouter.core.OsmNogoPolygon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Read coordinates from a gpx-file
 */
public class CoordinateReaderOsmAnd extends CoordinateReader {
    private final String osmandDir;

    public CoordinateReaderOsmAnd(final String basedir) {
        this(basedir, false);
    }

    public CoordinateReaderOsmAnd(final String basedir, final boolean shortPath) {
        super(basedir);
        if (shortPath) {
            osmandDir = basedir;
            tracksdir = "/tracks";
            rootdir = "";
        } else {
            osmandDir = basedir + "/osmand";
            tracksdir = "/osmand/tracks";
            rootdir = "/osmand";
        }
    }

    @Override
    public long getTimeStamp() throws Exception {
        final long t1 = new File(osmandDir + "/favourites_bak.gpx").lastModified();
        final long t2 = new File(osmandDir + "/favourites.gpx").lastModified();
        return t1 > t2 ? t1 : t2;
    }

    @Override
    public int getTurnInstructionMode() {
        return 3; // osmand style
    }

    /*
     * read the from and to position from a gpx-file
     * (with hardcoded name for now)
     */
    @Override
    public void readPointmap() throws Exception {
        try {
            readPointmapHelper(osmandDir + "/favourites_bak.gpx");
        } catch (Exception e) {
            readPointmapHelper(osmandDir + "/favourites.gpx");
        }
        try {
            readNogoLinesHelper(basedir + tracksdir);
        } catch (IOException ioe) {
        }
    }

    private void readPointmapHelper(final String filename) throws Exception {
        final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        OsmNodeNamed n = null;

        for (; ; ) {
            final String line = br.readLine();
            if (line == null) {
                break;
            }

            int idx0 = line.indexOf("<wpt lat=\"");
            int idx10 = line.indexOf("<name>");
            if (idx0 >= 0) {
                n = new OsmNodeNamed();
                idx0 += 10;
                final int idx1 = line.indexOf('"', idx0);
                n.ilat = (int) ((Double.parseDouble(line.substring(idx0, idx1)) + 90.) * 1000000. + 0.5);
                int idx2 = line.indexOf(" lon=\"");
                if (idx2 < 0) {
                    continue;
                }
                idx2 += 6;
                final int idx3 = line.indexOf('"', idx2);
                n.ilon = (int) ((Double.parseDouble(line.substring(idx2, idx3)) + 180.) * 1000000. + 0.5);
                continue;
            }
            if (n != null && idx10 >= 0) {
                idx10 += 6;
                final int idx11 = line.indexOf("</name>", idx10);
                if (idx11 >= 0) {
                    n.name = line.substring(idx10, idx11).trim();
                    checkAddPoint("(one-for-all)", n);
                }
            }
        }
        br.close();
    }

    private void readNogoLinesHelper(final String dirname) throws IOException {

        final File dir = new File(dirname);

        if (dir.exists() && dir.isDirectory()) {
            for (final File file : dir.listFiles()) {
                final String name = file.getName();
                if (name.startsWith("nogo") && name.endsWith(".gpx")) {
                    try {
                        readNogoLineHelper(file);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void readNogoLineHelper(final File file) throws Exception {
        final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        final XmlPullParser xpp = factory.newPullParser();

        xpp.setInput(new FileReader(file));
        final OsmNogoPolygon nogo = new OsmNogoPolygon(false);
        int eventType = xpp.getEventType();
        int numSeg = 0;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG: {
                    if (xpp.getName().equals("trkpt")) {
                        final String lon = xpp.getAttributeValue(null, "lon");
                        final String lat = xpp.getAttributeValue(null, "lat");
                        if (lon != null && lat != null) {
                            nogo.addVertex(
                                (int) ((Double.parseDouble(lon) + 180.) * 1000000. + 0.5),
                                (int) ((Double.parseDouble(lat) + 90.) * 1000000. + 0.5));
                        }
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    if (xpp.getName().equals("trkseg")) {
                        nogo.calcBoundingCircle();
                        final String name = file.getName();
                        nogo.name = name.substring(0, name.length() - 4);
                        if (numSeg > 0) {
                            nogo.name += Integer.toString(numSeg + 1);
                        }
                        numSeg++;
                        checkAddPoint("(one-for-all)", nogo);
                    }
                    break;
                }
                default:
                    break; // make Codacy happy
            }
            eventType = xpp.next();
        }
    }
}
