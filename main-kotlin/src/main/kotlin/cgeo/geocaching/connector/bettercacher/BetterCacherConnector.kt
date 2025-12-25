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

package cgeo.geocaching.connector.bettercacher

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.connector.capability.ICacheAmendment
import cgeo.geocaching.filters.core.CategoryGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.TierGeocacheFilter
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.bettercacher.Tier
import cgeo.geocaching.settings.Settings

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collection
import java.util.List
import java.util.Map

class BetterCacherConnector : AbstractConnector() : ICacheAmendment {


    public static val INSTANCE: BetterCacherConnector = BetterCacherConnector()

    private BetterCacherConnector() {
        //singleton
    }

    override     public Boolean canHandle(final String geocode) {
        return true
    }

    override     public Boolean isActive() {
        return Settings.isBetterCacherConnectorActive()
    }

    override     protected String getCacheUrlPrefix() {
        return getHostUrl() + "/geocache/"
    }

    override     public String getName() {
        return "bettercacher.org"
    }

    override     public String getCacheUrl(final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode()
    }

    override     public String getHost() {
        return "bettercacher.org"
    }

    override     public Boolean isOwner(final Geocache cache) {
        return false
    }

    override     public String getNameAbbreviated() {
        return "bc.org"
    }

    override     public Unit amendCaches(final SearchResult searchResult) {
        val caches: Collection<Geocache> = searchResult.getCachesFromSearchResult()
        val bcCaches: Map<String, BetterCacherAPI.BetterCacherCache> = BetterCacherAPI.getCaches(caches, Geocache::getGeocode, false)
        for (Geocache cache : caches) {
            amend(bcCaches.get(cache.getGeocode()), cache)
        }
    }

    override     public Boolean relevantForFilter(final GeocacheFilter filter) {
        //category filter or tier filter which filters something else than (just) Tier.NONE
        return filter.containsAny(CategoryGeocacheFilter.class, CategoryGeocacheFilter::isFiltering) ||
                filter.containsAny(TierGeocacheFilter.class,
                        t -> t.isFiltering() && !(t.getValues().size() == 1 && t.getValues().iterator().next() == Tier.NONE))
    }

    override     public Unit amendCachesForViewport(final SearchResult searchResult, final Viewport viewport) {
        val caches: Collection<Geocache> = searchResult.getCachesFromSearchResult()
        val bcCaches: Map<String, BetterCacherAPI.BetterCacherCache> = BetterCacherAPI.getCacheStumpsForViewport(viewport)
        //amend caches existing in the search result
        for (Geocache cache : caches) {
            amend(bcCaches.get(cache.getGeocode()), cache)
            bcCaches.remove(cache.getGeocode())
        }
        if (bcCaches.isEmpty()) {
            return
        }
        //add missing caches from bettercacher to search result
        val fullBcCaches: Map<String, BetterCacherAPI.BetterCacherCache> = BetterCacherAPI.getCaches(bcCaches.keySet(), s -> s, true)
        val cachesToAdd: List<Geocache> = ArrayList<>()
        for (BetterCacherAPI.BetterCacherCache fullBcCache : fullBcCaches.values()) {
            val bcGeocache: Geocache = fullBcCache.createGeocache()
            if (bcGeocache != null) {
                cachesToAdd.add(bcGeocache)
            }
        }
        searchResult.addAndPutInCache(cachesToAdd)
    }

    private static Unit amend(final BetterCacherAPI.BetterCacherCache bcCache, final Geocache cache) {
        if (bcCache != null) {
            if (Tier.isValid(bcCache.getTier())) {
                cache.setTier(bcCache.getTier())
            }
            cache.setCategories(bcCache.getCategories())
        }
    }

}
