package cgeo.geocaching.wherigo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.function.BiConsumer;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class WherigoGameService extends Service {

    private final CompositeDisposable serviceDisposables = new CompositeDisposable();
    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {

        @Override
        public void updateGeoDir(@NonNull final GeoData newGeo, final float newDirection) {
            WherigoLocationProvider.get().updateGeoDir(newGeo, newDirection);
        }
    };


    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.iForce("WherigoGameService STARTED");
        final String content = LocalizationUtils.getString(R.string.wherigo_notification_service);
        serviceDisposables.add(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR));
        startForeground(Notifications.ID_WHERIGO_SERVICE_NOTIFICATION_ID, Notifications.newBuilder(this, NotificationChannels.WHERIGO_NOTIFICATION)
            .setSmallIcon(R.drawable.ic_menu_wherigo)
            .setContentTitle(content)
            .setContentText(content)
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, WherigoActivity.class), ProcessUtils.getFlagImmutable()))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true).build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceDisposables.clear();
        Log.iForce("WherigoGameService STOPPED");
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.iForce("WherigoGameService STARTCOMMAND");

        return super.onStartCommand(intent, flags, startId);
    }

   public static void startService() {
        performWithIntent(ContextCompat::startForegroundService);
    }

    public static void stopService() {
        performWithIntent(Context::stopService);
    }

    private static void performWithIntent(final BiConsumer<Context, Intent> executer) {
        final Context context = CgeoApplication.getInstance();
        final Intent intent = new Intent(CgeoApplication.getInstance(), WherigoGameService.class);
        executer.accept(context, intent);
    }


}
