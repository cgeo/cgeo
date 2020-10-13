package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
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
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Central class to interact with locally stored PUBLIC folders.
 * Encapsulates the Android SAF framework.
 *
 * Note that methods of this class do not ask user for access permissions. This is done using
 * {@link PublicLocalStorageActivityHelper} in conjunction with c:geos Ativities.
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

    private DocumentFile getFolderFile(final PublicLocalFolder folder) {

        if (!checkUriPermissions(folder)) {
            //if this is a user-selected folder and base dir is ok we initiate a fallback to default folder
            if (!folder.isUserDefinedLocation() || checkUriPermissions(PublicLocalFolder.BASE_DIR)) {
                return null;
            }

            //TODO: internationalize!
            toast("Switching dir " + folder + " from " + folder.getUserDisplayableUri() + " to default");
            folder.setUri(null);
            if (!checkUriPermissions(folder)) {
                return null;
            }
        }

        final DocumentFile baseDir = DocumentFile.fromTreeUri(context, folder.getBaseUri());
        if (baseDir == null || !baseDir.isDirectory()) {
            return null;
        }
        if (folder.getSubfolder() == null) {
            return baseDir;
        }
        for (DocumentFile child : baseDir.listFiles()) {
            if (folder.getSubfolder().equals(child.getName())) {
                if (!child.isDirectory()) {
                    return null;
                }
                return child;
            }
        }
        return baseDir.createDirectory(folder.getSubfolder());
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
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(tempFile);
            out = openForWrite(create(folder, name));
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
        try {
            final DocumentFile folderFile = getFolderFile(folder);
            if (folderFile == null) {
                return null;
            }
            final String docName = name == null ? folder.createNewFilename() : name;
            return DocumentsContract.createDocument(context.getContentResolver(), folderFile.getUri(), folder.getDefaultMimeType(), docName);

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

    private void toast(final String message) {
        ActivityMixin.showToast(context, message);
    }

    //some helper methods copied from stackoverflow: https://stackoverflow.com/questions/34927748/android-5-0-documentfile-from-tree-uri
    //might come in handy later

//
//        private static final String PRIMARY_VOLUME_NAME = "primary";
//
//        @Nullable
//        public static String getFullPathFromTreeUri(@Nullable final Uri treeUri, Context con) {
//            if (treeUri == null) return null;
//            String volumePath = getVolumePath(getVolumeIdFromTreeUri(treeUri),con);
//            if (volumePath == null) return File.separator;
//            if (volumePath.endsWith(File.separator))
//                volumePath = volumePath.substring(0, volumePath.length() - 1);
//
//            String documentPath = getDocumentPathFromTreeUri(treeUri);
//            if (documentPath.endsWith(File.separator))
//                documentPath = documentPath.substring(0, documentPath.length() - 1);
//
//            if (documentPath.length() > 0) {
//                if (documentPath.startsWith(File.separator))
//                    return volumePath + documentPath;
//                else
//                    return volumePath + File.separator + documentPath;
//            }
//            else return volumePath;
//        }
//
//
//        private static String getVolumePath(final String volumeId, Context context) {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
//                return null;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
//                return getVolumePathForAndroid11AndAbove(volumeId, context);
//            else
//                return getVolumePathBeforeAndroid11(volumeId, context);
//        }
//
//
//        private static String getVolumePathBeforeAndroid11(final String volumeId, Context context){
//            try {
//                StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
//                Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
//                Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
//                Method getUuid = storageVolumeClazz.getMethod("getUuid");
//                Method getPath = storageVolumeClazz.getMethod("getPath");
//                Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
//                Object result = getVolumeList.invoke(mStorageManager);
//
//                final int length = Array.getLength(result);
//                for (int i = 0; i < length; i++) {
//                    Object storageVolumeElement = Array.get(result, i);
//                    String uuid = (String) getUuid.invoke(storageVolumeElement);
//                    Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);
//
//                    if (primary && PRIMARY_VOLUME_NAME.equals(volumeId))    // primary volume?
//                        return (String) getPath.invoke(storageVolumeElement);
//
//                    if (uuid != null && uuid.equals(volumeId))    // other volumes?
//                        return (String) getPath.invoke(storageVolumeElement);
//                }
//                // not found.
//                return null;
//            } catch (Exception ex) {
//                return null;
//            }
//        }
//
//        @TargetApi(Build.VERSION_CODES.R)
//        private static String getVolumePathForAndroid11AndAbove(final String volumeId, Context context) {
//            try {
//                StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
//                List<StorageVolume> storageVolumes = mStorageManager.getStorageVolumes();
//                for (StorageVolume storageVolume : storageVolumes) {
//                    // primary volume?
//                    if (storageVolume.isPrimary() && PRIMARY_VOLUME_NAME.equals(volumeId))
//                        return storageVolume.getDirectory().getPath();
//
//                    // other volumes?
//                    String uuid = storageVolume.getUuid();
//                    if (uuid != null && uuid.equals(volumeId))
//                        return storageVolume.getDirectory().getPath();
//
//                }
//                // not found.
//                return null;
//            } catch (Exception ex) {
//                return null;
//            }
//        }
//
//        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//        private static String getVolumeIdFromTreeUri(final Uri treeUri) {
//            final String docId = DocumentsContract.getTreeDocumentId(treeUri);
//            final String[] split = docId.split(":");
//            if (split.length > 0) return split[0];
//            else return null;
//        }
//
//
//        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//        private static String getDocumentPathFromTreeUri(final Uri treeUri) {
//            final String docId = DocumentsContract.getTreeDocumentId(treeUri);
//            final String[] split = docId.split(":");
//            if ((split.length >= 2) && (split[1] != null)) return split[1];
//            else return File.separator;
//        }


}
