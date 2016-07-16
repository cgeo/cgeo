package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.location.Viewport;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface ISearchByViewPort extends IConnector {
    @NonNull
    SearchResult searchByViewport(@NonNull final Viewport viewport, @Nullable final MapTokens tokens);
}
