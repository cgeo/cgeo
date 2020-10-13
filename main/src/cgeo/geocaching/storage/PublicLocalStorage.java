package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Central class to interact with locally stored PUBLIC folders.
 * Encapsulates the Android SAF framework.
 *
 * Note that methods of this class do not ask user for access permissions. This is done using
 * {@link PublicLocalStorageActivityHelper} in conjunction with c:geos Ativities.
 */
public class PublicLocalStorage {

    private final Context context;

    private static final PublicLocalStorage INSTANCE = new PublicLocalStorage();

    public static PublicLocalStorage get() {
        return INSTANCE;
    }

    private PublicLocalStorage() {
        this.context = CgeoApplication.getInstance().getApplicationContext();
    }

    private DocumentFile getFolderFile(final PublicLocalFolder folder) {
        final Uri baseUri = folder.getBaseUri();
        if (baseUri == null || !checkUriPermissions(baseUri, folder.needsWrite())) {
            return null;
        }

        final DocumentFile baseDir = DocumentFile.fromTreeUri(context, baseUri);
        if (baseDir == null || !baseDir.isDirectory()) {
            return null;
        }
        if (folder.getFolderName() == null) {
            return baseDir;
        }
        for (DocumentFile child : baseDir.listFiles()) {
            if (folder.getFolderName().equals(child.getName())) {
                if (!child.isDirectory()) {
                    return null;
                }
                return child;
            }
        }
        return baseDir.createDirectory(folder.getFolderName());
    }

    private boolean checkUriPermissions(final Uri uri, final boolean checkWrite) {
        for (UriPermission up : context.getContentResolver().getPersistedUriPermissions()) {
            if (up.getUri().equals(uri)) {
                final boolean hasAdequateRights = (up.isReadPermission()) && (!checkWrite || up.isWritePermission());
                if (!hasAdequateRights) {
                    return false;
                }
                final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | (checkWrite ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
                context.getContentResolver().takePersistableUriPermission(uri, flags);
                return true;
            }
        }
        return false;
    }

    public File createTempFile() {
        try {
            final File outputDir = context.getCacheDir(); // context being the Activity pointer
            return File.createTempFile("cgeo_tempfile_", ".tmp", outputDir);
        } catch (IOException ie) {
            Log.e("Problems creating temporary file", ie);
        }
        return null;
    }

    public boolean checkAvailability(final PublicLocalFolder folder) {
        final DocumentFile folderFile = getFolderFile(folder);
        return folderFile != null && folderFile.isDirectory();
    }

    /** Write something to external storage */
    public boolean writeTempFileToStorage(final PublicLocalFolder folder, final String name, final File tempFile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(tempFile);
            out = createNewForWrite(folder, name);
            IOUtils.copy(in, out);
        } catch (IOException ie) {
            Log.w("Problems writing file '" + tempFile + "' to '" + folder + "'", ie);
            return false;
        } finally {
            IOUtils.closeQuietly(in, out);
        }
        return tempFile.delete();
    }

    /** Creates a new storage location and returns its OutputStream. Remember to close it after usage! */
    public OutputStream createNewForWrite(final PublicLocalFolder folder, final String name) {
        try {
            final DocumentFile folderFile = getFolderFile(folder);
            if (folderFile == null) {
                return null;
            }
            final String docName = name == null ? folder.createNewFilename() : name;
            final Uri newDoc = DocumentsContract.createDocument(context.getContentResolver(), folderFile.getUri(), folder.getDefaultMimeType(), docName);
            return context.getContentResolver().openOutputStream(newDoc);
        } catch (IOException ioe) {
            Log.w("Problem creating new storage file in '" + folder + "'", ioe);
        }
        return null;
    }

    @NonNull
    public List<ImmutablePair<String, Uri>> listDocuments(final PublicLocalFolder folder) {
        final DocumentFile folderFile = getFolderFile(folder);
        if (folderFile == null) {
            return Collections.emptyList();
        }
        return CollectionStream.of(folderFile.listFiles()).map(df -> new ImmutablePair<>(df.getName(), df.getUri())).toList();
    }

//  Alternative implementation for listDocuments, which might be more efficient -> safe for later
//    private List<ImmutablePair<String, Uri>> listDocuments(final Uri folderUri) {
//        final List<ImmutablePair<String, Uri>> result = new ArrayList<>();
//
//        Cursor c = null;
//        try {
//            c = context.getContentResolver().query(folderUri,
//                new String[] {
//                    DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME},
//                null, null, null);
//            while (c.moveToNext()) {
//                final String documentId = c.getString(0);
//                final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri,
//                    documentId);
//                final String name = c.getString(1);
//                result.add(new ImmutablePair<>(name, documentUri));
//            }
//        } finally {
//            IOUtils.closeQuietly(c);
//        }
//        return result;
//    }

    public InputStream openForRead(final PublicLocalFolder folder, final String name) {
        final DocumentFile folderFile = getFolderFile(folder);
        if (folderFile == null) {
            return null;
        }

        //try a shortcut so we don't have to create so many DocumentFiles...
        final Uri fileUri = Uri.withAppendedPath(folderFile.getUri(), name);
        return openForRead(fileUri);
    }

    public InputStream openForRead(final Uri uri) {

        try {
            return this.context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException ioe) {
            Log.w("Trying to open a nonexisting file for read: '" + uri + "', error is ignored", ioe);
        }
        return null;
    }

    public boolean checkUriForRead(final Uri uri) {
        if (uri == null) {
            return false;
        }
        final DocumentFile df = DocumentFile.fromSingleUri(this.context, uri);
        return df != null && df.exists() && df.canRead();
    }

}
