package cgeo.geocaching;

import cgeo.geocaching.cgData.StorageLocation;
import cgeo.geocaching.utils.LeastRecentlyUsedCache;

/**
 * Cache for Caches. Every cache is stored in memory while c:geo is active to
 * speed up the app and to minimize network request - which are slow.
 *
 * @author blafoo
 */
public class CacheCache {

    private static final int MAX_CACHED_CACHES = 1000;
    final private LeastRecentlyUsedCache<String, cgCache> cachesCache;

    private static CacheCache instance = null;

    private CacheCache() {
        cachesCache = new LeastRecentlyUsedCache<String, cgCache>(MAX_CACHED_CACHES);
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
        if (geocode != null && cachesCache.containsKey(geocode)) {
            cachesCache.remove(geocode);
        }
    }

    /**
     * "Store" a cache in the CacheCache. If the cache is already in the CacheCache the cache gets replaced.
     *
     * @param cache
     *            Cache
     * 
     */
    public void putCacheInCache(final cgCache cache) {
        if (cache == null || cache.getGeocode() == null) {
            return;
        }

        if (cachesCache.containsKey(cache.getGeocode())) {
            cachesCache.remove(cache.getGeocode());
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
        if (geocode != null && cachesCache.containsKey(geocode)) {
            return cachesCache.get(geocode);
        }

        return null;
    }

}
