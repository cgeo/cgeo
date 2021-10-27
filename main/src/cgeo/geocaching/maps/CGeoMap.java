package cgeo.geocaching.maps;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CachePopup;
import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.WaypointPopup;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.ProximityNotification;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.location.WaypointDistanceInfo;
import cgeo.geocaching.maps.google.v2.GoogleGeoPoint;
import cgeo.geocaching.maps.google.v2.GoogleMapProvider;
import cgeo.geocaching.maps.google.v2.GooglePositionAndHistory;
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
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.TrailHistoryElement;
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
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.HistoryTrackUtils;
import cgeo.geocaching.utils.LeastRecentlyUsedSet;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import static cgeo.geocaching.location.Viewport.containingGCliveCaches;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.maps.model.LatLng;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
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
    private static final String BUNDLE_PROXIMITY_NOTIFICATION = "proximityNotification";
    private static final String BUNDLE_ROUTE = "route";

    private static final String KEY_ELAPSED_MS = "elapsedMs";
    private static final String KEY_PROGRESS = "progress";
    private static final String KEY_TOTAL = "total";

    // Those are initialized in onCreate() and will never be null afterwards
    private Resources res;
    private AppCompatActivity activity;
    private MapItemFactory mapItemFactory;
    private final LeastRecentlyUsedSet<Geocache> caches = new LeastRecentlyUsedSet<>(MAX_CACHES + DataStore.getAllCachesCount());
    private MapSource mapSource;


    private final GeoDirHandler geoDirUpdate = new UpdateLoc(this);
    private ProximityNotification proximityNotification;
    private IndividualRoute individualRoute = null;
    private Route tracks = null;

    // status data
    /**
     * Last search result used for displaying header
     */
    private SearchResult lastSearchResult = null;
    private Viewport lastViewport = null;
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
        getActionBar().setTitle(MapUtils.getColoredValue(calculateTitle()));
    }

    private String calculateTitle() {
        if (mapOptions.isLiveEnabled) {
            return res.getString(R.string.map_live);
        }
        final Geocache cache = getSingleModeCache();
        if (cache != null) {
            return cache.getName();
        }
        return StringUtils.defaultIfEmpty(mapOptions.title, res.getString(R.string.map_map));
    }

    @Nullable
    public Geocache getSingleModeCache() {
        // use a copy of the caches list to avoid concurrent modification
        if (mapOptions.mapMode == MapMode.SINGLE) {
            for (final Geocache geocache : caches.getAsList()) {
                if (geocache.getGeocode().equals(mapOptions.geocode)) {
                    return geocache;
                }
            }
        }
        return null;
    }

    private void setSubtitle() {
        final String subtitle = calculateSubtitle();
        if (StringUtils.isEmpty(subtitle)) {
            return;
        }
        getActionBar().setSubtitle(MapUtils.getColoredValue(subtitle));
    }

    private String calculateSubtitle() {
        // count caches in the sub title
        countVisibleCaches();
        if (!mapOptions.isLiveEnabled && mapOptions.mapMode == MapMode.SINGLE) {
            final Geocache cache = getSingleModeCache();
            if (cache != null) {
                return Formatter.formatMapSubtitle(cache);
            }
            return "";
        }
        final StringBuilder subtitle = new StringBuilder();
        final int totalCount = caches.size();

        if (cachesCnt != totalCount && Settings.isDebug()) {
            subtitle.append(cachesCnt).append('/').append(res.getQuantityString(R.plurals.cache_counts, totalCount, totalCount));
        } else {
            subtitle.append(res.getQuantityString(R.plurals.cache_counts, cachesCnt, cachesCnt));
        }

        if (Settings.isDebug() && lastSearchResult != null && StringUtils.isNotBlank(lastSearchResult.getUrl())) {
            subtitle.append(" [").append(lastSearchResult.getUrl()).append(']');
        }

        return subtitle.toString();
    }

    @NonNull
    private ActionBar getActionBar() {
        final ActionBar actionBar = activity.getSupportActionBar();
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
            if (map != null && map.spinner != null) {
                map.spinner.setVisibility(show ? View.VISIBLE : View.GONE);
            }
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
                        final Bundle updateProgressData = msg.getData();

                        final int detailProgress = updateProgressData.getInt(KEY_PROGRESS);
                        final int detailTotal = updateProgressData.getInt(KEY_TOTAL);
                        final int secondsElapsed = (int) (updateProgressData.getLong(KEY_ELAPSED_MS) / 1000);

                        final int secondsRemaining = detailProgress <= 0 ? -1 : (detailTotal - detailProgress) * secondsElapsed / detailProgress;

                        final Resources res = map.res;
                        waitDialog.setProgress(detailProgress);
                        if (secondsRemaining < 0) {
                            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.unknown_short));
                        } else if (secondsRemaining < 40) {
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
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        mapView.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_MAP_SOURCE, currentSourceId);
        outState.putParcelable(BUNDLE_MAP_STATE, currentMapState());
        outState.putBoolean(BUNDLE_LIVE_ENABLED, mapOptions.isLiveEnabled);
        if (proximityNotification != null) {
            outState.putParcelable(BUNDLE_PROXIMITY_NOTIFICATION, proximityNotification);
        }
        if (individualRoute != null) {
            outState.putParcelable(BUNDLE_ROUTE, individualRoute);
        }
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
    }

    protected Geopoint getIntentCoords() {
        if (mapOptions.coords != null) {
            return mapOptions.coords;
        } else
        if (mapOptions.geocode != null) {
            final Viewport bounds = DataStore.getBounds(mapOptions.geocode, false);
            if (bounds != null) {
                return bounds.center;
            }
        }
        return null;
    }

    protected Viewport getIntentViewport() {
        if (mapOptions.coords != null) {
            return new Viewport(mapOptions.coords);
        } else
        if (mapOptions.geocode != null) {
            return DataStore.getBounds(mapOptions.geocode, Settings.getZoomIncludingWaypoints());
        }
        return null;
    }

    protected void initializeMap(final ArrayList<TrailHistoryElement> trailHistory) {

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

        overlayPositionAndScale = mapView.createAddPositionAndScaleOverlay(activity.findViewById(R.id.distance1).getRootView(), getIntentCoords(), mapOptions.geocode);
        if (trailHistory != null) {
            overlayPositionAndScale.setHistory(trailHistory);
        }
        if (null == individualRoute) {
            individualRoute = new IndividualRoute(this::setNavigationTargetFromIndividualRoute);
            individualRoute.reloadRoute(overlayPositionAndScale);
        } else {
            individualRoute.updateRoute(overlayPositionAndScale);
        }

        CompactIconModeUtils.setCompactIconModeThreshold(getResources());

        // prepare circular progress spinner
        spinner = (ProgressBar) activity.findViewById(R.id.map_progressbar);
        if (null != spinner) {
            spinner.setVisibility(View.GONE);
        }

        mapView.repaintRequired(null);

        final Viewport viewport = getIntentViewport();
        if (mapOptions.geocode != null && viewport != null && !viewport.topRight.equals(viewport.bottomLeft)) {
            mapView.zoomToBounds(viewport, mapItemFactory.getGeoPointBase(viewport.center));
        } else {
            setZoom(Settings.getMapZoom(mapOptions.mapMode));
            mapView.getMapController().setCenter(Settings.getMapCenter());
        }

        if (mapOptions.mapState == null) {
            followMyLocation = followMyLocation && (mapOptions.mapMode == MapMode.LIVE);
            mapView.setCircles(Settings.isShowCircles());
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
        ApplicationSettings.setLocale(this.getActivity());
        super.onCreate(savedInstanceState);

        // class init
        res = this.getResources();
        activity = this.getActivity();

        MapsforgeMapProvider.getInstance().updateOfflineMaps();

        // hard coded for now to mitigate #9619 - GoogleMaps is the only map type left supported by CGeoMap
        // final MapProvider mapProvider = Settings.getMapProvider();
        final MapProvider mapProvider = GoogleMapProvider.getInstance();
        mapItemFactory = mapProvider.getMapItemFactory();

        // Get parameters from the intent
        final Bundle extras = activity.getIntent().getExtras();
        mapOptions = new MapOptions(activity, extras);

        final ArrayList<TrailHistoryElement> trailHistory = null;

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            currentSourceId = savedInstanceState.getInt(BUNDLE_MAP_SOURCE, Settings.getMapSource().getNumericalId());
            mapOptions.mapState = savedInstanceState.getParcelable(BUNDLE_MAP_STATE);
            mapOptions.isLiveEnabled = savedInstanceState.getBoolean(BUNDLE_LIVE_ENABLED, false);
            proximityNotification = savedInstanceState.getParcelable(BUNDLE_PROXIMITY_NOTIFICATION);
            individualRoute = savedInstanceState.getParcelable(BUNDLE_ROUTE);
        } else {
            currentSourceId = Settings.getMapSource().getNumericalId();
            proximityNotification = Settings.isGeneralProximityNotificationActive() ? new ProximityNotification(true, false) : null;
            individualRoute = null;
        }
        if (null != proximityNotification) {
            proximityNotification.setTextNotifications(activity);
        }
        if (mapOptions.mapState != null) {
            this.targetGeocode = mapOptions.mapState.getTargetGeocode();
            this.lastNavTarget = mapOptions.mapState.getLastNavTarget();
        }

        activity.setContentView(mapProvider.getMapLayoutId());

        // map settings popup
        activity.findViewById(R.id.map_settings_popup).setOnClickListener(v ->
            MapSettingsUtils.showSettingsPopup(getActivity(), individualRoute, this::refreshMapData, this::routingModeChanged, this::compactIconModeChanged, mapOptions.filterContext));

        // individual route popup
        activity.findViewById(R.id.map_individualroute_popup).setOnClickListener(v ->
            getIndividualRouteUtils().showPopup(activity.findViewById(R.id.map_individualroute_popup), individualRoute, StringUtils.isNotBlank(targetGeocode) && null != lastNavTarget, this::centerOnPosition, this::setTarget));

        // If recreating from an obsolete map source, we may need a restart
        if (changeMapSource(Settings.getMapSource())) {
            return;
        }

        ActivityMixin.onCreate(activity, true);

        // set layout
        ActivityMixin.setTheme(activity);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle();

        // initialize map
        mapView = (MapViewImpl) activity.findViewById(mapProvider.getMapViewId());

        // added keys must be removed before passing bundle to google's mapView, otherwise this will be thrown:
        // ClassNotFoundException: Didn't find class "cgeo.geocaching.sensors.GeoData"
        // solution from http://stackoverflow.com/questions/13900322/badparcelableexception-in-google-maps-code/15572337#15572337
        if (savedInstanceState != null) {
            savedInstanceState.remove(BUNDLE_MAP_SOURCE);
            savedInstanceState.remove(BUNDLE_MAP_STATE);
        }
        mapView.onCreate(savedInstanceState);
        mapView.setListId(mapOptions.fromList);

        mapView.onMapReady(() -> initializeMap(trailHistory));

        FilterUtils.initializeFilterBar(activity, mapActivity);
        MapUtils.updateFilterBar(activity, mapOptions.filterContext);

        AndroidBeam.disable(activity);

        MapUtils.showMapOneTimeMessages(activity, mapOptions.mapMode);

        MapsforgeMapProvider.getInstance().updateOfflineMaps();
    }

    public void toggleRouteItem(final IWaypoint item) {
        if (item == null || StringUtils.isEmpty(item.getGeocode())) {
            return;
        }
        if (individualRoute == null) {
            individualRoute = new IndividualRoute(this::setNavigationTargetFromIndividualRoute);
        }
        individualRoute.toggleItem(this.mapView.getContext(), new RouteItem(item), overlayPositionAndScale);
        ActivityMixin.invalidateOptionsMenu(activity);
        overlayPositionAndScale.repaintRequired();
    }

    private void setNavigationTargetFromIndividualRoute(@Nullable final Geopoint geopoint, final String geocode) {
        if (geopoint != null) {
            setTarget(geopoint, geocode);
        }
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

    private void resumeTrack(final boolean preventReloading) {
        if (null == tracks && !preventReloading) {
            getTrackUtils().loadTracks(this::setTracks);
        } else if (null != overlayPositionAndScale && overlayPositionAndScale instanceof GooglePositionAndHistory) {
            ((GooglePositionAndHistory) overlayPositionAndScale).updateRoute(tracks);
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
        MapUtils.updateFilterBar(activity, mapOptions.filterContext);
        resumeTrack(false);
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
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        super.onCreateOptionsMenu(menu);

        MapProviderFactory.addMapviewMenuItems(activity, menu);

        /* if we have an Actionbar find the my position toggle */
        initMyLocationSwitchButton(MapProviderFactory.createLocSwitchMenuItem(activity, menu));
        FilterUtils.initializeFilterMenu(activity, mapActivity);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mapOptions != null && (mapOptions.isLiveEnabled || mapOptions.isStoredEnabled)) {
            ViewUtils.extendMenuActionBarDisplayItemCount(getActivity(), menu);
        }
        for (final MapSource mapSource : MapProviderFactory.getMapSources()) {
            final MenuItem menuItem = menu.findItem(mapSource.getNumericalId());
            if (menuItem != null) {
                menuItem.setVisible(mapSource.isAvailable());
            }
        }

        try {
            final MenuItem itemMapLive = menu.findItem(R.id.menu_map_live);
            if (mapOptions.isLiveEnabled) {
                itemMapLive.setIcon(R.drawable.ic_menu_sync_enabled);
                itemMapLive.setTitle(res.getString(R.string.map_live_disable));
            } else {
                itemMapLive.setIcon(R.drawable.ic_menu_sync_disabled);
                itemMapLive.setTitle(res.getString(R.string.map_live_enable));
            }
            itemMapLive.setVisible(mapOptions.coords == null || mapOptions.mapMode == MapMode.LIVE);

            final Set<String> geocodesInViewport = getGeocodesForCachesInViewport();
            menu.findItem(R.id.menu_store_caches).setVisible(!isLoading() && CollectionUtils.isNotEmpty(geocodesInViewport));
            menu.findItem(R.id.menu_store_unsaved_caches).setVisible(!isLoading() && CollectionUtils.isNotEmpty(getUnsavedGeocodes(geocodesInViewport)));

            menu.findItem(R.id.menu_theme_mode).setVisible(false); // Always false, this is only used for Google Maps

            menu.findItem(R.id.menu_as_list).setVisible(!isLoading() && caches.size() > 1);

            getIndividualRouteUtils().onPrepareOptionsMenu(menu, activity.findViewById(R.id.container_individualroute), individualRoute, StringUtils.isNotBlank(targetGeocode) && null != lastNavTarget);

            menu.findItem(R.id.menu_hint).setVisible(mapOptions.mapMode == MapMode.SINGLE);
            menu.findItem(R.id.menu_compass).setVisible(mapOptions.mapMode == MapMode.SINGLE);

            HistoryTrackUtils.onPrepareOptionsMenu(menu);
            // TrackUtils.onPrepareOptionsMenu is in maps/google/v2/GoogleMapActivity only
        } catch (final RuntimeException e) {
            Log.e("CGeoMap.onPrepareOptionsMenu", e);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            ActivityMixin.navigateUp(activity);
        } else if (id == R.id.menu_map_rotation_off) {
            setMapRotation(item, Settings.MAPROTATION_OFF);
        } else if (id == R.id.menu_map_rotation_manual) {
            setMapRotation(item, Settings.MAPROTATION_MANUAL);
        } else if (id == R.id.menu_map_rotation_auto) {
            setMapRotation(item, Settings.MAPROTATION_AUTO);
        } else if (id == R.id.menu_filter) {
            getMapActivity().showFilterMenu(null);
        } else if (id == R.id.menu_map_live) {
            mapOptions.isLiveEnabled = !mapOptions.isLiveEnabled;
            if (mapOptions.mapMode == MapMode.LIVE) {
                Settings.setLiveMap(mapOptions.isLiveEnabled);
            }
            if (mapOptions.isLiveEnabled) {
                mapOptions.filterContext = new GeocacheFilterContext(GeocacheFilterContext.FilterType.LIVE);
                refreshMapData(false);
            }
            markersInvalidated = true;
            lastSearchResult = null;
            mapOptions.searchResult = null;
            ActivityMixin.invalidateOptionsMenu(activity);
            if (mapOptions.mapMode != MapMode.SINGLE) {
                mapOptions.title = StringUtils.EMPTY;
            }
            updateMapTitle();
        } else if (id == R.id.menu_store_caches) {
            return storeCaches(getGeocodesForCachesInViewport());
        } else if (id == R.id.menu_store_unsaved_caches) {
            return storeCaches(getUnsavedGeocodes(getGeocodesForCachesInViewport()));
        } else if (id == R.id.menu_theme_mode) {
            //this will never happen, Google does not support mapsforge themes -> do nothing
        } else if (id == R.id.menu_as_list) {
            CacheListActivity.startActivityMap(activity, new SearchResult(getGeocodesForCachesInViewport()));
        } else if (id == R.id.menu_hint) {
            menuShowHint();
        } else if (id == R.id.menu_compass) {
            menuCompass();
        } else if (!HistoryTrackUtils.onOptionsItemSelected(activity, id, () -> mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null), this::clearTrailHistory)
            && !getTrackUtils().onOptionsItemSelected(id, tracks)
            && !getIndividualRouteUtils().onOptionsItemSelected(id, individualRoute, this::centerOnPosition, this::setTarget)
            && !DownloaderUtils.onOptionsItemSelected(activity, id)) {
            final MapSource mapSource = MapProviderFactory.getMapSource(id);
            if (mapSource != null) {
                item.setChecked(true);
                changeMapSource(mapSource);
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void refreshMapData(final boolean circlesSwitched) {
        markersInvalidated = true;
        Tile.cache.clear();
        overlayPositionAndScale.repaintRequired();
        if (circlesSwitched) {
            mapView.setCircles(Settings.isShowCircles());
            mapView.repaintRequired(null);
        }
        MapUtils.updateFilterBar(activity, mapOptions.filterContext);

    }

    private void routingModeChanged(final RoutingMode newValue) {
        Settings.setRoutingMode(newValue);
        if ((null != individualRoute && individualRoute.getNumSegments() > 0) || null != tracks) {
            Toast.makeText(activity, R.string.brouter_recalculating, Toast.LENGTH_SHORT).show();
        }
        individualRoute.reloadRoute(overlayPositionAndScale);
        if (null != tracks) {
            try {
                AndroidRxUtils.andThenOnUi(Schedulers.computation(), () -> {
                    tracks.calculateNavigationRoute();
                    ((GooglePositionAndHistory) overlayPositionAndScale).updateRoute(tracks);
                }, () -> mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null));
            } catch (RejectedExecutionException e) {
                Log.e("CGeoMap.routingModeChanged: RejectedExecutionException: " + e.getMessage());
            }
        }
        mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
    }

    private void compactIconModeChanged(final int newValue) {
        Settings.setCompactIconMode(newValue);
        markersInvalidated = true;
        mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
    }

    @Override
    public void setTracks(final Route route) {
        tracks = route;
        resumeTrack(null == tracks);
        getTrackUtils().showTrackInfo(tracks);
    }

    @Override
    public void centerOnPosition(final double latitude, final double longitude, final Viewport viewport) {
        followMyLocation = false;
        switchMyLocationButton();
        mapView.zoomToBounds(viewport, new GoogleGeoPoint(new LatLng(latitude, longitude)));
    }

    @Override
    public void reloadIndividualRoute() {
        individualRoute.reloadRoute(overlayPositionAndScale);
        mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
    }

    private void clearTrailHistory() {
        DataStore.clearTrailHistory();
        overlayPositionAndScale.setHistory(new ArrayList<>());
        mapView.repaintRequired(overlayPositionAndScale instanceof GeneralOverlay ? ((GeneralOverlay) overlayPositionAndScale) : null);
        ActivityMixin.showToast(activity, res.getString(R.string.map_trailhistory_cleared));
    }

    @Override
    public void clearIndividualRoute() {
        individualRoute.clearRoute(overlayPositionAndScale);
        overlayPositionAndScale.repaintRequired();
        ActivityMixin.invalidateOptionsMenu(activity);
        ActivityMixin.showToast(activity, res.getString(R.string.map_individual_route_cleared));
    }

    private void setMapRotation(final MenuItem item, final int rotation) {
        Settings.setMapRotation(rotation);
        if (null != overlayPositionAndScale) {
            overlayPositionAndScale.updateMapRotation();
        }
        item.setChecked(true);
    }

    private boolean storeCaches(final Set<String> geocodesInViewport) {
        if (!isLoading()) {
            if (geocodesInViewport.size() == 0) {
                ActivityMixin.showToast(activity, res.getString(R.string.warn_save_nothing));

                return true;
            }

            if (Settings.getChooseList()) {
                // let user select list to store cache in
                new StoredList.UserInterface(activity).promptForMultiListSelection(R.string.lists_title, selectedListIds -> storeCaches(geocodesInViewport, selectedListIds), true, Collections.emptySet(), false);
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

        mapSource = newSource;

        final int mapAttributionViewId = mapSource.getMapProvider().getMapAttributionViewId();
        if (mapAttributionViewId > 0) {
            final View mapAttributionView = activity.findViewById(mapAttributionViewId);
            if (mapAttributionView != null) {
                mapAttributionView.setOnClickListener(
                    new NewMap.MapAttributionDisplayHandler(() -> this.mapSource.calculateMapAttribution(mapAttributionView.getContext())));
            }
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

        return new MapState(mapCenter.getCoords(), mapView.getMapZoomLevel(), followMyLocation, mapView.getCircles(), targetGeocode, lastNavTarget, mapOptions.isLiveEnabled, mapOptions.isStoredEnabled);
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
                searchResult = mapOptions.isLiveEnabled ? new SearchResult() : new SearchResult(DataStore.loadStoredInViewport(mapView.getViewport()));
            } else {
                // map started from another activity
                searchResult = mapOptions.searchResult != null ? new SearchResult(mapOptions.searchResult) : new SearchResult();
                if (mapOptions.geocode != null) {
                    searchResult.addGeocode(mapOptions.geocode);
                }
            }
            // live mode search result
            if (mapOptions.isLiveEnabled) {
                searchResult.addSearchResult(DataStore.loadCachedInViewport(mapView.getViewport()));
            }

            downloaded = true;
            final Set<Geocache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);
            // update the caches
            // new collection type needs to remove first
            caches.removeAll(cachesFromSearchResult);
            caches.addAll(cachesFromSearchResult);


            synchronized (caches) {
                MapUtils.filter(caches, mapOptions.filterContext);
            }

            countVisibleCaches();
            // we don't want to see any stale waypoints
            waypoints.clear();
            if (cachesCnt < Settings.getWayPointsThreshold() || mapOptions.geocode != null) {
                if (mapOptions.isLiveEnabled || mapMode == MapMode.LIVE || mapMode == MapMode.COORDS) {
                    //All visible waypoints
                    final Set<Waypoint> waypointsInViewport = DataStore.loadWaypoints(mapView.getViewport());
                    MapUtils.filter(waypointsInViewport, mapOptions.filterContext);
                    waypoints.addAll(waypointsInViewport);
                } else {
                    //All visible waypoints from the viewed caches
                    for (final Geocache c : caches.getAsList()) {
                        final Set<Waypoint> filteredWaypoints = new HashSet<>(c.getWaypoints());
                        MapUtils.filter(filteredWaypoints, mapOptions.filterContext);
                        waypoints.addAll(filteredWaypoints);
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

            final boolean useLastSearchResult = null != lastSearchResult && null != lastViewport && lastViewport.includes(mapView.getViewport());
            final Viewport newViewport = mapView.getViewport().resize(3.0);
            final SearchResult searchResult = useLastSearchResult ? lastSearchResult : ConnectorFactory.searchByViewport(newViewport);
            downloaded = true;

            final Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            MapUtils.filter(result, mapOptions.filterContext);

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
            if (null == lastViewport || !useLastSearchResult || (!caches.isEmpty() && lastSearchResult.getCount() > 400)) {
                lastViewport = containingGCliveCaches(caches.getAsList());
                if (Settings.isDebug() && overlayPositionAndScale instanceof GooglePositionAndHistory) {
                    ((GooglePositionAndHistory) overlayPositionAndScale).drawViewport(lastViewport);
                }
            }
            Log.d("searchByViewport: cached=" + useLastSearchResult + ", results=" + lastSearchResult.getCount() + ", viewport=" + lastViewport);

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
                countVisibleCaches();
                final boolean forceCompactIconMode = CompactIconModeUtils.forceCompactIconMode(cachesCnt);
                if (mapOptions.mapMode == MapMode.SINGLE || cachesCnt < Settings.getWayPointsThreshold()) {
                    for (final Waypoint waypoint : waypointsToDisplay) {
                        if (waypoint != null && waypoint.getCoords() != null && waypoint.getCoords().isValid()) {
                            itemsToDisplay.add(getWaypointItem(waypoint, forceCompactIconMode));
                        }
                    }
                }
                for (final Geocache cache : cachesToDisplay) {
                    if (cache != null && cache.getCoords() != null && cache.getCoords().isValid()) {
                        itemsToDisplay.add(getCacheItem(cache, forceCompactIconMode));
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
        if (!coords.isValid()) {
            return;
        }
        final Waypoint waypoint = new Waypoint("some place", mapOptions.waypointType != null ? mapOptions.waypointType : WaypointType.WAYPOINT, false);
        waypoint.setCoords(coords);
        waypoint.setGeocode(mapOptions.geocode);

        final CachesOverlayItemImpl item = getWaypointItem(waypoint, Settings.getCompactIconMode() == Settings.COMPACTICON_ON);
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
        waitDialog.setMax(geocodes.size());
        waitDialog.setOnCancelListener(arg0 -> {
            try {
                if (loadDetailsThread != null) {
                    loadDetailsThread.stopIt();
                }
            } catch (final Exception e) {
                Log.e("CGeoMap.storeCaches.onCancel", e);
            }
        });

        final float etaTime = geocodes.size() * 7.0f / 60.0f;
        final int roundedEta = Math.round(etaTime);
        if (etaTime < 0.4) {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
        } else {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getQuantityString(R.plurals.caches_eta_mins, roundedEta, roundedEta));
        }
        waitDialog.show();

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

            final long loadDetailsStartTime = System.currentTimeMillis();
            int progress = 0;

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
                    progress++;

                    final Message updateProgressMessage = new Message();
                    updateProgressMessage.what = UPDATE_PROGRESS;

                    final Bundle updateProgressData =  new Bundle();

                    updateProgressData.putLong(KEY_ELAPSED_MS,  System.currentTimeMillis() - loadDetailsStartTime);
                    updateProgressData.putInt(KEY_PROGRESS, progress);
                    updateProgressData.putInt(KEY_TOTAL, geocodes.size());

                    updateProgressMessage.setData(updateProgressData);
                    handler.sendMessage(updateProgressMessage);
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
        if (coords == null || !coords.isValid()) {
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
            item.setMarker(MapMarkerUtils.getCacheDotMarker(getResources(), cache));
        } else {
            item.setMarker(MapMarkerUtils.getCacheMarker(getResources(), cache, null));
        }
        return item;
    }

    private CachesOverlayItemImpl getWaypointItem(final Waypoint waypoint, final boolean isDotMode) {
        final CachesOverlayItemImpl item = mapItemFactory.getCachesOverlayItem(waypoint, waypoint.getWaypointType().applyDistanceRule());
        if (isDotMode) {
            item.setMarker(MapMarkerUtils.getWaypointDotMarker(getResources(), waypoint));
        } else {
            item.setMarker(MapMarkerUtils.getWaypointMarker(getResources(), waypoint, true));
        }
        return item;
    }


    @Override
    public void onCacheTap(final IWaypoint waypoint) {
        if (waypoint == null) {
            return;
        }

        final CoordinatesType coordType = waypoint.getCoordType();

        if (coordType == CoordinatesType.CACHE && StringUtils.isNotBlank(waypoint.getGeocode())) {
            final Geocache cache = DataStore.loadCache(waypoint.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            if (cache != null) {
                CGeoMap.markCacheAsDirty(cache.getGeocode());
                CachePopup.startActivityAllowTarget(activity, cache.getGeocode());
            }
            return;
        }

        if (coordType == CoordinatesType.WAYPOINT && waypoint.getId() >= 0) {
            CGeoMap.markCacheAsDirty(waypoint.getGeocode());
            WaypointPopup.startActivityAllowTarget(getActivity(), waypoint.getId(), waypoint.getGeocode());
        }
    }

    public WaypointDistanceInfo getClosestDistanceInM(final Geopoint coord) {
        int minDistance = 50000000;
        String name = "";
        // check caches
        for (final Geocache item : caches) {
            final int distance = (int) (1000 * coord.distanceTo(item.getCoords()));
            if (distance > 0 && distance < minDistance) {
                minDistance = distance;
                name = item.getShortGeocode() + " " + item.getName();
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

    public Collection<Geocache> getCaches() {
        return caches;
    }

    @Override
    public GeocacheFilterContext getFilterContext() {
        return mapOptions.filterContext;
    }

    @Override
    public MapOptions getMapOptions() {
        return mapOptions;
    }

}
