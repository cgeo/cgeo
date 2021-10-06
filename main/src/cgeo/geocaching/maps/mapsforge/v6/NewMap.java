package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.AbstractDialogFragment;
import cgeo.geocaching.AbstractDialogFragment.TargetInfo;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CachePopup;
import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.EditWaypointActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.WaypointPopup;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.ProximityNotification;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.maps.MapOptions;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.MapSettingsUtils;
import cgeo.geocaching.maps.MapState;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.mapsforge.AbstractMapsforgeMapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.maps.mapsforge.v6.caches.CachesBundle;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemLayer;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.maps.mapsforge.v6.layers.HistoryLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.NavigationLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.PositionLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.RouteLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.TapHandlerLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.TrackLayer;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.HistoryTrackUtils;
import cgeo.geocaching.utils.IndividualRouteUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TrackUtils;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE;
import static cgeo.geocaching.filters.gui.GeocacheFilterActivity.EXTRA_FILTER_CONTEXT;
import static cgeo.geocaching.maps.MapProviderFactory.MAP_LANGUAGE_DEFAULT_ID;
import static cgeo.geocaching.maps.mapsforge.v6.caches.CachesBundle.NO_OVERLAY_ID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Supplier;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.graphics.AndroidResourceBitmap;
import org.mapsforge.map.android.input.MapZoomControls;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.common.Observer;

@SuppressLint("ClickableViewAccessibility")
@SuppressWarnings("PMD.ExcessiveClassLength") // This is definitely a valid issue, but can't be refactored in one step
public class NewMap extends AbstractActionBarActivity implements Observer, FilteredActivity {

    private static final String STATE_INDIVIDUAlROUTEUTILS = "indrouteutils";
    private static final String STATE_TRACKUTILS = "trackutils";

    private static final String ROUTING_SERVICE_KEY = "NewMap";

    private MfMapView mapView;
    private TileCache tileCache;
    private MapSource mapSource;
    private ITileLayer tileLayer;
    private HistoryLayer historyLayer;
    private PositionLayer positionLayer;
    private NavigationLayer navigationLayer;
    private RouteLayer routeLayer;
    private TrackLayer trackLayer;
    private CachesBundle caches;
    private final MapHandlers mapHandlers = new MapHandlers(new TapHandler(this), new DisplayHandler(this), new ShowProgressHandler(this));

    private RenderThemeHelper renderThemeHelper = null; //must be initialized in onCreate();

    private DistanceView distanceView;
    private View mapAttribution;

    private final ArrayList<TrailHistoryElement> trailHistory = null;

    private String targetGeocode = null;
    private Geopoint lastNavTarget = null;
    private final Queue<String> popupGeocodes = new ConcurrentLinkedQueue<>();

    private ProgressDialog waitDialog;
    private LoadDetails loadDetailsThread;
    private ProgressBar spinner;

    private final UpdateLoc geoDirUpdate = new UpdateLoc(this);
    /**
     * initialization with an empty subscription to make static code analysis tools more happy
     */
    private ProximityNotification proximityNotification;
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private CheckBox myLocSwitch;
    private MapOptions mapOptions;
    private TargetView targetView;
    private IndividualRoute individualRoute = null;
    private Route tracks = null;

    private static boolean followMyLocation = true;

    private static final String BUNDLE_MAP_STATE = "mapState";
    private static final String BUNDLE_PROXIMITY_NOTIFICATION = "proximityNotification";
    private static final String BUNDLE_ROUTE = "route";

    // Handler messages
    // DisplayHandler
    public static final int UPDATE_TITLE = 0;
    // ShowProgressHandler
    public static final int HIDE_PROGRESS = 0;
    public static final int SHOW_PROGRESS = 1;
    // LoadDetailsHandler
    public static final int UPDATE_PROGRESS = 0;
    public static final int FINISHED_LOADING_DETAILS = 1;

    private Viewport lastViewport = null;
    private boolean lastCompactIconMode = false;

    private MapMode mapMode;

    private TrackUtils trackUtils = null;
    private IndividualRouteUtils individualRouteUtils = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        ApplicationSettings.setLocale(this);
        super.onCreate(savedInstanceState);

        Log.d("NewMap: onCreate");

        trackUtils = new TrackUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_TRACKUTILS), this::setTracks, this::centerOnPosition);
        individualRouteUtils = new IndividualRouteUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_INDIVIDUAlROUTEUTILS), this::clearIndividualRoute, this::reloadIndividualRoute);

        ResourceBitmapCacheMonitor.addRef();
        AndroidGraphicFactory.createInstance(this.getApplication());

        MapsforgeMapProvider.getInstance().updateOfflineMaps();

        this.renderThemeHelper = new RenderThemeHelper(this);

        // Support for multi-threaded map painting
        Parameters.NUMBER_OF_THREADS = Settings.getMapOsmThreads();
        Log.i("OSM #threads=" + Parameters.NUMBER_OF_THREADS);

        // Use fast parent tile rendering to increase performance when zooming in
        Parameters.PARENT_TILES_RENDERING = Parameters.ParentTilesRendering.SPEED;

        // Get parameters from the intent
        mapOptions = new MapOptions(this, getIntent().getExtras());

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            mapOptions.mapState = savedInstanceState.getParcelable(BUNDLE_MAP_STATE);
            proximityNotification = savedInstanceState.getParcelable(BUNDLE_PROXIMITY_NOTIFICATION);
            individualRoute = savedInstanceState.getParcelable(BUNDLE_ROUTE);
            followMyLocation = mapOptions.mapState.followsMyLocation();
        } else {
            individualRoute = null;
            followMyLocation = followMyLocation && mapOptions.mapMode == MapMode.LIVE;
            proximityNotification = Settings.isGeneralProximityNotificationActive() ? new ProximityNotification(true, false) : null;
        }
        if (null != proximityNotification) {
            proximityNotification.setTextNotifications(this);
        }

        ActivityMixin.onCreate(this, true);

        // set layout
        ActivityMixin.setTheme(this);

        setContentView(R.layout.map_mapsforge_v6);
        setTitle();
        this.mapAttribution = findViewById(R.id.map_attribution);

        // map settings popup
        findViewById(R.id.map_settings_popup).setOnClickListener(v -> MapSettingsUtils.showSettingsPopup(this, individualRoute, this::refreshMapData, this::routingModeChanged, this::compactIconModeChanged, mapOptions.filterContext));

        // individual route popup
        findViewById(R.id.map_individualroute_popup).setOnClickListener(v -> individualRouteUtils.showPopup(findViewById(R.id.map_individualroute_popup), individualRoute, StringUtils.isNotBlank(targetGeocode) && null != lastNavTarget, this::centerOnPosition, this::setTarget));

        // prepare circular progress spinner
        spinner = (ProgressBar) findViewById(R.id.map_progressbar);
        spinner.setVisibility(View.GONE);

        // initialize map
        mapView = (MfMapView) findViewById(R.id.mfmapv5);

        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(true);

        // style zoom controls
        final MapZoomControls zoomControls = mapView.getMapZoomControls();
        zoomControls.setZoomControlsOrientation(MapZoomControls.Orientation.VERTICAL_IN_OUT);
        zoomControls.setZoomInResource(R.drawable.zoomin);
        zoomControls.setZoomOutResource(R.drawable.zoomout);
        final int padding = ViewUtils.dpToPixel(10.0f);
        zoomControls.setPadding(padding, padding, padding, padding);
        zoomControls.setAutoHide(false);

        //make room for map attribution icon button
        final int mapAttPx = Math.round(this.getResources().getDisplayMetrics().density * 30);
        mapView.getMapScaleBar().setMarginHorizontal(mapAttPx);

        // create a tile cache of suitable size. always initialize it based on the smallest tile size to expect (256 for online tiles)
        tileCache = AndroidUtil.createTileCache(this, "mapcache", 256, 1f, this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        // attach drag handler
        final DragHandler dragHandler = new DragHandler(this);
        mapView.setOnMapDragListener(dragHandler);

        mapMode = (mapOptions != null && mapOptions.mapMode != null) ? mapOptions.mapMode : MapMode.LIVE;

        // prepare initial settings of mapView
        if (mapOptions.mapState != null) {
            this.mapView.getModel().mapViewPosition.setCenter(MapsforgeUtils.toLatLong(mapOptions.mapState.getCenter()));
            this.mapView.setMapZoomLevel((byte) mapOptions.mapState.getZoomLevel());
            this.targetGeocode = mapOptions.mapState.getTargetGeocode();
            this.lastNavTarget = mapOptions.mapState.getLastNavTarget();
            mapOptions.isLiveEnabled = mapOptions.mapState.isLiveEnabled();
            mapOptions.isStoredEnabled = mapOptions.mapState.isStoredEnabled();
        } else if (mapOptions.searchResult != null) {
            final Viewport viewport = DataStore.getBounds(mapOptions.searchResult.getGeocodes());

            if (viewport != null) {
                postZoomToViewport(viewport);
            }
        } else if (StringUtils.isNotEmpty(mapOptions.geocode) && mapOptions.mapMode != MapMode.COORDS) {
            final Viewport viewport = DataStore.getBounds(mapOptions.geocode, Settings.getZoomIncludingWaypoints());

            if (viewport != null) {
                postZoomToViewport(viewport);
            }
            targetGeocode = mapOptions.geocode;
        } else if (mapOptions.coords != null) {
            postZoomToViewport(new Viewport(mapOptions.coords, 0, 0));
            if (mapOptions.mapMode == MapMode.LIVE) {
                mapOptions.coords = null;   // no direction line, even if enabled in settings
                followMyLocation = false;   // do not center on GPS position, even if in LIVE mode
            }
        } else {
            postZoomToViewport(new Viewport(Settings.getMapCenter().getCoords(), 0, 0));
        }

        FilterUtils.initializeFilterBar(this, this);
        MapUtils.updateFilterBar(this, mapOptions.filterContext);

        Routing.connect(ROUTING_SERVICE_KEY, () -> resumeRoute(true));
        CompactIconModeUtils.setCompactIconModeThreshold(getResources());

        MapsforgeMapProvider.getInstance().updateOfflineMaps();

        MapUtils.showMapOneTimeMessages(this, mapMode);
    }

    private void postZoomToViewport(final Viewport viewport) {
        mapView.post(() -> mapView.zoomToViewport(viewport, this.mapMode));
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.map_activity, menu);

        MapProviderFactory.addMapviewMenuItems(this, menu);
        MapProviderFactory.addMapViewLanguageMenuItems(menu);

        initMyLocationSwitchButton(MapProviderFactory.createLocSwitchMenuItem(this, menu));
        FilterUtils.initializeFilterMenu(this, this);

        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mapOptions != null && (mapOptions.isLiveEnabled || mapOptions.isStoredEnabled)) {
            ViewUtils.extendMenuActionBarDisplayItemCount(this, menu);
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
                itemMapLive.setIcon(R.drawable.ic_menu_refresh);
                itemMapLive.setTitle(res.getString(R.string.map_live_disable));
            } else {
                itemMapLive.setIcon(R.drawable.ic_menu_sync_disabled);
                itemMapLive.setTitle(res.getString(R.string.map_live_enable));
            }
            itemMapLive.setVisible(mapOptions.coords == null || mapOptions.mapMode == MapMode.LIVE);

            final Set<String> visibleCacheGeocodes = caches.getVisibleCacheGeocodes();

            menu.findItem(R.id.menu_store_caches).setVisible(false);
            menu.findItem(R.id.menu_store_caches).setVisible(!caches.isDownloading() && !visibleCacheGeocodes.isEmpty());

            menu.findItem(R.id.menu_store_unsaved_caches).setVisible(false);
            menu.findItem(R.id.menu_store_unsaved_caches).setVisible(!caches.isDownloading() && new SearchResult(visibleCacheGeocodes).hasUnsavedCaches());

            menu.findItem(R.id.menu_theme_mode).setVisible(tileLayerHasThemes());
            menu.findItem(R.id.menu_theme_options).setVisible(tileLayerHasThemes() && this.renderThemeHelper.themeOptionsAvailable());

            menu.findItem(R.id.menu_as_list).setVisible(!caches.isDownloading() && caches.getVisibleCachesCount() > 1);

            this.individualRouteUtils.onPrepareOptionsMenu(menu, findViewById(R.id.container_individualroute), individualRoute, StringUtils.isNotBlank(targetGeocode) && null != lastNavTarget);

            menu.findItem(R.id.menu_hint).setVisible(mapOptions.mapMode == MapMode.SINGLE);
            menu.findItem(R.id.menu_compass).setVisible(mapOptions.mapMode == MapMode.SINGLE);
            HistoryTrackUtils.onPrepareOptionsMenu(menu);
            this.trackUtils.onPrepareOptionsMenu(menu);
        } catch (final RuntimeException e) {
            Log.e("NewMap.onPrepareOptionsMenu", e);
        }

        return true;
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            ActivityMixin.navigateUp(this);
        } else if (id == R.id.menu_map_live) {
            mapOptions.isLiveEnabled = !mapOptions.isLiveEnabled;
            if (mapOptions.isLiveEnabled) {
                mapOptions.isStoredEnabled = true;
                mapOptions.filterContext = new GeocacheFilterContext(LIVE);
                caches.setFilterContext(mapOptions.filterContext);
                refreshMapData(false);
            }

            if (mapOptions.mapMode == MapMode.LIVE) {
                Settings.setLiveMap(mapOptions.isLiveEnabled);
            }
            caches.handleStoredLayers(this, mapOptions);
            caches.handleLiveLayers(this, mapOptions);
            ActivityMixin.invalidateOptionsMenu(this);
            if (mapOptions.mapMode != MapMode.SINGLE) {
                mapOptions.title = StringUtils.EMPTY;
            } else {
                // reset target cache on single mode map
                targetGeocode = mapOptions.geocode;
            }
            setTitle();
        } else if (id == R.id.menu_filter) {
            showFilterMenu(null);
        } else if (id == R.id.menu_store_caches) {
            return storeCaches(caches.getVisibleCacheGeocodes());
        } else if (id == R.id.menu_store_unsaved_caches) {
            return storeCaches(getUnsavedGeocodes(caches.getVisibleCacheGeocodes()));
        } else if (id == R.id.menu_theme_mode) {
            this.renderThemeHelper.selectMapTheme(this.tileLayer, this.tileCache);
        } else if (id == R.id.menu_theme_options) {
            this.renderThemeHelper.selectMapThemeOptions();
        } else if (id == R.id.menu_as_list) {
            CacheListActivity.startActivityMap(this, new SearchResult(caches.getVisibleCacheGeocodes()));
        } else if (id == R.id.menu_hint) {
            menuShowHint();
        } else if (id == R.id.menu_compass) {
            menuCompass();
        } else if (!HistoryTrackUtils.onOptionsItemSelected(this, id, () -> historyLayer.requestRedraw(), this::clearTrailHistory)
            && !this.trackUtils.onOptionsItemSelected(id, tracks)
            && !this.individualRouteUtils.onOptionsItemSelected(id, individualRoute, this::centerOnPosition, this::setTarget)
            && !DownloaderUtils.onOptionsItemSelected(this, id)) {
            final String language = MapProviderFactory.getLanguage(id);
            if (language != null || id == MAP_LANGUAGE_DEFAULT_ID) {
                item.setChecked(true);
                changeLanguage(language);
                return true;
            } else {
                final MapSource mapSource = MapProviderFactory.getMapSource(id);
                if (mapSource != null) {
                    item.setChecked(true);
                    changeMapSource(mapSource);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private void refreshMapData(final boolean circlesSwitched) {
        if (circlesSwitched) {
            caches.switchCircles();
        }
        if (caches != null) {
            caches.invalidate();
        }
        Tile.cache.clear();

        if (null != trackLayer) {
            trackLayer.setHidden(Settings.isHideTrack());
            trackLayer.requestRedraw();
        }
        MapUtils.updateFilterBar(this, mapOptions.filterContext);
    }

    private void routingModeChanged(final RoutingMode newValue) {
        Settings.setRoutingMode(newValue);
        if ((null != individualRoute && individualRoute.getNumSegments() > 0) || null != tracks) {
            Toast.makeText(this, R.string.brouter_recalculating, Toast.LENGTH_SHORT).show();
        }
        individualRoute.reloadRoute(routeLayer);
        if (null != tracks) {
            try {
                AndroidRxUtils.andThenOnUi(Schedulers.computation(), () -> {
                    tracks.calculateNavigationRoute();
                    trackLayer.updateRoute(tracks);
                }, () -> trackLayer.requestRedraw());
            } catch (RejectedExecutionException e) {
                Log.e("NewMap.routingModeChanged: RejectedExecutionException: " + e.getMessage());
            }
        }
        navigationLayer.requestRedraw();
    }

    private void compactIconModeChanged(final int newValue) {
        Settings.setCompactIconMode(newValue);
        caches.invalidateAll(NO_OVERLAY_ID);
    }

    private void clearTrailHistory() {
        this.historyLayer.reset();
        this.historyLayer.requestRedraw();
        showToast(res.getString(R.string.map_trailhistory_cleared));
    }

    private void clearIndividualRoute() {
        individualRoute.clearRoute(routeLayer);
        distanceView.showRouteDistance();
        ActivityMixin.invalidateOptionsMenu(this);
        showToast(res.getString(R.string.map_individual_route_cleared));
    }

    private void centerOnPosition(final double latitude, final double longitude, final Viewport viewport) {
        followMyLocation = false;
        switchMyLocationButton();
        mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(new LatLong(latitude, longitude), (byte) mapView.getMapZoomLevel()));
        postZoomToViewport(viewport);
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

    private boolean storeCaches(final Set<String> geocodes) {
        if (!caches.isDownloading()) {
            if (geocodes.isEmpty()) {
                ActivityMixin.showToast(this, res.getString(R.string.warn_save_nothing));
                return true;
            }

            if (Settings.getChooseList()) {
                // let user select list to store cache in
                new StoredList.UserInterface(this).promptForMultiListSelection(R.string.lists_title, selectedListIds -> storeCaches(geocodes, selectedListIds), true, Collections.emptySet(), false);
            } else {
                storeCaches(geocodes, Collections.singleton(StoredList.STANDARD_LIST_ID));
            }
        }
        return true;
    }

    private void menuCompass() {
        final Geocache cache = getCurrentTargetCache();
        if (cache != null) {
            CompassActivity.startActivityCache(this, cache);
        }
    }

    private void menuShowHint() {
        final Geocache cache = getCurrentTargetCache();
        if (cache != null) {
            cache.showHintToast(this);
        }
    }

    /**
     * @param view Not used here, required by layout
     */
    @Override
    public void showFilterMenu(final View view) {
        FilterUtils.openFilterActivity(this, mapOptions.filterContext,
            new SearchResult(caches.getVisibleCacheGeocodes()).getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB));
    }

    @Override
    public boolean showFilterList(final View view) {
        return FilterUtils.openFilterList(this, mapOptions.filterContext);
    }

    @Override
    public void refreshWithFilter(final GeocacheFilter filter) {
        mapOptions.filterContext.set(filter);
        refreshMapData(false);
    }

    private void changeMapSource(@NonNull final MapSource newSource) {
        final MapSource oldSource = Settings.getMapSource();
        final boolean restartRequired = !MapProviderFactory.isSameActivity(oldSource, newSource);

        // Update MapSource in settings
        Settings.setMapSource(newSource);

        if (restartRequired) {
            mapRestart();
        } else if (mapView != null) {  // changeMapSource can be called by onCreate()
            switchTileLayer(newSource);
        }
    }

    private void changeLanguage(final String language) {
        Settings.setMapLanguage(language);
        mapRestart();
    }

    /**
     * Restart the current activity with the default map source.
     */
    private void mapRestart() {
        mapOptions.mapState = currentMapState();
        finish();
        mapOptions.startIntent(this, Settings.getMapProvider().getMapClass());
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
        final Geopoint mapCenter = mapView.getViewport().getCenter();
        return new MapState(mapCenter.getCoords(), mapView.getMapZoomLevel(), followMyLocation, Settings.isShowCircles(), targetGeocode, lastNavTarget, mapOptions.isLiveEnabled, mapOptions.isStoredEnabled);
    }


    private void switchTileLayer(final MapSource newSource) {
        final ITileLayer oldLayer = this.tileLayer;
        ITileLayer newLayer = null;

        mapSource = newSource;

        if (this.mapAttribution != null) {
            this.mapAttribution.setOnClickListener(
                new MapAttributionDisplayHandler(() -> this.mapSource.calculateMapAttribution(this)));
        }

        if (newSource instanceof AbstractMapsforgeMapSource) {
            newLayer = ((AbstractMapsforgeMapSource) newSource).createTileLayer(tileCache, this.mapView.getModel().mapViewPosition);
        }
        ActivityMixin.invalidateOptionsMenu(this);

        // Exchange layer
        if (newLayer != null) {
            mapView.setZoomLevelMax(newLayer.getZoomLevelMax());
            mapView.setZoomLevelMin(newLayer.getZoomLevelMin());

            // make sure map zoom level is within new zoom level boundaries
            final int currentZoomLevel = mapView.getMapZoomLevel();
            if (currentZoomLevel < newLayer.getZoomLevelMin()) {
                mapView.setMapZoomLevel(newLayer.getZoomLevelMin());
            } else if (currentZoomLevel > newLayer.getZoomLevelMax()) {
                mapView.setMapZoomLevel(newLayer.getZoomLevelMax());
            }

            final Layers layers = this.mapView.getLayerManager().getLayers();
            int index = 0;
            if (oldLayer != null) {
                index = layers.indexOf(oldLayer.getTileLayer()) + 1;
            }
            layers.add(index, newLayer.getTileLayer());
            this.tileLayer = newLayer;
            this.renderThemeHelper.reapplyMapTheme(this.tileLayer, this.tileCache);
            this.tileLayer.onResume();
        } else {
            this.tileLayer = null;
        }

        // Cleanup
        if (oldLayer != null) {
            this.mapView.getLayerManager().getLayers().remove(oldLayer.getTileLayer());
            oldLayer.getTileLayer().onDestroy();
        }
        tileCache.purge();
    }

    private void resumeTileLayer() {
        if (this.tileLayer != null) {
            this.tileLayer.onResume();
        }
    }

    private void pauseTileLayer() {
        if (this.tileLayer != null) {
            this.tileLayer.onPause();
        }
    }

    private boolean tileLayerHasThemes() {
        if (tileLayer != null) {
            return tileLayer.hasThemes();
        }

        return false;
    }

    private void resumeRoute(final boolean force) {
        if (null == individualRoute || force) {
            individualRoute = new IndividualRoute(this::setTarget);
            individualRoute.reloadRoute(routeLayer);
        } else {
            individualRoute.updateRoute(routeLayer);
        }
    }

    private void resumeTrack(final boolean preventReloading) {
        if (null == tracks && !preventReloading) {
            this.trackUtils.loadTracks(this::setTracks);
        } else if (null != trackLayer) {
            trackLayer.updateRoute(tracks);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("NewMap: onResume");

        resumeTileLayer();
        resumeRoute(false);
        resumeTrack(false);
        mapView.getModel().mapViewPosition.addObserver(this);
        MapUtils.updateFilterBar(this, mapOptions.filterContext);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("NewMap: onStart");

        initializeLayers();
    }

    private void initializeLayers() {

        switchTileLayer(Settings.getMapSource());

        // History Layer
        this.historyLayer = new HistoryLayer(trailHistory);
        this.mapView.getLayerManager().getLayers().add(this.historyLayer);

        // RouteLayer
        this.routeLayer = new RouteLayer(realRouteDistance -> {
            if (null != this.distanceView) {
                this.distanceView.setRouteDistance(realRouteDistance);
            }
        });
        this.mapView.getLayerManager().getLayers().add(this.routeLayer);

        // TrackLayer
        this.trackLayer = new TrackLayer(Settings.isHideTrack());
        this.mapView.getLayerManager().getLayers().add(this.trackLayer);

        // NavigationLayer
        Geopoint navTarget = lastNavTarget;
        if (navTarget == null) {
            navTarget = mapOptions.coords;
            if (navTarget == null && StringUtils.isNotEmpty(mapOptions.geocode)) {
                final Viewport bounds = DataStore.getBounds(mapOptions.geocode);
                if (bounds != null) {
                    navTarget = bounds.center;
                }
            }
        }
        this.navigationLayer = new NavigationLayer(navTarget, realDistance -> {
            if (null != this.distanceView) {
                this.distanceView.setRealDistance(realDistance);
            }
        });
        this.mapView.getLayerManager().getLayers().add(this.navigationLayer);

        // GeoitemLayer
        GeoitemLayer.resetColors();

        // TapHandler
        final TapHandlerLayer tapHandlerLayer = new TapHandlerLayer(this.mapHandlers.getTapHandler());
        this.mapView.getLayerManager().getLayers().add(tapHandlerLayer);

        // Caches bundle
        if (mapOptions.searchResult != null) {
            this.caches = new CachesBundle(this, mapOptions.searchResult, this.mapView, this.mapHandlers);
        } else if (StringUtils.isNotEmpty(mapOptions.geocode)) {
            if (mapOptions.mapMode == MapMode.COORDS && mapOptions.coords != null) {
                this.caches = new CachesBundle(this, mapOptions.coords, mapOptions.waypointType, this.mapView, this.mapHandlers, mapOptions.geocode);
            } else {
                this.caches = new CachesBundle(this, mapOptions.geocode, this.mapView, this.mapHandlers);
            }
        } else if (mapOptions.coords != null) {
            this.caches = new CachesBundle(this, mapOptions.coords, mapOptions.waypointType, this.mapView, this.mapHandlers, null);
        } else {
            caches = new CachesBundle(this, this.mapView, this.mapHandlers);
        }

        // Stored enabled map
        caches.handleStoredLayers(this, mapOptions);
        // Live enabled map
        caches.handleLiveLayers(this, mapOptions);
        caches.setFilterContext(mapOptions.filterContext);

        // Position layer
        this.positionLayer = new PositionLayer();
        this.mapView.getLayerManager().getLayers().add(positionLayer);

        //Distance view
        this.distanceView = new DistanceView(findViewById(R.id.distance1).getRootView(), navTarget, Settings.isBrouterShowBothDistances());

        //Target view
        this.targetView = new TargetView((TextView) findViewById(R.id.target), (TextView) findViewById(R.id.targetSupersize), StringUtils.EMPTY, StringUtils.EMPTY);
        final Geocache target = getCurrentTargetCache();
        if (target != null) {
            targetView.setTarget(target.getShortGeocode(), target.getName());
        }

        // resume location access
        PermissionHandler.executeIfLocationPermissionGranted(this,
                new RestartLocationPermissionGrantedCallback(PermissionRequestContext.NewMap) {

                    @Override
                    public void executeAfter() {
                        resumeDisposables.add(geoDirUpdate.start(GeoDirHandler.UPDATE_GEODIR));
                    }
                });
    }

    @Override
    public void onPause() {
        Log.d("NewMap: onPause");

        savePrefs();

        pauseTileLayer();
        mapView.getModel().mapViewPosition.removeObserver(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("NewMap: onStop");

        waitDialog = null;
        terminateLayers();

        super.onStop();
    }

    private void terminateLayers() {
        this.resumeDisposables.clear();

        this.caches.onDestroy();
        this.caches = null;

        this.mapView.getLayerManager().getLayers().remove(this.positionLayer);
        this.positionLayer = null;
        this.mapView.getLayerManager().getLayers().remove(this.navigationLayer);
        this.navigationLayer = null;
        this.mapView.getLayerManager().getLayers().remove(this.routeLayer);
        this.routeLayer = null;
        this.mapView.getLayerManager().getLayers().remove(this.trackLayer);
        this.trackLayer = null;
        this.mapView.getLayerManager().getLayers().remove(this.historyLayer);
        this.historyLayer = null;

        if (this.tileLayer != null) {
            this.mapView.getLayerManager().getLayers().remove(this.tileLayer.getTileLayer());
            this.tileLayer.getTileLayer().onDestroy();
            this.tileLayer = null;
        }
    }

    /**
     * store caches, invoked by "store offline" menu item
     *
     * @param listIds the lists to store the caches in
     */
    private void storeCaches(final Set<String> geocodes, final Set<Integer> listIds) {

        final int count = geocodes.size();
        final LoadDetailsHandler loadDetailsHandler = new LoadDetailsHandler(count, this);

        waitDialog = new ProgressDialog(this);
        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        waitDialog.setCancelable(true);
        waitDialog.setCancelMessage(loadDetailsHandler.disposeMessage());
        waitDialog.setMax(count);
        waitDialog.setOnCancelListener(arg0 -> {
            try {
                if (loadDetailsThread != null) {
                    loadDetailsThread.stopIt();
                }
            } catch (final Exception e) {
                Log.e("CGeoMap.storeCaches.onCancel", e);
            }
        });

        final float etaTime = count * 7.0f / 60.0f;
        final int roundedEta = Math.round(etaTime);
        if (etaTime < 0.4) {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getString(R.string.caches_eta_ltm));
        } else {
            waitDialog.setMessage(res.getString(R.string.caches_downloading) + " " + res.getQuantityString(R.plurals.caches_eta_mins, roundedEta, roundedEta));
        }
        loadDetailsHandler.setStart();

        waitDialog.show();

        loadDetailsThread = new LoadDetails(loadDetailsHandler, geocodes, listIds);
        loadDetailsThread.start();
    }

    @Override
    protected void onDestroy() {
        Log.d("NewMap: onDestroy");
        if (this.renderThemeHelper != null) {
            this.renderThemeHelper.onDestroy();
            this.renderThemeHelper = null;
        }
        this.tileCache.destroy();
        this.mapView.getModel().mapViewPosition.destroy();
        this.mapView.destroy();
        ResourceBitmapCacheMonitor.release();

        Routing.disconnect(ROUTING_SERVICE_KEY);
        if (this.mapAttribution != null) {
            this.mapAttribution.setOnClickListener(null);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_INDIVIDUAlROUTEUTILS, individualRouteUtils.getState());
        outState.putBundle(STATE_TRACKUTILS, trackUtils.getState());

        Log.d("New map: onSaveInstanceState");

        final MapState state = prepareMapState();
        outState.putParcelable(BUNDLE_MAP_STATE, state);
        if (proximityNotification != null) {
            outState.putParcelable(BUNDLE_PROXIMITY_NOTIFICATION, proximityNotification);
        }
        if (individualRoute != null) {
            outState.putParcelable(BUNDLE_ROUTE, individualRoute);
        }
    }

    private MapState prepareMapState() {
        return new MapState(MapsforgeUtils.toGeopoint(mapView.getModel().mapViewPosition.getCenter()), mapView.getMapZoomLevel(), followMyLocation, false, targetGeocode, lastNavTarget, mapOptions.isLiveEnabled, mapOptions.isStoredEnabled);
    }

    private void centerMap(final Geopoint geopoint) {
        mapView.getModel().mapViewPosition.setCenter(new LatLong(geopoint.getLatitude(), geopoint.getLongitude()));
    }

    public Location getCoordinates() {
        final LatLong center = mapView.getModel().mapViewPosition.getCenter();
        final Location loc = new Location("newmap");
        loc.setLatitude(center.latitude);
        loc.setLongitude(center.longitude);
        return loc;
    }

    private void initMyLocationSwitchButton(final CheckBox locSwitch) {
        myLocSwitch = locSwitch;
        /*
         * TODO: Switch back to ImageSwitcher for animations?
         * myLocSwitch.setFactory(this);
         * myLocSwitch.setInAnimation(activity, android.R.anim.fade_in);
         * myLocSwitch.setOutAnimation(activity, android.R.anim.fade_out);
         */
        myLocSwitch.setOnClickListener(new MyLocationListener(this));
        switchMyLocationButton();
    }

    // switch My Location button image
    private void switchMyLocationButton() {
        myLocSwitch.setChecked(followMyLocation);
        if (followMyLocation) {
            myLocationInMiddle(Sensors.getInstance().currentGeo());
        }
    }

    public void showAddWaypoint(final LatLong tapLatLong) {
        final Geocache cache = getCurrentTargetCache();
        if (cache != null) {
            EditWaypointActivity.startActivityAddWaypoint(this, cache, new Geopoint(tapLatLong.latitude, tapLatLong.longitude));
        } else if (Settings.isLongTapOnMapActivated()) {
            InternalConnector.interactiveCreateCache(this, new Geopoint(tapLatLong.latitude, tapLatLong.longitude), mapOptions.fromList, true);
        }
    }

    // set my location listener
    private static class MyLocationListener implements View.OnClickListener {

        @NonNull
        private final WeakReference<NewMap> mapRef;

        MyLocationListener(@NonNull final NewMap map) {
            mapRef = new WeakReference<>(map);
        }

        private void onFollowMyLocationClicked() {
            followMyLocation = !followMyLocation;
            final NewMap map = mapRef.get();
            if (map != null) {
                map.switchMyLocationButton();
            }
        }

        @Override
        public void onClick(final View view) {
            onFollowMyLocationClicked();
        }
    }

    // Set center of map to my location if appropriate.
    private void myLocationInMiddle(final GeoData geo) {
        if (followMyLocation) {
            centerMap(geo.getCoords());
        }
    }

    private static final class DisplayHandler extends Handler {

        @NonNull
        private final WeakReference<NewMap> mapRef;

        DisplayHandler(@NonNull final NewMap map) {
            this.mapRef = new WeakReference<>(map);
        }

        @Override
        public void handleMessage(final Message msg) {
            final NewMap map = mapRef.get();
            if (map == null) {
                return;
            }

            final int what = msg.what;
            switch (what) {
                case UPDATE_TITLE:
                    map.setTitle();
                    map.setSubtitle();
                    break;

                default:
                    break;
            }
        }

    }

    private void setTitle() {
        final String title = calculateTitle();

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(MapUtils.getColoredValue(title));
        }
    }

    @NonNull
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

    private void setSubtitle() {
        final String subtitle = calculateSubtitle();
        if (StringUtils.isEmpty(subtitle)) {
            return;
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(MapUtils.getColoredValue(subtitle));
        }
    }

    @NonNull
    private String calculateSubtitle() {
        if (!mapOptions.isLiveEnabled && mapOptions.mapMode == MapMode.SINGLE) {
            final Geocache cache = getSingleModeCache();
            if (cache != null) {
                return Formatter.formatMapSubtitle(cache);
            }
            return "";
        }

        // count caches in the sub title
        final int visible = countVisibleCaches();
        final int total = countTotalCaches();

        final StringBuilder subtitle = new StringBuilder();
        if (visible != total && Settings.isDebug()) {
            subtitle.append(visible).append('/').append(res.getQuantityString(R.plurals.cache_counts, total, total));
        } else {
            subtitle.append(res.getQuantityString(R.plurals.cache_counts, visible, visible));
        }

        return subtitle.toString();
    }

    private void switchCircles() {
        caches.switchCircles();
    }

    private int countVisibleCaches() {
        return caches != null ? caches.getVisibleCachesCount() : 0;
    }

    private int countTotalCaches() {
        return caches != null ? caches.getCachesCount() : 0;
    }

    /**
     * Updates the progress.
     */
    private static final class ShowProgressHandler extends Handler {
        private int counter = 0;

        @NonNull
        private final WeakReference<NewMap> mapRef;

        ShowProgressHandler(@NonNull final NewMap map) {
            this.mapRef = new WeakReference<>(map);
        }

        @Override
        public void handleMessage(final Message msg) {
            final int what = msg.what;

            if (what == HIDE_PROGRESS) {
                if (--counter == 0) {
                    showProgress(false);
                }
            } else if (what == SHOW_PROGRESS) {
                showProgress(true);
                counter++;
            }
        }

        private void showProgress(final boolean show) {
            final NewMap map = mapRef.get();
            if (map == null) {
                return;
            }
            map.spinner.setVisibility(show ? View.VISIBLE : View.GONE);
        }

    }

    private static final class LoadDetailsHandler extends DisposableHandler {

        private final int detailTotal;
        private int detailProgress;
        private long detailProgressTime;
        private final WeakReference<NewMap> mapRef;

        LoadDetailsHandler(final int detailTotal, final NewMap map) {
            super();

            this.detailTotal = detailTotal;
            this.detailProgress = 0;
            this.mapRef = new WeakReference<>(map);
        }

        public void setStart() {
            detailProgressTime = System.currentTimeMillis();
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            final NewMap map = mapRef.get();
            if (map == null) {
                return;
            }
            if (msg.what == UPDATE_PROGRESS) {
                if (detailProgress < detailTotal) {
                    detailProgress++;
                }
                if (map.waitDialog != null) {
                    final int secondsElapsed = (int) ((System.currentTimeMillis() - detailProgressTime) / 1000);
                    final int secondsRemaining;
                    if (detailProgress > 0) {
                        secondsRemaining = (detailTotal - detailProgress) * secondsElapsed / detailProgress;
                    } else {
                        secondsRemaining = (detailTotal - detailProgress) * secondsElapsed;
                    }

                    map.waitDialog.setProgress(detailProgress);
                    if (secondsRemaining < 40) {
                        map.waitDialog.setMessage(map.res.getString(R.string.caches_downloading) + " " + map.res.getString(R.string.caches_eta_ltm));
                    } else {
                        final int minsRemaining = secondsRemaining / 60;
                        map.waitDialog.setMessage(map.res.getString(R.string.caches_downloading) + " " + map.res.getQuantityString(R.plurals.caches_eta_mins, minsRemaining, minsRemaining));
                    }
                }
            } else if (msg.what == FINISHED_LOADING_DETAILS && map.waitDialog != null) {
                map.waitDialog.dismiss();
                map.waitDialog.setOnCancelListener(null);
            }
        }

        @Override
        public void handleDispose() {
            final NewMap map = mapRef.get();
            if (map == null) {
                return;
            }
            if (map.loadDetailsThread != null) {
                map.loadDetailsThread.stopIt();
            }
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

        @NonNull
        Location currentLocation = Sensors.getInstance().currentGeo();
        float currentHeading;

        private long timeLastPositionOverlayCalculation = 0;
        private long timeLastDistanceCheck = 0;
        /**
         * weak reference to the outer class
         */
        @NonNull
        private final WeakReference<NewMap> mapRef;

        UpdateLoc(@NonNull final NewMap map) {
            mapRef = new WeakReference<>(map);
        }

        @Override
        public void updateGeoDir(@NonNull final GeoData geo, final float dir) {
            currentLocation = geo;
            currentHeading = AngleUtils.getDirectionNow(dir);
            repaintPositionOverlay();
        }

        @NonNull
        public Location getCurrentLocation() {
            return currentLocation;
        }

        /**
         * Repaint position overlay but only with a max frequency and if position or heading changes sufficiently.
         */
        void repaintPositionOverlay() {
            final long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > (timeLastPositionOverlayCalculation + MIN_UPDATE_INTERVAL)) {
                timeLastPositionOverlayCalculation = currentTimeMillis;

                try {
                    final NewMap map = mapRef.get();
                    if (map != null) {
                        final boolean needsRepaintForDistanceOrAccuracy = needsRepaintForDistanceOrAccuracy();
                        final boolean needsRepaintForHeading = needsRepaintForHeading();

                        if (needsRepaintForDistanceOrAccuracy && NewMap.followMyLocation) {
                            map.centerMap(new Geopoint(currentLocation));
                        }

                        if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {

                            map.historyLayer.setCoordinates(currentLocation);
                            map.navigationLayer.setCoordinates(currentLocation);
                            map.distanceView.setCoordinates(currentLocation);
                            map.distanceView.showRouteDistance();
                            map.positionLayer.setCoordinates(currentLocation);
                            map.positionLayer.setHeading(currentHeading);
                            map.positionLayer.requestRedraw();

                            if (null != map.proximityNotification && (timeLastDistanceCheck == 0 || currentTimeMillis > (timeLastDistanceCheck + MIN_UPDATE_INTERVAL))) {
                                map.proximityNotification.checkDistance(map.caches.getClosestDistanceInM(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude())));
                                timeLastDistanceCheck = System.currentTimeMillis();
                            }
                        }
                    }
                } catch (final RuntimeException e) {
                    Log.w("Failed to update location", e);
                }
            }
        }

        boolean needsRepaintForHeading() {
            final NewMap map = mapRef.get();
            if (map == null) {
                return false;
            }
            return Math.abs(AngleUtils.difference(currentHeading, map.positionLayer.getHeading())) > MIN_HEADING_DELTA;
        }

        boolean needsRepaintForDistanceOrAccuracy() {
            final NewMap map = mapRef.get();
            if (map == null) {
                return false;
            }
            final Location lastLocation = map.getCoordinates();

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

    private static class DragHandler implements OnMapDragListener {

        @NonNull
        private final WeakReference<NewMap> mapRef;

        DragHandler(@NonNull final NewMap parent) {
            mapRef = new WeakReference<>(parent);
        }

        @Override
        public void onDrag() {
            final NewMap map = mapRef.get();
            if (map != null && NewMap.followMyLocation) {
                NewMap.followMyLocation = false;
                map.switchMyLocationButton();
            }
        }
    }

    public void showSelection(@NonNull final List<GeoitemRef> items, final boolean longPressMode) {
        if (items.isEmpty()) {
            return;
        }

        if (items.size() == 1) {
            if (longPressMode) {
                if (Settings.isLongTapOnMapActivated()) {
                    toggleRouteItem(items.get(0));
                }
            } else {
                showPopup(items.get(0));
            }
            return;
        }
        try {
            final ArrayList<GeoitemRef> sorted = new ArrayList<>(items);
            Collections.sort(sorted, GeoitemRef.NAME_COMPARATOR);

            final ListAdapter adapter = new ArrayAdapter<GeoitemRef>(this, R.layout.cacheslist_item_select, sorted) {
                @NonNull
                @Override
                public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                    return GeoItemSelectorUtils.createGeoItemView(NewMap.this, getItem(position),
                            GeoItemSelectorUtils.getOrCreateView(NewMap.this, convertView, parent));
                }
            };

            final AlertDialog dialog = Dialogs.newBuilder(this)
                    .setTitle(res.getString(R.string.map_select_multiple_items))
                    .setAdapter(adapter, new SelectionClickListener(sorted, longPressMode))
                    .create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();

        } catch (final NotFoundException e) {
            Log.e("NewMap.showSelection", e);
        }
    }

    private class SelectionClickListener implements DialogInterface.OnClickListener {

        @NonNull
        private final List<GeoitemRef> items;
        private final boolean longPressMode;

        SelectionClickListener(@NonNull final List<GeoitemRef> items, final boolean longPressMode) {
            this.items = items;
            this.longPressMode = longPressMode;
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            if (which >= 0 && which < items.size()) {
                final GeoitemRef item = items.get(which);
                if (longPressMode) {
                    if (Settings.isLongTapOnMapActivated()) {
                        toggleRouteItem(item);
                    }
                } else {
                    showPopup(item);
                }
            }
        }

    }

    private void showPopup(final GeoitemRef item) {
        if (item == null || StringUtils.isEmpty(item.getGeocode())) {
            return;
        }

        try {
            if (item.getType() == CoordinatesType.CACHE) {
                final Geocache cache = DataStore.loadCache(item.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    popupGeocodes.add(cache.getGeocode());
                    CachePopup.startActivityAllowTarget(this, cache.getGeocode());
                    return;
                }
                return;
            }

            if (item.getType() == CoordinatesType.WAYPOINT && item.getId() >= 0) {
                popupGeocodes.add(item.getGeocode());
                WaypointPopup.startActivityAllowTarget(this, item.getId(), item.getGeocode());
            }

        } catch (final NotFoundException e) {
            Log.e("NewMap.showPopup", e);
        }
    }

    private void toggleRouteItem(final GeoitemRef item) {
        if (item == null || StringUtils.isEmpty(item.getGeocode())) {
            return;
        }
        if (individualRoute == null) {
            individualRoute = new IndividualRoute(this::setTarget);
        }
        individualRoute.toggleItem(this, new RouteItem(item), routeLayer);
        distanceView.showRouteDistance();
        ActivityMixin.invalidateOptionsMenu(this);
    }

    @Nullable
    private Geocache getSingleModeCache() {
        if (StringUtils.isNotBlank(mapOptions.geocode)) {
            return DataStore.loadCache(mapOptions.geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }
        return null;
    }

    @Nullable
    private Geocache getCurrentTargetCache() {
        if (StringUtils.isNotBlank(targetGeocode)) {
            return DataStore.loadCache(targetGeocode, LoadFlags.LOAD_CACHE_OR_DB);
        }
        return null;
    }

    private void setTarget(final Geopoint coords, final String geocode) {
        lastNavTarget = coords;
        if (navigationLayer != null) {
            navigationLayer.setDestination(lastNavTarget);
            navigationLayer.requestRedraw();
        }
        if (distanceView != null) {
            distanceView.setDestination(lastNavTarget);
            distanceView.setCoordinates(geoDirUpdate.getCurrentLocation());
        }
        if (StringUtils.isNotBlank(geocode)) {
            targetGeocode = geocode;
            final Geocache target = getCurrentTargetCache();
            targetView.setTarget(targetGeocode, target != null ? target.getName() : StringUtils.EMPTY);
        } else {
            targetGeocode = null;
            targetView.setTarget(null, null);
        }
        ActivityMixin.invalidateOptionsMenu(this);
    }

    private void savePrefs() {
        Settings.setMapZoom(this.mapMode, mapView.getMapZoomLevel());
        Settings.setMapCenter(new MapsforgeGeoPoint(mapView.getModel().mapViewPosition.getCenter()));
    }

    /**
     * Thread to store the caches in the viewport. Started by Activity.
     */

    private class LoadDetails extends Thread {

        private final DisposableHandler handler;
        private final Collection<String> geocodes;
        private final Set<Integer> listIds;

        LoadDetails(final DisposableHandler handler, final Collection<String> geocodes, final Set<Integer> listIds) {
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
                    handler.sendEmptyMessage(UPDATE_PROGRESS);
                }
            }

            // we're done, but map might even have been closed.
            if (caches != null) {
                caches.invalidate(geocodes);
            }
            invalidateOptionsMenuCompatible();
            handler.sendEmptyMessage(FINISHED_LOADING_DETAILS);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AbstractDialogFragment.REQUEST_CODE_TARGET_INFO) {
            if (resultCode == AbstractDialogFragment.RESULT_CODE_SET_TARGET) {
                final TargetInfo targetInfo = data.getExtras().getParcelable(Intents.EXTRA_TARGET_INFO);
                if (targetInfo != null) {
                    if (Settings.isAutotargetIndividualRoute()) {
                        Settings.setAutotargetIndividualRoute(false);
                        Toast.makeText(this, R.string.map_disable_autotarget_individual_route, Toast.LENGTH_SHORT).show();
                    }
                    setTarget(targetInfo.coords, targetInfo.geocode);
                }
            }
            final List<String> changedGeocodes = new ArrayList<>();
            String geocode = popupGeocodes.poll();
            while (geocode != null) {
                changedGeocodes.add(geocode);
                geocode = popupGeocodes.poll();
            }
            if (caches != null) {
                caches.invalidate(changedGeocodes);
            }
        }
        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            mapOptions.filterContext = data.getParcelableExtra(EXTRA_FILTER_CONTEXT);
            refreshMapData(false);
        }

        this.trackUtils.onActivityResult(requestCode, resultCode, data);
        this.individualRouteUtils.onActivityResult(requestCode, resultCode, data);
    }

    private void setTracks(final Route route) {
        tracks = route;
        resumeTrack(null == tracks);
        this.trackUtils.showTrackInfo(tracks);
    }

    private void reloadIndividualRoute() {
        if (null != routeLayer) {
            individualRoute.reloadRoute(routeLayer);
        } else {
            // try again in 0.25 second
            new Handler(Looper.getMainLooper()).postDelayed(this::reloadIndividualRoute, 250);
        }
    }

    private static class ResourceBitmapCacheMonitor {

        private static int refCount = 0;

        static synchronized void addRef() {
            refCount++;
            Log.d("ResourceBitmapCacheMonitor.addRef");
        }

        static synchronized void release() {
            if (refCount > 0) {
                refCount--;
                Log.d("ResourceBitmapCacheMonitor.release");
                if (refCount == 0) {
                    Log.d("ResourceBitmapCacheMonitor.clearResourceBitmaps");
                    AndroidResourceBitmap.clearResourceBitmaps();
                }
            }
        }

    }

    public boolean getLastCompactIconMode() {
        return lastCompactIconMode;
    }

    public boolean checkCompactIconMode(final int overlayId, final int newCount) {
        boolean newCompactIconMode = lastCompactIconMode;
        if (null != caches) {
            newCompactIconMode = CompactIconModeUtils.forceCompactIconMode(caches.getVisibleCachesCount(overlayId, newCount));
            if (lastCompactIconMode != newCompactIconMode) {
                lastCompactIconMode = newCompactIconMode;
                // @todo Exchanging & redrawing the icons would be sufficient, do not have to invalidate everything!
                caches.invalidateAll(overlayId); // redraw all icons except for the given overlay
            }
        }
        return newCompactIconMode;
    }

    // get notified for viewport changes (zoom/pan)
    public void onChange() {
        final Viewport newViewport = mapView.getViewport();
        if (!newViewport.equals(lastViewport)) {
            lastViewport = newViewport;
            checkCompactIconMode(NO_OVERLAY_ID, 0);
        }
    }

    public static class MapAttributionDisplayHandler implements View.OnClickListener {

        private Supplier<ImmutablePair<String, Boolean>> attributionSupplier;
        private ImmutablePair<String, Boolean> attributionPair;

        public MapAttributionDisplayHandler(final Supplier<ImmutablePair<String, Boolean>> attributionSupplier) {
            this.attributionSupplier = attributionSupplier;
        }

        @Override
        public void onClick(final View v) {

            if (this.attributionPair == null) {
                this.attributionPair = attributionSupplier.get();
                if (this.attributionPair == null || this.attributionPair.left == null) {
                    this.attributionPair = new ImmutablePair<>("---", false);
                }
                this.attributionSupplier = null; //prevent possible memory leaks
            }
            displayMapAttribution(v.getContext(), this.attributionPair.left, this.attributionPair.right);
        }
    }

    private static void displayMapAttribution(final Context ctx, final String attribution, final boolean linkify) {

        //create text message
        CharSequence message = HtmlCompat.fromHtml(attribution, HtmlCompat.FROM_HTML_MODE_LEGACY);
        if (linkify) {
            final SpannableString s = new SpannableString(message);
            Linkify.addLinks(s, Linkify.ALL);
            message = s;
        }

        final AlertDialog alertDialog = Dialogs.newBuilder(ctx)
            .setTitle(ctx.getString(R.string.map_source_attribution_dialog_title))
            .setCancelable(true)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (dialog, pos) -> dialog.dismiss())
            .create();
        alertDialog.show();

        // Make the URLs in TextView clickable. Must be called after show()
        // Note: we do NOT use the "setView()" option of AlertDialog because this screws up the layout
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

    }


}
