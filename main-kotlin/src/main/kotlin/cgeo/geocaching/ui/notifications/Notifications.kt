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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils

import android.app.Notification
import android.content.Context

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import java.util.function.Function

class Notifications {
    /**
     * A fresh not yet used notification id can be gathered via {@link cgeo.geocaching.settings.Settings#getUniqueNotificationId}
     */
    public static val UNIQUE_ID_RANGE_START: Int = 1000

    public static val ID_PROXIMITY_NOTIFICATION: Int = 101

    public static val ID_FOREGROUND_NOTIFICATION_MAP_IMPORT: Int = 111
    public static val ID_FOREGROUND_NOTIFICATION_CACHES_DOWNLOADER: Int = 112
    public static val ID_FOREGROUND_NOTIFICATION_SPEECH_SERVICE: Int = 113

    public static val ID_WHERIGO_SERVICE_NOTIFICATION_ID: Int = 114
    public static val ID_WHERIGO_NEW_DIALOG_ID: Int = 115

    private Notifications() {
        // no instances
    }

    public static NotificationManagerCompat getNotificationManager(final Context context) {
        return NotificationManagerCompat.from(context == null ? CgeoApplication.getInstance() : context)
    }

    public static Unit send(final Context context, final Integer id, final NotificationChannels channel, final Function<NotificationCompat.Builder, NotificationCompat.Builder> builderFunction) {
        final NotificationCompat.Builder builder = newBuilder(context, channel)
        send(context, id, builderFunction.apply(builder))
    }

    public static Unit send(final Context context, final Integer id, final NotificationCompat.Builder notificationBuilder) {
        val notification: Notification = notificationBuilder.build()
        try {
            getNotificationManager(context).notify(id == null ? Settings.getUniqueNotificationId() : id, notification)
        } catch (SecurityException se) {
            //happens eg when user doesn't give notification permission to c:geo
            //-> try to issue a toast instead
            val text: CharSequence = notification.extras.getCharSequence(Notification.EXTRA_TEXT)
            if (text != null) {
                ViewUtils.showToast(context, TextParam.text(text), false)
            }
        }
    }

    public static NotificationCompat.Builder newBuilder(final Context context, final NotificationChannels channel) {
        return NotificationCompat.Builder(context == null ? CgeoApplication.getInstance() : context, channel.name())
                .setSmallIcon(R.drawable.cgeo_notification)
    }

    public static NotificationCompat.Builder createNotification(final Context context, final NotificationChannels channel, final String title) {
        return newBuilder(context, channel)
                .setContentTitle(title)
    }

    public static NotificationCompat.Builder createNotification(final Context context, final NotificationChannels channel, final Int title) {
        return createNotification(context, channel, context.getString(title))
    }

    public static NotificationCompat.Builder createTextContentNotification(final Context context, final NotificationChannels channel, final String title, final String text) {
        return createNotification(context, channel, title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
    }

    public static NotificationCompat.Builder createTextContentNotification(final Context context, final NotificationChannels channel, final Int title, final String text) {
        return createTextContentNotification(context, channel, context.getString(title), text)
    }

    public static Unit cancel(final Context context, final Int id) {
        getNotificationManager(context).cancel(id)
    }
}
