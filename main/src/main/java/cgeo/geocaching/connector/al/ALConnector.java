package cgeo.geocaching.connector.al;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByFilter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.GeocacheSort;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ALConnector extends AbstractConnector implements ISearchByGeocode, ISearchByFilter, ISearchByViewPort {

    @NonNull
    private static final String CACHE_URL = "https://adventurelab.page.link/";

    @NonNull
    protected static final String GEOCODE_PREFIX = "AL";

    /**
     * Pattern for AL codes
     */
    @NonNull
    private static final Pattern PATTERN_AL_CODE = Pattern.compile("AL[-\\w]+", Pattern.CASE_INSENSITIVE);

    private final String name;

    private ALConnector() {
        // singleton
        name = LocalizationUtils.getString(R.string.settings_title_lc);
        prefKey = R.string.preference_screen_al;
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final ALConnector INSTANCE = new ALConnector();
    }

    @NonNull
    public static ALConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_AL_CODE.matcher(geocode).matches();
    }

    @NotNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return new String[]{"AL%"};
    }


    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        final String launcher = Settings.getALCLauncher();
        if (StringUtils.isEmpty(launcher)) {
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
        return Settings.isALCfoundStateManual();
    }

    @Override
    public boolean supportsDifficultyTerrain() {
        return false;
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler) {
        if (geocode == null) {
            return null;
        }
        Log.d("_AL searchByGeocode: geocode = " + geocode);
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);
        final Geocache cache = ALApi.searchByGeocode(geocode);
        return cache != null ? new SearchResult(cache) : null;
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport) {
        return searchByViewport(viewport, null);
    }

    @NonNull
    @Override
    public SearchResult searchByViewport(@NonNull final Viewport viewport, @Nullable final GeocacheFilter filter) {
        try {
            final Collection<Geocache> caches = ALApi.searchByFilter(filter, viewport, this, 100);
            SearchResult searchResult = new SearchResult(caches);
            searchResult = searchResult.putInCacheAndLoadRating();
            searchResult.setPartialResult(this, caches.size() == 100);
            return searchResult;
        } catch (IOException ioe) {
            Log.w("ALApi.searchByViewport: caught exception", ioe);
            return new SearchResult(this, StatusCode.COMMUNICATION_ERROR);
        }
    }

    @NonNull
    @Override
    public EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.of(GeocacheFilterType.DISTANCE, GeocacheFilterType.ORIGIN);
    }

    @NonNull
    @Override
    public SearchResult searchByFilter(@NonNull final GeocacheFilter filter, @NonNull final GeocacheSort sort) {
        try {
            final Collection<Geocache> caches = ALApi.searchByFilter(filter, null, this, 100);
            final SearchResult result = new SearchResult(caches);
            result.setPartialResult(this, caches.size() == 100);
            return result;
        } catch (IOException ioe) {
            Log.w("ALApi.searchByFilter: caught exception", ioe);
            return new SearchResult(this, StatusCode.COMMUNICATION_ERROR);
        }
    }


    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        final String user = Settings.getUserName();
        return StringUtils.isNotEmpty(user) && StringUtils.equalsIgnoreCase(cache.getOwnerDisplayName(), user);
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return CACHE_URL;
    }

    @Override
    public boolean isActive() {
        return Settings.isALConnectorActive() && Settings.isGCPremiumMember() && Settings.isGCConnectorActive();
    }

    @Override
    public int getCacheMapMarkerId() {
        return R.drawable.marker;
    }

    @Override
    public int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_gc;
    }

    @Override
    public int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker;
    }

    @Override
    public int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_gc;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String geocode = "AL" + StringUtils.substringAfter(url, "https://adventurelab.page.link/");
        if (canHandle(geocode)) {
            return geocode;
        }
        return super.getGeocodeFromUrl(url);
    }
}

