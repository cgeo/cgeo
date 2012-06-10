package cgeo.geocaching.maps;

import cgeo.geocaching.DirectionProvider;
import cgeo.geocaching.IGeoData;
import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.LiveMapInfo;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.cgeocaches;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.Login;
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
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.GeoDirHandler;
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
import android.util.SparseArray;
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
import java.util.HashSet;
import java.util.List;
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

    /**Controls the behaviour of the map*/
    public enum MapMode {
        /** Live Map where caches are loaded from online */
        LIVE_ONLINE,
        /** Live Map where caches are loaded only from database */
        LIVE_OFFLINE,
        /** Map around some coordinates */
        COORDS,
        /** Map with a single cache (no reload on move) */
        SINGLE,
        /** Map with a list of caches (no reload on move) */
        LIST
    }
    /** Handler Messages */
    private static final int HIDE_PROGRESS = 0;
    private static final int SHOW_PROGRESS = 1;
    private static final int UPDATE_TITLE = 0;
    private static final int INVALIDATE_MAP = 1;
    private static final int UPDATE_PROGRESS = 0;
    private static final int FINISHED_LOADING_DETAILS = 1;

    //Menu
    private static final String EXTRAS_GEOCODE = "geocode";
    private static final String EXTRAS_COORDS = "coords";
    private static final String EXTRAS_WPTTYPE = "wpttype";
    private static final String EXTRAS_MAPSTATE = "mapstate";
    private static final String EXTRAS_SEARCH = "search";
    private static final String EXTRAS_MAP_MODE = "map_mode";

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

    private static final String BUNDLE_MAP_SOURCE = "mapSource";
    private static final String BUNDLE_MAP_STATE = "mapState";

    private Resources res = null;
    private MapItemFactory mapItemFactory = null;
    private Activity activity = null;
    private MapViewImpl mapView = null;
    private cgeoapplication app = null;
    final private GeoDirHandler geoDirUpdate = new UpdateLoc();
    private SearchResult searchIntent = null;
    private String geocodeIntent = null;
    private Geopoint coordsIntent = null;
    private WaypointType waypointTypeIntent = null;
    private int[] mapStateIntent = null;
    // status data
    /** Last search result used for displaying header */
    private SearchResult lastSearchResult = null;
    private String[] tokens = null;
    private boolean noMapTokenShowed = false;
    // map status data
    private boolean followMyLocation = false;
    private Viewport viewport = null;
    private Viewport viewportUsers = null;
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

    private static SparseArray<LayerDrawable> overlaysCache = new SparseArray<LayerDrawable>();
    private int cachesCnt = 0;
    /** List of caches in the viewport */
    private LeastRecentlyUsedSet<cgCache> caches = null;
    /** List of waypoints in the viewport */
    private final LeastRecentlyUsedSet<cgWaypoint> waypoints = new LeastRecentlyUsedSet<cgWaypoint>(MAX_CACHES);
    // storing for offline
    private ProgressDialog waitDialog = null;
    private int detailTotal = 0;
    private int detailProgress = 0;
    private long detailProgressTime = 0L;
    // views
    private ImageSwitcher myLocSwitch = null;

    /**Controls the map behaviour*/
    private MapMode mapMode = null;
    // other things
    //    private boolean live = true; // live map (live, dead) or rest (displaying caches on map)
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

                    if (mapMode == MapMode.LIVE_ONLINE) {
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

                    if (Settings.isDebug() && lastSearchResult != null && StringUtils.isNotBlank(lastSearchResult.getUrl())) {
                        title.append('[').append(lastSearchResult.getUrl()).append(']');
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

                geoDirUpdate.startDir();
            }
        }

        @Override
        public void handleCancel(final Object extra) {
            if (loadDetailsThread != null) {
                loadDetailsThread.stopIt();
            }

            geoDirUpdate.startDir();
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

    /* Current source id */
    private int currentSourceId;

    public CGeoMap(MapActivityImpl activity) {
        super(activity);
    }

    protected void countVisibleCaches() {
        final List<cgCache> protectedCaches = caches.getAsList();

        int count = 0;
        if (protectedCaches.size() > 0) {
            final Viewport viewport = mapView.getViewport();

            for (final cgCache cache : protectedCaches) {
                if (cache != null && cache.getCoords() != null) {
                    if (viewport.contains(cache)) {
                        count++;
                    }
                }
            }
        }
        cachesCnt = count;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putInt(BUNDLE_MAP_SOURCE, currentSourceId);
        outState.putIntArray(BUNDLE_MAP_STATE, currentMapState());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // class init
        res = this.getResources();
        activity = this.getActivity();
        app = (cgeoapplication) activity.getApplication();

        int countBubbleCnt = app.getAllStoredCachesCount(true, CacheType.ALL);
        caches = new LeastRecentlyUsedSet<cgCache>(MAX_CACHES + countBubbleCnt);

        final MapProvider mapProvider = Settings.getMapProvider();
        mapItemFactory = mapProvider.getMapItemFactory();

        // Get parameters from the intent
        final Bundle extras = activity.getIntent().getExtras();
        if (extras != null) {
            mapMode = (MapMode) extras.get(EXTRAS_MAP_MODE);
            searchIntent = (SearchResult) extras.getParcelable(EXTRAS_SEARCH);
            geocodeIntent = extras.getString(EXTRAS_GEOCODE);
            coordsIntent = (Geopoint) extras.getParcelable(EXTRAS_COORDS);
            waypointTypeIntent = WaypointType.findById(extras.getString(EXTRAS_WPTTYPE));
            mapStateIntent = extras.getIntArray(EXTRAS_MAPSTATE);
            mapTitle = extras.getString(EXTRAS_MAP_TITLE);
        }
        else {
            mapMode = Settings.isLiveMap() ? MapMode.LIVE_ONLINE : MapMode.LIVE_OFFLINE;
        }
        if (StringUtils.isBlank(mapTitle)) {
            mapTitle = res.getString(R.string.map_map);
        }

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            currentSourceId = savedInstanceState.getInt(BUNDLE_MAP_SOURCE, Settings.getMapSource());
            mapStateIntent = savedInstanceState.getIntArray(BUNDLE_MAP_STATE);
        } else {
            currentSourceId = Settings.getMapSource();
        }

        // If recreating from an obsolete map source, we may need a restart
        if (changeMapSource(Settings.getMapSource())) {
            return;
        }

        // reset status
        noMapTokenShowed = false;

        ActivityMixin.keepScreenOn(activity, true);

        // set layout
        ActivityMixin.setTheme(activity);
        activity.setContentView(mapProvider.getMapLayoutId());
        ActivityMixin.setTitle(activity, res.getString(R.string.map_map));

        // initialize map
        mapView = (MapViewImpl) activity.findViewById(mapProvider.getMapViewId());
        mapView.setMapSource();
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);
        mapView.preLoad();
        mapView.setOnDragListener(this);

        // initialize overlays
        mapView.clearOverlays();

        if (Settings.isPublicLoc() && overlayGo4Cache == null) {
            overlayGo4Cache = mapView.createAddUsersOverlay(activity, getResources().getDrawable(R.drawable.user_location));
        }

        if (overlayCaches == null) {
            overlayCaches = mapView.createAddMapOverlay(mapView.getContext(), getResources().getDrawable(R.drawable.marker));
        }

        if (overlayPosition == null) {
            overlayPosition = mapView.createAddPositionOverlay(activity);
        }

        if (overlayScale == null) {
            overlayScale = mapView.createAddScaleOverlay(activity);
        }

        mapView.repaintRequired(null);

        mapView.getMapController().setZoom(Settings.getMapZoom());

        if (null == mapStateIntent) {
            followMyLocation = mapMode == MapMode.LIVE_OFFLINE || mapMode == MapMode.LIVE_ONLINE;
        } else {
            followMyLocation = 1 == mapStateIntent[3];
            if ((overlayCaches.getCircles() ? 1 : 0) != mapStateIntent[4]) {
                overlayCaches.switchCircles();
            }
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

        if (!app.isLiveMapHintShown() && !Settings.getHideLiveMapHint()) {
            Intent hintIntent = new Intent(activity, LiveMapInfo.class);
            activity.startActivity(hintIntent);
            app.setLiveMapHintShown();
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

        addGeoDirObservers();

        if (!CollectionUtils.isEmpty(dirtyCaches)) {
            for (String geocode : dirtyCaches) {
                cgCache cache = app.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                // remove to update the cache
                caches.remove(cache);
                caches.add(cache);
            }
            dirtyCaches.clear();
            // Update display
            displayExecutor.execute(new DisplayRunnable(mapView.getViewport()));
        }

        startTimer();
    }

    private void addGeoDirObservers() {
        geoDirUpdate.startGeoAndDir();
    }

    private void deleteGeoDirObservers() {
        geoDirUpdate.stopGeoAndDir();
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

        deleteGeoDirObservers();

        savePrefs();

        if (mapView != null) {
            mapView.destroyDrawingCache();
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        SubMenu submenu = menu.addSubMenu(1, MENU_SELECT_MAPVIEW, 0, res.getString(R.string.map_view_map)).setIcon(R.drawable.ic_menu_mapmode);
        addMapViewMenuItems(submenu);

        menu.add(0, MENU_MAP_LIVE, 0, res.getString(R.string.map_live_disable)).setIcon(R.drawable.ic_menu_refresh);
        menu.add(0, MENU_STORE_CACHES, 0, res.getString(R.string.caches_store_offline)).setIcon(R.drawable.ic_menu_set_as).setEnabled(false);
        menu.add(0, MENU_TRAIL_MODE, 0, res.getString(R.string.map_trail_hide)).setIcon(R.drawable.ic_menu_trail);

        Strategy strategy = Settings.getLiveMapStrategy();
        SubMenu subMenuStrategy = menu.addSubMenu(0, SUBMENU_STRATEGY, 0, res.getString(R.string.map_strategy)).setIcon(R.drawable.ic_menu_preferences);
        subMenuStrategy.setHeaderTitle(res.getString(R.string.map_strategy_title));
        subMenuStrategy.add(2, MENU_STRATEGY_FASTEST, 0, Strategy.FASTEST.getL10n()).setCheckable(true).setChecked(strategy == Strategy.FASTEST);
        subMenuStrategy.add(2, MENU_STRATEGY_FAST, 0, Strategy.FAST.getL10n()).setCheckable(true).setChecked(strategy == Strategy.FAST);
        subMenuStrategy.add(2, MENU_STRATEGY_AUTO, 0, Strategy.AUTO.getL10n()).setCheckable(true).setChecked(strategy == Strategy.AUTO);
        subMenuStrategy.add(2, MENU_STRATEGY_DETAILED, 0, Strategy.DETAILED.getL10n()).setCheckable(true).setChecked(strategy == Strategy.DETAILED);
        subMenuStrategy.setGroupCheckable(2, true, true);

        menu.add(0, MENU_CIRCLE_MODE, 0, res.getString(R.string.map_circles_hide)).setIcon(R.drawable.ic_menu_circle);
        menu.add(0, MENU_AS_LIST, 0, res.getString(R.string.map_as_list)).setIcon(R.drawable.ic_menu_agenda);

        return true;
    }

    private static void addMapViewMenuItems(final Menu menu) {
        MapProviderFactory.addMapviewMenuItems(menu, 1, Settings.getMapSource());
        menu.setGroupCheckable(1, true, true);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (Integer mapSourceId : MapProviderFactory.getMapSources().keySet()) {
            final MenuItem menuItem = menu.findItem(mapSourceId);
            if (menuItem != null) {
                final MapSource mapSource = MapProviderFactory.getMapSource(mapSourceId);
                if (mapSource != null) {
                    menuItem.setEnabled(mapSource.isAvailable());
                }
            }
        }

        MenuItem item;
        try {
            item = menu.findItem(MENU_TRAIL_MODE); // show trail
            if (Settings.isMapTrail()) {
                item.setTitle(res.getString(R.string.map_trail_hide));
            } else {
                item.setTitle(res.getString(R.string.map_trail_show));
            }

            item = menu.findItem(MENU_MAP_LIVE); // live map
            if (mapMode == MapMode.LIVE_ONLINE) {
                item.setTitle(res.getString(R.string.map_live_disable));
            } else if (mapMode == MapMode.LIVE_OFFLINE) {
                item.setTitle(res.getString(R.string.map_live_enable));
            } else {
                item.setEnabled(false);
                item.setTitle(res.getString(R.string.map_live_enable));
            }

            final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
            menu.findItem(MENU_STORE_CACHES).setEnabled(isLiveMode() && !isLoading() && CollectionUtils.isNotEmpty(geocodesInViewport) && app.hasUnsavedCaches(new SearchResult(geocodesInViewport)));

            item = menu.findItem(MENU_CIRCLE_MODE); // show circles
            if (overlayCaches != null && overlayCaches.getCircles()) {
                item.setTitle(res.getString(R.string.map_circles_hide));
            } else {
                item.setTitle(res.getString(R.string.map_circles_show));
            }

            item = menu.findItem(MENU_AS_LIST);
            item.setEnabled(isLiveMode() && !isLoading());

            menu.findItem(SUBMENU_STRATEGY).setEnabled(isLiveMode());
        } catch (Exception e) {
            Log.e("cgeomap.onPrepareOptionsMenu: " + e.toString());
        }

        return true;
    }

    private boolean isLiveMode() {
        return mapMode == MapMode.LIVE_OFFLINE || mapMode == MapMode.LIVE_ONLINE;
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
                mapMode = Settings.isLiveMap() ? MapMode.LIVE_ONLINE : MapMode.LIVE_OFFLINE;
                liveChanged = true;
                lastSearchResult = null;
                searchIntent = null;
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case MENU_STORE_CACHES:
                if (!isLoading()) {
                    final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
                    final List<String> geocodes = new ArrayList<String>();

                    for (final String geocode : geocodesInViewport) {
                        if (!app.isOffline(geocode, null)) {
                            geocodes.add(geocode);
                        }
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

                        @Override
                        public void onCancel(DialogInterface arg0) {
                            try {
                                if (loadDetailsThread != null) {
                                    loadDetailsThread.stopIt();
                                }

                                geoDirUpdate.startDir();
                            } catch (Exception e) {
                                Log.e("cgeocaches.onPrepareOptionsMenu.onCancel: " + e.toString());
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
                cgeocaches.startActivityMap(activity, new SearchResult(getGeocodesForCachesInViewport()));
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
                int mapSource = MapProviderFactory.getMapSourceFromMenuId(id);
                if (MapProviderFactory.isValidSourceId(mapSource)) {
                    item.setChecked(true);

                    changeMapSource(mapSource);

                    return true;
                }
        }
        return false;
    }

    /**
     * @return a Set of geocodes corresponding to the caches that are shown on screen.
     */
    private Set<String> getGeocodesForCachesInViewport() {
        final Set<String> geocodes = new HashSet<String>();
        final List<cgCache> cachesProtected = caches.getAsList();

        final Viewport viewport = mapView.getViewport();

        for (final cgCache cache : cachesProtected) {
            if (viewport.contains(cache)) {
                geocodes.add(cache.getGeocode());
            }
        }
        return geocodes;
    }

    /**
     * Restart the current activity if the map provider has changed, or change the map source if needed.
     *
     * @param mapSource
     *            the new map source, which can be the same as the current one
     * @return true if a restart is needed, false otherwise
     */
    private boolean changeMapSource(final int mapSource) {
        // If the current or the requested map source is invalid, request the first available map source instead
        // and restart the activity.
        if (!MapProviderFactory.isValidSourceId(mapSource)) {
            Log.e("CGeoMap.onCreate: invalid map source requested: " + mapSource);
            currentSourceId = MapProviderFactory.getSourceIdFromOrdinal(0);
            Settings.setMapSource(currentSourceId);
            mapRestart();
            return true;
        }

        final boolean restartRequired = !MapProviderFactory.isSameActivity(currentSourceId, mapSource);

        Settings.setMapSource(mapSource);
        currentSourceId = mapSource;

        if (restartRequired) {
            mapRestart();
        } else if (mapView != null) {
            mapView.setMapSource();
        }

        return restartRequired;
    }

    /**
     * Restart the current activity with the default map source.
     */
    private void mapRestart() {
        // close old mapview
        activity.finish();

        // prepare information to restart a similar view
        Intent mapIntent = new Intent(activity, Settings.getMapProvider().getMapClass());

        mapIntent.putExtra(EXTRAS_SEARCH, searchIntent);
        mapIntent.putExtra(EXTRAS_GEOCODE, geocodeIntent);
        if (coordsIntent != null) {
            mapIntent.putExtra(EXTRAS_COORDS, coordsIntent);
        }
        mapIntent.putExtra(EXTRAS_WPTTYPE, waypointTypeIntent != null ? waypointTypeIntent.id : null);
        mapIntent.putExtra(EXTRAS_MAP_TITLE, mapTitle);
        mapIntent.putExtra(EXTRAS_MAP_MODE, mapMode);

        final int[] mapState = currentMapState();
        if (mapState != null) {
            mapIntent.putExtra(EXTRAS_MAPSTATE, mapState);
        }

        // start the new map
        activity.startActivity(mapIntent);
    }

    /**
     * Get the current map state from the map view if it exists or from the mapStateIntent field otherwise.
     *
     * @return the current map state as an array of int, or null if no map state is available
     */
    private int[] currentMapState() {
        if (mapView == null) {
            return mapStateIntent;
        }

        final GeoPointImpl mapCenter = mapView.getMapViewCenter();
        return new int[] {
                mapCenter.getLatitudeE6(),
                mapCenter.getLongitudeE6(),
                mapView.getMapZoomLevel(),
                followMyLocation ? 1 : 0,
                overlayCaches.getCircles() ? 1 : 0
        };
    }

    private void savePrefs() {
        if (mapView == null) {
            return;
        }

        Settings.setMapZoom(mapView.getMapZoomLevel());
    }

    // Set center of map to my location if appropriate.
    private void myLocationInMiddle(final IGeoData geo) {
        if (followMyLocation) {
            centerMap(geo.getCoords());
        }
    }

    // class: update location
    private class UpdateLoc extends GeoDirHandler {

        @Override
        protected void updateGeoData(final IGeoData geo) {
            try {
                boolean repaintRequired = false;

                if (overlayPosition == null && mapView != null) {
                    overlayPosition = mapView.createAddPositionOverlay(activity);
                }

                if ((overlayPosition != null && geo.getLocation() != null)) {
                    overlayPosition.setCoordinates(geo.getLocation());
                }

                if (geo.getCoords() != null) {
                    if (followMyLocation) {
                        myLocationInMiddle(geo);
                    } else {
                        repaintRequired = true;
                    }
                }

                if (!Settings.isUseCompass() || geo.getSpeed() > 5) { // use GPS when speed is higher than 18 km/h
                    overlayPosition.setHeading(geo.getBearing());
                    repaintRequired = true;
                }

                if (repaintRequired && mapView != null) {
                    mapView.repaintRequired(overlayPosition);
                }

            } catch (Exception e) {
                Log.w("Failed to update location.");
            }
        }

        @Override
        public void updateDirection(final float direction) {
            if (overlayPosition != null && mapView != null && (app.currentGeo().getSpeed() <= 5)) { // use compass when speed is lower than 18 km/h
                overlayPosition.setHeading(DirectionProvider.getDirectionNow(activity, direction));
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
                        final Viewport viewportNow = mapView.getViewport();
                        // Since zoomNow is used only for local comparison purposes,
                        // it is ok to use the Google Maps compatible zoom level of OSM Maps
                        final int zoomNow = mapView.getMapZoomLevel();

                        // check if map moved or zoomed
                        //TODO Portree Use Rectangle inside with bigger search window. That will stop reloading on every move
                        boolean moved = false;

                        if (liveChanged) {
                            moved = true;
                        } else if (mapMode == MapMode.LIVE_ONLINE && !downloaded) {
                            moved = true;
                        } else if (viewport == null) {
                            moved = true;
                        } else if (zoomNow != zoom) {
                            moved = true;
                        } else if (mapMoved(viewport, viewportNow) && (cachesCnt <= 0 || CollectionUtils.isEmpty(caches) || !viewport.includes(viewportNow))) {
                            moved = true;
                        }

                        // update title on any change
                        if (moved || zoomNow != zoom || !viewportNow.equals(viewport)) {
                            displayHandler.sendEmptyMessage(UPDATE_TITLE);
                        }
                        zoom = zoomNow;

                        // save new values
                        if (moved) {
                            liveChanged = false;

                            long currentTime = System.currentTimeMillis();

                            if (1000 < (currentTime - loadThreadRun)) {
                                viewport = viewportNow;
                                loadExecutor.execute(new LoadRunnable(viewport));
                            }
                        }
                    }

                    yield();
                } catch (Exception e) {
                    Log.w("cgeomap.LoadTimer.run: " + e.toString());
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
                        final Viewport viewportNow = mapView.getViewport();

                        // check if map moved or zoomed
                        boolean moved = false;

                        long currentTime = System.currentTimeMillis();

                        if (60000 < (currentTime - go4CacheThreadRun)) {
                            moved = true;
                        } else if (viewportUsers == null) {
                            moved = true;
                        } else if (mapMoved(viewportUsers, viewportNow) && !viewportUsers.includes(viewportNow)) {
                            moved = true;
                        }

                        // save new values
                        if (moved && (1000 < (currentTime - go4CacheThreadRun))) {
                            viewportUsers = viewportNow;
                            Go4CacheExecutor.execute(new Go4CacheRunnable(viewportUsers));
                        }
                    }

                    yield();
                } catch (Exception e) {
                    Log.w("cgeomap.LoadUsersTimer.run: " + e.toString());
                }
            }
        }
    }

    /**
     * Worker thread that loads caches and waypoints from the database and then spawns the {@link DownloadRunnable}.
     * started by {@link LoadTimer}
     */

    private class LoadRunnable extends DoRunnable {

        public LoadRunnable(final Viewport viewport) {
            super(viewport);
        }

        @Override
        public void run() {
            try {
                showProgressHandler.sendEmptyMessage(SHOW_PROGRESS);
                loadThreadRun = System.currentTimeMillis();

                SearchResult searchResult = null;
                // stage 1 - pull and render from the DB only for live map
                if (mapMode == MapMode.LIVE_ONLINE) {
                    searchResult = new SearchResult(app.getCachedInViewport(viewport, Settings.getCacheType()));
                } else if (mapMode == MapMode.LIVE_OFFLINE) {
                    searchResult = new SearchResult(app.getStoredInViewport(viewport, Settings.getCacheType()));
                } else {
                    // map started from another activity
                    searchResult = new SearchResult(searchIntent);
                    if (geocodeIntent != null) {
                        searchResult.addGeocode(geocodeIntent);
                    }
                }

                downloaded = true;
                Set<cgCache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);
                // to update the caches they have to be removed first
                caches.removeAll(cachesFromSearchResult);
                caches.addAll(cachesFromSearchResult);

                if (isLiveMode()) {
                    final boolean excludeMine = Settings.isExcludeMyCaches();
                    final boolean excludeDisabled = Settings.isExcludeDisabledCaches();

                    final List<cgCache> tempList = caches.getAsList();

                    for (cgCache cache : tempList) {
                        if ((cache.isFound() && excludeMine) || (cache.isOwn() && excludeMine) || (cache.isDisabled() && excludeDisabled)) {
                            caches.remove(cache);
                        }
                    }
                }
                countVisibleCaches();
                if (cachesCnt < Settings.getWayPointsThreshold() || geocodeIntent != null) {
                    waypoints.clear();
                    if (isLiveMode() || mapMode == MapMode.COORDS) {
                        //All visible waypoints
                        //FIXME apply type filter
                        waypoints.addAll(app.getWaypointsInViewport(viewport, Settings.isExcludeMyCaches(), Settings.isExcludeDisabledCaches()));
                    } else {
                        //All waypoints from the viewed caches
                        for (cgCache c : caches.getAsList()) {
                            waypoints.addAll(c.getWaypoints());
                        }
                    }
                }

                //render
                displayExecutor.execute(new DisplayRunnable(viewport));

                if (mapMode == MapMode.LIVE_ONLINE) {
                    downloadExecutor.execute(new DownloadRunnable(viewport));
                }
                lastSearchResult = searchResult;
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

        public DownloadRunnable(final Viewport viewport) {
            super(viewport);
        }

        @Override
        public void run() {
            try {
                showProgressHandler.sendEmptyMessage(SHOW_PROGRESS); // show progress

                int count = 0;
                SearchResult searchResult = null;
                do {

                    if (tokens == null) {
                        tokens = Login.getMapTokens();
                        if (noMapTokenHandler != null && tokens == null) {
                            noMapTokenHandler.sendEmptyMessage(0);
                        }
                    }

                    searchResult = ConnectorFactory.searchByViewport(viewport.resize(0.8), tokens);
                    if (searchResult != null) {
                        downloaded = true;
                        if (searchResult.getError() == StatusCode.NOT_LOGGED_IN) {
                            Login.login();
                            tokens = null;
                        } else {
                            break;
                        }
                    }
                    count++;

                } while (count < 2);

                if (searchResult != null) {
                    Set<cgCache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                    // to update the caches they have to be removed first
                    caches.removeAll(result);
                    caches.addAll(result);
                    lastSearchResult = searchResult;
                }

                //render
                displayExecutor.execute(new DisplayRunnable(viewport));

            } catch (ThreadDeath e) {
                Log.d("DownloadThread stopped");
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

        public DisplayRunnable(final Viewport viewport) {
            super(viewport);
        }

        @Override
        public void run() {
            try {
                showProgressHandler.sendEmptyMessage(SHOW_PROGRESS);
                if (mapView == null || caches == null) {
                    throw new ThreadDeath();
                }

                // display caches
                final List<cgCache> cachesToDisplay = caches.getAsList();
                final List<cgWaypoint> waypointsToDisplay = new ArrayList<cgWaypoint>(waypoints);
                final List<CachesOverlayItemImpl> itemsToDisplay = new ArrayList<CachesOverlayItemImpl>();

                if (!cachesToDisplay.isEmpty()) {
                    // Only show waypoints for single view or setting
                    // when less than showWaypointsthreshold Caches shown
                    if (mapMode == MapMode.SINGLE || (cachesCnt < Settings.getWayPointsThreshold())) {
                        for (cgWaypoint waypoint : waypointsToDisplay) {

                            if (waypoint == null || waypoint.getCoords() == null) {
                                continue;
                            }

                            itemsToDisplay.add(getItem(waypoint, null, waypoint));
                        }
                    }
                    for (cgCache cache : cachesToDisplay) {

                        if (cache == null || cache.getCoords() == null) {
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
                Log.d("DisplayThread stopped");
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

        public Go4CacheRunnable(final Viewport viewport) {
            super(viewport);
        }

        @Override
        public void run() {
            go4CacheThreadRun = System.currentTimeMillis();
            List<Go4CacheUser> go4CacheUsers = Go4Cache.getGeocachersInViewport(Settings.getUsername(), viewport.resize(1.5));
            go4CacheDisplayExecutor.execute(new Go4CacheDisplayRunnable(go4CacheUsers, viewport));
        }
    }

    /**
     * Thread to display users of Go 4 Cache started from {@link Go4CacheRunnable}
     */
    private class Go4CacheDisplayRunnable extends DoRunnable {

        private List<Go4CacheUser> users = null;

        public Go4CacheDisplayRunnable(List<Go4CacheUser> usersIn, final Viewport viewport) {
            super(viewport);
            users = usersIn;
        }

        @Override
        public void run() {
            if (mapView == null || CollectionUtils.isEmpty(users)) {
                return;
            }

            // display users
            List<OtherCachersOverlayItemImpl> items = new ArrayList<OtherCachersOverlayItemImpl>();

            int counter = 0;
            OtherCachersOverlayItemImpl item;

            for (Go4CacheUser userOne : users) {
                if (userOne.getCoords() == null) {
                    continue;
                }

                item = mapItemFactory.getOtherCachersOverlayItemBase(activity, userOne);
                items.add(item);

                counter++;
                if ((counter % 10) == 0) {
                    overlayGo4Cache.updateItems(items);
                    displayHandler.sendEmptyMessage(INVALIDATE_MAP);
                }
            }

            overlayGo4Cache.updateItems(items);
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

    private static abstract class DoRunnable implements Runnable {

        final protected Viewport viewport;

        public DoRunnable(final Viewport viewport) {
            this.viewport = viewport;
        }

        @Override
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

            deleteGeoDirObservers();

            for (final String geocode : geocodes) {
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
                            Log.i("Stopped storing process.");

                            break;
                        }

                        cgCache.storeCache(null, geocode, StoredList.STANDARD_LIST_ID, false, handler);
                    }
                } catch (Exception e) {
                    Log.e("cgeocaches.LoadDetails.run: " + e.toString());
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
            addGeoDirObservers();
        }
    }

    private static boolean mapMoved(final Viewport referenceViewport, final Viewport newViewport) {
        return Math.abs(newViewport.getLatitudeSpan() - referenceViewport.getLatitudeSpan()) > 50e-6 ||
                Math.abs(newViewport.getLongitudeSpan() - referenceViewport.getLongitudeSpan()) > 50e-6 ||
                Math.abs(newViewport.center.getLatitude() - referenceViewport.center.getLatitude()) > referenceViewport.getLatitudeSpan() / 4 ||
                Math.abs(newViewport.center.getLongitude() - referenceViewport.center.getLongitude()) > referenceViewport.getLongitudeSpan() / 4;
    }

    // center map to desired location
    private void centerMap(final Geopoint coords) {
        if (coords == null) {
            return;
        }
        if (mapView == null) {
            return;
        }

        final MapControllerImpl mapController = mapView.getMapController();
        final GeoPointImpl target = makeGeoPoint(coords);

        if (alreadyCentered) {
            mapController.animateTo(target);
        } else {
            mapController.setCenter(target);
        }

        alreadyCentered = true;

    }

    // move map to view results of searchIntent
    private void centerMap(String geocodeCenter, final SearchResult searchCenter, final Geopoint coordsCenter, int[] mapState) {
        final MapControllerImpl mapController = mapView.getMapController();

        if (!centered && mapState != null) {
            try {
                mapController.setCenter(mapItemFactory.getGeoPointBase(new Geopoint(mapState[0] / 1.0e6, mapState[1] / 1.0e6)));
                mapController.setZoom(mapState[2]);
            } catch (Exception e) {
                // nothing at all
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && (geocodeCenter != null || searchIntent != null)) {
            try {
                Viewport viewport = null;

                if (geocodeCenter != null) {
                    viewport = app.getBounds(geocodeCenter);
                } else if (searchCenter != null) {
                    viewport = app.getBounds(searchCenter.getGeocodes());
                }

                if (viewport == null) {
                    return;
                }

                mapController.setCenter(mapItemFactory.getGeoPointBase(viewport.center));
                if (viewport.getLatitudeSpan() != 0 && viewport.getLongitudeSpan() != 0) {
                    mapController.zoomToSpan((int) (viewport.getLatitudeSpan() * 1e6), (int) (viewport.getLongitudeSpan() * 1e6));
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
            myLocationInMiddle(app.currentGeo());
        } else {
            myLocSwitch.setImageResource(R.drawable.actionbar_mylocation_off);
        }
    }

    // set my location listener
    private class MyLocationListener implements View.OnClickListener {
        @Override
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
        return mapItemFactory.getGeoPointBase(coords);
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
        imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return imageView;
    }

    private static Intent newIntent(final Context context) {
        return new Intent(context, Settings.getMapProvider().getMapClass());
    }

    public static void startActivitySearch(final Activity fromActivity, final SearchResult search, final String title) {
        final Intent mapIntent = newIntent(fromActivity);
        mapIntent.putExtra(EXTRAS_SEARCH, search);
        mapIntent.putExtra(EXTRAS_MAP_MODE, MapMode.LIST);
        if (StringUtils.isNotBlank(title)) {
            mapIntent.putExtra(CGeoMap.EXTRAS_MAP_TITLE, title);
        }
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivityLiveMap(final Activity fromActivity) {
        final Intent mapIntent = newIntent(fromActivity);
        mapIntent.putExtra(EXTRAS_MAP_MODE, Settings.isLiveMap() ? MapMode.LIVE_ONLINE : MapMode.LIVE_OFFLINE);
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords, final WaypointType type, final String title) {
        final Intent mapIntent = newIntent(fromActivity);
        mapIntent.putExtra(EXTRAS_MAP_MODE, MapMode.COORDS);
        mapIntent.putExtra(EXTRAS_COORDS, coords);
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
        mapIntent.putExtra(EXTRAS_MAP_MODE, MapMode.SINGLE);
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
    private CachesOverlayItemImpl getItem(final IWaypoint coord, final cgCache cache, final cgWaypoint waypoint) {
        if (cache != null) {
            final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(coord, cache.getType());

            final int hashcode = new HashCodeBuilder()
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

            final LayerDrawable ldFromCache = CGeoMap.overlaysCache.get(hashcode);
            if (ldFromCache != null) {
                item.setMarker(ldFromCache);
                return item;
            }

            // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
            final ArrayList<Drawable> layers = new ArrayList<Drawable>(9);
            final ArrayList<int[]> insets = new ArrayList<int[]>(8);

            // background: disabled or not
            final Drawable marker = getResources().getDrawable(cache.isDisabled() || cache.isArchived() ? R.drawable.marker_disabled : R.drawable.marker);
            layers.add(marker);
            final int resolution = marker.getIntrinsicWidth() > 40 ? 1 : 0;
            // reliable or not
            if (!cache.isReliableLatLon()) {
                insets.add(INSET_RELIABLE[resolution]);
                layers.add(getResources().getDrawable(R.drawable.marker_notreliable));
            }
            // cache type
            layers.add(getResources().getDrawable(cache.getType().markerId));
            insets.add(INSET_TYPE[resolution]);
            // own
            if (cache.isOwn()) {
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

            final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

            int index = 1;
            for (final int[] inset : insets) {
                ld.setLayerInset(index++, inset[0], inset[1], inset[2], inset[3]);
            }

            CGeoMap.overlaysCache.put(hashcode, ld);

            item.setMarker(ld);
            return item;
        }

        if (waypoint != null) {

            final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(coord, null);
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
