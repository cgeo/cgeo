package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.address.AndroidGeocoder;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.gc.PocketQueryListActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.helper.UsefulAppsActivity;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.DefaultMap;
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
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.SearchView.OnSuggestionListener;
import androidx.core.view.MenuItemCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jakewharton.processphoenix.ProcessPhoenix;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;
import org.apache.commons.lang3.StringUtils;

public class MainActivity extends AbstractActionBarActivity {
    @BindView(R.id.nav_satellites) protected TextView navSatellites;
    @BindView(R.id.filter_button_title) protected TextView filterTitle;
    @BindView(R.id.map) protected ImageView findOnMap;
    @BindView(R.id.search_offline) protected ImageView findByOffline;
    @BindView(R.id.advanced_button) protected ImageView advanced;
    @BindView(R.id.any_button) protected ImageView any;
    @BindView(R.id.filter_button) protected ImageView filter;
    @BindView(R.id.nearest) protected ImageView nearestView;
    @BindView(R.id.nav_type) protected TextView navType;
    @BindView(R.id.nav_accuracy) protected TextView navAccuracy;
    @BindView(R.id.nav_location) protected TextView navLocation;
    @BindView(R.id.offline_count) protected TextView countBubble;
    @BindView(R.id.info_area) protected ListView infoArea;
    @BindView(R.id.info_notloggedin) protected View notLoggedIn;

    private static final int REQUEST_CODE_GRANT_ACCESS_CGEOBASEDIR = 9999;

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
                activity.infoArea.setAdapter(new ArrayAdapter<ILogin>(activity, R.layout.main_activity_connectorstatus, loginConns) {
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
                            Dialogs.basicOneTimeMessage((Activity) getContext(), OneTimeDialogs.DialogType.EXPLAIN_OFFLINE_FOUND_COUNTER, OneTimeDialogs.DialogStatus.DIALOG_SHOW);
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
                notLoggedIn.setVisibility(View.INVISIBLE);
                return;
            }
        }
        notLoggedIn.setVisibility(View.VISIBLE);
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
                navSatellites.setText(res.getString(R.string.loc_sat) + ": " + gnssStatus.satellitesFixed + '/' + gnssStatus.satellitesVisible);
            } else {
                navSatellites.setText(res.getString(R.string.loc_gps_disabled));
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
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);

        // Disable the up navigation for this activity
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);

            // show c:geo logo
            actionBar.setLogo(R.drawable.cgeo_actionbar_squircle);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_transparent_example));
        }

        setContentView(R.layout.main_activity);
        if (!Settings.isTransparentBackground()) {
            final View mainscreen = findViewById(R.id.mainscreen);
            mainscreen.setBackgroundColor(getResources().getColor(Settings.isLightSkin() ? R.color.background_light_notice : R.color.background_dark_notice));
        }
        ButterKnife.bind(this);

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
        notLoggedIn.setOnClickListener(v -> Dialogs.confirmYesNo(this, R.string.warn_notloggedin_title, R.string.warn_notloggedin_long, (dialog, which) -> SettingsActivity.openForScreen(R.string.preference_screen_services, this)));

        // reactivate dialogs which are set to show later
        OneTimeDialogs.nextStatus();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            PermissionHandler.executeCallbacksFor(permissions);
        } else {
            final Activity activity = this;
            final PermissionRequestContext perm = PermissionRequestContext.fromRequestCode(requestCode);
            new AlertDialog.Builder(this)
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
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        hideKeyboardOnSearchClick(searchItem);
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
                MenuItemCompat.collapseActionView(searchItem);
                searchView.setIconified(true);
                // return false to invoke standard behavior of launching the intent for the search result
                return false;
            }
        });

        // Used to collapse searchBar on submit from virtual keyboard
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String s) {
                MenuItemCompat.collapseActionView(searchItem);
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
        menu.findItem(R.id.menu_pocket_queries).setVisible(Settings.isGCConnectorActive() && Settings.isGCPremiumMember());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                // this activity must handle the home navigation different than all others
                showAbout(null);
                return true;
            case R.id.menu_about:
                showAbout(null);
                return true;
            case R.id.menu_helpers:
                startActivity(new Intent(this, UsefulAppsActivity.class));
                return true;
            case R.id.menu_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), Intents.SETTINGS_ACTIVITY_REQUEST_CODE);
                return true;
            case R.id.menu_backup:
                SettingsActivity.openForScreen(R.string.preference_screen_backup, this);
                return true;
            case R.id.menu_history:
                startActivity(CacheListActivity.getHistoryIntent(this));
                return true;
            case R.id.menu_scan:
                startScannerApplication();
                return true;
            case R.id.menu_pocket_queries:
                if (!Settings.isGCPremiumMember()) {
                    return true;
                }
                startActivity(new Intent(this, PocketQueryListActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        if (requestCode == Intents.SETTINGS_ACTIVITY_REQUEST_CODE) {
            if (resultCode == SettingsActivity.RESTART_NEEDED) {
                ProcessPhoenix.triggerRebirth(this);
            }
        } else if (requestCode == REQUEST_CODE_GRANT_ACCESS_CGEOBASEDIR) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                final Uri uri = intent.getData();
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    Log.e("permissions: " + uri.getPath());
                    Settings.setBaseDir(uri);
                }
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
        filterTitle.setText(Settings.getCacheType().getL10n());
    }

    private void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        findOnMap.setClickable(true);
        findOnMap.setOnClickListener(this::cgeoFindOnMap);

        findByOffline.setClickable(true);
        findByOffline.setOnClickListener(this::cgeoFindByOffline);
        findByOffline.setOnLongClickListener(v -> {
            new StoredList.UserInterface(MainActivity.this).promptForListSelection(R.string.list_title, selectedListId -> {
                Settings.setLastDisplayedList(selectedListId);
                CacheListActivity.startActivityOffline(MainActivity.this);
            }, false, PseudoList.HISTORY_LIST.id);
            return true;
        });
        findByOffline.setLongClickable(true);

        advanced.setClickable(true);
        advanced.setOnClickListener(this::cgeoSearch);

        any.setClickable(true);
        any.setOnClickListener(this::cgeoPoint);

        filter.setClickable(true);
        filter.setOnClickListener(v -> selectGlobalTypeFilter());
        filter.setOnLongClickListener(v -> {
            Settings.setCacheType(CacheType.ALL);
            setFilterTitle();
            return true;
        });

        updateCacheCounter();

        setFilterTitle();
        checkRestore();
        DataStore.cleanIfNeeded(this);

        /* check basic dir access if running on Android 10+ */

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && Settings.getBaseDir() == null) {
            // create (new) public base dir for c:geo
            final File cgeo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "cgeo");
            FileUtils.mkdirs(cgeo);
            Log.d("cgeo public base dir: " + cgeo.getPath() + ", exists=" + cgeo.exists() + ", canWrite=" + cgeo.canWrite());
            // request access to this dir
            Dialogs.message(this, "c:geo base directory", "Due to new Android system requirements c:geo needs to request directory access permissions for its base directory. Please select 'Download', then 'cgeo' and confirm your selection", getString(android.R.string.ok), (dialog, which) -> {
                final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                Log.e("uri dir: " + cgeo.getPath());
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(cgeo));
                startActivityForResult(intent, REQUEST_CODE_GRANT_ACCESS_CGEOBASEDIR);
                dialog.dismiss();
            });
        }
    }

    protected void selectGlobalTypeFilter() {
        Dialogs.selectGlobalTypeFilter(this, cacheType -> setFilterTitle());
    }

    public void updateCacheCounter() {
        AndroidRxUtils.bindActivity(this, DataStore.getAllCachesCountObservable()).subscribe(countBubbleCnt1 -> {
            if (countBubbleCnt1 == 0) {
                countBubble.setVisibility(View.GONE);
            } else {
                countBubble.setText(String.format(Locale.getDefault(), "%d", countBubbleCnt1));
                countBubble.bringToFront();
                countBubble.setVisibility(View.VISIBLE);
            }
        }, throwable -> Log.e("Unable to add bubble count", throwable));
    }

    private void checkRestore() {
        final BackupUtils backupUtils = new BackupUtils(MainActivity.this);

        if (DataStore.isNewlyCreatedDatebase() && !restoreMessageShown) {

            backupUtils.moveBackupIntoNewFolderStructureIfNeeded();
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
            if (!nearestView.isClickable()) {
                nearestView.setFocusable(true);
                nearestView.setClickable(true);
                nearestView.setOnClickListener(MainActivity.this::cgeoFindNearest);
                nearestView.setBackgroundResource(R.drawable.main_nearby);
            }

            navType.setText(res.getString(geo.getLocationProvider().resourceId));

            if (geo.getAccuracy() >= 0) {
                final int speed = Math.round(geo.getSpeed()) * 60 * 60 / 1000;
                navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()) + Formatter.SEPARATOR + Units.getSpeed(speed));
            } else {
                navAccuracy.setText(null);
            }

            final Geopoint currentCoords = geo.getCoords();
            if (Settings.isShowAddress()) {
                if (addCoords == null) {
                    navLocation.setText(R.string.loc_no_addr);
                }
                if (addCoords == null || currentCoords.distanceTo(addCoords) > 0.5) {
                    addCoords = currentCoords;
                    final Single<String> address = (new AndroidGeocoder(MainActivity.this).getFromLocation(currentCoords)).map(MainActivity::formatAddress).onErrorResumeWith(Single.just(currentCoords.toString()));
                    AndroidRxUtils.bindActivity(MainActivity.this, address)
                            .subscribeOn(AndroidRxUtils.networkScheduler)
                            .subscribe(address12 -> navLocation.setText(address12));
                }
            } else {
                navLocation.setText(currentCoords.toString());
            }
        }
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindOnMap(final View v) {
        findOnMap.setPressed(true);
        startActivity(DefaultMap.getLiveMapIntent(this));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindNearest(final View v) {
        nearestView.setPressed(true);
        startActivity(CacheListActivity.getNearestIntent(this));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindByOffline(final View v) {
        findByOffline.setPressed(true);
        CacheListActivity.startActivityOffline(this);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoSearch(final View v) {
        advanced.setPressed(true);
        startActivity(new Intent(this, SearchActivity.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoPoint(final View v) {
        any.setPressed(true);
        InternalConnector.assertHistoryCacheExists(this);
        CacheDetailActivity.startActivity(this, InternalConnector.GEOCODE_HISTORY_CACHE, true);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFilter(final View v) {
        filter.setPressed(true);
        filter.performClick();
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
                // show starting page after install
                AboutActivity.showStarting(this);
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
            MenuItemCompat.collapseActionView(searchItem);
        } else {
            super.onBackPressed();
        }
    }
}
