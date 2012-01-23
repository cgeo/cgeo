package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.CGeoMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class cgeo extends AbstractActivity {

    private static final String SCAN_INTENT = "com.google.zxing.client.android.SCAN";
    private static final int MENU_ABOUT = 0;
    private static final int MENU_HELPERS = 1;
    private static final int MENU_SETTINGS = 2;
    private static final int MENU_HISTORY = 3;
    private static final int MENU_SCAN = 4;
    private static final int SCAN_REQUEST_CODE = 1;
    private static final int MENU_OPEN_LIST = 100;

    private int version = 0;
    private cgGeo geo = null;
    private UpdateLocationCallback geoUpdate = null;
    private TextView filterTitle = null;
    private boolean cleanupRunning = false;
    private int countBubbleCnt = 0;
    private Geopoint addCoords = null;
    private List<Address> addresses = null;
    private boolean addressObtaining = false;
    private boolean initialized = false;
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

                    if (geo != null) {
                        addCoords = geo.coordsNow;
                    }

                    TextView navLocation = (TextView) findViewById(R.id.nav_location);
                    navLocation.setText(addText.toString());
                }
            } catch (Exception e) {
                // nothing
            }

            addresses = null;
        }
    };

    private Handler firstLoginHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                final StatusCode reason = (StatusCode) msg.obj;

                if (reason != null && reason != StatusCode.NO_ERROR) { //LoginFailed
                    showToast(res.getString(reason == StatusCode.MAINTENANCE ? reason.getErrorString() : R.string.err_login_failed_toast));
                }
            } catch (Exception e) {
                Log.w(Settings.tag, "cgeo.fisrtLoginHander: " + e.toString());
            }
        }
    };

    public cgeo() {
        super("c:geo-main-screen");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app.setAction(null);

        app.cleanGeo();
        app.cleanDir();

        setContentView(R.layout.main);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // type to search

        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);

            version = info.versionCode;

            Log.i(Settings.tag, "Starting " + info.packageName + " " + info.versionCode + " a.k.a " + info.versionName + "...");

            info = null;
            manager = null;
        } catch (Exception e) {
            Log.i(Settings.tag, "No info.");
        }

        try {
            if (!Settings.isHelpShown()) {
                RelativeLayout helper = (RelativeLayout) findViewById(R.id.helper);
                if (helper != null) {
                    helper.setVisibility(View.VISIBLE);
                    helper.setClickable(true);
                    helper.setOnClickListener(new View.OnClickListener() {

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

        init();
    }

    @Override
    public void onDestroy() {
        initialized = false;
        app.showLoginToast = true;

        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onDestroy();
    }

    @Override
    public void onStop() {
        initialized = false;

        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        initialized = false;

        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SETTINGS, 0, res.getString(R.string.menu_settings)).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_HISTORY, 0, res.getString(R.string.menu_history)).setIcon(android.R.drawable.ic_menu_recent_history);
        menu.add(0, MENU_HELPERS, 0, res.getString(R.string.menu_helpers)).setIcon(R.drawable.ic_menu_shopping);
        menu.add(0, MENU_SCAN, 0, res.getString(R.string.menu_scan_geo)).setIcon(R.drawable.ic_menu_barcode);
        menu.add(0, MENU_ABOUT, 0, res.getString(R.string.menu_about)).setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(MENU_SCAN);
        if (item != null) {
            item.setEnabled(isIntentAvailable(this, SCAN_INTENT));
        }
        return true;
    }

    private static boolean isIntentAvailable(Context context, String intent) {
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> list = packageManager.queryIntentActivities(
                new Intent(intent), PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case MENU_ABOUT:
                showAbout(null);
                return true;
            case MENU_HELPERS:
                startActivity(new Intent(this, UsefulAppsActivity.class));
                return true;
            case MENU_SETTINGS:
                startActivity(new Intent(this, cgeoinit.class));
                return true;
            case MENU_HISTORY:
                cgeocaches.startActivityHistory(this);
                return true;
            case MENU_SCAN:
                Intent intent = new Intent(SCAN_INTENT);
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, SCAN_REQUEST_CODE);
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SCAN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String scan = intent.getStringExtra("SCAN_RESULT");
                if (StringUtils.isBlank(scan)) {
                    return;
                }
                String host = "http://coord.info/";
                if (scan.toLowerCase().startsWith(host)) {
                    String geocode = scan.substring(host.length()).trim();
                    CacheDetailActivity.startActivity(this, geocode);
                }
                else {
                    showToast(res.getString(R.string.unknown_scan));
                }
            } else if (resultCode == RESULT_CANCELED) {
                // do nothing
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // context menu for offline button
        if (v.getId() == R.id.search_offline) {
            List<StoredList> cacheLists = app.getLists();
            int listCount = cacheLists.size();
            menu.setHeaderTitle(res.getString(R.string.list_title));
            for (int i = 0; i < listCount; i++) {
                StoredList list = cacheLists.get(i);
                menu.add(Menu.NONE, MENU_OPEN_LIST + list.id, Menu.NONE, list.getTitleAndCount());
            }
            return;
        }

        // standard context menu
        menu.setHeaderTitle(res.getString(R.string.menu_filter));

        //first add the most used types
        menu.add(1, 0, 0, CacheType.ALL.getL10n());
        menu.add(1, 1, 0, CacheType.TRADITIONAL.getL10n());
        menu.add(1, 2, 0, CacheType.MULTI.getL10n());
        menu.add(1, 3, 0, CacheType.MYSTERY.getL10n());

        // then add all other cache types sorted alphabetically
        List<String> sorted = new ArrayList<String>();
        for (CacheType ct : CacheType.values()) {
            if (ct == CacheType.ALL ||
                    ct == CacheType.TRADITIONAL ||
                    ct == CacheType.MULTI ||
                    ct == CacheType.MYSTERY) {
                continue;
            }
            sorted.add(ct.getL10n());
        }
        Collections.sort(sorted);
        for (String choice : sorted) {
            menu.add(1, menu.size(), 0, choice);
        }

        // mark current filter as checked
        menu.setGroupCheckable(1, true, true);
        boolean foundItem = false;
        int itemCount = menu.size();
        String typeTitle = Settings.getCacheType().getL10n();
        for (int i = 0; i < itemCount; i++) {
            if (menu.getItem(i).getTitle().equals(typeTitle)) {
                menu.getItem(i).setChecked(true);
                foundItem = true;
                break;
            }
        }
        if (!foundItem) {
            menu.getItem(0).setChecked(true);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == 0) {
            Settings.setCacheType(CacheType.ALL);
            setFilterTitle();

            return true;
        } else if (id > MENU_OPEN_LIST) {
            int listId = id - MENU_OPEN_LIST;
            Settings.saveLastList(listId);
            cgeocaches.startActivityOffline(this);
            return true;
        } else if (id > 0) {
            final String itemTitle = item.getTitle().toString();
            CacheType cacheType = CacheType.ALL;
            for (CacheType ct : CacheType.values()) {
                if (ct.getL10n().equalsIgnoreCase(itemTitle)) {
                    cacheType = ct;
                    break;
                }
            }
            Settings.setCacheType(cacheType);
            setFilterTitle();

            return true;
        }

        return false;
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

        //TODO This is ugly fix for #486 bug should be reported to library
        if (Thread.currentThread().getContextClassLoader() == null)
        {
            Thread.currentThread().setContextClassLoader(new ClassLoader() {
            });
            StringUtils.isNotBlank("haha");
        }

        initialized = true;

        Settings.setLanguage(Settings.isUseEnglish());

        /*
         * "update" the cache size/type. For a better performance
         * the resource strings are stored in the enum's. In case of a
         * locale change the resource strings don't get updated automatically.
         * That's why we have to do it on our own.
         */
        for (CacheSize cacheSize : CacheSize.values()) {
            cacheSize.setL10n();
        }
        for (CacheType cacheType : CacheType.values()) {
            cacheType.setL10n();
        }
        for (LogType logType : LogType.values()) {
            logType.setL10n();
        }
        for (WaypointType waypointType : WaypointType.values()) {
            waypointType.setL10n();
        }

        Settings.getLogin();

        if (app.firstRun) {
            (new firstLogin()).start();
        }

        if (geo == null) {
            geoUpdate = new UpdateLocation();
            geo = app.startGeo(geoUpdate);
        }

        final View findOnMap = findViewById(R.id.map);
        findOnMap.setClickable(true);
        findOnMap.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cgeoFindOnMap(v);
            }
        });

        final View findByOffline = findViewById(R.id.search_offline);
        findByOffline.setClickable(true);
        findByOffline.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cgeoFindByOffline(v);
            }
        });
        registerForContextMenu(findByOffline);

        final View advanced = findViewById(R.id.advanced_button);
        advanced.setClickable(true);
        advanced.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cgeoSearch(v);
            }
        });

        final View any = findViewById(R.id.any_button);
        any.setClickable(true);
        any.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cgeoPoint(v);
            }
        });

        final View filter = findViewById(R.id.filter_button);
        filter.setClickable(true);
        registerForContextMenu(filter);
        filter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openContextMenu(v);
            }
        });

        updateCacheCounter();

        setFilterTitle();
        checkRestore();
        (new cleanDatabase()).start();
    }

    private void updateCacheCounter() {
        (new CountBubbleUpdateThread()).start();
    }

    private void checkRestore() {
        if (!cgData.isNewlyCreatedDatebase() || null == cgData.isRestoreFile()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.init_backup_restore))
                .setMessage(res.getString(R.string.init_restore_confirm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        cgData.resetNewlyCreatedDatabase();
                        app.restoreDatabase(cgeo.this);
                        updateCacheCounter();
                    }
                })
                .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        cgData.resetNewlyCreatedDatabase();
                    }
                })
                .create()
                .show();
    }

    private class UpdateLocation implements UpdateLocationCallback {

        private final View nearestView = findViewById(R.id.nearest);
        private final TextView navType = (TextView) findViewById(R.id.nav_type);
        private final TextView navAccuracy = (TextView) findViewById(R.id.nav_accuracy);
        private final TextView navSatellites = (TextView) findViewById(R.id.nav_satellites);
        private final TextView navLocation = (TextView) findViewById(R.id.nav_location);

        @Override
        public void updateLocation(cgGeo geo) {
            if (geo == null) {
                return;
            }

            try {
                if (geo.coordsNow != null) {
                    if (!nearestView.isClickable()) {
                        nearestView.setFocusable(true);
                        nearestView.setClickable(true);
                        nearestView.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                cgeoFindNearest(v);
                            }
                        });
                        nearestView.setBackgroundResource(R.drawable.main_nearby);
                    }

                    String satellites = null;
                    if (geo.satellitesFixed > 0) {
                        satellites = res.getString(R.string.loc_sat) + ": " + geo.satellitesFixed + "/" + geo.satellitesVisible;
                    } else if (geo.satellitesVisible >= 0) {
                        satellites = res.getString(R.string.loc_sat) + ": 0/" + geo.satellitesVisible;
                    } else {
                        satellites = "";
                    }
                    navSatellites.setText(satellites);
                    navType.setText(res.getString(geo.locationProvider.resourceId));

                    if (geo.accuracyNow >= 0) {
                        if (Settings.isUseMetricUnits()) {
                            navAccuracy.setText("±" + Math.round(geo.accuracyNow) + " m");
                        } else {
                            navAccuracy.setText("±" + Math.round(geo.accuracyNow * 3.2808399) + " ft");
                        }
                    } else {
                        navAccuracy.setText(null);
                    }

                    if (Settings.isShowAddress()) {
                        if (addCoords == null) {
                            navLocation.setText(res.getString(R.string.loc_no_addr));
                        }
                        if (addCoords == null || (geo.coordsNow.distanceTo(addCoords) > 0.5 && !addressObtaining)) {
                            (new ObtainAddressThread()).start();
                        }
                    } else {
                        if (geo.altitudeNow != null) {
                            final String humanAlt = cgBase.getHumanDistance(geo.altitudeNow.floatValue() / 1000);
                            navLocation.setText(geo.coordsNow + " | " + humanAlt);
                        } else {
                            navLocation.setText(geo.coordsNow.toString());
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
                Log.w(Settings.tag, "Failed to update location.");
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
        if (geo == null || geo.coordsNow == null) {
            return;
        }

        findViewById(R.id.nearest).setPressed(true);
        cgeocaches.startActivityNearest(this, geo.coordsNow);
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
        startActivity(new Intent(this, cgeoadvsearch.class));
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
                    Log.w(Settings.tag, "cgeo.countBubbleHander: " + e.toString());
                }
            }
        };

        @Override
        public void run() {
            if (app == null) {
                return;
            }

            int checks = 0;
            while (!app.storageStatus()) {
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

            countBubbleCnt = app.getAllStoredCachesCount(true, CacheType.ALL, null);

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
                Log.i(Settings.tag, "Initializing hard cleanup - version changed from " + Settings.getVersion() + " to " + version + ".");

                more = true;
            }

            cleanupRunning = true;
            app.cleanDatabase(more);
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

            final StatusCode status = cgBase.login();

            if (status == StatusCode.NO_ERROR) {
                app.firstRun = false;
                cgBase.detectGcCustomDate();
            }

            if (app.showLoginToast) {
                firstLoginHandler.sendMessage(firstLoginHandler.obtainMessage(0, status));
                app.showLoginToast = false;

                // invoke settings activity to insert login details
                if (status == StatusCode.NO_LOGIN_INFO_STORED) {
                    final Context context = cgeo.this;
                    final Intent initIntent = new Intent(context, cgeoinit.class);
                    context.startActivity(initIntent);
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
            if (geo == null) {
                return;
            }
            if (addressObtaining) {
                return;
            }
            addressObtaining = true;

            try {
                final Geocoder geocoder = new Geocoder(cgeo.this, Locale.getDefault());

                addresses = geocoder.getFromLocation(geo.coordsNow.getLatitude(), geo.coordsNow.getLongitude(), 1);
            } catch (Exception e) {
                Log.i(Settings.tag, "Failed to obtain address");
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
        startActivity(new Intent(this, cgeoabout.class));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goSearch(View view) {
        onSearchRequested();
    }
}
