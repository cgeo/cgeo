package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Viewport;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/**
 * Connectors implementing this interface are able to amend one or more caches
 * (online) with additional information
 */
public interface ICacheAmendment extends IConnector {

    /**
     * amends the given caches with additional data specific to the implementing connector
     */
    @WorkerThread
    void amendCaches(@NonNull SearchResult searchResult);


    /** returns true if the cache data amended by the given connector would be relevant for the given filter */
    default boolean relevantForFilter(@NonNull GeocacheFilter filter) {
        return true;
    }

    /**
     * amends the given caches with additional data specific to the implementing connector.
     * The data was retrieved for the given viewport, this info should be used by the amender to
     * optimize amendment
     */
    @WorkerThread
    default void amendCachesForViewport(@NonNull SearchResult searchResult, @NonNull Viewport viewport) {
        amendCaches(searchResult);
    }
}
