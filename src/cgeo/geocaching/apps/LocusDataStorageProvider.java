package cgeo.geocaching.apps;

import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.DataCursor;
import menion.android.locus.addon.publiclib.utils.DataStorage;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.ArrayList;

/**
 * code provided by menion - developer of Locus
 */
public class LocusDataStorageProvider extends ContentProvider {

    @Override
    public Cursor query(Uri aUri, String[] aProjection, String aSelection,
            String[] aSelectionArgs, String aSortOrder) {

        DataCursor cursor = new DataCursor(new String[] { "data" });

        ArrayList<PointsData> data = DataStorage.getData();
        if (data == null || data.size() == 0)
            return cursor;

        for (int i = 0; i < data.size(); i++) {
            // get byte array
            Parcel par = Parcel.obtain();
            data.get(i).writeToParcel(par, 0);
            byte[] byteData = par.marshall();
            // add to row
            cursor.addRow(new Object[] { byteData });
        }
        // data filled to cursor, clear reference to prevent some memory issue
        DataStorage.clearData();
        // now finally return filled cursor
        return cursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

}
