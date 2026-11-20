package cgeo.geocaching.service;

import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.Log;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public abstract class AbstractForegroundIntentService extends IntentService {
    protected static String logTag = "ForegroundIntentService";

    protected final int wakelockTimeout = 10 * 60 * 1000;

    protected NotificationCompat.Builder notification;
    protected NotificationManagerCompat notificationManager;
    private PowerManager.WakeLock wakeLock;

    public AbstractForegroundIntentService() {
        super(logTag);
        setIntentRedelivery(true);
    }

    /**
     * Starts a service derived from this class.
     * <br />
     * If the app is in foreground, the service is started as a regular service, which then promotes itself
     * to a foreground service in onCreate(). This avoids the system's obligation to call startForeground()
     * for each start request in time, which otherwise crashes the app (ForegroundServiceDidNotStartInTimeException),
     * e.g. if the service stops itself while a start request is still pending (see #18931).
     * <br />
     * If the app is in background (where starting a regular service is not allowed),
     * it falls back to starting a foreground service.
     */
    public static void start(final Context context, final Intent intent) {
        try {
            context.startService(intent);
        } catch (IllegalStateException e) {
            // BackgroundServiceStartNotAllowedException on API 31+, plain IllegalStateException on API 26-30
            Log.w("cannot start service from background, falling back to foreground service start: " + e.getMessage());
            ContextCompat.startForegroundService(context, intent);
        }
    }

    protected abstract NotificationCompat.Builder createInitialNotification();

    protected abstract int getForegroundNotificationId();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(logTag + ".onCreate");

        final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cgeo:" + logTag);
        wakeLock.acquire(wakelockTimeout); // set timeout in case something got really wrong. Will be released earlier if work is done.
        Log.w(logTag + " - WakeLock acquired");

        notificationManager = Notifications.getNotificationManager(this);
        notification = createInitialNotification()
                .setOnlyAlertOnce(true)
                .setSilent(true);

        try {
            startForeground(getForegroundNotificationId(), notification.build());
        } catch (IllegalStateException | SecurityException e) {
            // actually a ForegroundServiceStartNotAllowedException, which is API 31+,
            // therefore resolving to first ancestor, which is available for API 26
            // SecurityException is thrown on API 34+ when permissions are missing
            Log.e("cannot start foreground service " + e.getMessage());
        }
    }


    @Override
    public void onDestroy() {
        Log.v(logTag + ".onDestroy");
        wakeLock.release();
        Log.w(logTag + " - WakeLock released");
        super.onDestroy();
    }

    protected void updateForegroundNotification() {
        Notifications.send(this, getForegroundNotificationId(), notification);
    }
}
