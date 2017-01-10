package cgeo.geocaching.maps;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LeastRecentlyUsedSet;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Class representing the Map in c:geo
 */
public class CGeoMap extends AbstractMap implements ViewFactory {

    /** max. number of caches displayed in the Live Map */
    public static final int MAX_CACHES = 500;
    /**
     * initialization with an empty subscription to make static code analysis tools more happy
     */
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();

    /** Handler Messages */
    private static final int HIDE_PROGRESS = 0;
    private static final int SHOW_PROGRESS = 1;
    private static final int UPDATE_TITLE = 0;
    private static final int INVALIDATE_MAP = 1;
    private static final int UPDATE_PROGRESS = 0;
    private static final int FINISHED_LOADING_DETAILS = 1;

    private static final String BUNDLE_MAP_SOURCE = "mapSource";
    private static final String BUNDLE_MAP_STATE = "mapState";
    private static final String BUNDLE_LIVE_ENABLED = "liveEnabled";
    private static final String BUNDLE_TRAIL_HISTORY = "trailHistory";

    // Those are initialized in onCreate() and will never be null afterwards
    private Resources res;
    private Activity activity;
    private MapItemFactory mapItemFactory;
    private final LeastRecentlyUsedSet<Geocache> caches = new LeastRecentlyUsedSet<>(MAX_CACHES + DataStore.getAllCachesCount());
    private MapViewImpl mapView;
    private CachesOverlay overlayCaches;
    private PositionAndScaleOverlay overlayPositionAndScale;

    private final GeoDirHandler geoDirUpdate = new UpdateLoc(this);
    // status data
    /** Last search result used for displaying header */
    private SearchResult lastSearchResult = null;
    private MapTokens tokens = null;
    private boolean noMapTokenShowed = false;
    // map status data
    private static boolean followMyLocation = true;
    // threads
    private Disposable loadTimer;
    private LoadDetails loadDetailsThread = null;
    /** Time of last {@link LoadRunnable} run */
    private volatile long loadThreadRun = 0L;
    //Interthread communication flag
    private volatile boolean downloaded = false;

    /** Count of caches currently visible */
    private int cachesCnt = 0;
    /** List of waypoints in the viewport */
    private final LeastRecentlyUsedSet<Waypoint> waypoints = new LeastRecentlyUsedSet<>(MAX_CACHES);
    // storing for offline
    private ProgressDialog waitDialog = null;
    private int detailTotal = 0;
    private int detailProgress = 0;
    private long detailProgressTime = 0L;

    // views
    private CheckBox myLocSwitch = null;
    // other things
    private boolean markersInvalidated = false; // previous state for loadTimer
    private boolean centered = false; // if map is already centered
    private boolean alreadyCentered = false; // -""- for setting my location
    private static final Set<String> dirtyCaches = new HashSet<>();

    /**
     * if live map is enabled, this is the minimum zoom level, independent of the stored setting
     */
    private static final int MIN_LIVEMAP_ZOOM = 12;
    // Thread pooling
    private static final BlockingQueue<Runnable> displayQueue = new ArrayBlockingQueue<>(1);
    private static final ThreadPoolExecutor displayExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, displayQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static final BlockingQueue<Runnable> downloadQueue = new ArrayBlockingQueue<>(1);
    private static final ThreadPoolExecutor downloadExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, downloadQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static final BlockingQueue<Runnable> loadQueue = new ArrayBlockingQueue<>(1);
    private static final ThreadPoolExecutor loadExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, loadQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
    private MapOptions mapOptions;
    // handlers
    /** Updates the titles */

    private static final class DisplayHandler extends Handler {

        private final WeakReference<CGeoMap> mapRef;

        DisplayHandler(@NonNull final CGeoMap map) {
            this.mapRef = new WeakReference<>(map);
        }
        @Override
        public void handleMessage(final Message msg) {
            final CGeoMap map = mapRef.get();
            if (map == null) {
                return;
            }

            final int what = msg.what;
            switch (what) {
                case UPDATE_TITLE:
                    map.setTitle();
                    map.setSubtitle();
                    break;

                case INVALIDATE_MAP:
                    map.mapView.repaintRequired(null);
                    break;

                default:
                    break;
            }
        }

    }

    private final Handler displayHandler = new DisplayHandler(this);

    private void setTitle() {
        getActionBar().setTitle(calculateTitle());
    }

    private String calculateTitle() {
        if (mapOptions.isLiveEnabled) {
            return res.getString(R.string.map_live);
        }
        if (mapOptions.mapMode == MapMode.SINGLE) {
            final Geocache cache = getSingleModeCache();
            if (cache != null) {
                return cache.getName();
            }
        }
        return StringUtils.defaultIfEmpty(mapOptions.title, res.getString(R.string.map_map));
    }

    @Nullable
    private Geocache getSingleModeCache() {
        // use a copy of the caches list to avoid concurrent modification
        for (final Geocache geocache : caches.getAsList()) {
            if (geocache.getGeocode().equals(mapOptions.geocode)) {
                return geocache;
            }
        }
        return null;
    }

    private void setSubtitle() {
        final String subtitle = calculateSubtitle();
        if (StringUtils.isEmpty(subtitle)) {
            return;
        }

        getActionBar().setSubtitle(subtitle);
    }

    private String calculateSubtitle() {
        // count caches in the sub title
        countVisibleCaches();
        if (!mapOptions.isLiveEnabled && mapOptions.mapMode == MapMode.SINGLE) {
            final Geocache cache = getSingleModeCache();
            if (cache != null) {
                return Formatter.formatMapSubtitle(cache);
            }
        }
        final StringBuilder subtitle = new StringBuilder();
        if (!caches.isEmpty()) {
            final int totalCount = caches.size();

            if (cachesCnt != totalCount && Settings.isDebug()) {
                subtitle.append(cachesCnt).append('/').append(res.getQuantityString(R.plurals.cache_counts, totalCount, totalCount));
            } else {
                subtitle.append(res.getQuantityString(R.plurals.cache_counts, cachesCnt, cachesCnt));
            }
        }

        if (Settings.isDebug() && lastSearchResult != null && StringUtils.isNotBlank(lastSearchResult.getUrl())) {
            subtitle.append(" [").append(lastSearchResult.getUrl()).append(']');
        }

        return subtitle.toString();
    }

    @NonNull
    private ActionBar getActionBar() {
        final ActionBar actionBar = activity.getActionBar();
        assert actionBar != null;
        return actionBar;
    }

    /** Updates the progress. */
    private static final class ShowProgressHandler extends Handler {
        private int counter = 0;

        @NonNull private final WeakReference<CGeoMap> mapRef;

        ShowProgressHandler(@NonNull final CGeoMap map) {
            this.mapRef = new WeakReference<>(map);
        }

        @Override
        public void handleMessage(final Message msg) {
            final int what = msg.what;

            if (what == SHOW_PROGRESS) {
                showProgress(true);
                counter++;
            } else if (what == HIDE_PROGRESS && --counter == 0) {
                showProgress(false);
            }
        }
        private void showProgress(final boolean show) {
            final CGeoMap map = mapRef.get();
            if (map == null) {
                return;
            }

            final ProgressBar progress = ButterKnife.findById(map.activity, R.id.actionbar_progress);
            if (progress != null) {
                final int visibility = show ? View.VISIBLE : View.GONE;
                progress.setVisibility(visibility);
            }
            map.activity.setProgressBarIndeterminateVisibility(show);
        }

    }

    private final Handler showProgressHandler = new ShowProgressHandler(this);

    private final class LoadDetailsHandler extends DisposableHandler {

        @Override
        public void handleRegularMessage(final Message msg) {
            if (waitDialog != null) {
                if (msg.what == UPDATE_PROGRESS) {
                    final int secondsElapsed = (int) ((System.currentTimeMillis() - detailProgressTime) / 1000);
                    final int secondsRemaining = (detailTotal - detailProgress) * secondsElapsed / detailProgress;

                    waitDialog.setProgress(detailProgress);
                    if (secondsRemaining < 40) {
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
                    } else {
                        final int minsRemaining = secondsRemaining / 60;
                        waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getQuantityString(R.plurals.caches_eta_mins, minsRemaining, minsRemaining));
                    }
                } else if (msg.what == FINISHED_LOADING_DETAILS) {
                    waitDialog.dismiss();
                    waitDialog.setOnCancelListener(null);
                }
            }
        }
        @Override
        public void handleDispose() {
            if (loadDetailsThread != null) {
                loadDetailsThread.stopIt();
            }
        }

    }

    /* Current source id */
    private int currentSourceId;

    public CGeoMap(final MapActivityImpl activity) {
        super(activity);
    }

    protected void countVisibleCaches() {
        cachesCnt = mapView.getViewport().count(caches.getAsList());
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putInt(BUNDLE_MAP_SOURCE, currentSourceId);
        outState.putParcelable(BUNDLE_MAP_STATE, currentMapState());
        outState.putBoolean(BUNDLE_LIVE_ENABLED, mapOptions.isLiveEnabled);
        outState.putParcelableArrayList(BUNDLE_TRAIL_HISTORY, overlayPositionAndScale.getHistory());
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // class init
        res = this.getResources();
        activity = this.getActivity();

        final MapProvider mapProvider = Settings.getMapProvider();
        mapItemFactory = mapProvider.getMapItemFactory();

        // Get parameters from the intent
        final Bundle extras = activity.getIntent().getExtras();
        mapOptions = new MapOptions(activity, extras);

        ArrayList<Location> trailHistory = null;

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            currentSourceId = savedInstanceState.getInt(BUNDLE_MAP_SOURCE, Settings.getMapSource().getNumericalId());
            mapOptions.mapState = savedInstanceState.getParcelable(BUNDLE_MAP_STATE);
            mapOptions.isLiveEnabled = savedInstanceState.getBoolean(BUNDLE_LIVE_ENABLED, false);
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

        ActivityMixin.onCreate(activity, true);


        // set layout
        ActivityMixin.setTheme(activity);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        activity.setContentView(mapProvider.getMapLayoutId());
        setTitle();

        // initialize map
        mapView = (MapViewImpl) activity.findViewById(mapProvider.getMapViewId());
        mapView.setMapSource();
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);
        mapView.preLoad();
        mapView.setOnDragListener(new MapDragListener(this));

        // initialize overlays
        mapView.clearOverlays();

        overlayCaches = mapView.createAddMapOverlay(mapView.getContext(), Compatibility.getDrawable(getResources(), R.drawable.marker));


        overlayPositionAndScale = mapView.createAddPositionAndScaleOverlay(mapOptions.coords, mapOptions.geocode);
        if (trailHistory != null) {
            overlayPositionAndScale.setHistory(trailHistory);
        }


        mapView.repaintRequired(null);

        setZoom(Settings.getMapZoom(mapOptions.mapMode));
        mapView.getMapController().setCenter(Settings.getMapCenter());

        if (mapOptions.mapState != null) {
            followMyLocation = mapOptions.mapState.followsMyLocation();
            if (overlayCaches.getCircles() != mapOptions.mapState.showsCircles()) {
                overlayCaches.switchCircles();
            }
        } else if (mapOptions.mapMode != MapMode.LIVE) {
            followMyLocation = false;
        }
        if (mapOptions.geocode != null || mapOptions.searchResult != null || mapOptions.coords != null || mapOptions.mapState != null) {
            centerMap(mapOptions.geocode, mapOptions.searchResult, mapOptions.coords, mapOptions.mapState);
        }

        prepareFilterBar();

        LiveMapHint.getInstance().showHint(activity);
        AndroidBeam.disable(activity);
    }

    private void initMyLocationSwitchButton(final CheckBox locSwitch) {
        myLocSwitch = locSwitch;
        /* TODO: Switch back to ImageSwitcher for animations?
        myLocSwitch.setFactory(this);
        myLocSwitch.setInAnimation(activity, android.R.anim.fade_in);
        myLocSwitch.setOutAnimation(activity, android.R.anim.fade_out); */
        myLocSwitch.setOnClickListener(new MyLocationListener(this));
        switchMyLocationButton();
    }

    /**
     * Set the zoom of the map. The zoom is restricted to a certain minimum in case of live map.
     *
     */
    private void setZoom(final int zoom) {
        mapView.getMapController().setZoom(mapOptions.isLiveEnabled ? Math.max(zoom, MIN_LIVEMAP_ZOOM) : zoom);
    }

    private void prepareFilterBar() {
        // show the filter warning bar if the filter is set
        if (Settings.getCacheType() != CacheType.ALL) {
            final String cacheType = Settings.getCacheType().getL10n();
            final TextView filterTitleView = ButterKnife.findById(activity, R.id.filter_text);
            filterTitleView.setText(cacheType);
            activity.findViewById(R.id.filter_bar).setVisibility(View.VISIBLE);
        } else {
            activity.findViewById(R.id.filter_bar).setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeDisposables.addAll(geoDirUpdate.start(GeoDirHandler.UPDATE_GEODIR), startTimer());

        final List<String> toRefresh;
        synchronized (dirtyCaches) {
            toRefresh = new ArrayList<>(dirtyCaches);
            dirtyCaches.clear();
        }

        if (!toRefresh.isEmpty()) {
            AndroidRxUtils.refreshScheduler.scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    for (final String geocode: toRefresh) {
                        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                        if (cache != null) {
                            // new collection type needs to remove first
                            caches.remove(cache);
                            // re-add to update the freshness
                            caches.add(cache);
                        }
                    }
                    displayExecutor.execute(new DisplayRunnable(CGeoMap.this));
                }
            });
        }
    }

    @Override
    public void onPause() {
        resumeDisposables.clear();
        savePrefs();

        mapView.destroyDrawingCache();

        MapUtils.clearCachedItems();

        super.onPause();
    }

    @Override
    public void onStop() {
        // Ensure that handlers will not try to update the dialog once the view is detached.
        waitDialog = null;
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);

        MapProviderFactory.addMapviewMenuItems(menu);

        final SubMenu subMenuStrategy = menu.findItem(R.id.submenu_strategy).getSubMenu();
        subMenuStrategy.setHeaderTitle(res.getString(R.string.map_strategy_title));

            /* if we have an Actionbar find the my position toggle */
        final MenuItem item = menu.findItem(R.id.menu_toggle_mypos);
        myLocSwitch = new CheckBox(activity);
        myLocSwitch.setButtonDrawable(R.drawable.ic_menu_myposition);
        item.setActionView(myLocSwitch);
        initMyLocationSwitchButton(myLocSwitch);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (final MapSource mapSource : MapProviderFactory.getMapSources()) {
            final MenuItem menuItem = menu.findItem(mapSource.getNumericalId());
            if (menuItem != null) {
                menuItem.setVisible(mapSource.isAvailable());
            }
        }

        try {
            final MenuItem itemMapLive = menu.findItem(R.id.menu_map_live);
            final int titleResource = mapOptions.isLiveEnabled ? R.string.map_live_disable : R.string.map_live_enable;
            itemMapLive.setTitle(res.getString(titleResource));
            itemMapLive.setVisible(mapOptions.coords == null);


            final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
            menu.findItem(R.id.menu_store_caches).setVisible(!isLoading() && CollectionUtils.isNotEmpty(geocodesInViewport) && new SearchResult(geocodesInViewport).hasUnsavedCaches());

            menu.findItem(R.id.menu_mycaches_mode).setChecked(Settings.isExcludeMyCaches());
            menu.findItem(R.id.menu_disabled_mode).setChecked(Settings.isExcludeDisabledCaches());
            menu.findItem(R.id.menu_direction_line).setChecked(Settings.isMapDirection());
            menu.findItem(R.id.menu_circle_mode).setChecked(overlayCaches.getCircles());
            menu.findItem(R.id.menu_trail_mode).setChecked(Settings.isMapTrail());

            menu.findItem(R.id.menu_theme_mode).setVisible(mapView.hasMapThemes());

            menu.findItem(R.id.menu_as_list).setVisible(!isLoading() && caches.size() > 1);

            menu.findItem(R.id.submenu_strategy).setVisible(mapOptions.isLiveEnabled);

            switch (Settings.getLiveMapStrategy()) {
                case FAST:
                    menu.findItem(R.id.menu_strategy_fast).setChecked(true);
                    break;
                case AUTO:
                    menu.findItem(R.id.menu_strategy_auto).setChecked(true);
                    break;
                default: // DETAILED
                    menu.findItem(R.id.menu_strategy_detailed).setChecked(true);
            }

            menu.findItem(R.id.submenu_routing).setVisible(Routing.isAvailable());
            switch (Settings.getRoutingMode()) {
                case STRAIGHT:
                    menu.findItem(R.id.menu_routing_straight).setChecked(true);
                    break;
                case WALK:
                    menu.findItem(R.id.menu_routing_walk).setChecked(true);
                    break;
                case BIKE:
                    menu.findItem(R.id.menu_routing_bike).setChecked(true);
                    break;
                case CAR:
                    menu.findItem(R.id.menu_routing_car).setChecked(true);
                    break;
            }
            menu.findItem(R.id.menu_hint).setVisible(mapOptions.mapMode == MapMode.SINGLE);
            menu.findItem(R.id.menu_compass).setVisible(mapOptions.mapMode == MapMode.SINGLE);
        } catch (final RuntimeException e) {
            Log.e("CGeoMap.onPrepareOptionsMenu", e);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                ActivityMixin.navigateUp(activity);
                return true;
            case R.id.menu_trail_mode:
                Settings.setMapTrail(!Settings.isMapTrail());
                mapView.repaintRequired(overlayPositionAndScale);
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_direction_line:
                Settings.setMapDirection(!Settings.isMapDirection());
                mapView.repaintRequired(overlayPositionAndScale);
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_map_live:
                mapOptions.isLiveEnabled = !mapOptions.isLiveEnabled;
                if (mapOptions.mapMode == MapMode.LIVE) {
                    Settings.setLiveMap(mapOptions.isLiveEnabled);
                }
                markersInvalidated = true;
                lastSearchResult = null;
                mapOptions.searchResult = null;
                ActivityMixin.invalidateOptionsMenu(activity);
                if (mapOptions.mapMode != MapMode.SINGLE) {
                    mapOptions.title = StringUtils.EMPTY;
                }
                updateMapTitle();
                return true;
            case R.id.menu_store_caches:
                if (!isLoading()) {
                    final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
                    final List<String> geocodes = new ArrayList<>();

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
                        new StoredList.UserInterface(activity).promptForMultiListSelection(R.string.list_title, new Action1<Set<Integer>>() {
                                    @Override
                            public void call(final Set<Integer> selectedListIds) {
                                storeCaches(geocodes, selectedListIds);
                                    }
                        }, true, Collections.<Integer>emptySet(), false);
                    } else {
                        storeCaches(geocodes, Collections.singleton(StoredList.STANDARD_LIST_ID));
                    }
                }
                return true;
            case R.id.menu_circle_mode:
                overlayCaches.switchCircles();
                mapView.repaintRequired(overlayCaches);
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_mycaches_mode:
                Settings.setExcludeMine(!Settings.isExcludeMyCaches());
                markersInvalidated = true;
                ActivityMixin.invalidateOptionsMenu(activity);
                if (!Settings.isExcludeMyCaches()) {
                    Tile.cache.clear();
                }
                return true;
            case R.id.menu_disabled_mode:
                Settings.setExcludeDisabled(!Settings.isExcludeDisabledCaches());
                markersInvalidated = true;
                ActivityMixin.invalidateOptionsMenu(activity);
                if (!Settings.isExcludeDisabledCaches()) {
                    Tile.cache.clear();
                }
                return true;
            case R.id.menu_theme_mode:
                selectMapTheme();
                return true;
            case R.id.menu_as_list: {
                CacheListActivity.startActivityMap(activity, new SearchResult(getGeocodesForCachesInViewport()));
                return true;
            }
            case R.id.menu_strategy_fast: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(LivemapStrategy.FAST);
                return true;
            }
            case R.id.menu_strategy_auto: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(LivemapStrategy.AUTO);
                return true;
            }
            case R.id.menu_strategy_detailed: {
                item.setChecked(true);
                Settings.setLiveMapStrategy(LivemapStrategy.DETAILED);
                return true;
            }
            case R.id.menu_routing_straight: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.STRAIGHT);
                mapView.repaintRequired(overlayPositionAndScale);
                return true;
            }
            case R.id.menu_routing_walk: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.WALK);
                mapView.repaintRequired(overlayPositionAndScale);
                return true;
            }
            case R.id.menu_routing_bike: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.BIKE);
                mapView.repaintRequired(overlayPositionAndScale);
                return true;
            }
            case R.id.menu_routing_car: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.CAR);
                mapView.repaintRequired(overlayPositionAndScale);
                return true;
            }
            case R.id.menu_hint:
                menuShowHint();
                return true;
            case R.id.menu_compass:
                menuCompass();
                return true;
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

    private void menuCompass() {
        final Geocache cache = getSingleModeCache();
        if (cache != null) {
            CompassActivity.startActivityCache(this.getActivity(), cache);
        }
    }

    private void menuShowHint() {
        final Geocache cache = getSingleModeCache();
        if (cache != null) {
            cache.showHintToast(getActivity());
        }
    }

    private void selectMapTheme() {

        final File[] themeFiles = Settings.getMapThemeFiles();

        String currentTheme = StringUtils.EMPTY;
        final String currentThemePath = Settings.getCustomRenderThemeFilePath();
        if (StringUtils.isNotEmpty(currentThemePath)) {
            final File currentThemeFile = new File(currentThemePath);
            currentTheme = currentThemeFile.getName();
        }

        final List<String> names = new ArrayList<>();
        names.add(res.getString(R.string.map_theme_builtin));
        int currentItem = 0;
        for (final File file : themeFiles) {
            if (currentTheme.equalsIgnoreCase(file.getName())) {
                currentItem = names.size();
            }
            names.add(file.getName());
        }

        final int selectedItem = currentItem;

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.map_theme_select);

        builder.setSingleChoiceItems(names.toArray(new String[names.size()]), selectedItem,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface dialog, final int newItem) {
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
        final Set<String> geocodes = new HashSet<>();
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
     * @param newSource
     *            the new map source, which can be the same as the current one
     * @return true if a restart is needed, false otherwise
     */
    private boolean changeMapSource(@NonNull final MapSource newSource) {
        final MapSource oldSource = MapProviderFactory.getMapSource(currentSourceId);
        final boolean restartRequired = oldSource == null || !MapProviderFactory.isSameActivity(oldSource, newSource);

        Settings.setMapSource(newSource);
        currentSourceId = newSource.getNumericalId();

        if (restartRequired) {
            mapRestart();
        } else if (mapView != null) {  // changeMapSource can be called by onCreate()
            mapOptions.mapState = currentMapState();
            mapView.setMapSource();
            // re-center the map
            centered = false;
            centerMap(mapOptions.geocode, mapOptions.searchResult, mapOptions.coords, mapOptions.mapState);
            // re-build menues
            ActivityMixin.invalidateOptionsMenu(activity);
        }

        return restartRequired;
    }

    /**
     * Restart the current activity with the default map source.
     */
    private void mapRestart() {
        mapOptions.mapState = currentMapState();
        activity.finish();
        mapOptions.startIntent(activity, Settings.getMapProvider().getMapClass());
    }

    /**
     * Get the current map state from the map view if it exists or from the mapStateIntent field otherwise.
     *
     * @return the current map state as an array of int, or null if no map state is available
     */
    private MapState currentMapState() {
        if (mapView == null) {
            return null;
        }
        final GeoPointImpl mapCenter = mapView.getMapViewCenter();
        return new MapState(mapCenter.getCoords(), mapView.getMapZoomLevel(), followMyLocation, overlayCaches.getCircles(), null, null, mapOptions.isLiveEnabled);
    }

    private void savePrefs() {
        Settings.setMapZoom(mapOptions.mapMode, mapView.getMapZoomLevel());
        Settings.setMapCenter(mapView.getMapViewCenter());
    }

    // Set center of map to my location if appropriate.
    private void myLocationInMiddle(final GeoData geo) {
        if (followMyLocation) {
            centerMap(geo.getCoords());
        }
    }

    // class: update location
    private static class UpdateLoc extends GeoDirHandler {
        // use the following constants for fine tuning - find good compromise between smooth updates and as less updates as possible

        // minimum time in milliseconds between position overlay updates
        private static final long MIN_UPDATE_INTERVAL = 500;
        // minimum change of heading in grad for position overlay update
        private static final float MIN_HEADING_DELTA = 15f;
        // minimum change of location in fraction of map width/height (whatever is smaller) for position overlay update
        private static final float MIN_LOCATION_DELTA = 0.01f;

        Location currentLocation = Sensors.getInstance().currentGeo();
        float currentHeading;

        private long timeLastPositionOverlayCalculation = 0;
        /**
         * weak reference to the outer class
         */
        private final WeakReference<CGeoMap> mapRef;

        UpdateLoc(final CGeoMap map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public void updateGeoDir(@NonNull final GeoData geo, final float dir) {
            currentLocation = geo;
            currentHeading = AngleUtils.getDirectionNow(dir);
            repaintPositionOverlay();
        }

        /**
         * Repaint position overlay but only with a max frequency and if position or heading changes sufficiently.
         */
        void repaintPositionOverlay() {
            final long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > timeLastPositionOverlayCalculation + MIN_UPDATE_INTERVAL) {
                timeLastPositionOverlayCalculation = currentTimeMillis;

                try {
                    final CGeoMap map = mapRef.get();
                    if (map != null) {
                        final boolean needsRepaintForDistanceOrAccuracy = needsRepaintForDistanceOrAccuracy();
                        final boolean needsRepaintForHeading = needsRepaintForHeading();

                        if (needsRepaintForDistanceOrAccuracy && followMyLocation) {
                            map.centerMap(new Geopoint(currentLocation));
                        }

                        if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {

                            map.overlayPositionAndScale.setCoordinates(currentLocation);
                            map.overlayPositionAndScale.setHeading(currentHeading);
                            map.mapView.repaintRequired(map.overlayPositionAndScale);
                        }
                    }
                } catch (final RuntimeException e) {
                    Log.w("Failed to update location", e);
                }
            }
        }

        boolean needsRepaintForHeading() {
            final CGeoMap map = mapRef.get();
            return map != null && Math.abs(AngleUtils.difference(currentHeading, map.overlayPositionAndScale.getHeading())) > MIN_HEADING_DELTA;
        }

        boolean needsRepaintForDistanceOrAccuracy() {
            final CGeoMap map = mapRef.get();
            if (map == null) {
                return false;
            }
            final Location lastLocation = map.overlayPositionAndScale.getCoordinates();

            float dist = Float.MAX_VALUE;
            if (lastLocation != null) {
                if (lastLocation.getAccuracy() != currentLocation.getAccuracy()) {
                    return true;
                }
                dist = currentLocation.distanceTo(lastLocation);
            }

            final float[] mapDimension = new float[1];
            if (map.mapView.getWidth() < map.mapView.getHeight()) {
                final double span = map.mapView.getLongitudeSpan() / 1e6;
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude() + span, mapDimension);
            } else {
                final double span = map.mapView.getLatitudeSpan() / 1e6;
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude() + span, currentLocation.getLongitude(), mapDimension);
            }

            return dist > (mapDimension[0] * MIN_LOCATION_DELTA);
        }
    }

    /**
     * Starts the load timer.
     */

    private Disposable startTimer() {
        if (mapOptions.coords != null) {
            // display just one point
            displayPoint(mapOptions.coords);
            loadTimer = new CompositeDisposable();
        } else {
            loadTimer = Schedulers.newThread().schedulePeriodicallyDirect(new LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
        }
        return loadTimer;
    }

    private static final class LoadTimerAction implements Runnable {

        @NonNull private final WeakReference<CGeoMap> mapRef;
        private int previousZoom = -100;
        private Viewport previousViewport;

        LoadTimerAction(@NonNull final CGeoMap map) {
            this.mapRef = new WeakReference<>(map);
        }

        @Override
        public void run() {
            final CGeoMap map = mapRef.get();
            if (map == null) {
                return;
            }
            try {
                // get current viewport
                final Viewport viewportNow = map.mapView.getViewport();
                // Since zoomNow is used only for local comparison purposes,
                // it is ok to use the Google Maps compatible zoom level of OSM Maps
                final int zoomNow = map.mapView.getMapZoomLevel();

                // check if map moved or zoomed
                //TODO Portree Use Rectangle inside with bigger search window. That will stop reloading on every move
                final boolean moved = map.markersInvalidated || (map.mapOptions.isLiveEnabled && !map.downloaded) || previousViewport == null || zoomNow != previousZoom ||
                        (mapMoved(previousViewport, viewportNow) && (map.cachesCnt <= 0 || CollectionUtils.isEmpty(map.caches) || !previousViewport.includes(viewportNow)));

                // update title on any change
                if (moved || !viewportNow.equals(previousViewport)) {
                    map.updateMapTitle();
                }

                // save new values
                if (moved) {
                    map.markersInvalidated = false;

                    final long currentTime = System.currentTimeMillis();

                    if ((currentTime - map.loadThreadRun) > 1000) {
                        previousViewport = viewportNow;
                        previousZoom = zoomNow;
                        loadExecutor.execute(new LoadRunnable(map));
                    }
                }
            } catch (final Exception e) {
                Log.w("CGeoMap.startLoadtimer.start", e);
            }
        }
    }

    /**
     * get if map is loading something
     *
     */
    private boolean isLoading() {
        return !loadTimer.isDisposed() &&
                (loadExecutor.getActiveCount() > 0 ||
                        downloadExecutor.getActiveCount() > 0 ||
                        displayExecutor.getActiveCount() > 0);
    }

    /**
     * Worker thread that loads caches and waypoints from the database and then spawns the {@link DownloadRunnable}.
     * started by the load timer.
     */

    private static class LoadRunnable extends DoRunnable {

        LoadRunnable(@NonNull final CGeoMap map) {
            super(map);
        }

        @Override
        public void runWithMap(final CGeoMap map) {
            map.doLoadRun();
        }
    }

    private void doLoadRun() {
        try {
            showProgressHandler.sendEmptyMessage(SHOW_PROGRESS);
            loadThreadRun = System.currentTimeMillis();

            final SearchResult searchResult;
            final MapMode mapMode = mapOptions.mapMode;
            if (mapMode == MapMode.LIVE) {
                searchResult = mapOptions.isLiveEnabled ? new SearchResult() : new SearchResult(DataStore.loadStoredInViewport(mapView.getViewport(), Settings.getCacheType()));
            } else {
                // map started from another activity
                searchResult = mapOptions.searchResult != null ? new SearchResult(mapOptions.searchResult) : new SearchResult();
                if (mapOptions.geocode != null) {
                    searchResult.addGeocode(mapOptions.geocode);
                }
            }
            // live mode search result
            if (mapOptions.isLiveEnabled) {
                searchResult.addSearchResult(DataStore.loadCachedInViewport(mapView.getViewport(), Settings.getCacheType()));
            }

            downloaded = true;
            final Set<Geocache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);
            // update the caches
            // new collection type needs to remove first
            caches.removeAll(cachesFromSearchResult);
            caches.addAll(cachesFromSearchResult);

            final boolean excludeMine = Settings.isExcludeMyCaches();
            final boolean excludeDisabled = Settings.isExcludeDisabledCaches();
            if (mapMode == MapMode.LIVE) {
                synchronized (caches) {
                    filter(caches);
                }
            }
            countVisibleCaches();
            // we don't want to see any stale waypoints
            waypoints.clear();
            if (cachesCnt < Settings.getWayPointsThreshold() || mapOptions.geocode != null) {
                if (mapOptions.isLiveEnabled || mapMode == MapMode.LIVE || mapMode == MapMode.COORDS) {
                    //All visible waypoints
                    final CacheType type = Settings.getCacheType();
                    final Set<Waypoint> waypointsInViewport = DataStore.loadWaypoints(mapView.getViewport(), excludeMine, excludeDisabled, type);
                    waypoints.addAll(waypointsInViewport);
                } else {
                    //All waypoints from the viewed caches
                    for (final Geocache c : caches.getAsList()) {
                        waypoints.addAll(c.getWaypoints());
                    }
                }
            }

            //render
            displayExecutor.execute(new DisplayRunnable(this));

            if (mapOptions.isLiveEnabled) {
                downloadExecutor.execute(new DownloadRunnable(this));
            }
            lastSearchResult = searchResult;
        } finally {
            showProgressHandler.sendEmptyMessage(HIDE_PROGRESS); // hide progress
        }

    }

    /**
     * Worker thread downloading caches from the Internet.
     * Started by {@link LoadRunnable}.
     */

    private static class DownloadRunnable extends DoRunnable {

        DownloadRunnable(final CGeoMap map) {
            super(map);
        }

        @Override
        public void runWithMap(final CGeoMap map) {
            map.doDownloadRun();
        }
    }

    private void doDownloadRun() {
        try {
            showProgressHandler.sendEmptyMessage(SHOW_PROGRESS); // show progress
            if (Settings.isGCConnectorActive() && tokens == null) {
                tokens = GCLogin.getInstance().getMapTokens();
                if (StringUtils.isEmpty(tokens.getUserSession()) || StringUtils.isEmpty(tokens.getSessionToken())) {
                    tokens = null;
                    if (!noMapTokenShowed) {
                        ActivityMixin.showToast(activity, res.getString(R.string.map_token_err));
                        noMapTokenShowed = true;
                    }
                }
            }
            final SearchResult searchResult = ConnectorFactory.searchByViewport(mapView.getViewport().resize(0.8), tokens);
            downloaded = true;

            final Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            filter(result);
            // update the caches
            // first remove filtered out
            final Set<String> filteredCodes = searchResult.getFilteredGeocodes();
            Log.d("Filtering out " + filteredCodes.size() + " caches: " + filteredCodes.toString());
            caches.removeAll(DataStore.loadCaches(filteredCodes, LoadFlags.LOAD_CACHE_ONLY));
            DataStore.removeCaches(filteredCodes, EnumSet.of(RemoveFlag.CACHE));
            // new collection type needs to remove first to refresh
            caches.removeAll(result);
            caches.addAll(result);
            lastSearchResult = searchResult;

            //render
            displayExecutor.execute(new DisplayRunnable(this));

        } finally {
            showProgressHandler.sendEmptyMessage(HIDE_PROGRESS); // hide progress
        }
    }

    /**
     * Thread to Display (down)loaded caches. Started by {@link LoadRunnable} and {@link DownloadRunnable}
     */
    private static class DisplayRunnable extends DoRunnable {

        DisplayRunnable(@NonNull final CGeoMap map) {
            super(map);
        }

        @Override
        public void runWithMap(final CGeoMap map) {
            map.doDisplayRun();
        }
    }

    private void doDisplayRun() {
        try {
            showProgressHandler.sendEmptyMessage(SHOW_PROGRESS);

            // display caches
            final List<Geocache> cachesToDisplay = caches.getAsList();
            final List<Waypoint> waypointsToDisplay = new ArrayList<>(waypoints);
            final List<CachesOverlayItemImpl> itemsToDisplay = new ArrayList<>();

            if (!cachesToDisplay.isEmpty()) {
                // Only show waypoints for single view or setting
                // when less than showWaypointsthreshold Caches shown
                if (mapOptions.mapMode == MapMode.SINGLE || cachesCnt < Settings.getWayPointsThreshold()) {
                    for (final Waypoint waypoint : waypointsToDisplay) {
                        if (waypoint != null && waypoint.getCoords() != null) {
                            itemsToDisplay.add(getWaypointItem(waypoint));
                        }
                    }
                }
                for (final Geocache cache : cachesToDisplay) {
                    if (cache != null && cache.getCoords() != null) {
                        itemsToDisplay.add(getCacheItem(cache));
                    }
                }
            }
            // don't add other waypoints to overlayCaches if just one point should be displayed
            if (mapOptions.coords == null) {
                overlayCaches.updateItems(itemsToDisplay);
            }
            displayHandler.sendEmptyMessage(INVALIDATE_MAP);

            updateMapTitle();
        } finally {
            showProgressHandler.sendEmptyMessage(HIDE_PROGRESS);
        }
    }

    private void displayPoint(final Geopoint coords) {
        final Waypoint waypoint = new Waypoint("some place", mapOptions.waypointType != null ? mapOptions.waypointType : WaypointType.WAYPOINT, false);
        waypoint.setCoords(coords);

        final CachesOverlayItemImpl item = getWaypointItem(waypoint);
        overlayCaches.updateItems(item);
        displayHandler.sendEmptyMessage(INVALIDATE_MAP);
        updateMapTitle();

        cachesCnt = 1;
    }

    private void updateMapTitle() {
        displayHandler.sendEmptyMessage(UPDATE_TITLE);
    }

    private abstract static class DoRunnable implements Runnable {

        private final WeakReference<CGeoMap> mapRef;

        protected DoRunnable(@NonNull final CGeoMap map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public final void run() {
            final CGeoMap map = mapRef.get();
            if (map != null) {
                runWithMap(map);
            }
        }

        protected abstract void runWithMap(final CGeoMap map);
    }

    /**
     * store caches, invoked by "store offline" menu item
     *
     * @param listIds
     *            the lists to store the caches in
     */
    private void storeCaches(final List<String> geocodes, final Set<Integer> listIds) {
        final LoadDetailsHandler loadDetailsHandler = new LoadDetailsHandler();

        waitDialog = new ProgressDialog(activity);
        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        waitDialog.setCancelable(true);
        waitDialog.setCancelMessage(loadDetailsHandler.disposeMessage());
        waitDialog.setMax(detailTotal);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(final DialogInterface arg0) {
                try {
                    if (loadDetailsThread != null) {
                        loadDetailsThread.stopIt();
                    }
                } catch (final Exception e) {
                    Log.e("CGeoMap.storeCaches.onCancel", e);
                }
            }
        });

        final float etaTime = detailTotal * 7.0f / 60.0f;
        final int roundedEta = Math.round(etaTime);
        if (etaTime < 0.4) {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
        } else {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getQuantityString(R.plurals.caches_eta_mins, roundedEta, roundedEta));
        }
        waitDialog.show();

        detailProgressTime = System.currentTimeMillis();

        loadDetailsThread = new LoadDetails(loadDetailsHandler, geocodes, listIds);
        loadDetailsThread.start();
    }

    /**
     * Thread to store the caches in the viewport. Started by Activity.
     */

    private class LoadDetails extends Thread {

        private final DisposableHandler handler;
        private final List<String> geocodes;
        private final Set<Integer> listIds;

        LoadDetails(final DisposableHandler handler, final List<String> geocodes, final Set<Integer> listIds) {
            this.handler = handler;
            this.geocodes = geocodes;
            this.listIds = listIds;
        }

        public void stopIt() {
            handler.dispose();
        }

        @Override
        public void run() {
            if (CollectionUtils.isEmpty(geocodes)) {
                return;
            }

            for (final String geocode : geocodes) {
                try {
                    if (handler.isDisposed()) {
                        break;
                    }

                    if (!DataStore.isOffline(geocode, null)) {
                        Geocache.storeCache(null, geocode, listIds, false, handler);
                    }
                } catch (final Exception e) {
                    Log.e("CGeoMap.LoadDetails.run", e);
                } finally {
                    // one more cache over
                    detailProgress++;
                    handler.sendEmptyMessage(UPDATE_PROGRESS);
                }
            }

            // we're done
            handler.sendEmptyMessage(FINISHED_LOADING_DETAILS);
        }
    }

    private static synchronized void filter(final Collection<Geocache> caches) {
        final boolean excludeMine = Settings.isExcludeMyCaches();
        final boolean excludeDisabled = Settings.isExcludeDisabledCaches();

        final List<Geocache> removeList = new ArrayList<>();
        for (final Geocache cache : caches) {
            if ((excludeMine && (cache.isFound() || cache.isOwner())) || (excludeDisabled && (cache.isDisabled() || cache.isArchived()))) {
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
    private void centerMap(final String geocodeCenter, final SearchResult searchCenter, final Geopoint coordsCenter, final MapState mapState) {
        final MapControllerImpl mapController = mapView.getMapController();

        if (!centered && mapState != null) {
            try {
                mapController.setCenter(mapItemFactory.getGeoPointBase(mapState.getCenter()));
                setZoom(mapState.getZoomLevel());
            } catch (final RuntimeException e) {
                Log.e("centermap", e);
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && (geocodeCenter != null || mapOptions.searchResult != null)) {
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
            } catch (final RuntimeException e) {
                Log.e("centermap", e);
            }

            centered = true;
            alreadyCentered = true;
        } else if (!centered && coordsCenter != null) {
            try {
                mapController.setCenter(makeGeoPoint(coordsCenter));
            } catch (final Exception e) {
                Log.e("centermap", e);
            }

            centered = true;
            alreadyCentered = true;
        }
    }

    // switch My Location button image
    private void switchMyLocationButton() {
        // FIXME: temporary workaround for the absence of "follow my location" on Android 3.x (see issue #4289).
        if (myLocSwitch != null) {
            myLocSwitch.setChecked(followMyLocation);
            if (followMyLocation) {
                myLocationInMiddle(Sensors.getInstance().currentGeo());
            }
        }
    }

    // set my location listener
    private static class MyLocationListener implements View.OnClickListener {

        private final WeakReference<CGeoMap> mapRef;

        MyLocationListener(@NonNull final CGeoMap map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public void onClick(final View view) {
            final CGeoMap map = mapRef.get();
            if (map != null) {
                map.onFollowMyLocationClicked();
            }
        }
    }

    private void onFollowMyLocationClicked() {
        followMyLocation = !followMyLocation;
        switchMyLocationButton();
    }

    public static class MapDragListener implements OnMapDragListener {

        private final WeakReference<CGeoMap> mapRef;

        public MapDragListener(@NonNull final CGeoMap map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public void onDrag() {
            final CGeoMap map = mapRef.get();
            if (map != null) {
                map.onDrag();
            }
        }

    }

    private void onDrag() {
        if (followMyLocation) {
            followMyLocation = false;
            switchMyLocationButton();
        }
    }

    // make geopoint
    private GeoPointImpl makeGeoPoint(final Geopoint coords) {
        return mapItemFactory.getGeoPointBase(coords);
    }

    @Override
    public View makeView() {
        final ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ScaleType.CENTER);
        imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return imageView;
    }

    public static void markCacheAsDirty(final String geocode) {
        synchronized (dirtyCaches) {
            dirtyCaches.add(geocode);
        }
    }

    private CachesOverlayItemImpl getCacheItem(final Geocache cache) {
        final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(cache, cache.applyDistanceRule());
        item.setMarker(MapUtils.getCacheMarker(getResources(), cache));
        return item;
    }

    private CachesOverlayItemImpl getWaypointItem(final Waypoint waypoint) {
        final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(waypoint, waypoint.getWaypointType().applyDistanceRule());
        item.setMarker(MapUtils.getWaypointMarker(getResources(), waypoint));
        return item;
    }

}
