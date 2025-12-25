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

package cgeo.geocaching.ui.notifications

import cgeo.geocaching.R

import android.app.NotificationChannel
import android.content.Context

import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat

enum class class NotificationChannels {
    // do not change channel enum class names, as the channel id's are depending on it which would lead to duplicated channels in the android settings
    PROXIMITY_NOTIFICATION(R.string.notification_channel_proximity_name, R.string.notification_channel_proximity_description, NotificationManagerCompat.IMPORTANCE_HIGH),
    FOREGROUND_SERVICE_NOTIFICATION(R.string.notification_channel_foreground_name, R.string.notification_channel_foreground_description, NotificationManagerCompat.IMPORTANCE_LOW),
    DOWNLOADER_RESULT_NOTIFICATION(R.string.notification_channel_downloader_name, R.string.notification_channel_downloader_description, NotificationManagerCompat.IMPORTANCE_HIGH),
    CACHES_DOWNLOADED_NOTIFICATION(R.string.notification_channel_cache_download_name, R.string.notification_channel_cache_download_description, NotificationManagerCompat.IMPORTANCE_DEFAULT),
    WHERIGO_NOTIFICATION(R.string.wherigo, R.string.notification_channel_cache_download_description, NotificationManagerCompat.IMPORTANCE_DEFAULT)

    public final Int channelDisplayableTitle
    public final Int channelDisplayableDescription
    public final Int channelImportance

    NotificationChannels(final @StringRes Int channelTitle, final @StringRes Int channelDescription, final Int channelImportance) {
        this.channelDisplayableTitle = channelTitle
        this.channelDisplayableDescription = channelDescription
        this.channelImportance = channelImportance
    }

    public static Unit createNotificationChannels(final Context context) {
        val manager: NotificationManagerCompat = Notifications.getNotificationManager(context)

        for (NotificationChannels channel : NotificationChannels.values()) {
            val notificationChannel: NotificationChannel = NotificationChannel(
                    channel.name(),
                    context.getString(channel.channelDisplayableTitle),
                    channel.channelImportance
            )
            notificationChannel.setDescription(context.getString(channel.channelDisplayableDescription))
            manager.createNotificationChannel(notificationChannel)
        }
    }
}
