package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.AbstractDialogFragment;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.AbstractNavigationBarMapActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.location.WaypointDistanceInfo;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.maps.MapOptions;
import cgeo.geocaching.maps.MapSettingsUtils;
import cgeo.geocaching.maps.MapStarUtils;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.INamedGeoCoordinate;
import cgeo.geocaching.models.MapSelectableItem;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.service.CacheDownloaderService;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CacheListActionBarChooser;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.RepeatOnHoldListener;
import cgeo.geocaching.ui.ToggleItemType;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.dialog.SimplePopupMenu;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemTestLayer;
import cgeo.geocaching.unifiedmap.layers.CacheCirclesLayer;
import cgeo.geocaching.unifiedmap.layers.CoordsIndicatorLayer;
import cgeo.geocaching.unifiedmap.layers.ElevationChart;
import cgeo.geocaching.unifiedmap.layers.GeoItemsLayer;
import cgeo.geocaching.unifiedmap.layers.IndividualRouteLayer;
import cgeo.geocaching.unifiedmap.layers.NavigationTargetLayer;
import cgeo.geocaching.unifiedmap.layers.PositionHistoryLayer;
import cgeo.geocaching.unifiedmap.layers.PositionLayer;
import cgeo.geocaching.unifiedmap.layers.TracksLayer;
import cgeo.geocaching.unifiedmap.layers.WherigoLayer;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.HideActionBarUtils;
import cgeo.geocaching.utils.HistoryTrackUtils;
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.wherigo.WherigoGame;
import cgeo.geocaching.wherigo.WherigoThingType;
import cgeo.geocaching.wherigo.WherigoViewUtils;
import static cgeo.geocaching.filters.gui.GeocacheFilterActivity.EXTRA_FILTER_CONTEXT;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO_LOWPOWER;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO_PRECISE;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;
import static cgeo.geocaching.settings.Settings.MAPROTATION_OFF;
import static cgeo.geocaching.unifiedmap.UnifiedMapState.BUNDLE_MAPSTATE;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.BUNDLE_MAPTYPE;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_List;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_PlainMap;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_SearchResult;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_TargetCoords;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_TargetGeocode;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_Viewport;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_LANGUAGE_DEFAULT_ID;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import cz.matejcik.openwig.Zone;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.oscim.core.BoundingBox;

public class UnifiedMapActivity extends AbstractNavigationBarMapActivity implements FilteredActivity, AbstractDialogFragment.TargetUpdateReceiver {

    // Activity should only contain display logic, everything else goes into the ViewModel

    private static final String STATE_ROUTETRACKUTILS = "routetrackutils";
    private static final String BUNDLE_FILTERCONTEXT = "filterContext";
    private static final String BUNDLE_OVERRIDEPOSITIONANDZOOM = "overridePositionAndZoom";

    private static final String ROUTING_SERVICE_KEY = "UnifiedMap";

    private UnifiedMapViewModel viewModel = null;
    private AbstractTileProvider tileProvider = null;
    private AbstractMapFragment mapFragment = null;
    private final List<GeoItemLayer<?>> layers = new ArrayList<>();
    GeoItemLayer<String> clickableItemsLayer;
    GeoItemLayer<String> nonClickableItemsLayer;
    NavigationTargetLayer navigationTargetLayer = null;
    private LoadInBackgroundHandler loadInBackgroundHandler = null;

    private LocUpdater geoDirUpdate;
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private final int[] inFollowMyLocation = { 0 };

    private RouteTrackUtils routeTrackUtils = null;
    private ElevationChart elevationChartUtils = null;
    private String lastElevationChartRoute = null; // null=none, empty=individual route, other=track

    private UnifiedMapType mapType = null;
    private MapMode compatibilityMapMode = MapMode.LIVE;
    private boolean overridePositionAndZoom = false; // to preserve those on config changes in favour to mapType defaults

    private int wherigoListenerId;

    private final CacheListActionBarChooser listChooser = new CacheListActionBarChooser(this, this::getSupportActionBar, newListId -> {
        final Optional<AbstractList> lNew = StoredList.UserInterface.getMenuLists(false, PseudoList.NEW_LIST.id).stream().filter(l2 -> l2.id == newListId).findFirst();
        if (lNew.isPresent()) {
            new UnifiedMapType(newListId).launchMap(this);
            finish();
        }
    });

    private UnifiedMapState lastMapStateFromOnPause = null;
    private static WeakReference<UnifiedMapActivity> unifiedMapActivity = new WeakReference<>(null);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        acquireUnifiedMap(this);

        HideActionBarUtils.setContentView(this, R.layout.unifiedmap_activity, true);

        viewModel = new ViewModelProvider(this).get(UnifiedMapViewModel.class);

        // get data from intent
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mapType = extras.getParcelable(BUNDLE_MAPTYPE);
        }
        setMapModeFromMapType();

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_MAPTYPE)) {
                mapType = savedInstanceState.getParcelable(BUNDLE_MAPTYPE);
            }
            if (mapType != null) {
                mapType.filterContext = savedInstanceState.getParcelable(BUNDLE_FILTERCONTEXT);
            }
            overridePositionAndZoom = savedInstanceState.getBoolean(BUNDLE_OVERRIDEPOSITIONANDZOOM, false);
        }

        // make sure we have a defined mapType
        if (mapType == null || mapType.type == UnifiedMapType.UnifiedMapTypeType.UMTT_Undefined) {
            mapType = new UnifiedMapType();
        }

        viewModel.followMyLocation.setValue(mapType.followMyLocation);

        viewModel.transientIsLiveEnabled.setValue(mapType.enableLiveMap() && Settings.isLiveMap());

        routeTrackUtils = new RouteTrackUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_ROUTETRACKUTILS), this::centerMap, viewModel::clearIndividualRoute, viewModel::reloadIndividualRoute, viewModel::setTrack, this::isTargetSet);
        viewModel.configureProximityNotification();
        if (viewModel.proximityNotification.getValue() != null) {
            viewModel.proximityNotification.getValue().setTextNotifications(this);
        }

        viewModel.trackUpdater.observe(this, event -> routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue()));
        viewModel.individualRoute.observe(this, individualRoute -> routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute));
        viewModel.followMyLocation.observe(this, this::initFollowMyLocation);
        viewModel.location.observe(this, this::handleLocUpdate);

        //wherigo
        final View view = findViewById(R.id.map_wherigo_popup);
        if (view != null) {
            view.setOnClickListener(v -> openWherigoPopup());
        }

        geoDirUpdate = new LocUpdater(this);

        // initialize layers
        layers.clear();

        clickableItemsLayer = new GeoItemLayer<>("clickableItems");
        nonClickableItemsLayer = new GeoItemLayer<>("nonClickableItems"); // default layer for all map items not worth an own layer

        layers.add(clickableItemsLayer);
        layers.add(nonClickableItemsLayer);

        new GeoItemTestLayer().initforUnifiedMap(clickableItemsLayer);

        new PositionLayer(this, nonClickableItemsLayer);
        new CoordsIndicatorLayer(this, nonClickableItemsLayer);
        new PositionHistoryLayer(this, nonClickableItemsLayer);
        new TracksLayer(this, clickableItemsLayer);
        navigationTargetLayer = new NavigationTargetLayer(this, nonClickableItemsLayer);
        new CacheCirclesLayer(this, nonClickableItemsLayer);

        new IndividualRouteLayer(this, clickableItemsLayer);
        new GeoItemsLayer(this, clickableItemsLayer);

        WherigoLayer.get().setLayer(clickableItemsLayer);

        viewModel.init(routeTrackUtils);

        final UnifiedMapState mapState = savedInstanceState != null ? savedInstanceState.getParcelable(BUNDLE_MAPSTATE) : null;
        changeMapSource(Settings.getTileProvider(), mapState);

        FilterUtils.initializeFilterBar(this, this);
        MapUtils.updateFilterBar(this, mapType.filterContext);

        Routing.connect(ROUTING_SERVICE_KEY, () -> viewModel.reloadIndividualRoute(), this);
        viewModel.reloadIndividualRoute();

        CompactIconModeUtils.setCompactIconModeThreshold(getResources());

        viewModel.mapCenter.observe(this, center -> refreshListChooser());
        viewModel.caches.observeForNotification(this, this::refreshListChooser);

        MapUtils.showMapOneTimeMessages(this, compatibilityMapMode);

        getLifecycle().addObserver(new GeocacheChangedBroadcastReceiver(this, true) {
            @Override
            protected void onReceive(final Context context, final String geocode) {
                handleGeocodeChangedBroadcastReceived(geocode);
            }
        });

        getLifecycle().addObserver(new LifecycleAwareBroadcastReceiver(this, Intents.ACTION_INDIVIDUALROUTE_CHANGED) {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                viewModel.reloadIndividualRoute();
            }
        });

        getLifecycle().addObserver(new LifecycleAwareBroadcastReceiver(this, Intents.ACTION_ELEVATIONCHART_CLOSED) {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                lastElevationChartRoute = null;
            }
        });
        add429observer();

        findViewById(R.id.map_zoomin).setOnTouchListener(new RepeatOnHoldListener(500, v -> {
            if (mapFragment != null) {
                mapFragment.zoomInOut(true);
            }
        }));
        findViewById(R.id.map_zoomout).setOnTouchListener(new RepeatOnHoldListener(500, v -> {
            if (mapFragment != null) {
                mapFragment.zoomInOut(false);
            }
        }));

        refreshListChooser();
    }

    public AbstractMapFragment getMapFragment() {
        return mapFragment;
    }

    public List<GeoItemLayer<?>> getLayers() {
        return layers;
    }

    private void setMapModeFromMapType() {
        if (mapType == null) {
            return;
        }
        if (mapType.type == UMTT_TargetGeocode) {
            compatibilityMapMode = MapMode.SINGLE;
        } else if (mapType.type == UMTT_TargetCoords) {
            compatibilityMapMode = MapMode.COORDS;
        } else if (mapType.type == UMTT_List) {
            compatibilityMapMode = MapMode.LIST;
        } else if (mapType.type == UMTT_SearchResult) {
            compatibilityMapMode = MapMode.LIST;
        } else {
            compatibilityMapMode = MapMode.LIVE;
        }
    }

    private void changeMapSource(final AbstractTileProvider newSource, @Nullable final UnifiedMapState mapState) {
        final AbstractTileProvider oldProvider = tileProvider;
        final AbstractMapFragment oldFragment = mapFragment;

        if (oldProvider != null && oldFragment != null) {
            oldFragment.prepareForTileSourceChange();
        }
        tileProvider = newSource;
        mapFragment = tileProvider.createMapFragment();
        Settings.setTileProvider(newSource); // store new tileProvider, so that next getRenderTheme retrieves correct tileProvider-specific theme

        if (oldFragment != null) {
            mapFragment.init(oldFragment.getCurrentZoom(), oldFragment.getCenter(), () -> onMapReadyTasks(newSource, mapState));
        } else {
            mapFragment.init(Settings.getMapZoom(compatibilityMapMode), null, () -> onMapReadyTasks(newSource, mapState));
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapViewFragment, mapFragment)
                .commit();
    }

    private void onMapReadyTasks(final AbstractTileProvider newSource, @Nullable final UnifiedMapState mapState) {
        TileProviderFactory.resetLanguages();
        mapFragment.setTileSource(newSource, false);
        Settings.setTileProvider(newSource);

        final boolean setDefaultCenterAndZoom = !overridePositionAndZoom && mapState == null;
        // map settings popup
        findViewById(R.id.map_settings_popup).setOnClickListener(v -> MapSettingsUtils.showSettingsPopup(this, viewModel.individualRoute.getValue(), this::refreshMapDataAfterSettingsChanged, this::routingModeChanged, this::compactIconModeChanged, () -> viewModel.configureProximityNotification(), mapType.filterContext));

        // routes / tracks popup
        findViewById(R.id.map_individualroute_popup).setOnClickListener(v -> routeTrackUtils.showPopup(viewModel.individualRoute.getValue(), viewModel::setTarget));
        routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue());

        // react to mapType
        setMapModeFromMapType();
        viewModel.coordsIndicator.setValue(null);
        reloadCachesAndWaypoints(setDefaultCenterAndZoom);

        hideProgressSpinner();

        // override some settings, if given
        if (mapState != null) {
            mapFragment.setCenter(mapState.center);
            mapFragment.setZoom(mapState.zoomLevel);
        }
        // both maps have invalid values for bounding box in the beginning, so need to delay counting a bit
        ActivityMixin.postDelayed(this::refreshListChooser, 2000);

        // refresh options menu and routes/tracks display
        invalidateOptionsMenu();
        setMapRotation(null, Settings.getMapRotation());
    }

    private void reloadCachesAndWaypoints(final boolean setDefaultCenterAndZoom) {
        switch (mapType.type) {
            case UMTT_PlainMap:
                // restore last saved position and zoom
                if (setDefaultCenterAndZoom) {
                    mapFragment.setZoom(Settings.getMapZoom(compatibilityMapMode));
                    mapFragment.setCenter(Settings.getUMMapCenter());
                }
                break;
            case UMTT_Viewport:
                // set bounds to given viewport
                if (setDefaultCenterAndZoom && mapType.viewport != null) {
                    mapFragment.setCenter(mapType.viewport.getCenter());
                    mapFragment.zoomToBounds(mapType.viewport);
                }
                break;
            case UMTT_TargetGeocode: // can be either a cache or a waypoint
                // load cache/waypoint, focus map on it, and set it as target
                final Geocache cache = DataStore.loadCache(mapType.target, LoadFlags.LOAD_WAYPOINTS);
                if (cache != null) {
                    if (setDefaultCenterAndZoom) {
                        mapFragment.zoomToBounds(DataStore.getBounds(mapType.target, Settings.getZoomIncludingWaypoints()));
                    }
                    if (mapType.waypointId > 0) { // single waypoint mode: display waypoint only
                        final Waypoint waypoint = cache.getWaypointById(mapType.waypointId);
                        if (waypoint != null) {
                            if (setDefaultCenterAndZoom) {
                                mapFragment.setCenter(waypoint.getCoords());
                            }
                            viewModel.waypoints.write(wps -> {
                                wps.clear();
                                wps.add(waypoint);
                            });
                            if (!isTargetSet()) {
                                onReceiveTargetUpdate(new AbstractDialogFragment.TargetInfo(waypoint.getCoords(), waypoint.getName()));
                            }
                        }
                    } else if (cache.getCoords() != null) { // geocache mode: display geocache and its waypoints
                        viewModel.caches.write(false, c -> c.add(cache));
                        if (setDefaultCenterAndZoom) {
                            mapFragment.setCenter(cache.getCoords());
                        }
                        viewModel.waypoints.write(wps -> {
                            wps.clear();
                            final Set<Waypoint> waypoints = new HashSet<>(cache.getWaypoints());
                            MapUtils.filter(waypoints, getFilterContext());
                            wps.addAll(waypoints);
                        });
                        if (!isTargetSet()) {
                            onReceiveTargetUpdate(new AbstractDialogFragment.TargetInfo(cache.getCoords(), cache.getGeocode()));
                        }
                    }
                    viewModel.waypoints.notifyDataChanged();
                }
                break;
            case UMTT_TargetCoords:
                // set given coords as map center
                if (setDefaultCenterAndZoom) {
                    mapFragment.setCenter(mapType.coords);
                }
                viewModel.coordsIndicator.setValue(mapType.coords);
                break;
            case UMTT_List:
                // load list of caches belonging to list and scale map to see them all
                final AtomicReference<Viewport> viewport3 = new AtomicReference<>();
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                    final SearchResult searchResult = DataStore.getBatchOfStoredCaches(null, mapType.fromList, mapType.filterContext.get(), null, false, -1);
                    viewport3.set(DataStore.getBounds(searchResult.getGeocodes(), Settings.getZoomIncludingWaypoints()));
                    addSearchResultByGeocaches(searchResult);
                    if (viewport3.get() != null) {
                        loadWaypoints(this, viewModel, viewport3.get());
                    }
                }, () -> {
                    if (viewport3.get() != null) {
                        if (setDefaultCenterAndZoom) {
                            mapFragment.zoomToBounds(viewport3.get());
                        }
                        refreshMapData(true);
                    }
                });
                break;
            case UMTT_SearchResult:
                // load list of caches and scale map to see them all
                final AtomicReference<Viewport> viewport2 = new AtomicReference<>();
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                    viewport2.set(DataStore.getBounds(mapType.searchResult.getGeocodes(), Settings.getZoomIncludingWaypoints()));
                    addSearchResultByGeocaches(mapType.searchResult);
                    if (viewport2.get() != null) {
                        loadWaypoints(this, viewModel, viewport2.get());
                    }
                }, () -> {
                    if (viewport2.get() != null) {
                        if (setDefaultCenterAndZoom) {
                            mapFragment.zoomToBounds(viewport2.get());
                        }
                        refreshMapData(true);
                    }
                });
                break;
            default:
                // nothing to do
                break;
        }
        if (overridePositionAndZoom) {
            mapFragment.setZoom(Settings.getMapZoom(compatibilityMapMode));
            mapFragment.setCenter(Settings.getUMMapCenter());
            overridePositionAndZoom = false;
        }
        refreshListChooser();

        // only initialize loadInBackgroundHandler if caches should actually be loaded
        if (mapType.enableLiveMap()) {
            refreshMapData(true);
            if (loadInBackgroundHandler == null) {
                loadInBackgroundHandler = new LoadInBackgroundHandler(this);
            }
        }
    }

    private void handleGeocodeChangedBroadcastReceived(final String geocode) {
        //add info from cache to viewmodel
        final Geocache changedCache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (changedCache == null || changedCache.getCoords() == null) {
            return;
        }
        viewModel.caches.write(caches -> caches.add(changedCache));
        final List<Waypoint> cacheWaypoints = DataStore.loadWaypoints(geocode);
        if (cacheWaypoints != null) {
            for (Waypoint wp : cacheWaypoints) {
                wp.recalculateVariableDependentValues(changedCache.getVariables());
            }
            viewModel.waypoints.write(waypoints -> waypoints.addAll(cacheWaypoints));
        }
        //call reload logic -> this will reapply filters and such
        reloadCachesAndWaypoints(false);
    }

    private void compactIconModeChanged(final int newValue) {
        Settings.setCompactIconMode(newValue);
        viewModel.caches.notifyDataChanged(true); // TODO: necessary?
    }

    public void showProgressSpinner() {
        final View spinner = findViewById(R.id.map_progressbar);
        spinner.setVisibility(View.VISIBLE);
    }

    public void hideProgressSpinner() {
        final View spinner = findViewById(R.id.map_progressbar);
        spinner.setVisibility(View.GONE);
    }

    public void loadWaypoints(final UnifiedMapActivity activity, final UnifiedMapViewModel viewModel, final Viewport viewport) {
        if (mapFragment == null) {
            return;
        }
        final Viewport currentlyVisible = mapFragment.getViewport();
        if (viewModel.caches.readWithResult(currentlyVisible::count) < Settings.getWayPointsThreshold()) {
            final Set<Waypoint> waypoints;
            if (Boolean.TRUE.equals(viewModel.transientIsLiveEnabled.getValue())) {
                //All waypoints in given viewport
                waypoints = DataStore.loadWaypoints(viewport);
            } else {
                waypoints = new HashSet<>();
                //All waypoints from the viewed caches
                viewModel.caches.read(caches -> {
                    for (final Geocache c : caches) {
                        waypoints.addAll(c.getWaypoints());
                    }
                });

            }
            Log.d("load.waypoints: " + waypoints.size());
            MapUtils.filter(waypoints, activity.getFilterContext());
            viewModel.waypoints.write(wps -> {
                wps.clear();
                wps.addAll(waypoints);
            });
        } else {
            viewModel.waypoints.notifyDataChanged();
        }
    }

    public void addSearchResultByGeocaches(final SearchResult searchResult) {
        Log.d("add " + searchResult.getGeocodes());
        final Set<Geocache> geocaches = DataStore.loadCaches(searchResult.getGeocodes(), LoadFlags.LOAD_CACHE_OR_DB);
        CommonUtils.filterCollection(geocaches, cache -> cache != null && cache.getCoords() != null);
        if (!geocaches.isEmpty()) {
            viewModel.caches.write(true, caches -> { // use post to make it background capable
                caches.removeAll(geocaches);
                caches.addAll(geocaches);
            });
        }
    }

    public void addSearchResultByGeocaches(final Set<Geocache> searchResult) {
        Log.d("addSearchResult: " + searchResult.size());
        viewModel.caches.write(true, caches -> { // use post to make it background capable
            caches.removeAll(searchResult);
            for (Geocache geocache : searchResult) {
                if (geocache.getCoords() != null) {
                    caches.add(geocache);
                }
            }
        });
    }

    @NonNull
    private String calculateTitle() {
        if (Boolean.TRUE.equals(viewModel.transientIsLiveEnabled.getValue())) {
            return getString(R.string.map_live);
        }
        if (mapType.type == UMTT_TargetGeocode) {
            final Geocache cache = DataStore.loadCache(mapType.target, LoadFlags.LOAD_CACHE_OR_DB);
            if (cache != null && cache.getCoords() != null) {
                return StringUtils.defaultIfBlank(cache.getName(), "");
            }
        }
        return StringUtils.defaultIfEmpty(mapType.title, getString(R.string.map_offline));
    }

    private void refreshListChooser() {
        //try to get count of visible caches
        int visibleCaches = 0;
        if (mapFragment != null && mapFragment.getViewport() != null && viewModel != null) {
            visibleCaches = viewModel.caches.readWithResult(mapFragment.getViewport()::count);
        }

        if (mapType.fromList != 0) {
            //List
            listChooser.setList(mapType.fromList, visibleCaches, false);
        } else if (mapType.type == UMTT_TargetGeocode) {
            //single cache
            final Geocache targetCache = getCurrentTargetCache();
            if (targetCache != null) {
                listChooser.setDirect(calculateTitle(), Formatter.formatMapSubtitle(targetCache));
            } else {
                listChooser.setDirect(calculateTitle(), visibleCaches);
            }
        } else {
            //all others, e.g. "Live"
            listChooser.setDirect(calculateTitle(), visibleCaches);
        }

        CompactIconModeUtils.forceCompactIconMode(visibleCaches);
    }

    /**
     * centers map on coords given + resets "followMyLocation" state
     **/
    private void centerMap(final Geopoint geopoint) {
        viewModel.followMyLocation.setValue(false);
        mapFragment.setCenter(geopoint);
    }

    private void initFollowMyLocation(final boolean followMyLocation) {
        synchronized (inFollowMyLocation) {
            if (mapFragment == null || inFollowMyLocation[0] > 0) {
                return;
            }
            inFollowMyLocation[0]++;
        }
        Settings.setFollowMyLocation(followMyLocation);

        final ImageButton followMyLocationButton = findViewById(R.id.map_followmylocation_btn);
        if (followMyLocationButton != null) { // can be null after screen rotation
            followMyLocationButton.setImageResource(followMyLocation ? R.drawable.map_followmylocation_btn : R.drawable.map_followmylocation_off_btn);
            followMyLocationButton.setOnClickListener(v -> viewModel.followMyLocation.setValue(Boolean.FALSE.equals(viewModel.followMyLocation.getValue())));
        }

        if (followMyLocation) {
            final Location currentLocation = LocationDataProvider.getInstance().currentGeo(); // get location even if none was delivered to the view-model yet
            mapFragment.setCenter(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
        }
        checkDrivingMode();
        synchronized (inFollowMyLocation) {
            inFollowMyLocation[0]--;
        }
    }

    private void handleLocUpdate(final LocUpdater.LocationWrapper locationWrapper) {
        // no need to handle location update if map fragment is gone
        if (mapFragment == null) {
            return;
        }

        final int mapRotation = Settings.getMapRotation();
        if (locationWrapper.needsRepaintForHeading && (mapRotation == MAPROTATION_AUTO_LOWPOWER || mapRotation == MAPROTATION_AUTO_PRECISE)) {
            mapFragment.setBearing(locationWrapper.heading);
        }

        if (locationWrapper.needsRepaintForDistanceOrAccuracy && Boolean.TRUE.equals(viewModel.followMyLocation.getValue())) {
            mapFragment.setCenter(new Geopoint(locationWrapper.location));

            if (viewModel.proximityNotification.getValue() != null) {
                viewModel.proximityNotification.getValue().checkDistance(getClosestDistanceInM(new Geopoint(locationWrapper.location.getLatitude(), locationWrapper.location.getLongitude()), viewModel));
            }
        }
    }

    private void checkDrivingMode() {
        final int mapRotation = Settings.getMapRotation();
        final boolean shouldBeInDrivingMode = Boolean.TRUE.equals(viewModel.followMyLocation.getValue()) && (mapRotation == MAPROTATION_AUTO_LOWPOWER || mapRotation == MAPROTATION_AUTO_PRECISE);
        mapFragment.setDrivingMode(shouldBeInDrivingMode);
    }

    // ========================================================================
    // Routes, tracks and targets handling

    private void routingModeChanged(final RoutingMode newValue) {
        Settings.setRoutingMode(newValue);
        if ((null != viewModel.individualRoute && viewModel.individualRoute.getValue().getNumSegments() > 0) || null != viewModel.getTracks()) {
            ViewUtils.showShortToast(this, R.string.brouter_recalculating);
        }
        viewModel.reloadIndividualRoute();
        viewModel.reloadTracks(routeTrackUtils);
        if (navigationTargetLayer != null) {
            navigationTargetLayer.triggerRepaint();
        }
    }

    @Nullable
    public Geocache getCurrentTargetCache() {
        final UnifiedMapViewModel.Target target = viewModel.target.getValue();
        if (target != null && StringUtils.isNotBlank(target.geocode)) {
            return DataStore.loadCache(target.geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }
        return null;
    }

    // glue method for old map
    // can be removed when removing CGeoMap and NewMap, routeTrackUtils need to be adapted then
    @SuppressWarnings("unused")
    private void centerMap(final double latitude, final double longitude, final Viewport viewport) {
        centerMap(new Geopoint(latitude, longitude)); // todo consider viewport
    }

    private void updateRouteTrackButtonVisibility() {
        routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue(), viewModel.getTracks());
    }

    private boolean isTargetSet() {
        return viewModel.target.getValue() != null;
    }

    // ========================================================================
    // Bottom navigation methods

    @Override
    public int getSelectedBottomItemId() {
        return mapType == null || mapType.type == UMTT_PlainMap || mapType.type == UMTT_Viewport || mapType.type == UMTT_List || mapType.type == UMTT_TargetCoords ? MENU_MAP : MENU_HIDE_NAVIGATIONBAR;
    }

    // ========================================================================
    // Menu handling

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final boolean result = super.onPrepareOptionsMenu(menu);
        TileProviderFactory.addMapViewLanguageMenuItems(menu);
        ViewUtils.extendMenuActionBarDisplayItemCount(this, menu);
        HistoryTrackUtils.onPrepareOptionsMenu(menu);

        // init followMyLocation
        initFollowMyLocation(Boolean.TRUE.equals(viewModel.followMyLocation.getValue()));

        // live map mode
        final MenuItem itemMapLive = menu.findItem(R.id.menu_map_live);
        ToggleItemType.LIVE_MODE.toggleMenuItem(itemMapLive, Boolean.TRUE.equals(viewModel.transientIsLiveEnabled.getValue()));
        itemMapLive.setVisible(true);

        // map rotation state
        menu.findItem(R.id.menu_map_rotation).setVisible(true); // @todo: can be visible always (xml definition) when CGeoMap/NewMap is removed
        final int mapRotation = Settings.getMapRotation();
        switch (mapRotation) {
            case MAPROTATION_OFF:
                menu.findItem(R.id.menu_map_rotation_off).setChecked(true);
                break;
            case MAPROTATION_MANUAL:
                menu.findItem(R.id.menu_map_rotation_manual).setChecked(true);
                break;
            case MAPROTATION_AUTO_LOWPOWER:
                menu.findItem(R.id.menu_map_rotation_auto_lowpower).setChecked(true);
                break;
            case MAPROTATION_AUTO_PRECISE:
                menu.findItem(R.id.menu_map_rotation_auto_precise).setChecked(true);
                break;
            default:
                break;
        }
        menu.findItem(R.id.menu_map_rotation_auto_precise).setVisible(true); // UnifiedMap supports high precision auto-rotate

        // map and theming options
        menu.findItem(R.id.menu_theme_mode).setVisible(tileProvider.supportsThemes());
        menu.findItem(R.id.menu_theme_options).setVisible(tileProvider.supportsThemeOptions());

        menu.findItem(R.id.menu_as_list).setVisible(true);
        MapUtils.onPrepareOptionsMenu(menu);

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.map_activity, menu);
        FilterUtils.initializeFilterMenu(this, this);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_map_live) {
            if (mapType.enableLiveMap()) {
                Settings.setLiveMap(!Settings.isLiveMap());
                viewModel.transientIsLiveEnabled.setValue(Settings.isLiveMap());
                ActivityMixin.invalidateOptionsMenu(this);
                refreshListChooser();
                setMapModeFromMapType();
                if (Boolean.TRUE.equals(viewModel.transientIsLiveEnabled.getValue())) {
                    reloadCachesAndWaypoints(false);
                }
            } else {
                mapType = new UnifiedMapType();
                viewModel.transientIsLiveEnabled.setValue(true);
                refreshListChooser();
                MapUtils.updateFilterBar(this, mapType.filterContext);
                updateSelectedBottomNavItemId();
                setMapModeFromMapType(); // to get zoomLevel stored for right mode
                saveCenterAndZoom();
                final Geopoint coordsIndicator = viewModel.coordsIndicator.getValue();
                onMapReadyTasks(tileProvider, getCurrentMapState());
                viewModel.coordsIndicator.setValue(coordsIndicator);
            }
        } else if (id == R.id.menu_map_rotation_off) {
            setMapRotation(item, MAPROTATION_OFF);
        } else if (id == R.id.menu_map_rotation_manual) {
            setMapRotation(item, MAPROTATION_MANUAL);
        } else if (id == R.id.menu_map_rotation_auto_lowpower) {
            setMapRotation(item, MAPROTATION_AUTO_LOWPOWER);
        } else if (id == R.id.menu_map_rotation_auto_precise) {
            setMapRotation(item, MAPROTATION_AUTO_PRECISE);
        } else if (id == R.id.menu_check_routingdata) {
            final BoundingBox bb = mapFragment.getBoundingBox();
            MapUtils.checkRoutingData(this, bb.getMinLatitude(), bb.getMinLongitude(), bb.getMaxLatitude(), bb.getMaxLongitude());
        } else if (id == R.id.menu_check_hillshadingdata) {
            final BoundingBox bb = mapFragment.getBoundingBox();
            MapUtils.checkHillshadingData(this, bb.getMinLatitude(), bb.getMinLongitude(), bb.getMaxLatitude(), bb.getMaxLongitude());
        } else if (HistoryTrackUtils.onOptionsItemSelected(this, id, () -> viewModel.positionHistory.setValue(viewModel.positionHistory.getValue()), () -> {
            PositionHistory positionHistory = viewModel.positionHistory.getValue();
            if (positionHistory == null) {
                positionHistory = new PositionHistory();
            }
            positionHistory.reset();
        })
                || DownloaderUtils.onOptionsItemSelected(this, id)) {
            return true;
        } else if (id == R.id.menu_filter) {
            showFilterMenu();
        } else if (id == R.id.menu_store_caches) {
            final Set<String> geocodes = viewModel.caches.readWithResult(caches ->
                    mapFragment.getViewport()
                            .filter(caches)
                            .stream()
                            .map(Geocache::getGeocode)
                            .collect(Collectors.toSet()));
            CacheDownloaderService.downloadCaches(this, geocodes, false, false, () -> viewModel.caches.notifyDataChanged(false));
        } else if (id == R.id.menu_theme_mode) {
            mapFragment.selectTheme(this);
        } else if (id == R.id.menu_theme_options) {
            mapFragment.selectThemeOptions(this);
        } else if (id == R.id.menu_routetrack) {
            routeTrackUtils.showPopup(viewModel.individualRoute.getValue(), viewModel::setTarget);
        } else if (id == R.id.menu_select_mapview) {
            // dynamically create submenu to reflect possible changes in map sources
            final View v = findViewById(R.id.menu_select_mapview);
            if (v != null) {
                final PopupMenu menu = new PopupMenu(this, v, Gravity.TOP);
                menu.inflate(R.menu.map_mapview);
                TileProviderFactory.addMapviewMenuItems(this, menu);
                menu.setOnMenuItemClickListener(this::onOptionsItemSelected);
                menu.setForceShowIcon(true);
                menu.show();
            }
        } else if (id == R.id.menu_as_list) {
            final Collection<Geocache> caches = viewModel.caches.readWithResult(vmCaches ->
                    mapFragment.getViewport().filter(vmCaches));
            CacheListActivity.startActivityMap(this, new SearchResult(caches));
        } else if (id == R.id.menu_hillshading) {
            Settings.setMapShadingShowLayer(!Settings.getMapShadingShowLayer());
            item.setChecked(Settings.getMapShadingShowLayer());
            changeMapSource(mapFragment.currentTileProvider, getCurrentMapState());
        } else {
            final String language = TileProviderFactory.getLanguage(id);
            if (language != null || id == MAP_LANGUAGE_DEFAULT_ID) {
                item.setChecked(true);
                Settings.setMapLanguage(language);
                mapFragment.setPreferredLanguage(language);
                return true;
            }
            final AbstractTileProvider tileProviderLocal = TileProviderFactory.getTileProvider(id);
            if (tileProviderLocal != null) {
                item.setChecked(true);
                changeMapSource(tileProviderLocal, getCurrentMapState());
                return true;
            }
            if (mapFragment.onOptionsItemSelected(item)) {
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    // ========================================================================
    // zoom, bearing & heading methods

    private void saveCenterAndZoom() {
        Settings.setMapZoom(compatibilityMapMode, mapFragment.getCurrentZoom());
        Settings.setMapCenter(mapFragment.getCenter());
    }

    private void setMapRotation(@Nullable final MenuItem item, final int mapRotation) {
        Settings.setMapRotation(mapRotation);
        mapFragment.setMapRotation(mapRotation);
        if (item != null) {
            item.setChecked(true);
        }
        ViewUtils.setVisibility(findViewById(R.id.map_compassrose), mapRotation == MAPROTATION_OFF ? View.GONE : View.VISIBLE);
        checkDrivingMode();
    }

    // ========================================================================

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.routeTrackUtils.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            mapType.filterContext = data.getParcelableExtra(EXTRA_FILTER_CONTEXT);
            refreshMapData(true);
        }
    }

    @Override
    public void onReceiveTargetUpdate(final AbstractDialogFragment.TargetInfo targetInfo) {
        if (Settings.isAutotargetIndividualRoute()) {
            Settings.setAutotargetIndividualRoute(false);
            ViewUtils.showShortToast(this, R.string.map_disable_autotarget_individual_route);
        }
        viewModel.setTarget(targetInfo.coords, targetInfo.geocode);
    }

    // ========================================================================
    // Filter related methods

    @Override
    public void showFilterMenu() {
        FilterUtils.openFilterActivity(this, mapType.filterContext, viewModel.caches.getListCopy());
    }

    @Override
    public boolean showSavedFilterList() {
        return FilterUtils.openFilterList(this, mapType.filterContext);
    }

    @Override
    public void refreshWithFilter(final GeocacheFilter filter) {
        mapType.filterContext.set(filter);
        onResume();
    }

    protected GeocacheFilterContext getFilterContext() {
        return mapType.filterContext;
    }

    private void refreshMapDataAfterSettingsChanged(final boolean circlesSwitched, final boolean filterChanged) {
        // parameter "circlesSwitched" is required for being called by showSettingsPopup only; can be removed after removing old map implementations
        if (loadInBackgroundHandler == null && filterChanged) {
            reloadCachesAndWaypoints(false);
        }
        refreshMapData(filterChanged);
    }

    private void refreshMapData(final boolean filterChanged) {
        viewModel.caches.write(false, caches -> MapUtils.filter(caches, mapType.filterContext));
        viewModel.waypoints.notifyDataChanged();
        if (filterChanged) {
            if (loadInBackgroundHandler != null) {
                loadInBackgroundHandler.onDestroy();
                loadInBackgroundHandler = new LoadInBackgroundHandler(this);
            }
            MapUtils.updateFilterBar(this, mapType.filterContext);
        }
    }

    // ========================================================================
    // Map tap handling

    public void onTap(final int latitudeE6, final int longitudeE6, final int x, final int y, final boolean isLongTap) {

        final Geopoint touchedPoint = Geopoint.forE6(latitudeE6, longitudeE6);

        // lookup elements touched by this
        final LinkedList<MapSelectableItem> result = new LinkedList<>();

        for (String key : clickableItemsLayer.getTouched(Geopoint.forE6(latitudeE6, longitudeE6))) {

            if (key.startsWith(UnifiedMapViewModel.CACHE_KEY_PREFIX)) {
                final String geocode = key.substring(UnifiedMapViewModel.CACHE_KEY_PREFIX.length());

                final Geocache temp = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (temp != null && temp.getCoords() != null) {
                    result.add(new MapSelectableItem(new RouteItem(temp)));
                }
            }

            if (key.startsWith(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX) && isLongTap) { // only consider when tap was a longTap
                final String identifier = key.substring(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX.length());

                for (RouteItem item : viewModel.individualRoute.getValue().getRouteItems()) {
                    if (identifier.equals(item.getIdentifier())) {
                        result.add(new MapSelectableItem(item));
                        break;
                    }
                }
            }

            if (key.startsWith(UnifiedMapViewModel.WAYPOINT_KEY_PREFIX)) {
                final String fullGpxId = key.substring(UnifiedMapViewModel.WAYPOINT_KEY_PREFIX.length());
                viewModel.waypoints.read(wps -> {
                    for (Waypoint waypoint : wps) {
                        if (fullGpxId.equals(waypoint.getFullGpxId())) {
                            result.add(new MapSelectableItem(new RouteItem(waypoint)));
                            break;
                        }
                    }
                });
            }

            if (key.startsWith(IndividualRouteLayer.KEY_INDIVIDUAL_ROUTE) && isLongTap) {
                result.add(new MapSelectableItem(viewModel.individualRoute.getValue()));
            }
            if (key.startsWith(TracksLayer.TRACK_KEY_PREFIX) && viewModel.getTracks().getTrack(key.substring(TracksLayer.TRACK_KEY_PREFIX.length())).getRoute() instanceof Route && isLongTap) {
                result.add(new MapSelectableItem((Route) viewModel.getTracks().getTrack(key.substring(TracksLayer.TRACK_KEY_PREFIX.length())).getRoute()));
            }
            if (key.startsWith(WherigoLayer.WHERIGO_KEY_PRAEFIX)) {
                result.add(new MapSelectableItem(WherigoGame.get().getZone(key.substring(WherigoLayer.WHERIGO_KEY_PRAEFIX.length())),
                        key.substring(WherigoLayer.WHERIGO_KEY_PRAEFIX.length()), // Zone name
                        WherigoGame.get().getCartridgeName(), // Wherigo
                        WherigoThingType.LOCATION.getIconId()));
            }

            if (key.startsWith(GeoItemTestLayer.TESTLAYER_KEY_PREFIX)) {
                result.add(new MapSelectableItem(key, "Test item: " + key.substring(GeoItemTestLayer.TESTLAYER_KEY_PREFIX.length()), clickableItemsLayer.get(key).getType().toString(), -1));
            }



        }
        Log.d("touched elements on " + touchedPoint + " (" + result.size() + "): " + result);

        if (result.isEmpty()) {
            if (isLongTap) {
                viewModel.longTapCoords.setValue(touchedPoint);
                MapUtils.createMapLongClickPopupMenu(this, touchedPoint, new Point(x, y), viewModel.individualRoute.getValue(), route -> viewModel.individualRoute.notifyDataChanged(), this::updateRouteTrackButtonVisibility, getCurrentTargetCache(), new MapOptions(null, "", mapType.fromList), viewModel::setTarget)
                        .setOnDismissListener(d -> viewModel.longTapCoords.setValue(null))
                        .show();
            } else {
                if (sheetRemoveFragment()) {
                    return;
                }
                mapFragment.adaptLayoutForActionBar(HideActionBarUtils.toggleActionBar(this));
                GeoItemTestLayer.handleTapTest(clickableItemsLayer, this, touchedPoint, "", isLongTap);
            }
        } else if (result.size() == 1) {
            handleTap(result.get(0), touchedPoint, isLongTap, x, y);
        } else {
            try {
                final ArrayList<MapSelectableItem> sorted = new ArrayList<>(result);
                Collections.sort(sorted, MapSelectableItem.NAME_COMPARATOR);

                final SimpleDialog.ItemSelectModel<MapSelectableItem> model = new SimpleDialog.ItemSelectModel<>();
                model
                        .setItems(sorted)
                        .setDisplayViewMapper((item, itemGroup, ctx, view, parent) ->
                                        GeoItemSelectorUtils.createMapSelectableItemView(UnifiedMapActivity.this, item, GeoItemSelectorUtils.getOrCreateView(UnifiedMapActivity.this, view, parent)),
                                (item, itemGroup) -> item == null ? "" : item.getSortFilterString())
                        .setItemPadding(0);

                SimpleDialog.of(this).setTitle(R.string.map_select_multiple_items).selectSingle(model, item -> handleTap(item, touchedPoint, isLongTap, x, y));

            } catch (final Resources.NotFoundException e) {
                Log.e("UnifiedMapActivity.showSelection", e);
            }
        }

    }

    private void handleTap(final MapSelectableItem item, final Geopoint touchedPoint, final boolean isLongTap, final int tapX, final int tapY) {
        final RouteItem routeItem = item.isRouteItem() ? item.getRouteItem() : null;
        if (isLongTap) {
            // toggle route item
            if (routeItem != null && Settings.isLongTapOnMapActivated()) {

                final Geocache cache = routeItem.getGeocache();

                if (Settings.isShowRouteMenu()) {
                    final SimplePopupMenu menu = MapUtils.createCacheWaypointLongClickPopupMenu(this, routeItem, tapX, tapY, viewModel.individualRoute.getValue(), viewModel, null);
                    if (MapStarUtils.canHaveStar(cache)) {
                        final String geocode = routeItem.getGeocode();
                        viewModel.cachesWithStarDrawn.write(cwsd -> {
                            final boolean isStarDrawn = cwsd.contains(geocode);
                            MapStarUtils.addMenuIfNecessary(menu, cache, isStarDrawn, drawStar -> {
                                CommonUtils.addRemove(cwsd, geocode, !drawStar);
                                viewModel.cachesWithStarDrawn.notifyDataChanged();
                            });
                        });
                    }
                    menu.show();
                } else {
                    viewModel.toggleRouteItem(this, routeItem);
                }
            } else if (item.isRoute() && routeTrackUtils != null && item.getRoute() != null) {
                // individual route or track
                if (lastElevationChartRoute != null && StringUtils.equals(item.getRoute().getName(), lastElevationChartRoute)) {
                    elevationChartUtils.removeElevationChart();
                } else {
                    routeTrackUtils.showRouteTrackContextMenu(tapX, tapY, this::handleLongTapOnRoutesOrTracks, RouteTrackUtils.isIndividualRoute(item.getRoute()) ? viewModel.individualRoute.getValue() : item.getRoute());
                }
            }
        } else if (routeItem != null) {
            // open popup for element
            if (routeItem.getType() == RouteItem.RouteItemType.GEOCACHE) {
                viewModel.sheetInfo.setValue(new UnifiedMapViewModel.SheetInfo(routeItem.getGeocode(), 0));
                sheetShowDetails(viewModel.sheetInfo.getValue());
                MapMarkerUtils.addHighlighting(routeItem.getGeocache(), getResources(), nonClickableItemsLayer);
            } else if (routeItem.getType() == RouteItem.RouteItemType.WAYPOINT && routeItem.getWaypointId() != 0) {
                viewModel.sheetInfo.setValue(new UnifiedMapViewModel.SheetInfo(routeItem.getGeocode(), routeItem.getWaypointId()));
                sheetShowDetails(viewModel.sheetInfo.getValue());
                MapMarkerUtils.addHighlighting(routeItem.getWaypoint(), getResources(), nonClickableItemsLayer);
            }
        } else if (item.getData() instanceof Zone) {
            WherigoViewUtils.displayThing(this, item.getData(), false);
        } else if (item.getData() instanceof String) {
            GeoItemTestLayer.handleTapTest(clickableItemsLayer, this, touchedPoint, item.getData().toString(), isLongTap);
        }
    }

    private void handleLongTapOnRoutesOrTracks(final Route item) {
        // elevation charts for individual route and/or routes/tracks
        if (elevationChartUtils == null) {
            elevationChartUtils = new ElevationChart(this, nonClickableItemsLayer);
        }
        if (lastElevationChartRoute != null && StringUtils.equals(item.getName(), lastElevationChartRoute)) {
            elevationChartUtils.removeElevationChart();
        } else {
            elevationChartUtils.showElevationChart(item, routeTrackUtils);
            lastElevationChartRoute = item.getName();
            if (RouteTrackUtils.isIndividualRoute(item)) {
                viewModel.individualRoute.observe(this, individualRoute -> {
                    if (lastElevationChartRoute != null && lastElevationChartRoute.isEmpty()) { // still individual route being shown?
                        elevationChartUtils.showElevationChart(individualRoute, routeTrackUtils);
                    }
                });
            } else {
                viewModel.trackUpdater.observe(this, event -> {
                    if (viewModel.getTracks().getRoute(event.peek()) instanceof Route) {
                        final Route route = (Route) viewModel.getTracks().getRoute(event.peek());
                        if (route != null && StringUtils.equals(lastElevationChartRoute, route.getName())) {
                            elevationChartUtils.showElevationChart(route, routeTrackUtils);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void clearSheetInfo() {
        MapMarkerUtils.removeHighlighting(nonClickableItemsLayer);
        viewModel.sheetInfo.setValue(null);
    }

    // ========================================================================
    // distance checks for proximity notifications
    private static WaypointDistanceInfo getClosestDistanceInM(final Geopoint coord, final UnifiedMapViewModel viewModel) {
        WaypointDistanceInfo result;
        // work on a copy to avoid race conditions
        result = getClosestDistanceInM(coord, viewModel.caches.getListCopy(), Integer.MAX_VALUE, item -> ((Geocache) item).getShortGeocode() + " " + item.getName());
        result = getClosestDistanceInM(coord, viewModel.waypoints.getListCopy(), result.meters, item -> item.getName() + " (" + ((Waypoint) item).getWaypointType().gpx + ")");
        return result;
    }

    private static WaypointDistanceInfo getClosestDistanceInM(final Geopoint center, final List<? extends INamedGeoCoordinate> items, final int minDistanceOld, final Func1<INamedGeoCoordinate, String> getName) {
        int minDistance = minDistanceOld;
        String name = "";
        for (INamedGeoCoordinate item : items) {
            final Geopoint coords = item.getCoords();
            if (coords != null) {
                final int distance = (int) (1000f * coords.distanceTo(center));
                if (distance > 0 && distance < minDistance) {
                    minDistance = distance;
                    name = getName.call(item);
                }
            }
        }
        return new WaypointDistanceInfo(name, minDistance);
    }

    // ========================================================================
    // Lifecycle methods

    private UnifiedMapState getCurrentMapState() {
        return new UnifiedMapState(mapFragment.getCenter(), mapFragment.getCurrentZoom(), Boolean.TRUE.equals(viewModel.transientIsLiveEnabled.getValue()));
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_ROUTETRACKUTILS, routeTrackUtils.getState());

        outState.putParcelable(BUNDLE_MAPSTATE, lastMapStateFromOnPause);
        outState.putParcelable(BUNDLE_MAPTYPE, mapType);
        if (mapType.filterContext != null) {
            outState.putParcelable(BUNDLE_FILTERCONTEXT, mapType.filterContext);
        }
        outState.putBoolean(BUNDLE_OVERRIDEPOSITIONANDZOOM, true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // close outdated details popups & restart sheet (if required)
        sheetManageLifecycleOnStart(viewModel.sheetInfo.getValue(), sheetInfo -> viewModel.sheetInfo.setValue(sheetInfo));

        // resume location access
        resumeDisposables.add(geoDirUpdate.start(GeoDirHandler.UPDATE_GEODIR));
    }

    @Override
    protected void onStop() {
        this.resumeDisposables.clear();
        super.onStop();
    }

    @Override
    public void onPause() {
        if (mapFragment != null) {
            saveCenterAndZoom();
            lastMapStateFromOnPause = getCurrentMapState();
        }
        if (tileProvider != null) {
            tileProvider.onPause();
        }
        if (!Settings.isFeatureEnabledDefaultTrue(R.string.pref_useDelayedMapFragment)) {
            destroyMapFragment();
        }
        WherigoGame.get().removeListener(wherigoListenerId);
        super.onPause();
    }

    public void destroyMapFragment() {
        if (mapFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(mapFragment).commitNowAllowingStateLoss();
        }
        mapFragment = null;
    }

    public static void acquireUnifiedMap(final UnifiedMapActivity newUnifiedMapActivity) {
        if (!Settings.isFeatureEnabledDefaultTrue(R.string.pref_useDelayedMapFragment)) {
            return;
        }
        final UnifiedMapActivity oldUnifiedMapActivity = unifiedMapActivity.get();
        if (oldUnifiedMapActivity != null) {
            // may be optimized further to only destroy map fragment if it's a VTM instance
            oldUnifiedMapActivity.destroyMapFragment();
        }
        unifiedMapActivity = new WeakReference<>(newUnifiedMapActivity);
    }

    @Override
    protected void onResume() {
        if (mapFragment == null) {
            recreate(); // restart with a fresh MapView
        }

        if (Settings.removeFromRouteOnLog()) {
            viewModel.reloadIndividualRoute();
        }
        super.onResume();
        reloadCachesAndWaypoints(false);
        MapUtils.updateFilterBar(this, mapType.filterContext);
        if (tileProvider != null) {
            tileProvider.onResume();
        }

        wherigoListenerId = WherigoGame.get().addListener(nt -> {
            final View view = findViewById(R.id.container_wherigo);
            if (view != null) {
                view.setVisibility(WherigoGame.get().isPlaying() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void openWherigoPopup() {
        final Dialog dialog = WherigoViewUtils.getQuickViewDialog(this);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (loadInBackgroundHandler != null) {
            loadInBackgroundHandler.onDestroy();
        }
        if (tileProvider != null) {
            tileProvider.onDestroy();
        }
        super.onDestroy();
    }

}
