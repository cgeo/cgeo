package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.address.AndroidGeocoder;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.gc.PocketQueryListActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.databinding.MainActivityBinding;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.helper.UsefulAppsActivity;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.permission.PermissionGrantedCallback;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.GnssStatusProvider;
import cgeo.geocaching.sensors.GnssStatusProvider.Status;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.Version;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.SearchView.OnSuggestionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jakewharton.processphoenix.ProcessPhoenix;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;
import org.apache.commons.lang3.StringUtils;

public class MainActivity extends AbstractActionBarActivity {

    private static final String STATE_BACKUPUTILS = "backuputils";

    private MainActivityBinding binding;

    /**
     * view of the action bar search
     */
    private SearchView searchView;
    private MenuItem searchItem;
    private Geopoint addCoords = null;
    private boolean initialized = false;
    private boolean restoreMessageShown = false;
    private ConnectivityChangeReceiver connectivityChangeReceiver;

    private final UpdateLocation locationUpdater = new UpdateLocation();
    private final Handler updateUserInfoHandler = new UpdateUserInfoHandler(this);
    private final Handler firstLoginHandler = new FirstLoginHandler(this);
    /**
     * initialization with an empty subscription
     */
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();

    private BackupUtils backupUtils = null;

    private static final class UpdateUserInfoHandler extends WeakReferenceHandler<MainActivity> {

        UpdateUserInfoHandler(final MainActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            final MainActivity activity = getReference();
            if (activity != null) {
                // Get active connectors with login status
                final ILogin[] loginConns = ConnectorFactory.getActiveLiveConnectors();

                // Update UI
                activity.binding.infoArea.setAdapter(new ArrayAdapter<ILogin>(activity, R.layout.main_activity_connectorstatus, loginConns) {
                    @Override
                    public View getView(final int position, final View convertView, @NonNull final android.view.ViewGroup parent) {
                        TextView rowView = (TextView) convertView;
                        if (rowView == null) {
                            rowView = (TextView) activity.getLayoutInflater().inflate(R.layout.main_activity_connectorstatus, parent, false);
                        }

                        final ILogin connector = getItem(position);
                        fillView(rowView, connector);
                        return rowView;

                    }

                    private void fillView(final TextView connectorInfo, final ILogin conn) {
                        boolean offlineFoundsAvailable = false;

                        final StringBuilder userInfo = new StringBuilder(conn.getNameAbbreviated()).append(Formatter.SEPARATOR);
                        if (conn.isLoggedIn()) {
                            userInfo.append(conn.getUserName()).append(" ");
                            if (conn.getCachesFound() >= 0) {
                                FoundNumCounter.updateFoundNum(conn.getName(), conn.getCachesFound());
                            }
                            activity.checkLoggedIn();
                        }

                        final FoundNumCounter f = FoundNumCounter.load(conn.getName());
                        if (f != null) {

                            userInfo.append('(').append(f.getCounter(false));

                            if (Settings.isDisplayOfflineLogsHomescreen()) {
                                final int offlinefounds = DataStore.getFoundsOffline(conn.getName());
                                if (offlinefounds > 0) {
                                    userInfo.append(" + ").append(offlinefounds);

                                    offlineFoundsAvailable = true;
                                }
                            }
                            userInfo.append(')').append(Formatter.SEPARATOR);

                        } else if (conn.isLoggedIn()) {
                            userInfo.append(Formatter.SEPARATOR);
                        }

                        userInfo.append(conn.getLoginStatusString());

                        connectorInfo.setText(userInfo);
                        connectorInfo.setOnClickListener(v -> SettingsActivity.openForScreen(R.string.preference_screen_services, activity));

                        if (offlineFoundsAvailable) {
                            Dialogs.basicOneTimeMessage((Activity) getContext(), OneTimeDialogs.DialogType.EXPLAIN_OFFLINE_FOUND_COUNTER);
                        }
                    }
                });
            }
        }
    }

    private final class ConnectivityChangeReceiver extends BroadcastReceiver {
        private boolean isConnected = Network.isConnected();

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final boolean wasConnected = isConnected;
            isConnected = Network.isConnected();
            if (isConnected && !wasConnected) {
                startBackgroundLogin();
            }
        }
    }

    /**
     * check if at least one connector has been logged in successfully
     * and set visibility of warning message accordingly
     */
    public void checkLoggedIn() {
        final ILogin[] activeConnectors = ConnectorFactory.getActiveLiveConnectors();
        for (final IConnector conn : activeConnectors) {
            if (((ILogin) conn).isLoggedIn()) {
                binding.infoNotloggedin.setVisibility(View.INVISIBLE);
                return;
            }
        }
        binding.infoNotloggedin.setVisibility(View.VISIBLE);
    }

    private static String formatAddress(final Address address) {
        final List<String> addressParts = new ArrayList<>();

        final String countryName = address.getCountryName();
        if (countryName != null) {
            addressParts.add(countryName);
        }
        final String locality = address.getLocality();
        if (locality != null) {
            addressParts.add(locality);
        } else {
            final String adminArea = address.getAdminArea();
            if (adminArea != null) {
                addressParts.add(adminArea);
            }
        }
        return StringUtils.join(addressParts, ", ");
    }

    private final Consumer<GnssStatusProvider.Status> satellitesHandler = new Consumer<Status>() {
        @Override
        @SuppressLint("SetTextI18n")
        public void accept(final Status gnssStatus) {
            if (gnssStatus.gnssEnabled) {
                binding.navSatellites.setText(res.getString(R.string.loc_sat) + ": " + gnssStatus.satellitesFixed + '/' + gnssStatus.satellitesVisible);
            } else {
                binding.navSatellites.setText(res.getString(R.string.loc_gps_disabled));
            }
        }
    };

    private static final class FirstLoginHandler extends WeakReferenceHandler<MainActivity> {

        FirstLoginHandler(final MainActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            final MainActivity activity = getReference();
            if (activity != null) {
                try {
                    final StatusCode reason = (StatusCode) msg.obj;

                    if (reason != null && reason != StatusCode.NO_ERROR) { //LoginFailed
                        activity.showToast(activity.res.getString(reason == StatusCode.MAINTENANCE ? reason.getErrorString() : R.string.err_login_failed_toast));
                    }
                } catch (final Exception e) {
                    Log.w("MainActivity.firstLoginHander", e);
                }
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (Settings.isTransparentBackground()) {
            setTheme(R.style.cgeo_main_transparent);
        }
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);

        backupUtils = new BackupUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS));

        //check database
        final String errorMsg = DataStore.initAndCheck(false);
        if (errorMsg != null) {
            DebugUtils.askUserToReportProblem(this, "Fatal DB error: " + errorMsg);
        }

        // Disable the up navigation for this activity
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.cgeo_actionbar_squircle);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (!Settings.isTransparentBackground()) {
            binding.mainscreen.setBackgroundColor(getResources().getColor(Settings.isLightSkin() ? R.color.background_light_notice : R.color.background_dark_notice));
        }

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // If we had been open already, start from the last used activity.
            finish();
            return;
        }

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // type to search

        Log.i("Starting " + getPackageName() + ' ' + Version.getVersionCode(this) + " a.k.a " + Version.getVersionName(this));

        final Activity mainActivity = this;

        PermissionHandler.requestStoragePermission(this, new PermissionGrantedCallback(PermissionRequestContext.MainActivityStorage) {
            @Override
            protected void execute() {
                PermissionHandler.executeIfLocationPermissionGranted(mainActivity, new PermissionGrantedCallback(PermissionRequestContext.MainActivityOnCreate) {
                    // TODO: go directly into execute if the device api level is below 26
                    @Override
                    public void execute() {
                        final Sensors sensors = Sensors.getInstance();
                        sensors.setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode());
                        sensors.setupDirectionObservable();

                        // Attempt to acquire an initial location before any real activity happens.
                        sensors.geoDataObservable(true).subscribeOn(AndroidRxUtils.looperCallbacksScheduler).take(1).subscribe();
                    }
                });
            }
        });

        init();

        checkChangedInstall();

        LocalStorage.initGeocacheDataDir();
        if (LocalStorage.isRunningLowOnDiskSpace()) {
            Dialogs.message(this, res.getString(R.string.init_low_disk_space), res.getString(R.string.init_low_disk_space_message));
        }

        confirmDebug();

        // infobox "not logged in" with link to service config; display delayed by 10 seconds
        final Handler handler = new Handler();
        handler.postDelayed(this::checkLoggedIn, 10000);
        binding.infoNotloggedin.setOnClickListener(v -> Dialogs.confirmYesNo(this, R.string.warn_notloggedin_title, R.string.warn_notloggedin_long, (dialog, which) -> SettingsActivity.openForScreen(R.string.preference_screen_services, this)));

        //do file migrations if necessary
        LocalStorage.migrateLocalStorage(this);

        //sync map Theme folder
        RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder();

        // reactivate dialogs which are set to show later
        OneTimeDialogs.nextStatus();

        checkForRoutingTileUpdates();
        checkForMapUpdates();
    }

    @Override

    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBundle(STATE_BACKUPUTILS, backupUtils.getState());
    }




    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            PermissionHandler.executeCallbacksFor(permissions);
        } else {
            final Activity activity = this;
            final PermissionRequestContext perm = PermissionRequestContext.fromRequestCode(requestCode);
            Dialogs.newBuilder(this)
                    .setMessage(perm.getAskAgainResource())
                    .setCancelable(false)
                    .setPositiveButton(R.string.ask_again, (dialog, which) -> PermissionHandler.askAgainFor(permissions, activity, perm))
                    .setNegativeButton(R.string.close_app, (dialog, which) -> {
                        activity.finish();
                        System.exit(0);
                    })
                    .setIcon(R.drawable.ic_menu_preferences)
                    .create()
                    .show();
        }
    }

    @SuppressWarnings("unused") // in Eclipse, BuildConfig.DEBUG is always true
    private void confirmDebug() {
        if (Settings.isDebug() && !BuildConfig.DEBUG) {
            Dialogs.confirmYesNo(this, R.string.init_confirm_debug, R.string.list_confirm_debug_message, (dialog, whichButton) -> Settings.setDebug(false));
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity mainActivity = this;
        PermissionHandler.requestStoragePermission(this, new PermissionGrantedCallback(PermissionRequestContext.MainActivityStorage) {
            @Override
            protected void execute() {
                PermissionHandler.executeIfLocationPermissionGranted(mainActivity, new PermissionGrantedCallback(PermissionRequestContext.MainActivityOnResume) {

                    @Override
                    public void execute() {
                        resumeDisposables.add(locationUpdater.start(GeoDirHandler.UPDATE_GEODATA | GeoDirHandler.LOW_POWER));
                        resumeDisposables.add(Sensors.getInstance().gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(satellitesHandler));

                    }
                });
            }
        });

        updateUserInfoHandler.sendEmptyMessage(-1);
        startBackgroundLogin();
        init();
        connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void startBackgroundLogin() {
        final boolean mustLogin = ConnectorFactory.mustRelog();

        for (final ILogin conn : ConnectorFactory.getActiveLiveConnectors()) {
            if (mustLogin || !conn.isLoggedIn()) {
                AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
                    if (mustLogin) {
                        // Properly log out from geocaching.com
                        conn.logout();
                    }
                    conn.login(firstLoginHandler, MainActivity.this);
                    updateUserInfoHandler.sendEmptyMessage(-1);
                });
            }
        }
    }

    private void checkForRoutingTileUpdates() {
        if (Settings.useInternalRouting() && Settings.isBrouterAutoTileDownloads() && !PersistableFolder.ROUTING_TILES.isLegacy() && Settings.brouterAutoTileDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, R.string.tileupdate_info, this::returnFromTileUpdateCheck);
        }
    }

    private void returnFromTileUpdateCheck(final boolean updateCheckAllowed) {
        if (updateCheckAllowed) {
            Settings.setBrouterAutoTileDownloadsLastCheck();
        }
    }

    private void checkForMapUpdates() {
        if (Settings.isMapAutoDownloads() && Settings.mapAutoDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED, R.string.updates_check, R.string.mapupdate_info, this::returnFromMapUpdateCheck);
        }
    }

    private void returnFromMapUpdateCheck(final boolean updateCheckAllowed) {
        if (updateCheckAllowed) {
            Settings.setMapAutoDownloadsLastCheck();
        }
    }

    @Override
    public void onDestroy() {
        initialized = false;

        super.onDestroy();
    }

    @Override
    public void onStop() {
        initialized = false;
        super.onStop();
    }

    @Override
    public void onPause() {
        initialized = false;
        resumeDisposables.clear();
        unregisterReceiver(connectivityChangeReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_options, menu);
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchItem = menu.findItem(R.id.menu_gosearch);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        hideKeyboardOnSearchClick(searchItem);

        // hide other action icons when search is active
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(final MenuItem item) {
                for (int i = 0; i < menu.size(); i++) {
                    if (menu.getItem(i).getItemId() != R.id.menu_gosearch) {
                        menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(final MenuItem item) {
                invalidateOptionsMenu();
                return true;
            }
        });

        return true;
    }

    private void hideKeyboardOnSearchClick(final MenuItem searchItem) {
        searchView.setOnSuggestionListener(new OnSuggestionListener() {

            @Override
            public boolean onSuggestionSelect(final int arg0) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(final int arg0) {
                searchItem.collapseActionView();
                searchView.setIconified(true);
                // return false to invoke standard behavior of launching the intent for the search result
                return false;
            }
        });

        // Used to collapse searchBar on submit from virtual keyboard
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String s) {
                searchItem.collapseActionView();
                searchView.setIconified(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String s) {
                return false;
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_wizard).setVisible(!InstallWizardActivity.isConfigurationOk(this));
        menu.findItem(R.id.menu_pocket_queries).setVisible(Settings.isGCConnectorActive() && Settings.isGCPremiumMember());
        menu.findItem(R.id.menu_update_routingdata).setEnabled(Settings.useInternalRouting());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home || id == R.id.menu_about) {
            showAbout(null);
        } else if (id == R.id.menu_report_problem) {
            DebugUtils.askUserToReportProblem(this, null);
        } else if (id == R.id.menu_helpers) {
            startActivity(new Intent(this, UsefulAppsActivity.class));
        } else if (id == R.id.menu_wizard) {
            final Intent wizard = new Intent(this, InstallWizardActivity.class);
            wizard.putExtra(InstallWizardActivity.BUNDLE_MODE, InstallWizardActivity.needsFolderMigration() ? InstallWizardActivity.WizardMode.WIZARDMODE_MIGRATION.id : InstallWizardActivity.WizardMode.WIZARDMODE_RETURNING.id);
            startActivity(wizard);
        } else if (id == R.id.menu_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), Intents.SETTINGS_ACTIVITY_REQUEST_CODE);
        } else if (id == R.id.menu_backup) {
            SettingsActivity.openForScreen(R.string.preference_screen_backup, this);
        } else if (id == R.id.menu_history) {
            startActivity(CacheListActivity.getHistoryIntent(this));
        } else if (id == R.id.menu_scan) {
            startScannerApplication();
        } else if (id == R.id.menu_pocket_queries) {
            if (Settings.isGCPremiumMember()) {
                startActivity(new Intent(this, PocketQueryListActivity.class));
            }
        } else if (id == R.id.menu_update_routingdata) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, this::returnFromTileUpdateCheck);
        } else if (id == R.id.menu_update_mapdata) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED, R.string.updates_check, this::returnFromMapUpdateCheck);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void startScannerApplication() {
        final IntentIntegrator integrator = new IntentIntegrator(this);
        // integrator dialog is English only, therefore localize it
        integrator.setButtonYesByID(android.R.string.yes);
        integrator.setButtonNoByID(android.R.string.no);
        integrator.setTitleByID(R.string.menu_scan_geo);
        integrator.setMessageByID(R.string.menu_scan_description);
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);  // call super to make lint happy
        if (backupUtils.onActivityResult(requestCode, resultCode, intent)) {
            return;
        }
        if (requestCode == Intents.SETTINGS_ACTIVITY_REQUEST_CODE) {
            if (resultCode == SettingsActivity.RESTART_NEEDED) {
                ProcessPhoenix.triggerRebirth(this);
            }
        } else {
            final IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult != null) {
                final String scan = scanResult.getContents();
                if (StringUtils.isBlank(scan)) {
                    return;
                }
                SearchActivity.startActivityScan(scan, this);
            } else if (requestCode == Intents.SEARCH_REQUEST_CODE) {
                // SearchActivity activity returned without making a search
                if (resultCode == RESULT_CANCELED) {
                    String query = intent.getStringExtra(SearchManager.QUERY);
                    if (query == null) {
                        query = "";
                    }
                    Dialogs.message(this, res.getString(R.string.unknown_scan) + "\n\n" + query);
                }
            }
        }
    }

    private void setFilterTitle() {
        binding.filterButtonTitle.setText(Settings.getCacheType().getL10n());
    }

    private void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        binding.map.setClickable(true);
        binding.map.setOnClickListener(this::cgeoFindOnMap);

        binding.searchOffline.setClickable(true);
        binding.searchOffline.setOnClickListener(this::cgeoFindByOffline);
        binding.searchOffline.setOnLongClickListener(v -> {
            new StoredList.UserInterface(MainActivity.this).promptForListSelection(R.string.list_title, selectedListId -> {
                if (selectedListId == PseudoList.HISTORY_LIST.id) {
                    startActivity(CacheListActivity.getHistoryIntent(this));
                } else {
                    Settings.setLastDisplayedList(selectedListId);
                    CacheListActivity.startActivityOffline(MainActivity.this);
                }
            }, false, PseudoList.NEW_LIST.id);
            return true;
        });
        binding.searchOffline.setLongClickable(true);

        binding.advancedButton.setClickable(true);
        binding.advancedButton.setOnClickListener(this::cgeoSearch);

        binding.anyButton.setClickable(true);
        binding.anyButton.setOnClickListener(this::cgeoPoint);

        binding.filterButton.setClickable(true);
        binding.filterButton.setOnClickListener(v -> selectGlobalTypeFilter());
        binding.filterButton.setOnLongClickListener(v -> {
            Settings.setCacheType(CacheType.ALL);
            setFilterTitle();
            return true;
        });

        updateCacheCounter();

        setFilterTitle();
        checkRestore();
        DataStore.cleanIfNeeded(this);
    }

    protected void selectGlobalTypeFilter() {
        Dialogs.selectGlobalTypeFilter(this, cacheType -> setFilterTitle());
    }

    public void updateCacheCounter() {
        AndroidRxUtils.bindActivity(this, DataStore.getAllCachesCountObservable()).subscribe(countBubbleCnt1 -> {
            if (countBubbleCnt1 == 0) {
                binding.offlineCount.setVisibility(View.GONE);
            } else {
                binding.offlineCount.setText(String.format(Locale.getDefault(), "%d", countBubbleCnt1));
                binding.offlineCount.bringToFront();
                binding.offlineCount.setVisibility(View.VISIBLE);
            }
        }, throwable -> Log.e("Unable to add bubble count", throwable));
    }

    private void checkRestore() {

        if (DataStore.isNewlyCreatedDatebase() && !restoreMessageShown) {

            if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder())) {

                restoreMessageShown = true;
                new AlertDialog.Builder(this)
                        .setTitle(res.getString(R.string.init_backup_restore))
                        .setMessage(res.getString(R.string.init_restore_confirm))
                        .setCancelable(false)
                        .setPositiveButton(getString(android.R.string.yes), (dialog, id) -> {
                            dialog.dismiss();
                            DataStore.resetNewlyCreatedDatabase();
                            backupUtils.restore(BackupUtils.newestBackupFolder());
                        })
                        .setNegativeButton(getString(android.R.string.no), (dialog, id) -> {
                            dialog.cancel();
                            DataStore.resetNewlyCreatedDatabase();
                        })
                        .create()
                        .show();
            }
        }
    }

    private class UpdateLocation extends GeoDirHandler {

        @Override
        @SuppressLint("SetTextI18n")
        public void updateGeoData(final GeoData geo) {
            if (!binding.nearest.isClickable()) {
                binding.nearest.setFocusable(true);
                binding.nearest.setClickable(true);
                binding.nearest.setOnClickListener(MainActivity.this::cgeoFindNearest);
                binding.nearest.setBackgroundResource(R.drawable.main_nearby);
            }

            binding.navType.setText(res.getString(geo.getLocationProvider().resourceId));

            if (geo.getAccuracy() >= 0) {
                final int speed = Math.round(geo.getSpeed()) * 60 * 60 / 1000;
                binding.navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()) + Formatter.SEPARATOR + Units.getSpeed(speed));
            } else {
                binding.navAccuracy.setText(null);
            }

            final Geopoint currentCoords = geo.getCoords();
            if (Settings.isShowAddress()) {
                if (addCoords == null) {
                    binding.navLocation.setText(R.string.loc_no_addr);
                }
                if (addCoords == null || currentCoords.distanceTo(addCoords) > 0.5) {
                    addCoords = currentCoords;
                    final Single<String> address = (new AndroidGeocoder(MainActivity.this).getFromLocation(currentCoords)).map(MainActivity::formatAddress).onErrorResumeWith(Single.just(currentCoords.toString()));
                    AndroidRxUtils.bindActivity(MainActivity.this, address)
                            .subscribeOn(AndroidRxUtils.networkScheduler)
                            .subscribe(address12 -> binding.navLocation.setText(address12));
                }
            } else {
                binding.navLocation.setText(currentCoords.toString());
            }
        }
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindOnMap(final View v) {
        binding.map.setPressed(true);
        startActivity(DefaultMap.getLiveMapIntent(this));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindNearest(final View v) {
        binding.nearest.setPressed(true);
        startActivity(CacheListActivity.getNearestIntent(this));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindByOffline(final View v) {
        binding.searchOffline.setPressed(true);
        CacheListActivity.startActivityOffline(this);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoSearch(final View v) {
        binding.advancedButton.setPressed(true);
        startActivity(new Intent(this, SearchActivity.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoPoint(final View v) {
        binding.anyButton.setPressed(true);
        InternalConnector.assertHistoryCacheExists(this);
        CacheDetailActivity.startActivity(this, InternalConnector.GEOCODE_HISTORY_CACHE, true);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFilter(final View v) {
        binding.filterButton.setPressed(true);
        binding.filterButton.performClick();
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoNavSettings(final View v) {
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void checkChangedInstall() {
        // temporary workaround for #4143
        //TODO: understand and avoid if possible
        try {
            final long lastChecksum = Settings.getLastChangelogChecksum();
            final long checksum = TextUtils.checksum(getString(R.string.changelog_master) + getString(R.string.changelog_release));
            Settings.setLastChangelogChecksum(checksum);

            if (lastChecksum == 0) {
                // initialize oneTimeMessages after fresh install
                OneTimeDialogs.initializeOnFreshInstall();
                // initialize useInternalRouting setting depending on whether BRouter app is installed or not
                Settings.setUseInternalRouting(!ProcessUtils.isInstalled(getString(R.string.package_brouter)));
            } else if (lastChecksum != checksum) {
                // show change log page after update
                AboutActivity.showChangeLog(this);
            }
        } catch (final Exception ex) {
            Log.e("Error checking/showing changelog!", ex);
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void showAbout(final View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    @Override
    public void onBackPressed() {
        // back may exit the app instead of closing the search action bar
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            searchItem.collapseActionView();
        } else {
            super.onBackPressed();
        }
    }
}
