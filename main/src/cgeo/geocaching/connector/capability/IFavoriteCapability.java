package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

/**
 * Connector interface to implement possibility to give recommendations (aka Favorite Points) to
 * caches.
 * See {@link cgeo.geocaching.connector.ILoggingWithFavorites} for possibility to give favorite point
 * directly on logging screen.
 */
public interface IFavoriteCapability extends IConnector {

    /**
     * Add the cache to favorites
     *
     * @return True - success/False - failure
     */
    boolean addToFavorites(@NonNull Geocache cache);

    /**
     * Remove the cache from favorites
     *
     * @return True - success/False - failure
     */
    boolean removeFromFavorites(@NonNull Geocache cache);


    /**
     * Enable/disable favorite points controls in cache details
     */
    boolean supportsFavoritePoints(@NonNull Geocache cache);


    /**
     * Check whether to show favorite controls during logging for the given log type
     *
     * @param cache a cache that this connector must be able to handle
     * @param type  a log type selected by the user
     * @return true, when cache can be added to favorite
     */
    boolean supportsAddToFavorite(@NonNull Geocache cache, LogType type);

}
