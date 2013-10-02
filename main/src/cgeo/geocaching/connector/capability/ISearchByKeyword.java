package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;

/**
 * connector capability of searching online for a cache by name
 *
 */
public interface ISearchByKeyword extends IConnector {
    public SearchResult searchByName(final String name);
}
