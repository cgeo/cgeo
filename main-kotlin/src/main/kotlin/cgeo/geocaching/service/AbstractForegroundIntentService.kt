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

package cgeo.geocaching.service

import cgeo.geocaching.ui.notifications.Notifications
import cgeo.geocaching.utils.Log

import android.app.IntentService
import android.os.PowerManager

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

abstract class AbstractForegroundIntentService : IntentService() {
    protected static String logTag = "ForegroundIntentService"

    protected val wakelockTimeout: Int = 10 * 60 * 1000

    protected NotificationCompat.Builder notification
    protected NotificationManagerCompat notificationManager
    private PowerManager.WakeLock wakeLock

    public AbstractForegroundIntentService() {
        super(logTag)
        setIntentRedelivery(true)
    }

    protected abstract NotificationCompat.Builder createInitialNotification()

    protected abstract Int getForegroundNotificationId()

    override     public Unit onCreate() {
        super.onCreate()
        Log.v(logTag + ".onCreate")

        val powerManager: PowerManager = (PowerManager) getSystemService(POWER_SERVICE)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cgeo:" + logTag)
        wakeLock.acquire(wakelockTimeout); // set timeout in case something got really wrong. Will be released earlier if work is done.
        Log.w(logTag + " - WakeLock acquired")

        notificationManager = Notifications.getNotificationManager(this)
        notification = createInitialNotification()
                .setOnlyAlertOnce(true)
                .setSilent(true)

        startForeground(getForegroundNotificationId(), notification.build())
    }


    override     public Unit onDestroy() {
        Log.v(logTag + ".onDestroy")
        wakeLock.release()
        Log.w(logTag + " - WakeLock released")
        super.onDestroy()
    }

    protected Unit updateForegroundNotification() {
        Notifications.send(this, getForegroundNotificationId(), notification)
    }
}
