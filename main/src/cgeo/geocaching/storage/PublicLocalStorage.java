package cgeo.geocaching.storage;

import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.core.util.Consumer;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

//Hackish code, trying out a few things
public class PublicLocalStorage {

    public static final int REQUEST_CODE_GRANT_URI_ACCESS = -582;

    private final Context context;
    private final ContentResolver contentResolver;

    private IntentData runningIntentData;

    private static class IntentData {
        public final PublicLocalFolder folder;
        public final Consumer<PublicLocalFolder> callback;

        IntentData(final PublicLocalFolder folder, final Consumer<PublicLocalFolder> callback) {
            this.folder = folder;
            this.callback = callback;
        }
    }

    public PublicLocalStorage(final Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    /**
     * Checks and (if necessary) asks for grant of a read/write permission for a folder. Note that asking for grants is only possible
     * when the context associated with this instance is an Activity AND in this Activities {@link Activity#onActivityResult(int, int, Intent)} method
     * does call {@link #onActivityResult(int, int, Intent)}
     * @param folder folder to ask/grant access for
     * @param requestGrantIfNecessary if true and folder does NOT have necessary grants, then these grants are requested
     * @param callback if grants are requested, then this callback is called when request is granted
     * @return true if grant is already available, false otherwise
     */

    public boolean checkAndGrantFolderAccess(final PublicLocalFolder folder, final boolean requestGrantIfNecessary, final Consumer<PublicLocalFolder> callback) {

        if (checkUriPermissions(folder.getBaseUri(), folder.needsWrite())) {
            return true;
        }
        if (requestGrantIfNecessary) {
            if (!(this.context instanceof Activity)) {
                //TODO: do something better
                throw new IllegalArgumentException("Programming error: context MUST be activity to start intent!");
            }
            //TODO: internationalize, cleanup, ...
            new AlertDialog.Builder(this.context)
                .setTitle("Select and grant access to folder")
                .setMessage("Please select and grant read/write access to parent folder for subfolder '" + folder.getFolderName() + "'")
                .setPositiveButton("OK", (d, p) -> {
                    // call for document tree dialog
                    final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.addFlags(
                        (folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : Intent.FLAG_GRANT_READ_URI_PERMISSION) |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    Log.e("Start uri dir: " + folder.getBaseUri());
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, folder.getBaseUri());

                    runningIntentData = new IntentData(folder, callback);
                    ((Activity) this.context).startActivityForResult(intent, REQUEST_CODE_GRANT_URI_ACCESS);
                }).create().show();
        }
        return false;
    }

    /** include in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)} of using Activity */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == REQUEST_CODE_GRANT_URI_ACCESS) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                final Uri uri = intent.getData();
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    Log.e("permissions: " + uri.getPath());
                    if (runningIntentData != null) {
                        runningIntentData.folder.setBaseUri(uri);
                        if (runningIntentData.callback != null) {
                            runningIntentData.callback.accept(runningIntentData.folder);
                        }
                    }
                    runningIntentData = null;
                }
            }
            return true;
        }
        return false;
    }

    private Uri getFolderUri(final PublicLocalFolder folder) {
        final Uri baseUri = folder.getBaseUri();
        if (baseUri == null || !checkUriPermissions(baseUri, folder.needsWrite())) {
            return null;
        }

        final DocumentFile cgeoDir = DocumentFile.fromTreeUri(context, baseUri);
        if (!cgeoDir.isDirectory()) {
            return null;
        }
        if (folder.getFolderName() == null) {
            return cgeoDir.getUri();
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

    private boolean checkUriPermissions(final Uri uri, final boolean checkWrite) {
        for (UriPermission up : contentResolver.getPersistedUriPermissions()) {
            if (up.getUri().equals(uri)) {
                final boolean hasAdequateRights = (up.isReadPermission()) && (!checkWrite || up.isWritePermission());
                if (!hasAdequateRights) {
                    return false;
                }
                final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | (checkWrite ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
                contentResolver.takePersistableUriPermission(uri, flags);
                return true;
            }
        }
        return false;
    }

    /** Write something to external storage */
    public boolean writeToStorage(final PublicLocalFolder folder, final String name, final InputStream in) {
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

    //TODO: methods for reading file lists and single files

}
