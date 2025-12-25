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

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.models.Download
import cgeo.geocaching.service.AbstractForegroundIntentService
import cgeo.geocaching.ui.notifications.NotificationChannels
import cgeo.geocaching.ui.notifications.Notifications

import android.content.Intent

import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat

class ReceiveDownloadService : AbstractForegroundIntentService() {
    static {
        logTag = "ReceiveDownloadService"
    }

    override     public NotificationCompat.Builder createInitialNotification() {
        return Notifications.createNotification(this, NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION, R.string.notification_download_receiver_title)
                .setProgress(100, 0, true)
    }

    override     protected Int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_MAP_IMPORT
    }

    override     protected Unit onHandleIntent(final Intent intent) {
        if (intent == null) {
            return
        }
        // unbundle parameters and start receive
        ReceiveDownload().receiveDownload(getBaseContext(), notificationManager, notification, this::updateForegroundNotification,
                intent.getData(),
                intent.getStringExtra(Intents.EXTRA_FILENAME),
                intent.getStringExtra(DownloaderUtils.RESULT_CHOSEN_URL),
                intent.getLongExtra(DownloaderUtils.RESULT_DATE, 0),
                intent.getIntExtra(DownloaderUtils.RESULT_TYPEID, Download.DownloadType.DEFAULT))
    }

}
