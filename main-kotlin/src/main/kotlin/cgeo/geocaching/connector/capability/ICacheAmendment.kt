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
import androidx.annotation.WorkerThread

/**
 * Connectors implementing this interface are able to amend one or more caches
 * (online) with additional information
 */
interface ICacheAmendment : IConnector() {

    /**
     * amends the given caches with additional data specific to the implementing connector
     */
    @WorkerThread
    Unit amendCaches(SearchResult searchResult)


    /** returns true if the cache data amended by the given connector would be relevant for the given filter */
    default Boolean relevantForFilter(GeocacheFilter filter) {
        return true
    }

    /**
     * amends the given caches with additional data specific to the implementing connector.
     * The data was retrieved for the given viewport, this info should be used by the amender to
     * optimize amendment
     */
    @WorkerThread
    default Unit amendCachesForViewport(SearchResult searchResult, Viewport viewport) {
        amendCaches(searchResult)
    }
}
