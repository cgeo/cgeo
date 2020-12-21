package cgeo.geocaching.storage;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;

import androidx.annotation.StringRes;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Consumer;

/**
 * Important: this class will oly work if you incorporate {@link #onActivityResult(int, int, Intent)}
 * into the {@link Activity#onActivityResult(int, int, Intent)} method of the using application!
 * TODO: once Activity ResultAPI is available -> refactor! Watch #9349
 */
public class PublicLocalStorageActivityHelper {

    //use a globally unique request code to mix-in with Activity.onActivityResult. (This will no longer be neccessary with Activity Result API)
    //code must be positive (>0) and <=65535 (restriction of SDK21)
    public static final int REQUEST_CODE_GRANT_FOLDER_URI_ACCESS = 59371; //this is a random number
    public static final int REQUEST_CODE_SELECT_FILE = 59372; //this is a random number

    private final Activity activity;

    //stores intermediate data of a running intent. (This will no longer be neccessary with Activity Result API)
    private IntentData<?> runningIntentData;

    private static class IntentData<T> {
        public final PublicLocalFolder folder;
        public final Consumer<T> callback;

        IntentData(final PublicLocalFolder folder, final Consumer<T> callback) {
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
    public void checkAndGrantBaseFolderAccess() {
        checkAndGrantFolderAccess(
            PublicLocalFolder.BASE_DIR,
            true,
            R.string.publiclocalstorage_grantaccess_dialog_msg_basedir_html,
            //check after setting whether access is now granted. If not, repeat...
            folder -> checkAndGrantBaseFolderAccess());
    }

    /**
     * Checks and (if necessary) asks for grant of a read/write permission for a folder. Note that asking for grants is only possible
     * when the context associated with this instance is an Activity AND in this Activities {@link Activity#onActivityResult(int, int, Intent)} method
     * does call {@link #onActivityResult(int, int, Intent)}
     * @param folder folder to ask/grant access for
     * @param requestGrantIfNecessary if true and folder does NOT have necessary grants, then these grants are requested
     * @param detailMessageHtml resourceid for a HTML message to display as explaining message to the user
     * @param callback if grants are requested, then this callback is called when request is granted
     * @return true if grant is already available, false otherwise
     */
    private boolean checkAndGrantFolderAccess(final PublicLocalFolder folder, final boolean requestGrantIfNecessary, @StringRes  final int detailMessageHtml, final Consumer<PublicLocalFolder> callback) {

        if (PublicLocalStorage.get().checkAvailability(folder)) {
            return true;
        }
        if (requestGrantIfNecessary) {
            final AlertDialog dialog = Dialogs.newBuilder(activity)
                .setTitle(R.string.publiclocalstorage_grantaccess_dialog_title)
                .setMessage(HtmlCompat.fromHtml(activity.getString(detailMessageHtml), HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(android.R.string.ok, (d, p) -> {
                    d.dismiss();
                    selectFolderUri(folder, callback);
                })
                .create();
            dialog.show();
            Dialogs.makeLinksClickable(dialog);
        }
        return false;
    }

    public void selectFolderUri(final PublicLocalFolder folder, final Consumer<PublicLocalFolder> callback) {
        //if this is not the base dir, user may choose to use default dir or user-selected dir
        if (folder.canUseDefault()) {
            Dialogs.newBuilder(activity)
                .setTitle(R.string.publiclocalstorage_selectfolder_dialog_user_or_default_title)
                .setMessage(activity.getString(R.string.publiclocalstorage_selectfolder_dialog_user_or_default_msg, folder.getDefaultFolderUserDisplayableUri()))
                .setPositiveButton(R.string.publiclocalstorage_userdefined, (d, p) -> {
                    d.dismiss();
                    selectUserFolderUri(folder, callback);
                })
                .setNegativeButton(R.string.publiclocalstorage_default, (d, p) -> {
                    d.dismiss();
                    folder.setUri(null);
                    if (callback != null) {
                        callback.accept(folder);
                    }
                })
                .setNeutralButton(android.R.string.cancel, (d, p) -> {
                    d.dismiss();
                    report(false, R.string.publiclocalstorage_folder_selection_aborted, folder.getUserDisplayableName());
                    if (callback != null) {
                        callback.accept(folder);
                    }
                })
                .setCancelable(true)
                .create().show();
        } else {
            selectUserFolderUri(folder, callback);
        }
    }

    private void selectUserFolderUri(final PublicLocalFolder folder, final Consumer<PublicLocalFolder> callback) {

        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | (folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0) | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        Log.i("Start uri dir: " + folder.getBaseUri());
        final Uri startUri = folder.getUri();
        if (startUri != null) {
            // Note: on SDK21, setting DocumentsContract.EXTRA_INITIAL_URI to either null
            // OR Uri.fromFile(LocalStorage.getExternalPublicCgeoDirectory()) leads to doc dialog not working in emulator
            // (Pixel 4 API 21)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, startUri);
        }

        runningIntentData = new IntentData(folder, callback);

        this.activity.startActivityForResult(intent, REQUEST_CODE_GRANT_FOLDER_URI_ACCESS);
    }

    public void selectFile(final String type, final Uri startUri, final Consumer<Uri> callback) {
        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type == null ? "*/*" : type);
        if (startUri != null) {
            // Note: on SDK21, setting DocumentsContract.EXTRA_INITIAL_URI to either null
            // OR Uri.fromFile(LocalStorage.getExternalPublicCgeoDirectory()) leads to doc dialog not working in emulator
            // (Pixel 4 API 21)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, startUri);
        }

        runningIntentData = new IntentData(null, callback);

        ((Activity) this.activity).startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    /** You MUST include in {@link Activity#onActivityResult(int, int, Intent)} of using Activity */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode != REQUEST_CODE_GRANT_FOLDER_URI_ACCESS && requestCode != REQUEST_CODE_SELECT_FILE) {
            return false;
        }
        if (runningIntentData == null) {
            report(true, R.string.publiclocalstorage_folder_selection_aborted, "unknown");
            return true;
        }

        final boolean resultOk = resultCode == Activity.RESULT_OK && intent != null;

        switch (requestCode) {
            case REQUEST_CODE_GRANT_FOLDER_URI_ACCESS:
                final Uri uri = !resultOk || intent == null ? null : intent.getData();
                if (uri == null) {
                    report(true, R.string.publiclocalstorage_folder_selection_aborted, runningIntentData.folder.getUserDisplayableName());
                } else {
                    final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | (runningIntentData.folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
                    activity.getContentResolver().takePersistableUriPermission(uri, flags);
                    Log.e("permissions: " + uri.getPath());
                    runningIntentData.folder.setUri(uri);
                    report(false, R.string.publiclocalstorage_folder_selection_success, runningIntentData.folder.getUserDisplayableName());
                }
                if (runningIntentData.callback != null) {
                    ((Consumer<PublicLocalFolder>) runningIntentData.callback).accept(runningIntentData.folder);
                }
                break;
            case REQUEST_CODE_SELECT_FILE:
                final Uri fileuri = !resultOk || intent == null ? null : intent.getData();
                if (fileuri == null) {
                    report(true, R.string.publiclocalstorage_file_selection_aborted);
                } else {
                    report(true, R.string.publiclocalstorage_file_selection_success, fileuri);
                }
                if (runningIntentData.callback != null) {
                    ((Consumer<Uri>) runningIntentData.callback).accept(fileuri);
                }
                break;
            default: //for codacy
                break;
        }

        runningIntentData = null;
        return true;
    }

    private void report(final boolean isWarning, @StringRes final int messageId, final Object ... params) {
        final String message = activity.getString(messageId, params);
        if (isWarning) {
            Log.w("PublicLocalStorageActivityHelper: " + message);
        } else {
            Log.i("PublicLocalStorageActivityHelper: " + message);
        }
        ActivityMixin.showToast(activity, message);
    }

}
