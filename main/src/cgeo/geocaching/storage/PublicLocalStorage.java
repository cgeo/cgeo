package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Central class to interact with locally stored PUBLIC folders.
 * Encapsulates the Android SAF framework.
 *
 * Note that methods of this class do not ask user for access permissions. This is done using
 * {@link PublicLocalStorageActivityHelper} in conjunction with c:geos Ativities.
 *
 * Implementation reference(s):
 * * helper methods: https://stackoverflow.com/questions/34927748/android-5-0-documentfile-from-tree-uri
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

    /**
     * Gets the Folder FIle; creates it if necessary and performs all possible error handlings
     *
     * This method is in many respects the core method of this class
     *
     * @param folder folder to get file for
     * @return file folder, or null if creation/retrieving was not at all possible
     */
    @Nullable
    private DocumentFile getFolderFile(final PublicLocalFolder folder) {

        if (!checkUriPermissions(folder)) {
            //if this is a user-selected folder and base dir is ok we initiate a fallback to default folder
            if (!folder.isUserDefinedLocation() || !checkUriPermissions(PublicLocalFolder.BASE_DIR)) {
                return null;
            }

            final String folderUserdefined = folder.getUserDisplayableName();
            folder.setUri(null);
            final String folderDefault = folder.getUserDisplayableName();
            if (!checkUriPermissions(folder)) {
                reportProblem(R.string.publiclocalstorage_err_folders_inaccessable_abort, folderUserdefined, folderDefault);
                return null;
            }
            reportProblem(R.string.publiclocalstorage_err_userdefinedfolder_inaccessable_usedefault, folderUserdefined, folderDefault);
        }

        final DocumentFile baseDir = DocumentFile.fromTreeUri(context, folder.getBaseUri());
        if (baseDir == null || !baseDir.isDirectory()) {
            reportProblem(R.string.publiclocalstorage_err_folder_not_a_directory_abort, folder.getBaseUri());
            return null;
        }
       if (folder.isUserDefinedLocation() || folder.getDefaultSubfolder() == null) {
            return baseDir;
        }
       String subfoldername = folder.getDefaultSubfolder();
       if (StringUtils.isBlank(subfoldername)) {
           subfoldername = "default";
       }
        for (DocumentFile child : baseDir.listFiles()) {
            if (subfoldername.equals(child.getName())) {
                if (!child.isDirectory()) {
                    reportProblem(R.string.publiclocalstorage_err_subfolder_not_a_directory_usebase, child.getName(), folder.getBaseUri());
                    return baseDir;
                }
                return child;
            }
        }
        final DocumentFile newChild = baseDir.createDirectory(subfoldername);
        if (newChild == null) {
            reportProblem(R.string.publiclocalstorage_err_subfolder_not_a_directory_usebase, folder.getDefaultSubfolder(), folder.getBaseUri());
            return baseDir;
        }
        return newChild;
    }

    private boolean checkUriPermissions(final PublicLocalFolder folder) {
        return checkUriPermissions(folder.getBaseUri(), folder.needsWrite());
    }

    private boolean checkUriPermissions(final Uri uri, final boolean checkWrite) {
        if (uri == null) {
            return false;
        }
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
        return writeTempFileToStorage(folder, FileNameCreator.forName(name), tempFile);
    }

    public boolean writeTempFileToStorage(final PublicLocalFolder folder, final FileNameCreator nameCreator, final File tempFile) {
        final FileNameCreator creator = nameCreator == null ? FileNameCreator.DEFAULT : nameCreator;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(tempFile);
            out = openForWrite(create(folder, creator));
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
    public Uri create(final PublicLocalFolder folder, final String name) {
        return create(folder, FileNameCreator.forName(name));
    }

    public Uri create(final PublicLocalFolder folder, final FileNameCreator nameCreator) {
        final FileNameCreator creator = nameCreator == null ? FileNameCreator.DEFAULT : nameCreator;
        try {
            final DocumentFile folderFile = getFolderFile(folder);
            if (folderFile == null) {
                return null;
            }
            final String docName = creator.createName();
            return DocumentsContract.createDocument(context.getContentResolver(), folderFile.getUri(), creator.getMimeType(), docName);

        } catch (IOException ioe) {
            Log.w("Problem creating new storage file in '" + folder + "'", ioe);
        }
        return null;
    }

    public OutputStream openForWrite(final Uri uri) {
        try {
            return context.getContentResolver().openOutputStream(uri);
        } catch (IOException ioe) {
            Log.w("Problem opening uri for write '" + uri + "'", ioe);
        }
        return null;
    }

    public boolean delete(final Uri uri) {
        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), uri);
        } catch (FileNotFoundException fnfe) {
            return false;
        }
    }

    @NonNull
    public List<ImmutablePair<String, Uri>> list(final PublicLocalFolder folder) {
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

    private void reportProblem(@StringRes final int messageId, final Object ... params) {
        final String message = context.getString(messageId, params);
        Log.w("PublicLocalStorage: " + message);
        ActivityMixin.showToast(context, message);
    }

    //some helper methods copied from stackoverflow: https://stackoverflow.com/questions/34927748/android-5-0-documentfile-from-tree-uri
    //might come in handy later



}
