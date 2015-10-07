package menion.android.locus;

import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.DataCursor;
import menion.android.locus.addon.publiclib.utils.DataStorage;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

/**
 * code provided by menion - developer of Locus
 */
public class LocusDataStorageProvider extends ContentProvider {

    @Override
    public Cursor query(Uri aUri, String[] aProjection, String aSelection,
            String[] aSelectionArgs, String aSortOrder) {

        final DataCursor cursor = new DataCursor(new String[] { "data" });

        for (final PointsData item : DataStorage.getData()) {
            final Parcel par = Parcel.obtain();
            item.writeToParcel(par, 0);
            // add byte array to row
            cursor.addRow(new Object[] { par.marshall() });
            par.recycle();
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
