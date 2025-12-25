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

import cgeo.geocaching.maps.Tracks
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer

import android.graphics.Color

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class TracksLayer {

    public static val TRACK_KEY_PREFIX: String = "TRACK-"

    final UnifiedMapViewModel viewModel

    public TracksLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        viewModel = ViewModelProvider(activity).get(UnifiedMapViewModel.class)

        viewModel.trackUpdater.observe(activity, event -> event.ifNotHandled((key -> {
            final Tracks.Track track = viewModel.getTracks().getTrack(key)
            if (track == null || track.getRoute() == null || track.getRoute().isHidden()) {
                layer.remove(TRACK_KEY_PREFIX + key)
            } else {

                //Apply current chosen default color to all elements and display

                val widthFactor: Float = 2f
                val defaultWidth: Float = track.getTrackfile().getWidth() / widthFactor
                val defaultStrokeColor: Int = track.getTrackfile().getColor()
                val defaultFillColor: Int = Color.argb(32, Color.red(defaultStrokeColor), Color.green(defaultStrokeColor), Color.blue(defaultStrokeColor))
                val defaultStyle: GeoStyle = GeoStyle.builder()
                        .setFillColor(defaultFillColor)
                        .setStrokeColor(defaultStrokeColor)
                        .setStrokeWidth(defaultWidth).build()

                layer.put(TRACK_KEY_PREFIX + key, track.getRoute().getItem().applyDefaultStyle(defaultStyle))
             }
        })))

    }

}
