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

package cgeo.geocaching

import cgeo.geocaching.databinding.WaypointItemBinding
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.ui.AbstractViewHolder
import cgeo.geocaching.ui.CoordinatesFormatSwitcher

import android.view.View

class WaypointViewHolder : AbstractViewHolder() {
    protected final WaypointItemBinding binding
    private final CoordinatesFormatSwitcher coordinateFormatSwitcher

    public WaypointViewHolder(final View rowView) {
        super(rowView)
        binding = WaypointItemBinding.bind(rowView)
        coordinateFormatSwitcher = CoordinatesFormatSwitcher().setView(binding.coordinates)
    }

    public Unit setCoordinate(final Geopoint coordinate) {
        this.coordinateFormatSwitcher.setCoordinate(coordinate)
    }

}
