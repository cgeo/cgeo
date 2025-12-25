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

package cgeo.geocaching.storage

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap

import androidx.annotation.NonNull

import java.io.IOException
import java.util.List
import java.util.Locale

/**
 * Base class for all content accessors
 */
abstract class AbstractContentAccessor {

    private final Context context

    AbstractContentAccessor(final Context context) {
        this.context = context
    }

    protected Context getContext() {
        return context
    }

    public abstract Boolean delete(Uri uri) throws IOException

    public abstract Uri rename(Uri uri, String newName) throws IOException

    public abstract Uri create(Folder folder, String name) throws IOException

    public abstract List<ContentStorage.FileInformation> list(Folder folder) throws IOException

    /**
     * If a file with given name exists in folder, it is returned. Otherwise null is returned
     */
    public abstract ContentStorage.FileInformation getFileInfo(Folder folder, String name) throws IOException

    /**
     * creates physical folder on device if it is not already there anyway
     */
    public abstract Boolean ensureFolder(Folder folder, Boolean needsWrite) throws IOException

    public abstract Uri getUriForFolder(Folder folder) throws IOException

    public abstract ContentStorage.FileInformation getFileInfo(Uri uri) throws IOException

    //some helpers for subclasses
    protected String getTypeForName(final String name) {
        val lastDot: Int = name.lastIndexOf('.')
        if (lastDot >= 0) {
            val extension: String = name.substring(lastDot + 1).toLowerCase(Locale.getDefault())
            val mime: String = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) {
                return mime
            }
        }
        return "application/octet-stream"
    }

}
