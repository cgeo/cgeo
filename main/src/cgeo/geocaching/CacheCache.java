package cgeo.geocaching;

import cgeo.geocaching.cgData.StorageLocation;
import cgeo.geocaching.connector.gc.GCBase;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.LeastRecentlyUsedMap.RemoveHandler;

import org.apache.commons.lang3.StringUtils;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Cache for Caches. Every cache is stored in memory while c:geo is active to
 * speed up the app and to minimize network request - which are slow.
 *
 * @author blafoo
 */
public class CacheCache {

    private static final int MAX_CACHED_CACHES = 1000;
    final private LeastRecentlyUsedMap<String, cgCache> cachesCache;
    final private RemoveHandler<cgCache> removeHandler;

    private static CacheCache instance = null;

    private CacheCache() {
        cachesCache = new LeastRecentlyUsedMap.LruCache<String, cgCache>(MAX_CACHED_CACHES);
        removeHandler = new CacheRemoveHandler();
        cachesCache.setRemoveHandler(removeHandler);
    }

    public static CacheCache getInstance() {
        if (instance == null) {
            instance = new CacheCache();
        }
        return instance;
    }

    public void removeAllFromCache() {
        cachesCache.clear();
    }

    /**
     * @param geocode
     *            Geocode of the cache to remove from the cache
     */
    public void removeCacheFromCache(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            throw new IllegalArgumentException("geocode must not be empty");
        }
        cachesCache.remove(geocode);
    }

    /**
     * "Store" a cache in the CacheCache. If the cache is already in the CacheCache the cache gets replaced.
     *
     * @param cache
     *            Cache
     *
     */
    public void putCacheInCache(final cgCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("cache must not be null");
        }
        if (StringUtils.isBlank(cache.getGeocode())) {
            throw new IllegalArgumentException("geocode must not be empty");
        }
        cache.addStorageLocation(StorageLocation.CACHE);
        cachesCache.put(cache.getGeocode(), cache);
    }

    /**
     * @param geocode
     *            Geocode of the cache to retrieve from the cache
     * @return cache if found, null else
     */
    public cgCache getCacheFromCache(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            throw new IllegalArgumentException("geocode must not be empty");
        }
        return cachesCache.get(geocode);
    }

    public Set<String> getInViewport(final Long centerLat, final Long centerLon, final Long spanLat, final Long spanLon, final CacheType cacheType) {

        Set<String> geocodes = new HashSet<String>();

        for (cgCache cache : cachesCache.values()) {
            if (Viewport.isCacheInViewPort(centerLat.intValue(), centerLon.intValue(), spanLat.intValue(), spanLon.intValue(), cache.getCoords())) {
                if (CacheType.ALL == cacheType || cache.getType() == cacheType) {
                    geocodes.add(cache.getGeocode());
                }

            }
        }

        return geocodes;
    }

    @Override
    public String toString() {
        return StringUtils.join(cachesCache.keySet(), ' ');
    }

    private class CacheRemoveHandler implements RemoveHandler<cgCache> {

        @Override
        public void onRemove(cgCache toBeRemoved) {
            GCBase.removeFromTileCache(toBeRemoved.getCoords());
            Log.d(Settings.tag, "Removing " + toBeRemoved.getGeocode());
        }

    }

}
