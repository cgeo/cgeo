package cgeo.geocaching.ui.notifications;

import cgeo.geocaching.R;

import android.app.NotificationChannel;
import android.content.Context;
import android.os.Build;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationManagerCompat;

public enum NotificationChannels {
    // do not change channel enum names, as the channel id's are depending on it which would lead to duplicated channels in the android settings
    PROXIMITY_NOTIFICATION(R.string.notification_channel_proximity_name, R.string.notification_channel_proximity_description, NotificationManagerCompat.IMPORTANCE_HIGH),
    FOREGROUND_SERVICE_NOTIFICATION(R.string.notification_channel_foreground_name, R.string.notification_channel_foreground_description, NotificationManagerCompat.IMPORTANCE_LOW),
    DOWNLOADER_RESULT_NOTIFICATION(R.string.notification_channel_downloader_name, R.string.notification_channel_downloader_description, NotificationManagerCompat.IMPORTANCE_HIGH),
    CACHES_DOWNLOADED_NOTIFICATION(R.string.notification_channel_cache_download_name, R.string.notification_channel_cache_download_description, NotificationManagerCompat.IMPORTANCE_DEFAULT);

    public final int channelDisplayableTitle;
    public final int channelDisplayableDescription;
    public final int channelImportance;

    NotificationChannels(final @StringRes int channelTitle, final @StringRes int channelDescription, final int channelImportance) {
        this.channelDisplayableTitle = channelTitle;
        this.channelDisplayableDescription = channelDescription;
        this.channelImportance = channelImportance;
    }

    public static void createNotificationChannels(final Context context) {
        final NotificationManagerCompat manager = Notifications.getNotificationManager(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (NotificationChannels channel : NotificationChannels.values()) {
                final NotificationChannel notificationChannel = new NotificationChannel(
                        channel.name(),
                        context.getString(channel.channelDisplayableTitle),
                        channel.channelImportance
                );
                notificationChannel.setDescription(context.getString(channel.channelDisplayableDescription));
                manager.createNotificationChannel(notificationChannel);
            }
        }
    }
}
