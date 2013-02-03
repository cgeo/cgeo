package cgeo.geocaching.connector.ox;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.CancellableHandler;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * connector for OpenCaching.com
 *
 */
public class OXConnector extends AbstractConnector implements ISearchByCenter, ISearchByGeocode {

    private static final Pattern PATTERN_GEOCODE = Pattern.compile("OX[A-Z0-9]+", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandle(String geocode) {
        return PATTERN_GEOCODE.matcher(geocode).matches();
    }

    @Override
    public String getCacheUrl(Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    public String getName() {
        return "OpenCaching.com";
    }

    @Override
    public String getHost() {
        return "www.opencaching.com";
    }

    @Override
    public String getLicenseText(Geocache cache) {
        // NOT TO BE TRANSLATED
        return "<a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a> data licensed under the Creative Commons BY-SA 3.0 License";
    }

    @Override
    public boolean isOwner(final ICache cache) {
        return false;
    }

    @Override
    public SearchResult searchByGeocode(String geocode, String guid, CancellableHandler handler) {
        final Geocache cache = OpenCachingApi.searchByGeoCode(geocode);
        if (cache == null) {
            return null;
        }
        final SearchResult searchResult = new SearchResult(cache);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {
        Collection<Geocache> caches = OpenCachingApi.searchByCenter(center);
        if (caches == null) {
            return null;
        }
        return new SearchResult(caches);
    }

    @Override
    protected String getCacheUrlPrefix() {
        return "http://www.opencaching.com/#!geocache/";
    }
}
