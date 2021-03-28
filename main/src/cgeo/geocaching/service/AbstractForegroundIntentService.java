package cgeo.geocaching.service;

import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.Log;

import android.app.IntentService;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public abstract class AbstractForegroundIntentService extends IntentService implements IAbstractForgreoundIntentService {
    protected static String logTag = "ForegroundIntentService";

    protected int wakelockTimeout = 10 * 60 * 1000;
    protected int foregroundNotificationId = Notifications.ID_FOREGROUND_NOTIFICATION;

    protected NotificationCompat.Builder notification;
    protected NotificationManagerCompat notificationManager;
    private PowerManager.WakeLock wakeLock;

    public AbstractForegroundIntentService() {
        super(logTag);
        setIntentRedelivery(true);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(logTag + ".onCreate");

        final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cgeo:" + logTag);
        wakeLock.acquire(wakelockTimeout); // set timeout in case something got really wrong. Will be released earlier if work is done.
        Log.v(logTag + " - WakeLock acquired");

        notificationManager = Notifications.getNotificationManager(this);
        notification = createInitialNotification();

        startForeground(foregroundNotificationId, notification.build());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(logTag + ".onDestroy");
        wakeLock.release();
        Log.v(logTag + " - WakeLock released");
    }
}
