package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.geopoint.Viewport;

public interface ISearchByViewPort {
    public SearchResult searchByViewport(final Viewport viewport, final String[] tokens);
}
