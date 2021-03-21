package menion.android.whereyougo.gui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.util.ArrayList;

import cgeo.geocaching.CgeoApplication;
import menion.android.whereyougo.MainApplication;
import cgeo.geocaching.R;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.preferences.Locale;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.preferences.PreviewPreference;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.ManagerNotify;
import menion.android.whereyougo.utils.StringToken;
import menion.android.whereyougo.utils.Utils;


public class XmlSettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private static final String TAG = "XmlSettingsActivity";

    private boolean needRestart;

    private static String getKey(final int prefKeyId) {
        return Locale.getString(prefKeyId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings);

        needRestart = false;

        /* workaround: I don't really know why I cannot call CustomActivity.customOnCreate(this); - OMG! */
        switch (Preferences.APPEARANCE_FONT_SIZE) {
            case PreferenceValues.VALUE_FONT_SIZE_SMALL:
                this.setTheme(R.style.FontSizeSmall);
                break;
            case PreferenceValues.VALUE_FONT_SIZE_MEDIUM:
                this.setTheme(R.style.FontSizeMedium);
                break;
            case PreferenceValues.VALUE_FONT_SIZE_LARGE:
                this.setTheme(R.style.FontSizeLarge);
                break;
        }


        addPreferencesFromResource(R.xml.whereyougo_preferences);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        /*
         * Remove internal preferences
         */
        Preference somePreference = findPreference(R.string.pref_KEY_X_HIDDEN_PREFERENCES);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removePreference(somePreference);

        /*
         * Register OnClick handler
         */
        Preference preferenceRoot = findPreference(R.string.pref_KEY_S_ROOT);
        preferenceRoot.setOnPreferenceClickListener(this);

        Preference preferenceAbout = findPreference(R.string.pref_KEY_X_ABOUT);
        if (preferenceAbout != null) {
            preferenceAbout.setOnPreferenceClickListener(this);

        }

        /*
         * Workaround: Update/set value preview
         */
        // String dir = Preferences.getStringPreference( R.string.pref_KEY_S_ROOT );
        // x.setSummary( "(" + dir + ") " + Locale.getString( R.string.pref_root_desc ) ); // TODO make it better :-(

        /* TODO - check this code */
        if (!Utils.isAndroid201OrMore()) {
            Preference prefSensorFilter = findPreference(R.string.pref_KEY_S_SENSORS_ORIENT_FILTER);
            if (prefSensorFilter != null) {
                prefSensorFilter.setEnabled(false);
            }
        }

        if (getIntent() != null && getIntent().hasExtra(getString(R.string.pref_KEY_X_LOGIN_PREFERENCES))) {
            Preference preferenceLogin = findPreference(R.string.pref_KEY_X_LOGIN_PREFERENCES);
            if (preferenceLogin != null) {
                PreferenceScreen screen = getPreferenceScreen();
                for (int i = 0; i < screen.getPreferenceCount(); ++i) {
                    if (screen.getPreference(i) == preferenceLogin) {
                        getIntent().putExtra(getString(R.string.pref_KEY_X_LOGIN_PREFERENCES), false);
                        screen.onItemClick(null, null, i, 0);
                        break;
                    }
                }
            }
        }

        /*
         * Enable/disable status bar propertie
         */
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CheckBoxPreference status_bar = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_STATUSBAR);
            CheckBoxPreference gps_hide = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_GPS_DISABLE_WHEN_HIDE);
            CheckBoxPreference gps_guiding = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_GUIDING_GPS_REQUIRED);
            CheckBoxPreference screen_off = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_RUN_SCREEN_OFF);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && screen_off.isChecked()) {
                status_bar.setEnabled(false);
            } else {
                if (gps_hide.isChecked()) {
                    status_bar.setEnabled(!gps_guiding.isChecked());
                } else {
                    status_bar.setEnabled(false);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            if (needRestart) {
                A.getMain().showDialogFinish(WhereYouGoActivity.FINISH_RESTART);
            }
        } catch (Exception e) {
            Logger.e(TAG, "onDestroy()", e);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        boolean status = false;
        String key = preference.getKey();

        if (key.equals(getString(R.string.pref_KEY_S_ROOT))) {
            UtilsGUI.dialogDoItem(this, getText(R.string.pref_root), R.drawable.var_empty, getText(R.string.pref_root_desc),
                    getString(R.string.cancel), null,
                    getString(R.string.folder_select), (dialog, which) -> {
//                        FileChooserDialog selectDialog = new FileChooserDialog(XmlSettingsActivity.this);
//                        selectDialog.loadFolder(Preferences.GLOBAL_ROOT);
//                        selectDialog.setFolderMode(true);
//                        selectDialog.setCanCreateFiles(false);
//                        selectDialog.setShowCancelButton(true);
//                        selectDialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
//                            public void onFileSelected(Dialog source, File folder) {
//                                source.dismiss();
//                                if (((MainApplication) A.getApp()).setRoot(folder.getAbsolutePath())) {
//                                    PreviewPreference preferenceRoot = (PreviewPreference) findPreference(R.string.pref_KEY_S_ROOT);
//                                    preferenceRoot.setValue(FileSystem.ROOT);
//                                    MainActivity.refreshCartridges();
//                                }
//                            }
//
//                            public void onFileSelected(Dialog source, File folder, String name) {
//                                String newFolder = folder.getAbsolutePath() + "/" + name;
//                                new File(newFolder).mkdir();
//                                ((FileChooserDialog) source).loadFolder(newFolder);
//                            }
//                        });
//                        selectDialog.show();
                    },
                    getString(R.string.folder_default), (dialog, which) -> {
                        if (((CgeoApplication) A.getApp()).setRoot(null)) {
                            PreviewPreference preferenceRoot = (PreviewPreference) findPreference(R.string.pref_KEY_S_ROOT);
                            preferenceRoot.setValue(FileSystem.ROOT);
                            WhereYouGoActivity.refreshCartridges();
                        }
                    });
            return false;
        }
        return status;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_FONT_SIZE)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.APPEARANCE_FONT_SIZE = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_FULLSCREEN)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.APPEARANCE_FULLSCREEN = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_GPS)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GPS = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_GPS_START_AUTOMATICALLY)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GPS_START_AUTOMATICALLY = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_GPS_ALTITUDE_MANUAL_CORRECTION)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.GPS_ALTITUDE_CORRECTION = Utils.parseDouble(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_GPS_BEEP_ON_GPS_FIX)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GPS_BEEP_ON_GPS_FIX = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_GPS_DISABLE_WHEN_HIDE)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GPS_DISABLE_WHEN_HIDE = Utils.parseBoolean(newValue);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                CheckBoxPreference status_bar = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_STATUSBAR);
                CheckBoxPreference gps_guideing = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_GUIDING_GPS_REQUIRED);
                CheckBoxPreference screen_off = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_RUN_SCREEN_OFF);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && screen_off.isChecked()) {
                    status_bar.setEnabled(false);
                } else {
                 if (newValue) {
                     status_bar.setEnabled(!gps_guideing.isChecked());
                 } else {
                    status_bar.setEnabled(false);
                 }
                }
            }
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_GUIDING_COMPASS_SOUNDS)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GUIDING_SOUNDS = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_GUIDING_GPS_REQUIRED)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GUIDING_GPS_REQUIRED = Utils.parseBoolean(newValue);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                CheckBoxPreference status_bar = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_STATUSBAR);
                CheckBoxPreference screen_off = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_RUN_SCREEN_OFF);
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && screen_off.isChecked()) {
                    status_bar.setEnabled(false);
                } else {
                    status_bar.setEnabled(!newValue);
                }
            }
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND)) {
            String newValue = sharedPreferences.getString(key, null);
            int result = Utils.parseInt(newValue);
            if (result == PreferenceValues.VALUE_GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("audio/*");
                if (!Utils.isIntentAvailable(intent)) {
                    intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                }
                this.startActivityForResult(intent, R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND);
            } else {
                Preferences.GUIDING_WAYPOINT_SOUND = result;
            }
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND_DISTANCE)) {
            String newValue = sharedPreferences.getString(key, null);
            int value = Utils.parseInt(newValue);
            if (value > 0) {
                Preferences.GUIDING_WAYPOINT_SOUND_DISTANCE = value;
            } else {
                ManagerNotify.toastShortMessage(R.string.invalid_value);
            }
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_GUIDING_ZONE_POINT)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.GUIDING_ZONE_NAVIGATION_POINT = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_HARDWARE_COMPASS_AUTO_CHANGE)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.SENSOR_HARDWARE_COMPASS_AUTO_CHANGE = Utils.parseBoolean(newValue);
            A.getRotator().manageSensors();
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_HARDWARE_COMPASS_AUTO_CHANGE_VALUE)) {
            String newValue = sharedPreferences.getString(key, null);
            int value = Utils.parseInt(newValue);
            if (value > 0) {
                Preferences.SENSOR_HARDWARE_COMPASS_AUTO_CHANGE_VALUE = value;
            } else {
                ManagerNotify.toastShortMessage(R.string.invalid_value);
            }
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_SENSOR_HARDWARE_COMPASS)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.SENSOR_HARDWARE_COMPASS = Utils.parseBoolean(newValue);
            A.getRotator().manageSensors();
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_HIGHLIGHT)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.APPEARANCE_HIGHLIGHT = Utils.parseInt(newValue);
            PreferenceValues.enableWakeLock();
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_IMAGE_STRETCH)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.APPEARANCE_IMAGE_STRETCH = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_LANGUAGE)) {
            String lang = sharedPreferences.getString(key, "");
            ArrayList<String> loc = StringToken.parse(lang, "_");
            Configuration config = getBaseContext().getResources().getConfiguration();
            java.util.Locale locale;
            if ("default".equals(lang)) {
                locale = java.util.Locale.getDefault();
            } else if (loc.size() == 1) {
                locale = new java.util.Locale(lang);
            } else if (loc.size() == 2) {
                locale = new java.util.Locale(loc.get(0), loc.get(1));
            } else {
                locale = config.locale;
            }
            if (!config.locale.getLanguage().equals(locale.getLanguage())) {
                config.locale = locale;
                getBaseContext().getResources().updateConfiguration(config,
                        getBaseContext().getResources().getDisplayMetrics());
                needRestart = true;
            }
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_MAP_PROVIDER)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.GLOBAL_MAP_PROVIDER = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_SAVEGAME_AUTO)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GLOBAL_SAVEGAME_AUTO = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_SAVEGAME_SLOTS)) {
            String newValue = sharedPreferences.getString(key, null);
            int value = Utils.parseInt(newValue);
            if (value >= 0) {
                Preferences.GLOBAL_SAVEGAME_SLOTS = value;
            } else {
                ManagerNotify.toastShortMessage(R.string.invalid_value);
            }
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_DOUBLE_CLICK)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GLOBAL_DOUBLE_CLICK = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_GC_USERNAME)) {
            Preferences.GC_USERNAME = sharedPreferences.getString(key, null);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_GC_PASSWORD)) {
            Preferences.GC_PASSWORD = sharedPreferences.getString(key, null);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_SENSORS_BEARING_TRUE)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.SENSOR_BEARING_TRUE = Utils.parseBoolean(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_SENSORS_ORIENT_FILTER)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.SENSOR_ORIENT_FILTER = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_STATUSBAR)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.APPEARANCE_STATUSBAR = Utils.parseBoolean(newValue);
        }
        // TODO - Preferences.GPS_MIN_TIME is used but there is no settings option - default value?
        // else if ( Preferences.comparePreferenceKey( key, R.string.pref_KEY_S_GPS_MIN_TIME_NOTIFICATION ) ) {
        // Preferences.GPS_MIN_TIME =
        //}
        else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_UNITS_ALTITUDE)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.FORMAT_ALTITUDE = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_UNITS_ANGLE)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.FORMAT_ANGLE = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_UNITS_COO_LATLON)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.FORMAT_COO_LATLON = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_UNITS_LENGTH)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.FORMAT_LENGTH = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_S_UNITS_SPEED)) {
            String newValue = sharedPreferences.getString(key, null);
            Preferences.FORMAT_SPEED = Utils.parseInt(newValue);
        } else if (Preferences.comparePreferenceKey(key, R.string.pref_KEY_B_RUN_SCREEN_OFF)) {
            boolean newValue = sharedPreferences.getBoolean(key, false);
            Preferences.GLOBAL_RUN_SCREEN_OFF = Utils.parseBoolean(newValue);
            CheckBoxPreference status_bar = (CheckBoxPreference) findPreference(R.string.pref_KEY_B_STATUSBAR);
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && newValue) {
                status_bar.setEnabled(false);
            }
            PreferenceValues.enableWakeLock();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    Logger.d(TAG, "uri:" + uri.toString());
                    Preferences.setStringPreference(R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND,
                            PreferenceValues.VALUE_GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND);
                    Preferences.setStringPreference(R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND_URI,
                            uri.toString());
                    Preferences.GUIDING_WAYPOINT_SOUND = Utils.parseInt(R.string.pref_VALUE_GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND);
                    Preferences.GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND_URI = uri.toString();
                }
            }
        }
    }

    private Preference findPreference(final int keyId) {
        return findPreference(getKey(keyId));
    }
}



