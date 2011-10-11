package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class cgeo extends AbstractActivity {

    private static final String SCAN_INTENT = "com.google.zxing.client.android.SCAN";
    private static final int MENU_ABOUT = 0;
    private static final int MENU_HELPERS = 1;
    private static final int MENU_SETTINGS = 2;
    private static final int MENU_HISTORY = 3;
    private static final int MENU_SCAN = 4;
    private static final int SCAN_REQUEST_CODE = 1;
    private static final int MENU_OPEN_LIST = 100;

    private Context context = null;
    private Integer version = null;
    private cgGeo geo = null;
    private cgUpdateLoc geoUpdate = new update();
    private TextView navType = null;
    private TextView navAccuracy = null;
    private TextView navSatellites = null;
    private TextView navLocation = null;
    private TextView filterTitle = null;
    private TextView countBubble = null;
    private boolean cleanupRunning = false;
    private int countBubbleCnt = 0;
    private Geopoint addCoords = null;
    private List<Address> addresses = null;
    private boolean addressObtaining = false;
    private boolean initialized = false;
    private Handler countBubbleHandler = new Handler() {

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

                    addCoords = geo.coordsNow;

                    if (navLocation == null) {
                        navLocation = (TextView) findViewById(R.id.nav_location);
                    }

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
                    showToast(res.getString(R.string.err_login_failed_toast));
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

        context = this;
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
                            ActivityMixin.goManual(context, "c:geo-intro");
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
        menu.add(0, MENU_SCAN, 0, res.getString(R.string.menu_scan)).setIcon(R.drawable.ic_menu_barcode);
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
                context.startActivity(new Intent(context, cgeohelpers.class));
                return true;
            case MENU_SETTINGS:
                context.startActivity(new Intent(context, cgeoinit.class));
                return true;
            case MENU_HISTORY:
                cgeocaches.startActivityHistory(context);
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
                    cgeodetail.startActivity(this, geocode);
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
            List<cgList> cacheLists = app.getLists();
            int listCount = cacheLists.size();
            menu.setHeaderTitle(res.getString(R.string.list_title));
            for (int i = 0; i < listCount; i++) {
                cgList list = cacheLists.get(i);
                menu.add(Menu.NONE, MENU_OPEN_LIST + list.id, Menu.NONE, list.title);
            }
            return;
        }

        // standard context menu
        menu.setHeaderTitle(res.getString(R.string.menu_filter));

        //first add the most used types
        menu.add(1, 0, 0, res.getString(R.string.all_types));
        menu.add(1, 1, 0, res.getString(R.string.traditional));
        menu.add(1, 2, 0, res.getString(R.string.multi));
        menu.add(1, 3, 0, res.getString(R.string.mystery));

        // then add all other cache types sorted alphabetically
        Map<String, String> allTypes = new HashMap<String, String>(cgBase.cacheTypesInv);
        allTypes.remove("traditional");
        allTypes.remove("multi");
        allTypes.remove("mystery");
        List<String> sorted = new ArrayList<String>(allTypes.values());
        Collections.sort(sorted);
        for (String choice : sorted) {
            menu.add(1, menu.size(), 0, choice);
        }

        // mark current filter as checked
        menu.setGroupCheckable(1, true, true);
        boolean foundItem = false;
        int itemCount = menu.size();
        if (Settings.getCacheType() != null) {
            String typeTitle = cgBase.cacheTypesInv.get(Settings.getCacheType());
            if (typeTitle != null) {
                for (int i = 0; i < itemCount; i++) {
                    if (menu.getItem(i).getTitle().equals(typeTitle)) {
                        menu.getItem(i).setChecked(true);
                        foundItem = true;
                        break;
                    }
                }
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
            Settings.setCacheType(null);
            setFilterTitle();

            return true;
        } else if (id > MENU_OPEN_LIST) {
            int listId = id - MENU_OPEN_LIST;
            Settings.saveLastList(listId);
            cgeocaches.startActivityOffline(context);
            return true;
        } else if (id > 0) {
            String itemTitle = item.getTitle().toString();
            String choice = null;
            for (Entry<String, String> entry : cgBase.cacheTypesInv.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(itemTitle)) {
                    choice = entry.getKey();
                    break;
                }
            }
            if (choice == null) {
                Settings.setCacheType(null);
            } else {
                Settings.setCacheType(choice);
            }
            setFilterTitle();

            return true;
        }

        return false;
    }

    private void setFilterTitle() {
        if (filterTitle == null) {
            filterTitle = (TextView) findViewById(R.id.filter_button_title);
        }
        if (Settings.getCacheType() != null) {
            filterTitle.setText(cgBase.cacheTypesInv.get(Settings.getCacheType()));
        } else {
            filterTitle.setText(res.getString(R.string.all));
        }
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

        Settings.getLogin();

        if (app.firstRun) {
            (new firstLogin()).start();
        }

        (new countBubbleUpdate()).start();
        (new cleanDatabase()).start();

        if (Settings.getCacheType() != null && !cgBase.cacheTypesInv.containsKey(Settings.getCacheType())) {
            Settings.setCacheType(null);
        }

        if (geo == null) {
            geo = app.startGeo(context, geoUpdate, base, 0, 0);
        }

        navType = (TextView) findViewById(R.id.nav_type);
        navAccuracy = (TextView) findViewById(R.id.nav_accuracy);
        navLocation = (TextView) findViewById(R.id.nav_location);

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

        (new countBubbleUpdate()).start();

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

        setFilterTitle();
    }

    private class update extends cgUpdateLoc {

        @Override
        public void updateLoc(cgGeo geo) {
            if (geo == null) {
                return;
            }

            try {
                if (navType == null || navLocation == null || navAccuracy == null) {
                    navType = (TextView) findViewById(R.id.nav_type);
                    navAccuracy = (TextView) findViewById(R.id.nav_accuracy);
                    navSatellites = (TextView) findViewById(R.id.nav_satellites);
                    navLocation = (TextView) findViewById(R.id.nav_location);
                }

                if (geo.coordsNow != null) {
                    View findNearest = findViewById(R.id.nearest);
                    findNearest.setClickable(true);
                    findNearest.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            cgeoFindNearest(v);
                        }
                    });
                    findNearest.setBackgroundResource(R.drawable.main_nearby);

                    String satellites = null;
                    if (geo.satellitesVisible != null && geo.satellitesFixed != null && geo.satellitesFixed > 0) {
                        satellites = res.getString(R.string.loc_sat) + ": " + geo.satellitesFixed + "/" + geo.satellitesVisible;
                    } else if (geo.satellitesVisible != null) {
                        satellites = res.getString(R.string.loc_sat) + ": 0/" + geo.satellitesVisible;
                    } else {
                        satellites = "";
                    }
                    navSatellites.setText(satellites);

                    if (geo.gps == -1) {
                        navType.setText(res.getString(R.string.loc_last));
                    } else if (geo.gps == 0) {
                        navType.setText(res.getString(R.string.loc_net));
                    } else {
                        navType.setText(res.getString(R.string.loc_gps));
                    }

                    if (geo.accuracyNow != null) {
                        if (Settings.isUseMetricUnits()) {
                            navAccuracy.setText("±" + String.format(Locale.getDefault(), "%.0f", geo.accuracyNow) + " m");
                        } else {
                            navAccuracy.setText("±" + String.format(Locale.getDefault(), "%.0f", (geo.accuracyNow * 3.2808399)) + " ft");
                        }
                    } else {
                        navAccuracy.setText(null);
                    }

                    if (Settings.isShowAddress()) {
                        if (addCoords == null) {
                            navLocation.setText(res.getString(R.string.loc_no_addr));
                        }
                        if (addCoords == null || (geo.coordsNow.distanceTo(addCoords) > 0.5 && !addressObtaining)) {
                            (new obtainAddress()).start();
                        }
                    } else {
                        if (geo.altitudeNow != null) {
                            String humanAlt;
                            if (Settings.isUseMetricUnits()) {
                                humanAlt = String.format("%.0f", geo.altitudeNow) + " m";
                            } else {
                                humanAlt = String.format("%.0f", (geo.altitudeNow * 3.2808399)) + " ft";
                            }
                            navLocation.setText(cgBase.formatCoords(geo.coordsNow, true) + " | " + humanAlt);
                        } else {
                            navLocation.setText(cgBase.formatCoords(geo.coordsNow, true));
                        }
                    }
                } else {
                    View findNearest = findViewById(R.id.nearest);
                    findNearest.setFocusable(false);
                    findNearest.setClickable(false);
                    findNearest.setOnClickListener(null);
                    findNearest.setBackgroundResource(R.drawable.main_nearby_disabled);

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
        context.startActivity(new Intent(context, Settings.getMapFactory().getMapClass()));
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
        cgeocaches.startActivityNearest(context, geo.coordsNow);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFindByOffline(View v) {
        findViewById(R.id.search_offline).setPressed(true);
        cgeocaches.startActivityOffline(context);
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoSearch(View v) {
        findViewById(R.id.advanced_button).setPressed(true);
        context.startActivity(new Intent(context, cgeoadvsearch.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoPoint(View v) {
        findViewById(R.id.any_button).setPressed(true);
        context.startActivity(new Intent(context, cgeopoint.class));
    }

    /**
     * @param v
     *            unused here but needed since this method is referenced from XML layout
     */
    public void cgeoFilter(View v) {
        findViewById(R.id.filter_button).setPressed(true);
        findViewById(R.id.filter_button).performClick();
    }

    private class countBubbleUpdate extends Thread {

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

            countBubbleCnt = app.getAllStoredCachesCount(true, null, null);

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

            if (version != null && version > 0) {
                SharedPreferences.Editor edit = prefs.edit();
                edit.putInt("version", version);
                edit.commit();
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

    private class obtainAddress extends Thread {

        public obtainAddress() {
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
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());

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
        context.startActivity(new Intent(context, cgeoabout.class));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goSearch(View view) {
        onSearchRequested();
    }
}
