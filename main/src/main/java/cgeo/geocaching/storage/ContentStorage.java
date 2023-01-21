package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;
import static cgeo.geocaching.storage.Folder.CGEO_PRIVATE_FILES;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import static org.apache.commons.io.IOUtils.closeQuietly;


/**
 * Central class to interact with locally stored Folders.
 *
 * Encapsulates and unifies access to the Android SAF framework as well as the File framework
 * (and maybe later also the MediaStore API?)
 *
 * Note that methods of this class do not and cannot ask user for access permissions when dealing with SAF This can only be
 * done in context of an Activity (using Intents) and is encapsulated in
 * {@link ContentStorageActivityHelper}.
 * When dealing with {@link PersistableFolder}s, methods of this class will try to fall back to default
 * accessible folders when a folder is not accessible.
 *
 * Implementation reference(s) with regards to SAF:
 * * Android Doku: https://developer.android.com/preview/privacy/storage
 * * Android Doku on use cases: https://developer.android.com/training/data-storage/use-cases#handle-non-media-files
 * * Introduction: https://www.androidcentral.com/what-scoped-storage
 * * Helpers: https://stackoverflow.com/questions/34927748/android-5-0-documentfile-from-tree-uri
 */
public class ContentStorage {

    private final Context context;
    private final DocumentContentAccessor documentAccessor;
    private final FileContentAccessor fileAccessor;
    private final ThreadLocal<Boolean> reportRunningFlag = new ThreadLocal<>();

    private static final ContentStorage INSTANCE = new ContentStorage();

    public static ContentStorage get() {
        return INSTANCE;
    }

    /**
     * Class which is used to return basic File information
     */
    public static class FileInformation {
        public final String name;
        public final Uri uri;
        public final boolean isDirectory;
        /**
         * if this is a directory: location of that directory. If this is not a directory: null
         */
        public final Folder dirLocation;
        public final String mimeType;
        public final long size;
        public final long lastModified;

        public FileInformation(final String name, final Uri uri, final boolean isDirectory, final Folder dirLocation, final String mimeType,
                               final long size, final long lastModified) {
            this.name = name;
            this.uri = uri;
            this.dirLocation = dirLocation;
            this.isDirectory = isDirectory;
            this.mimeType = mimeType;
            this.size = size;
            this.lastModified = lastModified;
        }

        @NonNull
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

    private ContentStorage() {
        try (ContextLogger ignored = new ContextLogger(true, "ContentStorage.init")) {
            this.context = CgeoApplication.getInstance().getApplicationContext();
            this.documentAccessor = new DocumentContentAccessor(this.context);
            this.fileAccessor = new FileContentAccessor(this.context);
            this.documentAccessor.refreshUriPermissionCache();
        }
    }

    /**
     * checks if folder is available and can be used, creates it if need be.
     */
    public boolean ensureFolder(final PersistableFolder publicFolder) {
        final Folder folder = getFolder(publicFolder, false);
        if (folder == null) {
            return false;
        }
        return ensureFolder(folder, publicFolder.needsWrite(), false);
    }

    public boolean ensureFolder(final Folder folder, final boolean needsWrite) {
        return ensureFolder(folder, needsWrite, false);
    }

    public boolean ensureFolder(final Folder folder, final boolean needsWrite, final boolean testReadWrite) {
        if (folder == null) {
            return false;
        }

        final boolean success;
        try {
            success = getAccessorFor(folder.getBaseType()).ensureFolder(folder, needsWrite);
        } catch (IOException ioe) {
            return false;
        }

        if (success && testReadWrite) {
            return tryTestReadWriteToFolder(folder, needsWrite);
        }

        return success;
    }

    /**
     * Creates a new file in folder and returns its Uri
     */
    public Uri create(final PersistableFolder folder, final String name) {
        return create(folder, FileNameCreator.forName(name), false);
    }

    /**
     * Creates a new file in folder and returns its Uri
     */
    public Uri create(final PersistableFolder folder, final FileNameCreator nameCreator, final boolean onlyIfNotExisting) {
        return create(getFolder(folder, false), nameCreator, onlyIfNotExisting);
    }

    /**
     * Creates a new file in folder and returns its Uri
     */
    public Uri create(final Folder folder, final String name) {
        return create(folder, FileNameCreator.forName(name), false);
    }

    /**
     * Creates a new file in a folder location and returns its Uri
     */
    public Uri create(final Folder folder, final FileNameCreator nameCreator, final boolean onlyIfNotExisting) {
        if (folder == null) {
            return null;
        }

        final String name = (nameCreator == null ? FileNameCreator.DEFAULT : nameCreator).createName();
        if (onlyIfNotExisting) {
            final FileInformation fi = getFileInfo(folder, name);
            if (fi != null) {
                return fi.uri;
            }
        }

        try {
            return getAccessorFor(folder.getBaseType()).create(folder, name);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_create_failed, ioe, false, name, folder);
        }
        return null;
    }

    /**
     * Deletes the file represented by given Uri
     */
    public boolean delete(final Uri uri) {
        if (isEmpty(uri)) {
            return false;
        }
        try {
            return getAccessorFor(uri).delete(uri);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_delete_failed, ioe, false, uri);
        }
        return false;
    }

    /**
     * Renames the file represented by given Uri. This might change the Uri, the new Uri is returned.
     * If rename is unsuccessful then null is returned
     */
    public Uri rename(final Uri uri, final FileNameCreator fileNameCreator) {
        if (isEmpty(uri)) {
            return null;
        }

        try {
            return getAccessorFor(uri).rename(uri, fileNameCreator.createName());
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_delete_failed, ioe, false, uri);
        }
        return null;
    }

    /**
     * Lists all direct content of given folder
     */
    @NonNull
    public List<FileInformation> list(final PersistableFolder folder) {
        return list(folder, false);
    }

    /**
     * Lists all direct content of given folder
     */
    @NonNull
    public List<FileInformation> list(final PersistableFolder folder, final boolean suppressWarningForUser) {
        return list(getFolder(folder, suppressWarningForUser), false, suppressWarningForUser);
    }

    /**
     * Lists all direct content of given folder location
     */
    @NonNull
    public List<FileInformation> list(final Folder folder) {
        return list(folder, false, false);
    }

    /**
     * Lists all direct content of given folder location
     */
    @NonNull
    public List<FileInformation> list(final Folder folder, final boolean sortByName, final boolean suppressWarningForUser) {
        try (ContextLogger cLog = new ContextLogger("ContentStorage.list: %s", folder)) {
            if (folder == null) {
                return Collections.emptyList();
            }
            final List<FileInformation> result = getAccessorFor(folder).list(folder);
            cLog.add("#" + result.size());
            if (sortByName) {
                Collections.sort(result, (fi1, fi2) -> fi1.name.compareTo(fi2.name));
            }
            return result;
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_list_failed, suppressWarningForUser, ioe, folder);
        }
        return Collections.emptyList();
    }

    public boolean exists(final Folder folder, final String name) {
        return getFileInfo(folder, name) != null;
    }

    /**
     * Retrieves file information for a file in a folder. See {@link #getParentFolderAndFileInfo(Folder, String)}  for details.
     */
    @Nullable
    public FileInformation getFileInfo(final Folder rootFolder, final String subfolderAndName) {
        final ImmutablePair<FileInformation, Folder> info = getParentFolderAndFileInfo(rootFolder, subfolderAndName);
        return info == null ? null : info.left;
    }

    /**
     * Returns information for a file denoted by a rootFolder and a file name.
     * This file name may be a simple file name (e.g. "myfile.txt"), but it may also include a
     * subfolder structure (e.g. "subfolder/deepersubfolder/myfile.txt").
     *
     * @param rootFolder       root Folder
     * @param subfolderAndName file name to get information for, optionally with a subfolder structure before it
     * @return information about file (left) and direct parent folder of this file (right).
     * Note that file (left) might be null although its parent folder (right) is not null - e.g. if folder does exist, but not the file within it
     */
    public ImmutablePair<FileInformation, Folder> getParentFolderAndFileInfo(final Folder rootFolder, final String subfolderAndName) {

        if (rootFolder == null || StringUtils.isBlank(subfolderAndName)) {
            return null;
        }

        //find parent folder of file
        final Folder parentFolder;
        final String fileName;
        final int idx = subfolderAndName.lastIndexOf("/");
        if (idx < 0) {
            parentFolder = rootFolder;
            fileName = subfolderAndName;
        } else {
            parentFolder = Folder.fromFolder(rootFolder, subfolderAndName.substring(0, idx));
            fileName = subfolderAndName.substring(idx + 1);
        }

        if (parentFolder == null) {
            return null;
        }

        try {
            final FileInformation fileInfo = getAccessorFor(parentFolder).getFileInfo(parentFolder, fileName);
            return new ImmutablePair<>(fileInfo, parentFolder);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_folder_access_failed, ioe, false, rootFolder);
        }
        return null;
    }

    /**
     * Returns Uri for a folder. Returns null if this folder does not yet exist
     */
    public Uri getUriForFolder(final Folder folder) {
        if (folder == null) {
            return null;
        }
        try {
            return getAccessorFor(folder).getUriForFolder(folder);
        } catch (IOException ioe) {
            //"getUriForFolder" is used in "reportProblem", so calling it here might create a loop-of-death
            //reportProblem(R.string.contentstorage_err_folder_access_failed, ioe, folder);
            Log.v("Problem accessing folder " + folder, ioe);
        }
        return null;
    }

    /**
     * Helper method to get the display name for a Uri. Returns null if Uri does not exist.
     */
    public String getName(final Uri uri) {
        final FileInformation fi = getFileInfo(uri);
        return fi == null ? null : fi.name;
    }

    /**
     * Helper method to get the File Information for a Uri. Returns null if Uri does not exist.
     *
     * Note carefully: in case this Uri is not a File-Uri and points to a directory, the Folder field of the returned object is NOT FILLED
     * Unfortunately it is not possible to retrieve this info from an Uri alone.
     */
    public FileInformation getFileInfo(final Uri uri) {
        if (isEmpty(uri)) {
            return null;
        }

        try {
            return getAccessorFor(uri).getFileInfo(uri);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_read_failed, ioe, false, uri);
        }
        return null;
    }


    /**
     * Opens an Uri for writing. Remember to close stream after usage! Returns null if Uri can't be opened for writing.
     */
    public OutputStream openForWrite(final Uri uri) {
        return openForWrite(uri, false);
    }

    public OutputStream openForWrite(final Uri uri, final boolean append) {
        if (isEmpty(uri)) {
            return null;
        }

        try {
            //values "wa" (for append) and "rwt" (for overwrite) were tested on SDK21, SDK23, SDk29 and SDK30 using "ContentStorageTest"
            //Note that different values behave differently in different SDKs so be careful before changing them
            return context.getContentResolver().openOutputStream(uri, append ? "wa" : "rwt");
        } catch (IOException | SecurityException | IllegalArgumentException se) {
            //SecurityException is thrown for valid Uri which we have no permission to access
            //IllegalArgumentException is thrown for invalid Uri (e.g. because a folder/file was deleted meanwhile)
            reportProblem(R.string.contentstorage_err_write_failed, se, false, uri);
        }
        return null;
    }

    /**
     * Opens a file for read identified relatively to a root Folder and its name.
     * Name may contain optionally a subfolder structure, see {@link #getParentFolderAndFileInfo(Folder, String)} for details.
     * Remember to close stream after usage! Returns null if Uri can't be opened for reading.
     */
    @Nullable
    public InputStream openForRead(final Folder folder, final String subfolderAndName) {
        if (folder == null || subfolderAndName == null) {
            return null;
        }
        final FileInformation fi = getFileInfo(folder, subfolderAndName);
        if (fi != null) {
            return openForRead(fi.uri);
        }
        return null;
    }

    /**
     * Opens an Uri for reading. Remember to close stream after usage! Returns null if Uri can't be opened for reading.
     */
    @Nullable
    public InputStream openForRead(final Uri uri) {
        return openForRead(uri, false);
    }

    /**
     * Opens an Uri for reading. Remember to close stream after usage! Returns null if Uri can't be opened for reading.
     *
     * @param suppressWarningForUser if true then failure to open will NOT result in a toast to user
     */
    public InputStream openForRead(final Uri uri, final boolean suppressWarningForUser) {
        if (isEmpty(uri)) {
            return null;
        }

        try {
            return this.context.getContentResolver().openInputStream(uri);
        } catch (IOException | SecurityException | IllegalArgumentException se) {
            //SecurityException is thrown for valid Uri which we have no permission to access
            //IllegalArgumentException is thrown for invalid Uri (e.g. because a folder/file was deleted meanwhile)
            reportProblem(R.string.contentstorage_err_read_failed, se, suppressWarningForUser, uri);
        }
        return null;
    }

    public Uri copy(final Uri source, final Folder target, @Nullable final FileNameCreator newName, final boolean move) {
        boolean success = true;
        Exception failureEx = null;

        InputStream in = null;
        OutputStream out = null;
        Uri outputUri = null;
        try {
            FileNameCreator fnc = newName;
            if (fnc == null) {
                String displayName = getName(source);
                if (displayName == null) {
                    displayName = UriUtils.getLastPathSegment(source);
                }
                fnc = FileNameCreator.forName(displayName);
            }
            outputUri = create(target, fnc, false);
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
            closeQuietly(in, out);
            if (!success) {
                reportProblem(R.string.contentstorage_err_copy_failed, failureEx, false, source, target, move);
            }
        }
        return outputUri;
    }

    /**
     * Write an (internal's) file content to external storage
     */
    public Uri writeFileToFolder(final PersistableFolder folder, final FileNameCreator nameCreator, final File file, final boolean deleteFileOnSuccess) {
        return copy(Uri.fromFile(file), getFolder(folder, false), nameCreator, deleteFileOnSuccess);
    }

    /**
     * Helper method, meant for usage in conjunction with {@link #writeFileToFolder(PersistableFolder, FileNameCreator, File, boolean)}
     */
    public File createTempFile() {
        return createTempFile(null);
    }

    public File createTempFile(final String fileName) {
        File outputDir = null;
        try {
            outputDir = context.getCacheDir(); // context being the Activity pointer
            if (fileName == null) {
                return File.createTempFile("cgeo_tempfile_", ".tmp", outputDir);
            }
            final File tempFile = new File(outputDir, fileName);
            return tempFile.createNewFile() ? tempFile : null;
        } catch (IOException ie) {
            reportProblem(R.string.contentstorage_err_create_failed, ie, false, fileName, outputDir);
        }
        return null;
    }

    /**
     * Writes the content of given Uri to a (temporary!) File
     */
    public File writeUriToTempFile(final Uri uri, final String fileName) {
        File file = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = openForRead(uri);
            if (is == null) {
                return null;
            }
            file = createTempFile(fileName);
            os = new FileOutputStream(file);
            IOUtils.copy(is, os);
        } catch (IOException ie) {
            reportProblem(R.string.contentstorage_err_copy_failed, ie, false, uri, file, false);
            if (file != null && !file.delete()) {
                Log.i("File could not be deleted: " + file);
            }
        } finally {
            closeQuietly(is, os);
        }
        return file;
    }


    /**
     * Sets a new User-defined Folder for a {@link PersistableFolder}.
     */
    public void setUserDefinedFolder(final PersistableFolder folder, final Folder userDefinedFolder, final boolean setByUser) {
        folder.setUserDefinedFolder(userDefinedFolder, setByUser);
        documentAccessor.releaseOutdatedUriPermissions();
        ensureFolder(folder);
        //in case this shifts other default folders (e.g. when MAPS-folder is changed then default THEME folder might need new creation)
        PersistableFolder.reevaluateDefaultFolders();
    }

    public void setPersistedDocumentUri(final PersistableUri persistedDocUi, final Uri uri) {
        persistedDocUi.setPersistedUri(uri);
        documentAccessor.releaseOutdatedUriPermissions();
    }

    public void refreshUriPermissionCache() {
        documentAccessor.refreshUriPermissionCache();
    }

    // ---- private methods ----

    /**
     * Tries to read and (optionally) write something to the given folder location, returns whether this was successful or not
     */
    private boolean tryTestReadWriteToFolder(final Folder folder, final boolean testWrite) {

        try {
            Uri testDoc = null;
            if (testWrite) {
                testDoc = create(folder, FileNameCreator.DEFAULT, false);
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
        } catch (RuntimeException re) {
            //if something unexpected happens, then we consider this folder non-readable/writeable
            Log.e("Error on testing read/write of folder " + folder + ", testWrite=" + testWrite, re);
            return false;
        }
    }

    /**
     * Gets this folder's current location. If it is not accessible, then null is returned
     */
    @Nullable
    private Folder getFolder(final PersistableFolder folder, final boolean suppressWarningForUser) {

        if (!ensureFolder(folder.getFolder(), folder.needsWrite())) {
            reportProblem(R.string.contentstorage_err_folder_access_failed, suppressWarningForUser, folder);
            return null;
        }
        return folder.getFolder();
    }

    private AbstractContentAccessor getAccessorFor(final Uri uri) {
        return getAccessorFor(UriUtils.isFileUri(uri) ? Folder.FolderType.FILE : Folder.FolderType.DOCUMENT);
    }

    private AbstractContentAccessor getAccessorFor(final Folder folder) {
        return getAccessorFor(folder.getBaseType());
    }

    private AbstractContentAccessor getAccessorFor(final Folder.FolderType type) {
        switch (type) {
            case DOCUMENT:
                return this.documentAccessor;
            case FILE:
            default:
                return this.fileAccessor;
        }
    }

    protected Folder getAccessibleDefaultFolder(final Folder[] candidates, final boolean needsWrite, final String fallbackName) {

        for (Folder candidate : candidates) {
            //candidate is ok if it is either directly accessible or based on another public folder (which will become accessible later)
            if (candidate != null && ensureFolder(candidate, needsWrite, true)) {
                return candidate;
            }
        }

        return Folder.fromFolder(CGEO_PRIVATE_FILES, "public/" + fallbackName);
    }

    private void reportProblem(@StringRes final int messageId, final boolean suppressForUser, final Object... params) {
        reportProblem(messageId, null, suppressForUser, params);
    }

    private void reportProblem(@StringRes final int messageId, final Exception ex, final boolean suppressForUser, final Object... params) {

        if (reportRunningFlag.get() != null && reportRunningFlag.get()) {
            return;
        }
        reportRunningFlag.set(true);

        //prepare params message
        final ImmutablePair<String, String> messages = LocalizationUtils.getMultiPurposeString(messageId, "ContentStorage", params);
        Log.w("ContentStorage" + (suppressForUser ? "[suppressedForUser]" : "") + ": " + messages.right, ex);
        if (!suppressForUser && context != null) {
            ActivityMixin.showToast(context, messages.left);
        }
        reportRunningFlag.set(false);
    }

    private static boolean isEmpty(final Uri uri) {
        return uri == null || uri.equals(Uri.EMPTY);
    }
}
