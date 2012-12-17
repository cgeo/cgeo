package cgeo.geocaching.connector.oc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CancellableHandler;

public class OCXMLApiConnector extends OCConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort {

    public OCXMLApiConnector(String name, String host, String prefix) {
        super(name, host, prefix);
    }

    @Override
    public SearchResult searchByGeocode(String geocode, String guid, CancellableHandler handler) {
        final cgCache cache = OCXMLClient.getCache(geocode);
        if (cache == null) {
            return null;
        }
        return new SearchResult(cache);
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {
        return new SearchResult(OCXMLClient.getCachesAround(center, 5.0));
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
        Geopoint center = viewport.getCenter();
        double distance = center.distanceTo(viewport.bottomLeft) * 1.15;
        return new SearchResult(OCXMLClient.getCachesAround(center, distance));
    }

}
