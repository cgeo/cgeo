package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.utils.DisposableHandler;

import android.support.annotation.Nullable;

/**
 * connector capability of searching online for a cache by geocode
 *
 */
public interface ISearchByGeocode extends IConnector {
    SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler);
}
