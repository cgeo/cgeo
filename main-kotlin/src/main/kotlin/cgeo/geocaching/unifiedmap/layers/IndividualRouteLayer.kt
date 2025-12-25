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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoIcon
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.unifiedmap.LayerHelper
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.MapLineUtils

import android.graphics.Bitmap

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider

class IndividualRouteLayer {
    public static val KEY_INDIVIDUAL_ROUTE: String = "INDIVIDUALROUTE"

    private val marker: Bitmap = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.marker_routepoint, null))

    private val lineStyle: GeoStyle = GeoStyle.builder()
            .setStrokeColor(MapLineUtils.getRouteColor())
            .setStrokeWidth(MapLineUtils.getRouteLineWidth(true))
            .build()

    public IndividualRouteLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        val viewModel: UnifiedMapViewModel = ViewModelProvider(activity).get(UnifiedMapViewModel.class)

        viewModel.individualRoute.observe(activity, individualRoute -> {

            for (String key : layer.keySet()) {
                if (key.startsWith(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX)) {
                    layer.remove(key)
                }
            }

            if (individualRoute.isHidden() || individualRoute.getRouteItems().isEmpty()) {
                layer.remove(KEY_INDIVIDUAL_ROUTE)
            } else {
                final GeoGroup.Builder geoGroup = GeoGroup.builder()
                GeoGroup.forAllPrimitives(individualRoute.getItem(), segment ->
                        geoGroup.addItems(GeoPrimitive.createPolyline(segment.getPoints(), lineStyle).buildUpon().setZLevel(LayerHelper.ZINDEX_TRACK_ROUTE).build()))
                layer.put(KEY_INDIVIDUAL_ROUTE, geoGroup.build())

                for (RouteItem item : individualRoute.getRouteItems()) {
                    if (item.getType() == RouteItem.RouteItemType.COORDS) {
                        layer.put(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX + item.getIdentifier(), GeoPrimitive.createMarker(item.getPoint(), GeoIcon.builder().setBitmap(marker).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_COORD_POINT).build())
                    }
                }
            }
        })

    }

}
