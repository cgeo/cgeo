package cgeo.geocaching.maps;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.DirectionProvider;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LiveMapStrategy.Strategy;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.LiveMapInfoDialogBuilder;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.LeastRecentlyUsedSet;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RunnableWithArgument;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
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

    /** Controls the behavior of the map */
    public enum MapMode {
        /** Live Map */
        LIVE,
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
    private static final String EXTRAS_MAP_TITLE = "mapTitle";
    private static final String EXTRAS_MAP_MODE = "mapMode";
    private static final String EXTRAS_LIVE_ENABLED = "liveEnabled";

    private static final String BUNDLE_MAP_SOURCE = "mapSource";
    private static final String BUNDLE_MAP_STATE = "mapState";
    private static final String BUNDLE_LIVE_ENABLED = "liveEnabled";
    private static final String BUNDLE_TRAIL_HISTORY = "trailHistory";

    private Resources res = null;
    private MapItemFactory mapItemFactory = null;
    private Activity activity = null;
    private MapViewImpl mapView = null;
    private CgeoApplication app = null;
    final private GeoDirHandler geoDirUpdate = new UpdateLoc();
    private SearchResult searchIntent = null;
    private String geocodeIntent = null;
    private Geopoint coordsIntent = null;
    private WaypointType waypointTypeIntent = null;
    private int[] mapStateIntent = null;
    // status data
    /** Last search result used for displaying header */
    private SearchResult lastSearchResult = null;
    private MapTokens tokens = null;
    private boolean noMapTokenShowed = false;
    // map status data
    private boolean followMyLocation = false;
    private Viewport viewport = null;
    private int zoom = -100;
    // threads
    private LoadTimer loadTimer = null;
    private LoadDetails loadDetailsThread = null;
    /** Time of last {@link LoadRunnable} run */
    private volatile long loadThreadRun = 0L;
    //Interthread communication flag
    private volatile boolean downloaded = false;
    // overlays
    private CachesOverlay overlayCaches = null;
    private PositionAndScaleOverlay overlayPositionAndScale = null;
    // data for overlays
    private static final int[][] INSET_RELIABLE = { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }; // center, 33x40 / 45x51 / 60x68
    private static final int[][] INSET_TYPE = { { 5, 8, 6, 10 }, { 4, 4, 5, 11 }, { 4, 4, 5, 11 } }; // center, 22x22 / 36x36
    private static final int[][] INSET_OWN = { { 21, 0, 0, 26 }, { 25, 0, 0, 35 }, { 40, 0, 0, 48 } }; // top right, 12x12 / 16x16 / 20x20
    private static final int[][] INSET_FOUND = { { 0, 0, 21, 28 }, { 0, 0, 25, 35 }, { 0, 0, 40, 48 } }; // top left, 12x12 / 16x16 / 20x20
    private static final int[][] INSET_USERMODIFIEDCOORDS = { { 21, 28, 0, 0 }, { 19, 25, 0, 0 }, { 25, 33, 0, 0 } }; // bottom right, 12x12 / 26x26 / 35x35
    private static final int[][] INSET_PERSONALNOTE = { { 0, 28, 21, 0 }, { 0, 25, 19, 0 }, { 0, 33, 25, 0 } }; // bottom left, 12x12 / 26x26 / 35x35

    private SparseArray<LayerDrawable> overlaysCache = new SparseArray<LayerDrawable>();
    /** Count of caches currently visible */
    private int cachesCnt = 0;
    /** List of caches in the viewport */
    private LeastRecentlyUsedSet<Geocache> caches = null;
    /** List of waypoints in the viewport */
    private final LeastRecentlyUsedSet<Waypoint> waypoints = new LeastRecentlyUsedSet<Waypoint>(MAX_CACHES);
    // storing for offline
    private ProgressDialog waitDialog = null;
    private int detailTotal = 0;
    private int detailProgress = 0;
    private long detailProgressTime = 0L;
    // views
    private ImageSwitcher myLocSwitch = null;

    /** Controls the map behaviour */
    private MapMode mapMode = null;
    /** Live mode enabled for map. **/
    private boolean isLiveEnabled;
    // other things
    private boolean markersInvalidated = false; // previous state for loadTimer
    private boolean centered = false; // if map is already centered
    private boolean alreadyCentered = false; // -""- for setting my location
    private static final Set<String> dirtyCaches = new HashSet<String>();
    /**
     * if live map is enabled, this is the minimum zoom level, independent of the stored setting
     */
    private static final int MIN_LIVEMAP_ZOOM = 12;

    // Thread pooling
    private static BlockingQueue<Runnable> displayQueue = new ArrayBlockingQueue<Runnable>(1);
    private static ThreadPoolExecutor displayExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, displayQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static BlockingQueue<Runnable> downloadQueue = new ArrayBlockingQueue<Runnable>(1);
    private static ThreadPoolExecutor downloadExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, downloadQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static BlockingQueue<Runnable> loadQueue = new ArrayBlockingQueue<Runnable>(1);
    private static ThreadPoolExecutor loadExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, loadQueue, new ThreadPoolExecutor.DiscardOldestPolicy());

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

                    if (mapMode == MapMode.LIVE && isLiveEnabled) {
                        title.append(res.getString(R.string.map_live));
                    } else {
                        title.append(mapTitle);
                    }

                    countVisibleCaches();
                    if (caches != null && !caches.isEmpty() && !mapTitle.contains("[")) {
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
                    } else {
                        int minsRemaining = secondsRemaining / 60;
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + minsRemaining + " " + res.getQuantityString(R.plurals.caches_eta_mins, minsRemaining));
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
        final List<Geocache> protectedCaches = caches.getAsList();

        int count = 0;
        if (!protectedCaches.isEmpty()) {
            final Viewport viewport = mapView.getViewport();

            for (final Geocache cache : protectedCaches) {
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
        outState.putBoolean(BUNDLE_LIVE_ENABLED, isLiveEnabled);
        if (overlayPositionAndScale != null) {
            outState.putParcelableArrayList(BUNDLE_TRAIL_HISTORY, overlayPositionAndScale.getHistory());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // class init
        res = this.getResources();
        activity = this.getActivity();
        app = (CgeoApplication) activity.getApplication();

        int countBubbleCnt = DataStore.getAllCachesCount();
        caches = new LeastRecentlyUsedSet<Geocache>(MAX_CACHES + countBubbleCnt);

        final MapProvider mapProvider = Settings.getMapProvider();
        mapItemFactory = mapProvider.getMapItemFactory();

        // Get parameters from the intent
        final Bundle extras = activity.getIntent().getExtras();
        if (extras != null) {
            mapMode = (MapMode) extras.get(EXTRAS_MAP_MODE);
            isLiveEnabled = extras.getBoolean(EXTRAS_LIVE_ENABLED, false);
            searchIntent = extras.getParcelable(EXTRAS_SEARCH);
            geocodeIntent = extras.getString(EXTRAS_GEOCODE);
            coordsIntent = extras.getParcelable(EXTRAS_COORDS);
            waypointTypeIntent = WaypointType.findById(extras.getString(EXTRAS_WPTTYPE));
            mapStateIntent = extras.getIntArray(EXTRAS_MAPSTATE);
            mapTitle = extras.getString(EXTRAS_MAP_TITLE);
        }
        else {
            mapMode = MapMode.LIVE;
            isLiveEnabled = Settings.isLiveMap();
        }
        if (StringUtils.isBlank(mapTitle)) {
            mapTitle = res.getString(R.string.map_map);
        }

        ArrayList<Location> trailHistory = null;

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            currentSourceId = savedInstanceState.getInt(BUNDLE_MAP_SOURCE, Settings.getMapSource().getNumericalId());
            mapStateIntent = savedInstanceState.getIntArray(BUNDLE_MAP_STATE);
            isLiveEnabled = savedInstanceState.getBoolean(BUNDLE_LIVE_ENABLED, false);
            trailHistory = savedInstanceState.getParcelableArrayList(BUNDLE_TRAIL_HISTORY);
        } else {
            currentSourceId = Settings.getMapSource().getNumericalId();
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

        if (overlayCaches == null) {
            overlayCaches = mapView.createAddMapOverlay(mapView.getContext(), getResources().getDrawable(R.drawable.marker));
        }

        if (overlayPositionAndScale == null) {
            overlayPositionAndScale = mapView.createAddPositionAndScaleOverlay(activity);
            if (trailHistory != null) {
                overlayPositionAndScale.setHistory(trailHistory);
            }
        }

        mapView.repaintRequired(null);

        setZoom(Settings.getMapZoom());
        mapView.getMapController().setCenter(Settings.getMapCenter());

        if (null == mapStateIntent) {
            followMyLocation = mapMode == MapMode.LIVE;
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
            LiveMapInfoDialogBuilder.create(activity).show();
        }
    }

    /**
     * Set the zoom of the map. The zoom is restricted to a certain minimum in case of live map.
     *
     * @param zoom
     */
    private void setZoom(final int zoom) {
        mapView.getMapController().setZoom(isLiveEnabled ? Math.min(zoom, MIN_LIVEMAP_ZOOM) : zoom);
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

        addGeoDirObservers();

        if (!CollectionUtils.isEmpty(dirtyCaches)) {
            for (String geocode : dirtyCaches) {
                Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                if (cache != null) {
                    // new collection type needs to remove first
                    caches.remove(cache);
                    // re-add to update the freshness
                    caches.add(cache);
                }
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
        stopTimer();
        deleteGeoDirObservers();
        savePrefs();

        if (mapView != null) {
            mapView.destroyDrawingCache();
        }

        overlaysCache.clear();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // menu inflation happens in Google/Mapsforge specific classes
        super.onCreateOptionsMenu(menu);

        MapProviderFactory.addMapviewMenuItems(menu);

        final SubMenu subMenuStrategy = menu.findItem(R.id.submenu_strategy).getSubMenu();
        subMenuStrategy.setHeaderTitle(res.getString(R.string.map_strategy_title));
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (MapSource mapSource : MapProviderFactory.getMapSources()) {
            final MenuItem menuItem = menu.findItem(mapSource.getNumericalId());
            if (menuItem != null) {
                menuItem.setEnabled(mapSource.isAvailable());
            }
        }

        try {
            MenuItem item = menu.findItem(R.id.menu_trail_mode);
            if (Settings.isMapTrail()) {
                item.setTitle(res.getString(R.string.map_trail_hide));
            } else {
                item.setTitle(res.getString(R.string.map_trail_show));
            }

            item = menu.findItem(R.id.menu_map_live); // live map
            if (isLiveEnabled) {
                item.setTitle(res.getString(R.string.map_live_disable));
            } else {
                item.setTitle(res.getString(R.string.map_live_enable));
            }

            item = menu.findItem(R.id.menu_mycaches_mode); // own & found caches
            if (Settings.isExcludeMyCaches()) {
                item.setTitle(res.getString(R.string.map_mycaches_show));
            } else {
                item.setTitle(res.getString(R.string.map_mycaches_hide));
            }

            final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
            menu.findItem(R.id.menu_store_caches).setEnabled(!isLoading() && CollectionUtils.isNotEmpty(geocodesInViewport) && new SearchResult(geocodesInViewport).hasUnsavedCaches());

            item = menu.findItem(R.id.menu_circle_mode); // show circles
            if (overlayCaches != null && overlayCaches.getCircles()) {
                item.setTitle(res.getString(R.string.map_circles_hide));
            } else {
                item.setTitle(res.getString(R.string.map_circles_show));
            }

            item = menu.findItem(R.id.menu_theme_mode); // show theme selection
            item.setVisible(mapView.hasMapThemes());

            menu.findItem(R.id.menu_as_list).setEnabled(!isLoading());

            menu.findItem(R.id.submenu_strategy).setEnabled(isLiveEnabled);

            switch (Settings.getLiveMapStrategy()) {
                case FASTEST:
                    menu.findItem(R.id.menu_strategy_fastest).setChecked(true);
                    break;
                case FAST:
                    menu.findItem(R.id.menu_strategy_fast).setChecked(true);
                    break;
                case AUTO:
                    menu.findItem(R.id.menu_strategy_auto).setChecked(true);
                    break;
                default: // DETAILED
                    menu.findItem(R.id.menu_strategy_detailed).setChecked(true);
            }
        } catch (RuntimeException e) {
            Log.e("CGeoMap.onPrepareOptionsMenu", e);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_trail_mode:
                Settings.setMapTrail(!Settings.isMapTrail());
                mapView.repaintRequired(overlayPositionAndScale);
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_map_live:
                isLiveEnabled = !isLiveEnabled;
                if (mapMode == MapMode.LIVE) {
                    Settings.setLiveMap(isLiveEnabled);
                }
                markersInvalidated = true;
                lastSearchResult = null;
                searchIntent = null;
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_store_caches:
                if (!isLoading()) {
                    final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
                    final List<String> geocodes = new ArrayList<String>();

                    for (final String geocode : geocodesInViewport) {
                        if (!DataStore.isOffline(geocode, null)) {
                            geocodes.add(geocode);
                        }
                    }

                    detailTotal = geocodes.size();
                    detailProgress = 0;

                    if (detailTotal == 0) {
                        ActivityMixin.showToast(activity, res.getString(R.string.warn_save_nothing));

                        return true;
                    }

                    if (Settings.getChooseList()) {
                        // let user select list to store cache in
                        new StoredList.UserInterface(activity).promptForListSelection(R.string.list_title,
                                new RunnableWithArgument<Integer>() {
                                    @Override
                                    public void run(final Integer selectedListId) {
                                        storeCaches(geocodes, selectedListId);
                                    }
                                }, true, StoredList.TEMPORARY_LIST_ID);
                    } else {
                        storeCaches(geocodes, StoredList.STANDARD_LIST_ID);
                    }
                }
                return true;
            case R.id.menu_circle_mode:
                if (overlayCaches == null) {
                    return false;
                }

                overlayCaches.switchCircles();
                mapView.repaintRequired(overlayCaches);
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_mycaches_mode:
                Settings.setExcludeMine(!Settings.isExcludeMyCaches());
                markersInvalidated = true;
                ActivityMixin.invalidateOptionsMenu(activity);
                if (!Settings.isExcludeMyCaches()) {
                    Tile.Cache.clear();
                }
                return true;
            case R.id.menu_theme_mode:
                selectMapTheme();
                return true;
            case R.id.menu_as_list: {
                CacheListActivity.startActivityMap(activity, new SearchResult(getGeocodesForCachesInViewport()));
                return true;
            }
            case R.id.menu_strategy_fastest: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(Strategy.FASTEST);
                return true;
            }
            case R.id.menu_strategy_fast: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(Strategy.FAST);
                return true;
            }
            case R.id.menu_strategy_auto: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(Strategy.AUTO);
                return true;
            }
            case R.id.menu_strategy_detailed: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(Strategy.DETAILED);
                return true;
            }
            default:
                final MapSource mapSource = MapProviderFactory.getMapSource(id);
                if (mapSource != null) {
                    item.setChecked(true);
                    changeMapSource(mapSource);
                    return true;
                }
        }
        return false;
    }

    private void selectMapTheme() {

        final File[] themeFiles = Settings.getMapThemeFiles();

        String currentTheme = StringUtils.EMPTY;
        String currentThemePath = Settings.getCustomRenderThemeFilePath();
        if (StringUtils.isNotEmpty(currentThemePath)) {
            File currentThemeFile = new File(currentThemePath);
            currentTheme = currentThemeFile.getName();
        }

        List<String> names = new ArrayList<String>();
        names.add(res.getString(R.string.map_theme_builtin));
        int currentItem = 0;
        for (File file : themeFiles) {
            if (currentTheme.equalsIgnoreCase(file.getName())) {
                currentItem = names.size();
            }
            names.add(file.getName());
        }

        final int selectedItem = currentItem;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.map_theme_select);

        builder.setSingleChoiceItems(names.toArray(new String[names.size()]), selectedItem,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int newItem) {
                        if (newItem != selectedItem) {
                            // Adjust index because of <default> selection
                            if (newItem > 0) {
                                Settings.setCustomRenderThemeFile(themeFiles[newItem - 1].getPath());
                            } else {
                                Settings.setCustomRenderThemeFile(StringUtils.EMPTY);
                            }
                            mapView.setMapTheme();
                        }
                        dialog.cancel();
                    }
                });

        builder.show();
    }

    /**
     * @return a non-null Set of geocodes corresponding to the caches that are shown on screen.
     */
    private Set<String> getGeocodesForCachesInViewport() {
        final Set<String> geocodes = new HashSet<String>();
        final List<Geocache> cachesProtected = caches.getAsList();

        final Viewport viewport = mapView.getViewport();

        for (final Geocache cache : cachesProtected) {
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
    private boolean changeMapSource(final MapSource mapSource) {
        final boolean restartRequired = !MapProviderFactory.isSameActivity(Settings.getMapSource(), mapSource);

        Settings.setMapSource(mapSource);
        currentSourceId = mapSource.getNumericalId();

        if (restartRequired) {
            mapRestart();
        } else if (mapView != null) {
            mapView.setMapSource();
            ActivityMixin.invalidateOptionsMenu(activity);
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
        mapIntent.putExtra(EXTRAS_LIVE_ENABLED, isLiveEnabled);

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
        Settings.setMapCenter(mapView.getMapViewCenter());
    }

    // Set center of map to my location if appropriate.
    private void myLocationInMiddle(final IGeoData geo) {
        if (followMyLocation && !geo.isPseudoLocation()) {
            centerMap(geo.getCoords());
        }
    }

    // class: update location
    private class UpdateLoc extends GeoDirHandler {
        // use the following constants for fine tuning - find good compromise between smooth updates and as less updates as possible

        // minimum time in milliseconds between position overlay updates
        private static final long MIN_UPDATE_INTERVAL = 500;
        // minimum change of heading in grad for position overlay update
        private static final float MIN_HEADING_DELTA = 15f;
        // minimum change of location in fraction of map width/height (whatever is smaller) for position overlay update
        private static final float MIN_LOCATION_DELTA = 0.01f;

        Location currentLocation = new Location("");
        boolean locationValid = false;
        float currentHeading;

        private long timeLastPositionOverlayCalculation = 0;

        @Override
        protected void updateGeoData(final IGeoData geo) {
            if (geo.isPseudoLocation()) {
                locationValid = false;
            } else {
                locationValid = true;

                currentLocation = geo.getLocation();

                if (!Settings.isUseCompass() || geo.getSpeed() > 5) { // use GPS when speed is higher than 18 km/h
                    currentHeading = geo.getBearing();
                }

                repaintPositionOverlay();
            }
        }

        @Override
        public void updateDirection(final float direction) {
            if (app.currentGeo().getSpeed() <= 5) { // use compass when speed is lower than 18 km/h
                currentHeading = DirectionProvider.getDirectionNow(activity, direction);
                repaintPositionOverlay();
            }
        }

        /**
         * Repaint position overlay but only with a max frequency and if position or heading changes sufficiently.
         */
        void repaintPositionOverlay() {
            final long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > timeLastPositionOverlayCalculation + MIN_UPDATE_INTERVAL) {
                timeLastPositionOverlayCalculation = currentTimeMillis;

                try {
                    if (mapView != null) {
                        if (overlayPositionAndScale == null) {
                            overlayPositionAndScale = mapView.createAddPositionAndScaleOverlay(activity);
                        }

                        boolean needsRepaintForDistance = needsRepaintForDistance();
                        boolean needsRepaintForHeading = needsRepaintForHeading();

                        if (needsRepaintForDistance) {
                            if (followMyLocation) {
                                centerMap(new Geopoint(currentLocation));
                            }
                        }

                        if (needsRepaintForDistance || needsRepaintForHeading) {
                            overlayPositionAndScale.setCoordinates(currentLocation);
                            overlayPositionAndScale.setHeading(currentHeading);
                            mapView.repaintRequired(overlayPositionAndScale);
                        }
                    }
                } catch (RuntimeException e) {
                    Log.w("Failed to update location.");
                }
            }
        }

        boolean needsRepaintForHeading() {
            return Math.abs(AngleUtils.difference(currentHeading, overlayPositionAndScale.getHeading())) > MIN_HEADING_DELTA;
        }

        boolean needsRepaintForDistance() {

            if (!locationValid) {
                return false;
            }

            final Location lastLocation = overlayPositionAndScale.getCoordinates();

            float dist = Float.MAX_VALUE;
            if (lastLocation != null) {
                dist = currentLocation.distanceTo(lastLocation);
            }

            final float[] mapDimension = new float[1];
            if (mapView.getWidth() < mapView.getHeight()) {
                final double span = mapView.getLongitudeSpan() / 1e6;
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude() + span, mapDimension);
            } else {
                final double span = mapView.getLatitudeSpan() / 1e6;
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude() + span, currentLocation.getLongitude(), mapDimension);
            }

            return dist > (mapDimension[0] * MIN_LOCATION_DELTA);
        }
    }

    /**
     * Starts the {@link LoadTimer}.
     */

    public synchronized void startTimer() {
        if (coordsIntent != null) {
            // display just one point
            (new DisplayPointThread()).start();
        } else {
            // start timer
            stopTimer();
            loadTimer = new LoadTimer();
            loadTimer.start();
        }
    }

    private synchronized void stopTimer() {
        if (loadTimer != null) {
            loadTimer.stopIt();
            loadTimer = null;
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
                        final boolean moved = markersInvalidated || (isLiveEnabled && !downloaded) || (viewport == null) || zoomNow != zoom ||
                                (mapMoved(viewport, viewportNow) && (cachesCnt <= 0 || CollectionUtils.isEmpty(caches) || !viewport.includes(viewportNow)));

                        // update title on any change
                        if (moved || !viewportNow.equals(viewport)) {
                            displayHandler.sendEmptyMessage(UPDATE_TITLE);
                        }
                        zoom = zoomNow;

                        // save new values
                        if (moved) {
                            markersInvalidated = false;

                            long currentTime = System.currentTimeMillis();

                            if (1000 < (currentTime - loadThreadRun)) {
                                viewport = viewportNow;
                                loadExecutor.execute(new LoadRunnable(viewport));
                            }
                        }
                    }

                    yield();
                } catch (Exception e) {
                    Log.w("CGeoMap.LoadTimer.run", e);
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

                SearchResult searchResult;
                if (mapMode == MapMode.LIVE) {
                    searchResult = isLiveEnabled ? new SearchResult() : new SearchResult(DataStore.loadStoredInViewport(viewport, Settings.getCacheType()));
                } else {
                    // map started from another activity
                    searchResult = searchIntent != null ? new SearchResult(searchIntent) : new SearchResult();
                    if (geocodeIntent != null) {
                        searchResult.addGeocode(geocodeIntent);
                    }
                }
                // live mode search result
                if (isLiveEnabled) {
                    searchResult.addSearchResult(DataStore.loadCachedInViewport(viewport, Settings.getCacheType()));
                }

                downloaded = true;
                Set<Geocache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);
                // update the caches
                // new collection type needs to remove first
                caches.removeAll(cachesFromSearchResult);
                caches.addAll(cachesFromSearchResult);

                final boolean excludeMine = Settings.isExcludeMyCaches();
                final boolean excludeDisabled = Settings.isExcludeDisabledCaches();
                if (mapMode == MapMode.LIVE) {
                    CGeoMap.filter(caches);
                }
                countVisibleCaches();
                if (cachesCnt < Settings.getWayPointsThreshold() || geocodeIntent != null) {
                    // we don't want to see any stale waypoints
                    waypoints.clear();
                    if (isLiveEnabled || mapMode == MapMode.LIVE
                            || mapMode == MapMode.COORDS) {
                        //All visible waypoints
                        CacheType type = Settings.getCacheType();
                        Set<Waypoint> waypointsInViewport = DataStore.loadWaypoints(viewport, excludeMine, excludeDisabled, type);
                        waypoints.addAll(waypointsInViewport);
                    }
                    else {
                        //All waypoints from the viewed caches
                        for (Geocache c : caches.getAsList()) {
                            waypoints.addAll(c.getWaypoints());
                        }
                    }
                }
                else {
                    // we don't want to see any stale waypoints when above threshold
                    waypoints.clear();
                }

                //render
                displayExecutor.execute(new DisplayRunnable(viewport));

                if (isLiveEnabled) {
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
     * Started by {@link LoadRunnable}.
     */

    private class DownloadRunnable extends DoRunnable {

        public DownloadRunnable(final Viewport viewport) {
            super(viewport);
        }

        @Override
        public void run() {
            try {
                showProgressHandler.sendEmptyMessage(SHOW_PROGRESS); // show progress
                SearchResult searchResult = new SearchResult();
                if (Settings.isGCConnectorActive()) {
                    if (tokens == null) {
                        tokens = GCLogin.getInstance().getMapTokens();
                        if (noMapTokenHandler != null && (StringUtils.isEmpty(tokens.getUserSession()) || StringUtils.isEmpty(tokens.getSessionToken()))) {
                            tokens = null;
                            noMapTokenHandler.sendEmptyMessage(0);
                        }
                    }

                    searchResult = ConnectorFactory.searchByViewport(viewport.resize(0.8), tokens);
                    downloaded = true;
                }

                Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                CGeoMap.filter(result);
                // update the caches
                // first remove filtered out
                final Set<String> filteredCodes = searchResult.getFilteredGeocodes();
                Log.d("Filtering out " + filteredCodes.size() + " caches: " + filteredCodes.toString());
                caches.removeAll(DataStore.loadCaches(filteredCodes, LoadFlags.LOAD_CACHE_ONLY));
                DataStore.removeCaches(filteredCodes, EnumSet.of(RemoveFlag.REMOVE_CACHE));
                // new collection type needs to remove first to refresh
                caches.removeAll(result);
                caches.addAll(result);
                lastSearchResult = searchResult;

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
                final List<Geocache> cachesToDisplay = caches.getAsList();
                final List<Waypoint> waypointsToDisplay = new ArrayList<Waypoint>(waypoints);
                final List<CachesOverlayItemImpl> itemsToDisplay = new ArrayList<CachesOverlayItemImpl>();

                if (!cachesToDisplay.isEmpty()) {
                    // Only show waypoints for single view or setting
                    // when less than showWaypointsthreshold Caches shown
                    if (mapMode == MapMode.SINGLE || (cachesCnt < Settings.getWayPointsThreshold())) {
                        for (Waypoint waypoint : waypointsToDisplay) {

                            if (waypoint == null || waypoint.getCoords() == null) {
                                continue;
                            }

                            itemsToDisplay.add(getWaypointItem(waypoint));
                        }
                    }
                    for (Geocache cache : cachesToDisplay) {

                        if (cache == null || cache.getCoords() == null) {
                            continue;
                        }
                        itemsToDisplay.add(getCacheItem(cache));
                    }

                    overlayCaches.updateItems(itemsToDisplay);
                    displayHandler.sendEmptyMessage(INVALIDATE_MAP);

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
     * Thread to display one point. Started on opening if in single mode.
     */
    private class DisplayPointThread extends Thread {

        @Override
        public void run() {
            if (mapView == null || caches == null) {
                return;
            }

            if (coordsIntent != null) {
                final Waypoint waypoint = new Waypoint("some place", waypointTypeIntent != null ? waypointTypeIntent : WaypointType.WAYPOINT, false);
                waypoint.setCoords(coordsIntent);

                final CachesOverlayItemImpl item = getWaypointItem(waypoint);
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

        protected DoRunnable(final Viewport viewport) {
            this.viewport = viewport;
        }
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
     * store caches, invoked by "store offline" menu item
     *
     * @param listId
     *            the list to store the caches in
     */
    private void storeCaches(List<String> geocodes, int listId) {
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
                    Log.e("CGeoMap.storeCaches.onCancel", e);
                }
            }
        });

        float etaTime = detailTotal * 7.0f / 60.0f;
        int roundedEta = Math.round(etaTime);
        if (etaTime < 0.4) {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
        } else {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + roundedEta + " " + res.getQuantityString(R.plurals.caches_eta_mins, roundedEta));
        }
        waitDialog.show();

        detailProgressTime = System.currentTimeMillis();

        loadDetailsThread = new LoadDetails(loadDetailsHandler, geocodes, listId);
        loadDetailsThread.start();
    }

    /**
     * Thread to store the caches in the viewport. Started by Activity.
     */

    private class LoadDetails extends Thread {

        final private CancellableHandler handler;
        final private List<String> geocodes;
        final private int listId;

        public LoadDetails(final CancellableHandler handler, final List<String> geocodes, final int listId) {
            this.handler = handler;
            this.geocodes = geocodes;
            this.listId = listId;
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

                    if (!DataStore.isOffline(geocode, null)) {
                        Geocache.storeCache(null, geocode, listId, false, handler);
                    }
                } catch (Exception e) {
                    Log.e("CGeoMap.LoadDetails.run", e);
                } finally {
                    // one more cache over
                    detailProgress++;
                    handler.sendEmptyMessage(UPDATE_PROGRESS);
                }

                // FIXME: what does this yield() do here?
                yield();
            }

            // we're done
            handler.sendEmptyMessage(FINISHED_LOADING_DETAILS);
            addGeoDirObservers();
        }
    }

    private static synchronized void filter(Collection<Geocache> caches) {
        boolean excludeMine = Settings.isExcludeMyCaches();
        boolean excludeDisabled = Settings.isExcludeDisabledCaches();

        List<Geocache> removeList = new ArrayList<Geocache>();
        for (Geocache cache : caches) {
            if ((excludeMine && cache.isFound()) || (excludeMine && cache.isOwner()) || (excludeDisabled && cache.isDisabled())) {
                removeList.add(cache);
            }
        }
        caches.removeAll(removeList);
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
                setZoom(mapState[2]);
            } catch (RuntimeException e) {
                Log.e("centermap", e);
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && (geocodeCenter != null || searchIntent != null)) {
            try {
                Viewport viewport = null;

                if (geocodeCenter != null) {
                    viewport = DataStore.getBounds(geocodeCenter);
                } else if (searchCenter != null) {
                    viewport = DataStore.getBounds(searchCenter.getGeocodes());
                }

                if (viewport == null) {
                    return;
                }

                mapController.setCenter(mapItemFactory.getGeoPointBase(viewport.center));
                if (viewport.getLatitudeSpan() != 0 && viewport.getLongitudeSpan() != 0) {
                    mapController.zoomToSpan((int) (viewport.getLatitudeSpan() * 1e6), (int) (viewport.getLongitudeSpan() * 1e6));
                }
            } catch (RuntimeException e) {
                Log.e("centermap", e);
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && coordsCenter != null) {
            try {
                mapController.setCenter(makeGeoPoint(coordsCenter));
            } catch (Exception e) {
                Log.e("centermap", e);
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
        mapIntent.putExtra(EXTRAS_LIVE_ENABLED, false);
        if (StringUtils.isNotBlank(title)) {
            mapIntent.putExtra(CGeoMap.EXTRAS_MAP_TITLE, title);
        }
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivityLiveMap(final Activity fromActivity) {
        final Intent mapIntent = newIntent(fromActivity);
        mapIntent.putExtra(EXTRAS_MAP_MODE, MapMode.LIVE);
        mapIntent.putExtra(EXTRAS_LIVE_ENABLED, Settings.isLiveMap());
        fromActivity.startActivity(mapIntent);
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords, final WaypointType type, final String title) {
        final Intent mapIntent = newIntent(fromActivity);
        mapIntent.putExtra(EXTRAS_MAP_MODE, MapMode.COORDS);
        mapIntent.putExtra(EXTRAS_LIVE_ENABLED, false);
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
        mapIntent.putExtra(EXTRAS_LIVE_ENABLED, false);
        mapIntent.putExtra(EXTRAS_GEOCODE, geocode);
        mapIntent.putExtra(EXTRAS_MAP_TITLE, geocode);
        fromActivity.startActivity(mapIntent);
    }

    public static void markCacheAsDirty(final String geocode) {
        dirtyCaches.add(geocode);
    }

    private CachesOverlayItemImpl getCacheItem(final Geocache cache) {
        final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(cache, cache.getType().applyDistanceRule());

        final int hashcode = new HashCodeBuilder()
                .append(cache.isReliableLatLon())
                .append(cache.getType().id)
                .append(cache.isDisabled() || cache.isArchived())
                .append(cache.getMapMarkerId())
                .append(cache.isOwner())
                .append(cache.isFound())
                .append(cache.hasUserModifiedCoords())
                .append(cache.getPersonalNote())
                .append(cache.isLogOffline())
                .append(cache.getListId() > 0)
                .toHashCode();

        LayerDrawable drawable = overlaysCache.get(hashcode);
        if (drawable == null) {
            drawable = createCacheItem(cache, hashcode);
        }
        item.setMarker(drawable);
        return item;
    }

    private LayerDrawable createCacheItem(final Geocache cache, final int hashcode) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        final ArrayList<Drawable> layers = new ArrayList<Drawable>(9);
        final ArrayList<int[]> insets = new ArrayList<int[]>(8);

        // background: disabled or not
        final Drawable marker = getResources().getDrawable(cache.getMapMarkerId());
        layers.add(marker);
        final int resolution = marker.getIntrinsicWidth() > 40 ? (marker.getIntrinsicWidth() > 50 ? 2 : 1) : 0;
        // reliable or not
        if (!cache.isReliableLatLon()) {
            insets.add(INSET_RELIABLE[resolution]);
            layers.add(getResources().getDrawable(R.drawable.marker_notreliable));
        }
        // cache type
        layers.add(getResources().getDrawable(cache.getType().markerId));
        insets.add(INSET_TYPE[resolution]);
        // own
        if (cache.isOwner()) {
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

        overlaysCache.put(hashcode, ld);
        return ld;
    }

    private CachesOverlayItemImpl getWaypointItem(final Waypoint waypoint) {
        final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(waypoint, waypoint.getWaypointType().applyDistanceRule());
        Drawable marker = getResources().getDrawable(!waypoint.isVisited() ? R.drawable.marker : R.drawable.marker_transparent);
        final Drawable[] layers = new Drawable[] {
                marker,
                getResources().getDrawable(waypoint.getWaypointType().markerId)
        };
        final LayerDrawable ld = new LayerDrawable(layers);
        if (layers[0].getIntrinsicWidth() > 40) {
            ld.setLayerInset(1, 9, 12, 10, 13);
        } else {
            ld.setLayerInset(1, 9, 12, 8, 12);
        }
        item.setMarker(ld);
        return item;
    }

}
