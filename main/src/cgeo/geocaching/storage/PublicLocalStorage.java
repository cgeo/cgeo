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
        public final FolderLocation dirLocation;
        public final String mimeType;

        public FileInformation(final String name, final Uri uri, final boolean isDirectory, final FolderLocation dirLocation, final String mimeType) {
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
    }

    /** checks if folder is available and can be used */
    public boolean checkFolderAvailability(final PublicLocalFolder folder) {
        final DocumentFile folderFile = getFolderFile(getAndAdjustFolderLocation(folder));
        return folderFile != null && folderFile.isDirectory();
    }

    /** Creates a new file in folder and returns its Uri */
    public Uri create(final PublicLocalFolder folder, final String name) {
        return create(folder, FileNameCreator.forName(name));
    }

    /** Creates a new file in folder and returns its Uri */
    public Uri create(final PublicLocalFolder folder, final FileNameCreator nameCreator) {
        return create("PublicLocalFolder " + folder.name(), getAndAdjustFolderLocation(folder), nameCreator);
    }

    /** Creates a new file in a folder location and returns its Uri */
    public Uri create(final String logContext, final FolderLocation folderLocation, final FileNameCreator nameCreator) {
        final FileNameCreator creator = nameCreator == null ? FileNameCreator.DEFAULT : nameCreator;
        try {
            final DocumentFile folderFile = getFolderFile(folderLocation);
            if (folderFile == null) {
                return null;
            }
            final String docName = creator.createName();
            final DocumentFile newFile = folderFile.createFile(creator.getMimeType(), docName);
            return newFile == null ? null : newFile.getUri();

        } catch (Exception ioe) {
            Log.w("[" + logContext + "]Problem creating new storage file in '" + folderLocation + "'", ioe);
        }
        return null;
    }

    /** Deletes the file represented by given Uri */
    public boolean delete(final Uri uri) {
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
    public List<FileInformation> list(final FolderLocation folderLocation) {
        final DocumentFile folderFile = getFolderFile(folderLocation);
        if (folderFile == null) {
            return Collections.emptyList();
        }

        return CollectionStream.of(folderFile.listFiles())
            .map(df -> new FileInformation(df.getName(), df.getUri(), df.isDirectory(),
                df.isDirectory() ? FolderLocation.fromSubfolder(folderLocation, df.getName()) : null, df.getType()))
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

    /** Opens a file for reading. Remember to close stream after usage! Returns null if Uri can't be opened for reading. */
    public InputStream openForRead(final PublicLocalFolder folder, final String name) {
        final DocumentFile folderFile = getFolderFile(getAndAdjustFolderLocation(folder));
        if (folderFile == null) {
            return null;
        }

        //try a shortcut so we don't have to create so many DocumentFiles...
        final Uri fileUri = Uri.withAppendedPath(folderFile.getUri(), name);
        return openForRead(fileUri);
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

    /** Sets a new User-defined Uri for a PublicLocalFolder. Must be a DocumentUri (retrieved via {@link Intent#ACTION_OPEN_DOCUMENT_TREE})! */
    public void setFolderUserDefinedUri(final PublicLocalFolder folder, final Uri documentUri) {
        folder.setUserDefinedLocation(documentUri);
        releaseOutdatedUriPermissions();
    }

    /** TODO: method is not yet tested! */
    public boolean copyTree(final FolderLocation source, final FolderLocation target, final boolean move) {
        if (!checkLocationPermissions(source, move) || !checkLocationPermissions(target, true)) {
            return false;
        }

        for (FileInformation fi : list(source)) {
            if (fi.isDirectory) {
                final boolean complete = copyTree(fi.dirLocation, FolderLocation.fromSubfolder(target, fi.name), move);
                if (!complete) {
                    return false;
                }
            } else {
                //copy file
                final FileNameCreator creator = FileNameCreator.forName(fi.name, fi.mimeType);
                InputStream in = null;
                OutputStream out = null;
                final Uri outputUri = create("COPY", target, creator);
                try {
                    in = openForRead(fi.uri);
                    out = openForWrite(outputUri);
                    IOUtils.copy(in, out);
                } catch (IOException ie) {
                    Log.w("Problems writing '" + fi + "' to '" + outputUri + "'", ie);
                    delete(outputUri);
                    return false;
                } finally {
                    IOUtils.closeQuietly(in, out);
                }
            }
            if (move) {
                delete(fi.uri);
            }
        }

        return true;
    }

    /** Tries to read and (optionally) write something to the given folder location, returns whether this was successful or not */
    public boolean performTestReadWriteToLocation(final String logContext, final FolderLocation folderLocation, final boolean testWrite) {

        Uri testDoc = null;
        if (testWrite) {
            testDoc = create(logContext + ":test read" + (testWrite ? "/write" : ""), folderLocation, FileNameCreator.DEFAULT);
            if (testDoc == null) {
                return false;
            }
        }

        //actually, on folder without write permission, we can not do much. Simply list the content...
        final List<FileInformation> files = list(folderLocation);

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
    public String getFolderLocationInformation(final FolderLocation folderLocation) {
        try {

            //get free space and number of files
            final ImmutablePair<Long, Long> freeSpaceAndNumberOfFiles;
            switch (folderLocation.getBaseType()) {
                case DOCUMENT:
                    freeSpaceAndNumberOfFiles = getFreeSpaceAndNoOfFilesForDocumentFolderLocation(folderLocation);
                    break;
                case FILE:
                default:
                    freeSpaceAndNumberOfFiles = new ImmutablePair<>(FileUtils.getFreeDiskSpace(new File(folderLocation.getUri().getPath())), -1l);
                    break;
            }
            return "Free Space: " + Formatter.formatBytes(freeSpaceAndNumberOfFiles.left) + ", No of files: " + freeSpaceAndNumberOfFiles.right;
        } catch (Exception e) {
            Log.i("Exception while getting system information for " + folderLocation, e);
            return "Ex(" + e.getClass().getName() + ")" + e.getMessage();
        }
   }


    /** Returns a pair of longs where left one is free space in bytes and right one is number of files */
    private ImmutablePair<Long, Long> getFreeSpaceAndNoOfFilesForDocumentFolderLocation(final FolderLocation folderLocation) throws Exception {

        final ImmutablePair<Long, Long> emptyResult = new ImmutablePair<>(-1l, -1l);

        final Uri treeUri = folderLocation.getUri();
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
    private DocumentFile getFolderFile(final FolderLocation folderLocation) {

        if (folderLocation == null) {
            return null;
        }

        final Uri baseUri = folderLocation.getBaseUri();
        final DocumentFile baseDir;
        switch (folderLocation.getBaseType()) {
            case DOCUMENT:
                baseDir = DocumentFile.fromTreeUri(context, baseUri);
                break;
            case FILE:
            default:
                baseDir = DocumentFile.fromFile(new File(baseUri.getPath()));
                break;
        }

       if (baseDir == null || !baseDir.isDirectory()) {
            reportProblem(R.string.publiclocalstorage_err_folder_not_a_directory_abort, baseUri);
            return null;
        }
       final List<String> subfolders = folderLocation.getSubdirsToBase();
       DocumentFile dir = baseDir;
       for (String subfolderName : subfolders) {
           DocumentFile child = findByName(dir, subfolderName);
           if (child == null) {
               //create a new subfolder
               child = baseDir.createDirectory(subfolderName);
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

       return dir;

    }
    /** Gets this folder's current location. Tries to adjust folder location if no permission given. May return null if no permission found */
    @Nullable
    private FolderLocation getAndAdjustFolderLocation(final PublicLocalFolder folder) {
        if (!checkFolderPermissions(folder)) {
            //if this is a user-selected folder and base dir is ok we initiate a fallback to default folder
            if (!folder.isUserDefinedLocation() || !checkLocationPermissions(folder.getDefaultLocation(), folder.needsWrite())) {
                return null;
            }

            final String folderUserdefined = folder.getUserDisplayableName();
            setFolderUserDefinedUri(folder, null);
            final String folderDefault = folder.getUserDisplayableName();
            if (!checkFolderPermissions(folder)) {
                reportProblem(R.string.publiclocalstorage_err_folders_inaccessable_abort, folderUserdefined, folderDefault);
                return null;
            }
            reportProblem(R.string.publiclocalstorage_err_userdefinedfolder_inaccessable_usedefault, folderUserdefined, folderDefault);
        }
        return folder.getLocation();
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

    private boolean checkFolderPermissions(final PublicLocalFolder folder) {
        return checkLocationPermissions(folder.getLocation(), folder.needsWrite());
    }

    private boolean checkLocationPermissions(final FolderLocation folderLocation, final boolean needsWrite) {
        switch (folderLocation.getBaseType()) {
            case DOCUMENT:
                if (folderLocation.getBaseUri() == null) {
                    //location does not need a permission
                    return true;
                }
                return checkUriPermissions(folderLocation.getBaseUri(), needsWrite);
            case FILE:
            default:
                final Uri fileUri = folderLocation.getUri();
                if (fileUri == null) {
                    return false;
                }
                final File dir = new File(fileUri.getPath());
                if (!dir.mkdirs()) {
                    Log.w("Could not create dir " + dir);
                }
                return dir.isDirectory() && dir.canRead() && (!needsWrite || dir.canWrite());
        }

    }

    private boolean checkUriPermissions(final Uri uri, final boolean checkWrite) {
        if (uri == null) {
            return false;
        }
        final int flags = calculateUriPermissionFlags(true, checkWrite);

        for (UriPermission up : getPersistedUriPermissions()) {
            if (up.getUri().equals(uri)) {
                final boolean hasAdequateRights = (up.isReadPermission()) && (!checkWrite || up.isWritePermission());
                if (!hasAdequateRights) {
                    return false;
                }
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
        for (PublicLocalFolder folder : PublicLocalFolder.values()) {
            if (folder.getLocation().getBaseUri() != null) {
                usedUris.add(folder.getLocation().getBaseUri().toString());
            }
        }

        for (UriPermission uriPerm : getPersistedUriPermissions()) {
            if (!usedUris.contains(uriPerm.getUri().toString())) {
                Log.iForce("Releasing UriPermission: " + UriUtils.uriPermissionToString(uriPerm));
                final int flags = calculateUriPermissionFlags(uriPerm.isReadPermission(), uriPerm.isWritePermission());
                context.getContentResolver().releasePersistableUriPermission(uriPerm.getUri(), flags);
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
