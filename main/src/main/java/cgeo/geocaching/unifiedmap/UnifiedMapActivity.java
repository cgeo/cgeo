package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.AbstractDialogFragment;
import cgeo.geocaching.CacheListActivity;
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
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteOrRouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.service.CacheDownloaderService;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.RepeatOnHoldListener;
import cgeo.geocaching.ui.ToggleItemType;
import cgeo.geocaching.ui.TwoLineSpinnerAdapter;
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
import cgeo.geocaching.unifiedmap.mapsforgevtm.legend.RenderThemeLegend;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.HideActionBarUtils;
import cgeo.geocaching.utils.HistoryTrackUtils;
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.Intents.ACTION_INDIVIDUALROUTE_CHANGED;
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
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_LANGUAGE_DEFAULT_ID;

import android.app.Activity;
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
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
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
    private LoadInBackgroundHandler loadInBackgroundHandler = null;

    private LocUpdater geoDirUpdate;
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private MenuItem followMyLocationItem = null;

    private RouteTrackUtils routeTrackUtils = null;
    private ElevationChart elevationChartUtils = null;
    private String lastElevationChartRoute = null;

    private UnifiedMapType mapType = null;
    private MapMode compatibilityMapMode = MapMode.LIVE;
    private boolean overridePositionAndZoom = false; // to preserve those on config changes in favour to mapType defaults

    private Spinner mapSpinner = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            mapType.filterContext = savedInstanceState.getParcelable(BUNDLE_FILTERCONTEXT);
            overridePositionAndZoom = savedInstanceState.getBoolean(BUNDLE_OVERRIDEPOSITIONANDZOOM, false);
            viewModel.followMyLocation.setValue(mapType.followMyLocation);
        } else {
            if (mapType != null) {
                viewModel.followMyLocation.setValue(mapType.followMyLocation);
            } else {
                viewModel.followMyLocation.setValue(Boolean.TRUE.equals(viewModel.followMyLocation.getValue()) && mapType.type == UMTT_PlainMap);
            }
        }
        // make sure we have a defined mapType
        if (mapType == null || mapType.type == UnifiedMapType.UnifiedMapTypeType.UMTT_Undefined) {
            mapType = new UnifiedMapType();
        }

        viewModel.transientIsLiveEnabled.setValue(mapType.type == UMTT_PlainMap && Settings.isLiveMap());

        routeTrackUtils = new RouteTrackUtils(this, null /* @todo: savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_ROUTETRACKUTILS) */, this::centerMap, viewModel::clearIndividualRoute, viewModel::reloadIndividualRoute, viewModel::setTrack, this::isTargetSet);
        viewModel.configureProximityNotification();
        if (viewModel.proximityNotification.getValue() != null) {
            viewModel.proximityNotification.getValue().setTextNotifications(this);
        }

        viewModel.trackUpdater.observe(this, event -> routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue()));
        viewModel.individualRoute.observe(this, individualRoute -> routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute));
        viewModel.followMyLocation.observe(this, this::initFollowMyLocation);
        viewModel.location.observe(this, this::handleLocUpdate);

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
        new NavigationTargetLayer(this, nonClickableItemsLayer);
        new CacheCirclesLayer(this, nonClickableItemsLayer);

        new IndividualRouteLayer(this, clickableItemsLayer);
        new GeoItemsLayer(this, clickableItemsLayer);

        viewModel.init(routeTrackUtils);

        final UnifiedMapState mapState = savedInstanceState != null ? savedInstanceState.getParcelable(BUNDLE_MAPSTATE) : null;
        changeMapSource(Settings.getTileProvider(), mapState);

        FilterUtils.initializeFilterBar(this, this);
        MapUtils.updateFilterBar(this, mapType.filterContext);

        Routing.connect(ROUTING_SERVICE_KEY, () -> viewModel.reloadIndividualRoute(), this);
        viewModel.reloadIndividualRoute();

        CompactIconModeUtils.setCompactIconModeThreshold(getResources());

        viewModel.mapCenter.observe(this, center -> updateCacheCountSubtitle());
        viewModel.caches.observe(this, caches -> updateCacheCountSubtitle());

        MapUtils.showMapOneTimeMessages(this, compatibilityMapMode);

        getLifecycle().addObserver(new GeocacheChangedBroadcastReceiver(this, true) {
            @Override
            protected void onReceive(final Context context, final String geocode) {
                final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null && cache.getCoords() != null) {
                    viewModel.caches.getValue().remove(cache);
                    viewModel.caches.getValue().add(cache);
                    viewModel.caches.notifyDataChanged();
                }
            }
        });

        getLifecycle().addObserver(new LifecycleAwareBroadcastReceiver(this, ACTION_INDIVIDUALROUTE_CHANGED) {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                viewModel.reloadIndividualRoute();
            }
        });

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

        setupActionBarSpinner();
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
//        if (oldFragment == null || !oldFragment.supportsTileSource(tileProvider)) {
        mapFragment = tileProvider.createMapFragment();

        if (oldFragment != null) {
            mapFragment.init(oldFragment.getCurrentZoom(), oldFragment.getCenter(), () -> onMapReadyTasks(newSource, true, mapState));
        } else {
            mapFragment.init(Settings.getMapZoom(compatibilityMapMode), null, () -> onMapReadyTasks(newSource, true, mapState));
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapViewFragment, mapFragment)
                .commit();
//        } else {
//            onMapReadyTasks(newSource, false);
//        }
    }

    private void onMapReadyTasks(final AbstractTileProvider newSource, final boolean mapChanged, @Nullable final UnifiedMapState mapState) {
        TileProviderFactory.resetLanguages();
        mapFragment.setTileSource(newSource);
        Settings.setTileProvider(newSource);

//        tileProvider.getMap().showSpinner(); - should be handled from UnifiedMapActivity instead
        if (mapChanged) {
            final boolean setDefaultCenterAndZoom = !overridePositionAndZoom && mapState == null;
            // map settings popup
            findViewById(R.id.map_settings_popup).setOnClickListener(v -> MapSettingsUtils.showSettingsPopup(this, viewModel.individualRoute.getValue(), this::refreshMapData, this::routingModeChanged, this::compactIconModeChanged, () -> viewModel.configureProximityNotification(), mapType.filterContext));

            // routes / tracks popup
            findViewById(R.id.map_individualroute_popup).setOnClickListener(v -> routeTrackUtils.showPopup(viewModel.individualRoute.getValue(), viewModel::setTarget));
            routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue());

            // react to mapType
            setMapModeFromMapType();
            viewModel.coordsIndicator.setValue(null);
            switch (mapType.type) {
                case UMTT_PlainMap:
                    // restore last saved position and zoom
                    if (setDefaultCenterAndZoom) {
                        mapFragment.setZoom(Settings.getMapZoom(compatibilityMapMode));
                        mapFragment.setCenter(Settings.getUMMapCenter());
                    }
                    break;
                case UMTT_TargetGeocode: // can be either a cache or a waypoint
                    // load cache/waypoint, focus map on it, and set it as target
                    final Geocache cache = DataStore.loadCache(mapType.target, LoadFlags.LOAD_WAYPOINTS);
                    if (cache != null) {
                        viewModel.waypoints.getValue().clear();
                        if (mapType.waypointId > 0) { // single waypoint mode: display waypoint only
                            final Waypoint waypoint = cache.getWaypointById(mapType.waypointId);
                            if (waypoint != null) {
                                if (setDefaultCenterAndZoom) {
                                    mapFragment.setCenter(waypoint.getCoords());
                                }
                                viewModel.waypoints.getValue().add(waypoint);
                                viewModel.setTarget(waypoint.getCoords(), waypoint.getName());
                            }
                        } else if (cache.getCoords() != null) { // geocache mode: display geocache and its waypoints
                            viewModel.caches.getValue().add(cache);
                            viewModel.caches.notifyDataChanged();
                            if (setDefaultCenterAndZoom) {
                                mapFragment.setCenter(cache.getCoords());
                            }
                            viewModel.waypoints.getValue().addAll(cache.getWaypoints());
                            viewModel.setTarget(cache.getCoords(), cache.getGeocode());
                        }
                        if (setDefaultCenterAndZoom) {
                            mapFragment.zoomToBounds(DataStore.getBounds(mapType.target, Settings.getZoomIncludingWaypoints()));
                        }
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
                    final SearchResult searchResult = DataStore.getBatchOfStoredCaches(null, mapType.fromList);
                    final Viewport viewport3 = DataStore.getBounds(searchResult.getGeocodes(), Settings.getZoomIncludingWaypoints());
                    addSearchResultByGeocaches(searchResult);
                    if (viewport3 != null) {
                        loadWaypoints(this, viewModel, viewport3);
                        if (setDefaultCenterAndZoom) {
                            mapFragment.zoomToBounds(viewport3);
                        }
                        refreshMapData(false);
                    }
                    break;
                case UMTT_SearchResult:
                    // load list of caches and scale map to see them all
                    final Viewport viewport2 = DataStore.getBounds(mapType.searchResult.getGeocodes(), Settings.getZoomIncludingWaypoints());
                    addSearchResultByGeocaches(mapType.searchResult);
                    if (viewport2 != null) {
                        loadWaypoints(this, viewModel, viewport2);
                        if (setDefaultCenterAndZoom) {
                            mapFragment.zoomToBounds(viewport2);
                        }
                    }
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
            setTitle();

            // only initialize loadInBackgroundHandler if caches should actually be loaded
            if (mapType.type == UMTT_PlainMap) {
                if (loadInBackgroundHandler != null) {
                    loadInBackgroundHandler.onDestroy();
                }
                loadInBackgroundHandler = new LoadInBackgroundHandler(this);
            }
        }
        hideProgressSpinner();

        // override some settings, if given
        if (mapState != null) {
            ActivityMixin.postDelayed(() -> {
                mapFragment.setCenter(mapState.center);
                mapFragment.setZoom(mapState.zoomLevel);
            }, 1000);
        }
        // both maps have invalid values for bounding box in the beginning, so need to delay counting a bit
        ActivityMixin.postDelayed(this::updateCacheCountSubtitle, 2000);

        // refresh options menu and routes/tracks display
        invalidateOptionsMenu();
//        onResume();
    }

    private void compactIconModeChanged(final int newValue) {
        Settings.setCompactIconMode(newValue);
        viewModel.caches.postNotifyDataChanged();
    }

    public void showProgressSpinner() {
        final View spinner = findViewById(R.id.map_progressbar);
        spinner.setVisibility(View.VISIBLE);
    }

    public void hideProgressSpinner() {
        final View spinner = findViewById(R.id.map_progressbar);
        spinner.setVisibility(View.GONE);
    }

    public static void loadWaypoints(final UnifiedMapActivity activity, final UnifiedMapViewModel viewModel, final Viewport viewport) {
        viewModel.waypoints.getValue().clear();
        if (viewport.count(viewModel.caches.getValue().getAsList()) < Settings.getWayPointsThreshold()) {
            final Set<Waypoint> waypoints;
            if (Boolean.TRUE.equals(viewModel.transientIsLiveEnabled.getValue())) {
                //All visible waypoints
                waypoints = DataStore.loadWaypoints(viewport);
            } else {
                waypoints = new HashSet<>();
                //All waypoints from the viewed caches
                for (final Geocache c : viewModel.caches.getValue().getAsList()) {
                    waypoints.addAll(c.getWaypoints());
                }
            }
            Log.d("load.waypoints: " + waypoints.size());
            MapUtils.filter(waypoints, activity.getFilterContext());
            viewModel.waypoints.getValue().addAll(waypoints);
            viewModel.waypoints.postNotifyDataChanged();
        }
    }

    private void updateCacheCountSubtitle() {
        final int cacheCount = mapFragment.getViewport().count(viewModel.caches.getValue().getAsList());
        String subtitle = res.getQuantityString(R.plurals.cache_counts, cacheCount, cacheCount);

        // for single cache map show cache details instead
        if (mapType.type == UMTT_TargetGeocode) {
            final Geocache targetCache = getCurrentTargetCache();
            if (targetCache != null) {
                subtitle = Formatter.formatMapSubtitle(targetCache);
            }
        }

        CompactIconModeUtils.forceCompactIconMode(cacheCount);
        final ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            ((TwoLineSpinnerAdapter) mapSpinner.getAdapter()).setTextByReference(mapType.fromList, false, subtitle);
        }
    }

    public void addSearchResultByGeocaches(final SearchResult searchResult) {
        Log.d("add " + searchResult.getGeocodes());
        for (String geocode : searchResult.getGeocodes()) {
            final Geocache temp = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (temp != null && temp.getCoords() != null) {
                viewModel.caches.getValue().remove(temp);
                viewModel.caches.getValue().add(temp);
                viewModel.caches.postNotifyDataChanged(); // use post to make it background capable
            }
        }
    }

    public void addSearchResultByGeocaches(final Set<Geocache> searchResult) {
        Log.d("addSearchResult: " + searchResult.size());
        viewModel.caches.getValue().removeAll(searchResult);
        for (Geocache geocache : searchResult) {
            if (geocache.getCoords() != null) {
                viewModel.caches.getValue().add(geocache);
            }
        }
        viewModel.caches.postNotifyDataChanged(); // use post to make it background capable

    }

    public void addSearchResultByGeocodes(final Set<String> searchResult) {
        final StringBuilder s = new StringBuilder();
        for (String geocode : searchResult) {
            s.append(" ").append(geocode);
            final Geocache temp = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (temp != null && temp.getCoords() != null) {
                viewModel.caches.getValue().remove(temp);
                viewModel.caches.getValue().add(temp);
                viewModel.caches.postNotifyDataChanged(); // use post to make it background capable
            }
        }
        Log.d("add [" + s + "]");
    }

    private void setTitle() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && mapSpinner != null) {
            if (mapType.fromList == 0) {
                ((TwoLineSpinnerAdapter) mapSpinner.getAdapter()).setTextByReference(mapType.fromList, true, calculateTitle());
            } else {
                final int position = ((TwoLineSpinnerAdapter) mapSpinner.getAdapter()).getPositionFromReference(mapType.fromList);
                if (position >= 0) {
                    actionBar.setSelectedNavigationItem(position);
                }
            }
        }
    }

    @NonNull
    private String calculateTitle() {
        if (Boolean.TRUE.equals(viewModel.transientIsLiveEnabled.getValue())) {
            return getString(R.string.map_live);
        }
        if (mapType.type == UMTT_TargetGeocode) {
            final Geocache cache = DataStore.loadCache(mapType.target, LoadFlags.LOAD_CACHE_OR_DB);
            if (cache != null && cache.getCoords() != null) {
                return cache.getName();
            }
        }
        return StringUtils.defaultIfEmpty(mapType.title, getString(R.string.map_offline));
    }

    private void setupActionBarSpinner() {
        // allow switching between lists
        final List<TwoLineSpinnerAdapter.TextSpinnerData> items = new ArrayList<>();
        if (mapType.fromList == 0) {
            items.add(new TwoLineSpinnerAdapter.TextSpinnerData("", "", 0));
        }
        for (AbstractList list : StoredList.UserInterface.getMenuLists(false, PseudoList.NEW_LIST.id)) {
            final int count = list.getNumberOfCaches();
            final TwoLineSpinnerAdapter.TextSpinnerData temp = new TwoLineSpinnerAdapter.TextSpinnerData(list.title, res.getQuantityString(R.plurals.cache_counts, count, count), list.id);
            if (list.id == mapType.fromList) {
                items.add(0, temp);
            } else {
                items.add(temp);
            }
        }
        final TwoLineSpinnerAdapter spinnerAdapter = new TwoLineSpinnerAdapter(this, items);
        mapSpinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        mapSpinner.setAdapter(spinnerAdapter);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setListNavigationCallbacks(spinnerAdapter, (position, l) -> {
                final int newListId = ((TwoLineSpinnerAdapter.TextSpinnerData) mapSpinner.getAdapter().getItem(position)).reference;
                if (position > 0) {
                    // load new item and switch list
                    if (newListId == 0) {
                        new UnifiedMapType().launchMap(this);
                        finish();
                    } else {
                        final Optional<AbstractList> lNew = StoredList.UserInterface.getMenuLists(false, PseudoList.NEW_LIST.id).stream().filter(l2 -> l2.id == newListId).findFirst();
                        if (lNew.isPresent()) {
                            new UnifiedMapType(newListId).launchMap(this);
                            finish();
                        }
                    }
                }
                return true;
            });
        }
    }

    /**
     * centers map on coords given + resets "followMyLocation" state
     **/
    private void centerMap(final Geopoint geopoint) {
        viewModel.followMyLocation.setValue(false);
        mapFragment.setCenter(geopoint);
    }

    private void initFollowMyLocation(final boolean followMyLocation) {
        Settings.setFollowMyLocation(followMyLocation);
        ToggleItemType.FOLLOW_MY_LOCATION.toggleMenuItem(followMyLocationItem, followMyLocation);

        if (followMyLocation) {
            final Location currentLocation = LocationDataProvider.getInstance().currentGeo(); // get location even if non was delivered to the view-model yet
            mapFragment.setCenter(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
        }
    }

    private void handleLocUpdate(final LocUpdater.LocationWrapper locationWrapper) {
        final int mapRotation = Settings.getMapRotation();
        if (locationWrapper.needsRepaintForHeading && (mapRotation == MAPROTATION_AUTO_LOWPOWER || mapRotation == MAPROTATION_AUTO_PRECISE)) {
            mapFragment.setBearing(locationWrapper.heading);
        }

        if (locationWrapper.needsRepaintForDistanceOrAccuracy && Boolean.TRUE.equals(viewModel.followMyLocation.getValue())) {
            mapFragment.setCenter(new Geopoint(locationWrapper.location));

            if (viewModel.proximityNotification.getValue() != null) {
                viewModel.proximityNotification.getValue().checkDistance(getClosestDistanceInM(new Geopoint(locationWrapper.location.getLatitude(), locationWrapper.location.getLongitude()), viewModel));
            }

            if (Settings.showElevation()) {
                viewModel.elevation.setValue(locationWrapper.location.hasAltitude() ? (float) locationWrapper.location.getAltitude() : Routing.NO_ELEVATION_AVAILABLE);
            }
        }
    }

    // ========================================================================
    // Routes, tracks and targets handling

    private void routingModeChanged(final RoutingMode newValue) {
        Settings.setRoutingMode(newValue);
        if ((null != viewModel.individualRoute && viewModel.individualRoute.getValue().getNumSegments() > 0) || null != viewModel.getTracks()) {
            Toast.makeText(this, R.string.brouter_recalculating, Toast.LENGTH_SHORT).show();
        }
        viewModel.reloadIndividualRoute();
        viewModel.reloadTracks(routeTrackUtils);
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

    private boolean isTargetSet() {
        return viewModel.target.getValue() != null;
    }

    // ========================================================================
    // Bottom navigation methods

    @Override
    public int getSelectedBottomItemId() {
        return mapType == null || mapType.type == UMTT_PlainMap || mapType.type == UMTT_List ? MENU_MAP : MENU_HIDE_NAVIGATIONBAR;
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

        // theming options
        menu.findItem(R.id.menu_theme_mode).setVisible(tileProvider.supportsThemes());
        menu.findItem(R.id.menu_theme_options).setVisible(tileProvider.supportsThemeOptions());
        menu.findItem(R.id.menu_theme_legend).setVisible(tileProvider.supportsThemes() && RenderThemeLegend.supportsLegend());

        menu.findItem(R.id.menu_as_list).setVisible(true);
        MapUtils.onPrepareOptionsMenu(menu);

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.map_activity, menu);
        followMyLocationItem = menu.findItem(R.id.menu_toggle_mypos);
        FilterUtils.initializeFilterMenu(this, this);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_map_live) {
            if (mapType.type == UMTT_PlainMap) {
                Settings.setLiveMap(!Settings.isLiveMap());
                viewModel.transientIsLiveEnabled.setValue(Settings.isLiveMap());
                ActivityMixin.invalidateOptionsMenu(this);
                setTitle();
                setMapModeFromMapType();
            } else {
                mapType = new UnifiedMapType();
                viewModel.transientIsLiveEnabled.setValue(true);
                setupActionBarSpinner();
                MapUtils.updateFilterBar(this, mapType.filterContext);
                updateSelectedBottomNavItemId();
                setMapModeFromMapType(); // to get zoomLevel stored for right mode
                saveCenterAndZoom();
                final Geopoint coordsIndicator = viewModel.coordsIndicator.getValue();
                onMapReadyTasks(tileProvider, true, getCurrentMapState());
                viewModel.coordsIndicator.setValue(coordsIndicator);
            }
        } else if (id == R.id.menu_toggle_mypos) {
            viewModel.followMyLocation.setValue(Boolean.FALSE.equals(viewModel.followMyLocation.getValue()));
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
        } else if (HistoryTrackUtils.onOptionsItemSelected(this, id, () -> viewModel.positionHistory.setValue(viewModel.positionHistory.getValue()), () -> {
            PositionHistory positionHistory = viewModel.positionHistory.getValue();
            if (positionHistory == null) {
                positionHistory = new PositionHistory();
            }
            positionHistory.reset();
        })
                || DownloaderUtils.onOptionsItemSelected(this, id, true)) {
            return true;
        } else if (id == R.id.menu_filter) {
            showFilterMenu();
        } else if (id == R.id.menu_store_caches) {
            final Set<String> geocodes = mapFragment.getViewport()
                    .filter(viewModel.caches.getValue().getAsList())
                    .stream()
                    .map(Geocache::getGeocode)
                    .collect(Collectors.toSet());
            CacheDownloaderService.downloadCaches(this, geocodes, false, false, () -> viewModel.caches.notifyDataChanged());
        } else if (id == R.id.menu_theme_mode) {
            mapFragment.selectTheme(this);
        } else if (id == R.id.menu_theme_options) {
            mapFragment.selectThemeOptions(this);
        } else if (id == R.id.menu_routetrack) {
            routeTrackUtils.showPopup(viewModel.individualRoute.getValue(), viewModel::setTarget);
        } else if (id == R.id.menu_select_mapview) {
            // dynamically create submenu to reflect possible changes in map sources
            View v = findViewById(R.id.menu_select_mapview);
            if (v == null) {
                // if map selection is moved to overflow menu, use toggle menu item instead as anchor for popup
                v = findViewById(R.id.menu_toggle_mypos);
            }
            if (v != null) {
                final PopupMenu menu = new PopupMenu(this, v, Gravity.TOP);
                menu.inflate(R.menu.map_downloader);
                TileProviderFactory.addMapviewMenuItems(this, menu);
                menu.setOnMenuItemClickListener(this::onOptionsItemSelected);
                menu.show();
            }
        } else if (id == R.id.menu_as_list) {
            final Collection<Geocache> caches = mapFragment.getViewport().filter(viewModel.caches.getValue().getAsList());
            CacheListActivity.startActivityMap(this, new SearchResult(caches));
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

    private void saveCenterAndZoom() {
        Settings.setMapZoom(compatibilityMapMode, mapFragment.getCurrentZoom());
        Settings.setMapCenter(mapFragment.getCenter());
    }

    private void setMapRotation(final MenuItem item, final int mapRotation) {
        Settings.setMapRotation(mapRotation);
        mapFragment.setMapRotation(mapRotation);
        item.setChecked(true);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.routeTrackUtils.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            mapType.filterContext = data.getParcelableExtra(EXTRA_FILTER_CONTEXT);
            refreshMapData(false);
        }
    }

    @Override
    public void onReceiveTargetUpdate(final AbstractDialogFragment.TargetInfo targetInfo) {
        if (Settings.isAutotargetIndividualRoute()) {
            Settings.setAutotargetIndividualRoute(false);
            Toast.makeText(this, R.string.map_disable_autotarget_individual_route, Toast.LENGTH_SHORT).show();
        }
        viewModel.setTarget(targetInfo.coords, targetInfo.geocode);
    }

    // ========================================================================
    // Filter related methods

    @Override
    public void showFilterMenu() {
        FilterUtils.openFilterActivity(this, mapType.filterContext, viewModel.caches.getValue());
    }

    @Override
    public boolean showSavedFilterList() {
        return FilterUtils.openFilterList(this, mapType.filterContext);
    }

    @Override
    public void refreshWithFilter(final GeocacheFilter filter) {
        mapType.filterContext.set(filter);
        MapUtils.updateFilterBar(this, mapType.filterContext);
        refreshMapData(false);
    }

    protected GeocacheFilterContext getFilterContext() {
        return mapType.filterContext;
    }

    private void refreshMapData(final boolean circlesSwitched) {
        MapUtils.filter(viewModel.caches.getValue(), mapType.filterContext);
        viewModel.caches.notifyDataChanged();
        viewModel.waypoints.notifyDataChanged();
        if (loadInBackgroundHandler != null) {
            loadInBackgroundHandler.onDestroy();
            loadInBackgroundHandler = new LoadInBackgroundHandler(this);
        }
    }

    // ========================================================================
    // Map tap handling

    public void onTap(final int latitudeE6, final int longitudeE6, final int x, final int y, final boolean isLongTap) {

        // lookup elements touched by this
        final LinkedList<RouteOrRouteItem> result = new LinkedList<>();

        for (String key : clickableItemsLayer.getTouched(Geopoint.forE6(latitudeE6, longitudeE6))) {

            if (key.startsWith(UnifiedMapViewModel.CACHE_KEY_PREFIX)) {
                final String geocode = key.substring(UnifiedMapViewModel.CACHE_KEY_PREFIX.length());

                final Geocache temp = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (temp != null && temp.getCoords() != null) {
                    result.add(new RouteOrRouteItem(new RouteItem(temp)));
                }
            }

            if (key.startsWith(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX) && isLongTap) { // only consider when tap was a longTap
                final String identifier = key.substring(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX.length());

                for (RouteItem item : viewModel.individualRoute.getValue().getRouteItems()) {
                    if (identifier.equals(item.getIdentifier())) {
                        result.add(new RouteOrRouteItem(item));
                        break;
                    }
                }
            }

            if (key.startsWith(UnifiedMapViewModel.WAYPOINT_KEY_PREFIX)) {
                final String fullGpxId = key.substring(UnifiedMapViewModel.WAYPOINT_KEY_PREFIX.length());

                for (Waypoint waypoint : viewModel.waypoints.getValue()) {
                    if (fullGpxId.equals(waypoint.getFullGpxId())) {
                        result.add(new RouteOrRouteItem(new RouteItem(waypoint)));
                        break;
                    }
                }
            }

            if (key.startsWith(IndividualRouteLayer.KEY_INDIVIDUAL_ROUTE)) {
                result.add(new RouteOrRouteItem(viewModel.individualRoute.getValue()));
            }
            if (key.startsWith(TracksLayer.TRACK_KEY_PREFIX) && viewModel.getTracks().getTrack(key.substring(TracksLayer.TRACK_KEY_PREFIX.length())).getRoute() instanceof Route) {
                result.add(new RouteOrRouteItem((Route) viewModel.getTracks().getTrack(key.substring(TracksLayer.TRACK_KEY_PREFIX.length())).getRoute()));
            }

        }
        Log.d("touched elements (" + result.size() + "): " + result);

        if (result.size() == 0) {
            if (isLongTap) {
                final Geopoint gp = Geopoint.forE6(latitudeE6, longitudeE6);
                viewModel.longTapCoords.setValue(gp);
                MapUtils.createMapLongClickPopupMenu(this, gp, new Point(x, y), viewModel.individualRoute.getValue(), route -> viewModel.individualRoute.notifyDataChanged(), null, getCurrentTargetCache(), new MapOptions(), viewModel::setTarget)
                        .setOnDismissListener(d -> viewModel.longTapCoords.setValue(null))
                        .show();
            } else {
                if (sheetRemoveFragment()) {
                    return;
                }
                mapFragment.adaptLayoutForActionbar(HideActionBarUtils.toggleActionBar(this));
                GeoItemTestLayer.handleTapTest(clickableItemsLayer, this, Geopoint.forE6(latitudeE6, longitudeE6), isLongTap);
            }
        } else if (result.size() == 1) {
            handleTap(result.get(0), isLongTap, x, y);
        } else {
            try {
                final ArrayList<RouteOrRouteItem> sorted = new ArrayList<>(result);
                Collections.sort(sorted, RouteOrRouteItem.NAME_COMPARATOR);

                final SimpleDialog.ItemSelectModel<RouteOrRouteItem> model = new SimpleDialog.ItemSelectModel<>();
                model
                    .setItems(sorted)
                    .setDisplayViewMapper((item, ctx, view, parent) ->
                        GeoItemSelectorUtils.createRouteOrRouteItemView(UnifiedMapActivity.this, item, GeoItemSelectorUtils.getOrCreateView(UnifiedMapActivity.this, view, parent)),
                        (item) -> item == null ? "" : item.getName())
                    .setItemPadding(0)
                    .setPlainItemPaddingLeftInDp(0);

                SimpleDialog.of(this).setTitle(R.string.map_select_multiple_items).selectSingle(model, item -> {
                    handleTap(item, isLongTap, x, y);
                });

            } catch (final Resources.NotFoundException e) {
                Log.e("UnifiedMapActivity.showSelection", e);
            }
        }

    }

    private void handleTap(final RouteOrRouteItem item, final boolean isLongTap, final int tapX, final int tapY) {
        final RouteItem routeItem = item.isRouteItem() ? item.getRouteItem() : null;
        if (isLongTap) {
            // toggle route item
            if (routeItem != null && Settings.isLongTapOnMapActivated()) {

                final Geocache cache = routeItem.getGeocache();
                final boolean canHaveStar = MapStarUtils.canHaveStar(cache);

                if (Settings.isShowRouteMenu() || canHaveStar) {
                    final SimplePopupMenu menu = MapUtils.createCacheWaypointLongClickPopupMenu(this, routeItem, tapX, tapY, viewModel.individualRoute.getValue(), viewModel, null);
                    if (canHaveStar) {
                        final String geocode = routeItem.getGeocode();
                        final boolean isStarDrawn = viewModel.cachesWithStarDrawn.getValue().contains(geocode);
                        MapStarUtils.addMenuIfNecessary(menu, cache, isStarDrawn, drawStar -> {
                            CommonUtils.addRemove(viewModel.cachesWithStarDrawn.getValue(), geocode, !drawStar);
                            viewModel.cachesWithStarDrawn.notifyDataChanged();
                        });
                    }
                    menu
                        //.setOnDismissListener(menu -> tapHandlerLayer.resetLongTapLatLong())
                        .show();
                } else {
                    viewModel.toggleRouteItem(this, routeItem);
                }
            } else if (item.isRoute() && routeTrackUtils != null && item.getRoute() != null) {
                // individual route or track
                routeTrackUtils.showRouteTrackContextMenu(tapX, tapY, RouteTrackUtils.isIndividualRoute(item.getRoute()) ? viewModel.individualRoute.getValue() : item.getRoute());
            }
        } else if (routeItem != null) {
            // open popup for element
            if (routeItem.getType() == RouteItem.RouteItemType.GEOCACHE) {
                viewModel.sheetInfo.setValue(new UnifiedMapViewModel.SheetInfo(routeItem.getGeocode(), 0));
                sheetShowDetails(viewModel.sheetInfo.getValue());
            } else if (routeItem.getType() == RouteItem.RouteItemType.WAYPOINT && routeItem.getWaypointId() != 0) {
                viewModel.sheetInfo.setValue(new UnifiedMapViewModel.SheetInfo(routeItem.getGeocode(), routeItem.getWaypointId()));
                sheetShowDetails(viewModel.sheetInfo.getValue());
            }
        } else if (item.getRoute() != null) {
            // elevation charts for individual route and/or routes/tracks
            if (elevationChartUtils == null) {
                elevationChartUtils = new ElevationChart(this, nonClickableItemsLayer);
            }
            if (lastElevationChartRoute != null && StringUtils.equals(item.getRoute().getName(), lastElevationChartRoute)) {
                elevationChartUtils.removeElevationChart();
            } else {
                elevationChartUtils.showElevationChart(item.getRoute(), routeTrackUtils);
                lastElevationChartRoute = item.getRoute().getName();
                if (RouteTrackUtils.isIndividualRoute(item.getRoute())) {
                    viewModel.individualRoute.observe(this, individualRoute -> {
                        if (lastElevationChartRoute.isEmpty()) { // still individual route being shown?
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
    }

    @Override
    protected void clearSheetInfo() {
        viewModel.sheetInfo.setValue(null);
    }

    // ========================================================================
    // distance checks for proximity notifications
    private static WaypointDistanceInfo getClosestDistanceInM(final Geopoint coord, final UnifiedMapViewModel viewModel) {
        WaypointDistanceInfo result;
        // work on a copy to avoid race conditions
        result = getClosestDistanceInM(coord, new ArrayList<>(viewModel.caches.getValue()), Integer.MAX_VALUE, item -> ((Geocache) item).getShortGeocode() + " " + ((Geocache) item).getName());
        result = getClosestDistanceInM(coord, new ArrayList<>(viewModel.waypoints.getValue()), result.meters, item -> ((Waypoint) item).getName() + " (" + ((Waypoint) item).getWaypointType().gpx + ")");
        return result;
    }

    private static WaypointDistanceInfo getClosestDistanceInM(final Geopoint center, final ArrayList<IWaypoint> items, final int minDistanceOld, final Func1<IWaypoint, String> getName) {
        int minDistance = minDistanceOld;
        String name = "";
        for (IWaypoint item : items) {
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

        outState.putParcelable(BUNDLE_MAPSTATE, getCurrentMapState());
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
        saveCenterAndZoom();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (Settings.removeFromRouteOnLog()) {
            viewModel.reloadIndividualRoute();
        }
        super.onResume();
        MapUtils.updateFilterBar(this, mapType.filterContext);
    }

    @Override
    protected void onDestroy() {
        if (loadInBackgroundHandler != null) {
            loadInBackgroundHandler.onDestroy();
        }
        super.onDestroy();
    }

}
