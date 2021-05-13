package cgeo.geocaching.connector.lc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class LCConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort {

    @NonNull
    private static final String CACHE_URL = "https://adventurelab.page.link/";

    @NonNull
    protected static final String GEOCODE_PREFIX = "LC";

    /**
     * Pattern for LC codes
     */
    @NonNull
    private static final Pattern PATTERN_LC_CODE = Pattern.compile("LC[-\\w]+", Pattern.CASE_INSENSITIVE);

    private final String name;

    private LCConnector() {
        // singleton
        name = CgeoApplication.getInstance().getString(R.string.settings_title_lc);
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final LCConnector INSTANCE = new LCConnector();
    }

    @NonNull
    public static LCConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_LC_CODE.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        final String launcher = Settings.getALCLauncher();
        if (launcher.equals(null) || launcher.equals("")) {
            return CACHE_URL + cache.getCacheId();
        } else {
            return launcher + cache.getGeocode().substring(2);
        }
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return GEOCODE_PREFIX;
    }

    @Override
    @NonNull
    public String getHost() {
        return "labs.geocaching.com";
    }

    @Override
    public String getExtraDescription() {
        return CgeoApplication.getInstance().getString(R.string.lc_default_description);
    }

    @Override
    public boolean supportsSettingFoundState() {
        return true;
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler) {
        if (geocode == null) {
            return null;
        }
        Log.d("_LC searchByGeocode: geocode = " + geocode);
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);
        final Geocache cache = LCApi.searchByGeocode(geocode);
        return cache != null ? new SearchResult(cache) : null;
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport) {
        final Collection<Geocache> caches = LCApi.searchByBBox(viewport);
        final SearchResult searchResult = new SearchResult(caches);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    @NonNull
    public SearchResult searchByCenter(@NonNull final Geopoint center) {
        final Collection<Geocache> caches = LCApi.searchByCenter(center);
        final SearchResult searchResult = new SearchResult(caches);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return CACHE_URL;
    }

    @Override
    public boolean isActive() {
        Log.d("_LC isActive: " + Settings.isLCConnectorActive());
        return Settings.isLCConnectorActive();
    }

    @Override
    public int getCacheMapMarkerId(final boolean disabled) {
        return disabled ? R.drawable.marker_disabled : R.drawable.marker;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String geocode = "LC" + StringUtils.substringAfter(url, "https://adventurelab.page.link/");
        if (canHandle(geocode)) {
            return geocode;
        }
        return super.getGeocodeFromUrl(url);
    }
}

