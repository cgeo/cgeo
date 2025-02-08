package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.maps.MapStarUtils;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinate;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.LiveMapGeocacheLoader;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.CollectionDiff;
import cgeo.geocaching.utils.CompactIconModeUtils;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;
import java.util.Map;

public class GeoItemsLayer {

    private Map<String, String> lastDisplayedGeocaches = new HashMap<>();
    private Map<String, String> lastDisplayedWaypoints = new HashMap<>();
    private final CollectionDiff<String, String, String> lastDisplayedCacheStars = new CollectionDiff<>(k -> k);
    private boolean lastForceCompactIconMode = false;


    private static String getKeyFor(final ICoordinate coords, final CacheMarker marker) {
        return coords.getCoords() + "-" + marker.hashCode();
    }

    public GeoItemsLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);


        viewModel.caches.observeForRead(activity, caches -> { // this is always executed on UI thread, thus doesn't need to be thread save

            final Map<String, String> currentlyDisplayedGeocaches = new HashMap<>();

            final boolean forceCompactIconMode = CompactIconModeUtils.forceCompactIconMode();
            if (lastForceCompactIconMode != forceCompactIconMode) {
                lastForceCompactIconMode = forceCompactIconMode;
                viewModel.waypoints.notifyDataChanged();
            }

            for (Geocache cache : caches) {
                final CacheMarker cm = forceCompactIconMode ? MapMarkerUtils.getCacheDotMarker(activity.getResources(), cache) : MapMarkerUtils.getCacheMarker(activity.getResources(), cache, null, true);
                final String contentKey = getKeyFor(cache, cm);
                currentlyDisplayedGeocaches.put(cache.getGeocode(), contentKey);

                if (!lastDisplayedGeocaches.containsKey(cache.getGeocode()) || !lastDisplayedGeocaches.get(cache.getGeocode()).equals(contentKey)) {

                    layer.put(UnifiedMapViewModel.CACHE_KEY_PREFIX + cache.getGeocode(), GeoPrimitive.createMarker(cache.getCoords(),
                        GeoIcon.builder()
                            .setBitmap(cm.getBitmap())
                            .setHotspot(forceCompactIconMode ? GeoIcon.Hotspot.CENTER : GeoIcon.Hotspot.BOTTOM_CENTER)
                            .build()
                    ).buildUpon().setZLevel(LayerHelper.ZINDEX_GEOCACHE).build());
                }
            }

            for (String geocode : currentlyDisplayedGeocaches.keySet()) {
                lastDisplayedGeocaches.remove(geocode);
            }

            for (String geocode : lastDisplayedGeocaches.keySet()) {
                layer.remove(UnifiedMapViewModel.CACHE_KEY_PREFIX + geocode);
            }

            lastDisplayedGeocaches = currentlyDisplayedGeocaches;

        });

        viewModel.cachesWithStarDrawn.observeForRead(activity, starCodes -> lastDisplayedCacheStars.executeDiff(starCodes, true, addStar -> {
            final Geocache cache = DataStore.loadCache(addStar, LoadFlags.LOAD_CACHE_OR_DB);
            final GeoItem star = MapStarUtils.createStar(cache);
            if (star != null) {
                layer.put(UnifiedMapViewModel.CACHE_STAR_KEY_PREFIX + addStar, star);
            }
        }, removeStar -> layer.remove(UnifiedMapViewModel.CACHE_STAR_KEY_PREFIX + removeStar)));


        viewModel.waypoints.observeForRead(activity, waypoints -> { // this is always executed on UI thread, thus doesn't need to be thread save

            final Map<String, String> currentlyDisplayedWaypoints = new HashMap<>();

            for (Waypoint waypoint : waypoints) {
                final CacheMarker cm = lastForceCompactIconMode ? MapMarkerUtils.getWaypointDotMarker(activity.getResources(), waypoint) : MapMarkerUtils.getWaypointMarker(activity.getResources(), waypoint, true, true);
                final String contentKey = getKeyFor(waypoint, cm);
                currentlyDisplayedWaypoints.put(waypoint.getFullGpxId(), contentKey);

                if (!lastDisplayedWaypoints.containsKey(waypoint.getFullGpxId()) || !lastDisplayedWaypoints.get(waypoint.getFullGpxId()).equals(contentKey)) {

                    layer.put(UnifiedMapViewModel.WAYPOINT_KEY_PREFIX + waypoint.getFullGpxId(), GeoPrimitive.createMarker(waypoint.getCoords(),
                            GeoIcon.builder()
                                    .setBitmap(cm.getBitmap())
                                    .setHotspot(lastForceCompactIconMode ? GeoIcon.Hotspot.CENTER : GeoIcon.Hotspot.BOTTOM_CENTER)
                                    .build()
                    ).buildUpon().setZLevel(LayerHelper.ZINDEX_WAYPOINT).build());
                }
            }

            for (String fullGpxId : currentlyDisplayedWaypoints.keySet()) {
                lastDisplayedWaypoints.remove(fullGpxId);
            }

            for (String fullGpxId : lastDisplayedWaypoints.keySet()) {
                layer.remove(UnifiedMapViewModel.WAYPOINT_KEY_PREFIX + fullGpxId);
            }

            lastDisplayedWaypoints = currentlyDisplayedWaypoints;
        });

        viewModel.liveLoadStatus.observe(activity, lv -> {
            if (Settings.enableFeatureUnifiedDebug() && lv != null) {
                if (Viewport.isValid(lv.cachedViewport)) {
                    layer.put("CACHED_LAYER", lv.cachedViewport.toGeoItem(GeoStyle.transparentFill(Color.GRAY, 128, 5), 50));
                    layer.put("CACHED_LAYER_CENTER", GeoPrimitive.createCircle(lv.cachedViewport.center, 0.5f, GeoStyle.solid(Color.RED, 4f)).buildUpon().setZLevel(55).build());
                }
                for (LiveMapGeocacheLoader.ConnectorState state : lv.connectorStates.values()) {
                    if (Viewport.isValid(state.viewport)) {
                        layer.put("CACHED_LAYER_" + state.connectorName, state.viewport.toGeoItem(GeoStyle.transparentFill(Color.BLUE, 128, 3), 51));

                    }
                }
            } else {
                layer.remove("CACHED_LAYER");
                layer.remove("CACHED_LAYER_CENTER");
                if (lv != null) {
                    for (String key : lv.connectorStates.keySet()) {
                        layer.remove("CACHED_LAYER_" + key);
                    }
                }
            }
        });

    }

}
