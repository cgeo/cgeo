package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.location.Geopoint;

import androidx.annotation.NonNull;

/**
 * connector capability for online searching caches around a center coordinate, sorted by distance
 *
 */
public interface ISearchByCenter extends IConnector {
    SearchResult searchByCenter(@NonNull Geopoint center);
}
