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
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.location.WaypointDistanceInfo;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.maps.MapOptions;
import cgeo.geocaching.maps.MapSettingsUtils;
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
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.service.CacheDownloaderService;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.ToggleItemType;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
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
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.HideActionBarUtils;
import cgeo.geocaching.utils.HistoryTrackUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.filters.gui.GeocacheFilterActivity.EXTRA_FILTER_CONTEXT;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;
import static cgeo.geocaching.settings.Settings.MAPROTATION_OFF;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.BUNDLE_MAPTYPE;
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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

    private final UpdateLoc geoDirUpdate = new UpdateLoc(this);
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private MenuItem followMyLocationItem = null;

    private RouteTrackUtils routeTrackUtils = null;
    private ElevationChart elevationChartUtils = null;
    private String lastElevationChartRoute = null;

    private UnifiedMapPosition currentMapPosition = new UnifiedMapPosition();
    private UnifiedMapType mapType = null;
    private MapMode compatibilityMapMode = MapMode.LIVE;
    private boolean overridePositionAndZoom = false; // to preserve those on config changes in favour to mapType defaults

    // rotation indicator


    // class: update location
    private static class UpdateLoc extends GeoDirHandler {
        // use the following constants for fine tuning - find good compromise between smooth updates and as less updates as possible

        // minimum time in milliseconds between position overlay updates
        private static final long MIN_UPDATE_INTERVAL = 500;
        private long timeLastPositionOverlayCalculation = 0;
        // last check for proximity notifications
        private long timeLastDistanceCheck = 0;
        // minimum change of heading in grad for position overlay update
        private static final float MIN_HEADING_DELTA = 15f;
        // minimum change of location in fraction of map width/height (whatever is smaller) for position overlay update
        private static final float MIN_LOCATION_DELTA = 0.01f;

        @NonNull
        Location currentLocation = LocationDataProvider.getInstance().currentGeo();
        float currentHeading;

        /**
         * weak reference to the outer class
         */
        @NonNull
        private final WeakReference<UnifiedMapActivity> mapActivityRef;

        UpdateLoc(@NonNull final UnifiedMapActivity mapActivity) {
            mapActivityRef = new WeakReference<>(mapActivity);
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
                    final UnifiedMapActivity mapActivity = mapActivityRef.get();
                    if (mapActivity != null) {
                        final boolean needsRepaintForDistanceOrAccuracy = needsRepaintForDistanceOrAccuracy();
                        final boolean needsRepaintForHeading = needsRepaintForHeading();

                        if (needsRepaintForDistanceOrAccuracy && Boolean.TRUE.equals(mapActivity.viewModel.followMyLocation.getValue())) {
                            mapActivity.mapFragment.setCenter(new Geopoint(currentLocation));
                            mapActivity.currentMapPosition.resetFollowMyLocation = false;
                        }

                        if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {
                            mapActivity.viewModel.setCurrentPositionAndHeading(currentLocation, currentHeading);

                            if (mapActivity.viewModel.proximityNotification.getValue() != null && (timeLastDistanceCheck == 0 || currentTimeMillis > (timeLastDistanceCheck + MIN_UPDATE_INTERVAL))) {
                                mapActivity.viewModel.proximityNotification.getValue().checkDistance(getClosestDistanceInM(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()), mapActivity.viewModel));
                                timeLastDistanceCheck = System.currentTimeMillis();
                            }

                            if (Settings.showElevation()) {
                                float elevation = Routing.getElevation(new Geopoint(currentLocation));
                                if (Float.isNaN(elevation) && currentLocation.hasAltitude()) {
                                    elevation = (float) currentLocation.getAltitude();
                                }
                                mapActivity.viewModel.elevation.setValue(elevation);
                            }
                        }
                    }
                } catch (final RuntimeException e) {
                    Log.w("Failed to update location", e);
                }
            }
        }

        boolean needsRepaintForHeading() {
            final UnifiedMapActivity mapActivity = mapActivityRef.get();
            if (mapActivity == null) {
                return false;
            }
            final Pair<Location, Float> positionAndHeading = mapActivity.viewModel.positionAndHeading.getValue();
            if (positionAndHeading == null) {
                return true;
            }
            return Math.abs(AngleUtils.difference(currentHeading, positionAndHeading.second)) > MIN_HEADING_DELTA;
        }

        boolean needsRepaintForDistanceOrAccuracy() {
            final UnifiedMapActivity map = mapActivityRef.get();
            if (map == null) {
                return false;
            }
            final Location lastLocation = map.getLocation();
            if (lastLocation.getAccuracy() != currentLocation.getAccuracy()) {
                return true;
            }
            // @todo: NewMap uses a more sophisticated calculation taking map dimensions into account - check if this is still needed
            return currentLocation.distanceTo(lastLocation) > MIN_LOCATION_DELTA;
        }
    }

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
//            followMyLocation = mapOptions.mapState.followsMyLocation();
        } else {
//            if (mapOptions.mapState != null) {
//                followMyLocation = mapOptions.mapState.followsMyLocation();
//            } else {
//                followMyLocation = followMyLocation && mapOptions.mapMode == MapMode.LIVE;
//            }
        }

        routeTrackUtils = new RouteTrackUtils(this, null /* @todo: savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_ROUTETRACKUTILS) */, this::centerMap, viewModel::clearIndividualRoute, viewModel::reloadIndividualRoute, viewModel::setTrack, this::isTargetSet);
        viewModel.configureProximityNotification();
        if (viewModel.proximityNotification.getValue() != null) {
            viewModel.proximityNotification.getValue().setTextNotifications(this);
        }

        viewModel.trackUpdater.observe(this, event -> routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), viewModel.individualRoute.getValue()));
        viewModel.individualRoute.observe(this, individualRoute -> routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute));
        viewModel.followMyLocation.observe(this, this::initFollowMyLocation);

        // make sure we have a defined mapType
        if (mapType == null || mapType.type == UnifiedMapType.UnifiedMapTypeType.UMTT_Undefined) {
            mapType = new UnifiedMapType();
        }

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

        new IndividualRouteLayer(this, clickableItemsLayer);
        new GeoItemsLayer(this, clickableItemsLayer);

        final GeoItemLayer<String> circlesLayer = new GeoItemLayer<>("circles");
        layers.add(circlesLayer);
        new CacheCirclesLayer(this, circlesLayer);

        viewModel.init(routeTrackUtils);

        changeMapSource(Settings.getTileProvider());

        FilterUtils.initializeFilterBar(this, this);
        MapUtils.updateFilterBar(this, mapType.filterContext);

        Routing.connect(ROUTING_SERVICE_KEY, () -> viewModel.reloadIndividualRoute(), this);
        viewModel.reloadIndividualRoute();

        CompactIconModeUtils.setCompactIconModeThreshold(getResources());

        viewModel.mapCenter.observe(this, center -> updateCacheCount());
        viewModel.caches.observe(this, caches -> updateCacheCount());

        MapUtils.showMapOneTimeMessages(this, compatibilityMapMode);

        getLifecycle().addObserver(new GeocacheChangedBroadcastReceiver(this) {
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
        } else if (mapType.type == UMTT_SearchResult) {
            compatibilityMapMode = MapMode.LIST;
        } else {
            compatibilityMapMode = MapMode.LIVE;
        }
    }

    private void changeMapSource(final AbstractTileProvider newSource) {
        final AbstractTileProvider oldProvider = tileProvider;
        final AbstractMapFragment oldFragment = mapFragment;

        if (oldProvider != null && oldFragment != null) {
            oldFragment.prepareForTileSourceChange();
        }
        tileProvider = newSource;
//        if (oldFragment == null || !oldFragment.supportsTileSource(tileProvider)) {
        mapFragment = tileProvider.createMapFragment();

        if (oldFragment != null) {
            mapFragment.init(oldFragment.getCurrentZoom(), oldFragment.getCenter(), () -> onMapReadyTasks(newSource, true));
        } else {
            mapFragment.init(Settings.getMapZoom(compatibilityMapMode), null, () -> onMapReadyTasks(newSource, true));
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapViewFragment, mapFragment)
                .commit();
//        } else {
//            onMapReadyTasks(newSource, false);
//        }
    }

    private void onMapReadyTasks(final AbstractTileProvider newSource, final boolean mapChanged) {
        TileProviderFactory.resetLanguages();
        mapFragment.setTileSource(newSource);
        Settings.setTileProvider(newSource);

//        tileProvider.getMap().showSpinner(); - should be handled from UnifiedMapActivity instead
        if (mapChanged) {

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
                    mapFragment.setZoom(Settings.getMapZoom(compatibilityMapMode));
                    mapFragment.setCenter(Settings.getUMMapCenter());
                    break;
                case UMTT_TargetGeocode:
                    // load cache, focus map on it, and set it as target
                    final Geocache cache = DataStore.loadCache(mapType.target, LoadFlags.LOAD_CACHE_OR_DB);
                    if (cache != null && cache.getCoords() != null) {
                        viewModel.caches.getValue().add(cache);
                        viewModel.caches.notifyDataChanged();
                        mapFragment.zoomToBounds(DataStore.getBounds(mapType.target, Settings.getZoomIncludingWaypoints()));
                        viewModel.setTarget(cache.getCoords(), cache.getName());
                    }
                    break;
                case UMTT_TargetCoords:
                    // set given coords as map center
                    mapFragment.setCenter(mapType.coords);
                    viewModel.coordsIndicator.setValue(mapType.coords);
                    break;
                case UMTT_SearchResult:
                    // load list of caches and scale map to see them all
                    final Viewport viewport2 = DataStore.getBounds(mapType.searchResult.getGeocodes());
                    addSearchResultByGeocaches(mapType.searchResult);
                    // tileProvider.getMap().zoomToBounds(Viewport.containing(tempCaches));
                    mapFragment.zoomToBounds(viewport2);
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

            if (loadInBackgroundHandler != null) {
                loadInBackgroundHandler.onDestroy();
            }
            loadInBackgroundHandler = new LoadInBackgroundHandler(this);
        }
        hideProgressSpinner();

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

    private void updateCacheCount() {
        final int cacheCount = mapFragment.getViewport().count(viewModel.caches.getValue().getAsList());
        CompactIconModeUtils.forceCompactIconMode(cacheCount);
        final ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setSubtitle(res.getQuantityString(R.plurals.cache_counts, cacheCount, cacheCount));
        }
    }

    public void addSearchResultByGeocaches(final SearchResult searchResult) {
        Log.e("add " + searchResult.getGeocodes());
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
        Log.e("addSearchResult: " + searchResult.size());
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
        Log.e("add [" + s + "]");
    }

    private void setTitle() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(MapUtils.getColoredValue(calculateTitle()));
        }
    }

    @NonNull
    private String calculateTitle() {
        if (Settings.isLiveMap() && mapType.type == UMTT_PlainMap) {
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
            final Location currentLocation = geoDirUpdate.getCurrentLocation();
            mapFragment.setCenter(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
            currentMapPosition.resetFollowMyLocation = false;
        }
    }

    private Location getLocation() {
        final Geopoint center = mapFragment.getCenter();
        final Location loc = new Location("UnifiedMap");
        loc.setLatitude(center.getLatitude());
        loc.setLongitude(center.getLongitude());
        return loc;
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
        return MENU_MAP;
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
        final MenuItem itemMapLive = menu.findItem(R.id.menu_map_live); // @todo: take it from mapMode
        ToggleItemType.LIVE_MODE.toggleMenuItem(itemMapLive, Settings.isLiveMap());

        /* @todo        itemMapLive.setVisible(mapOptions.coords == null || mapOptions.mapMode == MapMode.LIVE); */
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
            case MAPROTATION_AUTO:
                menu.findItem(R.id.menu_map_rotation_auto).setChecked(true);
                break;
            default:
                break;
        }

        // theming options
        menu.findItem(R.id.menu_theme_mode).setVisible(tileProvider.supportsThemes());
        menu.findItem(R.id.menu_theme_options).setVisible(tileProvider.supportsThemeOptions());
        menu.findItem(R.id.menu_theme_legend).setVisible(tileProvider.supportsThemes() && RenderThemeLegend.supportsLegend());

        menu.findItem(R.id.menu_as_list).setVisible(true);

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
        /* yet missing:
        - live mode (partially)
        - all cache related menu entries
        - all target related menu entries
        - filter related menu entries
         */
        if (id == R.id.menu_map_live) {
            // partial implementation for PlainMap mode
            if (mapType.type == UMTT_PlainMap) {
                Settings.setLiveMap(!Settings.isLiveMap());
                ActivityMixin.invalidateOptionsMenu(this);
                setTitle();
                setMapModeFromMapType();
            }

            /*
            mapOptions.isLiveEnabled = !mapOptions.isLiveEnabled;
            if (mapOptions.isLiveEnabled) {
                mapOptions.isStoredEnabled = true;
                mapType.filterContext = new GeocacheFilterContext(LIVE);
                caches.setFilterContext(mapType.filterContext);
                refreshMapData(false);
            }

            if (mapType.mapMode == MapMode.LIVE) {
                Settings.setLiveMap(mapOptions.isLiveEnabled);
            }
            caches.handleStoredLayers(this, mapOptions);
            caches.handleLiveLayers(this, mapOptions);
            ActivityMixin.invalidateOptionsMenu(this);
            if (mapOptions.mapMode == MapMode.SINGLE) {
                setTarget(mapOptions.coords, mapOptions.geocode);
            }
            mapOptions.mapMode = MapMode.LIVE;
            updateSelectedBottomNavItemId();
            mapOptions.title = StringUtils.EMPTY;
            setTitle();
            */
        } else if (id == R.id.menu_toggle_mypos) {
            viewModel.followMyLocation.setValue(Boolean.FALSE.equals(viewModel.followMyLocation.getValue()));
        } else if (id == R.id.menu_map_rotation_off) {
            setMapRotation(item, MAPROTATION_OFF);
        } else if (id == R.id.menu_map_rotation_manual) {
            setMapRotation(item, MAPROTATION_MANUAL);
        } else if (id == R.id.menu_map_rotation_auto) {
            setMapRotation(item, MAPROTATION_AUTO);
        } else if (id == R.id.menu_check_routingdata) {
            final BoundingBox bb = mapFragment.getBoundingBox();
            MapUtils.checkRoutingData(this, bb.getMinLatitude(), bb.getMinLongitude(), bb.getMaxLatitude(), bb.getMaxLongitude());
        } else if (HistoryTrackUtils.onOptionsItemSelected(this, id, () -> viewModel.positionHistory.setValue(Settings.isMapTrail() ? new PositionHistory() : null), () -> {
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
            final View v = findViewById(R.id.menu_select_mapview);
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
                changeMapSource(tileProviderLocal);
                return true;
            }
            if (mapFragment.onOptionsItemSelected(item)) {
                return true;
            }
            // @todo: remove this if-block after having completed implementation of UnifiedMap
            if (item.getItemId() != android.R.id.home) {
                ActivityMixin.showShortToast(this, "menu item '" + item.getTitle() + "' not yet implemented for UnifiedMap");
            }
            return super.onOptionsItemSelected(item);
        }
        return true;
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
        if (loadInBackgroundHandler != null) {
            loadInBackgroundHandler.onDestroy();
        }
        loadInBackgroundHandler = new LoadInBackgroundHandler(this);
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
            if (key.startsWith(TracksLayer.TRACK_KEY_PREFIX)) {
                result.add(new RouteOrRouteItem((Route) viewModel.getTracks().getTrack(key.substring(TracksLayer.TRACK_KEY_PREFIX.length())).getRoute()));
            }

        }
        Log.e("touched elements (" + result.size() + "): " + result);

        if (result.size() == 0) {
            if (isLongTap) {
                final Geopoint gp = Geopoint.forE6(latitudeE6, longitudeE6);
                viewModel.longTapCoords.setValue(gp);
                MapUtils.createMapLongClickPopupMenu(this, gp, new Point(x, y), viewModel.individualRoute.getValue(), route -> viewModel.individualRoute.notifyDataChanged(), null, getCurrentTargetCache(), new MapOptions(), viewModel::setTarget)
                        .setOnDismissListener(d -> viewModel.longTapCoords.setValue(null))
                        .show();
            } else {
                if (MapUtils.removeDetailsFragment(this)) {
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

                final ArrayAdapter<RouteOrRouteItem> adapter = new ArrayAdapter<RouteOrRouteItem>(this, R.layout.cacheslist_item_select, sorted) {
                    @NonNull
                    @Override
                    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                        return GeoItemSelectorUtils.createRouteOrRouteItemView(UnifiedMapActivity.this, getItem(position),
                                GeoItemSelectorUtils.getOrCreateView(UnifiedMapActivity.this, convertView, parent));
                    }
                };

                final AlertDialog dialog = Dialogs.newBuilder(this)
                        .setTitle(res.getString(R.string.map_select_multiple_items))
                        .setAdapter(adapter, (dialog1, which) -> {
                            if (which >= 0 && which < sorted.size()) {
                                handleTap(sorted.get(which), isLongTap, x, y);
                            }
                        })
                        .create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
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
                if (Settings.isShowRouteMenu()) {
                    MapUtils.createCacheWaypointLongClickPopupMenu(this, routeItem, tapX, tapY, viewModel.individualRoute.getValue(), viewModel, null)
//                            .setOnDismissListener(menu -> tapHandlerLayer.resetLongTapLatLong())
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
                // @todo: do we need a DataStore.loadCache() before?
                // CachePopup.startActivityAllowTarget(this, routeItem.getGeocode());
                MapUtils.showCacheDetails(this, routeItem.getGeocode());
            } else if (routeItem.getType() == RouteItem.RouteItemType.WAYPOINT && routeItem.getWaypointId() != 0) {
                // @todo: do we need a DataStore.loadWaypoint() before?
                // WaypointPopup.startActivityAllowTarget(this, routeItem.getWaypointId(), routeItem.getGeocode());
                MapUtils.showWaypointDetails(this, routeItem.getGeocode(), routeItem.getWaypointId());
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
                        final Route route = (Route) viewModel.getTracks().getRoute(event.peek());
                        if (route != null && StringUtils.equals(lastElevationChartRoute, route.getName())) {
                            elevationChartUtils.showElevationChart(route, routeTrackUtils);
                        }
                    });
                }
            }
        }
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

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_ROUTETRACKUTILS, routeTrackUtils.getState());

//        final MapState state = prepareMapState();
        outState.putParcelable(BUNDLE_MAPTYPE, mapType);
        if (mapType.filterContext != null) {
            outState.putParcelable(BUNDLE_FILTERCONTEXT, mapType.filterContext);
        }
        outState.putBoolean(BUNDLE_OVERRIDEPOSITIONANDZOOM, true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // close outdated details popups
        MapUtils.removeDetailsFragment(this);

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
        Settings.setMapZoom(compatibilityMapMode, mapFragment.getCurrentZoom());
        Settings.setMapCenter(mapFragment.getCenter());
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
