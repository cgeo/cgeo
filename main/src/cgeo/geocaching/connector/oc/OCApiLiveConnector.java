package cgeo.geocaching.connector.oc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CryptUtils;

public class OCApiLiveConnector extends OCApiConnector implements ISearchByCenter, ISearchByViewPort {

    private String cS;

    public OCApiLiveConnector(String name, String host, String prefix, int cKResId, int cSResId) {
        super(name, host, prefix, CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cKResId)));

        cS = CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cSResId));
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
        return new SearchResult(OkapiClient.getCachesBBox(viewport, this));
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {

        return new SearchResult(OkapiClient.getCachesAround(center, this));
    }

    public String getCS() {
        return CryptUtils.rot13(cS);
    }

}
