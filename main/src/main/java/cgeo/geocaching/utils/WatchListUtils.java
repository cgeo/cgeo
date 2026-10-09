package cgeo.geocaching.utils;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.service.UnwatchCachesBatchService;
import cgeo.geocaching.service.WatchCachesBatchService;

import android.content.Context;

import java.util.List;

public final class WatchListUtils {
    private WatchListUtils() {
        // utility class
    }

    public static void unwatchAll(final Context context, final List<Geocache> caches) {
        UnwatchCachesBatchService.start(context, caches);
    }

    public static void watchAll(final Context context, final List<Geocache> caches) {
        WatchCachesBatchService.start(context, caches);
    }

    public static boolean anySupportsWatchlist(final List<Geocache> caches) {
        return caches.stream().anyMatch(Geocache::supportsWatchList);
    }

    public static boolean anySupportsWatching(final List<Geocache> caches) {
        return caches.stream().anyMatch(cache -> !cache.isOnWatchlist());
    }

    public static boolean anySupportsUnwatching(final List<Geocache> caches) {
        return caches.stream().anyMatch(Geocache::isOnWatchlist);
    }
}
