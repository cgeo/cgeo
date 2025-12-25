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
 * Connector capability to ignore caches.
 */
interface IIgnoreCapability : IConnector() {
    /**
     * Whether the given cache can be ignored. Typically the implementation
     * would return constant value regardless of the cache.
     */
    Boolean canIgnoreCache(Geocache cache)

    /**
     * Whether the given cache can be un-ignored. This is mostly for
     * geocaching.com service where ignored caches cannot be accessed
     * from c:geo at all, so ignoring is one-way action.
     * For modern connectors both should be supported
     */
    Boolean canRemoveFromIgnoreCache(Geocache cache)

    /**
     * Ignore cache
     *
     * @return True - success/False - failure
     */
    @WorkerThread
    Boolean addToIgnorelist(Geocache cache)

    /**
     * Remove the cache from ignored
     *
     * @return True - success/False - failure
     */
    @WorkerThread
    Boolean removeFromIgnorelist(Geocache cache)


}
