package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.location.Geopoint;
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
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_LANGUAGE_DEFAULT_ID;

import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class UnifiedMapActivity extends AbstractBottomNavigationActivity {

    private AbstractUnifiedMap map = null;
    private final UpdateLoc geoDirUpdate = new UpdateLoc(this);
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private static boolean followMyLocation = Settings.isLiveMap();

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
        private final WeakReference<UnifiedMapActivity> mapActivityRef;

        UpdateLoc(@NonNull final UnifiedMapActivity mapActivity) {
            mapActivityRef = new WeakReference<>(mapActivity);
        }

        @Override
        public void updateGeoDir(@NonNull final GeoData geo, final float dir) {
            currentLocation = geo;
            currentHeading = AngleUtils.getDirectionNow(dir /* test */ + 360.0f * (float) Math.random());
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
                            mapActivity.centerMap(new Geopoint(currentLocation));
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
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.map_activity, menu);

        TileProviderFactory.addMapviewMenuItems(this, menu);
//        MapProviderFactory.addMapViewLanguageMenuItems(menu);     // available for mapsforge offline maps only

        initFollowMyLocationButton(menu.findItem(R.id.menu_toggle_mypos));
//        FilterUtils.initializeFilterMenu(this, this);

        return result;
    }

    private void initFollowMyLocationButton(final MenuItem item) {
        if (item != null) {
            item.setIcon(followMyLocation ? R.drawable.ic_menu_mylocation : R.drawable.ic_menu_mylocation_off);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_toggle_mypos) {
            followMyLocation = !followMyLocation;
            Settings.setLiveMap(followMyLocation);
            if (followMyLocation) {
                final Location currentLocation = geoDirUpdate.getCurrentLocation();
                map.setCenter(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
            }
            initFollowMyLocationButton(item);
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

    private void changeMapSource(final AbstractTileProvider newSource) {
        final AbstractUnifiedMap oldMap = map;
        if (oldMap != null) {
            oldMap.prepareForTileSourceChange();
        }
        map = newSource.getMap();
        if (map != oldMap) {
            map.init(this);
        }
        TileProviderFactory.resetLanguages();
        map.setTileSource(newSource);
        Settings.setTileProvider(newSource);

        // adjust zoom to be in allowed zoom range for current map
        final int currentZoom = map.getCurrentZoom();
        if (currentZoom < map.getZoomMin()) {
            map.setZoom(map.getZoomMin());
        } else if (currentZoom > map.getZoomMax()) {
            map.setZoom(map.getZoomMax());
        }
    }

    private void centerMap(final Geopoint geopoint) {
        map.setCenter(geopoint);
    }

    private Location getLocation() {
        final Geopoint center = map.getCenter();
        final Location loc = new Location("UnifiedMap");
        loc.setLatitude(center.getLatitude());
        loc.setLongitude(center.getLongitude());
        return loc;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
         final boolean result = super.onPrepareOptionsMenu(menu);
         TileProviderFactory.addMapViewLanguageMenuItems(menu);
         return result;
    }

    // ========================================================================
    // Lifecycle methods

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
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onDestroy() {
        map.onDestroy();
        super.onDestroy();
    }

    // Bottom navigation methods

    @Override
    public int getSelectedBottomItemId() {
        return MENU_MAP;    // @todo
    }

}
