package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class GeofenceCirclesLayer {
    private static final String KEY_GEOFENCE = "geofence";

    public GeofenceCirclesLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.waypoints.observeForRead(activity, waypoints -> {

            if (Settings.isShowGeofences()) {
                final GeoGroup.Builder geoGroup = GeoGroup.builder();

                for (Waypoint waypoint : waypoints) {
                    final Float geofenceInMeters = waypoint.getGeofence();
                    if (geofenceInMeters != null && geofenceInMeters > 0f) {
                        geoGroup.addItems(
                                GeoPrimitive.createCircle(waypoint.getCoords(), geofenceInMeters / 1000f, GeoStyle.builder()
                                        .setStrokeWidth(2.0f)
                                        .setStrokeColor(0x80008000)
                                        .setFillColor(0x20008000)
                                        .build()
                                ).buildUpon().setZLevel(LayerHelper.ZINDEX_CIRCLE).build());
                    }
                }
                layer.put(KEY_GEOFENCE, geoGroup.build());
            } else {
                layer.remove(KEY_GEOFENCE);
            }
        });

    }
}
