package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectMapfileActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.utils.DatabaseBackupUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.openintents.intents.FileManagerIntents;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design: Settings</a> for design
 * guidelines and the <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for
 * more information on developing a Settings UI.
 *
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String INTENT_GOTO = "GOTO";
    private static final int INTENT_GOTO_SERVICES = 1;

    /**
     * Enumeration for directory choosers. This is how we can retrieve information about the
     * directory and preference key in onActivityResult() easily just by knowing
     * the result code.
     */
    private enum DirChooserType {
        GPX_IMPORT_DIR(1, R.string.pref_gpxImportDir,
                Environment.getExternalStorageDirectory().getPath() + "/gpx", false),
        GPX_EXPORT_DIR(2, R.string.pref_gpxExportDir,
                Environment.getExternalStorageDirectory().getPath() + "/gpx", true),
        THEMES_DIR(3, R.string.pref_renderthemepath, "", false);
        public final int requestCode;
        public final int keyId;
        public final String defaultValue;
        public final boolean writeMode;

        DirChooserType(final int requestCode, final int keyId, final String defaultValue, final boolean writeMode) {
            this.requestCode = requestCode;
            this.keyId = keyId;
            this.defaultValue = defaultValue;
            this.writeMode = writeMode;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // Set light skin in preferences only for devices > 2.x, it doesn't work under 2.x
        setTheme(Settings.isLightSkin() && Build.VERSION.SDK_INT > 10 ? R.style.settings_light : R.style.settings);
        super.onCreate(savedInstanceState);

        SettingsActivity.addPreferencesFromResource(this, R.xml.preferences);
        initPreferences();

        Intent intent = getIntent();
        int gotoPage = intent.getIntExtra(INTENT_GOTO, 0);
        if (gotoPage == INTENT_GOTO_SERVICES) {
            // start with services screen
            PreferenceScreen main = (PreferenceScreen) getPreference(R.string.pref_fakekey_main_screen);
            try {
                if (main != null) {
                    int index = getPreference(R.string.pref_fakekey_services_screen).getOrder();
                    main.onItemClick(null, null, index, 0);
                }
            } catch (RuntimeException e) {
                Log.e("could not open services preferences", e);
            }
        }
    }

    @Override
    protected void onPause() {
        Compatibility.dataChanged(getPackageName());
        super.onPause();
    }

    private void initPreferences() {
        initMapSourcePreference();
        initDirChoosers();
        initDefaultNavigationPreferences();
        initBackupButtons();
        initDbLocationPreference();
        initDebugPreference();
        initBasicMemberPreferences();
        initSend2CgeoPreferences();
        initServicePreferences();
        initNavigationMenuPreferences();

        for (int k : new int[] { R.string.pref_username, R.string.pref_password,
                R.string.pref_pass_vote, R.string.pref_signature,
                R.string.pref_mapsource, R.string.pref_renderthemepath,
                R.string.pref_gpxExportDir, R.string.pref_gpxImportDir,
                R.string.pref_mapDirectory, R.string.pref_defaultNavigationTool,
                R.string.pref_defaultNavigationTool2, R.string.pref_webDeviceName,
                R.string.pref_fakekey_preference_backup_info, R.string.pref_twitter_cache_message, R.string.pref_twitter_trackable_message,
                R.string.pref_ecusername, R.string.pref_ecpassword, R.string.pref_ec_icons }) {
            bindSummaryToStringValue(k);
        }
        getPreference(R.string.pref_units).setDefaultValue(Settings.getImperialUnitsDefault());
    }

    private void initNavigationMenuPreferences() {
        for (NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled()) {
                getPreference(appEnum.preferenceKey).setEnabled(true);
            }
        }
        getPreference(R.string.pref_fakekey_basicmembers_screen)
                .setEnabled(!Settings.isPremiumMember());
        redrawScreen(R.string.pref_fakekey_navigation_menu_screen);
    }

    private void initServicePreferences() {
        getPreference(R.string.pref_connectorOCActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        getPreference(R.string.pref_connectorOCPLActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        getPreference(R.string.pref_connectorGCActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        getPreference(R.string.pref_connectorECActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        setWebsite(R.string.pref_fakekey_gc_website, GCConnector.getInstance().getHost());
        setWebsite(R.string.pref_fakekey_ocde_website, "opencaching.de");
        setWebsite(R.string.pref_fakekey_ocpl_website, "opencaching.pl");
        setWebsite(R.string.pref_fakekey_ec_website, "extremcaching.com");
        setWebsite(R.string.pref_fakekey_gcvote_website, "gcvote.com");
        setWebsite(R.string.pref_fakekey_sendtocgeo_website, "send2.cgeo.org");
    }

    private void setWebsite(final int preferenceKey, final String host) {
        Preference preference = getPreference(preferenceKey);
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + host)));
                return true;
            }
        });
    }

    private static String getKey(final int prefKeyId) {
        return CgeoApplication.getInstance().getString(prefKeyId);
    }

    private Preference getPreference(final int keyId) {
        return SettingsActivity.findPreference(this, getKey(keyId));
    }

    /**
     * Fill the choice list for map sources.
     */
    private void initMapSourcePreference() {
        ListPreference pref = (ListPreference) getPreference(R.string.pref_mapsource);

        List<MapSource> mapSources = MapProviderFactory.getMapSources();
        CharSequence[] entries = new CharSequence[mapSources.size()];
        CharSequence[] values = new CharSequence[mapSources.size()];
        for (int i = 0; i < mapSources.size(); ++i) {
            entries[i] = mapSources.get(i).getName();
            values[i] = String.valueOf(mapSources.get(i).getNumericalId());
        }
        pref.setEntries(entries);
        pref.setEntryValues(values);
    }

    /**
     * Fill the choice list for default navigation tools.
     */
    private void initDefaultNavigationPreferences() {

        final List<NavigationAppsEnum> apps = NavigationAppFactory.getInstalledDefaultNavigationApps();

        CharSequence[] entries = new CharSequence[apps.size()];
        CharSequence[] values = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); ++i) {
            entries[i] = apps.get(i).toString();
            values[i] = String.valueOf(apps.get(i).id);
        }

        ListPreference pref = (ListPreference) getPreference(R.string.pref_defaultNavigationTool);
        pref.setEntries(entries);
        pref.setEntryValues(values);
        pref = (ListPreference) getPreference(R.string.pref_defaultNavigationTool2);
        pref.setEntries(entries);
        pref.setEntryValues(values);
    }

    private void initDirChoosers() {
        for (final DirChooserType dct : DirChooserType.values()) {

            getPreference(dct.keyId).setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(final Preference preference) {
                            startDirChooser(dct);
                            return false;
                        }
                    });
        }

        getPreference(R.string.pref_mapDirectory).setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        Intent i = new Intent(SettingsActivity.this,
                                SelectMapfileActivity.class);
                        startActivityForResult(i, R.string.pref_mapDirectory);
                        return false;
                    }
                });
    }

    /**
     * Fire up a directory chooser on click on the preference.
     *
     * @see #onActivityResult() for processing of the selected directory
     *
     * @param dct
     *            type of directory to be selected
     */
    private void startDirChooser(final DirChooserType dct) {

        final String startDirectory = Settings.getString(dct.keyId, dct.defaultValue);

        try {
            final Intent dirChooser = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
            if (StringUtils.isNotBlank(startDirectory)) {
                dirChooser.setData(Uri.fromFile(new File(startDirectory)));
            }
            dirChooser.putExtra(FileManagerIntents.EXTRA_TITLE,
                    getString(R.string.simple_dir_chooser_title));
            dirChooser.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
                    getString(android.R.string.ok));
            startActivityForResult(dirChooser, dct.requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            // OI file manager not available
            final Intent dirChooser = new Intent(this, SimpleDirChooser.class);
            dirChooser.putExtra(Intents.EXTRA_START_DIR, startDirectory);
            dirChooser.putExtra(SimpleDirChooser.EXTRA_CHOOSE_FOR_WRITING, dct.writeMode);
            startActivityForResult(dirChooser, dct.requestCode);
        }
    }

    private void setChosenDirectory(final DirChooserType dct, final Intent data) {
        final String directory = new File(data.getData().getPath()).getAbsolutePath();
        if (StringUtils.isNotBlank(directory)) {
            Preference p = getPreference(dct.keyId);
            if (p == null) {
                return;
            }
            Settings.putString(dct.keyId, directory);
            p.setSummary(directory);
        }
    }

    public void initBackupButtons() {
        Preference backup = getPreference(R.string.pref_fakekey_preference_backup);
        backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                return DatabaseBackupUtils.createBackup(SettingsActivity.this, new Runnable() {

                    @Override
                    public void run() {
                        VALUE_CHANGE_LISTENER.onPreferenceChange(SettingsActivity.this.getPreference(R.string.pref_fakekey_preference_backup_info), "");
                    }
                });
            }
        });

        Preference restore = getPreference(R.string.pref_fakekey_preference_restore);
        restore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                DatabaseBackupUtils.restoreDatabase(SettingsActivity.this);
                return true;
            }
        });
    }

    private void initDbLocationPreference() {
        Preference p = getPreference(R.string.pref_dbonsdcard);
        p.setPersistent(false);
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                boolean oldValue = Settings.isDbOnSDCard();
                ((CgeoApplication) SettingsActivity.this.getApplication())
                        .moveDatabase(SettingsActivity.this);
                return oldValue != Settings.isDbOnSDCard();
            }
        });
    }

    private void initDebugPreference() {
        Preference p = getPreference(R.string.pref_debug);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                Log.setDebug((Boolean) newValue);
                return true;
            }
        });
    }

    void initBasicMemberPreferences() {
        getPreference(R.string.pref_fakekey_basicmembers_screen)
                .setEnabled(!Settings.isPremiumMember());
        getPreference(R.string.pref_loaddirectionimg)
                .setEnabled(!Settings.isPremiumMember());
        getPreference(R.string.pref_showcaptcha)
                .setEnabled(!Settings.isPremiumMember());

        redrawScreen(R.string.pref_fakekey_services_screen);
    }

    void redrawScreen(int key) {
        PreferenceScreen screen = (PreferenceScreen) getPreference(key);
        if (screen == null) {
            return;
        }
        ListAdapter adapter = screen.getRootAdapter();
        if (adapter instanceof BaseAdapter) {
            ((BaseAdapter) adapter).notifyDataSetChanged();
        }
    }

    private static void initSend2CgeoPreferences() {
        Settings.putString(R.string.pref_webDeviceName, Settings.getWebDeviceName());
    }

    public void setOcAuthTitle(int prefKeyId) {
        //TODO: Generalize!
        switch (prefKeyId) {
            case R.string.pref_fakekey_ocde_authorization:
                setOCDEAuthTitle();
                break;
            case R.string.pref_fakekey_ocpl_authorization:
                setOCPLAuthTitle();
                break;
            case R.string.pref_fakekey_twitter_authorization:
                setTwitterAuthTitle();
                break;
            default:
                Log.e(String.format(Locale.ENGLISH, "Invalid key %d in SettingsActivity.setTitle()", prefKeyId));
        }
    }

    void setOCDEAuthTitle() {
        getPreference(R.string.pref_fakekey_ocde_authorization)
                .setTitle(getString(Settings.hasOCAuthorization(R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret)
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    void setOCPLAuthTitle() {
        getPreference(R.string.pref_fakekey_ocpl_authorization)
                .setTitle(getString(Settings.hasOCAuthorization(R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret)
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    void setTwitterAuthTitle() {
        getPreference(R.string.pref_fakekey_twitter_authorization)
                .setTitle(getString(Settings.hasTwitterAuthorization()
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    public static void jumpToServicesPage(final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, SettingsActivity.class);
        intent.putExtra(INTENT_GOTO, INTENT_GOTO_SERVICES);
        fromActivity.startActivity(intent);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        for (DirChooserType dct : DirChooserType.values()) {
            if (requestCode == dct.requestCode) {
                setChosenDirectory(dct, data);
                return;
            }
        }

        switch (requestCode) {
            case R.string.pref_mapDirectory:
                if (data.hasExtra(Intents.EXTRA_MAP_FILE)) {
                    final String mapFile = data.getStringExtra(Intents.EXTRA_MAP_FILE);
                    File file = new File(mapFile);
                    if (!file.isDirectory()) {
                        Settings.setMapFile(mapFile);
                        if (!Settings.isValidMapFile(Settings.getMapFile())) {
                            ActivityMixin.showToast(this, R.string.warn_invalid_mapfile);
                        } else {
                            // Ensure map source preference is updated accordingly.
                            // TODO: There should be a better way to find and select the map source for a map file
                            Integer mapSourceId = mapFile.hashCode();
                            ListPreference mapSource = (ListPreference) getPreference(R.string.pref_mapsource);
                            mapSource.setValue(mapSourceId.toString());
                            VALUE_CHANGE_LISTENER.onPreferenceChange(mapSource, mapSourceId);
                        }
                    } else {
                        Settings.setMapFileDirectory(mapFile);
                    }
                }
                initMapSourcePreference();
                getPreference(R.string.pref_mapDirectory).setSummary(StringUtils.defaultString(Settings.getMapFileDirectory()));
                break;
            case R.string.pref_fakekey_ocde_authorization:
                setOCDEAuthTitle();
                redrawScreen(R.string.pref_fakekey_services_screen);
                break;
            case R.string.pref_fakekey_ocpl_authorization:
                setOCPLAuthTitle();
                redrawScreen(R.string.pref_fakekey_services_screen);
                break;
            case R.string.pref_fakekey_twitter_authorization:
                setTwitterAuthTitle();
                redrawScreen(R.string.pref_fakekey_services_screen);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static final Preference.OnPreferenceChangeListener VALUE_CHANGE_LISTENER = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object value) {
            String stringValue = value.toString();

            if (preference instanceof EditPasswordPreference) {
                if (StringUtils.isBlank((String) value)) {
                    preference.setSummary(StringUtils.EMPTY);
                } else {
                    preference.setSummary(StringUtils.repeat("\u2022 ", 10));
                }
            } else if (isPreference(preference, R.string.pref_mapsource)) {
                // reset the cached map source
                MapSource mapSource;
                try {
                    final int mapSourceId = Integer.valueOf(stringValue);
                    mapSource = MapProviderFactory.getMapSource(mapSourceId);
                } catch (final NumberFormatException e) {
                    Log.e("SettingsActivity.onPreferenceChange: bad source id `" + stringValue + "'");
                    mapSource = null;
                }
                // If there is no corresponding map source (because some map sources were
                // removed from the device since) then use the first one available.
                if (mapSource == null) {
                    mapSource = MapProviderFactory.getAnyMapSource();
                    if (mapSource == null) {
                        // There are no map source. There is little we can do here, except log an error and
                        // return to avoid triggering a null pointer exception.
                        Log.e("SettingsActivity.onPreferenceChange: no map source available");
                        return true;
                    }
                }
                Settings.setMapSource(mapSource);
                preference.setSummary(mapSource.getName());
            } else if (isPreference(preference, R.string.pref_connectorOCActive) || isPreference(preference, R.string.pref_connectorOCPLActive) || isPreference(preference, R.string.pref_connectorGCActive) || isPreference(preference, R.string.pref_connectorECActive)) {
                // // reset log-in status if connector activation was changed
                CgeoApplication.getInstance().checkLogin = true;
            } else if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else if (isPreference(preference, R.string.pref_fakekey_preference_backup_info)) {
                final String text;
                if (DatabaseBackupUtils.hasBackup()) {
                    text = preference.getContext().getString(R.string.init_backup_last) + " "
                            + DatabaseBackupUtils.getBackupDateTime();
                } else {
                    text = preference.getContext().getString(R.string.init_backup_last_no);
                }
                preference.setSummary(text);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            if ((isPreference(preference, R.string.pref_username) && !stringValue.equals(Settings.getUsername())) || (isPreference(preference, R.string.pref_password) && !stringValue.equals(Settings.getGcLogin().getRight()))) {
                // reset log-in if gc user or password is changed
                if (Login.isActualLoginStatus()) {
                    Login.logout();
                }
                CgeoApplication.getInstance().checkLogin = true;
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #VALUE_CHANGE_LISTENER
     */
    private static void bindSummaryToValue(final Preference preference, final Object value) {
        // Set the listener to watch for value changes.
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);

        // Trigger the listener immediately with the preference's
        // current value.
        VALUE_CHANGE_LISTENER.onPreferenceChange(preference, value);
    }

    /**
     * auto-care for the summary of the preference of string type with this key
     *
     * @param key
     */
    private void bindSummaryToStringValue(final int key) {

        Preference pref = getPreference(key);
        if (pref == null) {
            return;
        }

        String value = PreferenceManager
                .getDefaultSharedPreferences(pref.getContext())
                .getString(pref.getKey(), "");

        bindSummaryToValue(pref, value);
    }

    @SuppressWarnings("deprecation")
    public static Preference findPreference(final PreferenceActivity preferenceActivity, final CharSequence key) {
        return preferenceActivity.findPreference(key);
    }

    @SuppressWarnings("deprecation")
    public static void addPreferencesFromResource(final PreferenceActivity preferenceActivity, final int preferencesResId) {
        preferenceActivity.addPreferencesFromResource(preferencesResId);
    }

    private static boolean isPreference(final Preference preference, int preferenceKeyId) {
        return getKey(preferenceKeyId).equals(preference.getKey());
    }
}
