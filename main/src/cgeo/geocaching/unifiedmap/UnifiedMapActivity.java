package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.HistoryTrackUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;
import static cgeo.geocaching.settings.Settings.MAPROTATION_OFF;
import static cgeo.geocaching.unifiedmap.UnifiedMapType.BUNDLE_MAPTYPE;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_LANGUAGE_DEFAULT_ID;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;

public class UnifiedMapActivity extends AbstractBottomNavigationActivity {

    private static final String STATE_ROUTETRACKUTILS = "routetrackutils";
    private static final String BUNDLE_ROUTE = "route";

    private static final String ROUTING_SERVICE_KEY = "UnifiedMap";

    private AbstractTileProvider tileProvider = null;
    private AbstractGeoitemLayer geoitemLayer = null;

    private final UpdateLoc geoDirUpdate = new UpdateLoc(this);
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private static boolean followMyLocation = Settings.isLiveMap();
    private MenuItem followMyLocationItem = null;

    private RouteTrackUtils routeTrackUtils = null;
    private IndividualRoute individualRoute = null;
    private Tracks tracks = null;
    private UnifiedMapPosition currentMapPosition = new UnifiedMapPosition();
    private UnifiedMapType mapType = null;

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

        // Get fresh map information from the bundle if any
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_MAPTYPE)) {
                mapType = savedInstanceState.getParcelable(BUNDLE_MAPTYPE);
            }
            Log.e("mapType=" + mapType);
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

        Routing.connect(ROUTING_SERVICE_KEY, () -> resumeRoute(true));
        CompactIconModeUtils.setCompactIconModeThreshold(getResources());

//        MapUtils.showMapOneTimeMessages(this, mapMode);

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
                tileProvider.getMap().init(this, Settings.getMapZoom(MapMode.LIVE), null, () -> onMapReadyTasks(newSource, true));   // @todo: use actual mapmode
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

        final View spinner = findViewById(R.id.map_progressbar);
        if (spinner != null) {
            spinner.setVisibility(View.GONE);
        }

        if (mapChanged) {
            routeTrackUtils = new RouteTrackUtils(this, null /* @todo: savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_ROUTETRACKUTILS) */, this::centerMap, this::clearIndividualRoute, this::reloadIndividualRoute, this::setTrack, this::isTargetSet);
            tracks = new Tracks(routeTrackUtils, this::setTrack);

            // map settings popup
//        findViewById(R.id.map_settings_popup).setOnClickListener(v -> MapSettingsUtils.showSettingsPopup(this, individualRoute, this::refreshMapData, this::routingModeChanged, this::compactIconModeChanged, mapOptions.filterContext));

            // routes / tracks popup
            findViewById(R.id.map_individualroute_popup).setOnClickListener(v -> routeTrackUtils.showPopup(individualRoute, this::setTarget));

            // create geoitem layers
            geoitemLayer = tileProvider.getMap().createGeoitemLayers(tileProvider);

            // react to mapType
            switch (mapType.type) {
                case UMTT_PlainMap:
                    // restore last saved position and zoom
                    tileProvider.getMap().setZoom(Settings.getMapZoom(MapMode.LIVE));
                    tileProvider.getMap().setCenter(Settings.getUMMapCenter());
                    break;
                case UMTT_TargetGeocode:
                    final Geocache cache = DataStore.loadCache(mapType.target, LoadFlags.LOAD_CACHE_OR_DB);
                    if (cache != null && cache.getCoords() != null) {
                        geoitemLayer.add(cache);
                        tileProvider.getMap().setCenter(cache.getCoords());
                        setTarget(cache.getCoords(), cache.getName());
                        // @todo: adjust zoom if needed
                    }
                    break;
                case UMTT_TargetCoords:
                    tileProvider.getMap().setCenter(mapType.coords);
                    break;
                default:
                    // nothing to do
                    break;
            }
            // @todo for testing purposes only
            /*
            if (geoitemLayer != null) {
                geoitemLayer.add("GC9C8G5");
                geoitemLayer.add("GC9RZT2");
                geoitemLayer.add("GC37RRG");
                geoitemLayer.add("GC360D1");
                geoitemLayer.add("GC8902H");
            }
            */
        }

        // refresh options menu and routes/tracks display
        invalidateOptionsMenu();
        onResume();
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

    private void setTarget(final Geopoint geopoint, final String s) {
        if (tileProvider.getMap().positionLayer != null) {
            tileProvider.getMap().positionLayer.setDestination(new GeoPoint(geopoint.getLatitude(), geopoint.getLongitude()));
        }
        // @todo
        /*
        lastNavTarget = coords;
        if (StringUtils.isNotBlank(geocode)) {
            targetGeocode = geocode;
            final Geocache target = getCurrentTargetCache();
            targetView.setTarget(targetGeocode, target != null ? target.getName() : StringUtils.EMPTY);
            if (lastNavTarget == null && target != null) {
                lastNavTarget = target.getCoords();
            }
        } else {
            targetGeocode = null;
            targetView.setTarget(null, null);
        }
        if (navigationLayer != null) {
            navigationLayer.setDestination(lastNavTarget);
            navigationLayer.requestRedraw();
        }
        if (distanceView != null) {
            distanceView.setDestination(lastNavTarget);
            distanceView.setCoordinates(geoDirUpdate.getCurrentLocation());
        }

        ActivityMixin.invalidateOptionsMenu(this);

         */
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
            if (tileProvider.getMap().positionLayer != null) {
                tileProvider.getMap().positionLayer.updateIndividualRoute(route);
            }
        });
    }

    private void clearIndividualRoute() {
        individualRoute.clearRoute((route) -> tileProvider.getMap().positionLayer.updateIndividualRoute(route));
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
        ViewUtils.extendMenuActionBarDisplayItemCount(this, menu);

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

        // theming options
        menu.findItem(R.id.menu_theme_mode).setVisible(tileProvider.supportsThemes());
        menu.findItem(R.id.menu_theme_options).setVisible(tileProvider.supportsThemes());
//@todo        menu.findItem(R.id.menu_theme_legend).setVisible(tileProvider.supportsThemes() && RenderThemeLegend.supportsLegend());

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
        - live mode
        - all cache related menu entries
        - all target related menu entries
        - filter related menu entries
         */
        if (id == R.id.menu_toggle_mypos) {
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
        } else if (id == R.id.menu_theme_legend) {
            // @todo
            // RenderThemeLegend.showLegend(this, this.renderThemeHelper, mapView.getModel().displayModel);
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
            final AbstractTileProvider tileProvider = TileProviderFactory.getTileProvider(id);
            if (tileProvider != null) {
                item.setChecked(true);
                changeMapSource(tileProvider);
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
        tileProvider.getMap().onPause();
        configMapChangeListener(false);

        Settings.setMapZoom(MapMode.LIVE, tileProvider.getMap().getCurrentZoom()); // @todo: use actual map mode
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
        super.onDestroy();
    }

}
