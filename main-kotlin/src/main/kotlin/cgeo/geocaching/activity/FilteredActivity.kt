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

package cgeo.geocaching.activity

import cgeo.geocaching.filters.core.GeocacheFilter


interface FilteredActivity {
    /**
     * called from the filter bar view
     */
    Unit showFilterMenu()

    Boolean showSavedFilterList()

    Unit refreshWithFilter(GeocacheFilter filter)
}
