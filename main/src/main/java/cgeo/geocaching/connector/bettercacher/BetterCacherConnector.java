package cgeo.geocaching.connector.bettercacher;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ICacheAmendment;
import cgeo.geocaching.filters.core.CategoryGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.TierGeocacheFilter;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Tier;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BetterCacherConnector extends AbstractConnector implements ICacheAmendment {


    public static final BetterCacherConnector INSTANCE = new BetterCacherConnector();

    private BetterCacherConnector() {
        //singleton
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return true;
    }

    @Override
    public boolean isActive() {
        return Settings.isBetterCacherConnectorActive();
    }

    @NonNull
    @Override
    protected String getCacheUrlPrefix() {
        return getHostUrl() + "/geocache/";
    }

    @NonNull
    @Override
    public String getName() {
        return "bettercacher.org";
    }

    @Nullable
    @Override
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @NonNull
    @Override
    public String getHost() {
        return "bettercacher.org";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @NonNull
    @Override
    public String getNameAbbreviated() {
        return "bc.org";
    }

    @Override
    public void amendCaches(@NonNull final SearchResult searchResult) {
        final Collection<Geocache> caches = searchResult.getCachesFromSearchResult();
        final Map<String, BetterCacherAPI.BetterCacherCache> bcCaches = BetterCacherAPI.getCaches(caches, Geocache::getGeocode, false);
        for (Geocache cache : caches) {
            amend(bcCaches.get(cache.getGeocode()), cache);
        }
    }

    @Override
    public boolean relevantForFilter(@NonNull final GeocacheFilter filter) {
        //category filter or tier filter which filters something else than (just) Tier.NONE
        return filter.containsAny(CategoryGeocacheFilter.class, CategoryGeocacheFilter::isFiltering) ||
                filter.containsAny(TierGeocacheFilter.class,
                        t -> t.isFiltering() && !(t.getValues().size() == 1 && t.getValues().iterator().next() == Tier.NONE));
    }

    @Override
    public void amendCachesForViewport(@NonNull final SearchResult searchResult, @NonNull final Viewport viewport) {
        final Collection<Geocache> caches = searchResult.getCachesFromSearchResult();
        final Map<String, BetterCacherAPI.BetterCacherCache> bcCaches = BetterCacherAPI.getCacheStumpsForViewport(viewport);
        //amend caches existing in the search result
        for (Geocache cache : caches) {
            amend(bcCaches.get(cache.getGeocode()), cache);
            bcCaches.remove(cache.getGeocode());
        }
        if (bcCaches.isEmpty()) {
            return;
        }
        //add missing caches from bettercacher to search result
        final Map<String, BetterCacherAPI.BetterCacherCache> fullBcCaches = BetterCacherAPI.getCaches(bcCaches.keySet(), s -> s, true);
        final List<Geocache> cachesToAdd = new ArrayList<>();
        for (BetterCacherAPI.BetterCacherCache fullBcCache : fullBcCaches.values()) {
            final Geocache bcGeocache = fullBcCache.createGeocache();
            if (bcGeocache != null) {
                cachesToAdd.add(bcGeocache);
            }
        }
        searchResult.addAndPutInCache(cachesToAdd);
    }

    private static void amend(final BetterCacherAPI.BetterCacherCache bcCache, final Geocache cache) {
        if (bcCache != null) {
            if (Tier.isValid(bcCache.getTier())) {
                cache.setTier(bcCache.getTier());
            }
            cache.setCategories(bcCache.getCategories());
        }
    }

}
