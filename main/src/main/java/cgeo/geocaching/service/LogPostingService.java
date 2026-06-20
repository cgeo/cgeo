package cgeo.geocaching.service;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogUtils;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogPostingService extends AbstractForegroundIntentService {

    static {
        logTag = "LogPostingService";
    }

    private static final String EXTRA_GEOCODE = "extra_geocode";

    static final ConcurrentHashMap<String, PostingJob> PENDING_JOBS = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // PostingJob inner class
    // -----------------------------------------------------------------------

    public static final class PostingJob {
        final Geocache cache;
        final boolean isEdit;

        // CREATE fields
        final OfflineLogEntry logEntry;
        final Map<String, Trackable> inventory;

        // EDIT fields
        final LogEntry oldEntry;
        final LogEntry newEntry;

        private PostingJob(final Geocache cache, final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
            this.cache = cache;
            this.isEdit = false;
            this.logEntry = logEntry;
            this.inventory = inventory;
            this.oldEntry = null;
            this.newEntry = null;
        }

        private PostingJob(final Geocache cache, final LogEntry oldEntry, final LogEntry newEntry) {
            this.cache = cache;
            this.isEdit = true;
            this.logEntry = null;
            this.inventory = null;
            this.oldEntry = oldEntry;
            this.newEntry = newEntry;
        }
    }

    // -----------------------------------------------------------------------
    // Static factory methods
    // -----------------------------------------------------------------------

    public static void startCreate(final Context context, final Geocache cache,
            final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
        final String geocode = cache.getGeocode();
        PENDING_JOBS.put(geocode, new PostingJob(cache, logEntry, inventory));
        final Intent intent = new Intent(context, LogPostingService.class);
        intent.putExtra(EXTRA_GEOCODE, geocode);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void startEdit(final Context context, final Geocache cache,
            final LogEntry oldEntry, final LogEntry newEntry) {
        final String geocode = cache.getGeocode();
        PENDING_JOBS.put(geocode, new PostingJob(cache, oldEntry, newEntry));
        final Intent intent = new Intent(context, LogPostingService.class);
        intent.putExtra(EXTRA_GEOCODE, geocode);
        ContextCompat.startForegroundService(context, intent);
    }

    // -----------------------------------------------------------------------
    // AbstractForegroundIntentService implementation
    // -----------------------------------------------------------------------

    @Override
    protected NotificationCompat.Builder createInitialNotification() {
        return Notifications.createNotification(this, NotificationChannels.LOG_RESULT_NOTIFICATION, R.string.log_posting_log)
                .setProgress(0, 0, true)
                .setOngoing(true)
                .setSilent(true);
    }

    @Override
    protected int getForegroundNotificationId() {
        return Notifications.ID_LOG_CREATE_NOTIFICATION;
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        if (intent == null) {
            return;
        }

        final String geocode = intent.getStringExtra(EXTRA_GEOCODE);
        if (geocode == null) {
            return;
        }

        final PostingJob job = PENDING_JOBS.remove(geocode);
        if (job == null) {
            return;
        }

        // Progress consumer updates the foreground notification
        final java.util.function.Consumer<String> progressConsumer = msg -> {
            notification.setContentText(msg);
            updateForegroundNotification();
        };

        try {
            final LogResult result;
            if (job.isEdit) {
                result = LogUtils.editLogTaskLogic(job.cache, job.oldEntry, job.newEntry, progressConsumer);
            } else {
                result = LogUtils.createLogTaskLogic(job.cache, job.logEntry, job.inventory, progressConsumer);
            }

            if (result.isOk()) {
                ViewUtils.showToast(this, LocalizationUtils.getString(R.string.info_log_post_success, geocode));
                // offline log is created on exiting the log activity, so we need to clear it here in case of success
                final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                cache.clearOfflineLog(null);
                cache.notifyChange();
            } else {
                final LogEntry entryToEdit = job.isEdit ? job.oldEntry : null;
                showErrorNotification(geocode, result.getErrorString(), entryToEdit);
            }
        } catch (final Exception e) {
            final LogEntry entryToEdit = job.isEdit ? job.oldEntry : null;
            final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            showErrorNotification(geocode, msg, entryToEdit);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void showErrorNotification(final String geocode, final String errorMessage, @Nullable final LogEntry entryToEdit) {
        final String title = LocalizationUtils.getString(R.string.info_log_post_failed_geocode, geocode);
        ViewUtils.showToast(this, title);
        // Use a dedicated ID separate from the foreground notification ID so the error
        // notification persists after the service (and its foreground notification) stops.
        final PendingIntent retryIntent = buildRetryIntent(geocode, entryToEdit);
        Notifications.send(this, Notifications.ID_LOG_POST_ERROR_NOTIFICATION,
                Notifications.createTextContentNotification(this, NotificationChannels.LOG_RESULT_NOTIFICATION, title, errorMessage)
                        .setAutoCancel(true)
                        .setContentIntent(retryIntent));
    }

    private static PendingIntent buildRetryIntent(final String geocode, @Nullable final LogEntry entryToEdit) {
        final Intent intent = new Intent(CgeoApplication.getInstance(), LogCacheActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, geocode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (entryToEdit != null) {
            intent.putExtra(Intents.EXTRA_LOGENTRY, entryToEdit);
        }
        return PendingIntent.getActivity(
                CgeoApplication.getInstance(),
                geocode.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
