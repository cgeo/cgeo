package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;

import android.support.annotation.NonNull;

/**
 * Connector capability to search online by owner name. Implement this in a {@link IConnector} to take part in the
 * global search by owner.
 *
 */
public interface ISearchByOwner extends IConnector {
    SearchResult searchByOwner(@NonNull final String owner);
}
