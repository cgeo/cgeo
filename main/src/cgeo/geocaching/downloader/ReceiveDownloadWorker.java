package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReceiveDownloadWorker extends Worker {

    protected NotificationCompat.Builder notification;
    protected NotificationManagerCompat notificationManager;

    public ReceiveDownloadWorker(@NonNull final Context context, @NonNull final WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            notificationManager = Notifications.getNotificationManager(CgeoApplication.getInstance());
            notification = Notifications.createNotification(CgeoApplication.getInstance(), NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION, R.string.notification_download_receiver_title)
                    .setProgress(100, 0, true)
                    .setOnlyAlertOnce(true)
                    .setSilent(true);

            // unbundle parameters and start receive
            final Data data = getInputData();
            return new ReceiveDownload().receiveDownload(CgeoApplication.getInstance(), notificationManager, notification, this::updateForegroundNotification,
                    Uri.parse(data.getString(Intents.EXTRA_ADDRESS)),
                    data.getString(Intents.EXTRA_FILENAME),
                    data.getString(DownloaderUtils.RESULT_CHOSEN_URL),
                    data.getLong(DownloaderUtils.RESULT_DATE, 0),
                    data.getInt(DownloaderUtils.RESULT_TYPEID, Download.DownloadType.DEFAULT));
        } catch (Exception e) {
            return Result.failure();
        } finally {
            notificationManager.cancel(getForegroundNotificationId());
        }
    }

    private int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_MAP_IMPORT;
    }

    private void updateForegroundNotification() {
        notificationManager.notify(getForegroundNotificationId(), notification.build());
    }

}
