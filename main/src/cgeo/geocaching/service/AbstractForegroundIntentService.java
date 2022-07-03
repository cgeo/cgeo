package cgeo.geocaching.service;

import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.Log;

import android.app.IntentService;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

        startForeground(getForegroundNotificationId(), notification.build());
    }


    @Override
    public void onDestroy() {
        Log.v(logTag + ".onDestroy");
        wakeLock.release();
        Log.w(logTag + " - WakeLock released");
        super.onDestroy();
    }

    protected void updateForegroundNotification() {
        notificationManager.notify(getForegroundNotificationId(), notification.build());
    }
}
