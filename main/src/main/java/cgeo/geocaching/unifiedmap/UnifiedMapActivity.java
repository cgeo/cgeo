package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.CachePopup;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.WaypointPopup;
import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IGeoDataProvider;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.unifiedmap.mapsforgevtm.legend.RenderThemeLegend;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.HistoryTrackUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;
import static cgeo.geocaching.settings.Settings.MAPROTATION_OFF;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.BUNDLE_MAPTYPE;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_PlainMap;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_SearchResult;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_TargetCoords;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.UnifiedMapTypeType.UMTT_TargetGeocode;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_LANGUAGE_DEFAULT_ID;
import static cgeo.geocaching.utils.DisplayUtils.SIZE_CACHE_MARKER_DP;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.apache.commons.lang3.StringUtils;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;

public class UnifiedMapActivity extends AbstractBottomNavigationActivity {

    private static final String STATE_ROUTETRACKUTILS = "routetrackutils";
    private static final String BUNDLE_ROUTE = "route";
    private static final String BUNDLE_OVERRIDEPOSITIONANDZOOM = "overridePositionAndZoom";

    private static final String ROUTING_SERVICE_KEY = "UnifiedMap";

    private AbstractTileProvider tileProvider = null;
    private AbstractGeoitemLayer geoitemLayer = null;
    private LoadInBackgroundHandler loadInBackgroundHandler = null;

    private final UpdateLoc geoDirUpdate = new UpdateLoc(this);
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private static boolean followMyLocation = Settings.getFollowMyLocation();
    private MenuItem followMyLocationItem = null;

    private RouteTrackUtils routeTrackUtils = null;
    private IndividualRoute individualRoute = null;
    private Tracks tracks = null;
    private UnifiedMapPosition currentMapPosition = new UnifiedMapPosition();
    private UnifiedMapType mapType = null;
    private MapMode compatibilityMapMode = MapMode.LIVE;
    private boolean overridePositionAndZoom = false; // to preserve those on config changes in favour to mapType defaults

    // rotation indicator
    protected Bitmap rotationIndicator = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.bearing_indicator, null));
    protected int rotationWidth = rotationIndicator.getWidth();
    protected int rotationHeight = rotationIndicator.getHeight();

    // class: update location
    private static class UpdateLoc extends GeoDirHandler {
        // use the following constants for fine tuning - find good compromise between smooth updates and as less updates as possible

        // minimum time in milliseconds between position overlay updates
        private static final long MIN_UPDATE_INTERVAL = 500;
        private long timeLastPositionOverlayCalculation = 0;
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

                        if (needsRepaintForDistanceOrAccuracy && followMyLocation) {
                            mapActivity.tileProvider.getMap().setCenter(new Geopoint(currentLocation));
                            mapActivity.currentMapPosition.resetFollowMyLocation = false;
                        }

                        if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {
                            if (mapActivity.tileProvider.getMap().positionLayer != null) {
                                mapActivity.tileProvider.getMap().positionLayer.setCurrentPositionAndHeading(currentLocation, currentHeading);
                            }
                            // @todo: check if proximity notification needs an update
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
            return Math.abs(AngleUtils.difference(currentHeading, mapActivity.tileProvider.getMap().getHeading())) > MIN_HEADING_DELTA;
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
            overridePositionAndZoom = savedInstanceState.getBoolean(BUNDLE_OVERRIDEPOSITIONANDZOOM, false);
//            proximityNotification = savedInstanceState.getParcelable(BUNDLE_PROXIMITY_NOTIFICATION);
            individualRoute = savedInstanceState.getParcelable(BUNDLE_ROUTE);
//            followMyLocation = mapOptions.mapState.followsMyLocation();
        } else {
//            if (mapOptions.mapState != null) {
//                followMyLocation = mapOptions.mapState.followsMyLocation();
//            } else {
//                followMyLocation = followMyLocation && mapOptions.mapMode == MapMode.LIVE;
//            }
            individualRoute = null;
//            proximityNotification = Settings.isGeneralProximityNotificationActive() ? new ProximityNotification(true, false) : null;
        }
        // make sure we have a defined mapType
        if (mapType == null || mapType.type == UnifiedMapType.UnifiedMapTypeType.UMTT_Undefined) {
            mapType = new UnifiedMapType();
        }
        changeMapSource(Settings.getTileProvider());

        Routing.connect(ROUTING_SERVICE_KEY, () -> resumeRoute(true), this);
        CompactIconModeUtils.setCompactIconModeThreshold(getResources());

//        MapUtils.showMapOneTimeMessages(this, mapMode);

        /*
        getLifecycle().addObserver(new GeocacheChangedBroadcastReceiver(this) {
            @Override
            protected void onReceive(final Context context, final String geocode) {
                caches.invalidate(Collections.singleton(geocode));
            }
        });
        */

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
        if (oldProvider != null) {
            oldProvider.getMap().prepareForTileSourceChange();
        }
        tileProvider = newSource;
        if (tileProvider != oldProvider) {
            if (oldProvider != null) {
                tileProvider.getMap().init(this, oldProvider.getMap().getCurrentZoom(), oldProvider.getMap().getCenter(), () -> onMapReadyTasks(newSource, true));
            } else {
                tileProvider.getMap().init(this, Settings.getMapZoom(compatibilityMapMode), null, () -> onMapReadyTasks(newSource, true));
            }
            configMapChangeListener(true);
        } else {
            onMapReadyTasks(newSource, false);
        }
    }

    private void onMapReadyTasks(final AbstractTileProvider newSource, final boolean mapChanged) {
        TileProviderFactory.resetLanguages();
        tileProvider.getMap().setTileSource(newSource);
        Settings.setTileProvider(newSource);
        tileProvider.getMap().setDelayedZoomTo();
        tileProvider.getMap().setDelayedCenterTo();

        tileProvider.getMap().showSpinner();
        if (mapChanged) {
            initRouteTrackUtilsAndTracks(true);

            // map settings popup
//        findViewById(R.id.map_settings_popup).setOnClickListener(v -> MapSettingsUtils.showSettingsPopup(this, individualRoute, this::refreshMapData, this::routingModeChanged, this::compactIconModeChanged, mapOptions.filterContext));

            // routes / tracks popup
            findViewById(R.id.map_individualroute_popup).setOnClickListener(v -> routeTrackUtils.showPopup(individualRoute, this::setTarget));

            // create geoitem layers
            geoitemLayer = tileProvider.getMap().createGeoitemLayers(tileProvider);

            // react to mapType
            setMapModeFromMapType();
            switch (mapType.type) {
                case UMTT_PlainMap:
                    // restore last saved position and zoom
                    tileProvider.getMap().setZoom(Settings.getMapZoom(compatibilityMapMode));
                    tileProvider.getMap().setCenter(Settings.getUMMapCenter());
                    break;
                case UMTT_TargetGeocode:
                    // load cache, focus map on it, and set it as target
                    final Geocache cache = DataStore.loadCache(mapType.target, LoadFlags.LOAD_CACHE_OR_DB);
                    if (cache != null && cache.getCoords() != null) {
                        geoitemLayer.add(cache);
                        tileProvider.getMap().zoomToBounds(DataStore.getBounds(mapType.target, Settings.getZoomIncludingWaypoints()));
                        setTarget(cache.getCoords(), cache.getName());
                    }
                    break;
                case UMTT_TargetCoords:
                    // set given coords as map center
                    tileProvider.getMap().setCenter(mapType.coords);
                    break;
                case UMTT_SearchResult:
                    // load list of caches and scale map to see them all
                    final Viewport viewport2 = DataStore.getBounds(mapType.searchResult.getGeocodes());
                    addSearchResultByGeocaches(mapType.searchResult);
                    // tileProvider.getMap().zoomToBounds(Viewport.containing(tempCaches));
                    tileProvider.getMap().zoomToBounds(viewport2);
                    break;
                default:
                    // nothing to do
                    break;
            }
            if (overridePositionAndZoom) {
                tileProvider.getMap().setZoom(Settings.getMapZoom(compatibilityMapMode));
                tileProvider.getMap().setCenter(Settings.getUMMapCenter());
                overridePositionAndZoom = false;
            }
            setTitle();

            if (loadInBackgroundHandler != null) {
                loadInBackgroundHandler.onDestroy();
            }
            loadInBackgroundHandler = new LoadInBackgroundHandler(this, tileProvider);
        }
        tileProvider.getMap().hideSpinner();

        // refresh options menu and routes/tracks display
        invalidateOptionsMenu();
        onResume();
    }

    private void initRouteTrackUtilsAndTracks(final boolean force) {
        if (force || routeTrackUtils == null) {
            routeTrackUtils = new RouteTrackUtils(this, null /* @todo: savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_ROUTETRACKUTILS) */, this::centerMap, this::clearIndividualRoute, this::reloadIndividualRoute, this::setTrack, this::isTargetSet);
            tracks = new Tracks(routeTrackUtils, this::setTrack);
        }
    }

    public void addSearchResultByGeocaches(final SearchResult searchResult) {
        Log.e("add " + searchResult.getGeocodes());
        for (String geocode : searchResult.getGeocodes()) {
            final Geocache temp = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (temp != null && temp.getCoords() != null) {
                geoitemLayer.add(temp);
            }
        }
    }

    public void addSearchResultByGeocaches(final Set<Geocache> searchResult) {
        Log.e("addSearchResult: " + searchResult.size());
        for (Geocache cache : searchResult) {
            geoitemLayer.add(cache);
        }
    }

    public void addSearchResultByGeocodes(final Set<String> searchResult) {
        final StringBuilder s = new StringBuilder();
        for (String geocode : searchResult) {
            s.append(" ").append(geocode);
            final Geocache temp = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (temp != null && temp.getCoords() != null) {
                geoitemLayer.add(temp);
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

    private void configMapChangeListener(final boolean enabled) {
        if (enabled) {
            tileProvider.getMap().setActivityMapChangeListener(unifiedMapPosition -> {
                final UnifiedMapPosition old = currentMapPosition;
                currentMapPosition = (UnifiedMapPosition) unifiedMapPosition;
                if (currentMapPosition.zoomLevel != old.zoomLevel) {
                    Log.e("zoom level changed from " + old.zoomLevel + " to " + currentMapPosition.zoomLevel);
                }
                if (currentMapPosition.latitude != old.latitude || currentMapPosition.longitude != old.longitude) {
                    Log.e("position change from [" + old.latitude + "/" + old.longitude + "] to [" + currentMapPosition.latitude + "/" + currentMapPosition.longitude + "]");
                    if (old.resetFollowMyLocation) {
                        followMyLocation = false;
                        initFollowMyLocationButton();
                    }
                }
                if (currentMapPosition.bearing != old.bearing) {
                    repaintRotationIndicator(currentMapPosition.bearing);
                    Log.e("bearing change from " + old.bearing + " to " + currentMapPosition.bearing);
                }
            });
            tileProvider.getMap().setResetFollowMyLocationListener(() -> {
                followMyLocation = false;
                initFollowMyLocationButton();
            });
        } else {
            tileProvider.getMap().setActivityMapChangeListener(null);
            tileProvider.getMap().setResetFollowMyLocationListener(null);
        }
    }

    /**
     * centers map on coords given + resets "followMyLocation" state
     **/
    private void centerMap(final Geopoint geopoint) {
        followMyLocation = false;
        initFollowMyLocationButton();
        tileProvider.getMap().setCenter(geopoint);
    }

    private Location getLocation() {
        final Geopoint center = tileProvider.getMap().getCenter();
        final Location loc = new Location("UnifiedMap");
        loc.setLatitude(center.getLatitude());
        loc.setLongitude(center.getLongitude());
        return loc;
    }

    private void initFollowMyLocationButton() {
        Settings.setFollowMyLocation(followMyLocation);
        if (followMyLocationItem != null) {
            followMyLocationItem.setIcon(followMyLocation ? R.drawable.ic_menu_mylocation : R.drawable.ic_menu_mylocation_off);
        }
    }

    protected void repaintRotationIndicator(final float bearing) {
        if (!tileProvider.getMap().usesOwnBearingIndicator) {
            final ImageView compassrose = findViewById(R.id.bearingIndicator);
            if (bearing == 0.0f) {
                compassrose.setImageBitmap(null);
            } else {
                final Matrix matrix = new Matrix();
                matrix.setRotate(bearing, rotationWidth / 2.0f, rotationHeight / 2.0f);
                compassrose.setImageBitmap(Bitmap.createBitmap(rotationIndicator, 0, 0, rotationWidth, rotationHeight, matrix, true));
                compassrose.setOnClickListener(v -> {
                    tileProvider.getMap().setBearing(0.0f);
                    repaintRotationIndicator(0.0f);
                });
            }
        }
    }

    // ========================================================================
    // Routes, tracks and targets handling

    @Nullable
    private Geocache getCurrentTargetCache() {
        if (tileProvider.getMap().positionLayer != null) {
            final String targetGeocode = tileProvider.getMap().positionLayer.mapDistanceDrawer.getTargetGeocode();
            if (StringUtils.isNotBlank(targetGeocode)) {
                return DataStore.loadCache(targetGeocode, LoadFlags.LOAD_CACHE_OR_DB);
            }
        }
        return null;
    }

    private void setTarget(final Geopoint geopoint, final String geocode) {
        AbstractPositionLayer positionLayer = tileProvider.getMap().positionLayer;
        if (positionLayer == null) {
            positionLayer = tileProvider.getMap().configPositionLayer(true);
        }
        if (positionLayer != null) {
            positionLayer.mapDistanceDrawer.setLastNavTarget(geopoint);
            if (StringUtils.isNotBlank(geocode)) {
                positionLayer.mapDistanceDrawer.setTargetGeocode(geocode);
                final Geocache target = getCurrentTargetCache();
                positionLayer.mapDistanceDrawer.setTarget(target != null ? target.getName() : StringUtils.EMPTY);
                positionLayer.setDestination(new GeoPoint(geopoint.getLatitude(), geopoint.getLongitude()));
                if (positionLayer.mapDistanceDrawer.getLastNavTarget() == null && target != null) {
                    positionLayer.mapDistanceDrawer.setLastNavTarget(target.getCoords());
                }
            } else {
                positionLayer.mapDistanceDrawer.setTargetGeocode(null);
                positionLayer.mapDistanceDrawer.setTarget(null);
                if (tileProvider.getMap().positionLayer != null) {
                    tileProvider.getMap().positionLayer.setDestination(null);
                }
            }
            /*
            if (navigationLayer != null) {
                navigationLayer.setDestination(lastNavTarget);
                navigationLayer.requestRedraw();
            }
            if (distanceView != null) {
                distanceView.setDestination(lastNavTarget);
                distanceView.setCoordinates(geoDirUpdate.getCurrentLocation());
            }
            */
        }
        ActivityMixin.invalidateOptionsMenu(this);
    }

    // glue method for old map
    // can be removed when removing CGeoMap and NewMap, routeTrackUtils need to be adapted then
    @SuppressWarnings("unused")
    private void centerMap(final double latitude, final double longitude, final Viewport viewport) {
        centerMap(new Geopoint(latitude, longitude));
    }

    private Boolean isTargetSet() {
//        return StringUtils.isNotBlank(targetGeocode) && null != lastNavTarget;
        return false; // @todo
    }

    private void setTrack(final String key, final IGeoDataProvider route, final int unused1, final int unused2) {
        tracks.setRoute(key, route);
        resumeTrack(key, null == route);
        initRouteTrackUtilsAndTracks(false);
        routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute, tracks);
    }

    private void reloadIndividualRoute() {
        individualRoute.reloadRoute((route) -> {
            if (tileProvider.getMap().positionLayer != null) {
                tileProvider.getMap().positionLayer.updateIndividualRoute(route);
                initRouteTrackUtilsAndTracks(false);
                routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute, tracks);
            }
        });
    }

    private void clearIndividualRoute() {
        individualRoute.clearRoute((route) -> {
            tileProvider.getMap().positionLayer.updateIndividualRoute(route);
            initRouteTrackUtilsAndTracks(false);
            routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute, tracks);
        });
        showToast(res.getString(R.string.map_individual_route_cleared));
    }

    private void toggleRouteItem(final RouteItem item) {
        if (item == null || StringUtils.isEmpty(item.getGeocode())) {
            return;
        }
        if (individualRoute == null) {
            individualRoute = new IndividualRoute(this::setTarget);
        }
        individualRoute.toggleItem(this, item, tileProvider.getMap().positionLayer);
        // distanceView.showRouteDistance();
        initRouteTrackUtilsAndTracks(false);
        routeTrackUtils.updateRouteTrackButtonVisibility(findViewById(R.id.container_individualroute), individualRoute, tracks);
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

        // live map mode
        final MenuItem itemMapLive = menu.findItem(R.id.menu_map_live); // @todo: take it from mapMode
        if (Settings.isLiveMap()) {
            itemMapLive.setIcon(R.drawable.ic_menu_sync_enabled);
            itemMapLive.setTitle(res.getString(R.string.map_live_disable));
        } else {
            itemMapLive.setIcon(R.drawable.ic_menu_sync_disabled);
            itemMapLive.setTitle(res.getString(R.string.map_live_enable));
        }
/* @todo        itemMapLive.setVisible(mapOptions.coords == null || mapOptions.mapMode == MapMode.LIVE); */ itemMapLive.setVisible(true);

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

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.map_activity, menu);

        followMyLocationItem = menu.findItem(R.id.menu_toggle_mypos);
        initFollowMyLocationButton();

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
            if (mapOptions.mapMode == MapMode.SINGLE) {
                setTarget(mapOptions.coords, mapOptions.geocode);
            }
            mapOptions.mapMode = MapMode.LIVE;
            updateSelectedBottomNavItemId();
            mapOptions.title = StringUtils.EMPTY;
            setTitle();
            */
        } else if (id == R.id.menu_toggle_mypos) {
            followMyLocation = !followMyLocation;
            Settings.setLiveMap(followMyLocation);
            if (followMyLocation) {
                final Location currentLocation = geoDirUpdate.getCurrentLocation();
                tileProvider.getMap().setCenter(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
                currentMapPosition.resetFollowMyLocation = false;
            }
            initFollowMyLocationButton();
        } else if (id == R.id.menu_map_rotation_off) {
            setMapRotation(item, MAPROTATION_OFF);
        } else if (id == R.id.menu_map_rotation_manual) {
            setMapRotation(item, MAPROTATION_MANUAL);
        } else if (id == R.id.menu_map_rotation_auto) {
            setMapRotation(item, MAPROTATION_AUTO);
        } else if (id == R.id.menu_check_routingdata) {
            final BoundingBox bb = tileProvider.getMap().getBoundingBox();
            MapUtils.checkRoutingData(this, bb.getMinLatitude(), bb.getMinLongitude(), bb.getMaxLatitude(), bb.getMaxLongitude());
        } else if (HistoryTrackUtils.onOptionsItemSelected(this, id, () -> tileProvider.getMap().positionLayer.repaintHistory(), () -> tileProvider.getMap().positionLayer.clearHistory())
                || DownloaderUtils.onOptionsItemSelected(this, id, true)) {
            return true;
        } else if (id == R.id.menu_theme_mode) {
            tileProvider.getMap().selectTheme(this);
        } else if (id == R.id.menu_theme_options) {
            tileProvider.getMap().selectThemeOptions(this);
        } else if (id == R.id.menu_routetrack) {
            routeTrackUtils.showPopup(individualRoute, this::setTarget);
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
        } else {
            final String language = TileProviderFactory.getLanguage(id);
            if (language != null || id == MAP_LANGUAGE_DEFAULT_ID) {
                item.setChecked(true);
                Settings.setMapLanguage(language);
                tileProvider.getMap().setPreferredLanguage(language);
                return true;
            }
            final AbstractTileProvider tileProviderLocal = TileProviderFactory.getTileProvider(id);
            if (tileProviderLocal != null) {
                item.setChecked(true);
                changeMapSource(tileProviderLocal);
                return true;
            }
            if (tileProvider.getMap().onOptionsItemSelected(item)) {
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
        tileProvider.getMap().setMapRotation(mapRotation);
        item.setChecked(true);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.routeTrackUtils.onActivityResult(requestCode, resultCode, data);
    }

    // ========================================================================
    // Map tap handling

    public void onTap(final int latitudeE6, final int longitudeE6, final boolean isLongTap) {
        Log.e("registered " + (isLongTap ? "long " : "") + " tap on map @ (" + latitudeE6 + ", " + longitudeE6 + ")");

        // numbers of cache markers fitting into width/height
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        final View v = tileProvider.getMap().mMapView;
        final float numMarkersWidth = v.getWidth() / displayMetrics.density / SIZE_CACHE_MARKER_DP;
        final float numMarkersHeight = v.getHeight() / displayMetrics.density / SIZE_CACHE_MARKER_DP;

        // lat/lon covered by single marker
        final BoundingBox bb = tileProvider.getMap().getBoundingBox();
        final int deltaLat = (int) ((bb.maxLatitudeE6 - bb.minLatitudeE6) / numMarkersHeight);
        final int deltaLong = (int) ((bb.maxLongitudeE6 - bb.minLongitudeE6) / numMarkersWidth);

        // calculate new bounding box, taking offset of icon vs. hotspot into account
        bb.minLatitudeE6 = latitudeE6 - 2 * deltaLat;
        bb.maxLatitudeE6 = latitudeE6 + deltaLat;
        bb.minLongitudeE6 = (int) (longitudeE6 - 1.5 * deltaLong);
        bb.maxLongitudeE6 = (int) (longitudeE6 + 1.5 * deltaLong);

        // lookup elements touched by this
        final LinkedList<RouteItem> result = geoitemLayer.find(bb);
        Log.e("touched elements (" + result.size() + "): " + result);

        if (result.size() == 0) {
            if (isLongTap) {
                // @todo: open context popup for coordinates
            } else {
                FilterUtils.toggleActionBar(this);
            }
        } else if (result.size() == 1) {
            handleTap(result.get(0), isLongTap);
        } else {
            try {
                final ArrayList<RouteItem> sorted = new ArrayList<>(result);
                Collections.sort(sorted, RouteItem.NAME_COMPARATOR);

                final ArrayAdapter<RouteItem> adapter = new ArrayAdapter<RouteItem>(this, R.layout.cacheslist_item_select, sorted) {
                    @NonNull
                    @Override
                    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                        return GeoItemSelectorUtils.createRouteItemView(UnifiedMapActivity.this, getItem(position),
                                GeoItemSelectorUtils.getOrCreateView(UnifiedMapActivity.this, convertView, parent));
                    }
                };

                final AlertDialog dialog = Dialogs.newBuilder(this)
                    .setTitle(res.getString(R.string.map_select_multiple_items))
                    .setAdapter(adapter, (dialog1, which) -> {
                        if (which >= 0 && which < sorted.size()) {
                            handleTap(sorted.get(which), isLongTap);
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

    private void handleTap(final RouteItem item, final boolean isLongTap) {
        if (isLongTap) {
            // toggle route item
            if (Settings.isLongTapOnMapActivated()) {
                toggleRouteItem(item);
            }
        } else {
            // open popup for element
            if (item.getType() == RouteItem.RouteItemType.GEOCACHE) {
                // @todo: do we need a DataStore.loadCache() before?
                CachePopup.startActivityAllowTarget(this, item.getGeocode());
            } else if (item.getType() == RouteItem.RouteItemType.WAYPOINT && item.getWaypointId() != 0) {
                // @todo: do we need a DataStore.loadWaypoint() before?
                WaypointPopup.startActivityAllowTarget(this, item.getWaypointId(), item.getGeocode());
            } else {
                // @todo: open context popup for coordinates
            }
        }
    }

    // ========================================================================
    // Lifecycle methods

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_ROUTETRACKUTILS, routeTrackUtils.getState());

//        final MapState state = prepareMapState();
        outState.putParcelable(BUNDLE_MAPTYPE, mapType);
//        if (proximityNotification != null) {
//            outState.putParcelable(BUNDLE_PROXIMITY_NOTIFICATION, proximityNotification);
//        }
        if (individualRoute != null) {
            outState.putParcelable(BUNDLE_ROUTE, individualRoute);
        }
        outState.putBoolean(BUNDLE_OVERRIDEPOSITIONANDZOOM, true);
    }

    @Override
    protected void onStart() {
        super.onStart();

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
        tileProvider.getMap().onPause();
        configMapChangeListener(false);

        Settings.setMapZoom(compatibilityMapMode, tileProvider.getMap().getCurrentZoom());
        Settings.setMapCenter(tileProvider.getMap().getCenter());

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tileProvider.getMap().onResume();
        configMapChangeListener(true);
        resumeRoute(false);
        if (tracks != null) {
            tracks.resumeAllTracks(this::resumeTrack);
        }
//        MapUtils.updateFilterBar(this, mapOptions.filterContext);
    }

    private void resumeRoute(final boolean force) {
        if (null == individualRoute || force) {
            individualRoute = new IndividualRoute(this::setTarget);
            reloadIndividualRoute();
        } else if (tileProvider.getMap().positionLayer != null) {
            individualRoute.updateRoute((route) -> tileProvider.getMap().positionLayer.updateIndividualRoute(route));
        }
    }

    private void resumeTrack(final String key, final boolean preventReloading) {
        if (null == tracks && !preventReloading) {
            this.tracks = new Tracks(this.routeTrackUtils, this::setTrack);
        } else if (null != tracks) {
            tileProvider.getMap().positionLayer.updateTrack(key, tracks.getRoute(key));
        }
    }

    @Override
    protected void onDestroy() {
        tileProvider.getMap().onDestroy();
        if (loadInBackgroundHandler != null) {
            loadInBackgroundHandler.onDestroy();
        }
        super.onDestroy();
    }

}
