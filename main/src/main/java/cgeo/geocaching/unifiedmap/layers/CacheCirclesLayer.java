package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.location.IConversion;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.MapLineUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class CacheCirclesLayer {

    private static final String KEY_CACHE_CIRCLES = "cache_circles";
    private static final String KEY_WAYPOINT_CIRCLES = "wp_circles";

    private static final float radius = (float) (528.0 * IConversion.FEET_TO_KILOMETER);


    public CacheCirclesLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.caches.observeForRead(activity, caches -> {

            if (Settings.isShowCircles()) {
                final GeoGroup.Builder geoGroup = GeoGroup.builder();

                for (Geocache cache : caches) {
                    if (cache.applyDistanceRule()) {
                        geoGroup.addItems(
                                GeoPrimitive.createCircle(cache.getCoords(), radius, GeoStyle.builder()
                                        .setStrokeWidth(2.0f)
                                        .setStrokeColor(MapLineUtils.getCircleColor())
                                        .setFillColor(MapLineUtils.getCircleFillColor())
                                        .build()
                                ).buildUpon().setZLevel(LayerHelper.ZINDEX_CIRCLE).build());
                    }
                }
                layer.put(KEY_CACHE_CIRCLES, geoGroup.build());
            } else {
                layer.remove(KEY_CACHE_CIRCLES);
            }
        });

        viewModel.waypoints.observeForRead(activity, waypoints -> {

            if (Settings.isShowCircles()) {
                final GeoGroup.Builder geoGroup = GeoGroup.builder();

                for (Waypoint waypoint : waypoints) {
                    if (waypoint.applyDistanceRule()) {

                        geoGroup.addItems(
                                GeoPrimitive.createCircle(waypoint.getCoords(), radius, GeoStyle.builder()
                                        .setStrokeWidth(2.0f)
                                        .setStrokeColor(MapLineUtils.getCircleColor())
                                        .setFillColor(MapLineUtils.getCircleFillColor())
                                        .build()
                                ).buildUpon().setZLevel(LayerHelper.ZINDEX_CIRCLE).build());
                    }
                }
                layer.put(KEY_WAYPOINT_CIRCLES, geoGroup.build());
            } else {
                layer.remove(KEY_WAYPOINT_CIRCLES);
            }
        });

    }
}
