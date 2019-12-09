package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Xml;
import android.widget.Toast;

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

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class SharedPrefsBackupUtils extends Activity {

    static final String TYPE_BOOLEAN    = "boolean";
    static final String TYPE_FLOAT      = "float";
    static final String TYPE_INTEGER    = "int";
    static final String TYPE_LONG       = "long";
    static final String TYPE_STRING     = "string";
    static final String TYPE_UNKNOWN    = "unknown";

    static final String ATTRIBUTE_NAME  = "name";
    static final String ATTRIBUTE_VALUE = "value";

    static final String TAG_MAP = "map";

    static final String SETTINGS_FILENAME = "cgeo-settings.xml";

    private Activity activityContext = null;

    public SharedPrefsBackupUtils(final Activity activityContext) {
        this.activityContext = activityContext;
    }

    public void backup(final Boolean fullBackup, final Runnable runAfterwards) {
        if (!fullBackup) {
            backupInternal(false, runAfterwards);
        } else {
            Dialogs.confirm(activityContext, R.string.init_backup_settings_backup_full, R.string.init_backup_settings_backup_full_confirm, (dialog, which) -> {
                backupInternal(true, runAfterwards);
            });
        }
    }

    public void restore() {
        if (!hasBackup()) {
            Toast.makeText(activityContext, R.string.init_backup_settings_restore_no, Toast.LENGTH_LONG).show();
        } else {
            Dialogs.confirm(activityContext, R.string.init_backup_settings_restore, R.string.init_backup_settings_restore_confirm, (dialog, which) -> {
                if (restoreInternal()) {
                    Dialogs.confirmYesNo(activityContext, R.string.settings_restored, R.string.settings_restart, (dialog2, which2) -> {
                        ProcessUtils.restartApplication(activityContext);
                    });
                }
            });
        }
    }

    private void backupInternal(final Boolean fullBackup, final Runnable runAfterwards) {
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
                activityContext.getString(R.string.pref_su_tokensecret), activityContext.getString(R.string.pref_su_tokenpublic), activityContext.getString(R.string.pref_temp_su_token_secret), activityContext.getString(R.string.pref_temp_su_token_public)
            );
        }

        try {
            final File backupfn = getSettingsFile();
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
                    if (value instanceof String) {
                        xmlSerializer.startTag(null, TYPE_STRING);
                        xmlSerializer.attribute(null, ATTRIBUTE_NAME, key);
                        xmlSerializer.text(value.toString());
                        xmlSerializer.endTag(null, TYPE_STRING);
                    } else {
                        String type = TYPE_UNKNOWN;
                        if (value instanceof Boolean) {
                            type = TYPE_BOOLEAN;
                        } else if (value instanceof Integer) {
                            type = TYPE_INTEGER;
                        } else if (value instanceof Long) {
                            type = TYPE_LONG;
                        } else if (value instanceof Float) {
                            type = TYPE_FLOAT;
                        }
                        if (type != TYPE_UNKNOWN) {
                            xmlSerializer.startTag(null, type);
                            xmlSerializer.attribute(null, ATTRIBUTE_NAME, key);
                            xmlSerializer.attribute(null, ATTRIBUTE_VALUE, value.toString());
                            xmlSerializer.endTag(null, type);
                        }
                    }
                }
            }
            xmlSerializer.endTag(null, TAG_MAP);

            xmlSerializer.endDocument();
            xmlSerializer.flush();
            final String dataWrite = writer.toString();
            file.write(dataWrite.getBytes());
            file.close();
            Dialogs.message(activityContext, fullBackup ? R.string.init_backup_settings_backup_full : R.string.init_backup_settings_backup_light, activityContext.getString(R.string.settings_saved) + "\n" + backupfn);
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            final String error = e.getMessage();
            if (null != error) {
                Log.e("error writing settings file: " + error);
            }
            Dialogs.message(activityContext, fullBackup ? R.string.init_backup_settings_backup_full : R.string.init_backup_settings_backup_light, R.string.settings_savingerror);
        }
        if (runAfterwards != null) {
            runAfterwards.run();
        }
    }

    // returns true on success
    private boolean restoreInternal() {
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
            String type = TYPE_UNKNOWN;
            String key = "";
            String value = "";
            int eventType = 0;

            eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals(TAG_MAP)) {
                        inTag = true;
                    } else if (inTag) {
                        type = parser.getName();
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
                        if (parser.getName().equals(type)) {
                            switch (type) {
                                case TYPE_BOOLEAN:
                                    editor.putBoolean(key, Boolean.parseBoolean(value));
                                    break;
                                case TYPE_FLOAT:
                                    editor.putFloat(key, Float.parseFloat(value));
                                    break;
                                case TYPE_INTEGER:
                                    editor.putInt(key, Integer.parseInt(value));
                                    break;
                                case TYPE_LONG:
                                    editor.putLong(key, Long.parseLong(value));
                                    break;
                                case TYPE_STRING:
                                    editor.putString(key, value);
                                    break;
                                default:
                                    throw new XmlPullParserException("unknown type");
                            }
                            type = TYPE_UNKNOWN;
                        } else if (parser.getName().equals(TAG_MAP)) {
                            inTag = false;
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
        } catch (IOException | XmlPullParserException e) {
            final String error = e.getMessage();
            if (null != error) {
                Log.d("error reading settings file: " + error);
            }
            Dialogs.message(activityContext, R.string.init_backup_settings_restore, R.string.settings_readingerror);
            return false;
        }
    }

    @Nullable
    private static File getSettingsFile() {
        return new File(LocalStorage.getBackupDirectory(), SETTINGS_FILENAME);
    }

    public static Boolean hasBackup() {
        final File file = getSettingsFile();
        return file.exists() && file.length() > 0;
    }

    @NonNull
    public static String getBackupDateTime() {
        final File file = getSettingsFile();
        if (file == null) {
            return StringUtils.EMPTY;
        }
        return Formatter.formatShortDateTime(file.lastModified());
    }

}
