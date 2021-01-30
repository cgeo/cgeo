package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.FileNameCreator;
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
 *  * Android Doku: https://developer.android.com/preview/privacy/storage
 *  * Android Doku on use cases: https://developer.android.com/training/data-storage/use-cases#handle-non-media-files
 *  * Introduction: https://www.androidcentral.com/what-scoped-storage
 *  * Helpers: https://stackoverflow.com/questions/34927748/android-5-0-documentfile-from-tree-uri
 */
public class ContentStorage {

    private final Context context;
    private final DocumentContentAccessor documentAccessor;
    private final FileContentAccessor fileAccessor;

    private static final ContentStorage INSTANCE = new ContentStorage();

    public static ContentStorage get() {
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
        this.context = CgeoApplication.getInstance().getApplicationContext();
        this.documentAccessor = new DocumentContentAccessor(this.context);
        this.fileAccessor = new FileContentAccessor(this.context);
        this.documentAccessor.refreshUriPermissionCache();

        for (PersistableFolder folder : PersistableFolder.values()) {
            //(re)sets default folders and ensures that it is definitely accessible
            folder.setDefaultFolder(getAccessibleDefaultFolder(folder.getDefaultCandidates(), folder.needsWrite(), folder.name()));
            //tests user-defined folder (if any) and reset to default if not accessible
            ensureAndAdjustFolder(folder);
        }
    }

    /** checks if folder is available and can be used. If current setting is not available, folder may be adjusted e.g. to default */
    public boolean ensureAndAdjustFolder(final PersistableFolder publicFolder) {
        final Folder folder = getAndAdjustFolder(publicFolder);
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

    /** Creates a new file in folder and returns its Uri */
    public Uri create(final PersistableFolder folder, final String name) {
        return create(folder, FileNameCreator.forName(name), false);
    }

    /** Creates a new file in folder and returns its Uri */
    public Uri create(final PersistableFolder folder, final FileNameCreator nameCreator, final boolean onlyIfNotExisting) {
        return create(getAndAdjustFolder(folder), nameCreator, onlyIfNotExisting);
    }

    /** Creates a new file in folder and returns its Uri */
    public Uri create(final Folder folder, final String name) {
        return create(folder, FileNameCreator.forName(name), false);
    }

    /** Creates a new file in a folder location and returns its Uri */
    public Uri create(final Folder folder, final FileNameCreator nameCreator, final boolean onlyIfNotExisting) {
        final String name = (nameCreator == null ? FileNameCreator.DEFAULT : nameCreator).createName();
        if (folder == null) {
            return null;
        }

        if (onlyIfNotExisting) {
            final FileInformation fi = getFileInfo(folder, name);
            if (fi != null) {
                return fi.uri;
            }
        }

        try {
            return getAccessorFor(folder.getBaseType()).create(folder, name);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_create_failed, ioe, name, folder);
        }
        return null;
    }
     /** Deletes the file represented by given Uri */
    public boolean delete(final Uri uri) {
        if (uri == null) {
            return false;
        }
        try {
            return getAccessorFor(uri).delete(uri);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_delete_failed, ioe, uri);
        }
        return false;
    }

    /** Lists all direct content of given folder */
    @NonNull
    public List<FileInformation> list(final PersistableFolder folder) {
        return list(getAndAdjustFolder(folder));
    }

    /** Lists all direct content of given folder location */
    @NonNull
    public List<FileInformation> list(final Folder folder) {
        return list(folder, false);
    }

    /** Lists all direct content of given folder location */
    @NonNull
    public List<FileInformation> list(final Folder folder, final boolean sortByName) {
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
            reportProblem(R.string.contentstorage_err_list_failed, ioe, folder);
        }
        return Collections.emptyList();
    }

    public boolean exists(final Folder folder, final String name) {
        return getFileInfo(folder, name) != null;
    }

    @Nullable
    public FileInformation getFileInfo(final Folder folder, final String name) {
        if (folder == null || StringUtils.isBlank(name)) {
            return null;
        }
        try {
            return getAccessorFor(folder).getFileInfo(folder, name);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_folder_access_failed, ioe, folder);
        }
        return null;
    }

    /** Returns Uri for a folder. Returns null if this folder does not yet exist */
    public Uri getUriForFolder(final Folder folder) {
        if (folder == null) {
            return null;
        }
        try {
            return getAccessorFor(folder).getUriForFolder(folder);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_folder_access_failed, ioe, folder);
        }
        return null;
    }

    /** Helper method to get the display name for a Uri. Returns null if Uri does not exist. */
    public String getName(final Uri uri) {
        final FileInformation fi = getFileInfo(uri);
        return fi == null ? null : fi.name;
    }

    /**
     * Helper method to get the File Information for a Uri. Returns null if Uri does not exist.
     *
     * Note carefully: in case this Uri is not a File-Uri and points to a directory, the Folder field of the returned object is NOT FILLED
     * Unfortunately it is not possible to retrieve this info from an Uri alone.
     * */
    public FileInformation getFileInfo(final Uri uri) {
        if (uri == null) {
            return null;
        }

        try {
            return getAccessorFor(uri).getFileInfo(uri);
        } catch (IOException ioe) {
            reportProblem(R.string.contentstorage_err_read_failed, ioe, uri);
        }
        return null;
    }


    /** Opens an Uri for writing. Remember to close stream after usage! Returns null if Uri can't be opened for writing. */
    public OutputStream openForWrite(final Uri uri) {
        return openForWrite(uri, false);
    }

    public OutputStream openForWrite(final Uri uri, final boolean append) {
        if (uri == null) {
            return null;
        }

        try {
            //values "wa" (for append) and "rwt" (for overwrite) were tested on SDK21, SDK23, SDk29 and SDK30 using "ContentStorageTest"
            //Note that different values behave differently in different SDKs so be careful before changing them
            return context.getContentResolver().openOutputStream(uri, append ? "wa" : "rwt");
        } catch (IOException | SecurityException se) {
            reportProblem(R.string.contentstorage_err_write_failed, se, uri);
        }
        return null;
    }

    public InputStream openForRead(final Folder folder, final String name) {
        if (folder == null || name == null) {
            return null;
        }
        final FileInformation fi = getFileInfo(folder, name);
        if (fi != null) {
            return openForRead(fi.uri);
        }
        return null;
    }

    /** Opens an Uri for reading. Remember to close stream after usage! Returns null if Uri can't be opened for reading. */
    public InputStream openForRead(final Uri uri) {
        return openForRead(uri, false);
    }

    public InputStream openForRead(final Uri uri, final boolean suppressWarningOnFailure) {
        if (uri == null) {
            return null;
        }

        try {
            return this.context.getContentResolver().openInputStream(uri);
        } catch (IOException | SecurityException se) {
            if (!suppressWarningOnFailure) {
                reportProblem(R.string.contentstorage_err_read_failed, se, uri);
            }
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
            outputUri = create(target, newName != null ? newName : FileNameCreator.forName(UriUtils.getLastPathSegment(source)), false);
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
                reportProblem(R.string.contentstorage_err_copy_failed, failureEx, source, target, move);
            }
        }
        return outputUri;
    }

    /** Write an (internal's) file content to external storage */
    public Uri writeFileToFolder(final PersistableFolder folder, final FileNameCreator nameCreator, final File file, final boolean deleteFileOnSuccess) {
        return copy(Uri.fromFile(file), getAndAdjustFolder(folder), nameCreator, deleteFileOnSuccess);
    }

    /** Helper method, meant for usage in conjunction with {@link #writeFileToFolder(PersistableFolder, FileNameCreator, File, boolean)} */
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
            reportProblem(R.string.contentstorage_err_create_failed, ie, fileName, outputDir);
        }
        return null;
    }

    /** Writes the content of given Uri to a (temporary!) File */
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
            reportProblem(R.string.contentstorage_err_copy_failed, ie, uri, file, false);
            if (file != null && !file.delete()) {
                Log.i("File could not be deleted: " + file);
            }
        } finally {
            closeQuietly(is, os);
        }
        return file;
    }


    /** Sets a new User-defined Folder for a {@link PersistableFolder}. */
    public void setUserDefinedFolder(final PersistableFolder folder, final Folder userDefinedFolder) {
        folder.setUserDefinedFolder(userDefinedFolder);
        documentAccessor.releaseOutdatedUriPermissions();
    }

    public void setPersistedDocumentUri(final PersistableUri persistedDocUi, final Uri uri) {
        persistedDocUi.setPersistedUri(uri);
        documentAccessor.releaseOutdatedUriPermissions();
    }

    public void refreshUriPermissionCache() {
        documentAccessor.refreshUriPermissionCache();
    }

    // ---- private methods ----

    /** Tries to read and (optionally) write something to the given folder location, returns whether this was successful or not */
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

    /** Gets this folder's current location. Tries to adjust folder location if no permission given. May return null if no permission found */
    @Nullable
    private Folder getAndAdjustFolder(final PersistableFolder folder) {

        if (!ensureFolder(folder.getFolder(), folder.needsWrite())) {
            //try out default
            //if this is a user-selected folder and base dir is ok we initiate a fallback to default folder
            if (!folder.isUserDefined() || !ensureFolder(folder.getDefaultFolder(), folder.needsWrite())) {
                return null;
            }

            final String folderUserdefined = folder.toUserDisplayableValue();
            setUserDefinedFolder(folder, null);
            final String folderDefault = folder.toUserDisplayableValue();
            if (!ensureFolder(folder.getFolder(), folder.needsWrite())) {
                reportProblem(R.string.contentstorage_err_publicfolder_inaccessible_falling_back, folder.name(), folderUserdefined, null);
                return null;
            }
            reportProblem(R.string.contentstorage_err_publicfolder_inaccessible_falling_back, folder.name(), folderUserdefined, folderDefault);
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

    private Folder getAccessibleDefaultFolder(final Folder[] candidates, final boolean needsWrite, final String fallbackName) {

        for (Folder candidate : candidates) {
            //candidate is ok if it is either directly accessible or based on another public folder (which will become accessible later)
            if (candidate != null && (candidate.getRootPersistableFolder() != null || ensureFolder(candidate, needsWrite))) {
                return candidate;
            }
        }

        return Folder.fromFolder(CGEO_PRIVATE_FILES, "public/" + fallbackName);
    }

    private void reportProblem(@StringRes final int messageId, final Object ... params) {
        reportProblem(messageId, null, params);
    }

    private void reportProblem(@StringRes final int messageId, final Exception ex, final Object ... params) {

        //prepare params message
        final ImmutablePair<String, String> messages = constructMessage(messageId, params);
        Log.w("ContentStorage: " + messages.right, ex);
        if (context != null) {
            ActivityMixin.showToast(context, messages.left);
        }
    }

    /**
     * Given a resource id and parameters to fill it, constructs one message fit fpr user display (left) and one for log file (right)
     * Difference is that the one for the log file will contain more detailled information than that for the end user
     */
    public ImmutablePair<String, String> constructMessage(@StringRes final int messageId, final Object ... params) {
        //prepare params message
        final Object[] paramsForLog = new Object[params.length];
        final Object[] paramsForUser = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Folder) {
                paramsForUser[i] = ((Folder) params[i]).toUserDisplayableString();
                paramsForLog[i] = params[i] + "(" + getUriForFolder((Folder) params[i]) + ")";
            } else if (params[i] instanceof PersistableFolder) {
                paramsForUser[i] = ((PersistableFolder) params[i]).toUserDisplayableValue();
                paramsForLog[i] = params[i] + "(" + getUriForFolder(((PersistableFolder) params[i]).getFolder()) + ")";
            } else if (params[i] instanceof Uri) {
                paramsForUser[i] = UriUtils.toUserDisplayableString((Uri) params[i]);
                paramsForLog[i] = params[i];
            } else {
                paramsForUser[i] = params[i];
                paramsForLog[i] = params[i];
            }
        }
        return new ImmutablePair<>(context.getString(messageId, paramsForUser), context.getString(messageId, paramsForLog));
    }

}
