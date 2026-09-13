package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
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

public class UploadCoordinatesBatchService extends AbstractGeocacheBatchService {
    static {
        logTag = "UploadCoordinatesBatchService";
    }

    /**
     * Enqueues caches for background coordinate upload.
     *
     * @param modifiedOnly if true, only caches with user-modified coordinates are uploaded
     */
    public static void start(final Context context, final List<Geocache> caches, final boolean modifiedOnly) {
        final List<String> geocodes = caches.stream()
                .filter(cache -> (!modifiedOnly || cache.hasUserModifiedCoords())
                        && ConnectorFactory.getConnector(cache).supportsOwnCoordinates())
                .map(Geocache::getGeocode)
                .collect(Collectors.toList());
        addGeocodes(context, UploadCoordinatesBatchService.class, geocodes);
    }

    @Override
    protected boolean processGeocode(final String geocode) {
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache == null) {
            return false;
        }
        final IConnector connector = ConnectorFactory.getConnector(cache);
        return connector.uploadModifiedCoordinates(cache, cache.getCoords());
    }

    @Override
    protected int getTitle() {
        return R.string.export_modifiedcoords;
    }

    @Override
    protected int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_UPLOAD_COORDS;
    }

    @Override
    protected NotificationChannels getCompletionNotificationChannel() {
        return NotificationChannels.CACHES_DOWNLOADED_NOTIFICATION;
    }

    @Override
    @Nullable
    protected String getCompletedText(final int processedCount) {
        return LocalizationUtils.getPlural(R.plurals.export_modifiedcoords_background_result, processedCount, processedCount);
    }

    @Override
    @NonNull
    protected String getInterruptedText(final boolean wasCanceled, final int processedCount, final int totalCount) {
        return LocalizationUtils.getString(
                wasCanceled ? R.string.export_modifiedcoords_background_canceled : R.string.export_modifiedcoords_background_failed,
                processedCount, totalCount);
    }
}
