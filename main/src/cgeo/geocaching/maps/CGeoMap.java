package cgeo.geocaching.maps;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CachePopup;
import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.WaypointPopup;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCMap;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.ProximityNotification;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.location.WaypointDistanceInfo;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnCacheTapListener;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LeastRecentlyUsedSet;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Class representing the Map in c:geo
 */
public class CGeoMap extends AbstractMap implements ViewFactory, OnCacheTapListener {

    /**
     * max. number of caches displayed in the Live Map
     */
    public static final int MAX_CACHES = 500;
    /**
     * initialization with an empty subscription to make static code analysis tools more happy
     */
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();

    /**
     * Handler Messages
     */
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
    private static final String BUNDLE_PROXIMITY_NOTIFICATION = "proximityNotification";

    // Those are initialized in onCreate() and will never be null afterwards
    private Resources res;
    private Activity activity;
    private MapItemFactory mapItemFactory;
    private final LeastRecentlyUsedSet<Geocache> caches = new LeastRecentlyUsedSet<>(MAX_CACHES + DataStore.getAllCachesCount());
    private MapViewImpl<CachesOverlayItemImpl> mapView;
    private PositionAndHistory overlayPositionAndScale;
    private final Progress progress = new Progress();


    private final GeoDirHandler geoDirUpdate = new UpdateLoc(this);
    private ProximityNotification proximityNotification;
    // status data
    /**
     * Last search result used for displaying header
     */
    private SearchResult lastSearchResult = null;
    private boolean noMapTokenShowed = false;
    // map status data
    private static boolean followMyLocation = true;
    // threads
    private Disposable loadTimer;
    private LoadDetails loadDetailsThread = null;
    /**
     * Time of last {@link LoadRunnable} run
     */
    private volatile long loadThreadRun = 0L;
    //Interthread communication flag
    private volatile boolean downloaded = false;

    /**
     * Count of caches currently visible
     */
    private int cachesCnt = 0;
    /**
     * List of waypoints in the viewport
     */
    private final LeastRecentlyUsedSet<Waypoint> waypoints = new LeastRecentlyUsedSet<>(MAX_CACHES);
    // storing for offline
    private ProgressDialog waitDialog = null;
    private int detailTotal = 0;
    private int detailProgress = 0;
    private long detailProgressTime = 0L;
    private ProgressBar spinner;

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

    /**
     * Updates the titles
     */

    private static final class DisplayHandler extends WeakReferenceHandler<CGeoMap> {

        DisplayHandler(@NonNull final CGeoMap map) {
            super(map);
        }

        @Override
        public void handleMessage(final Message msg) {
            final CGeoMap map = getReference();
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

    /**
     * Updates the progress.
     */
    private static final class ShowProgressHandler extends Handler {
        private int counter = 0;

        @NonNull
        private final WeakReference<CGeoMap> mapRef;

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
            map.spinner.setVisibility(show ? View.VISIBLE : View.GONE);
        }

    }

    private final Handler showProgressHandler = new ShowProgressHandler(this);

    private static final class LoadDetailsHandler extends DisposableHandler {
        private final WeakReference<CGeoMap> mapRef;

        LoadDetailsHandler(final CGeoMap map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            final CGeoMap map = mapRef.get();
            if (map != null) {
                final ProgressDialog waitDialog = map.waitDialog;
                if (waitDialog != null) {
                    if (msg.what == UPDATE_PROGRESS) {
                        final int detailProgress = map.detailProgress;
                        final int secondsElapsed = (int) ((System.currentTimeMillis() - map.detailProgressTime) / 1000);
                        // FIXME: the Math.max below is purely defensive programming around an issue reported
                        // in https://github.com/cgeo/cgeo/issues/6447. This code should be rewritten to, at least,
                        // no longer use global variables to pass information between the handler and its user.
                        final int secondsRemaining = (map.detailTotal - detailProgress) * secondsElapsed / Math.max(detailProgress, 1);

                        final Resources res = map.res;
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
        }

        @Override
        public void handleDispose() {
            final CGeoMap map = mapRef.get();
            if (map != null) {
                final LoadDetails loadDetailsThread = map.loadDetailsThread;
                if (loadDetailsThread != null) {
                    loadDetailsThread.stopIt();
                }
            }
        }

    }

    /* Current source id */
    private int currentSourceId;

    public CGeoMap(final MapActivityImpl activity) {
        super(activity);
    }

    protected void countVisibleCaches() {
        final Viewport viewport = mapView.getViewport();
        cachesCnt =  viewport == null ? caches.size() : viewport.count(caches.getAsList());
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        mapView.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_MAP_SOURCE, currentSourceId);
        outState.putParcelable(BUNDLE_MAP_STATE, currentMapState());
        outState.putBoolean(BUNDLE_LIVE_ENABLED, mapOptions.isLiveEnabled);
        outState.putParcelableArrayList(BUNDLE_TRAIL_HISTORY, overlayPositionAndScale == null ? new ArrayList<Parcelable>() : overlayPositionAndScale.getHistory());
        if (proximityNotification != null) {
            outState.putParcelable(BUNDLE_PROXIMITY_NOTIFICATION, proximityNotification);
        }
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
    }

    protected Viewport getIntentViewport() {
        if (mapOptions.coords != null) {
            return new Viewport(mapOptions.coords);
        } else
        if (mapOptions.geocode != null) {
            return DataStore.getBounds(mapOptions.geocode, true);
        }
        return null;
    }

    protected void initializeMap(final ArrayList<Location> trailHistory) {

        mapView.setMapSource();
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);
        mapView.setOnDragListener(new MapDragListener(this));

        // initialize overlays
        mapView.clearOverlays();

        mapView.setOnTapListener(this);

        final Geopoint savedCoords = mapOptions.coords;  // remember for centerMap
        if (mapOptions.mapMode == MapMode.LIVE && savedCoords != null) {
            mapOptions.coords = null;   // no direction line, even if enabled in settings
            followMyLocation = false;   // do not center on GPS position, even if in LIVE mode
        }

        final Viewport viewport = getIntentViewport();
        overlayPositionAndScale = mapView.createAddPositionAndScaleOverlay(viewport != null ? viewport.center : null, mapOptions.geocode);
        if (trailHistory != null) {
            overlayPositionAndScale.setHistory(trailHistory);
        }

        // prepare circular progress spinner
        spinner = (ProgressBar) activity.findViewById(R.id.map_progressbar);
        spinner.setVisibility(View.GONE);

        mapView.repaintRequired(null);

        if (mapOptions.geocode != null && !viewport.topRight.equals(viewport.bottomLeft)) {
            mapView.zoomToBounds(viewport, mapItemFactory.getGeoPointBase(viewport.center));
        } else {
            setZoom(Settings.getMapZoom(mapOptions.mapMode));
            mapView.getMapController().setCenter(Settings.getMapCenter());
        }

        if (mapOptions.mapState == null) {
            followMyLocation = followMyLocation && (mapOptions.mapMode == MapMode.LIVE);
            mapView.setCircles(Settings.getCircles());
        } else {
            followMyLocation = mapOptions.mapState.followsMyLocation();
            if (mapView.getCircles() != mapOptions.mapState.showsCircles()) {
                mapView.setCircles(mapOptions.mapState.showsCircles());
            }
        }
        if (mapOptions.geocode != null || mapOptions.searchResult != null || savedCoords != null || mapOptions.mapState != null) {
            centerMap(mapOptions.geocode, mapOptions.searchResult, savedCoords, mapOptions.mapState);
        }
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

        final ArrayList<Location> trailHistory;

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            currentSourceId = savedInstanceState.getInt(BUNDLE_MAP_SOURCE, Settings.getMapSource().getNumericalId());
            mapOptions.mapState = savedInstanceState.getParcelable(BUNDLE_MAP_STATE);
            mapOptions.isLiveEnabled = savedInstanceState.getBoolean(BUNDLE_LIVE_ENABLED, false);
            trailHistory = savedInstanceState.getParcelableArrayList(BUNDLE_TRAIL_HISTORY);
            proximityNotification = savedInstanceState.getParcelable(BUNDLE_PROXIMITY_NOTIFICATION);
        } else {
            currentSourceId = Settings.getMapSource().getNumericalId();
            proximityNotification = Settings.isGeneralProximityNotificationActive() ? new ProximityNotification(true, false) : null;
            trailHistory = null;
        }
        if (null != proximityNotification) {
            proximityNotification.setTextNotifications(activity);
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

        // added keys must be removed before passing bundle to google's mapView, otherwise this will be thrown:
        // ClassNotFoundException: Didn't find class "cgeo.geocaching.sensors.GeoData"
        // solution from http://stackoverflow.com/questions/13900322/badparcelableexception-in-google-maps-code/15572337#15572337
        if (savedInstanceState != null) {
            savedInstanceState.remove(BUNDLE_MAP_SOURCE);
            savedInstanceState.remove(BUNDLE_MAP_STATE);
            savedInstanceState.remove(BUNDLE_TRAIL_HISTORY);
        }
        mapView.onCreate(savedInstanceState);

        mapView.onMapReady(() -> initializeMap(trailHistory));

        prepareFilterBar();

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
     */
    private void setZoom(final int zoom) {
        mapView.getMapController().setZoom(mapOptions.isLiveEnabled ? Math.max(zoom, MIN_LIVEMAP_ZOOM) : zoom);
    }

    private void prepareFilterBar() {
        // show the filter warning bar if the filter is set
        if (Settings.getCacheType() != CacheType.ALL) {
            final String cacheType = Settings.getCacheType().getL10n();
            final TextView filterTitleView = activity.findViewById(R.id.filter_text);
            filterTitleView.setText(cacheType);
            activity.findViewById(R.id.filter_bar).setVisibility(View.VISIBLE);
        } else {
            activity.findViewById(R.id.filter_bar).setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // resume location access
        PermissionHandler.executeIfLocationPermissionGranted(this.activity,
                new RestartLocationPermissionGrantedCallback(PermissionRequestContext.CGeoMap) {

                    @Override
                    public void executeAfter() {
                        mapView.onResume();
                        resumeDisposables.addAll(geoDirUpdate.start(GeoDirHandler.UPDATE_GEODIR), startTimer());
                    }
                });

        final List<String> toRefresh;
        synchronized (dirtyCaches) {
            toRefresh = new ArrayList<>(dirtyCaches);
            dirtyCaches.clear();
        }

        if (!toRefresh.isEmpty()) {
            AndroidRxUtils.refreshScheduler.scheduleDirect(() -> {
                for (final String geocode : toRefresh) {
                    final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                    if (cache != null) {
                        // new collection type needs to remove first
                        caches.remove(cache);
                        // re-add to update the freshness
                        caches.add(cache);
                    }
                }
                displayExecutor.execute(new DisplayRunnable(CGeoMap.this));
            });
        }
    }

    @Override
    public void onPause() {
        resumeDisposables.clear();
        savePrefs();

        mapView.destroyDrawingCache();

        // do not clear cached items - BitmapDescriptoCache needs same Drawables, not new ones created
        // after cache is cleared, or TODO implement ComparableDrawable?
        MapMarkerUtils.clearCachedItems();

        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        // Ensure that handlers will not try to update the dialog once the view is detached.
        waitDialog = null;
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) { // avoid occasionally NPE
            mapView.onDestroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);

        MapProviderFactory.addMapviewMenuItems(menu);

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
            itemMapLive.setVisible(mapOptions.coords == null || mapOptions.mapMode == MapMode.LIVE);

            final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
            menu.findItem(R.id.menu_store_caches).setVisible(!isLoading() && CollectionUtils.isNotEmpty(geocodesInViewport));
            menu.findItem(R.id.menu_store_unsaved_caches).setVisible(!isLoading() && CollectionUtils.isNotEmpty(getUnsavedGeocodes(geocodesInViewport)));

            menu.findItem(R.id.menu_mycaches_mode).setChecked(Settings.isExcludeMyCaches());
            menu.findItem(R.id.menu_disabled_mode).setChecked(Settings.isExcludeDisabledCaches());
            menu.findItem(R.id.menu_direction_line).setChecked(Settings.isMapDirection());
            menu.findItem(R.id.menu_circle_mode).setChecked(Settings.getCircles());
            menu.findItem(R.id.menu_trail_mode).setChecked(Settings.isMapTrail());
            menu.findItem(R.id.menu_dot_mode).setChecked(Settings.isDotMode());

            menu.findItem(R.id.menu_theme_mode).setVisible(mapView.hasMapThemes());

            menu.findItem(R.id.menu_as_list).setVisible(!isLoading() && caches.size() > 1);

            menu.findItem(R.id.menu_clear_trailhistory).setVisible(Settings.isMapTrail());

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
                mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_dot_mode:
                Settings.setDotMode(!Settings.isDotMode());
                markersInvalidated = true;
                mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_map_autorotate:
                Settings.setMapAutoRotationDisabled(!Settings.isMapAutoRotationDisabled());
                overlayPositionAndScale.updateMapAutoRotation();
                ActivityMixin.invalidateOptionsMenu(activity);
                return true;
            case R.id.menu_direction_line:
                Settings.setMapDirection(!Settings.isMapDirection());
                mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
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
                return storeCaches(getGeocodesForCachesInViewport());
            case R.id.menu_store_unsaved_caches:
                return storeCaches(getUnsavedGeocodes(getGeocodesForCachesInViewport()));
            case R.id.menu_circle_mode:
                Settings.setCircles(!Settings.getCircles());
                mapView.setCircles(Settings.getCircles());
                mapView.repaintRequired(null);
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
            case R.id.menu_clear_trailhistory: {
                DataStore.clearTrailHistory();
                overlayPositionAndScale.setHistory(new ArrayList<Location>());
                mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
                ActivityMixin.showToast(activity, res.getString(R.string.map_trailhistory_cleared));
                return true;
            }
            case R.id.menu_routing_straight: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.STRAIGHT);
                mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
                return true;
            }
            case R.id.menu_routing_walk: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.WALK);
                mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
                return true;
            }
            case R.id.menu_routing_bike: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.BIKE);
                mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
                return true;
            }
            case R.id.menu_routing_car: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.CAR);
                mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
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

    private boolean storeCaches(final Set<String> geocodesInViewport) {
        if (!isLoading()) {

            detailTotal = geocodesInViewport.size();
            detailProgress = 0;

            if (detailTotal == 0) {
                ActivityMixin.showToast(activity, res.getString(R.string.warn_save_nothing));

                return true;
            }

            if (Settings.getChooseList()) {
                // let user select list to store cache in
                new StoredList.UserInterface(activity).promptForMultiListSelection(R.string.lists_title, selectedListIds -> storeCaches(geocodesInViewport, selectedListIds), true, Collections.<Integer>emptySet(), false);
            } else {
                storeCaches(geocodesInViewport, Collections.singleton(StoredList.STANDARD_LIST_ID));
            }
        }
        return true;
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
                (dialog, newItem) -> {
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

        if (viewport == null) {
            // fail safe check
            return Collections.emptySet();
        }

        for (final Geocache cache : cachesProtected) {
            if (viewport.contains(cache)) {
                geocodes.add(cache.getGeocode());
            }
        }
        return geocodes;
    }

    private Set<String> getUnsavedGeocodes(final Set<String> geocodes) {
        final Set<String> unsavedGeocodes = new HashSet<>();

        for (final String geocode : geocodes) {
            if (!DataStore.isOffline(geocode, null)) {
                unsavedGeocodes.add(geocode);
            }
        }
        return unsavedGeocodes;
    }

    /**
     * Restart the current activity if the map provider has changed, or change the map source if needed.
     *
     * @param newSource the new map source, which can be the same as the current one
     * @return true if a restart is needed, false otherwise
     */
    private boolean changeMapSource(@NonNull final MapSource newSource) {
        final MapSource oldSource = Settings.getMapSource();
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

        if (mapCenter == null) {
            return null;
        }

        return new MapState(mapCenter.getCoords(), mapView.getMapZoomLevel(), followMyLocation, mapView.getCircles(), null, null, mapOptions.isLiveEnabled, mapOptions.isStoredEnabled);
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
                    if (map != null && map.overlayPositionAndScale != null) {
                        final boolean needsRepaintForDistanceOrAccuracy = needsRepaintForDistanceOrAccuracy();
                        final boolean needsRepaintForHeading = needsRepaintForHeading();

                        if (needsRepaintForDistanceOrAccuracy) {
                            if (followMyLocation) {
                                map.centerMap(new Geopoint(currentLocation));
                            }

                            map.overlayPositionAndScale.setCoordinates(currentLocation);
                            map.overlayPositionAndScale.repaintRequired();

                            if (map.proximityNotification != null) {
                                map.proximityNotification.checkDistance(map.getClosestDistanceInM(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude())));
                            }
                        } else if (needsRepaintForHeading) {
                            final float mapBearing = map.mapView.getBearing();
                            map.overlayPositionAndScale.setHeading(currentHeading + mapBearing);
                            map.overlayPositionAndScale.repaintRequired();
                        }
                    }
                } catch (final RuntimeException e) {
                    Log.w("Failed to update location", e);
                }
            }
        }

        boolean needsRepaintForHeading() {
            final CGeoMap map = mapRef.get();
            if (map == null || map.overlayPositionAndScale == null) {
                return false;
            }
            return Math.abs(AngleUtils.difference(currentHeading, map.overlayPositionAndScale.getHeading())) > MIN_HEADING_DELTA;
        }

        boolean needsRepaintForDistanceOrAccuracy() {
            final CGeoMap map = mapRef.get();
            if (map == null) {
                return false;
            }
            final Location lastLocation = map.overlayPositionAndScale != null ? map.overlayPositionAndScale.getCoordinates() : null;

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
        if (mapOptions.coords != null && mapOptions.mapMode != MapMode.LIVE) {
            // display just one point
            displayPoint(mapOptions.coords);
            loadTimer = new CompositeDisposable();
        } else {
            loadTimer = Schedulers.newThread().schedulePeriodicallyDirect(new LoadTimerAction(this), 0, 250, TimeUnit.MILLISECONDS);
        }
        return loadTimer;
    }

    private static final class LoadTimerAction implements Runnable {

        @NonNull
        private final WeakReference<CGeoMap> mapRef;
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
                if (viewportNow == null) {
                    return;
                }
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
                    MapUtils.filter(caches);
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
                    MapUtils.filter(waypointsInViewport);
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

            final SearchResult searchResult = ConnectorFactory.searchByViewport(mapView.getViewport().resize(0.8));
            downloaded = true;

            final Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            MapUtils.filter(result);
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
        // don't add anything to mapView if just one point should be displayed
        if (mapOptions.coords != null) {
            return;
        }
        try {
            showProgressHandler.sendEmptyMessage(SHOW_PROGRESS);

            // display caches
            final List<Geocache> cachesToDisplay = caches.getAsList();
            final List<Waypoint> waypointsToDisplay = new ArrayList<>(waypoints);
            final Set<CachesOverlayItemImpl> itemsToDisplay = new HashSet<>();

            if (!cachesToDisplay.isEmpty()) {
                // Only show waypoints for single view or setting
                // when less than showWaypointsthreshold Caches shown
                final boolean isDotMode = Settings.isDotMode();
                if (mapOptions.mapMode == MapMode.SINGLE || cachesCnt < Settings.getWayPointsThreshold()) {
                    for (final Waypoint waypoint : waypointsToDisplay) {
                        if (waypoint != null && waypoint.getCoords() != null) {
                            itemsToDisplay.add(getWaypointItem(waypoint, isDotMode));
                        }
                    }
                }
                for (final Geocache cache : cachesToDisplay) {
                    if (cache != null && cache.getCoords() != null) {
                        itemsToDisplay.add(getCacheItem(cache, isDotMode));
                    }
                }
            }
            // don't add other waypoints to overlayCaches if just one point should be displayed
            if (mapOptions.coords == null) {
                mapView.updateItems(itemsToDisplay);
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

        final CachesOverlayItemImpl item = getWaypointItem(waypoint, Settings.isDotMode());
        mapView.updateItems(Collections.singletonList(item));
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

        protected abstract void runWithMap(CGeoMap map);
    }

    /**
     * store caches, invoked by "store offline" menu item
     *
     * @param listIds the lists to store the caches in
     */
    private void storeCaches(final Set<String> geocodes, final Set<Integer> listIds) {
        final LoadDetailsHandler loadDetailsHandler = new LoadDetailsHandler(this);

        waitDialog = new ProgressDialog(activity);
        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        waitDialog.setCancelable(true);
        waitDialog.setCancelMessage(loadDetailsHandler.disposeMessage());
        waitDialog.setMax(detailTotal);
        waitDialog.setOnCancelListener(arg0 -> {
            try {
                if (loadDetailsThread != null) {
                    loadDetailsThread.stopIt();
                }
            } catch (final Exception e) {
                Log.e("CGeoMap.storeCaches.onCancel", e);
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
        private final Set<String> geocodes;
        private final Set<Integer> listIds;

        LoadDetails(final DisposableHandler handler, final Set<String> geocodes, final Set<Integer> listIds) {
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
                    Geocache.storeCache(null, geocode, listIds, false, handler);
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

    private CachesOverlayItemImpl getCacheItem(final Geocache cache, final boolean isDotMode) {
        final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(cache, cache.applyDistanceRule());
        if (isDotMode) {
            item.setMarker(new CacheMarker(0, (Drawable) MapMarkerUtils.createCacheDotMarker(getResources(), cache)));
        } else {
            item.setMarker(MapMarkerUtils.getCacheMarker(getResources(), cache));
        }
        return item;
    }

    private CachesOverlayItemImpl getWaypointItem(final Waypoint waypoint, final boolean isDotMode) {
        final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(waypoint, waypoint.getWaypointType().applyDistanceRule());
        if (isDotMode) {
            item.setMarker(new CacheMarker(0, (Drawable) MapMarkerUtils.createWaypointDotMarker(getResources(), waypoint)));
        } else {
            item.setMarker(MapMarkerUtils.getWaypointMarker(getResources(), waypoint));
        }
        return item;
    }


    @Override
    public void onCacheTap(final IWaypoint waypoint) {
        final Context context = mapView.getContext();

        progress.show(context, context.getResources().getString(R.string.map_live), context.getResources().getString(R.string.cache_dialog_loading_details), true, null);

        if (waypoint == null) {
            return;
        }

        final CoordinatesType coordType = waypoint.getCoordType();

        if (coordType == CoordinatesType.CACHE && StringUtils.isNotBlank(waypoint.getGeocode())) {
            final Geocache cache = DataStore.loadCache(waypoint.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            if (cache != null) {
                final RequestDetailsThread requestDetailsThread = new RequestDetailsThread(cache);
                if (!requestDetailsThread.requestRequired()) {
                    // don't show popup if we have enough details
                    progress.dismiss();
                }
                requestDetailsThread.start();
                return;
            }
            progress.dismiss();
            return;
        }

        if (coordType == CoordinatesType.WAYPOINT && waypoint.getId() >= 0) {
            CGeoMap.markCacheAsDirty(waypoint.getGeocode());
            WaypointPopup.startActivity(context, waypoint.getId(), waypoint.getGeocode());
        } else {
            progress.dismiss();
            return;
        }

        progress.dismiss();
    }

    public WaypointDistanceInfo getClosestDistanceInM(final Geopoint coord) {
        int minDistance = 50000000;
        String name = "";
        // check caches
        for (final Geocache item : caches) {
            final int distance = (int) (1000 * coord.distanceTo(item.getCoords()));
            if (distance > 0 && distance < minDistance) {
                minDistance = distance;
                name = item.getGeocode() + " " + item.getName();
            }
        }
        // check waypoints
        for (final Waypoint item : waypoints) {
            final int distance = (int) (1000 * coord.distanceTo(item.getCoords()));
            if (distance > 0 && distance < minDistance) {
                minDistance = distance;
                name = item.getName() + " (" + item.getWaypointType().gpx + ")";
            }
        }
        return new WaypointDistanceInfo(name, minDistance);
    }

    private class RequestDetailsThread extends Thread {

        @NonNull private final Geocache cache;

        RequestDetailsThread(@NonNull final Geocache cache) {
            this.cache = cache;
        }

        public boolean requestRequired() {
            return cache.getType() == CacheType.UNKNOWN || cache.getDifficulty() == 0;
        }

        @Override
        public void run() {
            if (requestRequired()) {
                GCMap.searchByGeocodes(Collections.singleton(cache.getGeocode()));
            }
            CGeoMap.markCacheAsDirty(cache.getGeocode());
            CachePopup.startActivity(mapView.getContext(), cache.getGeocode());
            progress.dismiss();
        }
    }

}
