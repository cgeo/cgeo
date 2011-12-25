package cgeo.geocaching.connector.opencaching;

import cgeo.geocaching.Parameters;
import cgeo.geocaching.ParseResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.CryptUtils;

public class ApiOpenCachingConnector extends OpenCachingConnector {

    private final String cK;

    public ApiOpenCachingConnector(String name, String host, String prefix, String cK) {
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
    public boolean supportsRefreshCache(cgCache cache) {
        return true;
    }

    @Override
    public ParseResult searchByGeocode(final String geocode, final String guid, final cgeoapplication app, final int listId, final CancellableHandler handler) {
        final cgCache cache = OkapiClient.getCache(geocode);
        if (cache == null) {
            return null;
        }
        final ParseResult parseResult = new ParseResult();
        parseResult.cacheList.add(cache);

        final ParseResult search = ParseResult.filterParseResults(parseResult, false, false, Settings.getCacheType());
        app.addSearch(search.cacheList, listId);

        return search;
    }
}
