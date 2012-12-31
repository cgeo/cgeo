package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.geopoint.Geopoint;

/**
 * connector capability for online searching caches around a center coordinate, sorted by distance
 *
 */
public interface ISearchByCenter {
    public SearchResult searchByCenter(final Geopoint center);

    public boolean isActivated();
}
