package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.geopoint.Viewport;

public interface ISearchByViewPort extends IConnector {
    public SearchResult searchByViewport(final Viewport viewport, final String[] tokens);
}
