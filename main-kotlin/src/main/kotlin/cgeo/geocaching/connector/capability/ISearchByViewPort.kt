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
import cgeo.geocaching.location.Viewport

import androidx.annotation.NonNull
import androidx.annotation.Nullable

interface ISearchByViewPort : IConnector() {
    SearchResult searchByViewport(Viewport viewport)

    default SearchResult searchByViewport(Viewport viewport, GeocacheFilter filter) {
        return searchByViewport(viewport)
    }

}
