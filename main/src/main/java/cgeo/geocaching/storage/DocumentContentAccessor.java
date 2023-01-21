package cgeo.geocaching.storage;

import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.io.IOUtils.closeQuietly;

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
class DocumentContentAccessor extends AbstractContentAccessor {

    private static final int DOCUMENT_FILE_CACHESIZE = 100;

    private static final String[] FILE_INFO_COLUMNS = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
    };


    /**
     * cache for Uri permissions
     */
    private final Map<String, UriPermission> uriPermissionCache = new HashMap<>();

    /**
     * LRU-cache for previously retrieved directory Uri's for Folders
     */
    private final Map<String, Uri> folderUriCache = Collections.synchronizedMap(new LinkedHashMap<String, Uri>(DOCUMENT_FILE_CACHESIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Entry<String, Uri> eldest) {
            return size() > DOCUMENT_FILE_CACHESIZE;
        }
    });

    DocumentContentAccessor(@NonNull final Context context) {
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
    public Uri rename(@NonNull final Uri uri, @NonNull final String newName) throws IOException {
        removeFromUriCache(uri); //rename might change Uri. Thus very important to prevent IllegalArgumentExceptions due to nonexisting Uris!
        try {
            return DocumentsContract.renameDocument(getContext().getContentResolver(), uri, newName);
        } catch (IllegalArgumentException iae) {
            //this happens if uri is invalid, e.g. because document or containing folder was deleted externally to c:geo
            Log.d("Exception on trying to rename '" + uri + "' to '" + newName + "' (assuming it was invalid): " + iae);
            return null;
        }
    }

    @Override
    public Uri create(@NonNull final Folder folder, @NonNull final String name) throws IOException {
        try {
            return createInternal(folder, name, false);
        } catch (IllegalArgumentException iae) {
            //This can happen if the folder uri is illegal, e.g. because the folder was deleted outside c:geo in the meantime and our cache is outdated.
            //-> cleanup cache and try again
            try {
                return createInternal(folder, name, true);
            } catch (IllegalArgumentException iae2) {
                Log.w("Problem creating document '" + name + "' in folder '" + folder + "'", iae2);
                return null;
            }
        }
    }

    private Uri createInternal(@NonNull final Folder folder, @NonNull final String name, final boolean validateCache) throws IOException {
        final Uri folderUri = getFolderUri(folder, true, true, validateCache);
        if (folderUri == null) {
            return null;
        }
        final String docName = FileUtils.createUniqueFilename(name, queryDir(folderUri, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, c -> c.getString(0)));
        try (ContextLogger cLog = new ContextLogger("DocumentFolderAccessor.create %s: %s", folder, name)) {
            return DocumentsContract.createDocument(getContext().getContentResolver(), folderUri, guessMimeTypeFor(docName), docName);
        }
    }

    /**
     * method tested on SDK21, 23, 29 and 30; both with targetSDK=29 and targetSDk=30
     */
    private String guessMimeTypeFor(final String filename) {

        if (StringUtils.isBlank(filename)) {
            return "";
        }

        //Note that when passing a wrong mimeType (not fitting to filename suffix),
        //then most document providers will append the mimeType's default extension
        //we want to avoid this from happening

        //Android SDK21/22 replace file suffixes with their defaults from mimeType.
        //This leads to e.g. "image.jpg" to be renamed to "image.jpeg".
        //->prevent this by using octet-stream for all files
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return "application/octet-stream";
        }

        //try guess mimeType from filename suffix;
        String suffix = filename;
        final int idx = filename.lastIndexOf(".");
        if (idx >= 0) {
            suffix = suffix.substring(idx + 1);
        }
        final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (mime != null) {
            return mime;
        }

        //special handling of c:geo special files
        if ("map".equals(suffix)) {
            return "application/octet-stream";
        }
        if ("gpx".equals(suffix)) {
            //you might be thinking that here "application/xml" or "text/xml" should be returned
            //but this is not advisable since it would lead Android to append ".xml" to the filename
            //(would result in filenames like "export.gpx.xml")
            return "application/octet-stream";
        }


        //as a last resort, return empty string. This seems to work for most of the document providers and API levels
        //(tested for standard doc provider in APi levels 21,23, 29 and 30. Null triggers an exception in API21)
        //Noteable exception: "" will trigger exception with "Download" folder provider when the "Download" folder itself is selected.
        //In this case, ONLY real mime type will help unfortunately. See issue #9903
        return "";
    }


    @Override
    public ContentStorage.FileInformation getFileInfo(@NonNull final Folder folder, final String name) throws IOException {

        //this is very slow...
        for (ContentStorage.FileInformation fi : list(folder)) {
            if (fi.name.equals(name)) {
                return fi;
            }
        }
        return null;
    }

    @Override
    public List<ContentStorage.FileInformation> list(@NonNull final Folder folder) throws IOException {
        try {
            return listInternal(folder, false);
        } catch (IllegalArgumentException iae) {
            try {
                return listInternal(folder, true);
            } catch (IllegalArgumentException iae2) {
                Log.d("Exception while trying to list '" + folder + "'", iae2);
                return Collections.emptyList();
            }
        }
    }

    private List<ContentStorage.FileInformation> listInternal(@NonNull final Folder folder, final boolean validateCache) throws IOException {
        final Uri dir = getFolderUri(folder, false, false, validateCache);
        if (dir == null) {
            return Collections.emptyList();
        }

        //using dir.listFiles() is FAR too slow. Thus we have to create an explicit query

        return queryDir(dir, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE,

        }, c -> fileInfoFromCursor(c, null, dir, folder));
    }

    /**
     * retrieves a FileInformation object from the current cursor row retrieved by using {@Link } columns
     */
    private static ContentStorage.FileInformation fileInfoFromCursor(final Cursor c, final Uri docUri, final Uri parentDirUri, final Folder parentFolder) {
        final String documentId = c.getString(0);
        final String name = c.getString(1);
        final String mimeType = c.getString(2);
        final long lastMod = c.getLong(3);
        final long size = c.getLong(4);

        final boolean isDir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
        final Uri uri = docUri != null ? docUri :
                (parentDirUri != null ? DocumentsContract.buildDocumentUriUsingTree(parentDirUri, documentId) : null);

        return new ContentStorage.FileInformation(name, uri, isDir, isDir && parentFolder != null ? Folder.fromFolder(parentFolder, name) : null, mimeType, size, lastMod);
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
            try {
                return getFolderUri(folder, false, false, true);
            } catch (IllegalArgumentException iae2) {
                Log.d("Exception while trying to getUriForFolder '" + folder + "'", iae2);
                return null;
            }
        }
    }

    @Override
    public ContentStorage.FileInformation getFileInfo(@NonNull final Uri uri) throws IOException {
        //In case this uri points to a directory: it is not possible to get the folder for a directory just from its Uri unfortunately!
        return getFileInfo(uri, null);
    }

    private ContentStorage.FileInformation getFileInfo(@NonNull final Uri uri, final Folder folder) throws IOException {
        try {
            return queryDoc(uri, FILE_INFO_COLUMNS, null,
                    c -> fileInfoFromCursor(c, uri, null, folder));
        } catch (IllegalArgumentException iae) {
            //this means that the uri does not exist or is not accessible. Return null
            Log.d("Exception trying to get file info for '" + uri + "' / '" + folder + "'", iae);
            return null;
        }
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
                Log.d("Uri failed permission check: '" + treeUri + "', nw =" + needsWrite);
                return null;
            }
            final Uri baseUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));

            if (validateCache && !isValidDirectoryUri(baseUri, needsWrite)) {
                Log.d("Uri not a valid directory uri: '" + baseUri + "', nw =" + needsWrite);
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
            Log.d("Exception in isValidDirectoryUri for uri '" + dirUri + "', nw=" + needsWrite);
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
        final List<Uri> result = queryDir(dirUri, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE}, c -> {
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
            try {
                return DocumentsContract.createDocument(getContext().getContentResolver(), dirUri, DocumentsContract.Document.MIME_TYPE_DIR, dirName);
            } catch (RuntimeException re) {
                Log.e("Could not create dir '" + dirName + "' in '" + dirUri + "'", re);
            }
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
            if (c == null) {
                throw new IllegalArgumentException("Cursor is null");
            }
            while (c.moveToNext()) {
                result.add(consumer.call(c));
            }
            return result;
        } catch (IllegalArgumentException iae) {
            //this is thrown if dirUri does not exist (any more). Rethrow
            throw iae;
        } catch (SecurityException se) {
            //tis is thrown if dirUri has no permission any more
            throw new IllegalArgumentException("No permission", se);
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
            if (c == null) {
                throw new IllegalArgumentException("Cursor is null when querying Uri '" + docUri + "'");
            }
            if (c.moveToFirst()) {
                return consumer.call(c);
            } else {
                return defaultValue;
            }
        } catch (IllegalArgumentException iae) {
            //this is thrown if dirUri does not exist (any more). Rethrow
            throw iae;
        } catch (SecurityException se) {
            //this is thrown if dirUri has no permission any more
            throw new IllegalArgumentException("No permission", se);
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
            // This happens when we use temporary folder uris (they have no entry in persistent permission cache).
            // Return true in this case
            return true;
        }

        return perm.isReadPermission() && (!checkWrite || perm.isWritePermission());
    }

    private int calculateUriPermissionFlags(final boolean read, final boolean write) {
        return (read ? Intent.FLAG_GRANT_READ_URI_PERMISSION : 0) | (write ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
    }

    /**
     * Checks and releases all Uri Permissions which are no longer needed
     */
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

    /**
     * refreshes internal Uri Permission cache
     */
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
