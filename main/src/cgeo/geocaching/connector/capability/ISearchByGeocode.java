package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.utils.DisposableHandler;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * connector capability of searching online for a cache by geocode
 */
public interface ISearchByGeocode extends IConnector {
    @WorkerThread
    SearchResult searchByGeocode(@Nullable String geocode, @Nullable String guid, DisposableHandler handler);
}
