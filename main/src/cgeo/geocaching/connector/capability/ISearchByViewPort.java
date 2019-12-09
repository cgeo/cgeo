package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.location.Viewport;

import androidx.annotation.NonNull;

public interface ISearchByViewPort extends IConnector {
    @NonNull
    SearchResult searchByViewport(@NonNull Viewport viewport);
}
