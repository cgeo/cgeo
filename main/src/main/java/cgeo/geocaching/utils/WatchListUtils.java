package cgeo.geocaching.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.models.Geocache;

public class WatchListUtils {

    public static void unwatchAll(final Context context, final List<Geocache> caches) {
        updateHandler(context, caches, false);
    }

    public static void watchAll(final Context context, final List<Geocache> caches) {
        updateHandler(context, caches, true);
    }

    private static void updateHandler(final Context context, final List<Geocache> caches, final boolean actionIsWatch) {
        ActivityMixin.showToast(context, context.getString(R.string.watchlist_background_started));
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
            final List<String> failedCaches = new ArrayList<>();
            for (Geocache cache : caches) {
                if (cache.supportsWatchList()) {
                    final WatchListCapability connector = (WatchListCapability) ConnectorFactory.getConnector(cache);
                    boolean ret;
                    if (actionIsWatch) {
                        ret = connector.addToWatchlist(cache);
                    } else {
                        ret = connector.removeFromWatchlist(cache);
                    }
                    if (!ret) {
                        failedCaches.add(cache.getGeocode());
                    }
                }
            }
            ActivityMixin.showToast(context, failedCaches.isEmpty() ? context.getString(actionIsWatch ? R.string.cachedetails_progress_watch : R.string.cachedetails_progress_unwatch) : context.getString(R.string.err_watchlist_failed_geocodes, String.join(",", failedCaches)));
        });
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
