package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.sorting.GeocacheSort;

import androidx.annotation.NonNull;

import java.util.EnumSet;

public interface ISearchByFilter extends IConnector {

    @NonNull
    default EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.noneOf(GeocacheFilterType.class);
    }

    @NonNull
    SearchResult searchByFilter(@NonNull GeocacheFilter filter, @NonNull GeocacheSort sort);
}
