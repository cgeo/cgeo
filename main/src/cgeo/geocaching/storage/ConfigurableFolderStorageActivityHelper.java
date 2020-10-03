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
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.AnyRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Consumer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

/**
 * Important: this class will oly work if you incorporate {@link #onActivityResult(int, int, Intent)}
 * into the {@link Activity#onActivityResult(int, int, Intent)} method of the using application!
 * TODO: once Activity ResultAPI is available -> refactor! Watch #9349
 */
public class ConfigurableFolderStorageActivityHelper {

    //use a globally unique request code to mix-in with Activity.onActivityResult. (This will no longer be neccessary with Activity Result API)
    //code must be positive (>0) and <=65535 (restriction of SDK21)
    public static final int REQUEST_CODE_GRANT_FOLDER_URI_ACCESS = 59371; //this is a random number
    public static final int REQUEST_CODE_SELECT_FILE = 59372; //this is a random number

    private final Activity activity;

    private enum CopyChoice { DO_NOTHING, COPY, MOVE }

    //stores intermediate data of a running intent by return code. (This will no longer be neccessary with Activity Result API)
    private IntentData<?> runningIntentData;

    private static class IntentData<T> {
        public final ConfigurableFolder folder;
        public final Consumer<T> callback;
        public final CopyChoice copyChoice;

        IntentData(final ConfigurableFolder folder, final Consumer<T> callback) {
            this(folder, callback, null);
        }

        IntentData(final ConfigurableFolder folder, final Consumer<T> callback, final CopyChoice copyChoice) {
            this.folder = folder;
            this.callback = callback;
            this.copyChoice = copyChoice;
        }
    }

    public ConfigurableFolderStorageActivityHelper(final Activity activity) {
        this.activity = activity;
    }

    /** Should be called on startup to check whether base dir is set as wanted.
     */
    public void checkBaseFolderAccess() {

        final ConfigurableFolder folder = ConfigurableFolder.BASE;

        if (folder.isUserDefined() && FolderStorage.get().checkAndAdjustAvailability(folder)) {
            //everything is as we want it
            return;
        }

        //ask/remind user to choose an explicit BASE dir, otherwise the default will be used
        final AlertDialog dialog = Dialogs.newBuilder(activity)
            .setTitle(R.string.folderstorage_grantaccess_dialog_title)
            .setMessage(HtmlCompat.fromHtml(activity.getString(R.string.folderstorage_grantaccess_dialog_msg_basedir_html, folder.getDefaultFolder().toUserDisplayableString()),
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
    public void selectFolderUri(final ConfigurableFolder folder, final Consumer<ConfigurableFolder> callback) {

        final ImmutablePair<Integer, Integer> fileInfo = FolderUtils.get().getFolderInfo(folder.getFolder());

        final AlertDialog.Builder dialog = Dialogs.newBuilder(activity);
        final View dialogView = LayoutInflater.from(dialog.getContext()).inflate(R.layout.folder_selection_dialog, null);

        final CharSequence message = getHtml(R.string.folderstorage_selectfolder_dialog_msg_html, folder.toUserDisplayableName(), folder.toUserDisplayableValue(),
            fileInfo.left, fileInfo.right, folder.getDefaultFolder().toUserDisplayableString());

        //init dialog
        ((TextView) dialogView.findViewById(R.id.message)).setText(message);
        final CopyChoice[] copyChoice = new CopyChoice[]{CopyChoice.DO_NOTHING};

        dialogView.findViewById(R.id.copymove_do_nothing).setOnClickListener(v -> copyChoice[0] = CopyChoice.DO_NOTHING);
        dialogView.findViewById(R.id.copymove_copy).setOnClickListener(v -> copyChoice[0] = CopyChoice.COPY);
        dialogView.findViewById(R.id.copymove_move).setOnClickListener(v -> copyChoice[0] = CopyChoice.MOVE);

        dialog
            .setView(dialogView)
            .setTitle(activity.getString(R.string.folderstorage_selectfolder_dialog_title, folder.toUserDisplayableName()))
            .setPositiveButton(R.string.configurablefolder_usertype_userdefined, (d, p) -> {
                d.dismiss();
                selectUserFolderUri(folder, copyChoice[0], callback);
                })
            .setNegativeButton(R.string.configurablefolder_usertype_default, (d, p) -> {
                d.dismiss();
                continueFolderSelectionCopyMove(folder, null, copyChoice[0], callback);
            })
            .setNeutralButton(android.R.string.cancel, (d, p) -> {
                d.dismiss();
                finalizeFolderSelection(false, folder, null, callback);
            })

            .create().show();
    }

    private void selectUserFolderUri(final ConfigurableFolder folder, final CopyChoice copyChoice, final Consumer<ConfigurableFolder> callback) {

        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | (folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0) | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        Log.i("Start uri dir: " + folder);
        final Uri startUri = folder.getFolder().getUri();
        if (startUri != null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Field is only supported starting with SDK26
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, startUri);
        }

        runningIntentData = new IntentData<>(folder, callback, copyChoice);

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
            report(true, R.string.folderstorage_folder_selection_aborted, "unknown");
            return true;
        }

        final boolean resultOk = resultCode == Activity.RESULT_OK && intent != null;

        switch (requestCode) {
            case REQUEST_CODE_GRANT_FOLDER_URI_ACCESS:
                handleResultFolderSelection(intent, resultOk);
                break;
            case REQUEST_CODE_SELECT_FILE:
                handleResultSelectFile(intent, resultOk);
                break;
            default: //for codacy
                break;
        }

        runningIntentData = null;
        return true;
    }

    private void handleResultSelectFile(final Intent intent, final boolean resultOk) {
        final Uri fileuri = !resultOk || intent == null ? null : intent.getData();
        if (fileuri == null) {
            report(true, R.string.folderstorage_file_selection_aborted);
        } else {
            report(true, R.string.folderstorage_file_selection_success, fileuri);
        }
        if (runningIntentData.callback != null) {
            ((Consumer<Uri>) runningIntentData.callback).accept(fileuri);
        }
    }

    private void handleResultFolderSelection(final Intent intent, final boolean resultOk) {
        final Uri uri = !resultOk || intent == null ? null : intent.getData();
        final ConfigurableFolder folder = runningIntentData.folder;
        final Consumer<ConfigurableFolder> callback = (Consumer<ConfigurableFolder>) runningIntentData.callback;
        if (uri == null) {
            finalizeFolderSelection(false, folder, null, callback);
        } else {
            final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | (runningIntentData.folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
            activity.getContentResolver().takePersistableUriPermission(uri, flags);
            FolderStorage.get().refreshUriPermissionCache();

            //Test if access is really working!
            final Folder target = Folder.fromDocumentUri(uri);
            if (!FolderStorage.get().checkAvailability(target, runningIntentData.folder.needsWrite(), true)) {
                finalizeFolderSelection(false, folder, null, callback);
            } else {
                continueFolderSelectionCopyMove(folder, uri, runningIntentData.copyChoice, callback);
            }
        }
    }

    private void continueFolderSelectionCopyMove(final ConfigurableFolder folder, final Uri targetUri, final CopyChoice copyChoice, final Consumer<ConfigurableFolder> callback) {
        final Folder before = folder.getFolder();
        final ImmutablePair<Integer, Integer> folderInfo = FolderUtils.get().getFolderInfo(before);
        if (copyChoice.equals(CopyChoice.DO_NOTHING) || new ImmutablePair<>(0, 0).equals(folderInfo)) {
            //nothing to copy/move
            finalizeFolderSelection(true, folder, targetUri, callback);
        } else {

            //perform copy or move
            final Folder target = targetUri == null ? folder.getDefaultFolder() : Folder.fromDocumentUri(targetUri);
            final ImmutableTriple<FolderUtils.CopyResult, Integer, Integer> copyResult = FolderUtils.get().copyAll(folder.getFolder(), target, copyChoice.equals(CopyChoice.MOVE));
            //display result
            Dialogs.newBuilder(activity)
                .setTitle(activity.getString(R.string.folderstorage_selectfolder_dialog_copy_move_finished_title, folder.toUserDisplayableName()))
                .setMessage(getHtml(R.string.folderstorage_selectfolder_dialog_copy_move_finished_msg_html,
                    copyResult.left, copyResult.middle, copyResult.right))
                .setPositiveButton(android.R.string.ok, (dd, pp) -> {
                    dd.dismiss();
                    finalizeFolderSelection(true, folder, targetUri, callback);
                 })
                .setNegativeButton(android.R.string.cancel, (dd, pp) -> {
                    dd.dismiss();
                    finalizeFolderSelection(false, folder, targetUri, callback);
                })
                .create().show();
        }
    }


    private void finalizeFolderSelection(final boolean success, final ConfigurableFolder folder, final Uri selectedUri, final Consumer<ConfigurableFolder> callback) {
        if (success) {
            FolderStorage.get().setUserDefinedFolder(folder, Folder.fromDocumentUri(selectedUri));
            report(false, R.string.folderstorage_folder_selection_success, folder.toUserDisplayableValue());
        } else {
            report(true, R.string.folderstorage_folder_selection_aborted, folder.toUserDisplayableValue());
        }
        if (callback != null) {
            callback.accept(folder);
        }
    }

    private void report(final boolean isWarning, @StringRes final int messageId, final Object ... params) {
        final String message = activity.getString(messageId, params);
        if (isWarning) {
            Log.w("ConfigurableFolderStorageActivityHelper: " + message);
        } else {
            Log.i("ConfigurableFolderStorageActivityHelper: " + message);
        }
        ActivityMixin.showToast(activity, message);
    }

    private Spanned getHtml(@AnyRes final int id, final Object ... params) {
        return HtmlCompat.fromHtml(activity.getString(id, params), HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

}
