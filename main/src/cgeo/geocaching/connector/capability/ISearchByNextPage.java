package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;

/**
 * connector capability for online searching caches by the next page
 * Note: this capability is used solely by {@link cgeo.geocaching.connector.gc.GCConnector}
 *   and inside it solely for Pocket Queries + search by finder. It would be good to keep it this way!
 */
@Deprecated //do not use for new functionality. Is still used by GCConnector pocket queries and GCConnector searchByFinder
public interface ISearchByNextPage extends IConnector {
    @Deprecated
    SearchResult searchByNextPage(SearchResult search);
}
