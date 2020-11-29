package cgeo.geocaching.utils;

import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.BackupSeekbarPreference;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;
import static cgeo.geocaching.utils.SettingsUtils.SettingsType.TYPE_STRING;
import static cgeo.geocaching.utils.SettingsUtils.SettingsType.TYPE_UNKNOWN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import static android.database.sqlite.SQLiteDatabase.OPEN_READONLY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;


public class BackupUtils extends Activity {
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String TAG_MAP = "map";
    private static final String SETTINGS_FILENAME = "cgeo-settings.xml";

    private Activity activityContext;

    public BackupUtils(final Activity activityContext) {
        this.activityContext = activityContext;
    }


    /**
     * Public methods also containing question dialogs, etc
     */

    @SuppressLint("SetTextI18n")
    public void restore(final File backupDir) {
        if (!hasBackup(backupDir)) {
            Toast.makeText(activityContext, R.string.init_backup_no_backup_available, Toast.LENGTH_LONG).show();
        } else {
            // We are using ContextThemeWrapper to prevent crashes caused by missing attribute definitions when starting the dialog from MainActivity
            final Context c = new ContextThemeWrapper(activityContext, Settings.isLightSkin() ? R.style.Dialog_Alert_light : R.style.Dialog_Alert);
            final View content = LayoutInflater.from(c).inflate(R.layout.restore_dialog, null);
            final CheckBox databaseCheckbox = content.findViewById(R.id.database_check_box);
            final CheckBox settingsCheckbox = content.findViewById(R.id.settings_check_box);
            final TextView warningText = content.findViewById(R.id.warning);

            if (getDatabaseBackupTime(backupDir) != 0) {
                databaseCheckbox.setText(activityContext.getString(R.string.init_backup_caches) + "\n(" + Formatter.formatShortDateTime(getDatabaseBackupTime(backupDir)) + ")");
                databaseCheckbox.setEnabled(true);
                databaseCheckbox.setChecked(true);
            } else {
                databaseCheckbox.setText(activityContext.getString(R.string.init_backup_caches) + "\n(" + activityContext.getString(R.string.init_backup_unavailable) + ")");
            }
            if (getSettingsBackupTime(backupDir) != 0) {
                settingsCheckbox.setText(activityContext.getString(R.string.init_backup_program_settings) + "\n(" + Formatter.formatShortDateTime(getSettingsBackupTime(backupDir)) + ")");
                settingsCheckbox.setEnabled(true);
                settingsCheckbox.setChecked(true);
            } else {
                settingsCheckbox.setText(activityContext.getString(R.string.init_backup_program_settings) + "\n(" + activityContext.getString(R.string.init_backup_unavailable) + ")");
            }

            final AlertDialog dialog = Dialogs.newBuilder(activityContext)
                    .setTitle(activityContext.getString(R.string.init_backup_restore))
                    .setView(content)
                    .setPositiveButton(activityContext.getString(android.R.string.yes), (alertDialog, id) -> {
                        final boolean database = databaseCheckbox.isChecked();
                        final boolean settings = settingsCheckbox.isChecked();

                        alertDialog.dismiss();

                        if (database) {
                            restoreDatabaseInternal(backupDir, settings);
                        } else {
                            if (restoreSettingsInternal(backupDir)) {
                                Dialogs.confirmYesNo(activityContext, R.string.init_restore_restored, R.string.settings_restart, (dialog2, which2) -> ProcessUtils.restartApplication(activityContext));
                            }
                        }
                    })
                    .setNegativeButton(activityContext.getString(android.R.string.no), (alertDialog, id) -> {
                        alertDialog.cancel();
                    })
                    .create();

            dialog.setOwnerActivity(activityContext);
            dialog.show();

            final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            updateRestoreDialog(button, databaseCheckbox, settingsCheckbox, warningText);
            databaseCheckbox.setOnClickListener(checkbox -> updateRestoreDialog(button, databaseCheckbox, settingsCheckbox, warningText));
            settingsCheckbox.setOnClickListener(checkbox -> updateRestoreDialog(button, databaseCheckbox, settingsCheckbox, warningText));

        }
    }

    private void updateRestoreDialog(final Button button, final CheckBox databaseCheckbox, final CheckBox settingsCheckbox, final TextView warningText) {

        if (databaseCheckbox.isChecked() || settingsCheckbox.isChecked()) {
            button.setEnabled(true);
        } else {
            button.setEnabled(false);
        }

        final int caches = DataStore.getAllCachesCount();
        if (databaseCheckbox.isChecked() && caches > 0) {
            warningText.setVisibility(View.VISIBLE);
            warningText.setText(activityContext.getString(settingsCheckbox.isChecked() ? R.string.restore_confirm_overwrite_database_and_settings : R.string.restore_confirm_overwrite_database, activityContext.getResources().getQuantityString(R.plurals.cache_counts, caches, caches)));
        } else if (settingsCheckbox.isChecked() && caches > 0) {
            warningText.setVisibility(View.VISIBLE);
            warningText.setText(R.string.restore_confirm_overwrite_settings);
        } else {
            warningText.setVisibility(View.GONE);
        }
    }

    public void deleteBackupHistoryDialog(final BackupSeekbarPreference preference, final int newValue) {
        final File[] dirs = getDirsToRemove(newValue + 1);

        if (dirs != null) {
            final View content = activityContext.getLayoutInflater().inflate(R.layout.dialog_text_checkbox, null);
            final CheckBox checkbox = (CheckBox) content.findViewById(R.id.check_box);
            final TextView textView = (TextView) content.findViewById(R.id.message);
            textView.setText(R.string.init_backup_history_delete_warning);
            checkbox.setText(R.string.init_user_confirmation);

            final AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(activityContext, R.style.Dialog_Alert))
                .setView(content)
                .setTitle(R.string.init_backup_backup_history)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> LocalStorage.deleteFilesOrDirectories(dirs))
                .setNeutralButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                .setOnCancelListener(dialog -> preference.setValue(Math.min(newValue + dirs.length, activityContext.getResources().getInteger(R.integer.backup_history_length_max))))
                .create();

            alertDialog.show();
            alertDialog.setOwnerActivity(activityContext);

            final Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(false);
            checkbox.setOnClickListener(check -> {
                if (checkbox.isChecked()) {
                    button.setEnabled(true);
                } else {
                    button.setEnabled(false);
                }
            });
        }

    }

    /**
     * Create a backup after confirming to overwrite the existing backup.
     */
    public void backup(final Runnable runAfterwards) {

        // avoid overwriting an existing backup with an empty database
        // (can happen directly after reinstalling the app)
        if (DataStore.getAllCachesCount() == 0) {
            Toast.makeText(activityContext, R.string.init_backup_unnecessary, Toast.LENGTH_LONG).show();
            return;
        }


        final File[] dirs = getDirsToRemove(Settings.allowedBackupsNumber());
        if (dirs != null) {
            Dialogs.advancedOneTimeMessage(activityContext, OneTimeDialogs.DialogType.DATABASE_CONFIRM_OVERWRITE, OneTimeDialogs.DialogStatus.DIALOG_SHOW, activityContext.getString(R.string.init_backup_backup), activityContext.getString(R.string.backup_confirm_overwrite, getBackupDateTime(dirs[dirs.length - 1])), true, null, () -> {
                LocalStorage.deleteFilesOrDirectories(dirs);
                backupInternal(runAfterwards);
            });
        } else {
            backupInternal(runAfterwards);
        }
    }


    /**
     * Private methods containing the real backup process
     */

    // returns true on success
    private boolean restoreSettingsInternal(final File backupDir) {
        try {
            // open file
            final FileInputStream file = new FileInputStream(getSettingsFile(backupDir));

            // open shared prefs for writing
            final SharedPreferences prefs = activityContext.getSharedPreferences(ApplicationSettings.getPreferencesName(), MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();

            // parse xml
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            final XmlPullParser parser = factory.newPullParser();
            parser.setInput(file, null);

            // retrieve data
            Boolean inTag = false;
            SettingsUtils.SettingsType type = TYPE_UNKNOWN;
            String key = "";
            String value = "";
            int eventType = 0;

            eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals(TAG_MAP)) {
                        inTag = true;
                    } else if (inTag) {
                        type = SettingsUtils.getType(parser.getName());
                        key = "";
                        value = "";

                        // read attributes
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            final String name = parser.getAttributeName(i);
                            if (name.equals(ATTRIBUTE_NAME)) {
                                key = parser.getAttributeValue(i);
                            } else if (name.equals(ATTRIBUTE_VALUE) && !type.equals(TYPE_STRING)) {
                                value = parser.getAttributeValue(i);
                            } else {
                                throw new XmlPullParserException("unknown attribute" + parser.getAttributeName(i));
                            }
                        }
                    } else {
                        throw new XmlPullParserException("unknown entity " + parser.getName());
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (inTag) {
                        if (parser.getName().equals(TAG_MAP)) {
                            inTag = false;
                        } else if (SettingsUtils.getType(parser.getName()) == type) {
                            SettingsUtils.putValue(editor, type, key, value);
                            type = TYPE_UNKNOWN;
                        } else {
                            throw new XmlPullParserException("invalid structure: unexpected closing tag " + parser.getName());
                        }
                    }
                } else if (eventType == XmlPullParser.TEXT && inTag && type.equals(TYPE_STRING)) {
                    value = parser.getText();
                }
                eventType = parser.next();
            }

            // close shared prefs
            if (!editor.commit()) {
                throw new XmlPullParserException("could not commit changed preferences");
            }
            return true;
        } catch (IOException | XmlPullParserException | NumberFormatException e) {
            final String error = e.getMessage();
            if (null != error) {
                Log.d("error reading settings file: " + error);
            }
            Dialogs.message(activityContext, R.string.init_backup_settings_restore, R.string.settings_readingerror);
            return false;
        }
    }

    private void restoreDatabaseInternal(final File backupDir, final boolean settingsAfterwards) {
        final File sourceFile = DataStore.getBackupFileInternal(backupDir, true);
        try {
            final SQLiteDatabase backup = SQLiteDatabase.openDatabase(sourceFile.getPath(), null, OPEN_READONLY);
            final int backupDbVersion = backup.getVersion();
            final int expectedDbVersion = DataStore.getExpectedDBVersion();
            if (!DataStore.versionsAreCompatible(backupDbVersion, expectedDbVersion)) {
                Dialogs.message(activityContext, R.string.init_restore_failed, String.format(activityContext.getString(R.string.init_restore_version_error), expectedDbVersion, backupDbVersion));
            } else {
                final Resources res = activityContext.getResources();
                final ProgressDialog dialog = ProgressDialog.show(activityContext, res.getString(R.string.init_backup_restore), res.getString(R.string.init_restore_running), true, false);
                final AtomicInteger restoreSuccessful = new AtomicInteger(DataStore.RESTORE_FAILED_GENERAL);
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> restoreSuccessful.set(DataStore.restoreDatabaseInternal(backupDir)), () -> {
                    dialog.dismiss();
                    final int restored = restoreSuccessful.get();
                    final String message =
                            restored == DataStore.RESTORE_SUCCESSFUL ? res.getString(R.string.init_restore_success) :
                                    restored == DataStore.RESTORE_FAILED_DBRECREATED ? res.getString(R.string.init_restore_failed_dbrecreated) :
                                            res.getString(R.string.init_restore_failed);
                    if (activityContext instanceof MainActivity) {
                        ((MainActivity) activityContext).updateCacheCounter();
                    }
                    if (settingsAfterwards && restoreSettingsInternal(backupDir)) {
                        Dialogs.confirmYesNo(activityContext, R.string.init_restore_restored, message + "\n\n" + res.getString(R.string.settings_restart), (dialog2, which2) -> ProcessUtils.restartApplication(activityContext));
                    } else {
                        Dialogs.message(activityContext, R.string.init_restore_restored, message);
                    }
                });
            }
        } catch (SQLiteException e) {
            // ignore
        }
    }

    private void backupInternal(final Runnable runAfterwards) {
        final File backupDir = LocalStorage.getNewBackupDirectory(System.currentTimeMillis());
        if (backupDir == null) {
            Toast.makeText(activityContext, R.string.init_backup_folder_exists_error, Toast.LENGTH_LONG).show();
            return;
        }
        createDatabaseBackupInternal(backupDir, createSettingsBackupInternal(backupDir, Settings.getBackupLoginData()), runAfterwards);
    }

    private File createSettingsBackupInternal(final File backupDir, final Boolean fullBackup) {
        final SharedPreferences prefs = activityContext.getSharedPreferences(ApplicationSettings.getPreferencesName(), MODE_PRIVATE);
        final Map<String, ?> keys = prefs.getAll();
        final HashSet<String> ignoreKeys = new HashSet<>();

        // if a backup without account data is requested add all account related preference keys to the ignore set
        if (!fullBackup) {
            Collections.addAll(ignoreKeys,
                    activityContext.getString(R.string.pref_username), activityContext.getString(R.string.pref_password), activityContext.getString(R.string.pref_memberstatus), activityContext.getString(R.string.pref_gccustomdate),
                    activityContext.getString(R.string.pref_ecusername), activityContext.getString(R.string.pref_ecpassword),
                    activityContext.getString(R.string.pref_user_vote), activityContext.getString(R.string.pref_pass_vote),
                    activityContext.getString(R.string.pref_twitter), activityContext.getString(R.string.pref_temp_twitter_token_secret), activityContext.getString(R.string.pref_temp_twitter_token_public), activityContext.getString(R.string.pref_twitter_token_secret), activityContext.getString(R.string.pref_twitter_token_public),
                    activityContext.getString(R.string.pref_ocde_tokensecret), activityContext.getString(R.string.pref_ocde_tokenpublic), activityContext.getString(R.string.pref_temp_ocde_token_secret), activityContext.getString(R.string.pref_temp_ocde_token_public),
                    activityContext.getString(R.string.pref_ocpl_tokensecret), activityContext.getString(R.string.pref_ocpl_tokenpublic), activityContext.getString(R.string.pref_temp_ocpl_token_secret), activityContext.getString(R.string.pref_temp_ocpl_token_public),
                    activityContext.getString(R.string.pref_ocnl_tokensecret), activityContext.getString(R.string.pref_ocnl_tokenpublic), activityContext.getString(R.string.pref_temp_ocnl_token_secret), activityContext.getString(R.string.pref_temp_ocnl_token_public),
                    activityContext.getString(R.string.pref_ocus_tokensecret), activityContext.getString(R.string.pref_ocus_tokenpublic), activityContext.getString(R.string.pref_temp_ocus_token_secret), activityContext.getString(R.string.pref_temp_ocus_token_public),
                    activityContext.getString(R.string.pref_ocro_tokensecret), activityContext.getString(R.string.pref_ocro_tokenpublic), activityContext.getString(R.string.pref_temp_ocro_token_secret), activityContext.getString(R.string.pref_temp_ocro_token_public),
                    activityContext.getString(R.string.pref_ocuk2_tokensecret), activityContext.getString(R.string.pref_ocuk2_tokenpublic), activityContext.getString(R.string.pref_temp_ocuk2_token_secret), activityContext.getString(R.string.pref_temp_ocuk2_token_public),
                    activityContext.getString(R.string.pref_su_tokensecret), activityContext.getString(R.string.pref_su_tokenpublic), activityContext.getString(R.string.pref_temp_su_token_secret), activityContext.getString(R.string.pref_temp_su_token_public),
                    activityContext.getString(R.string.pref_fakekey_geokrety_authorization)
            );
        }

        try {
            final File backupfn = new File(backupDir, SETTINGS_FILENAME);
            final FileOutputStream file = new FileOutputStream(backupfn);
            final XmlSerializer xmlSerializer = Xml.newSerializer();
            final StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);

            xmlSerializer.startTag(null, TAG_MAP);
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                final Object value = entry.getValue();
                final String key = entry.getKey();
                if (!ignoreKeys.contains(key)) {
                    final SettingsUtils.SettingsType type = SettingsUtils.getType(value);
                    if (type == TYPE_STRING) {
                        xmlSerializer.startTag(null, type.getId());
                        xmlSerializer.attribute(null, ATTRIBUTE_NAME, key);
                        xmlSerializer.text(value.toString());
                        xmlSerializer.endTag(null, type.getId());
                    } else if (type != TYPE_UNKNOWN) {
                        xmlSerializer.startTag(null, type.getId());
                        xmlSerializer.attribute(null, ATTRIBUTE_NAME, key);
                        xmlSerializer.attribute(null, ATTRIBUTE_VALUE, value.toString());
                        xmlSerializer.endTag(null, type.getId());
                    }
                }
            }
            xmlSerializer.endTag(null, TAG_MAP);

            xmlSerializer.endDocument();
            xmlSerializer.flush();
            final String dataWrite = writer.toString();
            file.write(dataWrite.getBytes());
            file.close();
            return backupfn;
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            final String error = e.getMessage();
            if (null != error) {
                Log.e("error writing settings file: " + error);
            }
            return null;
        }

    }

    private void createDatabaseBackupInternal(final File backupDir, final File settingsFile, final Runnable runAfterwards) {
        final ProgressDialog dialog = ProgressDialog.show(activityContext,
                activityContext.getString(R.string.init_backup),
                activityContext.getString(R.string.init_backup_running), true, false);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.backupDatabaseInternal(backupDir), backupFile -> {
            dialog.dismiss();
            showBackupCompletedStatusDialog(backupDir, settingsFile, backupFile);
            if (runAfterwards != null) {
                runAfterwards.run();
            }
        });
    }

    private void showBackupCompletedStatusDialog(final File backupDir, final File settingsFile, final File databaseFile) {
        String msg;
        final String title;
        if (settingsFile != null && databaseFile != null) {
            title = activityContext.getString(R.string.init_backup_finished);
            msg = activityContext.getString(R.string.backup_saved) + "\n" + backupDir + "/";
        } else {
            title = activityContext.getString(R.string.init_backup_backup_failed);

            if (databaseFile != null) {
                msg = activityContext.getString(R.string.init_backup_success) + "\n" + databaseFile;
            } else {
                msg = activityContext.getString(R.string.init_backup_failed);
            }

            msg += "\n\n";

            if (settingsFile != null) {
                msg += activityContext.getString(R.string.settings_saved) + "\n" + settingsFile;
            } else {
                msg += activityContext.getString(R.string.settings_savingerror);
            }
        }

        Dialogs.messageNeutral(activityContext, title, msg, R.string.cache_share_field,
            (dialog, which) -> ShareUtils.shareMultipleFiles(activityContext, Arrays.asList(settingsFile, databaseFile), R.string.init_backup_backup));
    }


    /**
     * Public methods for checking the backup availability
     */

    public static boolean hasBackup(final File backupDir) {
        return getDatabaseFile(backupDir) != null || getSettingsFile(backupDir) != null;
    }

    @Nullable
    public static File getDatabaseFile(final File backupDir) {
        final File fileSourceFile = DataStore.getBackupFileInternal(backupDir, true);
        return fileSourceFile.exists() && fileSourceFile.length() > 0 ? fileSourceFile : null;
    }

    @Nullable
    private static File getSettingsFile(final File backupDir) {
        final File fileSourceFile = new File(backupDir, SETTINGS_FILENAME);
        return fileSourceFile.exists() && fileSourceFile.length() > 0 ? fileSourceFile : null;
    }

    public static long getDatabaseBackupTime(final File backupDir) {
        final File restoreFile = getDatabaseFile(backupDir);
        if (restoreFile == null) {
            return 0;
        }
        return restoreFile.lastModified();
    }

    public static long getSettingsBackupTime(final File backupDir) {
        final File file = getSettingsFile(backupDir);
        if (file == null) {
            return 0;
        }
        return file.lastModified();
    }

    public static long getBackupTime(final File backupDir) {
        return backupDir == null ? 0 : Math.max(getDatabaseBackupTime(backupDir), getSettingsBackupTime(backupDir));
    }

    @NonNull
    public static String getNewestBackupDateTime() {
        return getBackupDateTime(newestBackupFolder());
    }

    @NonNull
    public static String getBackupDateTime(final File backupDir) {
        final long time = getBackupTime(backupDir);
        if (time == 0) {
            return StringUtils.EMPTY;
        }
        return Formatter.formatShortDateTime(time);
    }


    public void moveBackupIntoNewFolderStructureIfNeeded() {
        if (getExistingBackupFoldersSorted() == null) {
            final File oldFolder = LocalStorage.getBackupRootDirectory();
            final File databaseFile = DataStore.getBackupFileInternal(oldFolder, true);
            final File settingsFile = new File(LocalStorage.getBackupRootDirectory(), SETTINGS_FILENAME);

            final long timestamp = getBackupTime(oldFolder);
            if (timestamp == 0) {
                return;
            }
            final File newFolder = LocalStorage.getNewBackupDirectory(timestamp);
            if (newFolder == null) {
                return;
            }
            if (databaseFile.exists() && databaseFile.length() != 0 && databaseFile.renameTo(new File(newFolder, databaseFile.getName()))) {
                Log.iForce("The existing database backup was moved to " + newFolder.getPath());
            }
            if (settingsFile.exists() && settingsFile.length() != 0 && settingsFile.renameTo(new File(newFolder, settingsFile.getName()))) {
                Log.iForce("The existing settings backup was moved to " + newFolder.getPath());
            }
        }
    }

    @Nullable
    public static File newestBackupFolder() {
        final File[] files = getExistingBackupFoldersSorted();

        if (files == null) {
            return null;
        }

        return files [files.length - 1];
    }

    @Nullable
    public static File[] getExistingBackupFoldersSorted() {
        final File[] files = LocalStorage.getBackupRootDirectory().listFiles(s -> s.getName().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2} (20|21|22|23|[01]\\d|\\d)((-[0-5]\\d){1,2})$"));

        if (files == null || files.length == 0) {
            return null;
        }
        Arrays.sort(files);
        return files;
    }

    @Nullable
    public File[] getDirsToRemove(final int maxBackupNumber) {
        final File[] dirs = getExistingBackupFoldersSorted();

        if (dirs == null || dirs.length <= maxBackupNumber || maxBackupNumber >= activityContext.getResources().getInteger(R.integer.backup_history_length_max)) {
            Log.e("nothing to remove");
            return null;
        }
        Log.e("files for to remove");
        return Arrays.copyOfRange(dirs, 0, dirs.length - maxBackupNumber);
    }

}
