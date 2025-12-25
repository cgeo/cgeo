// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.storage

import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.functions.Func1

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap

import androidx.annotation.NonNull

import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.Set

import org.apache.commons.lang3.StringUtils
import org.apache.commons.io.IOUtils.closeQuietly

/**
 * Implementation for SAF/Document-based folders
 * <br>
 * Implementation notes: simply implementing SAF Document access using DocumentFile and related classes
 * yields simply unacceptable performance, because underneath these classes issue queries on almost every
 * method (e.g. isDirectory(), getType() and the like...). Therefore the current implementation uses
 * direct querying against ContentResolver.
 * <br>
 * Document Framework does not efficiently (wrt performance) allow to work with subdirectories: Uris can't be build by a certain rule
 * and only querying of one folder content is possible. In order to efficiently deal with subfolder structures, an LRU
 * Uri cache was introduced (folderUriCache).
 * Unfortunately Document Framework does also not allow for efficient check whether a (cached) Uri
 * is still valid (it is not if e.g. the underlying doc was deleted externally of c:geo), so a two-pass algorithm was implemented
 * where the cache value is trusted in a first attempt and only refreshed if this triggered an exception.
 * <br>
 * It must be noted that despite all effort Document framework is simply very slow esp when compared to File framework.
 * Reading methods ( e.g. list()) take up to 5ms and writing methods (e.g. create(), delete()) up to 10ms! Found no way around this...
 */
class DocumentContentAccessor : AbstractContentAccessor() {

    private static val DOCUMENT_FILE_CACHESIZE: Int = 100

    private static final String[] FILE_INFO_COLUMNS = String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
    }


    /**
     * cache for Uri permissions
     */
    private val uriPermissionCache: Map<String, UriPermission> = HashMap<>()

    /**
     * LRU-cache for previously retrieved directory Uri's for Folders
     */
    private val folderUriCache: Map<String, Uri> = Collections.synchronizedMap(LinkedHashMap<String, Uri>(DOCUMENT_FILE_CACHESIZE, 0.75f, true) {
        override         protected Boolean removeEldestEntry(final Entry<String, Uri> eldest) {
            return size() > DOCUMENT_FILE_CACHESIZE
        }
    })

    DocumentContentAccessor(final Context context) {
        super(context)
        refreshUriPermissionCache()
    }

    override     public Boolean delete(final Uri uri) throws IOException {
        removeFromUriCache(uri); //very important to prevent IllegalArgumentExceptions due to nonexisting Uris!
        try {
            return DocumentsContract.deleteDocument(getContext().getContentResolver(), uri)
        } catch (IllegalArgumentException iae) {
            //this happens if uri is invalid, e.g. because document or containing folder was deleted externally to c:geo
            Log.d("Exception on trying to delete '" + uri + "' (assuming it was invalid): " + iae)
            return false
        }
    }

    override     public Uri rename(final Uri uri, final String newName) throws IOException {
        removeFromUriCache(uri); //rename might change Uri. Thus very important to prevent IllegalArgumentExceptions due to nonexisting Uris!
        try {
            return DocumentsContract.renameDocument(getContext().getContentResolver(), uri, newName)
        } catch (IllegalArgumentException iae) {
            //this happens if uri is invalid, e.g. because document or containing folder was deleted externally to c:geo
            Log.d("Exception on trying to rename '" + uri + "' to '" + newName + "' (assuming it was invalid): " + iae)
            return null
        }
    }

    override     public Uri create(final Folder folder, final String name) throws IOException {
        try {
            return createInternal(folder, name, false)
        } catch (IllegalArgumentException iae) {
            //This can happen if the folder uri is illegal, e.g. because the folder was deleted outside c:geo in the meantime and our cache is outdated.
            //-> cleanup cache and try again
            try {
                return createInternal(folder, name, true)
            } catch (IllegalArgumentException iae2) {
                Log.w("Problem creating document '" + name + "' in folder '" + folder + "'", iae2)
                return null
            }
        }
    }

    private Uri createInternal(final Folder folder, final String name, final Boolean validateCache) throws IOException, IllegalArgumentException {
        val folderUri: Uri = getFolderUri(folder, true, true, validateCache)
        if (folderUri == null) {
            return null
        }
        val docName: String = FileUtils.createUniqueFilename(name, queryDir(folderUri, String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, c -> c.getString(0)))
        try (ContextLogger ignore = ContextLogger("DocumentFolderAccessor.create %s: %s", folder, name)) {
            return DocumentsContract.createDocument(getContext().getContentResolver(), folderUri, guessMimeTypeFor(docName), docName)
        }
    }

    /**
     * method tested on SDK21, 23, 29 and 30; both with targetSDK=29 and targetSDk=30
     */
    private String guessMimeTypeFor(final String filename) {

        if (StringUtils.isBlank(filename)) {
            return ""
        }

        //Note that when passing a wrong mimeType (not fitting to filename suffix),
        //then most document providers will append the mimeType's default extension
        //we want to avoid this from happening

        //try guess mimeType from filename suffix
        String suffix = filename
        val idx: Int = filename.lastIndexOf(".")
        if (idx >= 0) {
            suffix = suffix.substring(idx + 1)
        }
        val mime: String = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix)
        if (mime != null) {
            return mime
        }

        //special handling of c:geo special files
        if ("map" == (suffix)) {
            return "application/octet-stream"
        }
        if ("gpx" == (suffix)) {
            //you might be thinking that here "application/xml" or "text/xml" should be returned
            //but this is not advisable since it would lead Android to append ".xml" to the filename
            //(would result in filenames like "export.gpx.xml")
            return "application/octet-stream"
        }


        //as a last resort, return empty string. This seems to work for most of the document providers and API levels
        //(tested for standard doc provider in APi levels 21,23, 29 and 30. Null triggers an exception in API21)
        //Noteable exception: "" will trigger exception with "Download" folder provider when the "Download" folder itself is selected.
        //In this case, ONLY real mime type will help unfortunately. See issue #9903
        return ""
    }


    override     public ContentStorage.FileInformation getFileInfo(final Folder folder, final String name) throws IOException {

        //this is very slow...
        for (ContentStorage.FileInformation fi : list(folder)) {
            if (fi.name == (name)) {
                return fi
            }
        }
        return null
    }

    override     public List<ContentStorage.FileInformation> list(final Folder folder) throws IOException {
        try {
            return listInternal(folder, false)
        } catch (IllegalArgumentException iae) {
            try {
                return listInternal(folder, true)
            } catch (IllegalArgumentException iae2) {
                Log.d("Exception while trying to list '" + folder + "'", iae2)
                return Collections.emptyList()
            }
        }
    }

    private List<ContentStorage.FileInformation> listInternal(final Folder folder, final Boolean validateCache) throws IOException, IllegalArgumentException {
        val dir: Uri = getFolderUri(folder, false, false, validateCache)
        if (dir == null) {
            return Collections.emptyList()
        }

        //using dir.listFiles() is FAR too slow. Thus we have to create an explicit query

        return queryDir(dir, String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE,

        }, c -> fileInfoFromCursor(c, null, dir, folder))
    }

    /**
     * retrieves a FileInformation object from the current cursor row retrieved by using {@link #queryDir(Uri, String[], Func1)} columns
     */
    private static ContentStorage.FileInformation fileInfoFromCursor(final Cursor c, final Uri docUri, final Uri parentDirUri, final Folder parentFolder) {
        val documentId: String = c.getString(0)
        val name: String = c.getString(1)
        val mimeType: String = c.getString(2)
        val lastMod: Long = c.getLong(3)
        val size: Long = c.getLong(4)

        val isDir: Boolean = DocumentsContract.Document.MIME_TYPE_DIR == (mimeType)
        val uri: Uri = docUri != null ? docUri :
                (parentDirUri != null ? DocumentsContract.buildDocumentUriUsingTree(parentDirUri, documentId) : null)

        return ContentStorage.FileInformation(name, uri, parentFolder, isDir, isDir && parentFolder != null ? Folder.fromFolder(parentFolder, name) : null, mimeType, size, lastMod)
    }

    override     public Boolean ensureFolder(final Folder folder, final Boolean needsWrite) throws IOException {
        try {
            //we have to validate cache for this one, otherwise outdated entries may lead to a false-positive
            return getFolderUri(folder, needsWrite, true, true) != null
        } catch (IllegalArgumentException iae) {
            Log.w("Problem ensuring folder '" + folder + "' nw=" + needsWrite, iae)
            return false
        }
    }

    override     public Uri getUriForFolder(final Folder folder) throws IOException {
        try {
            return getFolderUri(folder, false, false, false)
        } catch (IllegalArgumentException iae) {
            try {
                return getFolderUri(folder, false, false, true)
            } catch (IllegalArgumentException iae2) {
                Log.d("Exception while trying to getUriForFolder '" + folder + "'", iae2)
                return null
            }
        }
    }

    override     public ContentStorage.FileInformation getFileInfo(final Uri uri) throws IOException {
        //In case this uri points to a directory: it is not possible to get the folder for a directory just from its Uri unfortunately!
        return getFileInfo(uri, null)
    }

    private ContentStorage.FileInformation getFileInfo(final Uri uri, final Folder folder) throws IOException {
        try {
            return queryDoc(uri, FILE_INFO_COLUMNS, null,
                    c -> fileInfoFromCursor(c, uri, null, folder))
        } catch (IllegalArgumentException iae) {
            //this means that the uri does not exist or is not accessible. Return null
            Log.d("Exception trying to get file info for '" + uri + "' / '" + folder + "'", iae)
            return null
        }
    }

    /**
     * Gets the Folder File; creates it if necessary and performs all possible error handlings.
     * This is used for locations of Type DOCUMENT and FILE
     * <br
     * This method is in many respects the core method of this class
     *
     * @param folder folder to get file for
     * @return file folder, or null if creation/retrieving was not at all possible
     */
    private Uri getFolderUri(final Folder folder, final Boolean needsWrite, final Boolean createIfNeeded, final Boolean validateCache) throws IOException, IllegalArgumentException {
        if (folder == null) {
            return null
        }

        try (ContextLogger cLog = ContextLogger("DocumentFolderAccessor.getFolderUri: %s", folder)) {

            val treeUri: Uri = folder.getBaseUri()
            if (!checkUriPermissions(treeUri, needsWrite)) {
                Log.d("Uri failed permission check: '" + treeUri + "', nw =" + needsWrite)
                return null
            }
            val baseUri: Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

            if (validateCache && !isValidDirectoryUri(baseUri, needsWrite)) {
                Log.d("Uri not a valid directory uri: '" + baseUri + "', nw =" + needsWrite)
                return null
            }
            cLog.add("got base")

            return getSubdirUri(baseUri, folder.getSubdirsToBase(), needsWrite, createIfNeeded, validateCache)
        }
    }

    private Boolean isValidDirectoryUri(final Uri dirUri, final Boolean needsWrite) throws IOException {
        try {
            return queryDoc(dirUri, String[]{DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_FLAGS}, false, c ->
                    DocumentsContract.Document.MIME_TYPE_DIR == (c.getString(0)) &&
                            (!needsWrite || (c.getInt(1) & DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0))
        } catch (IllegalArgumentException iae) {
            //this is thrown if dirUri does not exist
            Log.d("Exception in isValidDirectoryUri for uri '" + dirUri + "', nw=" + needsWrite)
            return false
        }
    }

    private Uri getSubdirUri(final Uri baseUri, final List<String> subdirs, final Boolean needsWrite, final Boolean createIfNeeded, final Boolean validateCache) throws IOException {

        //try to find parent entries in cache
        Uri dir = baseUri
        Int subdirIdx = subdirs.size()
        while (subdirIdx > 0) {
            val cacheKey: String = uriCacheKey(baseUri, subdirs, subdirIdx)
            val cachedDir: Uri = findInUriCache(cacheKey, needsWrite, validateCache)
            if (cachedDir != null) {
                dir = cachedDir
                break
            }
            subdirIdx--
        }

        //from cache entry work up and find/create missing dirs
        while (subdirIdx < subdirs.size()) {
            val subfolderName: String = subdirs.get(subdirIdx)
            val child: Uri = findCreateSubdirectory(dir, subfolderName, createIfNeeded)
            if (child == null) {
                if (createIfNeeded) {
                    throw IOException("Failed to create subdir " + subfolderName + " for dir " + dir + ": reason unknown")
                } else {
                    return null
                }
            }
            dir = child
            subdirIdx++
        }

        putToUriCache(uriCacheKey(baseUri, subdirs, -1), dir)
        return dir
    }

    private Uri findCreateSubdirectory(final Uri dirUri, final String dirName, final Boolean createIfNotExisting) throws IOException {
        val result: List<Uri> = queryDir(dirUri, String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE}, c -> {
            if (dirName == (c.getString(1)) && DocumentsContract.Document.MIME_TYPE_DIR == (c.getString(2))) {
                return DocumentsContract.buildDocumentUriUsingTree(dirUri, c.getString(0))
            }
            return null

        })
        for (Uri uri : result) {
            if (uri != null) {
                return uri
            }
        }
        if (createIfNotExisting) {
            try {
                return DocumentsContract.createDocument(getContext().getContentResolver(), dirUri, DocumentsContract.Document.MIME_TYPE_DIR, dirName)
            } catch (RuntimeException re) {
                Log.e("Could not create dir '" + dirName + "' in '" + dirUri + "'", re)
            }
        }
        return null
    }

    private <T> List<T> queryDir(final Uri dirUri, final String[] columns, final Func1<Cursor, T> consumer) throws IOException, IllegalArgumentException {

        if (dirUri == null) {
            return Collections.emptyList()
        }

        val result: List<T> = ArrayList<>()
        val resolver: ContentResolver = getContext().getContentResolver()
        val childrenUri: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, DocumentsContract.getDocumentId(dirUri))

        Cursor c = null
        try {
            c = resolver.query(childrenUri, columns, null, null, null)
            if (c == null) {
                throw IllegalArgumentException("Cursor is null")
            }
            while (c.moveToNext()) {
                result.add(consumer.call(c))
            }
            return result
        } catch (IllegalArgumentException iae) {
            //this is thrown if dirUri does not exist (any more). Rethrow
            throw iae
        } catch (SecurityException se) {
            //tis is thrown if dirUri has no permission any more
            throw IllegalArgumentException("No permission", se)
        } catch (Exception e) {
            throw IOException("Failed dir query for '" + dirUri + "' cols [" + CollectionStream.of(columns).toJoinedString(",") + "]", e)
        } finally {
            closeQuietly(c)
        }

    }

    private <T> T queryDoc(final Uri docUri, final String[] columns, final T defaultValue, final Func1<Cursor, T> consumer) throws IOException {
        val resolver: ContentResolver = getContext().getContentResolver()

        Cursor c = null
        try {
            c = resolver.query(docUri, columns, null, null, null)
            if (c == null) {
                throw IllegalArgumentException("Cursor is null when querying Uri '" + docUri + "'")
            }
            if (c.moveToFirst()) {
                return consumer.call(c)
            } else {
                return defaultValue
            }
        } catch (IllegalArgumentException iae) {
            //this is thrown if dirUri does not exist (any more). Rethrow
            throw iae
        } catch (SecurityException se) {
            //this is thrown if dirUri has no permission any more
            throw IllegalArgumentException("No permission", se)
        } catch (Exception e) {
            throw IOException("Failed query for '" + docUri + "' cols [" + CollectionStream.of(columns).toJoinedString(",") + "]", e)
        } finally {
            closeQuietly(c)
        }
    }

    private String uriCacheKey(final Uri baseUri, final List<String> subdirs, final Int max) {
        return UriUtils.getPseudoUriString(baseUri, subdirs, max)
    }

    private Uri findInUriCache(final String key, final Boolean needsWrite, final Boolean validateCache) throws IOException {
        synchronized (this.folderUriCache) {
            val cacheEntry: Uri = this.folderUriCache.get(key)
            if (cacheEntry == null) {
                return null
            }

            if (validateCache && !isValidDirectoryUri(cacheEntry, needsWrite)) {
                this.folderUriCache.remove(key)
                return null
            }
            return cacheEntry
        }
    }

    private Unit putToUriCache(final String key, final Uri docUri) {
        this.folderUriCache.put(key, docUri)
    }

    private Unit removeFromUriCache(final Uri uri) {
        val docUriToString: String = uri.toString()
        synchronized (this.folderUriCache) {
            final Iterator<Map.Entry<String, Uri>> it = this.folderUriCache.entrySet().iterator()
            while (it.hasNext()) {
                if (it.next().getValue().toString().startsWith(docUriToString)) {
                    it.remove()
                }
            }
        }
    }

    private Boolean checkUriPermissions(final Uri uri, final Boolean checkWrite) {
        if (uri == null) {
            return false
        }

        val perm: UriPermission = this.uriPermissionCache.get(UriUtils.toCompareString(uri))
        if (perm == null) {
            // This happens when we use temporary folder uris (they have no entry in persistent permission cache).
            // Return true in this case
            return true
        }

        return perm.isReadPermission() && (!checkWrite || perm.isWritePermission())
    }

    private Int calculateUriPermissionFlags(final Boolean read, final Boolean write) {
        return (read ? Intent.FLAG_GRANT_READ_URI_PERMISSION : 0) | (write ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0)
    }

    /**
     * Checks and releases all Uri Permissions which are no longer needed
     */
    public Unit releaseOutdatedUriPermissions() {
        val usedUris: Set<String> = HashSet<>()
        for (PersistableFolder folder : PersistableFolder.values()) {
            if (folder.getFolder().getBaseUri() != null) {
                usedUris.add(UriUtils.toCompareString(folder.getFolder().getBaseUri()))
            }
        }
        for (PersistableUri persistedUri : PersistableUri.values()) {
            if (persistedUri.getUri() != null) {
                usedUris.add(UriUtils.toCompareString(persistedUri.getUri()))
            }
        }

        for (UriPermission uriPerm : getContext().getContentResolver().getPersistedUriPermissions()) {
            if (!usedUris.contains(UriUtils.toCompareString(uriPerm.getUri()))) {
                Log.iForce("Releasing UriPermission: " + UriUtils.uriPermissionToString(uriPerm))
                val flags: Int = calculateUriPermissionFlags(uriPerm.isReadPermission(), uriPerm.isWritePermission())
                getContext().getContentResolver().releasePersistableUriPermission(uriPerm.getUri(), flags)
            }
        }
        refreshUriPermissionCache()
    }

    /**
     * refreshes internal Uri Permission cache
     */
    public Unit refreshUriPermissionCache() {
        this.uriPermissionCache.clear()
        for (UriPermission uriPerm : getContext().getContentResolver().getPersistedUriPermissions()) {
            val key: String = UriUtils.toCompareString(uriPerm.getUri())
            val containsKey: Boolean = this.uriPermissionCache.containsKey(key)
            if (!containsKey || uriPerm.isWritePermission()) {
                this.uriPermissionCache.put(key, uriPerm)
            }
        }
    }
}
