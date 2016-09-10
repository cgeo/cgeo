package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;

import android.support.annotation.NonNull;

/**
 * Connector capability of searching online for a cache by keyword.
 *
 */
public interface ISearchByKeyword extends IConnector {
    SearchResult searchByKeyword(@NonNull final String keyword);
}
