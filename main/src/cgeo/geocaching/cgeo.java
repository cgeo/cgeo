package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.network.StatusUpdater.Status;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.IObserver;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RunnableWithArgument;
import cgeo.geocaching.utils.Version;

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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class cgeo extends AbstractActivity {

    private static final String SCAN_INTENT = "com.google.zxing.client.android.SCAN";
    private static final int SCAN_REQUEST_CODE = 1;
    public static final int SEARCH_REQUEST_CODE = 2;

    private int version = 0;
    private TextView filterTitle = null;
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

            TextView userInfoView = (TextView) findViewById(R.id.user_info);

            StringBuilder userInfo = new StringBuilder("geocaching.com").append(Formatter.SEPARATOR);
            if (Login.isActualLoginStatus()) {
                userInfo.append(Login.getActualUserName());
                if (Login.getActualCachesFound() >= 0) {
                    userInfo.append(" (").append(String.valueOf(Login.getActualCachesFound())).append(')');
                }
                userInfo.append(Formatter.SEPARATOR);
            }
            userInfo.append(Login.getActualStatus());

            userInfoView.setText(userInfo.toString());
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

                    TextView navLocation = (TextView) findViewById(R.id.nav_location);
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

            final TextView navSatellites = (TextView) findViewById(R.id.nav_satellites);
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
                Log.w("cgeo.firstLoginHander: " + e.toString());
            }
        }
    };

    private class StatusHandler extends Handler implements IObserver<Status> {

        @Override
        public void update(final Status data) {
            obtainMessage(0, data).sendToTarget();
        }

        @Override
        public void handleMessage(final Message msg) {
            final Status data = (Status) msg.obj;
            updateDisplay(data != null && data.message != null ? data : null);
        }

        private void updateDisplay(final Status data) {
            final ViewGroup status = (ViewGroup) findViewById(R.id.status);
            final ImageView statusIcon = (ImageView) findViewById(R.id.status_icon);
            final TextView statusMessage = (TextView) findViewById(R.id.status_message);

            if (data == null) {
                status.setVisibility(View.GONE);
                return;
            }

            if (data.icon != null) {
                final int iconId = res.getIdentifier(data.icon, "drawable", getPackageName());
                if (iconId != 0) {
                    statusIcon.setImageResource(iconId);
                    statusIcon.setVisibility(View.VISIBLE);
                } else {
                    Log.e("StatusHandler: could not find icon corresponding to @drawable/" + data.icon);
                    statusIcon.setVisibility(View.GONE);
                }
            } else {
                statusIcon.setVisibility(View.GONE);
            }

            String message = data.message;
            if (data.messageId != null) {
                final int messageId = res.getIdentifier(data.messageId, "string", getPackageName());
                if (messageId != 0) {
                    message = res.getString(messageId);
                }
            }

            statusMessage.setText(message);
            status.setVisibility(View.VISIBLE);

            if (data.url != null) {
                status.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(data.url)));
                    }
                });
            } else {
                status.setClickable(false);
            }
        }

    }

    private StatusHandler statusHandler = new StatusHandler();

    public cgeo() {
        super("c:geo-main-screen");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // type to search

        version = Version.getVersionCode(this);
        Log.i("Starting " + getPackageName() + ' ' + version + " a.k.a " + Version.getVersionName(this));

        try {
            if (!Settings.isHelpShown()) {
                final RelativeLayout helper = (RelativeLayout) findViewById(R.id.helper);
                if (helper != null) {
                    helper.setVisibility(View.VISIBLE);
                    helper.setClickable(true);
                    helper.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            ActivityMixin.goManual(cgeo.this, "c:geo-intro");
                            view.setVisibility(View.GONE);
                        }
                    });
                    Settings.setHelpShown();
                }
            }
        } catch (Exception e) {
            // nothing
        }

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
        app.getStatusUpdater().addObserver(statusHandler);
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
        app.getStatusUpdater().deleteObserver(statusHandler);
        locationUpdater.stopGeo();
        satellitesHandler.stopGeo();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options, menu);
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
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
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
        Intent intent = new Intent(SCAN_INTENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); // when resuming our app, cancel this activity
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, SCAN_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SCAN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String scan = intent.getStringExtra("SCAN_RESULT");
                if (StringUtils.isBlank(scan)) {
                    return;
                }

                SearchActivity.startActivityScan(scan, this);
            } else if (resultCode == RESULT_CANCELED) {
                // do nothing
            }
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
        if (filterTitle == null) {
            filterTitle = (TextView) findViewById(R.id.filter_button_title);
        }
        filterTitle.setText(Settings.getCacheType().getL10n());
    }

    private void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        Settings.setLanguage(Settings.isUseEnglish());
        Settings.getLogin();

        if (app.firstRun) {
            (new firstLogin()).start();
        }

        final View findOnMap = findViewById(R.id.map);
        findOnMap.setClickable(true);
        findOnMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cgeoFindOnMap(v);
            }
        });

        final View findByOffline = findViewById(R.id.search_offline);
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
                new StoredList.UserInterface(cgeo.this).promptForListSelection(R.string.list_title, new RunnableWithArgument<Integer>() {

                    @Override
                    public void run(Integer selectedListId) {
                        Settings.saveLastList(selectedListId);
                        cgeocaches.startActivityOffline(cgeo.this);
                    }
                });
                return true;
            }
        });
        findByOffline.setLongClickable(true);

        final View advanced = findViewById(R.id.advanced_button);
        advanced.setClickable(true);
        advanced.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cgeoSearch(v);
            }
        });

        final View any = findViewById(R.id.any_button);
        any.setClickable(true);
        any.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cgeoPoint(v);
            }
        });

        final View filter = findViewById(R.id.filter_button);
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
        (new cleanDatabase()).start();
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
                        app.restoreDatabase(cgeo.this);
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
            final View nearestView = findViewById(R.id.nearest);
            final TextView navType = (TextView) findViewById(R.id.nav_type);
            final TextView navAccuracy = (TextView) findViewById(R.id.nav_accuracy);
            final TextView navLocation = (TextView) findViewById(R.id.nav_location);
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
        findViewById(R.id.map).setPressed(true);
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

        findViewById(R.id.nearest).setPressed(true);
        cgeocaches.startActivityNearest(this, app.currentGeo().getCoords());
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindByOffline(View v) {
        findViewById(R.id.search_offline).setPressed(true);
        cgeocaches.startActivityOffline(this);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoSearch(View v) {
        findViewById(R.id.advanced_button).setPressed(true);
        startActivity(new Intent(this, SearchActivity.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoPoint(View v) {
        findViewById(R.id.any_button).setPressed(true);
        startActivity(new Intent(this, cgeopoint.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFilter(View v) {
        findViewById(R.id.filter_button).setPressed(true);
        findViewById(R.id.filter_button).performClick();
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
            private TextView countBubble = null;

            @Override
            public void handleMessage(Message msg) {
                try {
                    if (countBubble == null) {
                        countBubble = (TextView) findViewById(R.id.offline_count);
                    }

                    if (countBubbleCnt == 0) {
                        countBubble.setVisibility(View.GONE);
                    } else {
                        countBubble.setText(Integer.toString(countBubbleCnt));
                        countBubble.bringToFront();
                        countBubble.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.w("cgeo.countBubbleHander: " + e.toString());
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

    private class cleanDatabase extends Thread {

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

    private class firstLogin extends Thread {

        @Override
        public void run() {
            if (app == null) {
                return;
            }

            // login
            final StatusCode status = Login.login();

            if (status == StatusCode.NO_ERROR) {
                app.firstRun = false;
                Login.detectGcCustomDate();
                updateUserInfoHandler.sendEmptyMessage(-1);
            }

            if (app.showLoginToast) {
                firstLoginHandler.sendMessage(firstLoginHandler.obtainMessage(0, status));
                app.showLoginToast = false;

                // invoke settings activity to insert login details
                if (status == StatusCode.NO_LOGIN_INFO_STORED) {
                    SettingsActivity.startActivity(cgeo.this);
                }
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
                final Geocoder geocoder = new Geocoder(cgeo.this, Locale.getDefault());
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
        startActivity(new Intent(this, AboutActivity.class));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goSearch(View view) {
        onSearchRequested();
    }

}
