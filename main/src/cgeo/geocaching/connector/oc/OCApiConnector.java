package cgeo.geocaching.connector.oc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.CryptUtils;

public class OCApiConnector extends OCConnector implements ISearchByGeocode {

    private final String cK;

    public OCApiConnector(String name, String host, String prefix, String cK) {
        super(name, host, prefix);
        this.cK = cK;
    }

    public void addAuthentication(final Parameters params) {
        params.put(CryptUtils.rot13("pbafhzre_xrl"), CryptUtils.rot13(cK));
    }

    @Override
    public String getLicenseText(final cgCache cache) {
        // NOT TO BE TRANSLATED
        return "<a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a> data licensed under the Creative Commons BY-SA 3.0 License";
    }

    @Override
    public SearchResult searchByGeocode(final String geocode, final String guid, final CancellableHandler handler) {
        final cgCache cache = OkapiClient.getCache(geocode);
        if (cache == null) {
            return null;
        }
        return new SearchResult(cache);
    }

    @Override
    public boolean isActivated() {
        // currently always active, but only for details download
        return true;
    }
}
