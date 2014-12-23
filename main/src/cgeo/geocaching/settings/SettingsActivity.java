package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectMapfileActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.utils.DatabaseBackupUtils;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import org.apache.commons.lang3.StringUtils;
import org.openintents.intents.FileManagerIntents;

import rx.functions.Action0;
import rx.schedulers.Schedulers;

import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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

    private static final String INTENT_OPEN_SCREEN = "OPEN_SCREEN";

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

        initHardwareAccelerationPreferences();
        initUnitPreferences();
        SettingsActivity.addPreferencesFromResource(this, R.xml.preferences);
        initPreferences();

        final Intent intent = getIntent();
        openInitialScreen(intent.getIntExtra(INTENT_OPEN_SCREEN, 0));
    }

    private void openInitialScreen(final int initialScreen) {
        if (initialScreen == 0) {
            return;
        }
        final PreferenceScreen screen = (PreferenceScreen) getPreference(initialScreen);
        if (screen == null) {
            return;
        }
        try {
            setPreferenceScreen(screen);
        } catch (final RuntimeException e) {
            Log.e("could not open preferences " + initialScreen, e);
        }
    }

    @Override
    protected void onPause() {
        Log.i("Requesting settings backup with settings manager");
        BackupManager.dataChanged(getPackageName());
        super.onPause();
    }

    private void initPreferences() {
        initMapSourcePreference();
        initDirChoosers();
        initDefaultNavigationPreferences();
        initBackupButtons();
        initDbLocationPreference();
        initGeoDirPreferences();
        initDebugPreference();
        initBasicMemberPreferences();
        initSend2CgeoPreferences();
        initServicePreferences();
        initNavigationMenuPreferences();
        initMaintenanceButtons();

        for (final int k : new int[] { R.string.pref_username, R.string.pref_password,
                R.string.pref_pass_vote, R.string.pref_signature,
                R.string.pref_mapsource, R.string.pref_renderthemepath,
                R.string.pref_gpxExportDir, R.string.pref_gpxImportDir,
                R.string.pref_mapDirectory, R.string.pref_defaultNavigationTool,
                R.string.pref_defaultNavigationTool2, R.string.pref_webDeviceName,
                R.string.pref_fakekey_preference_backup_info, R.string.pref_twitter_cache_message, R.string.pref_twitter_trackable_message,
                R.string.pref_ecusername, R.string.pref_ecpassword, R.string.pref_ec_icons }) {
            bindSummaryToStringValue(k);
        }
    }

    private void initNavigationMenuPreferences() {
        for (final NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled()) {
                getPreference(appEnum.preferenceKey).setEnabled(true);
            }
        }
        getPreference(R.string.preference_screen_basicmembers)
                .setEnabled(!Settings.isGCPremiumMember());
        redrawScreen(R.string.preference_screen_navigation_menu);
    }

    private void initServicePreferences() {
        for (final OCPreferenceKeys key : OCPreferenceKeys.values()) {
            getPreference(key.isActivePrefId).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
            setWebsite(key.websitePrefId, key.authParams.host);
            getPreference(key.prefScreenId).setSummary(getServiceSummary(Settings.isOCConnectorActive(key.isActivePrefId)));
        }
        getPreference(R.string.pref_connectorGCActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        setWebsite(R.string.pref_fakekey_gc_website, GCConnector.getInstance().getHost());
        getPreference(R.string.preference_screen_gc).setSummary(getServiceSummary(Settings.isGCConnectorActive()));

        getPreference(R.string.pref_connectorOXActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        setWebsite(R.string.pref_fakekey_ox_website, "opencaching.com");
        getPreference(R.string.preference_screen_ox).setSummary(getServiceSummary(Settings.isOXConnectorActive()));

        getPreference(R.string.pref_connectorECActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        setWebsite(R.string.pref_fakekey_ec_website, "extremcaching.com");
        getPreference(R.string.preference_screen_ec).setSummary(getServiceSummary(Settings.isECConnectorActive()));

        getPreference(R.string.pref_ratingwanted).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        setWebsite(R.string.pref_fakekey_gcvote_website, "gcvote.com");
        getPreference(R.string.preference_screen_gcvote).setSummary(getServiceSummary(Settings.isRatingWanted()));

        setWebsite(R.string.pref_fakekey_sendtocgeo_website, "send2.cgeo.org");
        getPreference(R.string.preference_screen_sendtocgeo).setSummary(getServiceSummary(Settings.isRegisteredForSend2cgeo()));
    }

    private void setWebsite(final int preferenceKey, final String host) {
        final Preference preference = getPreference(preferenceKey);
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + host)));
                } catch (final ActivityNotFoundException e) {
                    Log.e("Cannot find suitable activity", e);
                    ActivityMixin.showToast(SettingsActivity.this, R.string.err_application_no);
                }
                return true;
            }
        });
    }

    private static String getServiceSummary(final boolean status) {
        return status ? CgeoApplication.getInstance().getString(R.string.settings_service_active) : StringUtils.EMPTY;
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
        final ListPreference pref = (ListPreference) getPreference(R.string.pref_mapsource);

        final List<MapSource> mapSources = MapProviderFactory.getMapSources();
        final CharSequence[] entries = new CharSequence[mapSources.size()];
        final CharSequence[] values = new CharSequence[mapSources.size()];
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

        final CharSequence[] entries = new CharSequence[apps.size()];
        final CharSequence[] values = new CharSequence[apps.size()];
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
                        final Intent i = new Intent(SettingsActivity.this,
                                SelectMapfileActivity.class);
                        startActivityForResult(i, R.string.pref_mapDirectory);
                        return false;
                    }
                });
    }

    /**
     * Fire up a directory chooser on click on the preference.
     *
     * The result can be processed using {@link android.app.Activity#onActivityResult}.
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
        } catch (final ActivityNotFoundException ignored) {
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
            final Preference p = getPreference(dct.keyId);
            if (p == null) {
                return;
            }
            Settings.putString(dct.keyId, directory);
            p.setSummary(directory);
        }
    }

    public void initBackupButtons() {
        final Preference backup = getPreference(R.string.pref_fakekey_preference_backup);
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

        final Preference restore = getPreference(R.string.pref_fakekey_preference_restore);
        restore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                DatabaseBackupUtils.restoreDatabase(SettingsActivity.this);
                return true;
            }
        });
    }

    public void initMaintenanceButtons() {
        final Preference dirMaintenance = getPreference(R.string.pref_fakekey_preference_maintenance_directories);
        dirMaintenance.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                // disable the button, as the cleanup runs in background and should not be invoked a second time
                preference.setEnabled(false);

                final Resources res = getResources();
                final SettingsActivity activity = SettingsActivity.this;
                final ProgressDialog dialog = ProgressDialog.show(activity, res.getString(R.string.init_maintenance), res.getString(R.string.init_maintenance_directories), true, false);
                RxUtils.andThenOnUi(Schedulers.io(), new Action0() {
                    @Override
                    public void call() {
                        DataStore.removeObsoleteCacheDirectories();
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        dialog.dismiss();
                    }
                });
                return true;
                }
            });
		final Preference memoryDumpPref = getPreference(R.string.pref_memory_dump);
		memoryDumpPref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(
                            final Preference preference) {
                        DebugUtils.createMemoryDump(SettingsActivity.this);
                        return true;
                    }
                });
    }

    public static void initHardwareAccelerationPreferences() {
        // We have to ensure that the preference is initialized so that devices with hardware acceleration disabled
        // get the appropriate value.
        Settings.setUseHardwareAcceleration(Settings.useHardwareAcceleration());
    }

    private static void initUnitPreferences() {
        Settings.setUseImperialUnits(Settings.useImperialUnits());
    }

    private void initDbLocationPreference() {
        final Preference p = getPreference(R.string.pref_dbonsdcard);
        p.setPersistent(false);
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final boolean oldValue = Settings.isDbOnSDCard();
                DataStore.moveDatabase(SettingsActivity.this);
                return oldValue != Settings.isDbOnSDCard();
            }
        });
    }

    private void initDebugPreference() {
        final Preference p = getPreference(R.string.pref_debug);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                final boolean isDebug = (Boolean) newValue;
                Log.setDebug(isDebug);
                CgeoApplication.dumpOnOutOfMemory(isDebug);
                return true;
            }
        });
    }

    private void initGeoDirPreferences() {
        final Preference playServices = getPreference(R.string.pref_googleplayservices);
        playServices.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                CgeoApplication.getInstance().setupGeoDataObservables((Boolean) newValue, Settings.useLowPowerMode());
                return true;
            }
        });
        playServices.setEnabled(CgeoApplication.getInstance().isGooglePlayServicesAvailable());
        getPreference(R.string.pref_lowpowermode).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                final CgeoApplication app = CgeoApplication.getInstance();
                final Boolean useLowPower = (Boolean) newValue;
                app.setupGeoDataObservables(Settings.useGooglePlayServices(), useLowPower);
                app.setupDirectionObservable(useLowPower);
                return true;
            }
        });
    }

    void initBasicMemberPreferences() {
        getPreference(R.string.preference_screen_basicmembers)
                .setEnabled(!Settings.isGCPremiumMember());
        getPreference(R.string.pref_loaddirectionimg)
                .setEnabled(!Settings.isGCPremiumMember());
        getPreference(R.string.pref_showcaptcha)
                .setEnabled(!Settings.isGCPremiumMember());

        redrawScreen(R.string.preference_screen_services);
    }

    /**
     * Refresh a preference screen. Has no effect when called for a preference, that is not actually a preference
     * screen.
     *
     * @param key
     *            Key of a preference screen.
     */
    void redrawScreen(final int key) {
        final Preference preference = getPreference(key);
        redrawScreen(preference);
    }

    static void redrawScreen(final Preference preference) {
        if (!(preference instanceof PreferenceScreen)) {
            return;
        }
        final PreferenceScreen screen = (PreferenceScreen) preference;
        final ListAdapter adapter = screen.getRootAdapter();
        if (adapter instanceof BaseAdapter) {
            ((BaseAdapter) adapter).notifyDataSetChanged();
        }
    }

    private static void initSend2CgeoPreferences() {
        Settings.putString(R.string.pref_webDeviceName, Settings.getWebDeviceName());
    }

    public void setAuthTitle(final int prefKeyId) {
        switch (prefKeyId) {
            case R.string.pref_fakekey_ocde_authorization:
            case R.string.pref_fakekey_ocpl_authorization:
            case R.string.pref_fakekey_ocnl_authorization:
            case R.string.pref_fakekey_ocus_authorization:
            case R.string.pref_fakekey_ocro_authorization:
            case R.string.pref_fakekey_ocuk_authorization:
                setOCAuthTitle(OCPreferenceKeys.getByAuthId(prefKeyId));
                break;
            case R.string.pref_fakekey_twitter_authorization:
                setTwitterAuthTitle();
                break;
            default:
                Log.e(String.format(Locale.ENGLISH, "Invalid key %d in SettingsActivity.setTitle()", prefKeyId));
        }
    }

    void setOCAuthTitle(final OCPreferenceKeys key) {
        if (key != null) {
            getPreference(key.authPrefId)
                    .setTitle(getString(Settings.hasOCAuthorization(key.publicTokenPrefId, key.privateTokenPrefId)
                            ? R.string.settings_reauthorize
                            : R.string.settings_authorize));
        }
    }

    void setTwitterAuthTitle() {
        getPreference(R.string.pref_fakekey_twitter_authorization)
                .setTitle(getString(Settings.hasTwitterAuthorization()
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    public static void openForScreen(final int preferenceScreenKey, final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, SettingsActivity.class);
        intent.putExtra(INTENT_OPEN_SCREEN, preferenceScreenKey);
        fromActivity.startActivity(intent);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        for (final DirChooserType dct : DirChooserType.values()) {
            if (requestCode == dct.requestCode) {
                setChosenDirectory(dct, data);
                return;
            }
        }

        switch (requestCode) {
            case R.string.pref_mapDirectory:
                if (data.hasExtra(Intents.EXTRA_MAP_FILE)) {
                    final String mapFile = data.getStringExtra(Intents.EXTRA_MAP_FILE);
                    final File file = new File(mapFile);
                    if (!file.isDirectory()) {
                        Settings.setMapFile(mapFile);
                        if (!Settings.isValidMapFile(Settings.getMapFile())) {
                            ActivityMixin.showToast(this, R.string.warn_invalid_mapfile);
                        } else {
                            // Ensure map source preference is updated accordingly.
                            // TODO: There should be a better way to find and select the map source for a map file
                            final Integer mapSourceId = mapFile.hashCode();
                            final ListPreference mapSource = (ListPreference) getPreference(R.string.pref_mapsource);
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
            case R.string.pref_fakekey_ocpl_authorization:
            case R.string.pref_fakekey_ocnl_authorization:
            case R.string.pref_fakekey_ocus_authorization:
            case R.string.pref_fakekey_ocro_authorization:
            case R.string.pref_fakekey_ocuk_authorization:
                final OCPreferenceKeys key = OCPreferenceKeys.getByAuthId(requestCode);
                if (key != null) {
                    setOCAuthTitle(key);
                    redrawScreen(key.prefScreenId);
                }
                break;
            case R.string.pref_fakekey_twitter_authorization:
                setTwitterAuthTitle();
                redrawScreen(R.string.preference_screen_twitter);
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

        private PreferenceManager preferenceManager;

        @Override
        public boolean onPreferenceChange(final Preference preference, final Object value) {
            preferenceManager = preference.getPreferenceManager();
            final String stringValue = value.toString();

            if (isPreference(preference, R.string.pref_mapsource)) {
                // reset the cached map source
                MapSource mapSource;
                try {
                    final int mapSourceId = Integer.parseInt(stringValue);
                    mapSource = MapProviderFactory.getMapSource(mapSourceId);
                } catch (final NumberFormatException e) {
                    Log.e("SettingsActivity.onPreferenceChange: bad source id '" + stringValue + "'", e);
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
            } else if (isPreference(preference, R.string.pref_connectorOCActive)
                    || isPreference(preference, R.string.pref_connectorOCPLActive)
                    || isPreference(preference, R.string.pref_connectorOCNLActive)
                    || isPreference(preference, R.string.pref_connectorOCUSActive)
                    || isPreference(preference, R.string.pref_connectorOCROActive)
                    || isPreference(preference, R.string.pref_connectorOCUKActive)
                    || isPreference(preference, R.string.pref_connectorGCActive)
                    || isPreference(preference, R.string.pref_connectorOXActive)
                    || isPreference(preference, R.string.pref_connectorECActive)) {
                // update summary
                final boolean boolVal = (Boolean) value;
                final String summary = getServiceSummary(boolVal);
                if (OCPreferenceKeys.isOCPreference(preference.getKey())) {
                    final OCPreferenceKeys prefKey = OCPreferenceKeys.getByKey(preference.getKey());
                    preference.getPreferenceManager().findPreference(getKey(prefKey.prefScreenId)).setSummary(summary);
                } else if (isPreference(preference, R.string.pref_connectorGCActive)) {
                    preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_gc)).setSummary(summary);
                } else if (isPreference(preference, R.string.pref_connectorOXActive)) {
                    preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_ox)).setSummary(summary);
                } else if (isPreference(preference, R.string.pref_connectorECActive)) {
                    preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_ec)).setSummary(summary);
                }
                redrawScreen(preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_services)));
                // reset log-in status if connector activation was changed
                CgeoApplication.getInstance().forceRelog();
            } else if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                final ListPreference listPreference = (ListPreference) preference;
                final int index = listPreference.findIndexOfValue(stringValue);

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
            } else if (isPreference(preference, R.string.pref_ratingwanted)) {
                findPreference(R.string.preference_screen_gcvote).setSummary(getServiceSummary((Boolean) value));
                redrawScreen(findPreference(R.string.preference_screen_services));
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            // TODO: do not special case geocaching.com here
            if ((isPreference(preference, R.string.pref_username) && !stringValue.equals(Settings.getUsername())) || (isPreference(preference, R.string.pref_password) && !stringValue.equals(Settings.getGcCredentials().getRight()))) {
                // reset log-in if gc user or password is changed
                CgeoApplication.getInstance().forceRelog();
            }
            return true;
        }

        private Preference findPreference(final int preferenceKeyResourceId) {
            return preferenceManager.findPreference(getKey(preferenceKeyResourceId));
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
     */
    private void bindSummaryToStringValue(final int key) {

        final Preference pref = getPreference(key);
        if (pref == null) {
            return;
        }

        final String value = PreferenceManager
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

    @SuppressWarnings("deprecation")
    @Override
    public void setPreferenceScreen(final PreferenceScreen preferenceScreen) {
        // TODO replace with fragment based code
        super.setPreferenceScreen(preferenceScreen);
    }

    @SuppressWarnings("deprecation")
    @Override
    public PreferenceManager getPreferenceManager() {
        // TODO replace with fragment based code
        return super.getPreferenceManager();
    }

    private static boolean isPreference(final Preference preference, final int preferenceKeyId) {
        return getKey(preferenceKeyId).equals(preference.getKey());
    }
}
