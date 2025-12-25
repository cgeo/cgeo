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
import cgeo.geocaching.models.geoitem.GeoIcon
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.unifiedmap.LayerHelper
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer
import cgeo.geocaching.utils.ImageUtils

import android.graphics.Bitmap

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider

class CoordsIndicatorLayer {
    private static val KEY_COORDS_INDICATOR: String = "COORDS_INDICATOR"
    private static val KEY_LONG_TAP: String = "longtapmarker"

    private val marker: Bitmap = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.coords_indicator, null))
    private val markerLongTap: Bitmap = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.map_pin, null))

    public CoordsIndicatorLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        val viewModel: UnifiedMapViewModel = ViewModelProvider(activity).get(UnifiedMapViewModel.class)

        viewModel.coordsIndicator.observe(activity, geopoint -> {
            if (geopoint != null) {
                layer.put(KEY_COORDS_INDICATOR, GeoPrimitive.createMarker(geopoint, GeoIcon.builder().setBitmap(marker).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_SEARCHCENTER).build())
            } else {
                layer.remove(KEY_COORDS_INDICATOR)
            }
        })

        viewModel.longTapCoords.observe(activity, gp -> {
            if (gp == null) {
                layer.remove(KEY_LONG_TAP)
            } else {
                layer.put(KEY_LONG_TAP, GeoPrimitive.createMarker(gp, GeoIcon.builder()
                                .setYAnchor(markerLongTap.getHeight())
                                .setBitmap(markerLongTap).build())
                        .buildUpon().setZLevel(LayerHelper.ZINDEX_POSITION).build())
            }
        })
    }
}
