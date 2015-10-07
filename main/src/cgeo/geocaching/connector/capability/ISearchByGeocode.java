package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.utils.CancellableHandler;

import org.eclipse.jdt.annotation.Nullable;

/**
 * connector capability of searching online for a cache by geocode
 *
 */
public interface ISearchByGeocode extends IConnector {
    public SearchResult searchByGeocode(final @Nullable String geocode, final @Nullable String guid, final CancellableHandler handler);
}
