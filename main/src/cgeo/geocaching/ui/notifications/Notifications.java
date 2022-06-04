package cgeo.geocaching.ui.notifications;

import cgeo.geocaching.R;

import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class Notifications {
    /**
     * A fresh not yet used notification id can be gathered via {@link cgeo.geocaching.settings.Settings#getUniqueNotificationId}
     */
    public static final int UNIQUE_ID_RANGE_START = 1000;

    public static final int ID_PROXIMITY_NOTIFICATION = 101;

    public static final int ID_FOREGROUND_NOTIFICATION_MAP_IMPORT = 111;
    public static final int ID_FOREGROUND_NOTIFICATION_CACHES_DOWNLOADER = 112;
    public static final int ID_FOREGROUND_NOTIFICATION_SPEECH_SERVICE = 113;

    private Notifications() {
        // no instances
    }

    public static NotificationManagerCompat getNotificationManager(final Context context) {
        return NotificationManagerCompat.from(context);
    }

    public static NotificationCompat.Builder newBuilder(final Context context, final NotificationChannels channel) {
        return new NotificationCompat.Builder(context, channel.name())
                .setSmallIcon(R.drawable.cgeo_notification);
    }

    public static NotificationCompat.Builder createNotification(final Context context, final NotificationChannels channel, final String title) {
        return newBuilder(context, channel)
                .setContentTitle(title);
    }

    public static NotificationCompat.Builder createNotification(final Context context, final NotificationChannels channel, final int title) {
        return createNotification(context, channel, context.getString(title));
    }

    public static NotificationCompat.Builder createTextContentNotification(final Context context, final NotificationChannels channel, final String title, final String text) {
        return createNotification(context, channel, title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
    }

    public static NotificationCompat.Builder createTextContentNotification(final Context context, final NotificationChannels channel, final int title, final String text) {
        return createTextContentNotification(context, channel, context.getString(title), text);
    }
}
