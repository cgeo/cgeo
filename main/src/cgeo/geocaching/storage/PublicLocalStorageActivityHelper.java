package cgeo.geocaching.storage;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
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

    //stores intermediate data of a running intent by return code. (This will no longer be neccessary with Activity Result API)
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

    /** Should be called on startup to check whether base dir is set as wanted.
     */
    public void checkBaseFolderAccess() {

        final PublicLocalFolder folder = PublicLocalFolder.BASE;

        if (folder.isUserDefinedLocation() && PublicLocalStorage.get().checkFolderAvailability(folder)) {
            //everything is as we want it
            return;
        }

        //ask/remind user to choose an explicit BASE dir, otherwise the default will be used
        final AlertDialog dialog = Dialogs.newBuilder(activity)
            .setTitle(R.string.publiclocalstorage_grantaccess_dialog_title)
            .setMessage(HtmlCompat.fromHtml(activity.getString(R.string.publiclocalstorage_grantaccess_dialog_msg_basedir_html, folder.getDefaultLocation().getUserDisplayableName()),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(android.R.string.ok, (d, p) -> {
                d.dismiss();
                selectFolderUri(folder, null);
            })
            .setNegativeButton(android.R.string.cancel, (d, p) -> {
                d.dismiss();
            })
            .create();
        dialog.show();
        Dialogs.makeLinksClickable(dialog);
    }

    /**
     * Starts user selection of a new Uri for the given folder.
     * If this is not the base folder, user is also asked whether the default folder under base folder shall be used.
     * @param folder folder to request a new place from user
     * @param callback called after user changed the uri. Callback is always called, even if user cancelled or error occured
     */
    public void selectFolderUri(final PublicLocalFolder folder, final Consumer<PublicLocalFolder> callback) {

        if (!PublicLocalFolder.BASE.equals(folder)) {
            Dialogs.newBuilder(activity)
                .setTitle(R.string.publiclocalstorage_selectfolder_dialog_user_or_default_title)
                .setMessage(activity.getString(R.string.publiclocalstorage_selectfolder_dialog_user_or_default_msg, folder.getDefaultLocation().getUserDisplayableName()))
                .setPositiveButton(R.string.publiclocalstorage_userdefined, (d, p) -> {
                    d.dismiss();
                    selectUserFolderUri(folder, callback);
                })
                .setNegativeButton(R.string.publiclocalstorage_default, (d, p) -> {
                    d.dismiss();
                    PublicLocalStorage.get().setFolderUri(folder, null);
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
            selectUserFolderUri(folder,  callback);
        }
    }

    private void selectUserFolderUri(final PublicLocalFolder folder, final Consumer<PublicLocalFolder> callback) {

        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | (folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0) | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        Log.i("Start uri dir: " + folder);
        final Uri startUri = folder.getLocation().getUri();
        if (startUri != null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Field is only supported starting with SDK26
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, startUri);
        }

        runningIntentData = new IntentData<>(folder, callback);

        this.activity.startActivityForResult(intent, REQUEST_CODE_GRANT_FOLDER_URI_ACCESS);
    }

    /**
     * Asks user to select a file for single usage (e.g. to import something into c:geo
     * @param type mime type, used for intent search
     * @param startUri hint for intent where to start search
     * @param callback called when user made selection. If user aborts search, callback is called with value null
     */
    public void selectFile(@Nullable final String type, @Nullable final Uri startUri, final Consumer<Uri> callback) {
        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type == null ? "*/*" : type);
        if (startUri != null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Attribute is supported starting SDK26 / O
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, startUri);
        }

        runningIntentData = new IntentData<>(null, callback);

        this.activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
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
                    PublicLocalStorage.get().setFolderUri(runningIntentData.folder, uri);
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
