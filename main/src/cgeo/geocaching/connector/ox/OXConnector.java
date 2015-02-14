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

import org.apache.commons.lang3.StringUtils;
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
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_GEOCODE.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getName() {
        return "OpenCaching.com";
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.opencaching.com";
    }

    @Override
    @NonNull
    public String getLicenseText(@NonNull final Geocache cache) {
        // NOT TO BE TRANSLATED
        return "<a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a> data licensed under the Creative Commons CC-BY-SA 3.0 License";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
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
    public SearchResult searchByCenter(@NonNull final Geopoint center, final @NonNull RecaptchaReceiver recaptchaReceiver) {
        return createSearchResult(OpenCachingApi.searchByCenter(center));
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return "http://www.opencaching.com/#!geocache/";
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport, @NonNull final MapTokens tokens) {
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

    private static SearchResult createSearchResult(final Collection<Geocache> caches) {
        if (caches == null) {
            return null;
        }
        return new SearchResult(caches);
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String geocode = StringUtils.substringAfter(url, "http://www.opencaching.com/de/#!geocache/");
        if (canHandle(geocode)) {
            return geocode;
        }
        return super.getGeocodeFromUrl(url);
    }
}
