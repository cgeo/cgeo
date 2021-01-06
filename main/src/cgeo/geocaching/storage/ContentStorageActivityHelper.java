package cgeo.geocaching.storage;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

/**
 * Important: this class will oly work if you incorporate {@link #onActivityResult(int, int, Intent)}
 * into the {@link Activity#onActivityResult(int, int, Intent)} method of the using application!
 * TODO: once Activity ResultAPI is available -> refactor! Watch #9349
 */
public class ContentStorageActivityHelper {

    //use a globally unique request code to mix-in with Activity.onActivityResult. (This will no longer be neccessary with Activity Result API)
    //code must be positive (>0) and <=65535 (restriction of SDK21)
    private static final int REQUEST_CODE_SELECT_FOLDER = 59371; //this is a random number
    private static final int REQUEST_CODE_SELECT_FOLDER_PERSISTED = 59372;

    private static final int REQUEST_CODE_SELECT_FILE = 59373;
    private static final int REQUEST_CODE_SELECT_FILE_MULTIPLE = 59374;
    private static final int REQUEST_CODE_SELECT_FILE_PERSISTED = 59375;

    private final Activity activity;

    private enum CopyChoice { DO_NOTHING, COPY, MOVE }

    //stores intermediate data of a running intent by return code. (This will no longer be neccessary with Activity Result API)
    private IntentData<?> runningIntentData;

    private static class IntentData<T> {
        public final Consumer<T> callback; //for all requests

        public final PersistableFolder folder; //for REQUEST_CODE_GRANT_FOLDER_URI_ACCESS
        public final CopyChoice copyChoice; //for REQUEST_CODE_GRANT_FOLDER_URI_ACCESS

        public final PersistableUri persistedDocUri; // for REQUEST_CODE_SELECT_FILE_PERSISTED

        IntentData(final PersistableFolder folder, final CopyChoice copyChoice, final Consumer<T> callback) {
            this(folder, copyChoice, null, callback);
        }

        IntentData(final PersistableUri persistedDocUri, final Consumer<T> callback) {
            this(null, null, persistedDocUri, callback);
        }

        IntentData(final PersistableFolder folder, final CopyChoice copyChoice, final PersistableUri persistedDocUri, final Consumer<T> callback) {
            this.folder = folder;
            this.callback = callback;
            this.copyChoice = copyChoice;
            this.persistedDocUri = persistedDocUri;
        }
    }

    public ContentStorageActivityHelper(final Activity activity) {
        this.activity = activity;
    }

    /** Check routine, should be called on c:geo startup to check whether base dir is set as wanted. */
    public void checkBaseFolderAccess() {

        final PersistableFolder folder = PersistableFolder.BASE;

        if (folder.isUserDefined() && ContentStorage.get().ensureAndAdjustFolder(folder)) {
            //everything is as we want it
            return;
        }

        //ask/remind user to choose an explicit BASE dir, otherwise the default will be used
        final AlertDialog dialog = Dialogs.newBuilder(activity)
            .setTitle(R.string.contentstorage_grantaccess_dialog_title)
            .setMessage(HtmlCompat.fromHtml(activity.getString(R.string.contentstorage_grantaccess_dialog_msg_basedir_html, folder.getDefaultFolder().toUserDisplayableString()),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(android.R.string.ok, (d, p) -> {
                d.dismiss();
                selectPersistableFolder(folder, null);
            })
            .setNegativeButton(android.R.string.cancel, (d, p) -> d.dismiss())
            .create();
        dialog.show();
        Dialogs.makeLinksClickable(dialog);
    }

    /** Asks user to select a folder for single-time-usage (location and permission is not persisted) */
    public void selectFolder(@Nullable final Uri startUri, final Consumer<Folder> callback) {
        selectFolderInternal(REQUEST_CODE_SELECT_FOLDER, null, startUri, null, callback);
    }

    /**
     * Starts user selection of a new Location for the given persisted folder.
     * Persisted folder handling is a bit more complex and involved e.g. optional copying from previous location and provides a default-option to user
     * @param folder folder to request a new place from user
     * @param callback called after user changed the uri. Callback is always called, even if user cancelled or error occured
     */
    public void selectPersistableFolder(final PersistableFolder folder, final Consumer<PersistableFolder> callback) {

        final ImmutablePair<Integer, Integer> fileInfo = FolderUtils.get().getFolderInfo(folder.getFolder());

        final AlertDialog.Builder dialog = Dialogs.newBuilder(activity);
        final View dialogView = LayoutInflater.from(dialog.getContext()).inflate(R.layout.folder_selection_dialog, null);

        final CharSequence message = getHtml(R.string.contentstorage_selectfolder_dialog_msg_html, folder.toUserDisplayableName(), folder.toUserDisplayableValue(),
            fileInfo.left, fileInfo.right, folder.getDefaultFolder().toUserDisplayableString());

        //init dialog
        ((TextView) dialogView.findViewById(R.id.message)).setText(message);
        final CopyChoice[] copyChoice = new CopyChoice[]{CopyChoice.DO_NOTHING};

        dialogView.findViewById(R.id.copymove_do_nothing).setOnClickListener(v -> copyChoice[0] = CopyChoice.DO_NOTHING);
        dialogView.findViewById(R.id.copymove_copy).setOnClickListener(v -> copyChoice[0] = CopyChoice.COPY);
        dialogView.findViewById(R.id.copymove_move).setOnClickListener(v -> copyChoice[0] = CopyChoice.MOVE);

        dialog
            .setView(dialogView)
            .setTitle(activity.getString(R.string.contentstorage_selectfolder_dialog_title, folder.toUserDisplayableName()))
            .setPositiveButton(R.string.persistablefolder_usertype_userdefined, (d, p) -> {
                d.dismiss();
                selectFolderInternal(REQUEST_CODE_SELECT_FOLDER_PERSISTED, folder, null, copyChoice[0], callback);
                })
            .setNegativeButton(R.string.persistablefolder_usertype_default, (d, p) -> {
                d.dismiss();
                continuePersistableFolderSelectionCopyMove(folder, null, copyChoice[0], callback);
            })
            .setNeutralButton(android.R.string.cancel, (d, p) -> {
                d.dismiss();
                finalizePersistableFolderSelection(false, folder, null, callback);
            })

            .create().show();
    }

    /**
     * Asks user to select a file for single usage (e.g. to import something into c:geo
     * @param type mime type, used for intent search
     * @param startUri hint for intent where to start search
     * @param callback called when user made selection. If user aborts search, callback is called with value null
     */
    public void selectFile(@Nullable final String type, @Nullable final Uri startUri, final Consumer<Uri> callback) {
        selectFilesInternal(type, startUri, REQUEST_CODE_SELECT_FILE, null, callback);
    }

    /** Asks user to select multiple files at once */
    public void selectMultipleFiles(@Nullable final String type, @Nullable final Uri startUri, final Consumer<List<Uri>> callback) {
        selectFilesInternal(type, startUri, REQUEST_CODE_SELECT_FILE_MULTIPLE, null, callback);
    }

    /** Asks user to select a new location for a persisted uri (used e.g. for Track file). Permission is persisted as well. */
    public void selectPersistableUri(@NonNull final PersistableUri persistedDocUri, final Consumer<Uri> callback) {
        selectFilesInternal(persistedDocUri.getMimeType(), persistedDocUri.getUri(), REQUEST_CODE_SELECT_FILE_PERSISTED, persistedDocUri, callback);
    }



    /** You MUST include in {@link Activity#onActivityResult(int, int, Intent)} of using Activity */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode != REQUEST_CODE_SELECT_FOLDER && requestCode != REQUEST_CODE_SELECT_FOLDER_PERSISTED &&
            requestCode != REQUEST_CODE_SELECT_FILE && requestCode != REQUEST_CODE_SELECT_FILE_MULTIPLE && requestCode != REQUEST_CODE_SELECT_FILE_PERSISTED) {
            return false;
        }
        if (runningIntentData == null) {
            // this is not an error! It might mean that activity was requested by another instance of the Helper (thus usind same requestCodes)
            // -> signal that result was NOT handled
            return false;
        }

        try {

            final boolean resultOk = resultCode == Activity.RESULT_OK && intent != null;

            switch (requestCode) {
                case REQUEST_CODE_SELECT_FOLDER:
                    handleResultFolderSelection(intent, resultOk);
                    break;
                case REQUEST_CODE_SELECT_FOLDER_PERSISTED:
                    handleResultPersistableFolderSelection(intent, resultOk);
                    break;
                case REQUEST_CODE_SELECT_FILE:
                case REQUEST_CODE_SELECT_FILE_MULTIPLE:
                case REQUEST_CODE_SELECT_FILE_PERSISTED:
                    handleResultSelectFiles(requestCode, intent, resultOk);
                    break;
                default: //for codacy
                    break;
            }
            return true;
        } finally {
            //Make sure to delete the runningIntentData when the request was handled!
            runningIntentData = null;
        }
    }

    private void selectFilesInternal(@Nullable final String type, @Nullable final Uri startUri, final int requestCode, final PersistableUri docUri, final Consumer<?> callback) {
        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type == null ? "*/*" : type);
        if (startUri != null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && UriUtils.isContentUri(startUri)) {
            // Attribute is supported starting SDK26 / O
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, startUri);
        }
        if (requestCode == REQUEST_CODE_SELECT_FILE_MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION  |
            (docUri == null ? 0 : Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));

        runningIntentData = new IntentData<>(docUri, callback);

        this.activity.startActivityForResult(intent, requestCode);
    }

    private void selectFolderInternal(final int requestCode, final PersistableFolder folder, final Uri startUri, final CopyChoice copyChoice, final Consumer<?> callback) {

        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
            (folder == null || folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0) |
            (folder != null ? Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION : 0));
        final Uri realStartUri = startUri != null ? startUri : (folder != null ? folder.getUri() : null);
        if (realStartUri != null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Field is only supported starting with SDK26
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, realStartUri);
        }

        runningIntentData = new IntentData<>(folder, copyChoice, callback);

        this.activity.startActivityForResult(intent, requestCode);
    }


    private void handleResultSelectFiles(final int requestCode, final Intent intent, final boolean resultOk) {
        final List<Uri> selectedUris = new ArrayList<>();
        if (!resultOk || intent == null) {
            report(true, R.string.contentstorage_file_selection_aborted);
        } else {
            //get selected uris from intent
            if (intent.getData() != null) {
                selectedUris.add(intent.getData());
            }
            if (intent.getClipData() != null) {
                for (int idx = 0; idx < intent.getClipData().getItemCount(); idx ++) {
                    final Uri uri = intent.getClipData().getItemAt(idx).getUri();
                    if (uri != null) {
                        selectedUris.add(uri);
                    }
                }
            }

            if (selectedUris.isEmpty()) {
                report(true, R.string.contentstorage_file_selection_aborted);
            } else {
                if (runningIntentData.persistedDocUri != null) {
                    activity.getContentResolver().takePersistableUriPermission(selectedUris.get(0),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    ContentStorage.get().setPersistedDocumentUri(runningIntentData.persistedDocUri, selectedUris.get(0));
                }
                report(false, R.string.contentstorage_file_selection_success, selectedUris.get(0));
            }
        }

        if (runningIntentData.callback != null) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_FILE_MULTIPLE:
                    ((Consumer<List<Uri>>) runningIntentData.callback).accept(selectedUris);
                    break;
                default:
                    ((Consumer<Uri>) runningIntentData.callback).accept(selectedUris.isEmpty() ? null : selectedUris.get(0));
                    break;
            }
        }
    }

    private void handleResultFolderSelection(final Intent intent, final boolean resultOk) {
        final Uri uri = !resultOk || intent == null ? null : intent.getData();
        final Folder folder = uri == null ? null : Folder.fromDocumentUri(uri);

        final Consumer<Folder> callback = (Consumer<Folder>) runningIntentData.callback;

        if (uri == null) {
            report(true, R.string.contentstorage_folder_selection_aborted, "---");
        } else {
            report(true, R.string.contentstorage_folder_selection_success, uri);
        }
        if (callback != null) {
            callback.accept(folder);
        }
    }

    private void handleResultPersistableFolderSelection(final Intent intent, final boolean resultOk) {
        final Uri uri = !resultOk || intent == null ? null : intent.getData();
        final PersistableFolder folder = runningIntentData.folder;
        final Consumer<PersistableFolder> callback = (Consumer<PersistableFolder>) runningIntentData.callback;
        if (uri == null) {
            finalizePersistableFolderSelection(false, folder, null, callback);
        } else {
            final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | (runningIntentData.folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
            activity.getContentResolver().takePersistableUriPermission(uri, flags);
            ContentStorage.get().refreshUriPermissionCache();

            //Test if access is really working!
            final Folder target = Folder.fromDocumentUri(uri);
            if (!ContentStorage.get().ensureFolder(target, runningIntentData.folder.needsWrite(), true)) {
                finalizePersistableFolderSelection(false, folder, null, callback);
            } else {
                continuePersistableFolderSelectionCopyMove(folder, uri, runningIntentData.copyChoice, callback);
            }
        }
    }

    private void continuePersistableFolderSelectionCopyMove(final PersistableFolder folder, final Uri targetUri, final CopyChoice copyChoice, final Consumer<PersistableFolder> callback) {
        final Folder before = folder.getFolder();
        final ImmutablePair<Integer, Integer> folderInfo = FolderUtils.get().getFolderInfo(before);
        if (copyChoice.equals(CopyChoice.DO_NOTHING) || new ImmutablePair<>(0, 0).equals(folderInfo)) {
            //nothing to copy/move
            finalizePersistableFolderSelection(true, folder, targetUri, callback);
        } else {

            //perform copy or move
            final Folder target = targetUri == null ? folder.getDefaultFolder() : Folder.fromDocumentUri(targetUri);
            final ImmutableTriple<FolderUtils.CopyResult, Integer, Integer> copyResult = FolderUtils.get().copyAll(folder.getFolder(), target, copyChoice.equals(CopyChoice.MOVE));
            //display result
            Dialogs.newBuilder(activity)
                .setTitle(activity.getString(R.string.contentstorage_selectfolder_dialog_copy_move_finished_title, folder.toUserDisplayableName()))
                .setMessage(getHtml(R.string.contentstorage_selectfolder_dialog_copy_move_finished_msg_html,
                    copyResult.left, copyResult.middle, copyResult.right))
                .setPositiveButton(android.R.string.ok, (dd, pp) -> {
                    dd.dismiss();
                    finalizePersistableFolderSelection(true, folder, targetUri, callback);
                 })
                .setNegativeButton(android.R.string.cancel, (dd, pp) -> {
                    dd.dismiss();
                    finalizePersistableFolderSelection(false, folder, targetUri, callback);
                })
                .create().show();
        }
    }


    private void finalizePersistableFolderSelection(final boolean success, final PersistableFolder folder, final Uri selectedUri, final Consumer<PersistableFolder> callback) {
        if (success) {
            ContentStorage.get().setUserDefinedFolder(folder, Folder.fromDocumentUri(selectedUri));
            report(false, R.string.contentstorage_folder_selection_success, folder);
        } else {
            report(true, R.string.contentstorage_folder_selection_aborted, folder);
        }
        if (callback != null) {
            callback.accept(folder);
        }
    }

    private void report(final boolean isWarning, @StringRes final int messageId, final Object ... params) {
        final ImmutablePair<String, String> messages = ContentStorage.get().constructMessage(messageId, params);
        Log.log(isWarning ? Log.LogLevel.WARN : Log.LogLevel.INFO, messages.right);
        ActivityMixin.showToast(activity, messages.left);
    }

    private Spanned getHtml(@AnyRes final int id, final Object ... params) {
        return HtmlCompat.fromHtml(activity.getString(id, params), HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

}
