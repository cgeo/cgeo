package cgeo.geocaching.connector.ox;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CancellableHandler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * connector for OpenCaching.com
 *
 */
public class OXConnector extends AbstractConnector implements ISearchByCenter, ISearchByGeocode, ISearchByViewPort, ISearchByKeyword {

    private static final Pattern PATTERN_GEOCODE = Pattern.compile("OX[A-Z0-9]+", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandle(@NonNull String geocode) {
        return PATTERN_GEOCODE.matcher(geocode).matches();
    }

    @Override
    public String getCacheUrl(@NonNull Geocache cache) {
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
    public String getLicenseText(@NonNull Geocache cache) {
        // NOT TO BE TRANSLATED
        return "<a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a> data licensed under the Creative Commons CC-BY-SA 3.0 License";
    }

    @Override
    public boolean isOwner(final Geocache cache) {
        return false;
    }

    @Override
    public SearchResult searchByGeocode(final @Nullable String geocode, final @Nullable String guid, final CancellableHandler handler) {
        if (geocode == null) {
            return null;
        }
        final Geocache cache = OpenCachingApi.searchByGeoCode(geocode);
        if (cache == null) {
            return null;
        }
        final SearchResult searchResult = new SearchResult(cache);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    public SearchResult searchByCenter(@NonNull Geopoint center, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return createSearchResult(OpenCachingApi.searchByCenter(center));
    }

    @Override
    protected String getCacheUrlPrefix() {
        return "http://www.opencaching.com/#!geocache/";
    }

    @Override
    public SearchResult searchByViewport(@NonNull Viewport viewport, final MapTokens tokens) {
        return createSearchResult(OpenCachingApi.searchByBoundingBox(viewport));
    }

    @Override
    public boolean isActive() {
        return Settings.isOXConnectorActive();
    }

    @Override
    public SearchResult searchByKeyword(final @NonNull String name, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return createSearchResult(OpenCachingApi.searchByKeyword(name));
    }

    private static SearchResult createSearchResult(Collection<Geocache> caches) {
        if (caches == null) {
            return null;
        }
        return new SearchResult(caches);
    }
}
