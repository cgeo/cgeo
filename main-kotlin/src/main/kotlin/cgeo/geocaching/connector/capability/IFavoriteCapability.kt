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
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull

/**
 * Connector interface to implement possibility to give recommendations (aka Favorite Points) to
 * caches.
 */
interface IFavoriteCapability : IConnector() {

    /**
     * Add the cache to favorites
     *
     * @return True - success/False - failure
     */
    Boolean addToFavorites(Geocache cache)

    /**
     * Remove the cache from favorites
     *
     * @return True - success/False - failure
     */
    Boolean removeFromFavorites(Geocache cache)


    /**
     * Enable/disable favorite points controls in cache details
     */
    Boolean supportsFavoritePoints(Geocache cache)


    /**
     * Check whether to show favorite controls during logging for the given log type
     *
     * @param cache a cache that this connector must be able to handle
     * @param type  a log type selected by the user
     * @return true, when cache can be added to favorite
     */
    Boolean supportsAddToFavorite(Geocache cache, LogType type)

}
