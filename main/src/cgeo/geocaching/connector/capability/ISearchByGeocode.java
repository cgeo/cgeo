package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.utils.CancellableHandler;

/**
 * connector capability of searching online for a cache by geocode
 * 
 */
public interface ISearchByGeocode {
    public SearchResult searchByGeocode(final String geocode, final String guid, final CancellableHandler handler);
}
