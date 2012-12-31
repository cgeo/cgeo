package cgeo.geocaching.connector.oc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CancellableHandler;

public class OCXMLApiConnector extends OCConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort {

    private final static double SEARCH_DISTANCE_LIMIT = 15.0;
    private final static double NEARBY_SEARCH_DISTANCE = 5.0;

    public OCXMLApiConnector(String name, String host, String prefix) {
        super(name, host, prefix);
    }

    @Override
    public SearchResult searchByGeocode(final String geocode, final String guid, CancellableHandler handler) {
        final cgCache cache = OCXMLClient.getCache(geocode);
        if (cache == null) {
            return null;
        }
        return new SearchResult(cache);
    }

    @Override
    public SearchResult searchByCenter(final Geopoint center) {
        return new SearchResult(OCXMLClient.getCachesAround(center, NEARBY_SEARCH_DISTANCE));
    }

    @Override
    public SearchResult searchByViewport(final Viewport viewport, final String[] tokens) {
        final Geopoint center = viewport.getCenter();
        double distance = center.distanceTo(viewport.bottomLeft) * 1.15;
        if (distance > SEARCH_DISTANCE_LIMIT) {
            distance = SEARCH_DISTANCE_LIMIT;
        }
        return new SearchResult(OCXMLClient.getCachesAround(center, distance));
    }

    @Override
    public boolean isActivated() {
        // currently only tested and working with oc.de
        return Settings.isOCConnectorActive();
    }

}
