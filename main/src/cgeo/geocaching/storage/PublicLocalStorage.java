package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.system.Os;
import android.system.StructStatVfs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
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

    private final Context context;
    private final Map<String, UriPermission> uriPermissionCache = new HashMap<>();

    private static final PublicLocalStorage INSTANCE = new PublicLocalStorage();

    public static PublicLocalStorage get() {
        return INSTANCE;
    }

    /** Class which is used to return basic File information */
    public static class FileInformation {
        public final String name;
        public final Uri uri;
        public final boolean isDirectory;
        /** if this is a directory: location of that directory. If this is not a directory: null */
        public final Folder dirLocation;
        public final String mimeType;

        public FileInformation(final String name, final Uri uri, final boolean isDirectory, final Folder dirLocation, final String mimeType) {
            this.name = name;
            this.uri = uri;
            this.dirLocation = dirLocation;
            this.isDirectory = isDirectory;
            this.mimeType = mimeType;
        }

        @Override
        public String toString() {
            return
                "name='" + name + '\'' +
                ", uri=" + uri +
                ", isDirectory=" + isDirectory +
                ", dirLocation=" + dirLocation +
                ", mimeType=" + mimeType;
        }
    }

    private PublicLocalStorage() {
        this.context = CgeoApplication.getInstance().getApplicationContext();
        refreshUriPermissionCache();
    }

    /** checks if folder is available and can be used */
    public boolean checkAvailability(final PublicLocalFolder folder) {
        final DocumentFile folderFile = getFolderFile(getAndAdjustFolderLocation(folder), folder.needsWrite());
        return folderFile != null && folderFile.isDirectory() && folderFile.canRead() && (!folder.needsWrite() || folderFile.canWrite());
    }

    public boolean checkAvailability(final Folder folder, final boolean needsWrite) {
        return checkAvailability(folder, needsWrite, false);
    }

    public boolean checkAvailability(final Folder folder, final boolean needsWrite, final boolean testReadWrite) {
        final DocumentFile folderFile = getFolderFile(folder, needsWrite);
        final boolean success = folderFile != null && folderFile.isDirectory() && folderFile.canRead() && (!needsWrite || folderFile.canWrite());

        if (success && testReadWrite) {
            return tryTestReadWriteToFolder(folder, needsWrite);
        }

        return success;
    }




    /** Creates a new file in folder and returns its Uri */
    public Uri create(final PublicLocalFolder folder, final String name) {
        return create(folder, FileNameCreator.forName(name));
    }

    /** Creates a new file in folder and returns its Uri */
    public Uri create(final PublicLocalFolder folder, final FileNameCreator nameCreator) {
        return create(getAndAdjustFolderLocation(folder), nameCreator);
    }

    /** Creates a new file in folder and returns its Uri */
    public Uri create(final Folder folder, final String name) {
        return create(folder, FileNameCreator.forName(name));
    }

    /** Creates a new file in a folder location and returns its Uri */
    public Uri create(final Folder folder, final FileNameCreator nameCreator) {
        final FileNameCreator creator = nameCreator == null ? FileNameCreator.DEFAULT : nameCreator;
        try {
            final DocumentFile folderFile = getFolderFile(folder, true);
            if (folderFile == null) {
                return null;
            }
            final String docName = createUniqueFilename(folderFile, creator.createName());
            //Do NOT pass a mimeType. It will then be selected based on the file suffix.
            final DocumentFile newFile = folderFile.createFile(null, docName);
            return newFile == null ? null : newFile.getUri();

        } catch (Exception ioe) {
            Log.w("Problem creating new storage file in '" + folder + "'", ioe);
        }
        return null;
    }

    /** creates physical folder on device if it is not already there anyway */
    public boolean ensureFolder(final Folder folder) {
        //in fact, currently this is just the same as checking for availability
        return checkAvailability(folder, true);
    }

    private String createUniqueFilename(@NonNull final DocumentFile dir, @NonNull final String docName) {

        //split in suffix and praefix
        final int suffIdx = docName.lastIndexOf(".");
        final String suffix = suffIdx >= 0 ? docName.substring(suffIdx) : "";
        final String praefix = suffIdx >= 0 ? docName.substring(0, suffIdx) : docName;

        String newPraefix = praefix;

        int idx = 1;
        while (dir.findFile(newPraefix + suffix) != null) {
            newPraefix = praefix + " (" + (idx++) + ")";
        }

        return newPraefix + suffix;
    }

    /** Deletes the file represented by given Uri */
    public boolean delete(final Uri uri) {
        if (uri == null) {
            return false;
        }
        try {
            if (UriUtils.isFileUri(uri)) {
                return DocumentFile.fromFile(new File(uri.getPath())).delete();
            } else {
                return DocumentsContract.deleteDocument(context.getContentResolver(), uri);
            }
        } catch (Exception e) {
            Log.w("Could not delete Uri '" + uri + "'", e);
            return false;
        }
    }

    /** Lists all direct content of given folder */
    @NonNull
    public List<FileInformation> list(final PublicLocalFolder folder) {
        return list(getAndAdjustFolderLocation(folder));
    }

    /** Lists all direct content of given folder location */
    @NonNull
    public List<FileInformation> list(final Folder folder) {
        final DocumentFile folderFile = getFolderFile(folder, false);
        if (folderFile == null) {
            return Collections.emptyList();
        }

        return CollectionStream.of(folderFile.listFiles())
            .map(df -> new FileInformation(df.getName(), df.getUri(), df.isDirectory(),
                df.isDirectory() ? Folder.fromFolder(folder, df.getName()) : null, df.getType()))
            .toList();
    }

    /** Opens an Uri for writing. Remember to close stream after usage! Returns null if Uri can't be opened for writing. */
    public OutputStream openForWrite(final Uri uri) {
        try {
            return context.getContentResolver().openOutputStream(uri);
        } catch (IOException ioe) {
            reportProblem(R.string.publiclocalstorage_err_writing_file_Io_problem, ioe, uri);
        } catch (SecurityException se) {
            reportProblem(R.string.publiclocalstorage_err_writing_file_no_permission, se, uri);
        }
        return null;
    }

    /** Opens an Uri for reading. Remember to close stream after usage! Returns null if Uri can't be opened for reading. */
    public InputStream openForRead(final Uri uri) {

        try {
            return this.context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException ioe) {
            reportProblem(R.string.publiclocalstorage_err_reading_file_does_not_exist, ioe, uri);
        } catch (SecurityException se) {
            reportProblem(R.string.publiclocalstorage_err_reading_file_no_permission, se, uri);
        }
        return null;
    }

    public Uri copy(final Uri source, final Folder target, final FileNameCreator newName, final boolean move) {

        boolean success = true;
        Exception failureEx = null;

        InputStream in = null;
        OutputStream out = null;
        Uri outputUri = null;
        try {
            outputUri = create(target, newName != null ? newName : FileNameCreator.forName(UriUtils.getFileName(source)));
            if (outputUri == null) {
                success = false;
                return null;
            }
            in = openForRead(source);
            out = openForWrite(outputUri);
            if (in == null || out == null) {
                success = false;
                return null;
            }
            IOUtils.copy(in, out);
        } catch (Exception ie) {
            success = false;
            failureEx = ie;
            delete(outputUri);
            return null;
        } finally {
            IOUtils.closeQuietly(in, out);
            if (!success) {
                Log.w("Problems writing file '" + source + "' to '" + target + "' (move=" + move + ")", failureEx);
            }
        }
        return outputUri;
    }



    /** Write an (internal's) file content to external storage */
    public Uri writeFileToFolder(final PublicLocalFolder folder, final FileNameCreator nameCreator, final File file, final boolean deleteFileOnSuccess) {
        return copy(Uri.fromFile(file), getAndAdjustFolderLocation(folder), nameCreator, deleteFileOnSuccess);
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

    /** Sets a new User-defined Uri for a PublicLocalFolder. Must be a DocumentUri (retrieved via {@link Intent#ACTION_OPEN_DOCUMENT_TREE})! */
    public void setFolderUserDefinedUri(final PublicLocalFolder folder, final Uri documentUri) {
        folder.setUserDefinedLocation(documentUri);
        releaseOutdatedUriPermissions();
    }

    /** Tries to read and (optionally) write something to the given folder location, returns whether this was successful or not */
    private boolean tryTestReadWriteToFolder(final Folder folder, final boolean testWrite) {

        Uri testDoc = null;
        if (testWrite) {
            testDoc = create(folder, FileNameCreator.DEFAULT);
            if (testDoc == null) {
                return false;
            }
        }

        //actually, on folder without write permission, we can not do much. Simply list the content...
        final List<FileInformation> files = list(folder);

        if (testWrite && files.size() < 1) {
            return false;
        }

        return testDoc == null || delete(testDoc);
    }

    /** returns system information for a given folder, mainly for display in log and/or SystemInformation */
    public String getFolderInformation(final PublicLocalFolder folder) {
        return getFolderLocationInformation(folder.getLocation());
    }

    /** returns system information for a given folder location, mainly for display in log and/or SystemInformation */
    public String getFolderLocationInformation(final Folder folder) {
        try {

            //get free space and number of files
            final ImmutablePair<Long, Long> freeSpaceAndNumberOfFiles;
            switch (folder.getBaseType()) {
                case DOCUMENT:
                    freeSpaceAndNumberOfFiles = getFreeSpaceAndNoOfFilesForDocumentFolderLocation(folder);
                    break;
                case FILE:
                default:
                    freeSpaceAndNumberOfFiles = new ImmutablePair<>(FileUtils.getFreeDiskSpace(new File(folder.getUri().getPath())), -1l);
                    break;
            }
            return "Free Space: " + Formatter.formatBytes(freeSpaceAndNumberOfFiles.left) + ", No of files: " + freeSpaceAndNumberOfFiles.right;
        } catch (Exception e) {
            Log.i("Exception while getting system information for " + folder, e);
            return "Ex(" + e.getClass().getName() + ")" + e.getMessage();
        }
   }


    /** Returns a pair of longs where left one is free space in bytes and right one is number of files */
    private ImmutablePair<Long, Long> getFreeSpaceAndNoOfFilesForDocumentFolderLocation(final Folder folder) throws Exception {

        final ImmutablePair<Long, Long> emptyResult = new ImmutablePair<>(-1l, -1l);

        final Uri treeUri = folder.getUri();
        if (treeUri == null) {
            return emptyResult;
        }
        final Uri docTreeUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
        if (docTreeUri == null) {
            return emptyResult;
        }

        final ParcelFileDescriptor pfd = this.context.getContentResolver().openFileDescriptor(docTreeUri, "r");
        if (pfd == null) {
            return emptyResult;
        }
        final StructStatVfs stats = Os.fstatvfs(pfd.getFileDescriptor());
        if (stats == null) {
            return emptyResult;
        }

        return new ImmutablePair<>(stats.f_bavail * stats.f_bsize, stats.f_files);
    }

    /** get all currently persisted document uri permissions. */
    private List<UriPermission> getPersistedUriPermissions() {
        return context.getContentResolver().getPersistedUriPermissions();
    }

    /**
     * Gets the Folder File; creates it if necessary and performs all possible error handlings.
     * This is used for locations of Type DOCUMENT and FILE
     *
     * This method is in many respects the core method of this class
     *
     * @param folder folder to get file for
     * @return file folder, or null if creation/retrieving was not at all possible
     */
    @Nullable
    private DocumentFile getFolderFile(final Folder folder, final boolean needsWrite) {

        if (folder == null) {
            return null;
        }

        final Uri baseUri = folder.getBaseUri();
        DocumentFile baseDir = null;
        switch (folder.getBaseType()) {
            case DOCUMENT:
                if (checkUriPermissions(baseUri, needsWrite)) {
                    baseDir = DocumentFile.fromTreeUri(context, baseUri);
                }
                break;
            case FILE:
            default:
                final File dirFile = new File(baseUri.getPath());
                if (!dirFile.exists() && !dirFile.mkdirs()) {
                    return null;
                }
                baseDir = DocumentFile.fromFile(dirFile);
                break;
        }

        if (baseDir == null || !baseDir.isDirectory() || !baseDir.canRead() || (needsWrite && !baseDir.canWrite())) {
            reportProblem(R.string.publiclocalstorage_err_folder_not_a_directory_abort, baseUri);
            return null;
        }

        return getSubdirFolderFile(folder, baseUri, baseDir);
    }

    private DocumentFile getSubdirFolderFile(final Folder folder, final Uri baseUri, final DocumentFile baseDir) {

        final DocumentFile cachedFile = folder.getCachedDocFile();
        if (cachedFile != null) {
            if (cachedFile.isDirectory()) {
                return cachedFile;
            }
            //something is wrong with cached value -> invalidate
            folder.setCachedDocFile(null);
        }

        final List<String> subfolders = folder.getSubdirsToBase();
        DocumentFile dir = baseDir;
        for (String subfolderName : subfolders) {
            DocumentFile child = dir.findFile(subfolderName);
            if (child == null) {
                //create a new subfolder
                child = dir.createDirectory(subfolderName);
                if (child == null) {
                    reportProblem(R.string.publiclocalstorage_err_subfolder_not_a_directory_usebase, subfolderName, baseUri);
                    break;
                }
            } else if (!child.isDirectory()) {
                reportProblem(R.string.publiclocalstorage_err_subfolder_not_a_directory_usebase, subfolderName, baseUri);
                break;
            }
            dir = child;
        }

        folder.setCachedDocFile(dir);
        return dir;
    }

    /** Gets this folder's current location. Tries to adjust folder location if no permission given. May return null if no permission found */
    @Nullable
    private Folder getAndAdjustFolderLocation(final PublicLocalFolder folder) {
        if (!checkAvailability(folder.getLocation(), folder.needsWrite())) {
            //if this is a user-selected folder and base dir is ok we initiate a fallback to default folder
            if (!folder.isUserDefinedLocation() || !checkAvailability(folder.getDefaultLocation(), folder.needsWrite())) {
                return null;
            }

            final String folderUserdefined = folder.toUserDisplayableString();
            setFolderUserDefinedUri(folder, null);
            final String folderDefault = folder.toUserDisplayableString();
            if (!checkAvailability(folder.getLocation(), folder.needsWrite())) {
                reportProblem(R.string.publiclocalstorage_err_folders_inaccessable_abort, folderUserdefined, folderDefault);
                return null;
            }
            reportProblem(R.string.publiclocalstorage_err_userdefinedfolder_inaccessable_usedefault, folderUserdefined, folderDefault);
        }
        return folder.getLocation();
    }

    private boolean checkUriPermissions(final Uri uri, final boolean checkWrite) {
        if (uri == null) {
            return false;
        }

        final UriPermission perm = this.uriPermissionCache.get(UriUtils.toCompareString(uri));
        if (perm == null) {
            return false;
        }

        return perm.isReadPermission() && (!checkWrite || perm.isWritePermission());
    }

    private int calculateUriPermissionFlags(final boolean read, final boolean write) {
        return (read ? Intent.FLAG_GRANT_READ_URI_PERMISSION : 0) | (write ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
    }

    private void releaseOutdatedUriPermissions() {
        final Set<String> usedUris = new HashSet<>();
        for (PublicLocalFolder folder : PublicLocalFolder.values()) {
            if (folder.getLocation().getBaseUri() != null) {
                usedUris.add(UriUtils.toCompareString(folder.getLocation().getBaseUri()));
            }
        }

        for (UriPermission uriPerm : getPersistedUriPermissions()) {
            if (!usedUris.contains(UriUtils.toCompareString(uriPerm.getUri()))) {
                Log.iForce("Releasing UriPermission: " + UriUtils.uriPermissionToString(uriPerm));
                final int flags = calculateUriPermissionFlags(uriPerm.isReadPermission(), uriPerm.isWritePermission());
                context.getContentResolver().releasePersistableUriPermission(uriPerm.getUri(), flags);
            }
        }
        refreshUriPermissionCache();
    }

    protected void refreshUriPermissionCache() {
        this.uriPermissionCache.clear();
        for (UriPermission uriPerm : context.getContentResolver().getPersistedUriPermissions()) {
            final String key = UriUtils.toCompareString(uriPerm.getUri());
            final boolean containsKey = this.uriPermissionCache.containsKey(key);
            if (!containsKey || uriPerm.isWritePermission()) {
                this.uriPermissionCache.put(key, uriPerm);
            }
        }
    }


    private void reportProblem(@StringRes final int messageId, final Object ... params) {
        reportProblem(messageId, null, params);
    }

    private void reportProblem(@StringRes final int messageId, final Exception ex, final Object ... params) {
        final String message = context.getString(messageId, params);
        Log.w("PublicLocalStorage: " + message, ex);
        ActivityMixin.showToast(context, message);
    }

}
