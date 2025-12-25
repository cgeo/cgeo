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

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.capability.WatchListCapability
import cgeo.geocaching.models.Geocache

import android.content.Context

import java.util.ArrayList
import java.util.List

class WatchListUtils {
    private WatchListUtils() {
        // utility class
    }

    public static Unit unwatchAll(final Context context, final List<Geocache> caches) {
        updateHandler(context, caches, false)
    }

    public static Unit watchAll(final Context context, final List<Geocache> caches) {
        updateHandler(context, caches, true)
    }

    private static Unit updateHandler(final Context context, final List<Geocache> caches, final Boolean actionIsWatch) {
        ActivityMixin.showToast(context, context.getString(R.string.watchlist_background_started))
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
            val failedCaches: List<String> = ArrayList<>()
            for (final Geocache cache : caches) {
                if (cache.supportsWatchList()) {
                    val connector: WatchListCapability = (WatchListCapability) ConnectorFactory.getConnector(cache)
                    if (!(actionIsWatch ? connector.addToWatchlist(cache) : connector.removeFromWatchlist(cache))) {
                        failedCaches.add(cache.getGeocode())
                    }
                }
            }
            ActivityMixin.showToast(context, failedCaches.isEmpty() ? context.getString(actionIsWatch ? R.string.cachedetails_progress_watch : R.string.cachedetails_progress_unwatch) : context.getString(R.string.err_watchlist_failed_geocodes, String.join(",", failedCaches)))
        })
    }

    public static Boolean anySupportsWatchlist(final List<Geocache> caches) {
        return caches.stream().anyMatch(Geocache::supportsWatchList)
    }

    public static Boolean anySupportsWatching(final List<Geocache> caches) {
        return caches.stream().anyMatch(cache -> !cache.isOnWatchlist())
    }

    public static Boolean anySupportsUnwatching(final List<Geocache> caches) {
        return caches.stream().anyMatch(Geocache::isOnWatchlist)
    }
}
