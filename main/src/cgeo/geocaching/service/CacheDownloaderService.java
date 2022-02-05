package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class CacheDownloaderService extends AbstractForegroundIntentService {
    static {
        logTag = "CacheDownloaderService";
    }

    private static final String GEOCODES = "extra_geocodes";
    private static final String LIST_IDS = "extra_list_ids";

    public static void downloadCaches(final Activity context, final Set<String> geocodes) {
        if (geocodes.isEmpty()) {
            ActivityMixin.showToast(context, context.getString(R.string.warn_save_nothing));
            return;
        }

        SimpleDialog.of(context)
                .setTitle(TextParam.text("Warning"))
                .setMessage(TextParam.text("This feature is untested and can lead to unexpected behaviour or may even break your database.\n\nWe recommend to do a backup before using the feature, just to be save."))
                .confirm((d, w) -> {
                    if (Settings.getChooseList()) {
                        // let user select list to store cache in
                        new StoredList.UserInterface(context).promptForMultiListSelection(R.string.lists_title, selectedListIds -> downloadCachesInternal(context, geocodes, selectedListIds), true, Collections.emptySet(), false);
                    } else {
                        downloadCachesInternal(context, geocodes, Collections.singleton(StoredList.STANDARD_LIST_ID));
                    }
                });
    }

    private static void downloadCachesInternal(final Activity context, final Set<String> geocodes, final Set<Integer> listIds) {
        final Intent intent = new Intent(context, CacheDownloaderService.class);
        intent.putStringArrayListExtra(GEOCODES, new ArrayList<>(geocodes));
        intent.putIntegerArrayListExtra(LIST_IDS, new ArrayList<>(listIds));
        ContextCompat.startForegroundService(context, intent);
    }

    @Override
    public NotificationCompat.Builder createInitialNotification() {
        return Notifications.createNotification(this, NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION, R.string.caches_store_background_title)
                .setProgress(100, 0, true)
                .setOnlyAlertOnce(true);
    }

    @Override
    protected int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_CACHES_DOWNLOADER;
    }

    @Override
    protected void onHandleIntent(final @Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        final ArrayList<String> geocodes = intent.getStringArrayListExtra(GEOCODES);
        final Set<Integer> listIds = new HashSet<>(intent.getIntegerArrayListExtra(LIST_IDS));

        for (int i = 0; i < geocodes.size(); i++) {
            notification.setProgress(geocodes.size(), i, false);
            notification.setContentText(geocodes.get(i));
            updateForegroundNotification();

            Geocache.storeCache(null, geocodes.get(i), listIds, false, null);
        }

        notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                this, NotificationChannels.DOWNLOADER_RESULT_NOTIFICATION, R.string.caches_store_background_title, geocodes.size() + " caches downloaded")
                .build());
    }
}
