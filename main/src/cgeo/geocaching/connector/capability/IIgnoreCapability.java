package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/**
 * Connector capability to ignore caches.
 */
public interface IIgnoreCapability extends IConnector {
    /**
     * Whether the given cache can be ignored. Typically the implementation
     * would return constant value regardless of the cache.
     */
    boolean canIgnoreCache(@NonNull Geocache cache);

    /**
     * Whether the given cache can be un-ignored. This is mostly for
     * geocaching.com service where ignored caches cannot be accessed
     * from c:geo at all, so ignoring is one-way action.
     * For modern connectors both should be supported
     */
    boolean canRemoveFromIgnoreCache(@NonNull Geocache cache);

    /**
     * Ignore cache
     *
     * @return True - success/False - failure
     */
    @WorkerThread
    boolean addToIgnorelist(@NonNull Geocache cache);

    /**
     * Remove the cache from ignored
     *
     * @return True - success/False - failure
     */
    @WorkerThread
    boolean removeFromIgnorelist(@NonNull Geocache cache);


}
