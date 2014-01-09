package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.DatabaseBackupUtils;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RunnableWithArgument;
import cgeo.geocaching.utils.Version;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AbstractActivity {
    @InjectView(R.id.nav_satellites) protected TextView navSatellites;
    @InjectView(R.id.filter_button_title)protected TextView filterTitle;
    @InjectView(R.id.map) protected ImageView findOnMap;
    @InjectView(R.id.search_offline) protected ImageView findByOffline;
    @InjectView(R.id.advanced_button) protected ImageView advanced;
    @InjectView(R.id.any_button) protected ImageView any;
    @InjectView(R.id.filter_button) protected ImageView filter;
    @InjectView(R.id.nearest) protected ImageView nearestView;
    @InjectView(R.id.nav_type) protected TextView navType;
    @InjectView(R.id.nav_accuracy) protected TextView navAccuracy;
    @InjectView(R.id.nav_location) protected TextView navLocation;
    @InjectView(R.id.offline_count) protected TextView countBubble;
    @InjectView(R.id.info_area) protected LinearLayout infoArea;

    public static final int SEARCH_REQUEST_CODE = 2;

    private int version = 0;
    private boolean cleanupRunning = false;
    private int countBubbleCnt = 0;
    private Geopoint addCoords = null;
    private List<Address> addresses = null;
    private boolean addressObtaining = false;
    private boolean initialized = false;

    private final UpdateLocation locationUpdater = new UpdateLocation();

    private Handler updateUserInfoHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {

            // Get active connectors with login status
            ILogin[] loginConns = ConnectorFactory.getActiveLiveConnectors();

            // Update UI
            infoArea.removeAllViews();
            LayoutInflater inflater = getLayoutInflater();

            for (ILogin conn : loginConns) {

                TextView connectorInfo = (TextView) inflater.inflate(R.layout.main_activity_connectorstatus, null);
                infoArea.addView(connectorInfo);

                StringBuilder userInfo = new StringBuilder(conn.getName()).append(Formatter.SEPARATOR);
                if (conn.isLoggedIn()) {
                    userInfo.append(conn.getUserName());
                    if (conn.getCachesFound() >= 0) {
                        userInfo.append(" (").append(String.valueOf(conn.getCachesFound())).append(')');
                    }
                    userInfo.append(Formatter.SEPARATOR);
                }
                userInfo.append(conn.getLoginStatusString());

                connectorInfo.setText(userInfo);
            }
        }
    };

    private Handler obtainAddressHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            try {
                if (CollectionUtils.isNotEmpty(addresses)) {
                    final Address address = addresses.get(0);
                    final ArrayList<String> addressParts = new ArrayList<String>();

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

                    addCoords = app.currentGeo().getCoords();

                    navLocation.setText(StringUtils.join(addressParts, ", "));
                }
            } catch (RuntimeException e) {
                // nothing
            }

            addresses = null;
        }
    };

    private class SatellitesHandler extends GeoDirHandler {

        private boolean gpsEnabled = false;
        private int satellitesFixed = 0;
        private int satellitesVisible = 0;

        @Override
        public void updateGeoData(final IGeoData data) {
            if (data.getGpsEnabled() == gpsEnabled &&
                    data.getSatellitesFixed() == satellitesFixed &&
                    data.getSatellitesVisible() == satellitesVisible) {
                return;
            }
            gpsEnabled = data.getGpsEnabled();
            satellitesFixed = data.getSatellitesFixed();
            satellitesVisible = data.getSatellitesVisible();

            if (gpsEnabled) {
                if (satellitesFixed > 0) {
                    navSatellites.setText(res.getString(R.string.loc_sat) + ": " + satellitesFixed + '/' + satellitesVisible);
                } else if (satellitesVisible >= 0) {
                    navSatellites.setText(res.getString(R.string.loc_sat) + ": 0/" + satellitesVisible);
                }
            } else {
                navSatellites.setText(res.getString(R.string.loc_gps_disabled));
            }
        }

    }

    private SatellitesHandler satellitesHandler = new SatellitesHandler();

    private Handler firstLoginHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            try {
                final StatusCode reason = (StatusCode) msg.obj;

                if (reason != null && reason != StatusCode.NO_ERROR) { //LoginFailed
                    showToast(res.getString(reason == StatusCode.MAINTENANCE ? reason.getErrorString() : R.string.err_login_failed_toast));
                }
            } catch (Exception e) {
                Log.w("MainActivity.firstLoginHander", e);
            }
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.inject(this);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // If we had been open already, start from the last used activity.
            finish();
            return;
        }

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // type to search

        version = Version.getVersionCode(this);
        Log.i("Starting " + getPackageName() + ' ' + version + " a.k.a " + Version.getVersionName(this));

        init();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        locationUpdater.startGeo();
        satellitesHandler.startGeo();
        updateUserInfoHandler.sendEmptyMessage(-1);
        startBackgroundLogin();
        init();
    }

    private void startBackgroundLogin() {
        assert(app != null);

        final boolean mustLogin = app.mustRelog();

        for (final ILogin conn : ConnectorFactory.getActiveLiveConnectors()) {
            if (mustLogin || !conn.isLoggedIn()) {
                new Thread() {
                    @Override
                    public void run() {
                        conn.login(firstLoginHandler, MainActivity.this);
                        updateUserInfoHandler.sendEmptyMessage(-1);
                    }
                }.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        initialized = false;
        app.showLoginToast = true;

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
        locationUpdater.stopGeo();
        satellitesHandler.stopGeo();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_pocket_queries).setVisible(Settings.isPremiumMember());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_about:
                showAbout(null);
                return true;
            case R.id.menu_helpers:
                startActivity(new Intent(this, UsefulAppsActivity.class));
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_history:
                CacheListActivity.startActivityHistory(this);
                return true;
            case R.id.menu_scan:
                startScannerApplication();
                return true;
            case R.id.menu_pocket_queries:
                if (!Settings.isPremiumMember()) {
                    return true;
                }
                new PocketQueryList.UserInterface(MainActivity.this).promptForListSelection(new RunnableWithArgument<PocketQueryList>() {

                    @Override
                    public void run(final PocketQueryList pql) {
                        CacheListActivity.startActivityPocket(MainActivity.this, pql);
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScannerApplication() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        // integrator dialog is English only, therefore localize it
        integrator.setButtonYesByID(android.R.string.yes);
        integrator.setButtonNoByID(android.R.string.no);
        integrator.setTitleByID(R.string.menu_scan_geo);
        integrator.setMessageByID(R.string.menu_scan_description);
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            String scan = scanResult.getContents();
            if (StringUtils.isBlank(scan)) {
                return;
            }
            SearchActivity.startActivityScan(scan, this);
        } else if (requestCode == SEARCH_REQUEST_CODE) {
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

    private void setFilterTitle() {
        filterTitle.setText(Settings.getCacheType().getL10n());
    }

    private void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        Settings.setLanguage(Settings.isUseEnglish());

        findOnMap.setClickable(true);
        findOnMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cgeoFindOnMap(v);
            }
        });

        findByOffline.setClickable(true);
        findByOffline.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cgeoFindByOffline(v);
            }
        });
        findByOffline.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                new StoredList.UserInterface(MainActivity.this).promptForListSelection(R.string.list_title, new RunnableWithArgument<Integer>() {

                    @Override
                    public void run(final Integer selectedListId) {
                        Settings.saveLastList(selectedListId);
                        CacheListActivity.startActivityOffline(MainActivity.this);
                    }
                });
                return true;
            }
        });
        findByOffline.setLongClickable(true);

        advanced.setClickable(true);
        advanced.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cgeoSearch(v);
            }
        });

        any.setClickable(true);
        any.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                cgeoPoint(v);
            }
        });

        filter.setClickable(true);
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                selectGlobalTypeFilter();
            }
        });
        filter.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                selectGlobalTypeFilter();
                return true;
            }
        });

        updateCacheCounter();

        setFilterTitle();
        checkRestore();
        (new CleanDatabaseThread()).start();
    }

    protected void selectGlobalTypeFilter() {
        final List<CacheType> cacheTypes = new ArrayList<CacheType>();

        //first add the most used types
        cacheTypes.add(CacheType.ALL);
        cacheTypes.add(CacheType.TRADITIONAL);
        cacheTypes.add(CacheType.MULTI);
        cacheTypes.add(CacheType.MYSTERY);

        // then add all other cache types sorted alphabetically
        List<CacheType> sorted = new ArrayList<CacheType>();
        sorted.addAll(Arrays.asList(CacheType.values()));
        sorted.removeAll(cacheTypes);

        Collections.sort(sorted, new Comparator<CacheType>() {

            @Override
            public int compare(final CacheType left, final CacheType right) {
                return left.getL10n().compareToIgnoreCase(right.getL10n());
            }
        });

        cacheTypes.addAll(sorted);

        int checkedItem = cacheTypes.indexOf(Settings.getCacheType());
        if (checkedItem < 0) {
            checkedItem = 0;
        }

        String[] items = new String[cacheTypes.size()];
        for (int i = 0; i < cacheTypes.size(); i++) {
            items[i] = cacheTypes.get(i).getL10n();
        }

        Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_filter);
        builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int position) {
                CacheType cacheType = cacheTypes.get(position);
                Settings.setCacheType(cacheType);
                setFilterTitle();
                dialog.dismiss();
            }

        });
        builder.create().show();
    }

    public void updateCacheCounter() {
        (new CountBubbleUpdateThread()).start();
    }

    private void checkRestore() {
        if (!DataStore.isNewlyCreatedDatebase() || null == DatabaseBackupUtils.getRestoreFile()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.init_backup_restore))
                .setMessage(res.getString(R.string.init_restore_confirm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.dismiss();
                        DataStore.resetNewlyCreatedDatabase();
                        DatabaseBackupUtils.restoreDatabase(MainActivity.this);
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        DataStore.resetNewlyCreatedDatabase();
                    }
                })
                .create()
                .show();
    }

    private class UpdateLocation extends GeoDirHandler {

        @Override
        public void updateGeoData(final IGeoData geo) {
            try {
                if (geo.getCoords() != null) {
                    if (!nearestView.isClickable()) {
                        nearestView.setFocusable(true);
                        nearestView.setClickable(true);
                        nearestView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(final View v) {
                                cgeoFindNearest(v);
                            }
                        });
                        nearestView.setBackgroundResource(R.drawable.main_nearby);
                    }

                    navType.setText(res.getString(geo.getLocationProvider().resourceId));

                    if (geo.getAccuracy() >= 0) {
                        int speed = Math.round(geo.getSpeed()) * 60 * 60 / 1000;
                        navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()) + Formatter.SEPARATOR + Units.getSpeed(speed));
                    } else {
                        navAccuracy.setText(null);
                    }

                    if (Settings.isShowAddress()) {
                        if (addCoords == null) {
                            navLocation.setText(res.getString(R.string.loc_no_addr));
                        }
                        if (addCoords == null || (geo.getCoords().distanceTo(addCoords) > 0.5 && !addressObtaining)) {
                            (new ObtainAddressThread()).start();
                        }
                    } else {
                        navLocation.setText(geo.getCoords().toString());
                    }
                } else {
                    if (nearestView.isClickable()) {
                        nearestView.setFocusable(false);
                        nearestView.setClickable(false);
                        nearestView.setOnClickListener(null);
                        nearestView.setBackgroundResource(R.drawable.main_nearby_disabled);
                    }
                    navType.setText(null);
                    navAccuracy.setText(null);
                    navLocation.setText(res.getString(R.string.loc_trying));
                }
            } catch (RuntimeException e) {
                Log.w("Failed to update location.");
            }
        }
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindOnMap(final View v) {
        findOnMap.setPressed(true);
        CGeoMap.startActivityLiveMap(this);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindNearest(final View v) {
        if (app.currentGeo().getCoords() == null) {
            return;
        }

        nearestView.setPressed(true);
        CacheListActivity.startActivityNearest(this, app.currentGeo().getCoords());
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
        startActivity(new Intent(this, NavigateAnyPointActivity.class));
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

    private class CountBubbleUpdateThread extends Thread {
        private Handler countBubbleHandler = new Handler() {

            @Override
            public void handleMessage(final Message msg) {
                try {
                    if (countBubbleCnt == 0) {
                        countBubble.setVisibility(View.GONE);
                    } else {
                        countBubble.setText(Integer.toString(countBubbleCnt));
                        countBubble.bringToFront();
                        countBubble.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.w("MainActivity.countBubbleHander", e);
                }
            }
        };

        @Override
        public void run() {
            if (app == null) {
                return;
            }

            int checks = 0;
            while (!DataStore.isInitialized()) {
                try {
                    sleep(500);
                    checks++;
                } catch (Exception e) {
                    Log.e("MainActivity.CountBubbleUpdateThread.run", e);
                }

                if (checks > 10) {
                    return;
                }
            }

            countBubbleCnt = DataStore.getAllCachesCount();

            countBubbleHandler.sendEmptyMessage(0);
        }
    }

    private class CleanDatabaseThread extends Thread {

        @Override
        public void run() {
            if (app == null) {
                return;
            }
            if (cleanupRunning) {
                return;
            }

            boolean more = false;
            if (version != Settings.getVersion()) {
                Log.i("Initializing hard cleanup - version changed from " + Settings.getVersion() + " to " + version + ".");

                more = true;
            }

            cleanupRunning = true;
            DataStore.clean(more);
            cleanupRunning = false;

            if (version > 0) {
                Settings.setVersion(version);
            }
        }
    }

    private class ObtainAddressThread extends Thread {

        public ObtainAddressThread() {
            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            if (addressObtaining) {
                return;
            }
            addressObtaining = true;

            try {
                final Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                final Geopoint coords = app.currentGeo().getCoords();
                addresses = geocoder.getFromLocation(coords.getLatitude(), coords.getLongitude(), 1);
            } catch (final IOException e) {
                Log.i("Failed to obtain address");
            } catch (final IllegalArgumentException e) {
                Log.w("ObtainAddressThread.run", e);
            }

            obtainAddressHandler.sendEmptyMessage(0);

            addressObtaining = false;
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void showAbout(final View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goSearch(final View view) {
        onSearchRequested();
    }

}
