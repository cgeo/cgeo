package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.filters.core.GeocacheFilter;

import androidx.annotation.NonNull;

public interface ISearchByFilter extends IConnector {
    @NonNull
    SearchResult searchByFilter(@NonNull GeocacheFilter filter);
}
