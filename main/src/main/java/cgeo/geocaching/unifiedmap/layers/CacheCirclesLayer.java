package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.location.IConversion;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.MapLineUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.Set;

public class CacheCirclesLayer {

    private static final float radius = (float) (528.0 * IConversion.FEET_TO_KILOMETER);


    public CacheCirclesLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);


        viewModel.caches.observe(activity, caches -> {

            if (Settings.isShowCircles()) {
                for (Geocache cache : caches.getAsList()) { // Creates a clone to avoid ConcurrentModificationException
                    if (cache.applyDistanceRule()) {

                        layer.put(cache.getGeocode(),
                                GeoPrimitive.createCircle(cache.getCoords(), radius, GeoStyle.builder()
                                        .setStrokeWidth(2.0f)
                                        .setStrokeColor(MapLineUtils.getCircleColor())
                                        .setFillColor(MapLineUtils.getCircleFillColor())
                                        .build()
                                ).buildUpon().setZLevel(LayerHelper.ZINDEX_CIRCLE).build());
                    }
                }

            } else {
                // only possible cause we use a separate layer just for cache circles.
                // Not finally decided if this makes sense and we really want to split rendering into multiple layers.
                for (String key : layer.keySet()) {
                    layer.remove(key);
                }
            }

        });


        viewModel.waypoints.observe(activity, waypoints -> {

            if (Settings.isShowCircles()) {
                for (Waypoint waypoint : (Set<Waypoint>) waypoints.clone()) { // Creates a clone to avoid ConcurrentModificationExceptions
                    if (waypoint.applyDistanceRule()) {

                        layer.put(UnifiedMapViewModel.CACHE_KEY_PREFIX + waypoint.getFullGpxId(),
                                GeoPrimitive.createCircle(waypoint.getCoords(), radius, GeoStyle.builder()
                                        .setStrokeWidth(2.0f)
                                        .setStrokeColor(MapLineUtils.getCircleColor())
                                        .setFillColor(MapLineUtils.getCircleFillColor())
                                        .build()
                                ).buildUpon().setZLevel(LayerHelper.ZINDEX_CIRCLE).build());
                    }
                }
            }
        });

    }
}
