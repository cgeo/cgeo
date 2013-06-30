package cgeo.geocaching;

import butterknife.InjectView;
import butterknife.Views;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.settings.NewSettingsActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RunnableWithArgument;
import cgeo.geocaching.utils.Version;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
    @InjectView(R.id.nav_type) protected TextView navType ;
    @InjectView(R.id.nav_accuracy) protected TextView navAccuracy ;
    @InjectView(R.id.nav_location) protected TextView navLocation ;
    @InjectView(R.id.offline_count) protected TextView countBubble ;
    @InjectView(R.id.info_area) protected LinearLayout infoArea;

    private static final String SCAN_INTENT = "com.google.zxing.client.android.SCAN";
    public static final int SEARCH_REQUEST_CODE = 2;

    private int version = 0;
    private boolean cleanupRunning = false;
    private int countBubbleCnt = 0;
    private Geopoint addCoords = null;
    private List<Address> addresses = null;
    private boolean addressObtaining = false;
    private boolean initialized = false;

    final private UpdateLocation locationUpdater = new UpdateLocation();

    private Handler updateUserInfoHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

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
        public void handleMessage(Message msg) {
            try {
                if (CollectionUtils.isNotEmpty(addresses)) {
                    final Address address = addresses.get(0);
                    final StringBuilder addText = new StringBuilder();

                    if (address.getCountryName() != null) {
                        addText.append(address.getCountryName());
                    }
                    if (address.getLocality() != null) {
                        if (addText.length() > 0) {
                            addText.append(", ");
                        }
                        addText.append(address.getLocality());
                    } else if (address.getAdminArea() != null) {
                        if (addText.length() > 0) {
                            addText.append(", ");
                        }
                        addText.append(address.getAdminArea());
                    }

                    addCoords = app.currentGeo().getCoords();

                    navLocation.setText(addText.toString());
                }
            } catch (Exception e) {
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
        public void handleMessage(Message msg) {
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
    public void onCreate(Bundle savedInstanceState) {
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Views.inject(this);

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        locationUpdater.startGeo();
        satellitesHandler.startGeo();
        updateUserInfoHandler.sendEmptyMessage(-1);
        init();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.menu_scan);
        if (item != null) {
            item.setEnabled(isIntentAvailable(SCAN_INTENT));
        }
        return true;
    }

    public static boolean isIntentAvailable(String intent) {
        final PackageManager packageManager = cgeoapplication.getInstance().getPackageManager();
        final List<ResolveInfo> list = packageManager.queryIntentActivities(
                new Intent(intent), PackageManager.MATCH_DEFAULT_ONLY);

        return CollectionUtils.isNotEmpty(list);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_about:
                showAbout(null);
                return true;
            case R.id.menu_helpers:
                startActivity(new Intent(this, UsefulAppsActivity.class));
                return true;
            case R.id.menu_oldsettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, NewSettingsActivity.class));
                return true;
            case R.id.menu_history:
                cgeocaches.startActivityHistory(this);
                return true;
            case R.id.menu_scan:
                startScannerApplication();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void startScannerApplication() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
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
                new AlertDialog.Builder(this)
                        .setMessage(res.getString(R.string.unknown_scan) + "\n\n" + query)
                .setPositiveButton(getString(android.R.string.ok), null)
                .create()
                .show();
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
        Settings.getGcLogin();

        if (app.firstRun) {
            (new FirstLoginThread()).start();
        }

        findOnMap.setClickable(true);
        findOnMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cgeoFindOnMap(v);
            }
        });

        findByOffline.setClickable(true);
        findByOffline.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cgeoFindByOffline(v);
            }
        });
        findByOffline.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                new StoredList.UserInterface(MainActivity.this).promptForListSelection(R.string.list_title, new RunnableWithArgument<Integer>() {

                    @Override
                    public void run(Integer selectedListId) {
                        Settings.saveLastList(selectedListId);
                        cgeocaches.startActivityOffline(MainActivity.this);
                    }
                });
                return true;
            }
        });
        findByOffline.setLongClickable(true);

        advanced.setClickable(true);
        advanced.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cgeoSearch(v);
            }
        });

        any.setClickable(true);
        any.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cgeoPoint(v);
            }
        });

        filter.setClickable(true);
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectGlobalTypeFilter();
            }
        });
        filter.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
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
            public int compare(CacheType left, CacheType right) {
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
            public void onClick(DialogInterface dialog, int position) {
                CacheType cacheType = cacheTypes.get(position);
                Settings.setCacheType(cacheType);
                setFilterTitle();
                dialog.dismiss();
            }

        });
        builder.create().show();
    }

    void updateCacheCounter() {
        (new CountBubbleUpdateThread()).start();
    }

    private void checkRestore() {
        if (!cgData.isNewlyCreatedDatebase() || null == cgData.getRestoreFile()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.init_backup_restore))
                .setMessage(res.getString(R.string.init_restore_confirm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        cgData.resetNewlyCreatedDatabase();
                        app.restoreDatabase(MainActivity.this);
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        cgData.resetNewlyCreatedDatabase();
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
                            public void onClick(View v) {
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
                        if (geo.getAltitude() != 0.0) {
                            final String humanAlt = Units.getDistanceFromKilometers((float) geo.getAltitude() / 1000);
                            navLocation.setText(geo.getCoords() + " | " + humanAlt);
                        } else {
                            navLocation.setText(geo.getCoords().toString());
                        }
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
            } catch (Exception e) {
                Log.w("Failed to update location.");
            }
        }
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindOnMap(View v) {
        findOnMap.setPressed(true);
        CGeoMap.startActivityLiveMap(this);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindNearest(View v) {
        if (app.currentGeo().getCoords() == null) {
            return;
        }

        nearestView.setPressed(true);
        cgeocaches.startActivityNearest(this, app.currentGeo().getCoords());
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindByOffline(View v) {
        findByOffline.setPressed(true);
        cgeocaches.startActivityOffline(this);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoSearch(View v) {
        advanced.setPressed(true);
        startActivity(new Intent(this, SearchActivity.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoPoint(View v) {
        any.setPressed(true);
        startActivity(new Intent(this, NavigateAnyPointActivity.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFilter(View v) {
        filter.setPressed(true);
        filter.performClick();
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoNavSettings(View v) {
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private class CountBubbleUpdateThread extends Thread {
        private Handler countBubbleHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
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
            while (!cgData.isInitialized()) {
                try {
                    wait(500);
                    checks++;
                } catch (Exception e) {
                    // nothing;
                }

                if (checks > 10) {
                    return;
                }
            }

            countBubbleCnt = cgData.getAllCachesCount();

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
            cgData.clean(more);
            cleanupRunning = false;

            if (version > 0) {
                Settings.setVersion(version);
            }
        }
    }

    private class FirstLoginThread extends Thread {

        @Override
        public void run() {
            if (app == null) {
                return;
            }

            ILogin[] conns = ConnectorFactory.getActiveLiveConnectors();

            for (ILogin conn : conns) {
                conn.login(firstLoginHandler, MainActivity.this);
                updateUserInfoHandler.sendEmptyMessage(-1);
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
            } catch (Exception e) {
                Log.i("Failed to obtain address");
            }

            obtainAddressHandler.sendEmptyMessage(0);

            addressObtaining = false;
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void showAbout(View view) {
        AboutActivity_.intent(this).start();
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goSearch(View view) {
        onSearchRequested();
    }

}
