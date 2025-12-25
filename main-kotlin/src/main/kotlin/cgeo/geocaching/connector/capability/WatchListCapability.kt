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

import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

/**
 * Connector interface to implement for adding/removing caches to/from a watch list (which is hosted at the connectors
 * site).
 */
interface WatchListCapability : IConnector() {

    /**
     * Restrict the caches or circumstances when to add a cache to the watchlist.
     */
    Boolean canAddToWatchList(Geocache cache)

    /**
     * Add the cache to the watchlist
     *
     * @return True - success/False - failure
     */
    @WorkerThread
    Boolean addToWatchlist(Geocache cache)

    /**
     * Remove the cache from the watchlist
     *
     * @return True - success/False - failure
     */
    @WorkerThread
    Boolean removeFromWatchlist(Geocache cache)

}
