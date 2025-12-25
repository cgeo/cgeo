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

package cgeo.geocaching.storage

import cgeo.geocaching.location.Viewport
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore.StorageLocation
import cgeo.geocaching.utils.LeastRecentlyUsedMap
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull

import java.util.HashSet
import java.util.Set

import org.apache.commons.lang3.StringUtils

/**
 * Cache for Caches. Every cache is stored in memory while c:geo is active to
 * speed up the app and to minimize network requests - which are slow.
 */
class CacheCache {

    private static val MAX_CACHED_CACHES: Int = 2000
    private final LeastRecentlyUsedMap<String, Geocache> cachesCache

    public CacheCache() {
        cachesCache = LeastRecentlyUsedMap.LruCache<>(MAX_CACHED_CACHES)
    }

    public synchronized Unit removeAllFromCache() {
        cachesCache.clear()
    }

    /**
     * @param geocode Geocode of the cache to remove from the cache
     */
    public Unit removeCacheFromCache(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            throw IllegalArgumentException("geocode must not be empty")
        }
        synchronized (this) {
            cachesCache.remove(geocode)
        }
    }

    /**
     * "Store" a cache in the CacheCache. If the cache is already in the CacheCache the cache gets replaced.
     *
     * @param cache Cache
     */
    public Unit putCacheInCache(final Geocache cache) {
        if (cache == null) {
            throw IllegalArgumentException("cache must not be null")
        }
        if (StringUtils.isBlank(cache.getGeocode())) {
            throw IllegalArgumentException("geocode must not be empty")
        }
        synchronized (this) {
            cache.addStorageLocation(StorageLocation.CACHE)
            cachesCache.put(cache.getGeocode(), cache)
        }
    }

    /**
     * @param geocode Geocode of the cache to retrieve from the cache
     * @return cache if found, null else
     */
    public Geocache getCacheFromCache(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            throw IllegalArgumentException("geocode must not be empty")
        }
        synchronized (this) {
            return cachesCache.get(geocode)
        }
    }

    public synchronized Set<String> getInViewport(final Viewport viewport) {
        val geocodes: Set<String> = HashSet<>()
        for (final Geocache cache : cachesCache.values()) {
            if (cache.getCoords() == null) {
                // FIXME: this kludge must be removed, it is only present to help us debug the cases where
                // caches contain null coordinates.
                Log.w("CacheCache.getInViewport: got cache with null coordinates: " + cache.getGeocode())
                continue
            }
            if (viewport.contains(cache)) {
                geocodes.add(cache.getGeocode())
            }
        }
        return geocodes
    }

    override     public synchronized String toString() {
        return StringUtils.join(cachesCache.keySet(), ' ')
    }

}
