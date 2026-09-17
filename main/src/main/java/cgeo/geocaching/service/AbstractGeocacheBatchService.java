package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base for foreground services that batch-process geocaches with a progress notification.
 *
 * Different concrete subclasses run in parallel (each is a distinct Android service).
 * Multiple intents of the same subclass are queued and processed sequentially (IntentService behavior).
 *
 * To add a new batch task type, subclass this class and implement:
 * - {@link #processGeocode(String)} — the actual work per geocode
 * - {@link #getTitle()} — string resource for the notification title
 * - {@link #getForegroundNotificationId()} — unique notification ID for the progress notification
 * - {@link #getCompletionNotificationChannel()} — channel for the end notification
 * - {@link #getCompletedText(int)} — completion text (or null to suppress the end notification)
 * - {@link #getInterruptedText(boolean, int, int)} — text when processing is interrupted
 *
 * Then register the service in AndroidManifest.xml with android:foregroundServiceType="dataSync".
 */
public abstract class AbstractGeocacheBatchService extends AbstractForegroundIntentService {

    static final String EXTRA_SERVICE_CLASS = "serviceClass";

    private static final ConcurrentHashMap<String, BatchState> BATCH_STATES = new ConcurrentHashMap<>();

    protected static final class BatchState {
        final Set<String> pending = Collections.synchronizedSet(new LinkedHashSet<>());
        volatile boolean shouldStop = false;
        final AtomicInteger processed = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);
    }

    // ---- Static API for callers ----

    /**
     * Enqueue geocodes for batch processing and start (or re-trigger) the service.
     * Geocodes already in the queue are ignored.
     *
     * If the service is already running its processing loop will pick up the new geocodes
     * automatically (they land in the shared pending set before the intent is dispatched),
     * so the second intent's onHandleIntent simply finds nothing left to do and exits.
     */
    protected static <T extends AbstractGeocacheBatchService> void addGeocodes(
            final Context context, final Class<T> serviceClass, final Collection<String> geocodes) {
        if (geocodes.isEmpty()) {
            return;
        }
        getOrCreateState(serviceClass).pending.addAll(geocodes);
        ContextCompat.startForegroundService(context, new Intent(context, serviceClass));
    }

    /** Returns true if the geocode is queued or currently being processed by the given service. */
    public static boolean isGeocachePending(final Class<? extends AbstractGeocacheBatchService> clazz, final String geocode) {
        final BatchState state = BATCH_STATES.get(clazz.getName());
        return state != null && state.pending.contains(geocode);
    }

    /** Request cancellation for the given service type. Takes effect before the next geocode. */
    public static void requestStop(final Class<? extends AbstractGeocacheBatchService> clazz) {
        final BatchState state = BATCH_STATES.get(clazz.getName());
        if (state != null) {
            state.shouldStop = true;
        }
    }

    // ---- Abstract contract for subclasses ----

    /**
     * Perform the actual work for one geocode. Called on a background thread.
     *
     * @return true on success, false on failure
     */
    protected abstract boolean processGeocode(String geocode);

    /** String resource ID for the notification title (used in both progress and end notifications). */
    @StringRes
    protected abstract int getTitle();

    /** Notification channel for the end (completion/cancel) notification. */
    protected abstract NotificationChannels getCompletionNotificationChannel();

    /**
     * Text for the completion notification shown after all geocodes were processed.
     * Return null to suppress the notification (e.g. when only one cache was processed).
     */
    @Nullable
    protected abstract String getCompletedText(int processedCount);

    /**
     * Text for the end notification when processing was interrupted by cancellation or failures.
     *
     * @param wasCanceled  true if the user explicitly cancelled
     * @param processedCount number of successfully processed geocodes
     * @param totalCount   total geocodes in this batch (processed + failed + still pending)
     */
    @NonNull
    protected abstract String getInterruptedText(boolean wasCanceled, int processedCount, int totalCount);

    // ---- Service lifecycle ----

    @Override
    protected NotificationCompat.Builder createInitialNotification() {
        getOrCreateState(getClass()).shouldStop = false;

        final Intent stopIntent = new Intent(this, StopBatchServiceReceiver.class);
        stopIntent.putExtra(EXTRA_SERVICE_CLASS, getClass().getName());
        final PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                this, getForegroundNotificationId(), stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return Notifications.createNotification(this, NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION, getTitle())
                .setProgress(100, 0, true)
                .addAction(R.drawable.ic_menu_cancel, LocalizationUtils.getString(android.R.string.cancel), cancelPendingIntent);
    }

    /**
     * Drains the shared pending queue sequentially until empty or stopped.
     *
     * Because geocodes are added to the pending set *before* the triggering intent is
     * dispatched, a running loop will often absorb a second batch on its own. The intent
     * fired by that second addGeocodes call then triggers this method again, which simply
     * finds nothing left and returns — one notification, continuously updated total.
     */
    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        if (intent == null) {
            return;
        }
        final BatchState state = getOrCreateState(getClass());
        while (!state.shouldStop) {
            final String geocode;
            synchronized (state.pending) {
                if (state.pending.isEmpty()) {
                    break;
                }
                geocode = state.pending.iterator().next();
            }
            processGeocodeWithTracking(geocode);
        }
    }

    /**
     * Processes one geocode, updating the progress notification and tracking success/failure.
     *
     * Subclasses that override {@link #onHandleIntent} (e.g. to process geocodes in parallel)
     * should call this method instead of {@link #processGeocode} directly.
     */
    protected final void processGeocodeWithTracking(final String geocode) {
        final BatchState state = getOrCreateState(getClass());

        final int done = state.processed.get() + state.failed.get();
        final int total = state.pending.size() + done;
        notification.setProgress(total, done, false);
        notification.setContentText(done + "/" + total);
        updateForegroundNotification();

        try {
            if (processGeocode(geocode)) {
                state.processed.incrementAndGet();
            } else {
                state.failed.incrementAndGet();
            }
        } catch (final Exception e) {
            Log.e("Error processing geocode " + geocode, e);
            state.failed.incrementAndGet();
        }
        state.pending.remove(geocode);
    }

    @Override
    public void onDestroy() {
        final BatchState state = BATCH_STATES.get(getClass().getName());
        if (state != null) {
            final int processed = state.processed.get();
            final int failed = state.failed.get();
            final boolean canceled = state.shouldStop;

            if (!state.pending.isEmpty()) {
                showEndNotification(getInterruptedText(canceled, processed, processed + failed + state.pending.size()));
            } else if (failed > 0) {
                showEndNotification(getInterruptedText(false, processed, processed + failed));
            } else {
                final String text = getCompletedText(processed);
                if (text != null) {
                    showEndNotification(text);
                }
            }
            BATCH_STATES.remove(getClass().getName());
        }
        super.onDestroy();
    }

    // ---- Protected helpers ----

    protected void showEndNotification(final String text) {
        Notifications.send(this, Settings.getUniqueNotificationId(),
                Notifications.createTextContentNotification(this, getCompletionNotificationChannel(), getTitle(), text)
                        .setSilent(true));
    }

    protected BatchState getCurrentState() {
        return getOrCreateState(getClass());
    }

    private static BatchState getOrCreateState(final Class<?> clazz) {
        return BATCH_STATES.computeIfAbsent(clazz.getName(), k -> new BatchState());
    }
}
