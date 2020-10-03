package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Central class to interact with locally stored PUBLIC folders.
 * Encapsulates the Android SAF framework.
 *
 * Note that methods of this class do not ask user for access permissions. This can only be
 * done in context of an Activity (using Intents) and is encapsulated in
 * {@link PublicLocalStorageActivityHelper}.
 *
 * Implementation reference(s):
 *  * Android Doku: https://developer.android.com/preview/privacy/storage
 *  * Android Doku on use cases: https://developer.android.com/training/data-storage/use-cases#handle-non-media-files
 *  * Introduction: https://www.androidcentral.com/what-scoped-storage
 *  * Helpers: https://stackoverflow.com/questions/34927748/android-5-0-documentfile-from-tree-uri
 */
public class PublicLocalStorage {

    private static final String EMPTY = "---";

    private final Context context;

    private static final PublicLocalStorage INSTANCE = new PublicLocalStorage();

    public static PublicLocalStorage get() {
        return INSTANCE;
    }

    private PublicLocalStorage() {
        this.context = CgeoApplication.getInstance().getApplicationContext();
    }

    /** checks if folder is available and can be used */
    public boolean checkFolderAvailability(final PublicLocalFolder folder) {
        final DocumentFile folderFile = getFolderFile(folder);
        return folderFile != null && folderFile.isDirectory();
    }

    /** Creates a new file in folder and returns its Uri */
    public Uri create(final PublicLocalFolder folder, final String name) {
        return create(folder, FileNameCreator.forName(name));
    }

    /** Creates a new file in folder and returns its Uri */
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

    /** Deletes the file represented by given Uri */
    public boolean delete(final Uri uri) {
        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), uri);
        } catch (FileNotFoundException fnfe) {
            return false;
        }
    }

    /** Lists all direct content of given folder */
    @NonNull
    public List<ImmutablePair<String, Uri>> list(final PublicLocalFolder folder) {
        final DocumentFile folderFile = getFolderFile(folder);
        if (folderFile == null) {
            return Collections.emptyList();
        }
        return CollectionStream.of(folderFile.listFiles()).map(df -> new ImmutablePair<>(df.getName(), df.getUri())).toList();
    }

    /** Opens an Uri for writing. Remember to close stream after usage! */
    public OutputStream openForWrite(final Uri uri) {
        try {
            return context.getContentResolver().openOutputStream(uri);
        } catch (IOException ioe) {
            Log.w("Problem opening uri for write '" + uri + "'", ioe);
        }
        return null;
    }

    /** Opens a file for reading. Remember to close stream after usage! */
    public InputStream openForRead(final PublicLocalFolder folder, final String name) {
        final DocumentFile folderFile = getFolderFile(folder);
        if (folderFile == null) {
            return null;
        }

        //try a shortcut so we don't have to create so many DocumentFiles...
        final Uri fileUri = Uri.withAppendedPath(folderFile.getUri(), name);
        return openForRead(fileUri);
    }

    /** Opens an Uri for reading. Remember to close stream after usage! */
    public InputStream openForRead(final Uri uri) {

        try {
            return this.context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException ioe) {
            Log.w("Trying to open a nonexisting file for read: '" + uri + "', error is ignored", ioe);
        }
        return null;
    }

    /** Write an (internal's) file content to external storage */
    public Uri writeFileToFolder(final PublicLocalFolder folder, final FileNameCreator nameCreator, final File file, final boolean deleteFileOnSuccess) {
        final FileNameCreator creator = nameCreator == null ? FileNameCreator.DEFAULT : nameCreator;
        InputStream in = null;
        OutputStream out = null;
        final Uri outputUri = create(folder, creator);
        try {
            in = new FileInputStream(file);
            out = openForWrite(outputUri);
            IOUtils.copy(in, out);
        } catch (IOException ie) {
            Log.w("Problems writing file '" + file + "' to '" + folder + "'", ie);
            delete(outputUri);
            return null;
        } finally {
            IOUtils.closeQuietly(in, out);
        }
        if (deleteFileOnSuccess && !file.delete()) {
            Log.w("Unable to delete file: " + file);
        }
        return outputUri;
    }

    /** Helper method, meant for usage in conjunction with {@link #writeFileToFolder(PublicLocalFolder, FileNameCreator, File, boolean)} */
    public File createTempFile() {
        try {
            final File outputDir = context.getCacheDir(); // context being the Activity pointer
            return File.createTempFile("cgeo_tempfile_", ".tmp", outputDir);
        } catch (IOException ie) {
            Log.e("Problems creating temporary file", ie);
        }
        return null;
    }

    /** Sets a new Uri for a PublicLocalFolder */
    public void setFolderUri(final PublicLocalFolder folder, final Uri uri) {
        folder.setUri(uri);
        releaseOutdatedUriPermissions();
    }

    /** toString()-method for {@link UriPermission} */
    public static String uriPermissionToString(final UriPermission uriPerm) {
        if (uriPerm == null) {
            return EMPTY;
        }
        return uriPerm.getUri() + " (" + Formatter.formatShortDateTime(uriPerm.getPersistedTime()) +
            "):" + (uriPerm.isReadPermission() ? "R" : "-") + (uriPerm.isWritePermission() ? "W" : "-");
    }

    /** returns system information for a given folder, mainly for display in log and/or SystemInformation */
    public String getFolderSystemInformation(final PublicLocalFolder folder) {
        try {
            final Uri treeUri = folder.getUri();
            if (treeUri == null) {
                return EMPTY;
            }
            final Uri docTreeUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
            if (docTreeUri == null) {
                return EMPTY;
            }

            final ParcelFileDescriptor pfd = this.context.getContentResolver().openFileDescriptor(docTreeUri, "r");
            if (pfd == null) {
                return EMPTY;
            }
            final StructStatVfs stats = Os.fstatvfs(pfd.getFileDescriptor());
            if (stats == null) {
                return EMPTY;
            }

            return "Free space: " + Formatter.formatBytes(stats.f_bavail * stats.f_bsize) + "" +
                ", files: " + stats.f_files;

        } catch (RuntimeException | ErrnoException | FileNotFoundException e) {
            Log.i("Exception while getting system information for " + folder, e);
            return "Ex(" + e.getClass().getName() + ")" + e.getMessage();
        }
    }

    /** get all currently persisted uri permissions. Meant for usage by {@link cgeo.geocaching.utils.SystemInformation} only */
    private List<UriPermission> getPersistedUriPermissions() {
        return context.getContentResolver().getPersistedUriPermissions();
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

        if (!checkAndAdjustFolderPermission(folder)) {
            return null;
        }

        final DocumentFile baseDir = DocumentFile.fromTreeUri(context, folder.getBaseUri());
        if (baseDir == null || !baseDir.isDirectory()) {
            reportProblem(R.string.publiclocalstorage_err_folder_not_a_directory_abort, folder.getBaseUri());
            return null;
        }
        if (folder.isUserDefinedLocation() || folder.getDefaultSubfolder() == null) {
            return baseDir;
        }
        final String subfolderName = toFolderName(folder.getDefaultSubfolder());
        DocumentFile child = findByName(baseDir, subfolderName);
        if (child == null) {
            //create a new subfolder
            child = baseDir.createDirectory(subfolderName);
            if (child == null) {
                reportProblem(R.string.publiclocalstorage_err_subfolder_not_a_directory_usebase, folder.getDefaultSubfolder(), folder.getBaseUri());
            }
        } else if (!child.isDirectory()) {
            child = null;
            reportProblem(R.string.publiclocalstorage_err_subfolder_not_a_directory_usebase, child.getName(), folder.getBaseUri());
        }

        return child == null ? baseDir : child;
    }

    private String toFolderName(final String name) {
        String folderName = name == null ? "default" : name.replaceAll("[^a-zA-Z0-9-_.]", "-").trim();
        if (StringUtils.isBlank(folderName)) {
            folderName = "default";
        }
        return folderName;
    }

    /** Checks folder uri permission. Tries to adjust folder uri if no permission given */
    private boolean checkAndAdjustFolderPermission(final PublicLocalFolder folder) {
        if (!checkUriPermissions(folder)) {
            //if this is a user-selected folder and base dir is ok we initiate a fallback to default folder
            if (!folder.isUserDefinedLocation() || !checkUriPermissions(PublicLocalFolder.BASE_DIR)) {
                return false;
            }

            final String folderUserdefined = folder.getUserDisplayableName();
            setFolderUri(folder, null);
            final String folderDefault = folder.getUserDisplayableName();
            if (!checkUriPermissions(folder)) {
                reportProblem(R.string.publiclocalstorage_err_folders_inaccessable_abort, folderUserdefined, folderDefault);
                return false;
            }
            reportProblem(R.string.publiclocalstorage_err_userdefinedfolder_inaccessable_usedefault, folderUserdefined, folderDefault);
        }
        return true;
    }

    private DocumentFile findByName(final DocumentFile dir, final String name) {
        if (dir == null || !dir.isDirectory()) {
            return null;
        }
        for (DocumentFile child : dir.listFiles()) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    private boolean checkUriPermissions(final PublicLocalFolder folder) {
        return checkUriPermissions(folder.getBaseUri(), folder.needsWrite());
    }

    private boolean checkUriPermissions(final Uri uri, final boolean checkWrite) {
        if (uri == null) {
            return false;
        }
        for (UriPermission up : getPersistedUriPermissions()) {
            if (up.getUri().equals(uri)) {
                final boolean hasAdequateRights = (up.isReadPermission()) && (!checkWrite || up.isWritePermission());
                if (!hasAdequateRights) {
                    return false;
                }
                final int flags = calculateUriPermissionFlags(true, checkWrite);
                context.getContentResolver().takePersistableUriPermission(uri, flags);
                return true;
            }
        }
        return false;
    }

    private int calculateUriPermissionFlags(final boolean read, final boolean write) {
        return (read ? Intent.FLAG_GRANT_READ_URI_PERMISSION : 0) | (write ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
    }

    private void releaseOutdatedUriPermissions() {
        final Set<String> usedUris = new HashSet<>();
        for (PublicLocalFolder folder : PublicLocalFolder.ALL) {
            if (folder.getBaseUri() != null) {
                usedUris.add(folder.getBaseUri().toString());
            }
        }

        for (UriPermission uriPerm : getPersistedUriPermissions()) {
            if (!usedUris.contains(uriPerm.getUri().toString())) {
                Log.iForce("Releasing UriPermission: " + uriPermissionToString(uriPerm));
                final int flags = calculateUriPermissionFlags(uriPerm.isReadPermission(), uriPerm.isWritePermission());
                context.getContentResolver().releasePersistableUriPermission(uriPerm.getUri(), flags);
            }
        }
    }


    private void reportProblem(@StringRes final int messageId, final Object ... params) {
        final String message = context.getString(messageId, params);
        Log.w("PublicLocalStorage: " + message);
        ActivityMixin.showToast(context, message);
    }




}
