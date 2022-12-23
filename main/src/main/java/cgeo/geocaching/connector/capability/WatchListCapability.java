package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/**
 * Connector interface to implement for adding/removing caches to/from a watch list (which is hosted at the connectors
 * site).
 */
public interface WatchListCapability extends IConnector {

    /**
     * Restrict the caches or circumstances when to add a cache to the watchlist.
     */
    boolean canAddToWatchList(@NonNull Geocache cache);

    /**
     * Add the cache to the watchlist
     *
     * @return True - success/False - failure
     */
    @WorkerThread
    boolean addToWatchlist(@NonNull Geocache cache);

    /**
     * Remove the cache from the watchlist
     *
     * @return True - success/False - failure
     */
    @WorkerThread
    boolean removeFromWatchlist(@NonNull Geocache cache);

}
