package cgeo.geocaching.settings;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.apps.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.brouter.util.DefaultFilesUtils;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.al.ALConnector;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.playservices.GooglePlayServices;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ShareUtils;

import android.R.string;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

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

    public static final String STATE_CSAH = "csah";
    public static final String STATE_BACKUPUTILS = "backuputils";

    private BackupUtils backupUtils = null;

    private ContentStorageActivityHelper contentStorageHelper = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ApplicationSettings.setLocale(this);
        setTheme(Settings.isLightSkin(this) ? R.style.settings_light : R.style.settings);
        super.onCreate(savedInstanceState);

        backupUtils = new BackupUtils(SettingsActivity.this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS));

        this.contentStorageHelper = new ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_CSAH))
            .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FOLDER_PERSISTED, PersistableFolder.class, folder ->  {
                getPreference(folder.getPrefKeyId()).setSummary(folder.toUserDisplayableValue());
                if (PersistableFolder.OFFLINE_MAP_THEMES.equals(folder)) {
                    RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder();
                }
            });

        initDeviceSpecificPreferences();
        initUnitPreferences();
        addPreferencesFromResource(this, R.xml.preferences);
        initPreferences();

        final Intent intent = getIntent();
        openInitialScreen(intent.getIntExtra(INTENT_OPEN_SCREEN, 0));
        AndroidBeam.disable(this);

        setResult(NO_RESTART_NEEDED);
    }

    // set up toolbar for settings' main screen
    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addToolbar((LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent(), getString(R.string.settings_titlebar), v -> finish());
    }

    // set up toolbar for nested preference screen
    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // If the user has clicked on a preference screen, set up toolbar
        if (preference instanceof PreferenceScreen) {
            final Dialog dialog = ((PreferenceScreen) preference).getDialog();
            final View temp = (View) dialog.findViewById(android.R.id.list).getParent();
            addToolbar((LinearLayout) (Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? temp : temp.getParent()), preference.getTitle(), v -> dialog.dismiss());
        }
        return false;
    }

    private void addToolbar(final LinearLayout root, final CharSequence title, final View.OnClickListener onClickListener) {
        final Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setTitle(title);
        bar.setNavigationOnClickListener(onClickListener);
        // @todo Remove next two lines after switching to AppCompatActitivy
        bar.setTitleTextColor(getResources().getColor(R.color.colorTextActionBar));
        bar.setBackgroundColor(getResources().getColor(R.color.colorBackgroundActionBar));
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBundle(STATE_CSAH, contentStorageHelper.getState());
        savedInstanceState.putBundle(STATE_BACKUPUTILS, backupUtils.getState());
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
        initAppearancePreferences();
        initMapSourcePreference();
        initExtCgeoDirPreference();
        initDefaultNavigationPreferences();
        initBackupButtons();
        initDbLocationPreference();
        initMapPreferences();
        initOfflineRoutingPreferences();
        initGeoDirPreferences();
        initDebugPreference();
        initForceOrientationSensorPreference();
        initBasicMemberPreferences();
        initSend2CgeoPreferences();
        initServicePreferences();
        initNavigationMenuPreferences();
        initLanguagePreferences();
        initMaintenanceButtons();
        initCacheDetailsPreference();

        for (final int k : new int[] {
                R.string.pref_pass_vote, R.string.pref_signature,
                R.string.pref_fakekey_dataDir,
                R.string.pref_defaultNavigationTool,
                R.string.pref_defaultNavigationTool2, R.string.pref_webDeviceName,
                R.string.pref_fakekey_preference_backup, R.string.pref_twitter_cache_message,
                R.string.pref_twitter_trackable_message, R.string.pref_ec_icons, R.string.pref_selected_language }) {
            bindSummaryToStringValue(k);
        }
        bindGeocachingUserToGCVoteuser();

        //PublicFolder initialization
        initPublicFolders(PersistableFolder.values());
    }

    /**
     * Fill the choice list for map sources.
     */
    private void initMapSourcePreference() {
        final ListPreference pref = (ListPreference) getPreference(R.string.pref_mapsource);

        final Collection<MapSource> mapSources = MapProviderFactory.getMapSources();
        final CharSequence[] entries = new CharSequence[mapSources.size()];
        final CharSequence[] values = new CharSequence[mapSources.size()];
        int idx = 0;
        for (MapSource mapSource : MapProviderFactory.getMapSources()) {
            entries[idx] = mapSource.getName();
            values[idx] = mapSource.getId();
            idx++;
        }
        pref.setEntries(entries);
        pref.setEntryValues(values);
        pref.setOnPreferenceChangeListener(this);
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

    private void initCacheDetailsPreference() {
        final CheckBoxPreference customTabsPreference = (CheckBoxPreference) getPreference(R.string.pref_customtabs_as_browser);
        if (!ProcessUtils.isChromeLaunchable()) {
            customTabsPreference.setEnabled(false);
            customTabsPreference.setChecked(false);
        }
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

        getPreference(R.string.pref_connectorALActive).setOnPreferenceChangeListener(this);
        setWebsite(R.string.pref_fakekey_al_website, ALConnector.getInstance().getHost());
        initLCServicePreference(Settings.isGCConnectorActive());

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

    private void initLCServicePreference(final boolean gcConnectorActive) {
        final boolean isActiveGCPM = gcConnectorActive && Settings.isGCPremiumMember();
        getPreference(R.string.preference_screen_lc).setSummary(getLcServiceSummary(Settings.isALConnectorActive(), gcConnectorActive));
        if (isActiveGCPM) {
            getPreference(R.string.pref_connectorALActive).setEnabled(true);
        }
    }

    private String getLcServiceSummary(final boolean lcConnectorActive, final boolean gcConnectorActive) {
        if (!lcConnectorActive) {
            return StringUtils.EMPTY;
        }

        //lc service is set to active by user. Check whether it can actually be actived due to GC conditions
        final int lcStatusTextId = gcConnectorActive && Settings.isGCPremiumMember() ?
            R.string.settings_service_active : R.string.settings_service_active_unavailable;

        return CgeoApplication.getInstance().getString(lcStatusTextId);
    }

    private void setWebsite(final int preferenceKey, final String urlOrHost) {
        final Preference preference = getPreference(preferenceKey);
        preference.setOnPreferenceClickListener(preference1 -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(SettingsActivity.this, url);
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

        final AlertDialog.Builder builder = Dialogs.newBuilder(SettingsActivity.this);
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
            SimpleDialog.of(SettingsActivity.this).setTitle(R.string.confirm_data_dir_move_title).setMessage(R.string.confirm_data_dir_move).confirm((dialog1, which) -> {
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

    private void initPublicFolders(final PersistableFolder[] folders) {

        if (!Settings.isDebug()) {
            hidePreference(getPreference(PersistableFolder.TEST_FOLDER.getPrefKeyId()), R.string.pref_group_localfilesystem);
        }

        for (PersistableFolder folder : folders) {
            final Preference pref = getPreference(folder.getPrefKeyId());
            if (pref == null) {
                continue;
            }

            bindSummaryToValue(pref, folder.toUserDisplayableValue());
            pref.setOnPreferenceClickListener(p -> {
                contentStorageHelper.selectPersistableFolder(folder);
                return false;
            });
            folder.registerChangeListener(this, f -> pref.setSummary(f.toUserDisplayableValue()));
        }
    }

    private void hidePreference(final Preference prefToHide, @AnyRes final int prefGroupId) {
        if (prefToHide == null) {
            return;
        }
        prefToHide.setEnabled(false);
        if (prefGroupId != 0) {
            //Before API26/O there are tricks necessary to get the prefereneGroup
            final Preference prefGroup = getPreference(prefGroupId);
            if (prefGroup instanceof PreferenceGroup) {
                ((PreferenceGroup) prefGroup).removePreference(prefToHide);
            }
        }
    }

    private void initAppearancePreferences() {
        final Preference themePref = getPreference(R.string.pref_theme_setting);
        themePref.setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
            final Settings.DarkModeSetting darkTheme = Settings.DarkModeSetting.valueOf((String) newValue);
            Settings.setAppTheme(darkTheme);

            // simulate previous view stack hierarchy
            startActivity(new Intent(this, SettingsActivity.class));
            openForScreen(R.string.pref_appearance, this);
            finish();

            return true;
        });
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

        final Preference restoreFromDir = getPreference(R.string.pref_fakekey_preference_restore_dirselect);
        restoreFromDir.setOnPreferenceClickListener(preference -> {
            backupUtils.selectBackupDirIntent();
            return true;
        });

        final CheckBoxPreference loginData = (CheckBoxPreference) getPreference(R.string.pref_backup_logins);
        loginData.setOnPreferenceClickListener(preference -> {
            if (Settings.getBackupLoginData()) {
                loginData.setChecked(false);
                SimpleDialog.of(SettingsActivity.this).setTitle(R.string.init_backup_settings_logins).setMessage(R.string.init_backup_settings_backup_full_confirm).confirm((dialog, which) -> loginData.setChecked(true));
            }
            return true;
        });

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
        getPreference(R.string.pref_generate_infos_downloadmanager).setOnPreferenceClickListener(preference -> {
            DebugUtils.dumpDownloadmanagerInfos(SettingsActivity.this);
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
        final String[] entries = new String[BuildConfig.TRANSLATION_ARRAY.length + 1];
        final String[] entryValues = new String[BuildConfig.TRANSLATION_ARRAY.length + 1];
        final Locale currentLocale = Settings.getApplicationLocale();

        entries[0] = getString(R.string.init_use_default_language);
        entryValues[0] = "";
        for (int i = 0; i < BuildConfig.TRANSLATION_ARRAY.length; i++) {
            entryValues[1 + i] = BuildConfig.TRANSLATION_ARRAY[i];
            final Locale l = new Locale(BuildConfig.TRANSLATION_ARRAY[i], "");
            entries[1 + i] = BuildConfig.TRANSLATION_ARRAY[i] + " (" + l.getDisplayLanguage(currentLocale) + ")";
        }

        final ListPreference selectedLanguage = (ListPreference) getPreference(R.string.pref_selected_language);
        selectedLanguage.setEntries(entries);
        selectedLanguage.setEntryValues(entryValues);
    }

    private void initMapPreferences() {
        getPreference(R.string.pref_bigSmileysOnMap).setOnPreferenceChangeListener((preference, newValue) -> {
            setResult(RESTART_NEEDED);
            return true;
        });

        getPreference(R.string.pref_renderthemefolder_synctolocal).setOnPreferenceChangeListener((preference, newValue) ->
            RenderThemeHelper.changeSyncSetting(this, (newValue instanceof Boolean) ? ((Boolean) newValue).booleanValue() : false, changedValue -> {
                ((CheckBoxPreference) getPreference(R.string.pref_renderthemefolder_synctolocal)).setChecked(changedValue);
        }));
    }

    private void initOfflineRoutingPreferences() {
        DefaultFilesUtils.checkDefaultFiles();
        getPreference(R.string.pref_useInternalRouting).setOnPreferenceChangeListener((preference, newValue) -> {
            updateRoutingPrefs(!Settings.useInternalRouting());
            return true;
        });
        updateRoutingPrefs(Settings.useInternalRouting());
        updateRoutingProfilesPrefs();
    }

    private void updateRoutingProfilesPrefs() {
        final ArrayList<String> profiles = new ArrayList<>();
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.ROUTING_BASE);
        for (ContentStorage.FileInformation file : files) {
            if (file.name.endsWith(BRouterConstants.BROUTER_PROFILE_FILEEXTENSION)) {
                profiles.add(file.name);
            }
        }
        final CharSequence[] entries = profiles.toArray(new CharSequence[0]);
        final CharSequence[] values = profiles.toArray(new CharSequence[0]);
        updateRoutingProfilePref(R.string.pref_brouterProfileWalk, RoutingMode.WALK, entries, values);
        updateRoutingProfilePref(R.string.pref_brouterProfileBike, RoutingMode.BIKE, entries, values);
        updateRoutingProfilePref(R.string.pref_brouterProfileCar, RoutingMode.CAR, entries, values);
    }

    private void updateRoutingProfilePref(@StringRes final int prefId, final RoutingMode mode, final CharSequence[] entries, final CharSequence[] values) {
        final String current = Settings.getRoutingProfile(mode);
        final ListPreference pref = (ListPreference) getPreference(prefId);
        pref.setEntries(entries);
        pref.setEntryValues(values);
        pref.setSummary(current);
        if (current != null) {
            for (int i = 0; i < entries.length; i++) {
                if (current.contentEquals(entries[i])) {
                    pref.setValueIndex(i);
                    break;
                }
            }
        }
    }

    private void updateRoutingPrefs(final boolean useInternalRouting) {
        final boolean anyRoutingAvailable = useInternalRouting || ProcessUtils.isInstalled(getString(R.string.package_brouter));
        getPreference(R.string.pref_fakekey_brouterDistanceThresholdTitle).setEnabled(anyRoutingAvailable);
        getPreference(R.string.pref_brouterDistanceThreshold).setEnabled(anyRoutingAvailable);
        getPreference(R.string.pref_brouterShowBothDistances).setEnabled(anyRoutingAvailable);
        getPreference(R.string.pref_brouterProfileWalk).setEnabled(useInternalRouting);
        getPreference(R.string.pref_brouterProfileBike).setEnabled(useInternalRouting);
        getPreference(R.string.pref_brouterProfileCar).setEnabled(useInternalRouting);
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

        if (contentStorageHelper.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (backupUtils.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        if (DownloaderUtils.onActivityResult(this, requestCode, resultCode, data)) {
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
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
                initLCServicePreference(Settings.isGCConnectorActive());
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
                mapSource = MapProviderFactory.getMapSource(stringValue);
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
                || isPreference(preference, R.string.pref_connectorALActive)
                || isPreference(preference, R.string.pref_connectorSUActive)) {
            // update summary
            final boolean boolVal = (Boolean) value;
            final String summary = getServiceSummary(boolVal);
            if (OCPreferenceKeys.isOCPreference(preference.getKey())) {
                final OCPreferenceKeys prefKey = OCPreferenceKeys.getByKey(preference.getKey());
                preference.getPreferenceManager().findPreference(getKey(prefKey.prefScreenId)).setSummary(summary);
            } else if (isPreference(preference, R.string.pref_connectorGCActive)) {
                preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_gc)).setSummary(summary);
                initLCServicePreference(boolVal);
            } else if (isPreference(preference, R.string.pref_connectorECActive)) {
                preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_ec)).setSummary(summary);
            } else if (isPreference(preference, R.string.pref_connectorALActive)) {
                preference.getPreferenceManager().findPreference(getKey(R.string.preference_screen_lc)).setSummary(getLcServiceSummary(boolVal, Settings.isGCConnectorActive()));
                setResult(RESTART_NEEDED);
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

            if (isPreference(preference, R.string.pref_selected_language)) {
                setResult(RESTART_NEEDED);
            }

            // Set the summary to reflect the new value.
            preference.setSummary(
                    index >= 0
                            ? (isPreference(preference, R.string.pref_selected_language) ? getString(R.string.init_summary_select_language) : "") + listPreference.getEntries()[index]
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
