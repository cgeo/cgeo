package menion.android.whereyougo.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import cgeo.geocaching.R;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;

public class NotificationService extends Service {
    private static final int notification_id = 10;
    private static final String NOTIFICATION_CHANNEL_ID = "menion.android.whereyougo.utils.NotificationService";
    private boolean foreground = false;
    private boolean running = false;
    private NotificationManager mNM;
    private String contentTitel;

    public static final String TITEL = "ContentTitel";
    public static final String START_NOTIFICATION_SERVICE = "START_NOTIFICATION_SERVICE";
    public static final String START_NOTIFICATION_SERVICE_FOREGROUND = "START_NOTIFICATION_SERVICE_FOREGROUND";
    public static final String STOP_NOTIFICATION_SERVICE = "STOP_NOTIFICATION_SERVICE";

    private static final String TAG = "NotificationService";

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case if service are bound (Bound Services).
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.v(TAG, "onCreate()");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
            mNM.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case START_NOTIFICATION_SERVICE:
                        contentTitel = intent.getStringExtra(TITEL);
                        startNotificationService(true);
                        break;
                    case START_NOTIFICATION_SERVICE_FOREGROUND:
                        contentTitel = intent.getStringExtra(TITEL);
                        startNotificationService(false);
                        break;
                    case STOP_NOTIFICATION_SERVICE:
                        stopNotificationService();
                        break;
                    default:
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startNotificationService(boolean background) {
        Logger.v(TAG, "Start notification service.");

        Context context = A.getMain().getApplicationContext();
        Intent intent = new Intent(context, WhereYouGoActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(contentTitel);
        builder.setSmallIcon(R.drawable.ic_title_logo);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);

        Notification notification = builder.build();

        if (running && background == foreground) {
           if (foreground) {
              stopForeground(true);
           } else {
              mNM.cancel(notification_id);
           }
           running = false;
        }

        if (!running) {
            if (!background) {
                startForeground(notification_id, notification);
                foreground = true;
            } else {
                mNM.notify(notification_id, notification);
                foreground = false;
            }
            running = true;
        }
    }

    private void stopNotificationService() {
        Logger.v(TAG, "Stop notification service.");
        if (foreground) {
            stopForeground(true);
            foreground = false;
        } else {
            mNM.cancel(notification_id);
        }

        running = false;
        stopSelf();
    }
}
