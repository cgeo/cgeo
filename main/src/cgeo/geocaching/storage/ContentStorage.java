package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.storage.Folder.CGEO_PRIVATE_FILES;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final int DOCUMENT_FILE_CACHESIZE = 100;

    private final Context context;
    private final DocumentFolderAccessor documentAccessor;
    private final FileFolderAccessor fileAccessor;

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

    /** Base class for all folder accessors */
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

        public abstract List<FileInformation> list(@NonNull Folder folder) throws IOException;

        /** If a file with given name exists in folder, it is returned. Otherwise null is returned */
        public abstract FileInformation get(@NonNull Folder folder, String name) throws IOException;

        /** creates physical folder on device if it is not already there anyway */
        public abstract boolean ensureFolder(@NonNull Folder folder, boolean needsWrite) throws IOException;

        public abstract Uri getUriForFolder(@NonNull Folder folder) throws IOException;

        public abstract String getName(@NonNull Uri uri) throws IOException;

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



        protected String createUniqueFilename(@NonNull final String requestedName, @NonNull final List<String> existingNames) {

            //split in suffix and praefix
            final int suffIdx = requestedName.lastIndexOf(".");
            final String suffix = suffIdx >= 0 ? requestedName.substring(suffIdx) : "";
            final String praefix = suffIdx >= 0 ? requestedName.substring(0, suffIdx) : requestedName;

            String newPraefix = praefix;

            int idx = 1;
            while (existingNames.contains(newPraefix + suffix)) {
                newPraefix = praefix + " (" + (idx++) + ")";
            }

            return newPraefix + suffix;
        }

    }

    /** Implementation for File-based folders (= Files with isDirectory = true) */
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
            final String fileName = createUniqueFilename(name, Arrays.asList(dir.list()));
            try {
                final File newFile = new File(dir, fileName);
                return newFile.createNewFile() ? Uri.fromFile(newFile) : null;
            } catch (IOException ioe) {
                throw new IOException("Could not create file '" + fileName + "' in dir '" + dir + "'", ioe);
            }
        }

        public FileInformation get(@NonNull final Folder folder, final String name) {
            final File dir = toFile(folder, false);
            if (dir == null) {
                return null;
            }
            final File f = new File(dir, name);
            return f.exists() ? fileToInformation(folder, f) : null;
        }

        public List<FileInformation> list(@NonNull final Folder folder) {
            final File dir = toFile(folder, false);
            if (dir == null) {
                return Collections.emptyList();
            }
            return CollectionStream.of(dir.listFiles())
                .map(f -> fileToInformation(folder, f)).toList();
        }

        public boolean ensureFolder(@NonNull final Folder folder, final boolean needsWrite) {
            final File dir = new File(folderToUri(folder).getPath());
            if (dir.isDirectory()) {
                return dir.canRead() && (!needsWrite || dir.canWrite());
            }
            return dir.mkdirs() && dir.canRead() && (!needsWrite || dir.canWrite());
        }

        /** Must return null if folder does not yet exist */
        public Uri getUriForFolder(@NonNull final Folder folder) {
            final Uri folderUri = folderToUri(folder);
            if (new File(folderUri.getPath()).isDirectory()) {
                return folderUri;
            }
            return null;
        }

        /** Must return null if file does not yet exist */
        public String getName(@NonNull final Uri uri) {
            if (new File(uri.getPath()).exists()) {
                return UriUtils.getLastPathSegment(uri);
            }
            return null;
        }

        private Uri folderToUri(final Folder folder) {
            return UriUtils.appendPath(folder.getBaseUri(), CollectionStream.of(folder.getSubdirsToBase()).toJoinedString("/"));
        }

        private File toFile(@NonNull final Folder folder, final boolean needsWrite) {
            if (!ensureFolder(folder, needsWrite)) {
                return null;
            }
            return new File(folderToUri(folder).getPath());
        }

        private FileInformation fileToInformation(final Folder folder, final File file) {
            return new FileInformation(
                file.getName(), UriUtils.appendPath(folderToUri(folder), file.getName()),
                file.isDirectory(),
                file.isDirectory() ? Folder.fromFolder(folder, file.getName()) : null, getTypeForName(file.getName()),
                file.length(), file.lastModified());
        }
    }

    /**
     * Implementation for SAF/Document-based folders
     *
     * Implementation notes: simply implementing SAF Document access using DocumentFile and related classes
     * yields simply unacceptable performance, because underneath these classes issue queries on almost every
     * method (e.g. isDirectory(), getType() and the like...). Therefore the current implementation uses
     * direct querying against ContentResolver.
     *
     * Document Framework does not efficiently (wrt performance) allow to work with subdirectories: Uris can't be build by a certain rule
     * and only querying of one folder content is possible. In order to efficiently deal with subfolder structures, an LRU
     * Uri cache was introduced (folderUriCache).
     * Unfortunately Document Framework does also not allow for efficient check whether a (cached) Uri
     * is still valid (it is not if e.g. the underlying doc was deleted externally of c:geo), so a two-pass algorithm was implemented
     * where the cache value is trusted in a first attempt and only refreshed if this triggered an exception.
     *
     * It must be noted that despite all effort Document framework is simply very slow esp when compared to File framework.
     * Reading methods ( e.g. list()) take up to 5ms and writing methods (e.g. create(), delete()) up to 10ms! Found no way around this...
     */
    private static class DocumentFolderAccessor extends FolderAccessor {

        /** cache for Uri permissions */
        private final Map<String, UriPermission> uriPermissionCache = new HashMap<>();

        /** LRU-cache for previously retrieved directory Uri's for Folders */
        private final Map<String, Uri> folderUriCache = Collections.synchronizedMap(new LinkedHashMap<String, Uri>(DOCUMENT_FILE_CACHESIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<String, Uri> eldest) {
                return size() > DOCUMENT_FILE_CACHESIZE;
            }
        });

        DocumentFolderAccessor(@NonNull final Context context) {
            super(context);
            refreshUriPermissionCache();
        }

        @Override
        public boolean delete(@NonNull final Uri uri) throws IOException {
            removeFromUriCache(uri); //very important to prevent IllegalArgumentExceptions due to nonexisting Uris!
            try {
                return DocumentsContract.deleteDocument(getContext().getContentResolver(), uri);
            } catch (IllegalArgumentException iae) {
                //this happens if uri is invalid, e.g. because document or containing folder was deleted externally to c:geo
                Log.d("Exception on trying to delete '" + uri + "' (assuming it was invalid): " + iae);
                return false;
            }
        }

        @Override
        public Uri create(@NonNull final Folder folder, @NonNull final String name) throws IOException {
            try {
                return createInternal(folder, name, false);
            } catch (IllegalArgumentException iae) {
                //This can happen if the folder uri is illegal, e.g. because the folder was deleted outside c:geo in the meantime and our cache is outdated.
                //-> cleanup cache and try again
                return createInternal(folder, name, true);
            }
        }

        private Uri createInternal(@NonNull final Folder folder, @NonNull final String name, final boolean validateCache) throws IOException {
            final Uri folderUri = getFolderUri(folder, true, true, validateCache);
            if (folderUri == null) {
                return null;
            }
            final String docName = createUniqueFilename(name, queryDir(folderUri, new String[]{ DocumentsContract.Document.COLUMN_DISPLAY_NAME }, c -> c.getString(0)));
            try (ContextLogger cLog = new ContextLogger("DocumentFolderAccessor.create %s: %s", folder, name)) {
                //Do NOT pass a mimeType. It will then be selected based on the file suffix.
                return DocumentsContract.createDocument(getContext().getContentResolver(), folderUri, null, docName);
            }
        }

        @Override
        public FileInformation get(@NonNull final Folder folder, final String name) throws IOException {

            //this is very slow...
            for (FileInformation fi : list(folder)) {
                if (fi.name.equals(name)) {
                    return fi;
                }
            }
            return null;
        }

        @Override
        public List<FileInformation> list(@NonNull final Folder folder) throws IOException {
            try {
                return listInternal(folder, false);
            } catch (IllegalArgumentException iae) {
                return listInternal(folder, true);
            }
        }

        private List<FileInformation> listInternal(@NonNull final Folder folder, final boolean validateCache) throws IOException {
            final Uri dir = getFolderUri(folder, false, false, validateCache);
            if (dir == null) {
                return Collections.emptyList();
            }

            //using dir.listFiles() is FAR too slow. Thus we have to create an explicit query

            return queryDir(dir, new String[] {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE,

            }, c -> {
                final String documentId = c.getString(0);
                final String name = c.getString(1);
                final String mimeType = c.getString(2);
                final long lastMod = c.getLong(3);
                final long size = c.getLong(4);

                final boolean isDir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
                final Uri uri = DocumentsContract.buildDocumentUriUsingTree(dir, documentId);

                return new FileInformation(name, uri, isDir, isDir ? Folder.fromFolder(folder, name) : null, mimeType, size, lastMod);
            });
        }

        @Override
        public boolean ensureFolder(@NonNull final Folder folder, final boolean needsWrite) throws IOException {
            //we have to validate cache for this one, otherwise outdated entries may lead to a false-positive
            return getFolderUri(folder, needsWrite, true, true) != null;
        }

        @Override
        public Uri getUriForFolder(@NonNull final Folder folder) throws IOException {
            try {
                return getFolderUri(folder, false, false, false);
            } catch (IllegalArgumentException iae) {
                return getFolderUri(folder, false, false, true);
            }
        }

        @Override
        public String getName(@NonNull final Uri uri) throws IOException {
            return queryDoc(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, c -> c.getString(0));
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
        private Uri getFolderUri(final Folder folder, final boolean needsWrite, final boolean createIfNeeded, final boolean validateCache) throws IOException {
            if (folder == null) {
                return null;
            }

            try (ContextLogger cLog = new ContextLogger("DocumentFolderAccessor.getFolderUri: %s", folder)) {

                final Uri treeUri = folder.getBaseUri();
                if (!checkUriPermissions(treeUri, needsWrite)) {
                    return null;
                }
                final Uri baseUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));

                if (validateCache && !isValidDirectoryUri(baseUri, needsWrite)) {
                    return null;
                }
                cLog.add("got base");

                return getSubdirUri(baseUri, folder.getSubdirsToBase(), needsWrite, createIfNeeded, validateCache);
            }
        }

        private boolean isValidDirectoryUri(final Uri dirUri, final boolean needsWrite) throws IOException {
            try {
                return queryDoc(dirUri, new String[]{DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_FLAGS}, false, c ->
                    DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(0)) &&
                        (!needsWrite || (c.getInt(1) & DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0));
            } catch (IllegalArgumentException iae) {
                //this is thrown if dirUri does not exist
                return false;
            }
        }

        private Uri getSubdirUri(@NonNull final Uri baseUri, @NonNull final List<String> subdirs, final boolean needsWrite, final boolean createIfNeeded, final boolean validateCache) throws IOException {

            //try to find parent entries in cache
            Uri dir = baseUri;
            int subdirIdx = subdirs.size();
            while (subdirIdx > 0) {
                final String cacheKey = uriCacheKey(baseUri, subdirs, subdirIdx);
                final Uri cachedDir = findInUriCache(cacheKey, needsWrite, validateCache);
                if (cachedDir != null) {
                    dir = cachedDir;
                    break;
                }
                subdirIdx--;
            }

            //from cache entry work up and find/create missing dirs
            while (subdirIdx < subdirs.size()) {
                final String subfolderName = subdirs.get(subdirIdx);
                final Uri child = findCreateSubdirectory(dir, subfolderName, createIfNeeded);
                if (child == null) {
                    if (createIfNeeded) {
                        throw new IOException("Failed to create subdir " + subfolderName + " for dir " + dir + ": reason unknown");
                    } else {
                        return null;
                    }
                }
                dir = child;
                subdirIdx++;
            }

            putToUriCache(uriCacheKey(baseUri, subdirs, -1), dir);
            return dir;
        }

        private Uri findCreateSubdirectory(final Uri dirUri, final String dirName, final boolean createIfNotExisting) throws IOException {
            final List<Uri> result = queryDir(dirUri, new String[]{ DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE }, c -> {
                    if (dirName.equals(c.getString(1)) && DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2))) {
                        return DocumentsContract.buildDocumentUriUsingTree(dirUri, c.getString(0));
                    }
                    return null;

                });
            for (Uri uri : result) {
                if (uri != null) {
                    return uri;
                }
            }
            if (createIfNotExisting) {
                return DocumentsContract.createDocument(getContext().getContentResolver(), dirUri, DocumentsContract.Document.MIME_TYPE_DIR, dirName);
            }
            return null;
        }

        private <T> List<T> queryDir(final Uri dirUri, final String[] columns, final Func1<Cursor, T> consumer) throws IOException {

            if (dirUri == null) {
                return Collections.emptyList();
            }

            final List<T> result = new ArrayList<>();
            final ContentResolver resolver = getContext().getContentResolver();
            final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, DocumentsContract.getDocumentId(dirUri));

            Cursor c = null;
            try {
                c = resolver.query(childrenUri, columns, null, null, null);
                while (c.moveToNext()) {
                    result.add(consumer.call(c));
                }
                return result;
            } catch (IllegalArgumentException iae) {
                //this is thrown if dirUri does not exist (any more). Rethrow
                throw iae;
            } catch (Exception e) {
                throw new IOException("Failed dir query for '" + dirUri + "' cols [" + CollectionStream.of(columns).toJoinedString(",") + "]", e);
            } finally {
                closeQuietly(c);
            }

        }

        private <T> T queryDoc(final Uri docUri, final String[] columns, final T defaultValue, final Func1<Cursor, T> consumer) throws IOException {
            final ContentResolver resolver = getContext().getContentResolver();

            Cursor c = null;
            try {
                c = resolver.query(docUri, columns, null, null, null);
                if (c.moveToFirst()) {
                    return consumer.call(c);
                } else {
                    return defaultValue;
                }
            } catch (IllegalArgumentException iae) {
                //this is thrown if dirUri does not exist (any more). Rethrow
                throw iae;
            } catch (Exception e) {
                throw new IOException("Failed query for '" + docUri + "' cols [" + CollectionStream.of(columns).toJoinedString(",") + "]", e);
            } finally {
                closeQuietly(c);
            }
        }

        private String uriCacheKey(final Uri baseUri, final List<String> subdirs, final int max) {
            return UriUtils.getPseudoUriString(baseUri, subdirs, max);
        }

        private Uri findInUriCache(final String key, final boolean needsWrite, final boolean validateCache) throws IOException {
            synchronized (this.folderUriCache) {
                final Uri cacheEntry = this.folderUriCache.get(key);
                if (cacheEntry == null) {
                    return null;
                }

                if (validateCache && !isValidDirectoryUri(cacheEntry, needsWrite)) {
                    this.folderUriCache.remove(key);
                    return null;
                }
                return cacheEntry;
            }
        }

        private void putToUriCache(final String key, final Uri docUri) {
            this.folderUriCache.put(key, docUri);
        }

        private void removeFromUriCache(final Uri uri) {
            final String docUriToString = uri.toString();
            synchronized (this.folderUriCache) {
                final Iterator<Map.Entry<String, Uri>> it = this.folderUriCache.entrySet().iterator();
                while (it.hasNext()) {
                    if (it.next().getValue().toString().startsWith(docUriToString)) {
                        it.remove();
                    }
                }
            }
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
            for (PersistableFolder folder : PersistableFolder.values()) {
                if (folder.getFolder().getBaseUri() != null) {
                    usedUris.add(UriUtils.toCompareString(folder.getFolder().getBaseUri()));
                }
            }
            for (PersistableUri persistedUri : PersistableUri.values()) {
                if (persistedUri.getUri() != null) {
                    usedUris.add(UriUtils.toCompareString(persistedUri.getUri()));
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

    private ContentStorage() {
        this.context = CgeoApplication.getInstance().getApplicationContext();
        this.documentAccessor = new DocumentFolderAccessor(this.context);
        this.fileAccessor = new FileFolderAccessor(this.context);
        this.documentAccessor.refreshUriPermissionCache();

        //(re)sets default folders
        for (PersistableFolder folder : PersistableFolder.values()) {
            folder.setDefaultFolder(getAccessibleDefaultFolder(folder.getDefaultCandidates(), folder.needsWrite(), folder.name()));
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
            return getAccessorFor(folder).get(folder, name);
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
        if (uri == null) {
            return null;
        }

        try {
            return getAccessorFor(uri).getName(uri);
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
        if (uri == null) {
            return null;
        }

        try {
            return this.context.getContentResolver().openInputStream(uri);
        } catch (IOException | SecurityException se) {
            reportProblem(R.string.contentstorage_err_read_failed, se, uri);
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
        File outputDir = null;
        try {
            outputDir = context.getCacheDir(); // context being the Activity pointer
            return File.createTempFile("cgeo_tempfile_", ".tmp", outputDir);
        } catch (IOException ie) {
            reportProblem(R.string.contentstorage_err_create_failed, ie, "temp file", outputDir);
        }
        return null;
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
