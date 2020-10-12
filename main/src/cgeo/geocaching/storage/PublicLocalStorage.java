package cgeo.geocaching.storage;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

//Hackish code, trying out a few things
public class PublicLocalStorage {

    public static final int REQUEST_CODE = -582;

    private final Context context;
    private final ContentResolver contentResolver;

    private Uri baseUri;

    public PublicLocalStorage(final Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                final Uri uri = intent.getData();
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    Log.e("permissions: " + uri.getPath());
                    Settings.setBaseDir(uri);
                }
            }
            return true;
        }
        return false;
    }

    private Uri getFolderUri(final LocalFolder folder) {
        if (baseUri == null) {
            baseUri = Settings.getBaseDir();
        }
        if (baseUri == null || !takeUriPermissions(baseUri, true, true)) {
            return null;
        }

        final DocumentFile cgeoDir = DocumentFile.fromTreeUri(context, baseUri);
        if (!cgeoDir.isDirectory()) {
            return null;
        }
        for (DocumentFile child : cgeoDir.listFiles()) {
            if (child.getName().equals(folder.getFolderName())) {
                if (!child.isDirectory()) {
                    return null;
                }
                return child.getUri();
            }
        }
        return cgeoDir.createDirectory(folder.getFolderName()).getUri();
    }

    private boolean takeUriPermissions(final Uri uri, final boolean checkRead, final boolean checkWrite) {
        for (UriPermission up : contentResolver.getPersistedUriPermissions()) {
            if (up.getUri().equals(uri)) {
                final boolean hasAdequateRights = (!checkRead || up.isReadPermission()) && (!checkWrite || up.isWritePermission());
                if (!hasAdequateRights) {
                    return false;
                }
                final int flags = (checkRead ? Intent.FLAG_GRANT_READ_URI_PERMISSION : 0) | (checkWrite ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
                contentResolver.takePersistableUriPermission(uri, flags);
                return true;
            }
        }
        //MediaStore.Images.Media.INTERNAL_CONTENT_URI
        return false;
    }

    public boolean hasUriPermission(final Uri uri, final boolean checkRead, final boolean checkWrite) {
        return false;
    }

    public boolean writeToStorage(final LocalFolder folder, final String name, final InputStream in) {
        OutputStream out = null;
        try {
            final Uri folderUri = getFolderUri(folder);
            final Uri newDoc = DocumentsContract.createDocument(contentResolver, folderUri, folder.getDefaultMimeType(), name);
            out = contentResolver.openOutputStream(newDoc);
            IOUtils.copy(in, out);
        } catch (IOException ioe) {
            Log.w("Problem copying", ioe);
            return false;
        } finally {
            IOUtils.closeQuietly(in, out);
        }
        return true;
    }


}
