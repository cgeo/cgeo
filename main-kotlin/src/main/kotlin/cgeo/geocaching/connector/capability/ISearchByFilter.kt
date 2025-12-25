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

package cgeo.geocaching.connector.capability

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.sorting.GeocacheSort

import androidx.annotation.NonNull

import java.util.EnumSet

interface ISearchByFilter : IConnector() {

    default EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.noneOf(GeocacheFilterType.class)
    }

    SearchResult searchByFilter(GeocacheFilter filter, GeocacheSort sort)
}
