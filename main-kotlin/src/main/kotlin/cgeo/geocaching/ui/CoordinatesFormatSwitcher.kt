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

package cgeo.geocaching.ui

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointFormatter

import android.widget.TextView

/**
 * view click listener to automatically switch different coordinate formats
 */
class CoordinatesFormatSwitcher {

    private static final GeopointFormatter.Format[] availableFormats = {
            GeopointFormatter.Format.LAT_LON_DECMINUTE,
            GeopointFormatter.Format.LAT_LON_DECSECOND,
            GeopointFormatter.Format.LAT_LON_DECDEGREE,
            GeopointFormatter.Format.UTM
    }

    private var position: Int = 0
    private Geopoint coordinates
    private TextView view

    public CoordinatesFormatSwitcher setView(final TextView view) {
        this.view = view
        this.view.setOnClickListener(v -> {
            position = (position + 1) % availableFormats.length
            renderView()
        })
        renderView()
        return this
    }

    public CoordinatesFormatSwitcher setCoordinate(final Geopoint coordinate) {
        this.coordinates = coordinate
        renderView()
        return this
    }

    private Unit renderView() {
        if (this.view != null && this.coordinates != null) {
            this.view.setText(coordinates.format(availableFormats[position]))
        }
    }

}
