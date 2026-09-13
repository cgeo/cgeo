package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
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

import org.apache.commons.lang3.StringUtils;

public class PersonalNoteUploadService extends AbstractGeocacheBatchService {
    static {
        logTag = "PersonalNoteUploadService";
    }

    /** Enqueues caches that have a personal note and a capable connector for background upload. */
    public static void start(final Context context, final List<Geocache> caches) {
        final List<String> geocodes = caches.stream()
                .filter(cache -> ConnectorFactory.getConnector(cache) instanceof PersonalNoteCapability
                        && StringUtils.isNotBlank(cache.getPersonalNote()))
                .map(Geocache::getGeocode)
                .collect(Collectors.toList());
        addGeocodes(context, PersonalNoteUploadService.class, geocodes);
    }

    @Override
    protected boolean processGeocode(final String geocode) {
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache == null) {
            return false;
        }
        ((PersonalNoteCapability) ConnectorFactory.getConnector(cache)).uploadPersonalNote(cache);
        return true;
    }

    @Override
    protected int getTitle() {
        return R.string.export_persnotes;
    }

    @Override
    protected int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_UPLOAD_PERSNOTES;
    }

    @Override
    protected NotificationChannels getCompletionNotificationChannel() {
        return NotificationChannels.CACHES_DOWNLOADED_NOTIFICATION;
    }

    @Override
    @Nullable
    protected String getCompletedText(final int processedCount) {
        return LocalizationUtils.getPlural(R.plurals.export_persnotes_upload_success, processedCount, processedCount);
    }

    @Override
    @NonNull
    protected String getInterruptedText(final boolean wasCanceled, final int processedCount, final int totalCount) {
        return LocalizationUtils.getString(
                wasCanceled ? R.string.export_persnotes_background_canceled : R.string.export_persnotes_background_failed,
                processedCount, totalCount);
    }
}
