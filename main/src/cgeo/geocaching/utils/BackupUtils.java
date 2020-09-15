package cgeo.geocaching.utils;

import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;
import static cgeo.geocaching.utils.SettingsUtils.SettingsType.TYPE_STRING;
import static cgeo.geocaching.utils.SettingsUtils.SettingsType.TYPE_UNKNOWN;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Xml;
import android.view.ContextThemeWrapper;
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
    static final String ATTRIBUTE_NAME  = "name";
    static final String ATTRIBUTE_VALUE = "value";

    static final String TAG_MAP = "map";

    static final String SETTINGS_FILENAME = "cgeo-settings.xml";

    private Activity activityContext;

    public BackupUtils(final Activity activityContext) {
        this.activityContext = activityContext;
    }



    /** Public methods also containing question dialogs, etc */

    public void restore() {
        if (!hasBackup()) {
            Toast.makeText(activityContext, R.string.init_backup_no_backup_available, Toast.LENGTH_LONG).show();
        } else {

            final View content = activityContext.getLayoutInflater().inflate(R.layout.restore_dialog, null);
            final CheckBox databaseCheckbox = content.findViewById(R.id.database_check_box);
            final CheckBox settingsCheckbox = content.findViewById(R.id.settings_check_box);
            final TextView warningText = content.findViewById(R.id.warning);

            if (getDatabaseBackupTime() != 0) {
                databaseCheckbox.setText(activityContext.getString(R.string.init_backup_caches) + "\n(" + Formatter.formatShortDateTime(getDatabaseBackupTime()) + ")");
                databaseCheckbox.setEnabled(true);
                databaseCheckbox.setChecked(true);
            } else {
                databaseCheckbox.setText(activityContext.getString(R.string.init_backup_caches) + "\n(" + activityContext.getString(R.string.init_backup_unavailable) + ")");
            }
            if (getSettingsBackupTime() != 0) {
                settingsCheckbox.setText(activityContext.getString(R.string.init_backup_program_settings) + "\n(" + Formatter.formatShortDateTime(getSettingsBackupTime()) + ")");
                settingsCheckbox.setEnabled(true);
                settingsCheckbox.setChecked(true);
            } else {
                settingsCheckbox.setText(activityContext.getString(R.string.init_backup_program_settings) + "\n(" + activityContext.getString(R.string.init_backup_unavailable) + ")");
            }

            final AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(activityContext, R.style.Dialog_Alert))
                    .setTitle(activityContext.getString(R.string.init_backup_restore))
                    .setView(content)
                    .setPositiveButton(activityContext.getString(android.R.string.yes), (alertDialog, id) -> {
                        final boolean database = databaseCheckbox.isChecked();
                        final boolean settings = settingsCheckbox.isChecked();

                        alertDialog.dismiss();

                        if (database) {
                            restoreDatabaseInternal(settings);
                        } else {
                            if (restoreSettingsInternal()) {
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

            final Runnable updateDialog = () -> {
                final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

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
            };

            updateDialog.run();
            databaseCheckbox.setOnClickListener(checkbox -> updateDialog.run());
            settingsCheckbox.setOnClickListener(checkbox -> updateDialog.run());

        }
    }

    /**
     * Create a backup after confirming to overwrite the existing backup.
     *
     */
    public void backup(final Runnable runAfterwards) {

        // avoid overwriting an existing backup with an empty database
        // (can happen directly after reinstalling the app)
        if (DataStore.getAllCachesCount() == 0) {
            Dialogs.message(activityContext, R.string.init_backup_backup, activityContext.getString(R.string.init_backup_unnecessary));
            return;
        }

        if ((Settings.getBackupDatabase() && getDatabaseFile() != null) || (Settings.getBackupSettings() && getSettingsFile() != null)) {
            Dialogs.advancedOneTimeMessage(activityContext, OneTimeDialogs.DialogType.DATABASE_CONFIRM_OVERWRITE, OneTimeDialogs.DialogStatus.DIALOG_SHOW, activityContext.getString(R.string.init_backup_backup), activityContext.getString(R.string.backup_confirm_overwrite, getNewestBackupDateTime()), true, null, () -> backupInternal(runAfterwards));
        } else {
            backupInternal(runAfterwards);
        }
    }



    /** Private methods containing the real backup process */

    // returns true on success
    private boolean restoreSettingsInternal() {
        try {
            // open file
            final FileInputStream file = new FileInputStream (getSettingsFile());

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

    private void restoreDatabaseInternal(final boolean settingsAfterwards) {
        final File sourceFile = DataStore.getBackupFileInternal(true);
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
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> restoreSuccessful.set(DataStore.restoreDatabaseInternal()), () -> {
                    dialog.dismiss();
                    final int restored = restoreSuccessful.get();
                    final String message =
                            restored == DataStore.RESTORE_SUCCESSFUL ? res.getString(R.string.init_restore_success) :
                                    restored == DataStore.RESTORE_FAILED_DBRECREATED ? res.getString(R.string.init_restore_failed_dbrecreated) :
                                            res.getString(R.string.init_restore_failed);
                    if (activityContext instanceof MainActivity) {
                        ((MainActivity) activityContext).updateCacheCounter();
                    }
                    if (settingsAfterwards && restoreSettingsInternal()) {
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
        String message = "";
        if (Settings.getBackupSettings()) {
            message = "\n\n" + createSettingsBackupInternal(Settings.getBackupLoginData());
        }

        if (Settings.getBackupDatabase()) {
            createDatabaseBackupInternal(message, runAfterwards);
        } else {
            Dialogs.message(activityContext, R.string.init_backup_finished, message.trim());
            if (runAfterwards != null) {
                runAfterwards.run();
            }
        }
    }

    private String createSettingsBackupInternal(final Boolean fullBackup) {
        String exitMessage;

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
            final File backupfn = new File(LocalStorage.getBackupDirectory(), SETTINGS_FILENAME);
            FileUtils.mkdirs(backupfn.getParentFile());
            final FileOutputStream file = new FileOutputStream (backupfn);
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
            exitMessage = activityContext.getString(R.string.settings_saved) + "\n" + backupfn;
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            final String error = e.getMessage();
            if (null != error) {
                Log.e("error writing settings file: " + error);
            }
            exitMessage = activityContext.getString(R.string.settings_savingerror);
        }

        return exitMessage;
    }

    private void createDatabaseBackupInternal(final String additionalText, final Runnable runAfterwards) {
        final ProgressDialog dialog = ProgressDialog.show(activityContext,
                activityContext.getString(R.string.init_backup),
                activityContext.getString(R.string.init_backup_running), true, false);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), DataStore::backupDatabaseInternal, backupFileName -> {
            dialog.dismiss();
            Dialogs.message(activityContext,
                    R.string.init_backup_finished,
                    (backupFileName != null
                            ? activityContext.getString(R.string.init_backup_success)
                            + "\n" + backupFileName
                            : activityContext.getString(R.string.init_backup_failed)) + additionalText);
            if (runAfterwards != null) {
                runAfterwards.run();
            }
        });
    }



    /** Public methods for checking the backup availability */

    public static boolean hasBackup() {
        return getDatabaseFile() != null || getSettingsFile() != null;
    }

    @Nullable
    public static File getDatabaseFile() {
        final File fileSourceFile = DataStore.getBackupFileInternal(true);
        return fileSourceFile.exists() && fileSourceFile.length() > 0 ? fileSourceFile : null;
    }

    @Nullable
    private static File getSettingsFile() {
        final File fileSourceFile = new File(LocalStorage.getBackupDirectory(), SETTINGS_FILENAME);
        return fileSourceFile.exists() && fileSourceFile.length() > 0 ? fileSourceFile : null;
    }

    public static long getDatabaseBackupTime() {
        final File restoreFile = getDatabaseFile();
        if (restoreFile == null) {
            return 0;
        }
        return restoreFile.lastModified();
    }

    public static long getSettingsBackupTime() {
        final File file = getSettingsFile();
        if (file == null) {
            return 0;
        }
        return file.lastModified();
    }

    @NonNull
    public static String getNewestBackupDateTime() {
        final long time = Math.max(getDatabaseBackupTime(), getSettingsBackupTime());
        if (time == 0) {
            return StringUtils.EMPTY;
        }
        return Formatter.formatShortDateTime(time);
    }

}
