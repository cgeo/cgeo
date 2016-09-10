package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;

/**
 * connector capability for online searching caches by the next page
 *
 */
public interface ISearchByNextPage extends IConnector {
    SearchResult searchByNextPage(final SearchResult search);
}
