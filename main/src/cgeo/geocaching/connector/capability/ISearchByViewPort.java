package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.geopoint.Viewport;

import org.eclipse.jdt.annotation.NonNull;

public interface ISearchByViewPort extends IConnector {
    public SearchResult searchByViewport(final @NonNull Viewport viewport, final String[] tokens);
}
