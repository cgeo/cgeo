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

package cgeo.geocaching.wherigo

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.ui.notifications.NotificationChannels
import cgeo.geocaching.ui.notifications.Notifications
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

import androidx.annotation.NonNull
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import java.util.function.BiConsumer

import io.reactivex.rxjava3.disposables.CompositeDisposable

class WherigoGameService : Service() {

    private val serviceDisposables: CompositeDisposable = CompositeDisposable()
    private val geoDirHandler: GeoDirHandler = GeoDirHandler() {

        override         public Unit updateGeoDir(final GeoData newGeo, final Float newDirection) {
            WherigoLocationProvider.get().updateGeoDir(newGeo, newDirection)
        }
    }


    override     public IBinder onBind(final Intent intent) {
        return null
    }

    override     public Unit onCreate() {
        super.onCreate()
        Log.iForce("WherigoGameService STARTED")
        val content: String = LocalizationUtils.getString(R.string.wherigo_notification_service)
        serviceDisposables.add(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR))
        try {
            startForeground(Notifications.ID_WHERIGO_SERVICE_NOTIFICATION_ID, Notifications.newBuilder(this, NotificationChannels.WHERIGO_NOTIFICATION)
                    .setSmallIcon(R.drawable.ic_menu_wherigo)
                    .setContentTitle(content)
                    .setContentText(content)
                    .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, WherigoActivity.class), PendingIntent.FLAG_IMMUTABLE))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSilent(true)
                    .setOngoing(true).build())
        } catch (IllegalStateException re) {
            // See #17487.
            // ForegroundServiceStartNotAllowedException : IllegalStateException() and can't be used before SDK level 31
            Log.e("WherigoGameService: IllegalStateException on starting as foreground service", re)
        }
    }

    override     public Unit onDestroy() {
        super.onDestroy()
        serviceDisposables.clear()
        Log.iForce("WherigoGameService STOPPED")
    }

    override     public Int onStartCommand(final Intent intent, final Int flags, final Int startId) {
        Log.iForce("WherigoGameService STARTCOMMAND")

        return super.onStartCommand(intent, flags, startId)
    }

   public static Unit startService() {
        performWithIntent(ContextCompat::startForegroundService)
    }

    public static Unit stopService() {
        performWithIntent(Context::stopService)
    }

    private static Unit performWithIntent(final BiConsumer<Context, Intent> executer) {
        val context: Context = CgeoApplication.getInstance()
        val intent: Intent = Intent(CgeoApplication.getInstance(), WherigoGameService.class)
        executer.accept(context, intent)
    }


}
