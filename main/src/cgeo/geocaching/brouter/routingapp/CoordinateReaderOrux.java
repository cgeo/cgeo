package cgeo.geocaching.brouter.routingapp;

import cgeo.geocaching.brouter.core.OsmNodeNamed;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

/**
 * Read coordinates from a gpx-file
 */
public class CoordinateReaderOrux extends CoordinateReader {
    public CoordinateReaderOrux(final String basedir) {
        super(basedir);
        tracksdir = "/oruxmaps/tracklogs";
        rootdir = "/oruxmaps";
    }

    @Override
    public long getTimeStamp() throws Exception {
        final long t1 = new File(basedir + "/oruxmaps/tracklogs/oruxmapstracks.db").lastModified();
        return t1;
    }

    @Override
    public int getTurnInstructionMode() {
        return 0; // none
    }

    /*
     * read the from and to position from a ggx-file
     * (with hardcoded name for now)
     */
    @Override
    public void readPointmap() throws Exception {
        readPointmapHelper(basedir + "/oruxmaps/tracklogs/oruxmapstracks.db");
    }

    private void readPointmapHelper(final String filename) throws Exception {
        final SQLiteDatabase myDataBase = SQLiteDatabase.openDatabase(filename, null, SQLiteDatabase.OPEN_READONLY);
        final Cursor c = myDataBase.rawQuery("SELECT poiname, poilon, poilat, poifolder FROM pois", null);
        while (c.moveToNext()) {
            final OsmNodeNamed n = new OsmNodeNamed();
            n.name = c.getString(0);
            n.ilon = (int) ((c.getDouble(1) + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((c.getDouble(2) + 90.) * 1000000. + 0.5);
            final String category = c.getString(3);
            checkAddPoint(category, n);
        }
        c.close();
        myDataBase.close();
    }
}
