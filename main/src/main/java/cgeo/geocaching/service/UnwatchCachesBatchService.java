package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class UnwatchCachesBatchService extends AbstractGeocacheBatchService {
    static {
        logTag = "UnwatchCachesBatchService";
    }

    /** Enqueues all watchlist-capable caches for background unwatching. */
    public static void start(final Context context, final List<Geocache> caches) {
        final List<String> geocodes = caches.stream()
                .filter(Geocache::supportsWatchList)
                .map(Geocache::getGeocode)
                .collect(Collectors.toList());
        addGeocodes(context, UnwatchCachesBatchService.class, geocodes);
    }

    @Override
    protected boolean processGeocode(final String geocode) {
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache == null) {
            return false;
        }
        return ((WatchListCapability) ConnectorFactory.getConnector(cache)).removeFromWatchlist(cache);
    }

    @Override
    protected int getTitle() {
        return R.string.watchlist_remove_background_title;
    }

    @Override
    protected int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_UNWATCH_CACHES;
    }

    @Override
    protected NotificationChannels getCompletionNotificationChannel() {
        return NotificationChannels.CACHES_DOWNLOADED_NOTIFICATION;
    }

    @Override
    @Nullable
    protected String getCompletedText(final int processedCount) {
        return LocalizationUtils.getPlural(R.plurals.watchlist_remove_background_result, processedCount, processedCount);
    }

    @Override
    @NonNull
    protected String getInterruptedText(final boolean wasCanceled, final int processedCount, final int totalCount) {
        return LocalizationUtils.getString(
                wasCanceled ? R.string.watchlist_remove_background_canceled : R.string.watchlist_remove_background_failed,
                processedCount, totalCount);
    }
}
