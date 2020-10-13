package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectMapfileActivity;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.apps.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.playservices.GooglePlayServices;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PublicLocalFolder;
import cgeo.geocaching.storage.PublicLocalStorageActivityHelper;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapDownloadUtils;
import cgeo.geocaching.utils.ProcessUtils;
import static cgeo.geocaching.utils.MapUtils.showInvalidMapfileMessage;

import android.R.string;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.openintents.intents.FileManagerIntents;

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
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String INTENT_OPEN_SCREEN = "OPEN_SCREEN";
    public static final int NO_RESTART_NEEDED = 1;
    public static final int RESTART_NEEDED = 2;

    private final BackupUtils backupUtils = new BackupUtils(SettingsActivity.this);

    private final PublicLocalStorageActivityHelper publicLocalStorage = new PublicLocalStorageActivityHelper(this);

    /**
     * Enumeration for directory choosers. This is how we can retrieve information about the
     * directory and preference key in onActivityResult() easily just by knowing
     * the result code.
     */
    private enum DirChooserType {
        GPX_IMPORT_DIR(1, R.string.pref_gpxImportDir,
                LocalStorage.getGpxImportDirectory().getPath(), false),
        GPX_EXPORT_DIR(2, R.string.pref_gpxExportDir,
                LocalStorage.getGpxExportDirectory().getPath(), true),
        THEMES_DIR(3, R.string.pref_renderthemepath, "", false),
        BACKUP_DIR(4, R.string.pref_fakekey_preference_restore_dirselect,
                       LocalStorage.getBackupRootDirectory().getPath() + "/", false);
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
        setTheme(Settings.isLightSkin() ? R.style.settings_light : R.style.settings);
        super.onCreate(savedInstanceState);

        initDeviceSpecificPreferences();
        initUnitPreferences();
        addPreferencesFromResource(this, R.xml.preferences);
        initPreferences();

        final Intent intent = getIntent();
        openInitialScreen(intent.getIntExtra(INTENT_OPEN_SCREEN, 0));
        AndroidBeam.disable(this);

        setResult(NO_RESTART_NEEDED);

        publicLocalStorage.checkAndGrantFolderAccess(PublicLocalFolder.LOGFILES);
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
        initExtCgeoDirPreference();
        initDirChoosers();
        initDefaultNavigationPreferences();
        initBackupButtons();
        initDbLocationPreference();
        initGeoDirPreferences();
        initDebugPreference();
        initForceOrientationSensorPreference();
        initBasicMemberPreferences();
        initSend2CgeoPreferences();
        initServicePreferences();
        initNavigationMenuPreferences();
        initLanguagePreferences();
        initMaintenanceButtons();

        for (final int k : new int[] {
                R.string.pref_pass_vote, R.string.pref_signature,
                R.string.pref_mapsource, R.string.pref_renderthemepath,
                R.string.pref_gpxExportDir, R.string.pref_gpxImportDir,
                R.string.pref_fakekey_dataDir,
                R.string.pref_mapDirectory, R.string.pref_defaultNavigationTool,
                R.string.pref_defaultNavigationTool2, R.string.pref_webDeviceName,
                R.string.pref_fakekey_preference_backup, R.string.pref_twitter_cache_message,
                R.string.pref_twitter_trackable_message, R.string.pref_ec_icons }) {
            bindSummaryToStringValue(k);
        }
        bindGeocachingUserToGCVoteuser();
    }

    private void initNavigationMenuPreferences() {
        for (final NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            final Preference preference = getPreference(appEnum.preferenceKey);
            if (appEnum.app.isInstalled()) {
                preference.setEnabled(true);
            } else {
                preference.setSummary(R.string.settings_navigation_disabled);
            }
        }
        getPreference(R.string.preference_screen_basicmembers)
                .setEnabled(!Settings.isGCPremiumMember());
        redrawScreen(R.string.preference_screen_navigation_menu);
    }

    private void initServicePreferences() {
        for (final OCPreferenceKeys key : OCPreferenceKeys.values()) {
            getPreference(key.isActivePrefId).setOnPreferenceChangeListener(this);
            setWebsite(key.websitePrefId, key.authParams.host);
            getPreference(key.prefScreenId).setSummary(getServiceSummary(Settings.isOCConnectorActive(key.isActivePrefId)));
        }
        getPreference(R.string.pref_connectorGCActive).setOnPreferenceChangeListener(this);
        setWebsite(R.string.pref_fakekey_gc_website, GCConnector.getInstance().getHost());
        getPreference(R.string.preference_screen_gc).setSummary(getServiceSummary(Settings.isGCConnectorActive()));

        getPreference(R.string.pref_connectorECActive).setOnPreferenceChangeListener(this);
        setWebsite(R.string.pref_fakekey_ec_website, ECConnector.getInstance().getHost());
        getPreference(R.string.preference_screen_ec).setSummary(getServiceSummary(Settings.isECConnectorActive()));

        getPreference(R.string.pref_connectorSUActive).setOnPreferenceChangeListener(this);
        setWebsite(R.string.pref_fakekey_su_website, SuConnector.getInstance().getHost());
        getPreference(R.string.preference_screen_su).setSummary(getServiceSummary(Settings.isSUConnectorActive()));

        getPreference(R.string.pref_ratingwanted).setOnPreferenceChangeListener(this);
        setWebsite(R.string.pref_fakekey_gcvote_website, GCVote.getWebsite());
        getPreference(R.string.preference_screen_gcvote).setSummary(getServiceSummary(Settings.isRatingWanted()));

        getPreference(R.string.pref_connectorGeokretyActive).setOnPreferenceChangeListener(this);
        setWebsite(R.string.pref_fakekey_geokrety_website, "https://geokrety.org");
        setWebsite(R.string.pref_fakekey_geokretymap_website, "https://geokretymap.org");
        getPreference(R.string.preference_screen_geokrety).setSummary(getServiceSummary(Settings.isGeokretyConnectorActive()));

        setWebsite(R.string.pref_fakekey_sendtocgeo_website, "send2.cgeo.org");
        getPreference(R.string.preference_screen_sendtocgeo).setSummary(getServiceSummary(Settings.isRegisteredForSend2cgeo()));
    }

    private void setWebsite(final int preferenceKey, final String urlOrHost) {
        final Preference preference = getPreference(preferenceKey);
        preference.setOnPreferenceClickListener(preference1 -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ProcessUtils.openUri(url, SettingsActivity.this);
            return true;
        });
    }

    private static String getServiceSummary(final boolean status) {
        return status ? CgeoApplication.getInstance().getString(R.string.settings_service_active) : StringUtils.EMPTY;
    }

    private static String getKey(final int prefKeyId) {
        return CgeoApplication.getInstance().getString(prefKeyId);
    }

    private Preference getPreference(final int keyId) {
        return findPreference(this, getKey(keyId));
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
     * Fill the choice list for external private cgeo directory.
     */
    private void initExtCgeoDirPreference() {
        final Preference dataDirPref = getPreference(R.string.pref_fakekey_dataDir);
        if (LocalStorage.getAvailableExternalPrivateCgeoDirectories().size() < 2) {
            dataDirPref.setEnabled(false);
            return;
        }

        final AtomicLong usedBytes = new AtomicLong();
        dataDirPref.setOnPreferenceClickListener(preference -> {
            final ProgressDialog progress = ProgressDialog.show(SettingsActivity.this, getString(R.string.calculate_dataDir_title), getString(R.string.calculate_dataDir), true, false);
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                // calculate disk usage
                usedBytes.set(FileUtils.getSize(LocalStorage.getExternalPrivateCgeoDirectory()));
            }, () -> {
                progress.dismiss();
                showExtCgeoDirChooser(usedBytes.get());
            });
            return true;
        });

    }

    /**
     * Shows a list of available mount points.
     */
    private void showExtCgeoDirChooser(final long usedBytes) {
        final List<File> extDirs = LocalStorage.getAvailableExternalPrivateCgeoDirectories();
        final String currentExtDir = LocalStorage.getExternalPrivateCgeoDirectory().getAbsolutePath();
        final List<CharSequence> directories = new ArrayList<>();
        final List<Long> freeSpaces = new ArrayList<>();
        int selectedDirIndex = -1;
        for (final File dir : extDirs) {
            if (StringUtils.equals(currentExtDir, dir.getAbsolutePath())) {
                selectedDirIndex = directories.size();
            }
            final long freeSpace = FileUtils.getFreeDiskSpace(dir);
            freeSpaces.add(freeSpace);
            directories.add(dir.getAbsolutePath());
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle(getString(R.string.settings_title_data_dir_usage, Formatter.formatBytes(usedBytes)));
        builder.setSingleChoiceItems(new ArrayAdapter<CharSequence>(SettingsActivity.this,
                                        android.R.layout.simple_list_item_single_choice,
                                        formatDirectoryNames(directories, freeSpaces)) {
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @SuppressWarnings("null")
            @NonNull
            @Override
            public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                view.setEnabled(isEnabled(position));
                return view;
            }

            @Override
            public boolean isEnabled(final int position) {
                return usedBytes < freeSpaces.get(position);
            }
        }, selectedDirIndex, (dialog, itemId) -> {
            Dialogs.confirm(SettingsActivity.this, R.string.confirm_data_dir_move_title, R.string.confirm_data_dir_move, (dialog1, which) -> {
                final File dir = extDirs.get(itemId);
                if (!StringUtils.equals(currentExtDir, dir.getAbsolutePath())) {
                    LocalStorage.changeExternalPrivateCgeoDir(SettingsActivity.this, dir.getAbsolutePath());
                }
                Settings.setExternalPrivateCgeoDirectory(dir.getAbsolutePath());
                onPreferenceChange(getPreference(R.string.pref_fakekey_dataDir), dir.getAbsolutePath());
            });
            dialog.dismiss();
        });
        builder.setNegativeButton(string.cancel, (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private List<CharSequence> formatDirectoryNames(final List<CharSequence> directories, final List<Long> freeSpaces) {
        final List<CharSequence> truncated = Formatter.truncateCommonSubdir(directories);
        final List<CharSequence> formatted = new ArrayList<>(truncated.size());
        for (int i = 0; i < truncated.size(); i++) {
            formatted.add(getString(R.string.settings_data_dir_item, truncated.get(i), Formatter.formatBytes(freeSpaces.get(i))));
        }
        return formatted;
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

        final ListPreference defaultNavigationTool = (ListPreference) getPreference(R.string.pref_defaultNavigationTool);
        defaultNavigationTool.setEntries(entries);
        defaultNavigationTool.setEntryValues(values);
        final ListPreference defaultNavigationTool2 = (ListPreference) getPreference(R.string.pref_defaultNavigationTool2);
        defaultNavigationTool2.setEntries(entries);
        defaultNavigationTool2.setEntryValues(values);
    }

    private void initDirChoosers() {
        for (final DirChooserType dct : DirChooserType.values()) {

            getPreference(dct.keyId).setOnPreferenceClickListener(
                    preference -> {
                        startDirChooser(dct);
                        return false;
                    });
        }

        getPreference(R.string.pref_mapDirectory).setOnPreferenceClickListener(
                preference -> {
                    final Intent intent = new Intent(SettingsActivity.this,
                            SelectMapfileActivity.class);
                    startActivityForResult(intent, R.string.pref_mapDirectory);
                    return false;
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
                    getString(string.ok));
            startActivityForResult(dirChooser, dct.requestCode);
        } catch (final RuntimeException ignored) {
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

    private void initBackupButtons() {

        final Preference backup = getPreference(R.string.pref_fakekey_preference_backup);
        backup.setOnPreferenceClickListener(preference -> {
            backupUtils.backup(() -> onPreferenceChange(getPreference(R.string.pref_fakekey_preference_restore), ""));
            return true;
        });

        final Preference restore = getPreference(R.string.pref_fakekey_preference_restore);
        restore.setOnPreferenceClickListener(preference -> {
            backupUtils.restore(BackupUtils.newestBackupFolder());
            return true;
        });

        final CheckBoxPreference loginData = (CheckBoxPreference) getPreference(R.string.pref_backup_logins);
        loginData.setOnPreferenceClickListener(preference -> {
            if (Settings.getBackupLoginData()) {
                loginData.setChecked(false);
                Dialogs.confirm(SettingsActivity.this, R.string.init_backup_settings_logins, R.string.init_backup_settings_backup_full_confirm, (dialog, which) -> loginData.setChecked(true));
            }
            return true;
        });

        backupUtils.moveBackupIntoNewFolderStructureIfNeeded();
        onPreferenceChange(getPreference(R.string.pref_fakekey_preference_restore), "");

        final BackupSeekbarPreference keepOld = (BackupSeekbarPreference) getPreference(R.string.pref_backups_backup_history_length);

        keepOld.setOnPreferenceChangeListener((preference, value) -> {
            backupUtils.deleteBackupHistoryDialog((BackupSeekbarPreference) preference, (int) value);
            return true;
        });

    }

    private void initMaintenanceButtons() {
        final Preference dirMaintenance = getPreference(R.string.pref_fakekey_preference_maintenance_directories);
        dirMaintenance.setOnPreferenceClickListener(preference -> {
            // disable the button, as the cleanup runs in background and should not be invoked a second time
            preference.setEnabled(false);

            final Resources res = getResources();
            final SettingsActivity activity = SettingsActivity.this;
            final ProgressDialog dialog = ProgressDialog.show(activity, res.getString(R.string.init_maintenance), res.getString(R.string.init_maintenance_directories), true, false);
            AndroidRxUtils.andThenOnUi(Schedulers.io(), DataStore::removeObsoleteGeocacheDataDirectories, dialog::dismiss);
            return true;
            });
        getPreference(R.string.pref_memory_dump).setOnPreferenceClickListener(preference -> {
            DebugUtils.createMemoryDump(SettingsActivity.this);
            return true;
        });
        getPreference(R.string.pref_generate_logcat).setOnPreferenceClickListener(preference -> {
            DebugUtils.createLogcat(SettingsActivity.this);
            return true;
        });
        getPreference(R.string.pref_view_settings).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(this, ViewSettingsActivity.class));
            return true;
        });
    }

    private static void initDeviceSpecificPreferences() {
        // We have to ensure that those preferences are initialized so that devices with specific default values
        // will get the appropriate ones.
        Settings.setUseHardwareAcceleration(Settings.useHardwareAcceleration());
        Settings.setUseGooglePlayServices(Settings.useGooglePlayServices());
    }

    private static void initUnitPreferences() {
        Settings.setUseImperialUnits(Settings.useImperialUnits());
    }

    private void initDbLocationPreference() {
        final Preference p = getPreference(R.string.pref_dbonsdcard);
        p.setPersistent(false);
        p.setOnPreferenceClickListener(preference -> {
            final boolean oldValue = Settings.isDbOnSDCard();
            DataStore.moveDatabase(SettingsActivity.this);
            return oldValue != Settings.isDbOnSDCard();
        });
    }

    private void initDebugPreference() {
        final Preference p = getPreference(R.string.pref_debug);
        p.setOnPreferenceChangeListener((preference, newValue) -> {
            Log.setDebug((Boolean) newValue);
            return true;
        });
    }

    private void initForceOrientationSensorPreference() {
        final Preference p = getPreference(R.string.pref_force_orientation_sensor);
        p.setOnPreferenceChangeListener((preference, newValue) -> {
            final boolean useOrientationSensor = (Boolean) newValue;
            Settings.setForceOrientationSensor(useOrientationSensor);
            Sensors.getInstance().setupDirectionObservable();
            return true;
        });
        p.setEnabled(OrientationProvider.hasOrientationSensor(this) && RotationProvider.hasRotationSensor(this));
    }

    private void initLanguagePreferences() {
        final Preference p = getPreference(R.string.pref_useenglish);
        p.setOnPreferenceChangeListener((preference, newValue) -> {
            setResult(RESTART_NEEDED);
            return true;
        });
    }

    private void initGeoDirPreferences() {
        final Sensors sensors = Sensors.getInstance();
        final Preference playServices = getPreference(R.string.pref_googleplayservices);
        playServices.setOnPreferenceChangeListener((preference, newValue) -> {
            sensors.setupGeoDataObservables((Boolean) newValue, Settings.useLowPowerMode());
            return true;
        });
        playServices.setEnabled(GooglePlayServices.isAvailable());
        getPreference(R.string.pref_lowpowermode).setOnPreferenceChangeListener((preference, newValue) -> {
            final boolean useLowPower = (Boolean) newValue;
            sensors.setupGeoDataObservables(Settings.useGooglePlayServices(), useLowPower);
            sensors.setupDirectionObservable();
            return true;
        });
    }

    void initBasicMemberPreferences() {
        getPreference(R.string.preference_screen_basicmembers)
                .setEnabled(!Settings.isGCPremiumMember());
        getPreference(R.string.pref_loaddirectionimg)
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
    private void redrawScreen(final int key) {
        final Preference preference = getPreference(key);
        redrawScreen(preference);
    }

    private static void redrawScreen(final Preference preference) {
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
            case R.string.pref_fakekey_gc_authorization:
                setAuthTitle(prefKeyId, GCConnector.getInstance());
                setConnectedUsernameTitle(prefKeyId, GCConnector.getInstance());
                break;
            case R.string.pref_fakekey_ocde_authorization:
            case R.string.pref_fakekey_ocpl_authorization:
            case R.string.pref_fakekey_ocnl_authorization:
            case R.string.pref_fakekey_ocus_authorization:
            case R.string.pref_fakekey_ocro_authorization:
            case R.string.pref_fakekey_ocuk_authorization:
                final OCPreferenceKeys key = OCPreferenceKeys.getByAuthId(prefKeyId);
                if (key != null) {
                    setOCAuthTitle(key);
                    setConnectedTitle(prefKeyId, Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId));
                } else {
                    setConnectedTitle(prefKeyId, false);
                }
                break;
            case R.string.pref_fakekey_ec_authorization:
                setAuthTitle(prefKeyId, ECConnector.getInstance());
                setConnectedUsernameTitle(prefKeyId, ECConnector.getInstance());
                break;
            case R.string.pref_fakekey_su_authorization:
                setSuAuthTitle();
                setConnectedTitle(prefKeyId, Settings.hasOAuthAuthorization(R.string.pref_su_tokenpublic, R.string.pref_su_tokensecret));
                break;
            case R.string.pref_fakekey_gcvote_authorization:
                setAuthTitle(prefKeyId, GCVote.getInstance());
                setConnectedUsernameTitle(prefKeyId, GCVote.getInstance());
                break;
            case R.string.pref_fakekey_twitter_authorization:
                setTwitterAuthTitle();
                setConnectedTitle(prefKeyId, Settings.hasTwitterAuthorization());
                break;
            case R.string.pref_fakekey_geokrety_authorization:
                setGeokretyAuthTitle();
                setConnectedTitle(prefKeyId, Settings.hasGeokretyAuthorization());
                break;
            default:
                Log.e(String.format(Locale.ENGLISH, "Invalid key %d in SettingsActivity.setTitle()", prefKeyId));
        }
    }

    private void setOCAuthTitle(final OCPreferenceKeys key) {
        getPreference(key.authPrefId)
                .setTitle(getString(Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId)
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    private void setSuAuthTitle() {
        getPreference(R.string.pref_fakekey_su_authorization)
                .setTitle(getString(Settings.hasOAuthAuthorization(R.string.pref_su_tokenpublic, R.string.pref_su_tokensecret)
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    private void setTwitterAuthTitle() {
        getPreference(R.string.pref_fakekey_twitter_authorization)
                .setTitle(getString(Settings.hasTwitterAuthorization()
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    private void setGeokretyAuthTitle() {
        getPreference(R.string.pref_fakekey_geokrety_authorization)
                .setTitle(getString(Settings.hasGeokretyAuthorization()
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    private void setAuthTitle(final int prefKeyId, @NonNull final ICredentials connector) {
        final Credentials credentials = Settings.getCredentials(connector);

        getPreference(prefKeyId)
                .setTitle(getString(StringUtils.isNotBlank(credentials.getUsernameRaw())
                        ? R.string.settings_reauthorize
                        : R.string.settings_authorize));
    }

    private void setConnectedUsernameTitle(final int prefKeyId, @NonNull final ICredentials connector) {
        final Credentials credentials = Settings.getCredentials(connector);

        getPreference(prefKeyId)
                .setSummary(credentials.isValid()
                        ? getString(R.string.auth_connected_as, credentials.getUserName())
                        : getString(R.string.auth_unconnected));
    }

    private void setConnectedTitle(final int prefKeyId, final boolean hasToken) {
        getPreference(prefKeyId)
                .setSummary(getString(hasToken
                        ? R.string.auth_connected
                        : R.string.auth_unconnected));
    }

    public static void openForScreen(final int preferenceScreenKey, final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, SettingsActivity.class);
        intent.putExtra(INTENT_OPEN_SCREEN, preferenceScreenKey);
        fromActivity.startActivity(intent);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (publicLocalStorage.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        if (MapDownloadUtils.onActivityResult(this, requestCode, resultCode, data)) {
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == DirChooserType.BACKUP_DIR.requestCode) {
            backupUtils.restore(new File(data.getData().getPath()));
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
                        if (!Settings.isCurrentlySelectedMapUriValid()) {
                            showInvalidMapfileMessage(this);
                        } else {
                            // Ensure map source preference is updated accordingly.
                            // TODO: There should be a better way to find and select the map source for a map file
                            final Integer mapSourceId = mapFile.hashCode();
                            final ListPreference mapSource = (ListPreference) getPreference(R.string.pref_mapsource);
                            mapSource.setValue(mapSourceId.toString());
                            onPreferenceChange(mapSource, mapSourceId);
                        }
                    } else {
                        Settings.setMapFileDirectory(mapFile);
                    }
                }
                initMapSourcePreference();
                getPreference(R.string.pref_mapDirectory).setSummary(StringUtils.defaultString(Settings.getMapFileDirectory()));
                break;
            case R.string.pref_fakekey_dataDir:
                getPreference(R.string.pref_fakekey_dataDir).setSummary(Settings.getExternalPrivateCgeoDirectory());
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
                    setConnectedTitle(requestCode, Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId));
                    redrawScreen(key.prefScreenId);
                } else {
                    setConnectedTitle(requestCode, false);
                }
                break;
            case R.string.pref_fakekey_gc_authorization:
                setAuthTitle(requestCode, GCConnector.getInstance());
                setConnectedUsernameTitle(requestCode, GCConnector.getInstance());
                redrawScreen(R.string.preference_screen_gc);
                initBasicMemberPreferences();
                break;
            case R.string.pref_fakekey_ec_authorization:
                setAuthTitle(requestCode, ECConnector.getInstance());
                setConnectedUsernameTitle(requestCode, ECConnector.getInstance());
                redrawScreen(R.string.preference_screen_ec);
                break;
            case R.string.pref_fakekey_gcvote_authorization:
                setAuthTitle(requestCode, GCVote.getInstance());
                setConnectedUsernameTitle(requestCode, GCVote.getInstance());
                redrawScreen(R.string.init_gcvote);
                break;
            case R.string.pref_fakekey_twitter_authorization:
                setTwitterAuthTitle();
                setConnectedTitle(requestCode, Settings.hasTwitterAuthorization());
                redrawScreen(R.string.preference_screen_twitter);
                break;
            case R.string.pref_fakekey_geokrety_authorization:
                setGeokretyAuthTitle();
                setConnectedTitle(requestCode, Settings.hasGeokretyAuthorization());
                redrawScreen(R.string.preference_screen_geokrety);
                break;
            case R.string.pref_fakekey_su_authorization:
                setSuAuthTitle();
                setConnectedTitle(requestCode, Settings.hasOAuthAuthorization(R.string.pref_su_tokenpublic, R.string.pref_su_tokensecret));
                redrawScreen(R.string.preference_screen_su);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object value) {
        final PreferenceManager preferenceManager = preference.getPreferenceManager();
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
        } else if (isPreference(preference, R.string.pref_fakekey_dataDir)) {
            preference.setSummary(Settings.getExternalPrivateCgeoDirectory());
        } else if (isPreference(preference, R.string.pref_connectorOCActive)
                || isPreference(preference, R.string.pref_connectorOCPLActive)
                || isPreference(preference, R.string.pref_connectorOCNLActive)
                || isPreference(preference, R.string.pref_connectorOCUSActive)
                || isPreference(preference, R.string.pref_connectorOCROActive)
                || isPreference(preference, R.string.pref_connectorOCUKActive)
                || isPreference(preference, R.string.pref_connectorGCActive)
                || isPreference(preference, R.string.pref_connectorECActive)
                || isPreference(preference, R.string.pref_connectorSUActive)) {
            // update summary
            final boolean boolVal = (Boolean) value;
            final String summary = getServiceSummary(boolVal);
            if (OCPreferenceKeys.isOCPreference(preference.getKey())) {
                final OCPreferenceKeys prefKey = OCPreferenceKeys.getByKey(preference.getKey());
                preference.getPreferenceManager().findPreference(getKey(prefKey.prefScreenId)).setSummary(summary);
            } else if (isPreference(preference, R.string.pref_connectorGCActive)) {
                preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_gc)).setSummary(summary);
            } else if (isPreference(preference, R.string.pref_connectorECActive)) {
                preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_ec)).setSummary(summary);
            } else if (isPreference(preference, R.string.pref_connectorSUActive)) {
                preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_su)).setSummary(summary);
            }
            redrawScreen(preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_services)));
            // reset log-in status if connector activation was changed
            ConnectorFactory.forceRelog();
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
        } else if (isPreference(preference, R.string.pref_fakekey_preference_restore)) {
            final String textRestore;
            if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder())) {
                textRestore = preference.getContext().getString(R.string.init_backup_last) + " "
                        + BackupUtils.getNewestBackupDateTime();
            } else {
                textRestore = preference.getContext().getString(R.string.init_backup_last_no);
            }
            preference.setSummary(textRestore);
        } else if (isPreference(preference, R.string.pref_ratingwanted)) {
            preferenceManager.findPreference(getKey(R.string.preference_screen_gcvote)).setSummary(getServiceSummary((Boolean) value));
            redrawScreen(preferenceManager.findPreference(getKey(R.string.preference_screen_services)));
        } else if (isPreference(preference, R.string.pref_connectorGeokretyActive)) {
            preferenceManager.findPreference(getKey(R.string.preference_screen_geokrety)).setSummary(getServiceSummary((Boolean) value));
            redrawScreen(preferenceManager.findPreference(getKey(R.string.preference_screen_services)));
        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            preference.setSummary(stringValue);
        }
        return true;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    private void bindSummaryToValue(final Preference preference, final Object value) {
        // Set the listener to watch for value changes.
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference, value);
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

    private void bindGeocachingUserToGCVoteuser() {

        if (!Settings.hasGCCredentials()) {
            return;
        }

        final String value = Settings.getGcCredentials().getUserName();
        getPreference(R.string.pref_fakekey_gcvote_authorization).setSummary(value);
        final Preference prefvote = getPreference(R.string.pref_user_vote);
        bindSummaryToValue(prefvote, value);
    }

    @SuppressWarnings("deprecation")
    private static Preference findPreference(final PreferenceActivity preferenceActivity, final CharSequence key) {
        return preferenceActivity.findPreference(key);
    }

    @SuppressWarnings("deprecation")
    private static void addPreferencesFromResource(final PreferenceActivity preferenceActivity, @AnyRes final int preferencesResId) {
        preferenceActivity.addPreferencesFromResource(preferencesResId);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setPreferenceScreen(final PreferenceScreen preferenceScreen) {
        // TODO replace with fragment based code
        super.setPreferenceScreen(preferenceScreen);
    }

    @SuppressWarnings({"deprecation", "EmptyMethod"})
    @Override
    public PreferenceManager getPreferenceManager() {
        // TODO replace with fragment based code
        return super.getPreferenceManager();
    }

    private static boolean isPreference(final Preference preference, final int preferenceKeyId) {
        return getKey(preferenceKeyId).equals(preference.getKey());
    }
}
