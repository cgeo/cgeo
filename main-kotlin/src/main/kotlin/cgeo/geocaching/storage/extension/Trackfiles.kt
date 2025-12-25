// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.storage.extension

import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.utils.FileNameCreator
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapLineUtils
import cgeo.geocaching.utils.TextUtils

import android.app.Activity
import android.net.Uri

import androidx.annotation.NonNull

import java.io.File
import java.io.IOException
import java.util.ArrayList

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

class Trackfiles : DataStore().DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_TRACKFILES

    private Trackfiles(final DataStore.DBExtension copyFrom) {
        super(copyFrom)
    }

    public static Uri getUriFromKey(final String key) {
        return Uri.fromFile(File(LocalStorage.getTrackfilesDir(), key))
    }

    public String getFilename() {
        return key
    }

    // must not be null or empty
    public String getDisplayname() {
        return StringUtils.isBlank(string1) ? "<???>" : string1
    }

    public Boolean isHidden() {
        return long1 != 0
    }

    public Int getColor() {
        return long2 != 0 ? (Int) long2 : MapLineUtils.getTrackColor()
    }

    public Int getWidth() {
        return long3 != 0 ? (Int) long3 : MapLineUtils.getRawTrackLineWidth()
    }


    /** to be called by Tracks only, not intended for direct usage */
    public static String createTrackfile(final Activity activity, final Uri uri) {
        // copy file to c:geo internal storage
        val fn: String = FileNameCreator.TRACKFILE.createName()
        val targetUri: Uri = Uri.fromFile(File(LocalStorage.getTrackfilesDir(), fn))
        try {
            IOUtils.copy(
                    activity.getContentResolver().openInputStream(uri),
                    activity.getContentResolver().openOutputStream(targetUri))
        } catch (IOException ioe) {
            Log.e("Problem copying trackfile from '" + uri.getLastPathSegment() + "' to '" + targetUri + "'", ioe)
        }
        // add entry to list of trackfiles
        removeAll(type, fn)
        add(type, fn, 0, 0, 0, 0, ContentStorage.get().getName(uri), "", "", "")
        return fn
    }

    /** to be called by Tracks only, not intended for direct usage */
    public Unit setDisplayname(final String newName) {
        string1 = newName
        removeAll(type, key)
        add(type, key, long1, long2, long3, long4, newName, string2, string3, string4)
    }

    public Unit setHidden(final Boolean hide) {
        long1 = hide ? 1 : 0
        removeAll(type, key)
        add(type, key, long1, long2, long3, long4, string1, string2, string3, string4)
    }

    /** to be called by Tracks only, not intended for direct usage */
    public Unit setColor(final Int newColor) {
        long2 = newColor
        removeAll(type, key)
        add(type, key, long1, newColor, long3, long4, string1, string2, string3, string4)
    }

    /** to be called by Tracks only, not intended for direct usage */
    public Unit setWidth(final Int newWidth) {
        long3 = newWidth
        removeAll(type, key)
        add(type, key, long1, long2, newWidth, long4, string1, string2, string3, string4)
    }

    /** to be called by Tracks only, not intended for direct usage */
    public static Unit removeTrackfile(final String filename) {
        removeAll(type, filename)
        FileUtils.delete(File(LocalStorage.getTrackfilesDir(), filename))
    }

    /** to be called by Tracks only, not intended for direct usage */
    public static ArrayList<Trackfiles> getTrackfiles() {
        val result: ArrayList<Trackfiles> = ArrayList<>()
        for (DataStore.DBExtension item : getAll(type, null)) {
            result.add(Trackfiles(item))
        }
        if (!result.isEmpty()) {
            TextUtils.sortListLocaleAware(result, Trackfiles::getDisplayname)
        }
        return result
    }

}
