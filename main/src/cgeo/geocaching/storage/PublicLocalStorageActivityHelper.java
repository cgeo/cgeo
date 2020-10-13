package cgeo.geocaching.storage;

import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;

import androidx.core.util.Consumer;

/**
 * Important: this class will oly work if you incorporate {@link #onActivityResult(int, int, Intent)}
 * into the {@link Activity#onActivityResult(int, int, Intent)} method of the using application!
 * TODO: once Activity ResultAPI is available -> refactor! Watch #9349
 */
public class PublicLocalStorageActivityHelper {

    //use a globally unique request code to mix-in with Activity.onActivityResult. (This will no longer be neccessary with Activity Result API)
    //code must be positive (>0) and <=65535 (restriction of SDK21)
    public static final int REQUEST_CODE_GRANT_URI_ACCESS = 59371; //this is a random number

    private final Activity activity;

    //stores intermediate data of a running intent. (This will no longer be neccessary with Activity Result API)
    private IntentData runningIntentData;

    private static class IntentData {
        public final PublicLocalFolder folder;
        public final Consumer<PublicLocalFolder> callback;

        IntentData(final PublicLocalFolder folder, final Consumer<PublicLocalFolder> callback) {
            this.folder = folder;
            this.callback = callback;
        }
    }

    public PublicLocalStorageActivityHelper(final Activity activity) {
        this.activity = activity;
    }

    /** convenience version of {@link #checkAndGrantFolderAccess(PublicLocalFolder, boolean, Consumer)}.
     * Call this method in {@link Activity#onCreate(Bundle)} with all PublicLocalFolders this Activity MIGHT need to access.
     */
    public void checkAndGrantFolderAccess(final PublicLocalFolder ... folders) {
        for (PublicLocalFolder folder : folders) {
            checkAndGrantFolderAccess(folder, true, null);
        }
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
    private boolean checkAndGrantFolderAccess(final PublicLocalFolder folder, final boolean requestGrantIfNecessary, final Consumer<PublicLocalFolder> callback) {

        if (PublicLocalStorage.get().checkAvailability(folder)) {
            return true;
        }
        if (requestGrantIfNecessary) {
            //TODO: internationalize, cleanup, ...
            new AlertDialog.Builder(this.activity)
                .setTitle("Select and grant access to folder")
                .setMessage("Please select and grant read/write access to parent folder for subfolder '" + folder.getFolderName() + "'")
                .setPositiveButton("OK", (d, p) -> {
                    // call for document tree dialog
                    final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | (folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0) | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    Log.i("Start uri dir: " + folder.getBaseUri());
                    final Uri startUri = folder.getBaseUri();
                    if (startUri != null) {
                        // Note: on SDK21, setting DocumentsContract.EXTRA_INITIAL_URI to either null
                        // OR Uri.fromFile(LocalStorage.getExternalPublicCgeoDirectory()) leads to doc dialog not working in emulator
                        // (Pixel 4 API 21)
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, startUri);
                    }


                    runningIntentData = new IntentData(folder, callback);

                    ((Activity) this.activity).startActivityForResult(intent, REQUEST_CODE_GRANT_URI_ACCESS);
                }).create().show();
        }
        return false;
    }

    /** You MUST include in {@link Activity#onActivityResult(int, int, Intent)} of using Activity */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == REQUEST_CODE_GRANT_URI_ACCESS) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                final Uri uri = intent.getData();
                if (uri != null && runningIntentData != null) {
                    final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | (runningIntentData.folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
                    activity.getContentResolver().takePersistableUriPermission(uri, flags);
                    Log.e("permissions: " + uri.getPath());
                    runningIntentData.folder.setBaseUri(uri);
                    if (runningIntentData.callback != null) {
                        runningIntentData.callback.accept(runningIntentData.folder);
                    }
                    runningIntentData = null;
                }
            }
            return true;
        }
        return false;
    }

}
