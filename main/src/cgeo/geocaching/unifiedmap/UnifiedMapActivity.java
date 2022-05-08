package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.HistoryTrackUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;
import static cgeo.geocaching.settings.Settings.MAPROTATION_OFF;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_LANGUAGE_DEFAULT_ID;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;

public class UnifiedMapActivity extends AbstractBottomNavigationActivity {

    private static final String STATE_ROUTETRACKUTILS = "routetrackutils";
    private static final String BUNDLE_ROUTE = "route";

    private static final String ROUTING_SERVICE_KEY = "UnifiedMap";

    private AbstractUnifiedMap map = null;
    private final UpdateLoc geoDirUpdate = new UpdateLoc(this);
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private static boolean followMyLocation = Settings.isLiveMap();
    private MenuItem followMyLocationItem = null;

    private RouteTrackUtils routeTrackUtils = null;
    private IndividualRoute individualRoute = null;
    private Tracks tracks = null;
    private UnifiedMapPosition currentMapPosition = new UnifiedMapPosition();

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
        Location currentLocation = Sensors.getInstance().currentGeo();
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
                            mapActivity.map.setCenter(new Geopoint(currentLocation));
                            mapActivity.currentMapPosition.resetFollowMyLocation = false;
                        }

                        if (needsRepaintForDistanceOrAccuracy || needsRepaintForHeading) {
                            if (mapActivity.map.positionLayer != null) {
                                mapActivity.map.positionLayer.setCurrentPositionAndHeading(currentLocation, currentHeading);
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
            return Math.abs(AngleUtils.difference(currentHeading, mapActivity.map.getHeading())) > MIN_HEADING_DELTA;
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
        changeMapSource(Settings.getTileProvider());

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
//            mapOptions.mapState = savedInstanceState.getParcelable(BUNDLE_MAP_STATE);
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

        Routing.connect(ROUTING_SERVICE_KEY, () -> resumeRoute(true));
        CompactIconModeUtils.setCompactIconModeThreshold(getResources());

//        MapUtils.showMapOneTimeMessages(this, mapMode);

    }

    private void changeMapSource(final AbstractTileProvider newSource) {
        final AbstractUnifiedMap oldMap = map;
        if (oldMap != null) {
            oldMap.prepareForTileSourceChange();
        }
        map = newSource.getMap();
        if (map != oldMap) {
            if (oldMap != null) {
                map.init(this, oldMap.getCurrentZoom(), oldMap.getCenter(), () -> onMapReadyTasks(newSource, true));
            } else {
                map.init(this, Settings.getMapZoom(MapMode.LIVE), null, () -> onMapReadyTasks(newSource, true));   // @todo: use actual mapmode
            }
            configMapChangeListener(true);
        } else {
            onMapReadyTasks(newSource, false);
        }
    }

    private void onMapReadyTasks(final AbstractTileProvider newSource, final boolean mapChanged) {
        TileProviderFactory.resetLanguages();
        map.setTileSource(newSource);
        Settings.setTileProvider(newSource);
        map.setDelayedZoomTo();
        map.setDelayedCenterTo();

        if (mapChanged) {
            routeTrackUtils = new RouteTrackUtils(this, null /* @todo: savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_ROUTETRACKUTILS) */, this::centerMap, this::clearIndividualRoute, this::reloadIndividualRoute, this::setTrack, this::isTargetSet);
            tracks = new Tracks(routeTrackUtils, this::setTrack);

            // map settings popup
//        findViewById(R.id.map_settings_popup).setOnClickListener(v -> MapSettingsUtils.showSettingsPopup(this, individualRoute, this::refreshMapData, this::routingModeChanged, this::compactIconModeChanged, mapOptions.filterContext));

            // routes / tracks popup
            findViewById(R.id.map_individualroute_popup).setOnClickListener(v -> routeTrackUtils.showPopup(individualRoute, this::setTarget));
        }

        // refresh options menu and routes/tracks display
        invalidateOptionsMenu();
        onResume();
    }

    private void configMapChangeListener(final boolean enabled) {
        if (enabled) {
            map.setActivityMapChangeListener(unifiedMapPosition -> {
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
                    Log.e("bearing change from " + old.bearing + " to " + currentMapPosition.bearing);
                }
            });
        } else {
            map.setActivityMapChangeListener(null);
        }
    }

    /** centers map on coords given + resets "followMyLocation" state **/
    private void centerMap(final Geopoint geopoint) {
        followMyLocation = false;
        initFollowMyLocationButton();
        map.setCenter(geopoint);
    }

    private Location getLocation() {
        final Geopoint center = map.getCenter();
        final Location loc = new Location("UnifiedMap");
        loc.setLatitude(center.getLatitude());
        loc.setLongitude(center.getLongitude());
        return loc;
    }

    private void initFollowMyLocationButton() {
        if (followMyLocationItem != null) {
            followMyLocationItem.setIcon(followMyLocation ? R.drawable.ic_menu_mylocation : R.drawable.ic_menu_mylocation_off);
        }
    }

    // ========================================================================
    // Routes, tracks and targets handling

    @SuppressWarnings("unused") // @todo
    private void setTarget(final Geopoint geopoint, final String s) {
        if (map.positionLayer != null) {
            map.positionLayer.setDestination(new GeoPoint(geopoint.getLatitude(), geopoint.getLongitude()));
        }
        // @todo
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

    private void setTrack(final String key, final Route route) {
        tracks.setRoute(key, route);
        resumeTrack(key, null == route);
    }

    private void reloadIndividualRoute() {
        individualRoute.reloadRoute((route) -> {
            if (map.positionLayer != null) {
                map.positionLayer.updateIndividualRoute(route);
            }
        });
    }

    private void clearIndividualRoute() {
        individualRoute.clearRoute((route) -> map.positionLayer.updateIndividualRoute(route));
//        ActivityMixin.invalidateOptionsMenu(this); // @todo still needed since introduction of route popup?
        showToast(res.getString(R.string.map_individual_route_cleared));
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
        this.routeTrackUtils.onPrepareOptionsMenu(menu, findViewById(R.id.container_individualroute), individualRoute, tracks);

        // map rotation state
        menu.findItem(R.id.menu_map_rotation).setVisible(true); // @todo: can be visible always when CGeoMap/NewMap is removed
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

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.map_activity, menu);

        TileProviderFactory.addMapviewMenuItems(this, menu);

        followMyLocationItem = menu.findItem(R.id.menu_toggle_mypos);
        initFollowMyLocationButton();

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();
        /* yet missing:
        - live mode
        - all cache related menu entries
        - all target related menu entries
        - filter related menu entries
        - theme related menu entries (select theme, theme options, theme legend)
         */
        if (id == R.id.menu_toggle_mypos) {
            followMyLocation = !followMyLocation;
            Settings.setLiveMap(followMyLocation);
            if (followMyLocation) {
                final Location currentLocation = geoDirUpdate.getCurrentLocation();
                map.setCenter(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
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
            final BoundingBox bb = map.getBoundingBox();
            MapUtils.checkRoutingData(this, bb.getMinLatitude(), bb.getMinLongitude(), bb.getMaxLatitude(), bb.getMaxLongitude());
        } else if (HistoryTrackUtils.onOptionsItemSelected(this, id, () -> map.positionLayer.repaintHistory(), () -> map.positionLayer.clearHistory())
            || DownloaderUtils.onOptionsItemSelected(this, id)) {
            return true;
        } else if (id == R.id.menu_routetrack) {
            routeTrackUtils.showPopup(individualRoute, this::setTarget);
        } else {
            final String language = TileProviderFactory.getLanguage(id);
            if (language != null || id == MAP_LANGUAGE_DEFAULT_ID) {
                item.setChecked(true);
                Settings.setMapLanguage(language);
                map.setPreferredLanguage(language);
                return true;
            }
            final AbstractTileProvider tileProvider = TileProviderFactory.getTileProvider(id);
            if (tileProvider != null) {
                item.setChecked(true);
                changeMapSource(tileProvider);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void setMapRotation(final MenuItem item, final int mapRotation) {
        Settings.setMapRotation(mapRotation);
        map.setMapRotation(mapRotation);
        item.setChecked(true);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.routeTrackUtils.onActivityResult(requestCode, resultCode, data);
    }

    // ========================================================================
    // Lifecycle methods

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_ROUTETRACKUTILS, routeTrackUtils.getState());

//        final MapState state = prepareMapState();
//        outState.putParcelable(BUNDLE_MAP_STATE, state);
//        if (proximityNotification != null) {
//            outState.putParcelable(BUNDLE_PROXIMITY_NOTIFICATION, proximityNotification);
//        }
        if (individualRoute != null) {
            outState.putParcelable(BUNDLE_ROUTE, individualRoute);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

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
    protected void onStop() {
        this.resumeDisposables.clear();
        super.onStop();
    }

    @Override
    public void onPause() {
        map.onPause();
        configMapChangeListener(false);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
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
        } else if (map.positionLayer != null) {
            individualRoute.updateRoute((route) -> map.positionLayer.updateIndividualRoute(route));
        }
    }

    private void resumeTrack(final String key, final boolean preventReloading) {
        if (null == tracks && !preventReloading) {
            this.tracks = new Tracks(this.routeTrackUtils, this::setTrack);
        } else if (null != tracks) {
            map.positionLayer.updateTrack(key, tracks.getRoute(key));
        }
    }

    @Override
    protected void onDestroy() {
        map.onDestroy();
        super.onDestroy();
    }

}
