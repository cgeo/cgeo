package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Viewport;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ISearchByViewPort extends IConnector {
    @NonNull
    SearchResult searchByViewport(@NonNull Viewport viewport);

    @NonNull
    default SearchResult searchByViewport(@NonNull Viewport viewport, @Nullable GeocacheFilter filter) {
        return searchByViewport(viewport);
    }

}
