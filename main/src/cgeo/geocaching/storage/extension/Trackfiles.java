package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;

public class Trackfiles extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_TRACKFILES;

    private Trackfiles(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    public static Uri getUriFromKey(final String key) {
        return Uri.fromFile(new File(LocalStorage.getTrackfilesDir(), key));
    }

    public String getFilename() {
        return key;
    }

    public String getDisplayname() {
        return string1;
    }

    public boolean isHidden() {
        return long1 != 0;
    }

    /** to be called by Tracks only, not intended for direct usage */
    public static String createTrackfile(final Activity activity, final Uri uri) {
        // copy file to c:geo internal storage
        final String fn = FileNameCreator.TRACKFILE.createName();
        final Uri targetUri = Uri.fromFile(new File(LocalStorage.getTrackfilesDir(), fn));
        try {
            IOUtils.copy(
                activity.getContentResolver().openInputStream(uri),
                activity.getContentResolver().openOutputStream(targetUri));
        } catch (IOException ioe) {
            Log.e("Problem copying trackfile from '" + uri.getLastPathSegment() + "' to '" + targetUri + "'", ioe);
        }
        // add entry to list of trackfiles
        removeAll(type, fn);
        add(type, fn, 0, 0, 0, 0, uri.getLastPathSegment(), "", "", "");
        return fn;
    }

    /** to be called by Tracks only, not intended for direct usage */
    public static void removeTrackfile(@NonNull final String filename) {
        removeAll(type, filename);
        FileUtils.delete(new File(LocalStorage.getTrackfilesDir(), filename));
    }

    /** to be called by Tracks only, not intended for direct usage */
    public static ArrayList<Trackfiles> getTrackfiles() {
        final ArrayList<Trackfiles> result = new ArrayList<>();
        for (DataStore.DBExtension item : getAll(type, null)) {
            result.add(new Trackfiles(item));
        }
        return result;
    }

    public static void hide(@NonNull final String filename, final boolean hide) {
        final DataStore.DBExtension itemRaw = load(type, filename);
        if (itemRaw != null) {
            final Trackfiles item = new Trackfiles(itemRaw);
            if (item.isHidden() != hide) {
                item.long1 = hide ? 1 : 0;
                removeAll(type, filename);
                add(type, item.key, item.long1, item.long2, item.long3, item.long4, item.string1, item.string2, item.string3, item.string4);
            }
        }
    }

}
