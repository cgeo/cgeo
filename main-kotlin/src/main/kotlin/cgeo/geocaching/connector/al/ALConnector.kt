// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.al

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.connector.capability.ISearchByFilter
import cgeo.geocaching.connector.capability.ISearchByGeocode
import cgeo.geocaching.connector.capability.ISearchByViewPort
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.sorting.GeocacheSort
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.IOException
import java.util.Collection
import java.util.EnumSet
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

class ALConnector : AbstractConnector() : ISearchByGeocode, ISearchByFilter, ISearchByViewPort {

    private static val CACHE_URL: String = "https://labs.geocaching.com/goto/"

    protected static val GEOCODE_PREFIX: String = "AL"

    /**
     * Pattern for AL codes
     */
    private static val PATTERN_AL_CODE: Pattern = Pattern.compile("AL[-\\w]+", Pattern.CASE_INSENSITIVE)

    private final String name

    private ALConnector() {
        // singleton
        name = LocalizationUtils.getString(R.string.settings_title_lc)
        prefKey = R.string.preference_screen_al
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static val INSTANCE: ALConnector = ALConnector()
    }

    public static ALConnector getInstance() {
        return Holder.INSTANCE
    }

    override     public Boolean canHandle(final String geocode) {
        return PATTERN_AL_CODE.matcher(geocode).matches()
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{"AL%"}
    }


    override     public String getCacheUrl(final Geocache cache) {
        val launcher: String = Settings.getALCLauncher()
        if (StringUtils.isEmpty(launcher)) {
            return CACHE_URL + cache.getCacheId()
        } else {
            return launcher + cache.getGeocode().substring(2)
        }
    }

    override     public String getName() {
        return name
    }

    override     public String getNameAbbreviated() {
        return GEOCODE_PREFIX
    }

    override     public String getHost() {
        return "labs.geocaching.com"
    }

    override     public String getExtraDescription() {
        return CgeoApplication.getInstance().getString(R.string.lc_default_description)
    }

    override     public Boolean supportsSettingFoundState() {
        return Settings.isALCfoundStateManual()
    }

    override     public Boolean supportsDifficultyTerrain() {
        return false
    }

    override     public SearchResult searchByGeocode(final String geocode, final String guid, final DisposableHandler handler) {
        if (geocode == null) {
            return null
        }
        Log.d("_AL searchByGeocode: geocode = " + geocode)
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage)
        val cache: Geocache = ALApi.searchByGeocode(geocode)
        return cache != null ? SearchResult(cache) : null
    }

    override     public SearchResult searchByViewport(final Viewport viewport) {
        return searchByViewport(viewport, null)
    }

    override     public SearchResult searchByViewport(final Viewport viewport, final GeocacheFilter filter) {
        try {
            val caches: Collection<Geocache> = ALApi.searchByFilter(filter, viewport, this, 100)
            SearchResult searchResult = SearchResult(caches)
            searchResult = searchResult.putInCacheAndLoadRating()
            searchResult.setPartialResult(this, caches.size() == 100)
            return searchResult
        } catch (IOException ioe) {
            Log.w("ALApi.searchByViewport: caught exception", ioe)
            return SearchResult(this, StatusCode.COMMUNICATION_ERROR)
        }
    }

    override     public EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.of(GeocacheFilterType.DISTANCE, GeocacheFilterType.ORIGIN)
    }

    override     public SearchResult searchByFilter(final GeocacheFilter filter, final GeocacheSort sort) {
        try {
            val caches: Collection<Geocache> = ALApi.searchByFilter(filter, null, this, 100)
            val result: SearchResult = SearchResult(caches)
            result.setPartialResult(this, caches.size() == 100)
            return result
        } catch (IOException ioe) {
            Log.w("ALApi.searchByFilter: caught exception", ioe)
            return SearchResult(this, StatusCode.COMMUNICATION_ERROR)
        }
    }


    override     public Boolean isOwner(final Geocache cache) {
        val user: String = Settings.getUserName()
        return StringUtils.isNotEmpty(user) && StringUtils.equalsIgnoreCase(cache.getOwnerDisplayName(), user)
    }

    override     protected String getCacheUrlPrefix() {
        return CACHE_URL
    }

    override     public Boolean isActive() {
        return Settings.isALConnectorActive() && Settings.isGCPremiumMember() && Settings.isGCConnectorActive()
    }

    override     public Int getCacheMapMarkerId() {
        return R.drawable.marker
    }

    override     public Int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_gc
    }

    override     public Int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker
    }

    override     public Int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_gc
    }

    override     public String getGeocodeFromUrl(final String url) {
        val geocode: String = "AL" + StringUtils.substringAfter(url, "https://adventurelab.page.link/")
        if (canHandle(geocode)) {
            return geocode
        }
        return super.getGeocodeFromUrl(url)
    }
}

