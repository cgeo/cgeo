package cgeo.geocaching.brouter.routingapp;

import cgeo.geocaching.brouter.core.OsmNodeNamed;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

/**
 * Read coordinates from a gpx-file
 */
public class CoordinateReaderLocus extends CoordinateReader {
    public CoordinateReaderLocus(final String basedir) {
        super(basedir);
        tracksdir = "/Locus/mapItems";
        rootdir = "/Locus";
    }

    @Override
    public long getTimeStamp() throws Exception {
        final long t1 = new File(basedir + "/Locus/data/database/waypoints.db").lastModified();
        return t1;
    }

    @Override
    public int getTurnInstructionMode() {
        return 2; // locus style
    }

    /*
     * read the from and to position from a ggx-file
     * (with hardcoded name for now)
     */
    @Override
    public void readPointmap() throws Exception {
        readPointmapHelper(basedir + "/Locus/data/database/waypoints.db");
    }

    private void readPointmapHelper(final String filename) throws Exception {
        final SQLiteDatabase myDataBase = SQLiteDatabase.openDatabase(filename, null, SQLiteDatabase.OPEN_READONLY);

        final Cursor c = myDataBase.rawQuery("SELECT c.name, w.name, w.longitude, w.latitude FROM waypoints w, categories c where w.parent_id = c._id", null);
        while (c.moveToNext()) {
            final OsmNodeNamed n = new OsmNodeNamed();
            final String category = c.getString(0);
            n.name = c.getString(1);
            n.ilon = (int) ((c.getDouble(2) + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((c.getDouble(3) + 90.) * 1000000. + 0.5);
            checkAddPoint(category, n);
        }
        c.close();
        myDataBase.close();
    }
}
