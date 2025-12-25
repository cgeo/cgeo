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

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.InstallWizardActivity
import cgeo.geocaching.MainActivity
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.settings.BackupSeekbarPreference
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.ContentStorageActivityHelper
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.Folder
import cgeo.geocaching.storage.FolderUtils
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.storage.PersistableUri
import cgeo.geocaching.storage.extension.OneTimeDialogs
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.SettingsUtils.SettingsType.TYPE_STRING
import cgeo.geocaching.utils.SettingsUtils.SettingsType.TYPE_UNKNOWN

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Consumer
import androidx.preference.PreferenceManager

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.concurrent.atomic.AtomicBoolean

import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.ImmutableTriple
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer


class BackupUtils {
    private static val ATTRIBUTE_NAME: String = "name"
    private static val ATTRIBUTE_VALUE: String = "value"
    private static val TAG_MAP: String = "map"
    private static val SETTINGS_FILENAME: String = "cgeo-settings.xml"
    private static val TRACKS_SUBFOLDER: String = "tracks"

    private static val MAX_AUTO_BACKUPS: Int = 4;  // most recent + x more
    private static val AUTO_BACKUP_FOLDER: String = "auto"; // subfolder of PersistableFolder.BACKUP

    private static val STATE_CSAH: String = "csam"
    private static val NO_ACCESSIBLE_FILE: Int = -1

    private final ContentStorageActivityHelper fileSelector

    private final Activity activityContext

    private final List<ImmutableTriple<PersistableFolder, String, String>> regrantAccessFolders = ArrayList<>()
    private final List<ImmutableTriple<PersistableUri, String, String>> regrantAccessUris = ArrayList<>()
    private var regrantAccessRestartNeeded: Boolean = false
    private var regrantAccessResultString: String = null

    public BackupUtils(final Activity activityContext, final Bundle savedState) {
        this.activityContext = activityContext
        this.fileSelector = ContentStorageActivityHelper(activityContext, savedState == null ? null : savedState.getBundle(STATE_CSAH))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FOLDER, Folder.class, this::restore)
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FOLDER_PERSISTED, PersistableFolder.class, pf -> triggerNextRegrantStep(pf, null))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE_PERSISTED, PersistableUri.class, uri -> triggerNextRegrantStep(null, uri))
    }

    private Unit triggerNextRegrantStep(final PersistableFolder folder, final PersistableUri uri) {
        if (folder != null) {
            final Iterator<ImmutableTriple<PersistableFolder, String, String>> it = regrantAccessFolders.iterator()
            while (it.hasNext()) {
                if (it.next().left == folder) {
                    it.remove()
                    break
                }
            }
        }
        if (uri != null) {
            final Iterator<ImmutableTriple<PersistableUri, String, String>> it = regrantAccessUris.iterator()
            while (it.hasNext()) {
                if (it.next().left == uri) {
                    it.remove()
                    break
                }
            }
        }

        if (!regrantAccessFolders.isEmpty()) {
            val current: ImmutableTriple<PersistableFolder, String, String> = regrantAccessFolders.get(0)
            val folderToBeRestored: Folder = Folder.fromConfig(current.right)

            SimpleDialog.of(activityContext)
                    .setTitle(R.string.init_backup_settings_restore)
                    .setMessage(R.string.settings_folder_changed, activityContext.getString(current.left.getNameKeyId()), folderToBeRestored.toUserDisplayableString(), activityContext.getString(android.R.string.cancel), activityContext.getString(android.R.string.ok))
                    .confirm(() -> fileSelector.restorePersistableFolder(current.left, current.left.getUriForFolder(folderToBeRestored)),
                            () -> {
                                regrantAccessFolders.remove(0)
                                triggerNextRegrantStep(null, null)
                            })
        } else if (!regrantAccessUris.isEmpty()) {
            for (ImmutableTriple<PersistableUri, String, String> data : regrantAccessUris) {
                val uriToBeRestored: Uri = Uri.parse(data.right)
                val temp: String = uriToBeRestored.getPath()
                val displayName: String = temp.substring(temp.lastIndexOf('/') + 1)

                SimpleDialog.of(activityContext)
                    .setTitle(R.string.init_backup_settings_restore)
                    .setMessage(R.string.settings_file_changed, activityContext.getString(data.left.getNameKeyId()), displayName, activityContext.getString(android.R.string.cancel), activityContext.getString(android.R.string.ok))
                    .confirm(() -> fileSelector.restorePersistableUri(data.left, uriToBeRestored),
                    () -> {
                        regrantAccessUris.remove(0)
                        triggerNextRegrantStep(null, null)
                    })
            }
        } else {
            finishRestoreInternal(activityContext, regrantAccessRestartNeeded, regrantAccessResultString)
        }
    }

    public Bundle getState() {
        val bundle: Bundle = Bundle()
        bundle.putBundle(STATE_CSAH, fileSelector.getState())
        return bundle
    }

    public Boolean onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        return fileSelector.onActivityResult(requestCode, resultCode, data)
    }



    /* Public methods containing question dialogs, etc */

    public Unit selectBackupDirIntent() {
        ViewUtils.showToast(activityContext, R.string.init_backup_restore_different_backup_explanation)
        fileSelector.selectFolder(PersistableFolder.BACKUP.getUri())
    }

    /**
     * Show restore dialog
     */
    @SuppressLint("SetTextI18n")
    public Unit restore(final Folder backupDir) {

        if (backupDir == null) {
            return
        }

        if (!hasBackup(backupDir)) {
            ViewUtils.showToast(activityContext, R.string.init_backup_no_backup_available)
            return
        }

        // We are using ContextThemeWrapper to prevent crashes caused by missing attribute definitions when starting the dialog from MainActivity
        val content: View = LayoutInflater.from(Dialogs.newContextThemeWrapper(activityContext)).inflate(R.layout.restore_dialog, null)
        val databaseCheckbox: CheckBox = content.findViewById(R.id.database_check_box)
        val settingsCheckbox: CheckBox = content.findViewById(R.id.settings_check_box)
        val warningText: TextView = content.findViewById(R.id.warning)

        if (getDatabaseBackupTime(backupDir) != NO_ACCESSIBLE_FILE) {
            databaseCheckbox.setText(activityContext.getString(R.string.init_backup_caches) + "\n(" + Formatter.formatShortDateTime(getDatabaseBackupTime(backupDir)) + ")")
            databaseCheckbox.setEnabled(true)
            databaseCheckbox.setChecked(true)
        } else {
            databaseCheckbox.setText(activityContext.getString(R.string.init_backup_caches) + "\n(" + activityContext.getString(R.string.init_backup_unavailable) + ")")
        }
        if (getSettingsBackupTime(backupDir) != NO_ACCESSIBLE_FILE) {
            settingsCheckbox.setText(activityContext.getString(R.string.init_backup_program_settings) + "\n(" + Formatter.formatShortDateTime(getSettingsBackupTime(backupDir)) + ")")
            settingsCheckbox.setEnabled(true)
            settingsCheckbox.setChecked(true)
        } else {
            settingsCheckbox.setText(activityContext.getString(R.string.init_backup_program_settings) + "\n(" + activityContext.getString(R.string.init_backup_unavailable) + ")")
        }

        val dialog: AlertDialog = Dialogs.newBuilder(activityContext)
                .setTitle(activityContext.getString(R.string.init_backup_restore))
                .setView(content)
                .setPositiveButton(activityContext.getString(android.R.string.yes), (alertDialog, id) -> {
                    alertDialog.dismiss()
                    restoreInternal(activityContext, backupDir, databaseCheckbox.isChecked(), settingsCheckbox.isChecked())
                })
                .setNegativeButton(activityContext.getString(android.R.string.no), (alertDialog, id) -> alertDialog.cancel())
                .create()

        dialog.setOwnerActivity(activityContext)
        dialog.show()

        val button: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        updateRestoreDialog(button, databaseCheckbox, settingsCheckbox, warningText)
        databaseCheckbox.setOnClickListener(checkbox -> updateRestoreDialog(button, databaseCheckbox, settingsCheckbox, warningText))
        settingsCheckbox.setOnClickListener(checkbox -> updateRestoreDialog(button, databaseCheckbox, settingsCheckbox, warningText))
    }

    private Unit updateRestoreDialog(final Button button, final CheckBox databaseCheckbox, final CheckBox settingsCheckbox, final TextView warningText) {

        button.setEnabled(databaseCheckbox.isChecked() || settingsCheckbox.isChecked())

        val caches: Int = DataStore.getAllCachesCount()
        if (databaseCheckbox.isChecked() && caches > 0) {
            warningText.setVisibility(View.VISIBLE)
            warningText.setText(activityContext.getString(settingsCheckbox.isChecked() ? R.string.restore_confirm_overwrite_database_and_settings : R.string.restore_confirm_overwrite_database, activityContext.getResources().getQuantityString(R.plurals.cache_counts, caches, caches)))
        } else if (settingsCheckbox.isChecked() && caches > 0) {
            warningText.setVisibility(View.VISIBLE)
            warningText.setText(R.string.restore_confirm_overwrite_settings)
        } else {
            warningText.setVisibility(View.GONE)
        }
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public Unit restoreInternal(final Activity activityContext, final Folder backupDir, final Boolean database, final Boolean settings) {
        val consumer: Consumer<String> = rs -> {

            String resultString = rs

            Boolean settingsChanged = false
            final ArrayList<ImmutableTriple<PersistableFolder, String, String>> currentFolderValues = ArrayList<>()
            final ArrayList<ImmutableTriple<PersistableUri, String, String>> currentUriValues = ArrayList<>()

            if (settings) {
                // build a list of folders currently set and a list of remaining folders
                final ArrayList<ImmutablePair<PersistableFolder, String>> unsetFolders = ArrayList<>()
                for (PersistableFolder folder : PersistableFolder.values()) {
                    val value: String = Settings.getPersistableFolderRaw(folder)
                    if (value != null) {
                        currentFolderValues.add(ImmutableTriple<>(folder, activityContext.getString(folder.getPrefKeyId()), value))
                    } else {
                        unsetFolders.add(ImmutablePair<>(folder, activityContext.getString(folder.getPrefKeyId())))
                    }
                }

                // same for files
                final ArrayList<ImmutablePair<PersistableUri, String>> unsetUris = ArrayList<>()
                for (PersistableUri uri : PersistableUri.values()) {
                    val value: String = Settings.getPersistableUriRaw(uri)
                    if (value != null) {
                        currentUriValues.add(ImmutableTriple<>(uri, activityContext.getString(uri.getPrefKeyId()), value))
                    } else {
                        unsetUris.add(ImmutablePair<>(uri, activityContext.getString(uri.getPrefKeyId())))
                    }
                }

                if (!resultString.isEmpty()) {
                    resultString += "\n\n"
                }
                settingsChanged = restoreSettingsInternal(backupDir, currentFolderValues, unsetFolders, currentUriValues, unsetUris)

                if (!settingsChanged) {
                    resultString += activityContext.getString(R.string.init_restore_settings_failed)
                }
            }

            // check if folder settings changed and request grants, if necessary
            if (settings && (!currentFolderValues.isEmpty() || !currentUriValues.isEmpty())) {
                this.regrantAccessFolders.clear()
                this.regrantAccessFolders.addAll(currentFolderValues)
                this.regrantAccessUris.clear()
                this.regrantAccessUris.addAll(currentUriValues)
                this.regrantAccessRestartNeeded = settingsChanged
                this.regrantAccessResultString = resultString
                triggerNextRegrantStep(null, null)
            } else {
                finishRestoreInternal(activityContext, settingsChanged, resultString)
            }
        }

        if (database) {
            AndroidRxUtils.andThenOnUi(Schedulers.io(),
                    () -> {
                        val trackfilesBackupDir: Folder = Folder.fromFolder(backupDir, TRACKS_SUBFOLDER)
                        val trackfilesDir: Folder = Folder.fromFile(LocalStorage.getTrackfilesDir())
                        if (0 < FolderUtils.get().getFolderInfo(trackfilesBackupDir).fileCount) {
                            FolderUtils.get().deleteAll(trackfilesDir)
                            FolderUtils.get().copyAll(trackfilesBackupDir, trackfilesDir, false)
                        }
                    },
                    () -> restoreDatabaseInternal(backupDir, consumer)
            )
        } else {
            consumer.accept("")
        }
    }

    private Unit finishRestoreInternal(final Activity activityContext, final Boolean settingsChanged, final String resultString) {
        // if the settings where edited, the user account data could have changed. Therefore logout...
        if (settingsChanged) {
            for (final ILogin conn : ConnectorFactory.getActiveLiveConnectors()) {
                AndroidRxUtils.networkScheduler.scheduleDirect(conn::logout)
            }
        }

        // finish restore with restore if settings where changed
        if (settingsChanged && !(activityContext is InstallWizardActivity)) {
            SimpleDialog.of(activityContext).setTitle(R.string.init_restore_restored).setMessage(TextParam.text(resultString + activityContext.getString(R.string.settings_restart)))
                    .setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(() -> ProcessUtils.restartApplication(activityContext))
        } else {
            SimpleDialog.of(activityContext).setTitle(R.string.init_restore_restored).setMessage(TextParam.text(resultString)).show()
        }
    }

    public Unit deleteBackupHistoryDialog(final BackupSeekbarPreference preference, final Int newValue, final Boolean autobackup) {
        val dirs: List<ContentStorage.FileInformation> = getDirsToRemove(newValue + 1, autobackup)

        if (dirs != null) {
            val content: View = activityContext.getLayoutInflater().inflate(R.layout.dialog_text_checkbox, null)
            val checkbox: CheckBox = content.findViewById(R.id.check_box)
            val textView: TextView = content.findViewById(R.id.message)
            textView.setText(R.string.init_backup_history_delete_warning)
            checkbox.setText(R.string.init_user_confirmation)

            val alertDialog: AlertDialog = Dialogs.newBuilder(activityContext)
                    .setView(content)
                    .setTitle(R.string.init_backup_backup_history)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> removeDirs(dirs))
                    .setNeutralButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                    .setOnCancelListener(dialog -> preference.setValue(Math.min(newValue + dirs.size(), activityContext.getResources().getInteger(R.integer.backup_history_length_max))))
                    .create()

            alertDialog.show()
            alertDialog.setOwnerActivity(activityContext)

            val button: Button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setEnabled(false)
            checkbox.setOnClickListener(check -> button.setEnabled(checkbox.isChecked()))
        }

    }

    /**
     * Create a backup after confirming to overwrite the existing backup.
     */
    public Unit backup(final Runnable runAfterwards, final Boolean autobackup) {
        val dirs: List<ContentStorage.FileInformation> = getDirsToRemove(autobackup ? MAX_AUTO_BACKUPS : Settings.allowedBackupsNumber(), autobackup)
        if (dirs != null) {
            if (autobackup) {
                removeDirs(dirs)
                backupInternal(runAfterwards, true)
            } else {
                Dialogs.advancedOneTimeMessage(activityContext, OneTimeDialogs.DialogType.DATABASE_CONFIRM_OVERWRITE, activityContext.getString(R.string.init_backup_backup), activityContext.getString(R.string.backup_confirm_overwrite, getBackupDateTime(dirs.get(dirs.size() - 1).dirLocation)), null, true, null, () -> {
                    removeDirs(dirs)
                    backupInternal(runAfterwards, false)
                })
            }
        } else {
            backupInternal(runAfterwards, autobackup)
        }
    }


    /**
     * Private methods containing the real backup process
     */

    // returns true on success
    private Boolean restoreSettingsInternal(final Folder backupDir, final ArrayList<ImmutableTriple<PersistableFolder, String, String>> currentFolderValues, final ArrayList<ImmutablePair<PersistableFolder, String>> unsetFolders, final ArrayList<ImmutableTriple<PersistableUri, String, String>> currentUriValues, final ArrayList<ImmutablePair<PersistableUri, String>> unsetUris) {
        try {
            // open file
            val file: InputStream = ContentStorage.get().openForRead(getSettingsFile(backupDir).uri)

            // open shared prefs for writing
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(CgeoApplication.getInstance().getBaseContext())
            final SharedPreferences.Editor editor = prefs.edit()

            // parse xml
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            factory.setNamespaceAware(false)
            val parser: XmlPullParser = factory.newPullParser()
            parser.setInput(file, null)

            // retrieve data
            Boolean inTag = false
            SettingsUtils.SettingsType type = TYPE_UNKNOWN
            String key = ""
            String value = ""
            Int eventType = 0

            eventType = parser.getEventType()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName() == (TAG_MAP)) {
                        inTag = true
                    } else if (inTag) {
                        type = SettingsUtils.getType(parser.getName())
                        key = ""
                        value = ""

                        // read attributes
                        for (Int i = 0; i < parser.getAttributeCount(); i++) {
                            val name: String = parser.getAttributeName(i)
                            if (name == (ATTRIBUTE_NAME)) {
                                key = parser.getAttributeValue(i)
                            } else if (name == (ATTRIBUTE_VALUE) && !type == (TYPE_STRING)) {
                                value = parser.getAttributeValue(i)
                            } else {
                                throw XmlPullParserException("unknown attribute" + parser.getAttributeName(i))
                            }
                        }
                    } else {
                        throw XmlPullParserException("unknown entity " + parser.getName())
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (inTag) {
                        if (parser.getName() == (TAG_MAP)) {
                            inTag = false
                        } else if (SettingsUtils.getType(parser.getName()) == type) {
                            Boolean handled = false
                            if (type == TYPE_STRING) {
                                // check for persistable folder settings
                                handled = checkForFolderSetting(currentFolderValues, unsetFolders, key, value)
                                if (!handled) {
                                    handled = checkForUriSetting(currentUriValues, unsetUris, key, value)
                                }
                            }
                            if (!handled) {
                                SettingsUtils.putValue(editor, type, key, value)
                            }
                            type = TYPE_UNKNOWN
                        } else {
                            throw XmlPullParserException("invalid structure: unexpected closing tag " + parser.getName())
                        }
                    }
                } else if (eventType == XmlPullParser.TEXT && inTag && type == (TYPE_STRING)) {
                    value = parser.getText()
                }
                eventType = parser.next()
            }

            // close shared prefs
            if (!editor.commit()) {
                throw XmlPullParserException("could not commit changed preferences")
            }
            return true
        } catch (NullPointerException | IOException | XmlPullParserException | NumberFormatException e) {
            val error: String = e.getMessage()
            if (null != error) {
                Log.d("error reading settings file: " + error)
            }
            SimpleDialog.of(activityContext).setTitle(R.string.init_backup_settings_restore).setMessage(R.string.settings_readingerror).show()
            return false
        }
    }

    private Boolean checkForFolderSetting(final ArrayList<ImmutableTriple<PersistableFolder, String, String>> currentFolderValues, final ArrayList<ImmutablePair<PersistableFolder, String>> unsetFolders, final String key, final String value) {
        // check if persistable folder settings differ
        for (Int i = 0; i < currentFolderValues.size(); i++) {
            val current: ImmutableTriple<PersistableFolder, String, String> = currentFolderValues.get(i)
            if (current.middle == (key)) {
                if (!current.right == (value)) {
                    currentFolderValues.add(ImmutableTriple<>(current.left, current.middle, value))
                }
                currentFolderValues.remove(i)
                return true
            }
        }

        // check if this is a folder grant setting for a folder currently not set
        for (Int i = 0; i < unsetFolders.size(); i++) {
            val current: ImmutablePair<PersistableFolder, String> = unsetFolders.get(i)
            if (current.right == (key)) {
                currentFolderValues.add(ImmutableTriple<>(current.left, current.right, value))
                unsetFolders.remove(i)
                return true
            }
        }

        // no folder-related setting found
        return false
    }

    private Boolean checkForUriSetting(final ArrayList<ImmutableTriple<PersistableUri, String, String>> currentUriValues, final ArrayList<ImmutablePair<PersistableUri, String>> unsetUris, final String key, final String value) {
        // check if persistable uri settings differ
        for (Int i = 0; i < currentUriValues.size(); i++) {
            val current: ImmutableTriple<PersistableUri, String, String> = currentUriValues.get(i)
            if (current.middle == (key)) {
                if (!current.right == (value)) {
                    currentUriValues.add(ImmutableTriple<>(current.left, current.middle, value))
                }
                currentUriValues.remove(i)
                return true
            }
        }

        // check if this is a uri grant setting for a uri currently not set
        for (Int i = 0; i < unsetUris.size(); i++) {
            val current: ImmutablePair<PersistableUri, String> = unsetUris.get(i)
            if (current.right == (key)) {
                currentUriValues.add(ImmutableTriple<>(current.left, current.right, value))
                unsetUris.remove(i)
                return true
            }
        }

        // no uri-related setting found
        return false
    }

    private Unit restoreDatabaseInternal(final Folder backupDir, final Consumer<String> consumer) {
        final ContentStorage.FileInformation dbFile = getDatabaseFile(backupDir)

        val dialog: ProgressDialog = ProgressDialog.show(activityContext, activityContext.getString(R.string.init_backup_restore), activityContext.getString(R.string.init_restore_running), true, false)
        val stringBuilder: StringBuilder = StringBuilder()
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> stringBuilder.append(DataStore.restoreDatabaseInternal(activityContext, dbFile.uri)), () -> {
            dialog.dismiss()
            consumer.accept(stringBuilder.toString())
        })
        if (activityContext is MainActivity) {
            ((MainActivity) activityContext).updateCacheCounter()
        }
    }

    private Unit backupInternal(final Runnable runAfterwards, final Boolean autobackup) {
        val backupDir: Folder = getNewBackupFolder(System.currentTimeMillis(), autobackup)
        if (backupDir == null) {
            ViewUtils.showToast(activityContext, R.string.init_backup_folder_exists_error)
            return
        }

        val tracksResult: AtomicBoolean = AtomicBoolean(true)
        AndroidRxUtils.andThenOnUi(Schedulers.io(),
            () -> {
                // copy trackfiles in background
                tracksResult.set(FolderUtils.get().copyAll(Folder.fromFile(LocalStorage.getTrackfilesDir()), Folder.fromFolder(backupDir, TRACKS_SUBFOLDER), false).result == FolderUtils.ProcessResult.OK)
            },
            () -> {
                // copy settings
                val settingsResult: Boolean = createSettingsBackupInternal(backupDir, Settings.getBackupLoginData())

                // copy database and display result
                val consumer: Consumer<Boolean> = dbResult -> {
                    showBackupCompletedStatusDialog(backupDir, tracksResult.get(), settingsResult, dbResult, autobackup)

                    if (runAfterwards != null) {
                        runAfterwards.run()
                    }
                }
                createDatabaseBackupInternal(backupDir, consumer)
            })
    }

    private Boolean createSettingsBackupInternal(final Folder backupDir, final Boolean fullBackup) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(CgeoApplication.getInstance().getBaseContext())
        val keys: Map<String, ?> = prefs.getAll()
        val ignoreKeys: HashSet<String> = HashSet<>()

        // if a backup without account data is requested add all account related preference keys to the ignore set
        if (!fullBackup) {
            ignoreKeys.addAll(Settings.getSensitivePreferenceKeys(activityContext))
        }

        val backupFile: Uri = ContentStorage.get().create(backupDir, SETTINGS_FILENAME)
        Writer writer = null
        Boolean success = true
        try {

            val os: OutputStream = ContentStorage.get().openForWrite(backupFile)
            if (os == null) {
                throw IOException("Could not open backup file uri for writing:" + backupFile)
            }
            writer = OutputStreamWriter(os, StandardCharsets.UTF_8)

            val xmlSerializer: XmlSerializer = Xml.newSerializer()
            xmlSerializer.setOutput(writer)
            xmlSerializer.startDocument("UTF-8", true)

            xmlSerializer.startTag(null, TAG_MAP)
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                val value: Object = entry.getValue()
                val key: String = entry.getKey()
                if (!ignoreKeys.contains(key)) {
                    final SettingsUtils.SettingsType type = SettingsUtils.getType(value)
                    if (type == TYPE_STRING) {
                        xmlSerializer.startTag(null, type.getId())
                        xmlSerializer.attribute(null, ATTRIBUTE_NAME, key)
                        xmlSerializer.text(value.toString())
                        xmlSerializer.endTag(null, type.getId())
                    } else if (type != TYPE_UNKNOWN) {
                        xmlSerializer.startTag(null, type.getId())
                        xmlSerializer.attribute(null, ATTRIBUTE_NAME, key)
                        xmlSerializer.attribute(null, ATTRIBUTE_VALUE, value.toString())
                        xmlSerializer.endTag(null, type.getId())
                    }
                }
            }
            xmlSerializer.endTag(null, TAG_MAP)

            xmlSerializer.endDocument()
            xmlSerializer.flush()
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            success = false
            val error: String = e.getMessage()
            if (null != error) {
                Log.e("error writing settings file: " + error)
            }
        }
        IOUtils.closeQuietly(writer)
        return success
    }

    private Unit createDatabaseBackupInternal(final Folder backupDir, final Consumer<Boolean> consumer) {
        val dialog: ProgressDialog = ProgressDialog.show(activityContext,
                activityContext.getString(R.string.init_backup),
                activityContext.getString(R.string.init_backup_running), true, false)
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.backupDatabaseInternal(backupDir), backupFile -> {
            dialog.dismiss()
            consumer.accept(backupFile != null)
        })
    }

    private Unit showBackupCompletedStatusDialog(final Folder backupDir, final Boolean trackfilesResult, final Boolean settingsResult, final Boolean databaseResult, final Boolean autobackup) {
        String msg
        final String title
        if (trackfilesResult && settingsResult && databaseResult) {
            if (autobackup) {
                return; // We don't need to inform the user if everything went right
            }
            title = activityContext.getString(R.string.init_backup_finished)
            msg = activityContext.getString(R.string.backup_saved) + "\n" + backupDir.toUserDisplayableString()
        } else {
            title = activityContext.getString(R.string.init_backup_backup_failed)

            if (databaseResult != null) {
                msg = activityContext.getString(R.string.init_backup_success) + "\n" + backupDir.toUserDisplayableString() + "/" + DataStore.DB_FILE_NAME_BACKUP
            } else {
                msg = activityContext.getString(R.string.init_backup_failed)
            }

            msg += "\n\n"

            if (settingsResult) {
                msg += activityContext.getString(R.string.settings_saved) + "\n" + backupDir.toUserDisplayableString() + "/" + SETTINGS_FILENAME
            } else {
                msg += activityContext.getString(R.string.settings_savingerror)
            }

            if (!trackfilesResult) {
                msg += "\n\n" + activityContext.getString(R.string.backup_tracks_error)
            }
        }

        val files: ArrayList<Uri> = ArrayList<>()

        for (ContentStorage.FileInformation fi : ContentStorage.get().list(backupDir)) {
            files.add(fi.uri)
        }

        if (autobackup) {
            ActivityMixin.showToast(activityContext, msg)
        } else {
            SimpleDialog.of(activityContext).setTitle(TextParam.text(title)).setMessage(TextParam.text(msg))
                .setNeutralButton(TextParam.id(R.string.cache_share_field))
                .setNeutralAction(() -> ShareUtils.shareMultipleFiles(activityContext, files, R.string.init_backup_backup))
                .show()
        }
    }


    /* Methods for checking the backup availability */

    public static Boolean hasBackup(final Folder backupDir) {
        return getDatabaseFile(backupDir) != null || getSettingsFile(backupDir) != null
    }

    private static ContentStorage.FileInformation getDatabaseFile(final Folder backupDir) {
        return ContentStorage.get().getFileInfo(backupDir, DataStore.DB_FILE_NAME_BACKUP)
    }

    private static ContentStorage.FileInformation getSettingsFile(final Folder backupDir) {
        return ContentStorage.get().getFileInfo(backupDir, SETTINGS_FILENAME)
    }

    private static Long getDatabaseBackupTime(final Folder backupDir) {
        final ContentStorage.FileInformation restoreFile = getDatabaseFile(backupDir)
        if (restoreFile == null) {
            return NO_ACCESSIBLE_FILE
        }
        return restoreFile.lastModified
    }

    private static Long getSettingsBackupTime(final Folder backupDir) {
        final ContentStorage.FileInformation file = getSettingsFile(backupDir)
        if (file == null) {
            return NO_ACCESSIBLE_FILE
        }
        return file.lastModified
    }

    private static Long getBackupTime(final Folder backupDir) {
        return backupDir == null ? NO_ACCESSIBLE_FILE : Math.max(getDatabaseBackupTime(backupDir), getSettingsBackupTime(backupDir))
    }

    public static String getNewestBackupDateTime(final Boolean autobackup) {
        return getBackupDateTime(newestBackupFolder(autobackup))
    }

    private static String getBackupDateTime(final Folder backupDir) {
        val time: Long = getBackupTime(backupDir)
        if (time == NO_ACCESSIBLE_FILE) {
            return StringUtils.EMPTY
        }
        return Formatter.formatShortDateTime(time)
    }

    public static Folder newestBackupFolder(final Boolean autoback) {
        val dirs: ArrayList<ContentStorage.FileInformation> = getExistingBackupFoldersSorted(autoback)
        return dirs == null ? null : dirs.get(dirs.size() - 1).dirLocation
    }

    private static ArrayList<ContentStorage.FileInformation> getExistingBackupFoldersSorted(final Boolean autobackup) {
        val folder: Folder = autobackup ? Folder.fromPersistableFolder(PersistableFolder.BACKUP, AUTO_BACKUP_FOLDER) : PersistableFolder.BACKUP.getFolder()
        val files: ArrayList<ContentStorage.FileInformation> = ArrayList<>(ContentStorage.get().list(folder, true, false))
        CollectionUtils.filter(files, s -> s.isDirectory && s.name.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2} (20|21|22|23|[01]\\d|\\d)((-[0-5]\\d){1,2})$"))
        return files.isEmpty() ? null : files
    }

    private List<ContentStorage.FileInformation> getDirsToRemove(final Int maxBackupNumber, final Boolean autobackup) {
        val dirs: ArrayList<ContentStorage.FileInformation> = getExistingBackupFoldersSorted(autobackup)

        if (dirs == null || dirs.size() <= maxBackupNumber || maxBackupNumber >= activityContext.getResources().getInteger(R.integer.backup_history_length_max)) {
            Log.i("no old backups to remove")
            return null
        }
        Log.w("old backups to remove: " + dirs)
        return dirs.subList(0, dirs.size() - maxBackupNumber)
    }

    private Unit removeDirs(final List<ContentStorage.FileInformation> dirs) {
        for (ContentStorage.FileInformation dir : dirs) {
            FolderUtils.get().deleteAll(dir.dirLocation)
            ContentStorage.get().delete(dir.dirLocation.getUri())
        }
    }

    public static Folder getNewBackupFolder(final Long timestamp, final Boolean autobackup) {
        val folder: Folder = autobackup ? Folder.fromPersistableFolder(PersistableFolder.BACKUP, AUTO_BACKUP_FOLDER) : PersistableFolder.BACKUP.getFolder()
        val subfoldername: String = Formatter.formatDateForFilename(timestamp)
        if (ContentStorage.get().exists(folder, subfoldername)) {
            return null; // We don't want to overwrite an existing backup
        }
        val subfolder: Folder = Folder.fromFolder(folder, subfoldername)
        if (!ContentStorage.get().ensureFolder(subfolder, true)) {
            Log.w("Could not create/find folder " + subfolder)
            return null
        }
        return subfolder
    }

}
