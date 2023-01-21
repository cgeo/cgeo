package cgeo.geocaching.storage;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Base class for all content accessors
 */
abstract class AbstractContentAccessor {

    private final Context context;

    AbstractContentAccessor(@NonNull final Context context) {
        this.context = context;
    }

    protected Context getContext() {
        return context;
    }

    public abstract boolean delete(@NonNull Uri uri) throws IOException;

    public abstract Uri rename(@NonNull Uri uri, @NonNull String newName) throws IOException;

    public abstract Uri create(@NonNull Folder folder, @NonNull String name) throws IOException;

    public abstract List<ContentStorage.FileInformation> list(@NonNull Folder folder) throws IOException;

    /**
     * If a file with given name exists in folder, it is returned. Otherwise null is returned
     */
    public abstract ContentStorage.FileInformation getFileInfo(@NonNull Folder folder, String name) throws IOException;

    /**
     * creates physical folder on device if it is not already there anyway
     */
    public abstract boolean ensureFolder(@NonNull Folder folder, boolean needsWrite) throws IOException;

    public abstract Uri getUriForFolder(@NonNull Folder folder) throws IOException;

    public abstract ContentStorage.FileInformation getFileInfo(@NonNull Uri uri) throws IOException;

    //some helpers for subclasses
    protected String getTypeForName(@NonNull final String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase(Locale.getDefault());
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

}
