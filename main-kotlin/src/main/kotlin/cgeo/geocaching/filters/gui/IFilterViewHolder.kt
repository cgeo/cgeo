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

package cgeo.geocaching.filters.gui

import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter

import android.app.Activity
import android.view.View

interface IFilterViewHolder<T : IGeocacheFilter()> {

    Unit init(GeocacheFilterType type, Activity activity)

    GeocacheFilterType getType()

    Activity getActivity()

    Unit setViewFromFilter(T filter)

    View getView()

    T createFilterFromView()

    Unit setAdvancedMode(Boolean isAdvanced)

    Boolean canBeSwitchedToBasicLossless()
}
