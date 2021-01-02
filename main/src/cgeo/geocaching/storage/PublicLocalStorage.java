package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import static org.apache.commons.io.IOUtils.closeQuietly;

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

    private static final int DOCUMENT_FILE_CACHESIZE = 100;

    private final Context context;
    private final DocumentFolderAccessor documentAccessor;
    private final FileFolderAccessor fileAccessor;

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

    private abstract static class FolderAccessor {

        private final Context context;

        FolderAccessor(@NonNull final Context context) {
            this.context = context;
        }

        protected Context getContext() {
            return context;
        }

        public abstract boolean delete(@NonNull Uri uri) throws IOException;

        public abstract Uri create(@NonNull Folder folder, @NonNull String name) throws IOException;

        public abstract boolean checkAvailability(@NonNull Folder folder, boolean needsWrite) throws IOException;

        public abstract List<FileInformation> list(@NonNull Folder folder) throws IOException;

        /** creates physical folder on device if it is not already there anyway */
        public abstract boolean ensureFolder(@NonNull Folder folder) throws IOException;

        //some helpers for subclasses
        protected String getTypeForName(@NonNull final String name) {
            final int lastDot = name.lastIndexOf('.');
            if (lastDot >= 0) {
                final String extension = name.substring(lastDot + 1).toLowerCase();
                final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (mime != null) {
                    return mime;
                }
            }
            return "application/octet-stream";
        }

        protected boolean hasFile(@NonNull final Folder folder, @NonNull final String name) throws IOException {
            for (FileInformation fi : list(folder)) {
                if (fi.name.equals(name)) {
                    return true;
                }
            }
            return false;
        }

        protected String createUniqueFilename(@NonNull final Folder dir, @NonNull final String docName) throws IOException {

            //split in suffix and praefix
            final int suffIdx = docName.lastIndexOf(".");
            final String suffix = suffIdx >= 0 ? docName.substring(suffIdx) : "";
            final String praefix = suffIdx >= 0 ? docName.substring(0, suffIdx) : docName;

            String newPraefix = praefix;

            int idx = 1;
            while (hasFile(dir, newPraefix + suffix)) {
                newPraefix = praefix + " (" + (idx++) + ")";
            }

            return newPraefix + suffix;
        }

    }

    private static class FileFolderAccessor extends FolderAccessor {

        FileFolderAccessor(@NonNull final Context context) {
            super(context);
        }

        public boolean delete(@NonNull final Uri uri) {
            return new File(uri.getPath()).delete();
        }

        public Uri create(@NonNull final Folder folder, @NonNull final String name) throws IOException {
            final File dir = toFile(folder, true);
            if (dir == null) {
                return null;
            }
            final String fileName = createUniqueFilename(folder, name);
            try {
                final File newFile = new File(dir, fileName);
                newFile.createNewFile();
                return Uri.fromFile(newFile);
            } catch (IOException ioe) {
                throw new IOException("Could not create file '" + fileName + "' in dir '" + dir + "'", ioe);
            }
        }

        public boolean checkAvailability(@NonNull final Folder folder, @NonNull final boolean needsWrite) {
            return toFile(folder, needsWrite) != null;
        }

        public List<FileInformation> list(@NonNull final Folder folder) {
            final File dir = toFile(folder, false);
            if (dir == null) {
                return Collections.emptyList();
            }
            return CollectionStream.of(dir.listFiles())
                .map(f -> new FileInformation(
                    f.getName(), Uri.withAppendedPath(folder.getUri(),
                    f.getName()), f.isDirectory(),
                    f.isDirectory() ? Folder.fromFolder(folder, f.getName()) : null, getTypeForName(f.getName()),
                    f.length(), f.lastModified())).toList();
        }

        public boolean ensureFolder(@NonNull final Folder folder) {
            final File dir = new File(folder.getUri().getPath());
            if (dir.isDirectory()) {
                return true;
            }
            return new File(folder.getUri().getPath()).mkdirs();
        }

        private File toFile(@NonNull final Folder folder, final boolean needsWrite) {
            ensureFolder(folder);
            final File dir = new File(folder.getUri().getPath());
            return dir.isDirectory() && dir.canRead() && (!needsWrite || dir.canWrite()) ? dir : null;
        }

    }

    private static class DocumentFolderAccessor extends FolderAccessor {

        /** cache for Uri permissions */
        private final Map<String, UriPermission> uriPermissionCache = new HashMap<>();

        /** LRU-cache for previously retrieved DocumentFiles */
        private final Map<String, DocumentFile> documentFileCache = Collections.synchronizedMap(new LinkedHashMap<String, DocumentFile>(DOCUMENT_FILE_CACHESIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<String, DocumentFile> eldest) {
                return size() > DOCUMENT_FILE_CACHESIZE;
            }
        });


        DocumentFolderAccessor(@NonNull final Context context) {
            super(context);
            refreshUriPermissionCache();
        }

        @Override
        public boolean delete(@NonNull final Uri uri) throws IOException {
            return DocumentsContract.deleteDocument(getContext().getContentResolver(), uri);
        }

        @Override
        public Uri create(@NonNull final Folder folder, @NonNull final String name) throws IOException {
            final DocumentFile folderFile = getFolderFile(folder, true);
            if (folderFile == null) {
                return null;
            }
            final String docName = createUniqueFilename(folder, name);
            //Do NOT pass a mimeType. It will then be selected based on the file suffix.
            final DocumentFile newFile = folderFile.createFile(null, docName);
            return newFile == null ? null : newFile.getUri();
        }

        @Override
        public boolean checkAvailability(@NonNull final Folder folder, final boolean needsWrite) throws IOException  {
            final DocumentFile folderFile = getFolderFile(folder, needsWrite);
            return folderFile != null && folderFile.isDirectory() && folderFile.canRead() && (!needsWrite || folderFile.canWrite());
        }

        @Override
        public List<FileInformation> list(@NonNull final Folder folder) throws IOException {
            final DocumentFile dir = getFolderFile(folder, false);
            if (dir == null) {
                return Collections.emptyList();
            }

            //using dir.listFiles() is FAR too slow. Thus we have to create an explicit query

            final ContentResolver resolver = getContext().getContentResolver();
            final Uri mUri = dir.getUri();
            final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mUri,
                DocumentsContract.getDocumentId(mUri));
            final ArrayList<FileInformation> results = new ArrayList<>();

            Cursor c = null;
            try {
                c = resolver.query(childrenUri, new String[] {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE,

                }, null, null, null);
                while (c.moveToNext()) {
                    final String documentId = c.getString(0);
                    final String name = c.getString(1);
                    final String mimeType = c.getString(2);
                    final long lastMod = c.getLong(3);
                    final long size = c.getLong(4);

                    final boolean isDir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
                    final Uri uri = DocumentsContract.buildDocumentUriUsingTree(mUri, documentId);

                    results.add(new FileInformation(name, uri, isDir, isDir ? Folder.fromFolder(folder, name) : null, mimeType, size, lastMod));
                }
            } finally {
                closeQuietly(c);
            }

            return results;
        }

        @Override
        public boolean ensureFolder(@NonNull final Folder folder) throws IOException {
            //checkAvailability will also create folder if not already existing
            return checkAvailability(folder, true);
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
        private DocumentFile getFolderFile(final Folder folder, final boolean needsWrite) throws IOException {
            if (folder == null) {
                return null;
            }

            try (ContextLogger cLog = new ContextLogger("DocumentFileAccessor.getFolderFile: %s", folder)) {

                final Uri baseUri = folder.getBaseUri();
                if (!checkUriPermissions(baseUri, needsWrite)) {
                    return null;
                }
                final DocumentFile baseDir = DocumentFile.fromTreeUri(getContext(), baseUri);
                if (baseDir == null || !baseDir.isDirectory() || !baseDir.canRead() || (needsWrite && !baseDir.canWrite())) {
                    Log.w("Base Folder not accessible or no necessary permissions: '" + folder + "'");
                    return null;
                }
                cLog.add("got base");

                return getSubdirFolderFile(baseUri, baseDir, folder.getSubdirsToBase());
            }
        }

        private DocumentFile getSubdirFolderFile(@NonNull final Uri baseUri, @NonNull final DocumentFile baseDir, @NonNull final List<String> subdirs) throws IOException {

            if (subdirs.isEmpty()) {
                return baseDir;
            }

            //try to find parent entries in cache
            DocumentFile dir = baseDir;
            int subdirIdx = subdirs.size();
            while (subdirIdx > 0) {
                final String cacheKey = documentFileCacheKey(baseUri, subdirs, subdirIdx);
                final DocumentFile docFile = findInDocumentCache(cacheKey);
                if (docFile != null) {
                    dir = docFile;
                    break;
                }
                subdirIdx--;
            }

            //from cache entry work up and find/create missing dirs
            while (subdirIdx < subdirs.size()) {
                final String subfolderName = subdirs.get(subdirIdx);
                DocumentFile child = dir.findFile(subfolderName);
                if (child == null) {
                    //create a new subfolder
                    child = dir.createDirectory(subfolderName);
                    if (child == null) {
                        throw new IOException("Failed to create subdir " + subfolderName + " for dir " + dir + ": reason unknown");
                    }
                } else if (!child.isDirectory()) {
                    throw new IOException("Failed to access subdir " + subfolderName + " for dir " + dir + ": entry exists but is not a directory");
                }
                dir = child;
                subdirIdx++;
            }

            putToDocumentCache(documentFileCacheKey(baseUri, subdirs, -1), dir);
            return dir;
        }

        private String documentFileCacheKey(final Uri baseUri, final List<String> subdirs, final int max) {
            final StringBuilder key = new StringBuilder(baseUri.toString()).append("::");
            int cnt = 0;
            for (String subdir : subdirs) {
                key.append("/").append(subdir);
                if (max >= 0 && ++cnt >= max) {
                    break;
                }
            }
            return key.toString();
        }

        private DocumentFile findInDocumentCache(final String key) {
            final DocumentFile cacheEntry = this.documentFileCache.get(key);
            if (cacheEntry == null) {
                return null;
            }
            if (!cacheEntry.isDirectory()) {
                this.documentFileCache.remove(key);
                return null;
            }
            return cacheEntry;
        }

        private void putToDocumentCache(final String key, final DocumentFile docFile) {
            this.documentFileCache.put(key, docFile);
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

        /** Checks and releases all Uri Permissions which are no longer needed */
        public void releaseOutdatedUriPermissions() {
            final Set<String> usedUris = new HashSet<>();
            for (PublicLocalFolder folder : PublicLocalFolder.values()) {
                if (folder.getFolder().getBaseUri() != null) {
                    usedUris.add(UriUtils.toCompareString(folder.getFolder().getBaseUri()));
                }
            }

            for (UriPermission uriPerm : getContext().getContentResolver().getPersistedUriPermissions()) {
                if (!usedUris.contains(UriUtils.toCompareString(uriPerm.getUri()))) {
                    Log.iForce("Releasing UriPermission: " + UriUtils.uriPermissionToString(uriPerm));
                    final int flags = calculateUriPermissionFlags(uriPerm.isReadPermission(), uriPerm.isWritePermission());
                    getContext().getContentResolver().releasePersistableUriPermission(uriPerm.getUri(), flags);
                }
            }
            refreshUriPermissionCache();
        }

        /** refreshes internal Uri Permission cache */
        public void refreshUriPermissionCache() {
            this.uriPermissionCache.clear();
            for (UriPermission uriPerm : getContext().getContentResolver().getPersistedUriPermissions()) {
                final String key = UriUtils.toCompareString(uriPerm.getUri());
                final boolean containsKey = this.uriPermissionCache.containsKey(key);
                if (!containsKey || uriPerm.isWritePermission()) {
                    this.uriPermissionCache.put(key, uriPerm);
                }
            }
        }
    }

    private PublicLocalStorage() {
        this.context = CgeoApplication.getInstance().getApplicationContext();
        this.documentAccessor = new DocumentFolderAccessor(this.context);
        this.fileAccessor = new FileFolderAccessor(this.context);
        this.documentAccessor.refreshUriPermissionCache();
    }

    /** checks if folder is available and can be used. If current setting is not available, folder may be adjusted e.g. to default */
    public boolean checkAndAdjustAvailability(final PublicLocalFolder publicFolder) {
        final Folder folder = getAndAdjustFolderLocation(publicFolder);
        return checkAvailability(folder, publicFolder.needsWrite(), false);
     }

    public boolean checkAvailability(final Folder folder, final boolean needsWrite) {
        return checkAvailability(folder, needsWrite, false);
    }

    public boolean checkAvailability(final Folder folder, final boolean needsWrite, final boolean testReadWrite) {
        if (folder == null) {
            return false;
        }

        final boolean success;
        try {
            success = getAccessorFor(folder.getBaseType()).checkAvailability(folder, needsWrite);
        } catch (IOException ioe) {
            return false;
        }

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
        final String name = (nameCreator == null ? FileNameCreator.DEFAULT : nameCreator).createName();
        if (folder == null) {
            return null;
        }

        try {
            return getAccessorFor(folder.getBaseType()).create(folder, name);
        } catch (IOException ioe) {
            reportProblem(R.string.publiclocalstorage_err_create_failed, ioe, name, folder);
        }
        return null;
    }

    /** creates physical folder on device if it is not already there anyway */
    public boolean ensureFolder(final Folder folder) {
        if (folder == null) {
            return false;
        }
        try {
            return getAccessorFor(folder.getBaseType()).ensureFolder(folder);
        } catch (IOException ioe) {
            reportProblem(R.string.publiclocalstorage_err_ensurefolder_failed, ioe, folder);
        }
        return false;
    }

     /** Deletes the file represented by given Uri */
    public boolean delete(final Uri uri) {
        if (uri == null) {
            return false;
        }
        try {
            return getAccessorFor(uri).delete(uri);
        } catch (IOException ioe) {
            reportProblem(R.string.publiclocalstorage_err_delete_failed, ioe, uri);
        }
        return false;
    }

    /** Lists all direct content of given folder */
    @NonNull
    public List<FileInformation> list(final PublicLocalFolder folder) {
        return list(getAndAdjustFolderLocation(folder));
    }

    /** Lists all direct content of given folder location */
    @NonNull
    public List<FileInformation> list(final Folder folder) {
        if (folder == null) {
            return Collections.emptyList();
        }
        try (ContextLogger cLog = new ContextLogger("PublicLocalStorage.list: %s", folder)) {
            final List<FileInformation> result = getAccessorFor(folder).list(folder);
            cLog.add("#" + result.size());
            return result;
        } catch (IOException ioe) {
            reportProblem(R.string.publiclocalstorage_err_list_failed, ioe, folder);
        }
        return Collections.emptyList();
    }

    /** Opens an Uri for writing. Remember to close stream after usage! Returns null if Uri can't be opened for writing. */
    public OutputStream openForWrite(final Uri uri) {
        try {
            return context.getContentResolver().openOutputStream(uri);
        } catch (IOException | SecurityException se) {
            reportProblem(R.string.publiclocalstorage_err_write_failed, se, uri);
        }
        return null;
    }

    /** Opens an Uri for reading. Remember to close stream after usage! Returns null if Uri can't be opened for reading. */
    public InputStream openForRead(final Uri uri) {

        try {
            return this.context.getContentResolver().openInputStream(uri);
        } catch (IOException | SecurityException se) {
            reportProblem(R.string.publiclocalstorage_err_read_failed, se, uri);
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
            closeQuietly(in, out);
            if (!success) {
                reportProblem(R.string.publiclocalstorage_err_copy_failed, failureEx, source, target, move);
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
        File outputDir = null;
        try {
            outputDir = context.getCacheDir(); // context being the Activity pointer
            return File.createTempFile("cgeo_tempfile_", ".tmp", outputDir);
        } catch (IOException ie) {
            reportProblem(R.string.publiclocalstorage_err_create_failed, ie, "temp file", outputDir);
        }
        return null;
    }

    /** Sets a new User-defined Uri for a PublicLocalFolder. Must be a DocumentUri (retrieved via {@link Intent#ACTION_OPEN_DOCUMENT_TREE})! */
    public void setFolderUserDefinedUri(final PublicLocalFolder folder, final Uri documentUri) {
        folder.setUserDefinedDocumentUri(documentUri);
        documentAccessor.releaseOutdatedUriPermissions();
    }

    public void refreshUriPermissionCache() {
        documentAccessor.refreshUriPermissionCache();
    }

    // ---- private methods ----

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

    /** Gets this folder's current location. Tries to adjust folder location if no permission given. May return null if no permission found */
    @Nullable
    private Folder getAndAdjustFolderLocation(final PublicLocalFolder folder) {

        if (!checkAvailability(folder.getFolder(), folder.needsWrite())) {
            //try out default
            //if this is a user-selected folder and base dir is ok we initiate a fallback to default folder
            if (!folder.getUserDefinedFolder() || !checkAvailability(folder.getDefaultFolder(), folder.needsWrite())) {
                return null;
            }

            final String folderUserdefined = folder.toUserDisplayableString();
            setFolderUserDefinedUri(folder, null);
            final String folderDefault = folder.toUserDisplayableString();
            if (!checkAvailability(folder.getFolder(), folder.needsWrite())) {
                reportProblem(R.string.publiclocalstorage_err_publicfolder_inaccessible_falling_back, folder.name(), folderUserdefined, null);
                return null;
            }
            reportProblem(R.string.publiclocalstorage_err_publicfolder_inaccessible_falling_back, folder.name(), folderUserdefined, folderDefault);
        }
        return folder.getFolder();
    }

    private FolderAccessor getAccessorFor(final Uri uri) {
        return getAccessorFor(UriUtils.isFileUri(uri) ? Folder.FolderType.FILE : Folder.FolderType.DOCUMENT);
    }

    private FolderAccessor getAccessorFor(final Folder folder) {
        return getAccessorFor(folder.getBaseType());
    }

    private FolderAccessor getAccessorFor(final Folder.FolderType type) {
        switch (type) {
            case DOCUMENT:
                return this.documentAccessor;
            case FILE:
            default:
                return this.fileAccessor;
        }
    }


    private void reportProblem(@StringRes final int messageId, final Object ... params) {
        reportProblem(messageId, null, params);
    }

    private void reportProblem(@StringRes final int messageId, final Exception ex, final Object ... params) {
        final String logMessage = context.getString(messageId, params);
        Log.w("PublicLocalStorage: " + logMessage, ex);

        //prepare user message
        final Object[] paramsForUser = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Folder) {
                paramsForUser[i] = ((Folder) params[i]).toUserDisplayableString();
            } else if (params[i] instanceof Uri) {
                paramsForUser[i] = UriUtils.toUserDisplayableString((Uri) params[i]);
            } else {
                paramsForUser[i] = params[i];
            }
        }
        ActivityMixin.showToast(context, context.getString(messageId, paramsForUser));
    }

}
