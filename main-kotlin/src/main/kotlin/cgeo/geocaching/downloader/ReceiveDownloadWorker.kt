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

package cgeo.geocaching.downloader

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.models.Download
import cgeo.geocaching.ui.notifications.NotificationChannels
import cgeo.geocaching.ui.notifications.Notifications

import android.content.Context
import android.net.Uri

import androidx.annotation.NonNull
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReceiveDownloadWorker : Worker() {

    protected NotificationCompat.Builder notification
    protected NotificationManagerCompat notificationManager

    public ReceiveDownloadWorker(final Context context, final WorkerParameters params) {
        super(context, params)
    }

    override     public Result doWork() {
        try {
            notificationManager = Notifications.getNotificationManager(CgeoApplication.getInstance())
            notification = Notifications.createNotification(CgeoApplication.getInstance(), NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION, R.string.notification_download_receiver_title)
                    .setProgress(100, 0, true)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)

            // unbundle parameters and start receive
            val data: Data = getInputData()
            return ReceiveDownload().receiveDownload(CgeoApplication.getInstance(), notificationManager, notification, this::updateForegroundNotification,
                    Uri.parse(data.getString(Intents.EXTRA_ADDRESS)),
                    data.getString(Intents.EXTRA_FILENAME),
                    data.getString(DownloaderUtils.RESULT_CHOSEN_URL),
                    data.getLong(DownloaderUtils.RESULT_DATE, 0),
                    data.getInt(DownloaderUtils.RESULT_TYPEID, Download.DownloadType.DEFAULT))
        } catch (Exception e) {
            return Result.failure()
        } finally {
            notificationManager.cancel(getForegroundNotificationId())
        }
    }

    private Int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_MAP_IMPORT
    }

    private Unit updateForegroundNotification() {
        Notifications.send(CgeoApplication.getInstance(), getForegroundNotificationId(), notification)
    }

}
