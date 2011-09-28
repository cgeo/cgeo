package cgeo.geocaching.connector.opencaching;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.utils.CryptUtils;

import java.util.List;
import java.util.UUID;

public class ApiOpenCachingConnector extends OpenCachingConnector implements IConnector {

    private final String cK;

    public ApiOpenCachingConnector(String name, String host, String prefix, String cK) {
        super(name, host, prefix);
        this.cK = cK;
    }

    public String getAuthentication(int level) {
        return CryptUtils.rot13("&pbafhzre_xrl=" + cK);
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
    public UUID searchByGeocode(final cgBase base, final String geocode, final String guid, final cgeoapplication app, final cgSearch search, final int reason) {
        final cgCache cache = OkapiClient.getCache(geocode);
        if (cache == null) {
            return null;
        }
        final cgCacheWrap caches = new cgCacheWrap();
        caches.cacheList.add(cache);

        final List<cgCache> cacheList = cgBase.processSearchResults(search, caches, 0, 0, null);
        app.addSearch(search, cacheList, true, reason);

        return search.getCurrentId();
    }
}
