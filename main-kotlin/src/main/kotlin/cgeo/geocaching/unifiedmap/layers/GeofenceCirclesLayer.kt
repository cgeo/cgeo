// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.unifiedmap.layers

import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.unifiedmap.LayerHelper
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer
import cgeo.geocaching.utils.MapLineUtils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class GeofenceCirclesLayer {
    private static val KEY_GEOFENCE: String = "geofence"

    public GeofenceCirclesLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        val viewModel: UnifiedMapViewModel = ViewModelProvider(activity).get(UnifiedMapViewModel.class)

        viewModel.waypoints.observeForRead(activity, waypoints -> {

            if (Settings.isShowCircles()) {
                final GeoGroup.Builder geoGroup = GeoGroup.builder()

                for (Waypoint waypoint : waypoints) {
                    val geofenceInMeters: Int = waypoint.getGeofence()
                    if (geofenceInMeters > 0) {
                        geoGroup.addItems(
                                GeoPrimitive.createCircle(waypoint.getCoords(), geofenceInMeters / 1000f, GeoStyle.builder()
                                        .setStrokeWidth(2.0f)
                                        .setStrokeColor(MapLineUtils.getGeofenceColor())
                                        .setFillColor(MapLineUtils.getGeofenceFillColor())
                                        .build()
                                ).buildUpon().setZLevel(LayerHelper.ZINDEX_CIRCLE).build())
                    }
                }
                layer.put(KEY_GEOFENCE, geoGroup.build())
            } else {
                layer.remove(KEY_GEOFENCE)
            }
        })

    }
}
