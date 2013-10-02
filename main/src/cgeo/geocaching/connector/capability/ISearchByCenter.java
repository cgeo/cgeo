package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.geopoint.Geopoint;

/**
 * connector capability for online searching caches around a center coordinate, sorted by distance
 *
 */
public interface ISearchByCenter extends IConnector {
    public SearchResult searchByCenter(final Geopoint center);
}
