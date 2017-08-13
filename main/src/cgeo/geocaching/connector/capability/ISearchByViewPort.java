package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.location.Viewport;

import android.support.annotation.NonNull;

import io.reactivex.Single;

public interface ISearchByViewPort extends IConnector {
    @NonNull
    Single<SearchResult> searchByViewport(@NonNull final Viewport viewport, @NonNull final MapTokens tokens);
}
