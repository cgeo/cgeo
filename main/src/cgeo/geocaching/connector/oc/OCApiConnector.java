package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

public class OCApiConnector extends OCConnector implements ISearchByGeocode {

    // Levels of Okapi we support
    // oldapi is around rev 500
    // current is from rev 798 onwards
    public enum ApiSupport {
        oldapi,
        current
    }

    // Levels of OAuth-Authentication we support
    public enum OAuthLevel {
        Level1,
        Level3
    }

    private final String cK;
    private final ApiSupport apiSupport;

    public OCApiConnector(String name, String host, String prefix, String cK, ApiSupport apiSupport) {
        super(name, host, prefix);
        this.cK = cK;
        this.apiSupport = apiSupport;
    }

    public void addAuthentication(final Parameters params) {
        params.put(CryptUtils.rot13("pbafhzre_xrl"), CryptUtils.rot13(cK));
    }

    @Override
    public String getLicenseText(final Geocache cache) {
        // NOT TO BE TRANSLATED
        return "© " + cache.getOwnerDisplayName() + ", <a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a>, CC-BY-NC-ND, alle Logeinträge © jeweiliger Autor";
    }

    @Override
    public SearchResult searchByGeocode(final String geocode, final String guid, final CancellableHandler handler) {
        final Geocache cache = OkapiClient.getCache(geocode);
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

    @SuppressWarnings("static-method")
    public OAuthLevel getSupportedAuthLevel() {
        return OAuthLevel.Level1;
    }

    public String getCK() {
        return CryptUtils.rot13(cK);
    }

    @SuppressWarnings("static-method")
    public String getCS() {
        return StringUtils.EMPTY;
    }

    public ApiSupport getApiSupport() {
        return apiSupport;
    }

}
