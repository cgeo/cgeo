package cgeo.geocaching.storage;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

/**
 * Important: this class will only work if you incorporate {@link #onActivityResult(int, int, Intent)}
 * into the {@link Activity#onActivityResult(int, int, Intent)} method of the using application!
 * TODO: once Activity ResultAPI is available -> refactor! Watch #9349
 */
public class ContentStorageActivityHelper {


    public enum SelectAction {
        //use globally unique request codes to mix-in with Activity.onActivityResult. (This will no longer be neccessary with Activity Result API)
        //code must be positive (>0) and <=65535 (restriction of SDK21)
        SELECT_FOLDER(59371, Folder.class),
        SELECT_FOLDER_PERSISTED(59372, PersistableFolder.class),
        SELECT_FILE(59373, Uri.class),
        SELECT_FILE_MULTIPLE(59374, List.class),
        SELECT_FILE_PERSISTED(59375, PersistableUri.class);

        public final int requestCode;
        public final Class<?> callbackParameterClass;

        SelectAction(final int requestCode, final Class<?> callbackParameterClass) {
            this.requestCode = requestCode;
            this.callbackParameterClass = callbackParameterClass;
        }

        public static SelectAction getByRequestCode(final int requestCode) {
            for (SelectAction a : values()) {
                if (a.requestCode == requestCode) {
                    return a;
                }
            }
            return null;
        }

    }

    private final Activity activity;
    private final Map<SelectAction, Consumer<Object>> selectActionCallbacks = new HashMap<>();

    private enum CopyChoice { ASK_IF_DIFFERENT, GO_BACK, DO_NOTHING, COPY, MOVE }

    //stores intermediate data of a running intent by return code. (This will no longer be neccessary with Activity Result API)
    private IntentData runningIntentData;

    private static class IntentData implements Parcelable {
        public final SelectAction action;

        public final PersistableFolder folder; //for SelectAction.GRANT_FOLDER_URI_ACCESS
        public final CopyChoice copyChoice; //for SelectAction.GRANT_FOLDER_URI_ACCESS

        public final PersistableUri persistedDocUri; // for SelectAction.SELECT_FILE_PERSISTED

        IntentData(final PersistableFolder folder, final CopyChoice copyChoice, final SelectAction action) {
            this(folder, copyChoice, null, action);
        }

        IntentData(final PersistableUri persistedDocUri, final SelectAction action) {
            this(null, null, persistedDocUri, action);
        }

        IntentData(final PersistableFolder folder, final CopyChoice copyChoice, final PersistableUri persistedDocUri, final SelectAction action) {
            this.folder = folder;
            this.action = action;
            this.copyChoice = copyChoice;
            this.persistedDocUri = persistedDocUri;
        }

        protected IntentData(final Parcel in) {
            this.folder = EnumUtils.getEnum(PersistableFolder.class, in.readString());
            this.action = EnumUtils.getEnum(SelectAction.class, in.readString());
            this.copyChoice = EnumUtils.getEnum(CopyChoice.class, in.readString());
            this.persistedDocUri = EnumUtils.getEnum(PersistableUri.class, in.readString());
        }

        public static final Creator<IntentData> CREATOR = new Creator<IntentData>() {
            @Override
            public IntentData createFromParcel(final Parcel in) {
                return new IntentData(in);
            }

            @Override
            public IntentData[] newArray(final int size) {
                return new IntentData[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeString(folder == null ? null : folder.name());
            dest.writeString(action == null ? null : action.name());
            dest.writeString(copyChoice == null ? null : copyChoice.name());
            dest.writeString(persistedDocUri == null ? null : persistedDocUri.name());
        }
    }

    public ContentStorageActivityHelper(final Activity activity, final Bundle state) {
        this.activity = activity;
        if (state != null) {
            runningIntentData = state.getParcelable("state");
        }
    }

    public Bundle getState() {
        final Bundle state = new Bundle();
        state.putParcelable("state", runningIntentData);
        return state;
    }

    @SuppressWarnings("unchecked")
    public <T> ContentStorageActivityHelper addSelectActionCallback(final SelectAction action, final Class<T> clazz, final Consumer<T> callback) {
        if (!action.callbackParameterClass.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Callback for action '" + action + "' must be of type " + action.callbackParameterClass + ", found " + clazz);
        }
        this.selectActionCallbacks.put(action, (Consumer<Object>) callback);
        return this;
    }

    public static boolean baseFolderIsSet() {
        final PersistableFolder folder = PersistableFolder.BASE;
        return folder.isUserDefined() && ContentStorage.get().ensureFolder(folder);
    }

    /**
     * Asks user to select a folder for single-time-usage (location and permission is not persisted)
     * if a callback for action {@link SelectAction#SELECT_FOLDER} is registered, it will be called after selection has finished
     */
    public void selectFolder(@Nullable final Uri startUri) {
        selectFolderInternal(SelectAction.SELECT_FOLDER, null, startUri, null);
    }

    /**
     * Starts user selection of a new Location for the given persisted folder.
     * Persisted folder handling is a bit more complex and involved e.g. optional copying from previous location and provides a default-option to user
     * if a callback for action {@link SelectAction#SELECT_FOLDER_PERSISTED} is registered, it will be called after selection has finished
     *
     * @param folder folder to request a new place from user
     */
    public void selectPersistableFolder(final PersistableFolder folder) {

        final ImmutableTriple<String, String, String> folderInfo = getInternationalizedFolderInfoStrings(folder.getFolder());

        //create the message;
        final String folderData = activity.getString(R.string.contentstorage_selectfolder_dialog_msg_folderdata,
                folder.toUserDisplayableName(), folder.toUserDisplayableValue(), folderInfo.left, folderInfo.middle, folderInfo.right);
        final String defaultFolder = activity.getString(R.string.contentstorage_selectfolder_dialog_msg_defaultfolder, folder.getDefaultFolder().toUserDisplayableString(true, false));

        final AlertDialog.Builder dialog = Dialogs.newBuilder(activity);
        dialog
                .setTitle(activity.getString(R.string.contentstorage_selectfolder_dialog_title, folder.toUserDisplayableName()))
                .setMessage(folderData + (folder.isUserDefined() ? "\n\n" + defaultFolder : ""))
                .setPositiveButton(R.string.persistablefolder_pickfolder, (d, p) -> {
                    d.dismiss();
                    selectFolderInternal(SelectAction.SELECT_FOLDER_PERSISTED, folder, null, CopyChoice.ASK_IF_DIFFERENT);
                })
                .setNegativeButton(android.R.string.cancel, (d, p) -> {
                    d.dismiss();
                    finalizePersistableFolderSelection(false, folder, null, SelectAction.SELECT_FOLDER_PERSISTED);
                });

        //only allow default selection if folder is currently NOT at default
        if (folder.isUserDefined()) {
            dialog.setNeutralButton(R.string.persistablefolder_usedefault, (d, p) -> {
                d.dismiss();
                continuePersistableFolderSelectionCheckFoldersAreEqual(folder, null, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION, CopyChoice.ASK_IF_DIFFERENT, SelectAction.SELECT_FOLDER_PERSISTED);
            });
        }

        dialog.create().show();
    }

    /**
     * Simplified form of selectPersistableFolder without initial dialog
     */
    public void migratePersistableFolder(final PersistableFolder folder) {
        selectFolderInternal(SelectAction.SELECT_FOLDER_PERSISTED, folder, null, CopyChoice.ASK_IF_DIFFERENT);
    }

    /**
     * Simplified form of selectPersistableFolder used on settings' restore
     */
    public void restorePersistableFolder(final PersistableFolder folder, final Uri newUri) {
        selectFolderInternal(SelectAction.SELECT_FOLDER_PERSISTED, folder, newUri, CopyChoice.ASK_IF_DIFFERENT);
    }

    /**
     * Asks user to select a file for single usage (e.g. to import something into c:geo
     * if a callback for action {@link SelectAction#SELECT_FILE} is registered, it will be called after selection has finished
     *
     * @param type     mime type, used for intent search
     * @param startUri hint for intent where to start search
     */
    public void selectFile(@Nullable final String type, @Nullable final Uri startUri) {
        selectFilesInternal(type, startUri, SelectAction.SELECT_FILE, null);
    }

    /**
     * Asks user to select multiple files at once
     * if a callback for action {@link SelectAction#SELECT_FILE_MULTIPLE} is registered, it will be called after selection has finished
     */
    public void selectMultipleFiles(@Nullable final String type, @Nullable final Uri startUri) {
        selectFilesInternal(type, startUri, SelectAction.SELECT_FILE_MULTIPLE, null);
    }

    /**
     * Asks user to select a new location for a persisted uri (used e.g. for Track file). Permission is persisted as well.
     * if a callback for action {@link SelectAction#SELECT_FILE_PERSISTED} is registered, it will be called after selection has finished
     */
    public void selectPersistableUri(@NonNull final PersistableUri persistedDocUri) {
        selectFilesInternal(persistedDocUri.getMimeType(), persistedDocUri.getUri(), SelectAction.SELECT_FILE_PERSISTED, persistedDocUri);
    }

    /**
     * Simplified form of selectPersistableUri used on settings' restore
     */
    public void restorePersistableUri(final PersistableUri persistableUri, final Uri newUri) {
        selectFilesInternal(persistableUri.getMimeType(), newUri, SelectAction.SELECT_FILE_PERSISTED, persistableUri);
    }

    /**
     * You MUST include in {@link Activity#onActivityResult(int, int, Intent)} of using Activity
     */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        final SelectAction action = SelectAction.getByRequestCode(requestCode);
        if (action == null) {
            return false;
        }
        if (runningIntentData == null) {
            // this is not an error! It might mean that activity was requested by another instance of the Helper (thus using same requestCodes)
            // -> signal that result was NOT handled
            return false;
        }

        try {

            final boolean resultOk = resultCode == Activity.RESULT_OK && intent != null;

            switch (action) {
                case SELECT_FOLDER:
                    handleResultFolderSelection(intent, resultOk);
                    break;
                case SELECT_FOLDER_PERSISTED:
                    handleResultPersistableFolderSelection(intent, resultOk);
                    break;
                case SELECT_FILE:
                case SELECT_FILE_MULTIPLE:
                case SELECT_FILE_PERSISTED:
                    handleResultSelectFiles(action, intent, resultOk);
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

    private void selectFilesInternal(@Nullable final String type, @Nullable final Uri startUri, final SelectAction action, final PersistableUri docUri) {
        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type == null ? "*/*" : type);
        if (startUri != null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Attribute is supported starting SDK26 / O
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, startUri);
        }
        if (action == SelectAction.SELECT_FILE_MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                (docUri == null ? 0 : Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));

        runningIntentData = new IntentData(docUri, action);

        this.activity.startActivityForResult(intent, action.requestCode);
    }

    private void selectFolderInternal(final SelectAction action, final PersistableFolder folder, final Uri startUri, final CopyChoice copyChoice) {

        // call for document tree dialog
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                (folder == null || folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0) |
                (folder != null ? Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION : 0));
        Uri realStartUri = startUri != null ? startUri : (folder != null ? folder.getUri() : null);

        // show internal storage
        intent.putExtra(Intents.EXTRA_SHOW_ADVANCED, true);

        if (realStartUri != null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (UriUtils.isFileUri(realStartUri)) {
                realStartUri = UriUtils.getPseudoTreeUriForFileUri(realStartUri);
            }
            // Field is only supported starting with SDK26
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, realStartUri);
        }

        runningIntentData = new IntentData(folder, copyChoice, action);

        this.activity.startActivityForResult(intent, action.requestCode);
    }


    private void handleResultSelectFiles(final SelectAction action, final Intent intent, final boolean resultOk) {
        final List<Uri> selectedUris = new ArrayList<>();
        if (!resultOk || intent == null) {
            report(true, R.string.contentstorage_file_selection_aborted);
        } else {
            //get selected uris from intent
            if (intent.getData() != null) {
                selectedUris.add(intent.getData());
            }
            if (intent.getClipData() != null) {
                for (int idx = 0; idx < intent.getClipData().getItemCount(); idx++) {
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

        callCallback(action, action == SelectAction.SELECT_FILE_MULTIPLE ? selectedUris :
                (action == SelectAction.SELECT_FILE_PERSISTED ? runningIntentData.persistedDocUri :
                        (selectedUris.isEmpty() ? null : selectedUris.get(0))));
    }

    private void handleResultFolderSelection(final Intent intent, final boolean resultOk) {
        final Uri uri = !resultOk || intent == null ? null : intent.getData();
        final Folder folder = uri == null ? null : Folder.fromDocumentUri(uri);

        if (uri == null) {
            report(true, R.string.contentstorage_folder_selection_aborted, "---");
        } else {
            report(true, R.string.contentstorage_folder_selection_success, uri);
        }
        callCallback(SelectAction.SELECT_FOLDER, folder);
    }

    private void handleResultPersistableFolderSelection(final Intent intent, final boolean resultOk) {
        final Uri uri = !resultOk || intent == null ? null : intent.getData();
        final PersistableFolder folder = runningIntentData.folder;
        if (uri == null) {
            finalizePersistableFolderSelection(false, folder, null, runningIntentData.action);
        } else {
            final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | (runningIntentData.folder.needsWrite() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
            activity.getContentResolver().takePersistableUriPermission(uri, flags);
            ContentStorage.get().refreshUriPermissionCache();

            //Test if access is really working!
            final Folder target = Folder.fromDocumentUri(uri);
            if (!ContentStorage.get().ensureFolder(target, runningIntentData.folder.needsWrite(), true)) {
                finalizePersistableFolderSelection(false, folder, null, runningIntentData.action);
            } else {
                continuePersistableFolderSelectionCheckFoldersAreEqual(folder, uri, flags, runningIntentData.copyChoice, runningIntentData.action);
            }
        }
    }

    /**
     * releases a folder grant
     */
    private void releaseGrant(final Uri uri, final int flags) {
        if (uri != null) {
            activity.getContentResolver().releasePersistableUriPermission(uri, flags);
        }
        ContentStorage.get().refreshUriPermissionCache();
    }

    private void continuePersistableFolderSelectionCheckFoldersAreEqual(final PersistableFolder folder, final Uri targetUri, final int flags, final CopyChoice copyChoice, final SelectAction action) {
        final Folder before = folder.getFolder();
        boolean askUserForCopyMove =
                copyChoice == CopyChoice.ASK_IF_DIFFERENT && before != null && !FolderUtils.get().foldersAreEqual(before, Folder.fromDocumentUri(targetUri));
        FolderUtils.FolderInfo folderInfoBeforeRaw = null;

        if (askUserForCopyMove) {
            folderInfoBeforeRaw = FolderUtils.get().getFolderInfo(before, -1);
            //suppress asking user to copy/move if source has 0 files
            if (folderInfoBeforeRaw.fileCount == 0 && !folderInfoBeforeRaw.resultIsIncomplete) {
                askUserForCopyMove = false;
            }
        }

        if (askUserForCopyMove) {

            final ImmutableTriple<String, String, String> folderInfo = folderInfoBeforeRaw.getUserDisplayableFolderInfoStrings();

            final AlertDialog.Builder dialog = Dialogs.newBuilder(activity);
            final View dialogView = LayoutInflater.from(dialog.getContext()).inflate(R.layout.folder_selection_dialog, null);
            ((TextView) dialogView.findViewById(R.id.message)).setText(LocalizationUtils.getString(R.string.contentstorage_selectfolder_dialog_choice, folderInfo.left, folderInfo.middle, folderInfo.right));
            final CopyChoice[] cc = new CopyChoice[]{CopyChoice.DO_NOTHING};
            dialogView.findViewById(R.id.copymove_justselect).setOnClickListener(v -> cc[0] = CopyChoice.DO_NOTHING);
            dialogView.findViewById(R.id.copymove_move).setOnClickListener(v -> cc[0] = CopyChoice.MOVE);
            dialogView.findViewById(R.id.copymove_copy).setOnClickListener(v -> cc[0] = CopyChoice.COPY);
            dialog
                    .setView(dialogView)
                    .setTitle(activity.getString(R.string.contentstorage_selectfolder_dialog_title, folder.toUserDisplayableName()))
                    .setPositiveButton(android.R.string.ok, (d, p) -> {
                        d.dismiss();
                        continuePersistableFolderSelectionCopyMove(folder, targetUri, cc[0], action);
                    })
                    .setNegativeButton(android.R.string.cancel, (d, p) -> {
                        d.dismiss();
                        releaseGrant(targetUri, flags);
                        finalizePersistableFolderSelection(false, folder, null, action);
                    })
                    .setNeutralButton(R.string.back, (d, p) -> {
                        d.dismiss();
                        releaseGrant(targetUri, flags);
                        migratePersistableFolder(folder);
                    })
                    .create()
                    .show();
        } else {
            continuePersistableFolderSelectionCopyMove(folder, targetUri, copyChoice == CopyChoice.ASK_IF_DIFFERENT ? CopyChoice.DO_NOTHING : copyChoice, action);
        }
    }

    private void continuePersistableFolderSelectionCopyMove(final PersistableFolder folder, final Uri targetUri, final CopyChoice copyChoice, final SelectAction action) {
        final Folder before = folder.getFolder();
        if (copyChoice.equals(CopyChoice.DO_NOTHING) || FolderUtils.FolderInfo.EMPTY_FOLDER.equals(FolderUtils.get().getFolderInfo(before))) {
            //nothing to copy/move
            finalizePersistableFolderSelection(true, folder, targetUri, action);
        } else {

            //perform copy or move
            final Folder target = targetUri == null ? folder.getDefaultFolder() : Folder.fromDocumentUri(targetUri);
            FolderUtils.get().copyAllAsynchronousWithGui(activity, folder.getFolder(), target, copyChoice.equals(CopyChoice.MOVE), copyResult -> {
                if (copyResult != null) {
                    finalizePersistableFolderSelection(true, folder, targetUri, action);
                }
            });
        }
    }

    private void finalizePersistableFolderSelection(final boolean success, final PersistableFolder folder, final Uri selectedUri, final SelectAction action) {
        if (success) {
            ContentStorage.get().setUserDefinedFolder(folder, Folder.fromDocumentUri(selectedUri), true);
            report(false, R.string.contentstorage_folder_selection_success, folder);
        } else {
            report(true, R.string.contentstorage_folder_selection_aborted, folder);
        }
        callCallback(action, folder);
    }

    private void report(final boolean isWarning, @StringRes final int messageId, final Object... params) {
        final ImmutablePair<String, String> messages = LocalizationUtils.getMultiPurposeString(messageId, "CSActivityHelper", params);
        Log.log(isWarning ? Log.LogLevel.WARN : Log.LogLevel.INFO, messages.right);
        ActivityMixin.showToast(activity, messages.left);
    }

    /**
     * returns for a folder internationalized strings for file count (left), dir count (middle) and total file size (right)
     */
    private static ImmutableTriple<String, String, String> getInternationalizedFolderInfoStrings(final Folder folder) {
        final FolderUtils.FolderInfo folderInfo = FolderUtils.get().getFolderInfo(folder, -1);
        return folderInfo.getUserDisplayableFolderInfoStrings();
    }

    private void callCallback(final SelectAction action, final Object parameter) {
        final Consumer<Object> callback = (Consumer<Object>) this.selectActionCallbacks.get(action);
        if (callback != null) {
            callback.accept(parameter);
        }
    }

}
