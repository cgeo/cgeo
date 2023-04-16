package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapLineUtils;

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

    public int getColor() {
        return long2 != 0 ? (int) long2 : MapLineUtils.getTrackColor();
    }

    public int getWidth() {
        return long3 != 0 ? (int) long3 : MapLineUtils.getRawTrackLineWidth();
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
        add(type, fn, 0, 0, 0, 0, ContentStorage.get().getName(uri), "", "", "");
        return fn;
    }

    /** to be called by Tracks only, not intended for direct usage */
    public void setDisplayname(@NonNull final String newName) {
        string1 = newName;
        removeAll(type, key);
        add(type, key, long1, long2, long3, long4, newName, string2, string3, string4);
    }

    public void setHidden(final boolean hide) {
        long1 = hide ? 1 : 0;
        removeAll(type, key);
        add(type, key, long1, long2, long3, long4, string1, string2, string3, string4);
    }

    /** to be called by Tracks only, not intended for direct usage */
    public void setColor(final int newColor) {
        long2 = newColor;
        removeAll(type, key);
        add(type, key, long1, newColor, long3, long4, string1, string2, string3, string4);
    }

    /** to be called by Tracks only, not intended for direct usage */
    public void setWidth(final int newWidth) {
        long3 = newWidth;
        removeAll(type, key);
        add(type, key, long1, long2, newWidth, long4, string1, string2, string3, string4);
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

}
