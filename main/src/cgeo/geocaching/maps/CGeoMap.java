package cgeo.geocaching.maps;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.LiveMapInfo;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.UpdateDirectionCallback;
import cgeo.geocaching.UpdateLocationCallback;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgDirection;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.cgeocaches;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCBase;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LiveMapStrategy.Strategy;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.go4cache.Go4Cache;
import cgeo.geocaching.go4cache.Go4CacheUser;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;
import cgeo.geocaching.network.Login;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.LeastRecentlyUsedSet;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class representing the Map in c:geo
 */
public class CGeoMap extends AbstractMap implements OnMapDragListener, ViewFactory {

    /** max. number of caches displayed in the Live Map */
    public static final int MAX_CACHES = 500;

    /** Handler Messages */
    private static final int HIDE_PROGRESS = 0;
    private static final int SHOW_PROGRESS = 1;
    private static final int UPDATE_TITLE = 0;
    private static final int INVALIDATE_MAP = 1;
    private static final int UPDATE_PROGRESS = 0;
    private static final int FINISHED_LOADING_DETAILS = 1;

    //Menu
    private static final String EXTRAS_GEOCODE = "geocode";
    private static final String EXTRAS_LONGITUDE = "longitude";
    private static final String EXTRAS_LATITUDE = "latitude";
    private static final String EXTRAS_WPTTYPE = "wpttype";
    private static final String EXTRAS_MAPSTATE = "mapstate";
    private static final String EXTRAS_SEARCH = "search";
    private static final int MENU_SELECT_MAPVIEW = 1;
    private static final int MENU_MAP_LIVE = 2;
    private static final int MENU_STORE_CACHES = 3;
    private static final int MENU_TRAIL_MODE = 4;
    private static final int SUBMENU_STRATEGY = 5;
    private static final int MENU_STRATEGY_FASTEST = 51;
    private static final int MENU_STRATEGY_FAST = 52;
    private static final int MENU_STRATEGY_AUTO = 53;
    private static final int MENU_STRATEGY_DETAILED = 74;

    private static final int MENU_CIRCLE_MODE = 6;
    private static final int MENU_AS_LIST = 7;

    private static final String EXTRAS_MAP_TITLE = "mapTitle";

    private Resources res = null;
    private MapProvider mapProvider = null;
    private Activity activity = null;
    private MapViewImpl mapView = null;
    private MapControllerImpl mapController = null;
    private cgeoapplication app = null;
    private cgGeo geo = null;
    private cgDirection dir = null;
    private UpdateLocationCallback geoUpdate = new UpdateLoc();
    private UpdateDirectionCallback dirUpdate = new UpdateDir();
    private SearchResult searchIntent = null;
    private String geocodeIntent = null;
    private Geopoint coordsIntent = null;
    private WaypointType waypointTypeIntent = null;
    private int[] mapStateIntent = null;
    // status data
    private SearchResult search = null;
    private String[] tokens = null;
    private boolean noMapTokenShowed = false;
    // map status data
    private boolean followMyLocation = false;
    private Integer centerLatitude = null;
    private Integer centerLongitude = null;
    private Integer spanLatitude = null;
    private Integer spanLongitude = null;
    private Integer centerLatitudeUsers = null;
    private Integer centerLongitudeUsers = null;
    private Integer spanLatitudeUsers = null;
    private Integer spanLongitudeUsers = null;
    private int zoom = -100;
    // threads
    private LoadTimer loadTimer = null;
    private Go4CacheTimer go4CacheTimer = null;
    private LoadDetails loadDetailsThread = null;
    /** Time of last {@link LoadRunnable} run */
    private volatile long loadThreadRun = 0L;
    /** Time of last {@link Go4CacheRunnable} run */
    private volatile long go4CacheThreadRun = 0L;
    //Interthread communication flag
    private volatile boolean downloaded = false;
    // overlays
    private CachesOverlay overlayCaches = null;
    private OtherCachersOverlay overlayGo4Cache = null;
    private ScaleOverlay overlayScale = null;
    private PositionOverlay overlayPosition = null;
    // data for overlays
    private static final int[][] INSET_RELIABLE = { { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }; // center, 33x40 / 45x51
    private static final int[][] INSET_TYPE = { { 5, 8, 6, 10 }, { 4, 4, 5, 11 } }; // center, 22x22 / 36x36
    private static final int[][] INSET_OWN = { { 21, 0, 0, 26 }, { 25, 0, 0, 35 } }; // top right, 12x12 / 16x16
    private static final int[][] INSET_FOUND = { { 0, 0, 21, 28 }, { 0, 0, 25, 35 } }; // top left, 12x12 / 16x16
    private static final int[][] INSET_USERMODIFIEDCOORDS = { { 21, 28, 0, 0 }, { 19, 25, 0, 0 } }; // bottom right, 12x12 / 26x26
    private static final int[][] INSET_PERSONALNOTE = { { 0, 28, 21, 0 }, { 0, 25, 19, 0 } }; // bottom left, 12x12 / 26x26

    private static Map<Integer, LayerDrawable> overlaysCache = new HashMap<Integer, LayerDrawable>();
    private int cachesCnt = 0;
    /** List of caches in the viewport */
    private final LeastRecentlyUsedSet<cgCache> caches = new LeastRecentlyUsedSet<cgCache>(MAX_CACHES);
    /** List of waypoints in the viewport */
    private final LeastRecentlyUsedSet<cgWaypoint> waypoints = new LeastRecentlyUsedSet<cgWaypoint>(MAX_CACHES);
    // storing for offline
    private ProgressDialog waitDialog = null;
    private int detailTotal = 0;
    private int detailProgress = 0;
    private long detailProgressTime = 0L;
    // views
    private ImageSwitcher myLocSwitch = null;
    // other things
    private boolean live = true; // live map (live, dead) or rest (displaying caches on map)
    private boolean liveChanged = false; // previous state for loadTimer
    private boolean centered = false; // if map is already centered
    private boolean alreadyCentered = false; // -""- for setting my location
    private static Set<String> dirtyCaches = null;

    // Thread pooling
    private static BlockingQueue<Runnable> displayQueue = new ArrayBlockingQueue<Runnable>(1);
    private static ThreadPoolExecutor displayExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, displayQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static BlockingQueue<Runnable> downloadQueue = new ArrayBlockingQueue<Runnable>(1);
    private static ThreadPoolExecutor downloadExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, downloadQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static BlockingQueue<Runnable> loadQueue = new ArrayBlockingQueue<Runnable>(1);
    private static ThreadPoolExecutor loadExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, loadQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static BlockingQueue<Runnable> Go4CacheQueue = new ArrayBlockingQueue<Runnable>(1);
    private static ThreadPoolExecutor Go4CacheExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, Go4CacheQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static BlockingQueue<Runnable> go4CacheDisplayQueue = new ArrayBlockingQueue<Runnable>(1);
    private static ThreadPoolExecutor go4CacheDisplayExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, go4CacheDisplayQueue, new ThreadPoolExecutor.DiscardOldestPolicy());


    // handlers
    /** Updates the titles */
    final private Handler displayHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            switch (what) {
                case UPDATE_TITLE:
                    // set title
                    final StringBuilder title = new StringBuilder();

                    if (live) {
                        title.append(res.getString(R.string.map_live));
                    } else {
                        title.append(mapTitle);
                    }

                    countVisibleCaches();
                    if (caches != null && caches.size() > 0 && !mapTitle.contains("[")) {
                        title.append(" [").append(cachesCnt);
                        if (cachesCnt != caches.size()) {
                            title.append('/').append(caches.size());
                        }
                        title.append(']');
                    }

                    ActivityMixin.setTitle(activity, title.toString());
                    break;
                case INVALIDATE_MAP:
                    mapView.repaintRequired(null);
                    break;

                default:
                    break;
            }
        }
    };
    /** Updates the progress. */
    final private Handler showProgressHandler = new Handler() {

        private int counter = 0;

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            if (what == HIDE_PROGRESS) {
                if (--counter == 0) {
                    ActivityMixin.showProgress(activity, false);
                }
            } else if (what == SHOW_PROGRESS) {
                ActivityMixin.showProgress(activity, true);
                counter++;
            }
        }
    };

    final private class LoadDetailsHandler extends CancellableHandler {

        @Override
        public void handleRegularMessage(Message msg) {
            if (msg.what == UPDATE_PROGRESS) {
                if (waitDialog != null) {
                    int secondsElapsed = (int) ((System.currentTimeMillis() - detailProgressTime) / 1000);
                    int secondsRemaining;
                    if (detailProgress > 0) {
                        secondsRemaining = (detailTotal - detailProgress) * secondsElapsed / detailProgress;
                    } else {
                        secondsRemaining = (detailTotal - detailProgress) * secondsElapsed;
                    }

                    waitDialog.setProgress(detailProgress);
                    if (secondsRemaining < 40) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
                    } else if (secondsRemaining < 90) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + (secondsRemaining / 60) + " " + res.getString(R.string.caches_eta_min));
                    } else {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + (secondsRemaining / 60) + " " + res.getString(R.string.caches_eta_mins));
                    }
                }
            } else if (msg.what == FINISHED_LOADING_DETAILS) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }

                if (geo == null) {
                    geo = app.startGeo(geoUpdate);
                }
                if (Settings.isUseCompass() && dir == null) {
                    dir = app.startDir(activity, dirUpdate);
                }
            }
        }

        @Override
        public void handleCancel(final Object extra) {
            if (loadDetailsThread != null) {
                loadDetailsThread.stopIt();
            }

            if (geo == null) {
                geo = app.startGeo(geoUpdate);
            }
            if (Settings.isUseCompass() && dir == null) {
                dir = app.startDir(activity, dirUpdate);
            }
        }
    }

    final private Handler noMapTokenHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (!noMapTokenShowed) {
                ActivityMixin.showToast(activity, res.getString(R.string.map_token_err));

                noMapTokenShowed = true;
            }
        }
    };
    /**
     * calling activities can set the map title via extras
     */
    private String mapTitle;

    public CGeoMap(MapActivityImpl activity) {
        super(activity);
    }

    protected void countVisibleCaches() {
        final ArrayList<cgCache> protectedCaches = new ArrayList<cgCache>(caches);

        int count = 0;
        if (protectedCaches.size() > 0) {
            final GeoPointImpl mapCenter = mapView.getMapViewCenter();
            final int mapCenterLat = mapCenter.getLatitudeE6();
            final int mapCenterLon = mapCenter.getLongitudeE6();
            final int mapSpanLat = mapView.getLatitudeSpan();
            final int mapSpanLon = mapView.getLongitudeSpan();

            for (cgCache cache : protectedCaches) {
                if (cache != null && cache.getCoords() != null) {
                    if (Viewport.isCacheInViewPort(mapCenterLat, mapCenterLon, mapSpanLat, mapSpanLon, cache.getCoords())) {
                        count++;
                    }
                }
            }
        }
        cachesCnt = count;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // class init
        res = this.getResources();
        activity = this.getActivity();
        app = (cgeoapplication) activity.getApplication();
        mapProvider = Settings.getMapProvider();

        // reset status
        noMapTokenShowed = false;

        ActivityMixin.keepScreenOn(activity, true);

        // set layout
        ActivityMixin.setTheme(activity);
        activity.setContentView(mapProvider.getMapLayoutId());
        ActivityMixin.setTitle(activity, res.getString(R.string.map_map));

        if (geo == null) {
            geo = app.startGeo(geoUpdate);
        }
        if (Settings.isUseCompass() && dir == null) {
            dir = app.startDir(activity, dirUpdate);
        }

        // initialize map
        mapView = (MapViewImpl) activity.findViewById(mapProvider.getMapViewId());
        mapView.setMapSource();
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);
        mapView.preLoad();
        mapView.setOnDragListener(this);

        // initialize overlays
        mapView.clearOverlays();

        if (overlayPosition == null) {
            overlayPosition = mapView.createAddPositionOverlay(activity);
        }

        if (Settings.isPublicLoc() && overlayGo4Cache == null) {
            overlayGo4Cache = mapView.createAddUsersOverlay(activity, getResources().getDrawable(R.drawable.user_location));
        }

        if (overlayCaches == null) {
            overlayCaches = mapView.createAddMapOverlay(mapView.getContext(), getResources().getDrawable(R.drawable.marker));
        }

        if (overlayScale == null) {
            overlayScale = mapView.createAddScaleOverlay(activity);
        }

        mapView.repaintRequired(null);

        mapController = mapView.getMapController();
        mapController.setZoom(Settings.getMapZoom());

        // start location and directory services
        if (geo != null) {
            geoUpdate.updateLocation(geo);
        }
        if (dir != null) {
            dirUpdate.updateDirection(dir);
        }

        // get parameters
        Bundle extras = activity.getIntent().getExtras();
        if (extras != null) {
            searchIntent = (SearchResult) extras.getParcelable(EXTRAS_SEARCH);
            geocodeIntent = extras.getString(EXTRAS_GEOCODE);
            final double latitudeIntent = extras.getDouble(EXTRAS_LATITUDE);
            final double longitudeIntent = extras.getDouble(EXTRAS_LONGITUDE);
            coordsIntent = new Geopoint(latitudeIntent, longitudeIntent);
            waypointTypeIntent = WaypointType.findById(extras.getString(EXTRAS_WPTTYPE));
            mapStateIntent = extras.getIntArray(EXTRAS_MAPSTATE);
            mapTitle = extras.getString(EXTRAS_MAP_TITLE);

            if (coordsIntent.getLatitude() == 0.0 || coordsIntent.getLongitude() == 0.0) {
                coordsIntent = null;
            }
        }

        if (StringUtils.isBlank(mapTitle)) {
            mapTitle = res.getString(R.string.map_map);
        }

        // live map, if no arguments are given
        live = (searchIntent == null && geocodeIntent == null && coordsIntent == null);

        if (null == mapStateIntent) {
            followMyLocation = live;
        } else {
            followMyLocation = 1 == mapStateIntent[3] ? true : false;
        }
        if (geocodeIntent != null || searchIntent != null || coordsIntent != null || mapStateIntent != null) {
            centerMap(geocodeIntent, searchIntent, coordsIntent, mapStateIntent);
        }

        // prepare my location button
        myLocSwitch = (ImageSwitcher) activity.findViewById(R.id.my_position);
        myLocSwitch.setFactory(this);
        myLocSwitch.setInAnimation(activity, android.R.anim.fade_in);
        myLocSwitch.setOutAnimation(activity, android.R.anim.fade_out);
        myLocSwitch.setOnClickListener(new MyLocationListener());
        switchMyLocationButton();

        prepareFilterBar();

        if (!Settings.getHideLiveMapHint()) {
            Intent hintIntent = new Intent(activity, LiveMapInfo.class);
            activity.startActivity(hintIntent);
        }
    }

    private void prepareFilterBar() {
        // show the filter warning bar if the filter is set
        if (Settings.getCacheType() != CacheType.ALL) {
            String cacheType = Settings.getCacheType().getL10n();
            ((TextView) activity.findViewById(R.id.filter_text)).setText(cacheType);
            activity.findViewById(R.id.filter_bar).setVisibility(View.VISIBLE);
        } else {
            activity.findViewById(R.id.filter_bar).setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        app.setAction(StringUtils.defaultIfBlank(geocodeIntent, null));
        if (geo == null) {
            geo = app.startGeo(geoUpdate);
        }
        if (Settings.isUseCompass() && dir == null) {
            dir = app.startDir(activity, dirUpdate);
        }

        geoUpdate.updateLocation(geo);

        if (dir != null) {
            dirUpdate.updateDirection(dir);
        }

        if (!CollectionUtils.isEmpty(dirtyCaches)) {
            for (String geocode : dirtyCaches) {
                cgCache cache = app.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                // remove to update the cache
                caches.remove(cache);
                caches.add(cache);
            }
            dirtyCaches.clear();
            // Update display
            GeoPointImpl mapCenterNow = mapView.getMapViewCenter();
            int centerLatitudeNow = mapCenterNow.getLatitudeE6();
            int centerLongitudeNow = mapCenterNow.getLongitudeE6();
            int spanLatitudeNow = mapView.getLatitudeSpan();
            int spanLongitudeNow = mapView.getLongitudeSpan();
            displayExecutor.execute(new DisplayRunnable(centerLatitudeNow, centerLongitudeNow, spanLatitudeNow, spanLongitudeNow));
        }

        startTimer();
    }

    @Override
    public void onStop() {
        if (loadTimer != null) {
            loadTimer.stopIt();
            loadTimer = null;
        }

        if (go4CacheTimer != null) {
            go4CacheTimer.stopIt();
            go4CacheTimer = null;
        }

        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        savePrefs();

        if (mapView != null) {
            mapView.destroyDrawingCache();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        if (loadTimer != null) {
            loadTimer.stopIt();
            loadTimer = null;
        }

        if (go4CacheTimer != null) {
            go4CacheTimer.stopIt();
            go4CacheTimer = null;
        }

        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        savePrefs();

        if (mapView != null) {
            mapView.destroyDrawingCache();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (loadTimer != null) {
            loadTimer.stopIt();
            loadTimer = null;
        }

        if (go4CacheTimer != null) {
            go4CacheTimer.stopIt();
            go4CacheTimer = null;
        }

        if (dir != null) {
            dir = app.removeDir();
        }
        if (geo != null) {
            geo = app.removeGeo();
        }

        savePrefs();

        if (mapView != null) {
            mapView.destroyDrawingCache();
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        SubMenu submenu = menu.addSubMenu(1, MENU_SELECT_MAPVIEW, 0, res.getString(R.string.map_view_map)).setIcon(android.R.drawable.ic_menu_mapmode);
        addMapViewMenuItems(submenu);

        menu.add(0, MENU_MAP_LIVE, 0, res.getString(R.string.map_live_disable)).setIcon(R.drawable.ic_menu_refresh);
        menu.add(0, MENU_STORE_CACHES, 0, res.getString(R.string.caches_store_offline)).setIcon(android.R.drawable.ic_menu_set_as).setEnabled(false);
        menu.add(0, MENU_TRAIL_MODE, 0, res.getString(R.string.map_trail_hide)).setIcon(R.drawable.ic_menu_trail);

        Strategy strategy = Settings.getLiveMapStrategy();
        SubMenu subMenuStrategy = menu.addSubMenu(0, SUBMENU_STRATEGY, 0, res.getString(R.string.map_strategy)).setIcon(android.R.drawable.ic_menu_preferences);
        subMenuStrategy.setHeaderTitle(res.getString(R.string.map_strategy_title));
        subMenuStrategy.add(2, MENU_STRATEGY_FASTEST, 0, Strategy.FASTEST.getL10n()).setCheckable(true).setChecked(strategy == Strategy.FASTEST);
        subMenuStrategy.add(2, MENU_STRATEGY_FAST, 0, Strategy.FAST.getL10n()).setCheckable(true).setChecked(strategy == Strategy.FAST);
        subMenuStrategy.add(2, MENU_STRATEGY_AUTO, 0, Strategy.AUTO.getL10n()).setCheckable(true).setChecked(strategy == Strategy.AUTO);
        subMenuStrategy.add(2, MENU_STRATEGY_DETAILED, 0, Strategy.DETAILED.getL10n()).setCheckable(true).setChecked(strategy == Strategy.DETAILED);
        subMenuStrategy.setGroupCheckable(2, true, true);

        menu.add(0, MENU_CIRCLE_MODE, 0, res.getString(R.string.map_circles_hide)).setIcon(R.drawable.ic_menu_circle);
        menu.add(0, MENU_AS_LIST, 0, res.getString(R.string.map_as_list)).setIcon(android.R.drawable.ic_menu_agenda);

        return true;
    }

    private static void addMapViewMenuItems(final Menu menu) {
        MapProviderFactory.addMapviewMenuItems(menu, 1, Settings.getMapSource());
        menu.setGroupCheckable(1, true, true);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item;
        try {
            item = menu.findItem(MENU_TRAIL_MODE); // show trail
            if (Settings.isMapTrail()) {
                item.setTitle(res.getString(R.string.map_trail_hide));
            } else {
                item.setTitle(res.getString(R.string.map_trail_show));
            }

            item = menu.findItem(MENU_MAP_LIVE); // live map
            if (live) {
                if (Settings.isLiveMap()) {
                    item.setTitle(res.getString(R.string.map_live_disable));
                } else {
                    item.setTitle(res.getString(R.string.map_live_enable));
                }
            } else {
                item.setEnabled(false);
                item.setTitle(res.getString(R.string.map_live_enable));
            }

            menu.findItem(MENU_STORE_CACHES).setEnabled(live && !isLoading() && CollectionUtils.isNotEmpty(caches) && app.hasUnsavedCaches(search));

            item = menu.findItem(MENU_CIRCLE_MODE); // show circles
            if (overlayCaches != null && overlayCaches.getCircles()) {
                item.setTitle(res.getString(R.string.map_circles_hide));
            } else {
                item.setTitle(res.getString(R.string.map_circles_show));
            }

            item = menu.findItem(MENU_AS_LIST);
            item.setEnabled(live && CollectionUtils.isNotEmpty(caches));

            menu.findItem(SUBMENU_STRATEGY).setEnabled(live);
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeomap.onPrepareOptionsMenu: " + e.toString());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case MENU_TRAIL_MODE:
                Settings.setMapTrail(!Settings.isMapTrail());
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case MENU_MAP_LIVE:
                Settings.setLiveMap(!Settings.isLiveMap());
                liveChanged = true;
                search = null;
                searchIntent = null;
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case MENU_STORE_CACHES:
                if (live && !isLoading() && CollectionUtils.isNotEmpty(caches)) {
                    final List<String> geocodes = new ArrayList<String>();

                    List<cgCache> cachesProtected = new ArrayList<cgCache>(caches);
                    try {
                        if (cachesProtected.size() > 0) {
                            final GeoPointImpl mapCenter = mapView.getMapViewCenter();
                            final int mapCenterLat = mapCenter.getLatitudeE6();
                            final int mapCenterLon = mapCenter.getLongitudeE6();
                            final int mapSpanLat = mapView.getLatitudeSpan();
                            final int mapSpanLon = mapView.getLongitudeSpan();

                            for (cgCache cache : cachesProtected) {
                                if (cache != null && cache.getCoords() != null) {
                                    if (Viewport.isCacheInViewPort(mapCenterLat, mapCenterLon, mapSpanLat, mapSpanLon, cache.getCoords()) && !app.isOffline(cache.getGeocode(), null)) {
                                        geocodes.add(cache.getGeocode());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(Settings.tag, "cgeomap.onOptionsItemSelected.#4: " + e.toString());
                    }

                    detailTotal = geocodes.size();
                    detailProgress = 0;

                    if (detailTotal == 0) {
                        ActivityMixin.showToast(activity, res.getString(R.string.warn_save_nothing));

                        return true;
                    }

                    final LoadDetailsHandler loadDetailsHandler = new LoadDetailsHandler();

                    waitDialog = new ProgressDialog(activity);
                    waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    waitDialog.setCancelable(true);
                    waitDialog.setCancelMessage(loadDetailsHandler.cancelMessage());
                    waitDialog.setMax(detailTotal);
                    waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                        public void onCancel(DialogInterface arg0) {
                            try {
                                if (loadDetailsThread != null) {
                                    loadDetailsThread.stopIt();
                                }

                                if (geo == null) {
                                    geo = app.startGeo(geoUpdate);
                                }
                                if (Settings.isUseCompass() && dir == null) {
                                    dir = app.startDir(activity, dirUpdate);
                                }
                            } catch (Exception e) {
                                Log.e(Settings.tag, "cgeocaches.onPrepareOptionsMenu.onCancel: " + e.toString());
                            }
                        }
                    });

                    float etaTime = detailTotal * 7.0f / 60.0f;
                    if (etaTime < 0.4) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
                    } else if (etaTime < 1.5) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + Math.round(etaTime) + " " + res.getString(R.string.caches_eta_min));
                    } else {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + Math.round(etaTime) + " " + res.getString(R.string.caches_eta_mins));
                    }
                    waitDialog.show();

                    detailProgressTime = System.currentTimeMillis();

                    loadDetailsThread = new LoadDetails(loadDetailsHandler, geocodes);
                    loadDetailsThread.start();
                }
                return true;
            case MENU_CIRCLE_MODE:
                if (overlayCaches == null) {
                    return false;
                }

                overlayCaches.switchCircles();
                mapView.repaintRequired(overlayCaches);
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case MENU_AS_LIST: {
                cgeocaches.startActivityMap(activity, search);
                return true;
            }
            case MENU_STRATEGY_FASTEST: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(Strategy.FASTEST);
                return true;
            }
            case MENU_STRATEGY_FAST: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(Strategy.FAST);
                return true;
            }
            case MENU_STRATEGY_AUTO: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(Strategy.AUTO);
                return true;
            }
            case MENU_STRATEGY_DETAILED: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(Strategy.DETAILED);
                return true;
            }
            default:
                if (MapProviderFactory.isValidSourceId(MapProviderFactory.getMapSourceFromMenuId(id))) {
                    item.setChecked(true);
                    int mapSource = MapProviderFactory.getMapSourceFromMenuId(id);

                    boolean mapRestartRequired = switchMapSource(mapSource);

                    if (mapRestartRequired) {
                        // close old mapview
                        activity.finish();

                        // prepare information to restart a similar view
                        Intent mapIntent = new Intent(activity, Settings.getMapProvider().getMapClass());

                        mapIntent.putExtra(EXTRAS_SEARCH, searchIntent);
                        mapIntent.putExtra(EXTRAS_GEOCODE, geocodeIntent);
                        if (coordsIntent != null) {
                            mapIntent.putExtra(EXTRAS_LATITUDE, coordsIntent.getLatitude());
                            mapIntent.putExtra(EXTRAS_LONGITUDE, coordsIntent.getLongitude());
                        }
                        mapIntent.putExtra(EXTRAS_WPTTYPE, waypointTypeIntent != null ? waypointTypeIntent.id : null);
                        int[] mapState = new int[4];
                        GeoPointImpl mapCenter = mapView.getMapViewCenter();
                        mapState[0] = mapCenter.getLatitudeE6();
                        mapState[1] = mapCenter.getLongitudeE6();
                        mapState[2] = mapView.getMapZoomLevel();
                        mapState[3] = followMyLocation ? 1 : 0;
                        mapIntent.putExtra(EXTRAS_MAPSTATE, mapState);
                        mapIntent.putExtra(EXTRAS_MAP_TITLE, mapTitle);

                        // start the new map
                        activity.startActivity(mapIntent);

                    }

                    return true;
                }
        }
        return false;
    }

    private boolean switchMapSource(int sourceId) {
        boolean mapRestartRequired = !MapProviderFactory.isSameProvider(Settings.getMapSource(), sourceId);

        Settings.setMapSource(sourceId);

        if (!mapRestartRequired) {
            mapView.setMapSource();
        }

        return mapRestartRequired;
    }

    private void savePrefs() {
        if (mapView == null) {
            return;
        }

        Settings.setMapZoom(mapView.getMapZoomLevel());
    }

    // Set center of map to my location if appropriate.
    private void myLocationInMiddle() {
        if (followMyLocation && geo != null) {
            centerMap(geo.coordsNow);
        }
    }

    // class: update location
    private class UpdateLoc implements UpdateLocationCallback {

        @Override
        public void updateLocation(cgGeo geo) {
            if (geo == null) {
                return;
            }

            try {
                boolean repaintRequired = false;

                if (overlayPosition == null && mapView != null) {
                    overlayPosition = mapView.createAddPositionOverlay(activity);
                }

                if (overlayPosition != null && geo.location != null) {
                    overlayPosition.setCoordinates(geo.location);
                }

                if (geo.coordsNow != null) {
                    if (followMyLocation) {
                        myLocationInMiddle();
                    } else {
                        repaintRequired = true;
                    }
                }

                if (!Settings.isUseCompass() || geo.speedNow > 5) { // use GPS when speed is higher than 18 km/h
                    overlayPosition.setHeading(geo.bearingNow);
                    repaintRequired = true;
                }

                if (repaintRequired && mapView != null) {
                    mapView.repaintRequired(overlayPosition);
                }

            } catch (Exception e) {
                Log.w(Settings.tag, "Failed to update location.");
            }
        }
    }

    // class: update direction
    private class UpdateDir implements UpdateDirectionCallback {

        @Override
        public void updateDirection(cgDirection dir) {
            if (dir == null || dir.directionNow == null) {
                return;
            }

            if (overlayPosition != null && mapView != null && (geo == null || geo.speedNow <= 5)) { // use compass when speed is lower than 18 km/h
                overlayPosition.setHeading(dir.directionNow);
                mapView.repaintRequired(overlayPosition);
            }
        }
    }

    /**
     * Starts the {@link LoadTimer} and {@link Go4CacheTimer}.
     */

    public synchronized void startTimer() {
        if (coordsIntent != null) {
            // display just one point
            (new DisplayPointThread()).start();
        } else {
            // start timer
            if (loadTimer != null) {
                loadTimer.stopIt();
                loadTimer = null;
            }
            loadTimer = new LoadTimer();
            loadTimer.start();
        }

        if (Settings.isPublicLoc()) {
            if (go4CacheTimer != null) {
                go4CacheTimer.stopIt();
                go4CacheTimer = null;
            }
            go4CacheTimer = new Go4CacheTimer();
            go4CacheTimer.start();
        }
    }

    /**
     * loading timer Triggers every 250ms and checks for viewport change and starts a {@link LoadRunnable}.
     */
    private class LoadTimer extends Thread {

    	public LoadTimer() {
            super("Load Timer");
        }

        private volatile boolean stop = false;

        public void stopIt() {
            stop = true;
        }

        @Override
        public void run() {

            while (!stop) {
                try {
                    sleep(250);

                    if (mapView != null) {
                        // get current viewport
                        GeoPointImpl mapCenterNow = mapView.getMapViewCenter();
                        int centerLatitudeNow = mapCenterNow.getLatitudeE6();
                        int centerLongitudeNow = mapCenterNow.getLongitudeE6();
                        int spanLatitudeNow = mapView.getLatitudeSpan();
                        int spanLongitudeNow = mapView.getLongitudeSpan();

                        // check if map moved or zoomed
                        //TODO Portree Use Rectangle inside with bigger search window. That will stop reloading on every move
                        boolean moved = false;

                        if (liveChanged) {
                            moved = true;
                        } else if (live && Settings.isLiveMap() && !downloaded) {
                            moved = true;
                        } else if (centerLatitude == null || centerLongitude == null) {
                            moved = true;
                        } else if (spanLatitude == null || spanLongitude == null) {
                            moved = true;
                        } else if (((Math.abs(spanLatitudeNow - spanLatitude) > 50) || (Math.abs(spanLongitudeNow - spanLongitude) > 50) || // changed zoom
                                (Math.abs(centerLatitudeNow - centerLatitude) > (spanLatitudeNow / 4)) || (Math.abs(centerLongitudeNow - centerLongitude) > (spanLongitudeNow / 4)) // map moved
                        ) && (cachesCnt <= 0 || CollectionUtils.isEmpty(caches)
                                || !Viewport.isInViewPort(centerLatitude, centerLongitude, centerLatitudeNow, centerLongitudeNow, spanLatitude, spanLongitude, spanLatitudeNow, spanLongitudeNow))) {
                            moved = true;
                        }

                        // update title on any change
                        int zoomNow = mapView.getMapZoomLevel();
                        if (moved || zoomNow != zoom || spanLatitudeNow != spanLatitude || spanLongitudeNow != spanLongitude || centerLatitudeNow != centerLatitude || centerLongitudeNow != centerLongitude) {
                            displayHandler.sendEmptyMessage(UPDATE_TITLE);
                        }
                        zoom = zoomNow;

                        // save new values
                        if (moved) {
                            liveChanged = false;

                            long currentTime = System.currentTimeMillis();

                            if (1000 < (currentTime - loadThreadRun)) {
                                centerLatitude = centerLatitudeNow;
                                centerLongitude = centerLongitudeNow;
                                spanLatitude = spanLatitudeNow;
                                spanLongitude = spanLongitudeNow;

                                loadExecutor.execute(new LoadRunnable(centerLatitude, centerLongitude, spanLatitude, spanLongitude));
                            }
                        }
                    }

                    yield();
                } catch (Exception e) {
                    Log.w(Settings.tag, "cgeomap.LoadTimer.run: " + e.toString());
                }
            }
        }

        public boolean isLoading() {
            return loadExecutor.getActiveCount() > 0 ||
                    downloadExecutor.getActiveCount() > 0 ||
                    displayExecutor.getActiveCount() > 0;
        }

    }

    /**
     * Timer triggering every 250 ms to start the {@link Go4CacheRunnable} for displaying user.
     */

    private class Go4CacheTimer extends Thread {

        public Go4CacheTimer() {
            super("Users Timer");
        }

        private volatile boolean stop = false;

        public void stopIt() {
            stop = true;
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    sleep(250);

                    if (mapView != null) {
                        // get current viewport
                        GeoPointImpl mapCenterNow = mapView.getMapViewCenter();
                        int centerLatitudeNow = mapCenterNow.getLatitudeE6();
                        int centerLongitudeNow = mapCenterNow.getLongitudeE6();
                        int spanLatitudeNow = mapView.getLatitudeSpan();
                        int spanLongitudeNow = mapView.getLongitudeSpan();

                        // check if map moved or zoomed
                        boolean moved = false;

                        long currentTime = System.currentTimeMillis();

                        if (60000 < (currentTime - go4CacheThreadRun)) {
                            moved = true;
                        } else if (centerLatitudeUsers == null || centerLongitudeUsers == null) {
                            moved = true;
                        } else if (spanLatitudeUsers == null || spanLongitudeUsers == null) {
                            moved = true;
                        } else if (((Math.abs(spanLatitudeNow - spanLatitudeUsers) > 50) || (Math.abs(spanLongitudeNow - spanLongitudeUsers) > 50) || // changed zoom
                                (Math.abs(centerLatitudeNow - centerLatitudeUsers) > (spanLatitudeNow / 4)) || (Math.abs(centerLongitudeNow - centerLongitudeUsers) > (spanLongitudeNow / 4)) // map moved
                        ) && !Viewport.isInViewPort(centerLatitudeUsers, centerLongitudeUsers, centerLatitudeNow, centerLongitudeNow, spanLatitudeUsers, spanLongitudeUsers, spanLatitudeNow, spanLongitudeNow)) {
                            moved = true;
                        }

                        // save new values
                        if (moved && (1000 < (currentTime - go4CacheThreadRun))) {
                            centerLatitudeUsers = centerLatitudeNow;
                            centerLongitudeUsers = centerLongitudeNow;
                            spanLatitudeUsers = spanLatitudeNow;
                            spanLongitudeUsers = spanLongitudeNow;

                            Go4CacheExecutor.execute(new Go4CacheRunnable(centerLatitudeNow, centerLongitudeNow, spanLatitudeNow, spanLongitudeNow));
                        }
                    }

                    yield();
                } catch (Exception e) {
                    Log.w(Settings.tag, "cgeomap.LoadUsersTimer.run: " + e.toString());
                }
            }
        }
    }

    /**
     * Worker thread that loads caches and waypoints from the database and then spawns the {@link DownloadRunnable}.
     * started by {@link LoadTimer}
     */

    private class LoadRunnable extends DoRunnable {

        public LoadRunnable(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
        }

        @Override
        public void run() {
            try {
                showProgressHandler.sendEmptyMessage(SHOW_PROGRESS);
                loadThreadRun = System.currentTimeMillis();

                // stage 1 - pull and render from the DB only for live map
                if (searchIntent != null || geocodeIntent != null) {
                    // map started from another activity
                    search = new SearchResult(searchIntent);
                    if (geocodeIntent != null) {
                        search.addGeocode(geocodeIntent);
                    }
                } else {
                    // live map
                    if (!live || !Settings.isLiveMap()) {
                        search = new SearchResult(app.getStoredInViewport(centerLat, centerLon, spanLat, spanLon, Settings.getCacheType()));
                    } else {
                        search = new SearchResult(app.getCachedInViewport(centerLat, centerLon, spanLat, spanLon, Settings.getCacheType()));
                    }
                }

                if (search != null) {
                    downloaded = true;
                    caches.addAll(search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_ONLY));
                    waypoints.addAll(app.getWaypointsInViewport(centerLat, centerLon, spanLat, spanLon));
                }

                if (live) {
                    final boolean excludeMine = Settings.isExcludeMyCaches();
                    final boolean excludeDisabled = Settings.isExcludeDisabledCaches();

                    final ArrayList<cgCache> tempList = new ArrayList<cgCache>(caches);
                    for (cgCache cache : tempList) {
                        if ((cache.isFound() && excludeMine) || (cache.isOwn() && excludeMine) || (cache.isDisabled() && excludeDisabled)) {
                            caches.remove(cache);
                        }
                    }
                }

                //render
                displayExecutor.execute(new DisplayRunnable(centerLat, centerLon, spanLat, spanLon));

                if (live && Settings.isLiveMap()) {
                    downloadExecutor.execute(new DownloadRunnable(centerLat, centerLon, spanLat, spanLon));
                }
            } finally {
                showProgressHandler.sendEmptyMessage(HIDE_PROGRESS); // hide progress
            }
        }
    }

    /**
     * Worker thread downloading caches from the internet.
     * Started by {@link LoadRunnable}. Duplicate Code with {@link Go4CacheRunnable}
     */

    private class DownloadRunnable extends DoRunnable {

        public DownloadRunnable(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
        }

        @Override
        public void run() {
            try {
                showProgressHandler.sendEmptyMessage(SHOW_PROGRESS); // show progress

                int count = 0;
                do {

                    if (tokens == null) {
                        tokens = GCBase.getTokens();
                        if (noMapTokenHandler != null && tokens == null) {
                            noMapTokenHandler.sendEmptyMessage(0);
                        }
                    }

                    final Viewport viewport = new Viewport(new Geopoint(centerLat / 1e6, centerLon / 1e6), 0.8 * spanLat / 1e6, 0.8 * spanLon / 1e6);
                    search = ConnectorFactory.searchByViewport(viewport, tokens);
                    if (search != null) {
                        downloaded = true;
                        if (search.getError() == StatusCode.NOT_LOGGED_IN) {
                            Login.login();
                            tokens = null;
                        } else {
                            break;
                        }
                    }
                    count++;

                } while (count < 2);

                if (search != null) {
                    Set<cgCache> result = search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                    // to update the caches they have to be removed first
                    caches.removeAll(result);
                    caches.addAll(result);
                }

                //render
                displayExecutor.execute(new DisplayRunnable(centerLat, centerLon, spanLat, spanLon));

            } catch (ThreadDeath e) {
                Log.d(Settings.tag, "DownloadThread stopped");
                displayHandler.sendEmptyMessage(UPDATE_TITLE);
            } finally {
                showProgressHandler.sendEmptyMessage(HIDE_PROGRESS); // hide progress
            }
        }
    }

    /**
     * Thread to Display (down)loaded caches. Started by {@link LoadRunnable} and {@link DownloadRunnable}
     */
    private class DisplayRunnable extends DoRunnable {

        public DisplayRunnable(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
        }

        @Override
        public void run() {
            try {
                showProgressHandler.sendEmptyMessage(SHOW_PROGRESS);
                if (mapView == null || caches == null) {
                    throw new ThreadDeath();
                }

                // display caches
                final List<cgCache> cachesToDisplay = new ArrayList<cgCache>(caches);
                final List<cgWaypoint> waypointsToDisplay = new ArrayList<cgWaypoint>(waypoints);
                final List<CachesOverlayItemImpl> itemsToDisplay = new ArrayList<CachesOverlayItemImpl>();

                if (!cachesToDisplay.isEmpty()) {
                    // Only show waypoints for single view or setting
                    // when less than showWaypointsthreshold Caches shown
                    if (cachesToDisplay.size() == 1 || (cachesCnt < Settings.getWayPointsThreshold())) {
                        for (cgWaypoint waypoint : waypointsToDisplay) {

                            if (waypoint.getCoords() == null) {
                                continue;
                            }

                            itemsToDisplay.add(getItem(waypoint, null, waypoint));
                        }
                    }
                    for (cgCache cache : cachesToDisplay) {

                        if (cache.getCoords() == null) {
                            continue;
                        }
                        itemsToDisplay.add(getItem(cache, cache, null));
                    }

                    overlayCaches.updateItems(itemsToDisplay);
                    displayHandler.sendEmptyMessage(INVALIDATE_MAP);

                    cachesCnt = cachesToDisplay.size();

                } else {
                    overlayCaches.updateItems(itemsToDisplay);
                    displayHandler.sendEmptyMessage(INVALIDATE_MAP);
                }

                displayHandler.sendEmptyMessage(UPDATE_TITLE);
            } catch (ThreadDeath e) {
                Log.d(Settings.tag, "DisplayThread stopped");
                displayHandler.sendEmptyMessage(UPDATE_TITLE);
            } finally {
                showProgressHandler.sendEmptyMessage(HIDE_PROGRESS);
            }
        }
    }

    /**
     * Thread to load users from Go 4 Cache
     */

    private class Go4CacheRunnable extends DoRunnable {

        public Go4CacheRunnable(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
        }

        @Override
        public void run() {
            final Geopoint center = new Geopoint((int) centerLat, (int) centerLon);
            final Viewport viewport = new Viewport(center, spanLat / 1e6 * 1.5, spanLon / 1e6 * 1.5);

            try {
                go4CacheThreadRun = System.currentTimeMillis();
                List<Go4CacheUser> go4CacheUsers = Go4Cache.getGeocachersInViewport(Settings.getUsername(), viewport);
                go4CacheDisplayExecutor.execute(new Go4CacheDisplayRunnable(go4CacheUsers, centerLat, centerLon, spanLat, spanLon));
            } finally {
            }
        }
    }

    /**
     * Thread to display users of Go 4 Cache started from {@link Go4CacheRunnable}
     */
    private class Go4CacheDisplayRunnable extends DoRunnable {

        private List<Go4CacheUser> users = null;

        public Go4CacheDisplayRunnable(List<Go4CacheUser> usersIn, long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            super(centerLatIn, centerLonIn, spanLatIn, spanLonIn);
            users = usersIn;
        }

        @Override
        public void run() {
            try {
                if (mapView == null || CollectionUtils.isEmpty(users)) {
                    return;
                }

                // display users
                List<OtherCachersOverlayItemImpl> items = new ArrayList<OtherCachersOverlayItemImpl>();

                int counter = 0;
                OtherCachersOverlayItemImpl item = null;

                for (Go4CacheUser userOne : users) {
                    if (userOne.getCoords() == null) {
                        continue;
                    }

                    item = mapProvider.getOtherCachersOverlayItemBase(activity, userOne);
                    items.add(item);

                    counter++;
                    if ((counter % 10) == 0) {
                        overlayGo4Cache.updateItems(items);
                        displayHandler.sendEmptyMessage(INVALIDATE_MAP);
                    }
                }

                overlayGo4Cache.updateItems(items);
            } finally {
            }
        }
    }

    /**
     * Thread to display one point. Started on opening if in single mode.
     */
    private class DisplayPointThread extends Thread {

        @Override
        public void run() {
            if (mapView == null || caches == null) {
                return;
            }

            if (coordsIntent != null) {
                final cgWaypoint waypoint = new cgWaypoint("some place", waypointTypeIntent != null ? waypointTypeIntent : WaypointType.WAYPOINT, false);
                waypoint.setCoords(coordsIntent);

                final CachesOverlayItemImpl item = getItem(waypoint, null, waypoint);
                overlayCaches.updateItems(item);
                displayHandler.sendEmptyMessage(INVALIDATE_MAP);

                cachesCnt = 1;
            } else {
                cachesCnt = 0;
            }

            displayHandler.sendEmptyMessage(UPDATE_TITLE);
        }
    }

    private abstract class DoRunnable implements Runnable {

        protected long centerLat = 0L;
        protected long centerLon = 0L;
        protected long spanLat = 0L;
        protected long spanLon = 0L;

        public DoRunnable(long centerLatIn, long centerLonIn, long spanLatIn, long spanLonIn) {
            centerLat = centerLatIn;
            centerLon = centerLonIn;
            spanLat = spanLatIn;
            spanLon = spanLonIn;
        }

        public abstract void run();
    }

    /**
     * get if map is loading something
     *
     * @return
     */
    private synchronized boolean isLoading() {
        if (loadTimer != null) {
            return loadTimer.isLoading();
        }

        return false;
    }

    /**
     * Thread to store the caches in the viewport. Started by Activity.
     */

    private class LoadDetails extends Thread {

        final private CancellableHandler handler;
        final private List<String> geocodes;
        private long last = 0L;

        public LoadDetails(final CancellableHandler handler, final List<String> geocodes) {
            this.handler = handler;
            this.geocodes = geocodes;
        }

        public void stopIt() {
            handler.cancel();
        }

        @Override
        public void run() {
            if (CollectionUtils.isEmpty(geocodes)) {
                return;
            }

            if (dir != null) {
                dir = app.removeDir();
            }
            if (geo != null) {
                geo = app.removeGeo();
            }

            for (String geocode : geocodes) {
                try {
                    if (handler.isCancelled()) {
                        break;
                    }

                    if (!app.isOffline(geocode, null)) {
                        if ((System.currentTimeMillis() - last) < 1500) {
                            try {
                                int delay = 1000 + (int) (Math.random() * 1000.0) - (int) (System.currentTimeMillis() - last);
                                if (delay < 0) {
                                    delay = 500;
                                }

                                sleep(delay);
                            } catch (Exception e) {
                                // nothing
                            }
                        }

                        if (handler.isCancelled()) {
                            Log.i(Settings.tag, "Stopped storing process.");

                            break;
                        }

                        cgBase.storeCache(activity, null, geocode, StoredList.STANDARD_LIST_ID, false, handler);
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgeocaches.LoadDetails.run: " + e.toString());
                } finally {
                    // one more cache over
                    detailProgress++;
                    handler.sendEmptyMessage(UPDATE_PROGRESS);
                }

                // FIXME: what does this yield() do here?
                yield();

                last = System.currentTimeMillis();
            }

            // we're done
            handler.sendEmptyMessage(FINISHED_LOADING_DETAILS);
        }
    }

    // center map to desired location
    private void centerMap(final Geopoint coords) {
        if (coords == null) {
            return;
        }
        if (mapView == null) {
            return;
        }

        if (!alreadyCentered) {
            alreadyCentered = true;

            mapController.setCenter(makeGeoPoint(coords));
        } else {
            mapController.animateTo(makeGeoPoint(coords));
        }
    }

    // move map to view results of searchIntent
    private void centerMap(String geocodeCenter, final SearchResult searchCenter, final Geopoint coordsCenter, int[] mapState) {

        if (!centered && mapState != null) {
            try {
                mapController.setCenter(mapProvider.getGeoPointBase(new Geopoint(mapState[0] / 1.0e6, mapState[1] / 1.0e6)));
                mapController.setZoom(mapState[2]);
            } catch (Exception e) {
                // nothing at all
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && (geocodeCenter != null || searchIntent != null)) {
            try {
                List<Number> viewport = null;

                if (geocodeCenter != null) {
                    viewport = app.getBounds(geocodeCenter);
                } else {
                    if (searchCenter != null) {
                        viewport = app.getBounds(searchCenter.getGeocodes());
                    }
                }

                if (viewport == null || viewport.size() < 5) {
                    return;
                }

                int cnt = (Integer) viewport.get(0);
                if (cnt <= 0) {
                    return;
                }
                int minLat = (int) ((Double) viewport.get(1) * 1e6);
                int maxLat = (int) ((Double) viewport.get(2) * 1e6);
                int maxLon = (int) ((Double) viewport.get(3) * 1e6);
                int minLon = (int) ((Double) viewport.get(4) * 1e6);

                int centerLat = 0;
                int centerLon = 0;

                if ((Math.abs(maxLat) - Math.abs(minLat)) != 0) {
                    centerLat = minLat + ((maxLat - minLat) / 2);
                } else {
                    centerLat = maxLat;
                }
                if ((Math.abs(maxLon) - Math.abs(minLon)) != 0) {
                    centerLon = minLon + ((maxLon - minLon) / 2);
                } else {
                    centerLon = maxLon;
                }

                mapController.setCenter(mapProvider.getGeoPointBase(new Geopoint(centerLat, centerLon)));
                if (Math.abs(maxLat - minLat) != 0 && Math.abs(maxLon - minLon) != 0) {
                    mapController.zoomToSpan(Math.abs(maxLat - minLat), Math.abs(maxLon - minLon));
                }
            } catch (Exception e) {
                // nothing at all
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && coordsCenter != null) {
            try {
                mapController.setCenter(makeGeoPoint(coordsCenter));
            } catch (Exception e) {
                // nothing at all
            }

            centered = true;
            alreadyCentered = true;
        }
    }

    // switch My Location button image
    private void switchMyLocationButton() {
        if (followMyLocation) {
            myLocSwitch.setImageResource(R.drawable.actionbar_mylocation_on);
            myLocationInMiddle();
        } else {
            myLocSwitch.setImageResource(R.drawable.actionbar_mylocation_off);
        }
    }

    // set my location listener
    private class MyLocationListener implements View.OnClickListener {
        public void onClick(View view) {
            followMyLocation = !followMyLocation;
            switchMyLocationButton();
        }
    }

    @Override
    public void onDrag() {
        if (followMyLocation) {
            followMyLocation = false;
            switchMyLocationButton();
        }
    }

    // make geopoint
    private GeoPointImpl makeGeoPoint(final Geopoint coords) {
        return mapProvider.getGeoPointBase(coords);
    }

    // close activity and open homescreen
    @Override
    public void goHome(View view) {
        ActivityMixin.goHome(activity);
    }

    // open manual entry
    @Override
    public void goManual(View view) {
        ActivityMixin.goManual(activity, "c:geo-live-map");
    }

    @Override
    public View makeView() {
        ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ScaleType.CENTER);
        imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        return imageView;
    }

    private static Intent newIntent(final Context context) {
        return new Intent(context, Settings.getMapProvider().getMapClass());
    }

    public static void startActivitySearch(final Activity fromActivity, final SearchResult search, final String title) {
        final Intent mapIntent = newIntent(fromActivity);
        mapIntent.putExtra(EXTRAS_SEARCH, search);
        if (StringUtils.isNotBlank(title)) {
            mapIntent.putExtra(CGeoMap.EXTRAS_MAP_TITLE, title);
        }
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivityLiveMap(final Activity fromActivity) {
        fromActivity.startActivity(newIntent(fromActivity));
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords, final WaypointType type, final String title) {
        final Intent mapIntent = newIntent(fromActivity);
        mapIntent.putExtra(EXTRAS_LATITUDE, coords.getLatitude());
        mapIntent.putExtra(EXTRAS_LONGITUDE, coords.getLongitude());
        if (type != null) {
            mapIntent.putExtra(EXTRAS_WPTTYPE, type.id);
        }
        if (StringUtils.isNotBlank(title)) {
            mapIntent.putExtra(EXTRAS_MAP_TITLE, title);
        }
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivityGeoCode(final Activity fromActivity, final String geocode) {
        final Intent mapIntent = newIntent(fromActivity);
        mapIntent.putExtra(EXTRAS_GEOCODE, geocode);
        mapIntent.putExtra(EXTRAS_MAP_TITLE, geocode);
        fromActivity.startActivity(mapIntent);
    }

    public static void markCacheAsDirty(final String geocode) {
        if (dirtyCaches == null) {
            dirtyCaches = new HashSet<String>();
        }
        dirtyCaches.add(geocode);
    }

    /**
     * Returns a OverlayItem represented by an icon
     *
     * @param coord
     *            The coords
     * @param cache
     *            Cache
     * @param waypoint
     *            Waypoint. Mutally exclusive with cache
     * @return
     */
    private CachesOverlayItemImpl getItem(IWaypoint coord, cgCache cache, cgWaypoint waypoint) {
        if (cache != null) {

            CachesOverlayItemImpl item = mapProvider.getCachesOverlayItem(coord, cache.getType());

            int hashcode = new HashCodeBuilder()
                .append(cache.isReliableLatLon())
                .append(cache.getType().id)
                    .append(cache.isDisabled() || cache.isArchived())
                .append(cache.isOwn())
                .append(cache.isFound())
                .append(cache.hasUserModifiedCoords())
                .append(cache.getPersonalNote())
                    .append(cache.isLogOffline())
                    .append(cache.getListId() > 0)
                .toHashCode();

            LayerDrawable ldFromCache = CGeoMap.overlaysCache.get(hashcode);
            if (ldFromCache != null) {
                item.setMarker(ldFromCache);
                return item;
            }

            ArrayList<Drawable> layers = new ArrayList<Drawable>();
            ArrayList<int[]> insets = new ArrayList<int[]>();


            // background: disabled or not
            Drawable marker = getResources().getDrawable(R.drawable.marker);
            if (cache.isDisabled() || cache.isArchived()) {
                marker = getResources().getDrawable(R.drawable.marker_disabled);
            }
            layers.add(marker);
            int resolution = marker.getIntrinsicWidth() > 40 ? 1 : 0;
            // reliable or not
            if (!cache.isReliableLatLon()) {
                insets.add(INSET_RELIABLE[resolution]);
                layers.add(getResources().getDrawable(R.drawable.marker_notreliable));
            }
            // cache type
            layers.add(getResources().getDrawable(cache.getType().markerId));
            insets.add(INSET_TYPE[resolution]);
            // own
            if ( cache.isOwn() ) {
                layers.add(getResources().getDrawable(R.drawable.marker_own));
                insets.add(INSET_OWN[resolution]);
                // if not, checked if stored
            } else if (cache.getListId() > 0) {
                layers.add(getResources().getDrawable(R.drawable.marker_stored));
                insets.add(INSET_OWN[resolution]);
            }
            // found
            if (cache.isFound()) {
                layers.add(getResources().getDrawable(R.drawable.marker_found));
                insets.add(INSET_FOUND[resolution]);
                // if not, perhaps logged offline
            } else if (cache.isLogOffline()) {
                layers.add(getResources().getDrawable(R.drawable.marker_found_offline));
                insets.add(INSET_FOUND[resolution]);
            }
            // user modified coords
            if (cache.hasUserModifiedCoords()) {
                layers.add(getResources().getDrawable(R.drawable.marker_usermodifiedcoords));
                insets.add(INSET_USERMODIFIEDCOORDS[resolution]);
            }
            // personal note
            if (cache.getPersonalNote() != null) {
                layers.add(getResources().getDrawable(R.drawable.marker_personalnote));
                insets.add(INSET_PERSONALNOTE[resolution]);
            }


            LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

            int index = 1;
            for ( int[] inset : insets) {
                ld.setLayerInset(index++, inset[0], inset[1], inset[2], inset[3]);
            }

            CGeoMap.overlaysCache.put(hashcode, ld);

            item.setMarker(ld);
            return item;

        } else if (waypoint != null) {

            CachesOverlayItemImpl item = mapProvider.getCachesOverlayItem(coord, null);
            Drawable[] layers = new Drawable[2];
            layers[0] = getResources().getDrawable(R.drawable.marker);
            layers[1] = getResources().getDrawable(waypoint.getWaypointType().markerId);

            LayerDrawable ld = new LayerDrawable(layers);
            if (layers[0].getIntrinsicWidth() > 40) {
                ld.setLayerInset(1, 9, 12, 10, 13);
            } else {
                ld.setLayerInset(1, 9, 12, 8, 12);
            }
            item.setMarker(ld);
            return item;
        }

        return null;

    }

}
