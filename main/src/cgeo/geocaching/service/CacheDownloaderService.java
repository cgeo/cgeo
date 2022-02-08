package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.CheckboxDialogConfig;
import cgeo.geocaching.ui.dialog.Dialogs;
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
    private static final String REDOWNLOAD = "extra_redownload";

    public static void downloadCaches(final Activity context, final Set<String> geocodes, final boolean defaultForceRedownload, final boolean isOffline) {
        if (geocodes.isEmpty()) {
            ActivityMixin.showToast(context, context.getString(R.string.warn_save_nothing));
            return;
        }

        Dialogs.confirmWithCheckbox(context, "Warning", "This feature is untested and can lead to unexpected behaviour or may even break your database.\n\nWe recommend to do a backup before using the feature, just to be save.",
                CheckboxDialogConfig.newCheckbox(R.string.caches_store_background_should_force_refresh)
                        .setCheckedOnInit(defaultForceRedownload)
                        .setVisible(!isOffline),
                forceRedownload -> {
                    if (isOffline) {
                        downloadCachesInternal(context, geocodes, null, forceRedownload);
                    } else if (Settings.getChooseList()) {
                        // let user select list to store cache in
                        new StoredList.UserInterface(context).promptForMultiListSelection(R.string.lists_title, selectedListIds -> downloadCachesInternal(context, geocodes, selectedListIds, forceRedownload), true, Collections.emptySet(), false);
                    } else {
                        downloadCachesInternal(context, geocodes, Collections.singleton(StoredList.STANDARD_LIST_ID), forceRedownload);
                    }
                }, null);
    }

    private static void downloadCachesInternal(final Activity context, final Set<String> geocodes, @Nullable final Set<Integer> listIds, final boolean forceRedownload) {
        final Intent intent = new Intent(context, CacheDownloaderService.class);
        intent.putStringArrayListExtra(GEOCODES, new ArrayList<>(geocodes));
        if (listIds != null) {
            intent.putIntegerArrayListExtra(LIST_IDS, new ArrayList<>(listIds));
        }
        intent.putExtra(REDOWNLOAD, forceRedownload);
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

        final ArrayList<Integer> listIdArray = intent.getIntegerArrayListExtra(LIST_IDS);
        final Set<Integer> listIds = listIdArray != null ? new HashSet<>(intent.getIntegerArrayListExtra(LIST_IDS)) : null;

        final boolean forceRedownload = intent.getBooleanExtra(REDOWNLOAD, false);


        for (int i = 0; i < geocodes.size(); i++) {
            notification.setProgress(geocodes.size(), i, false);
            notification.setContentText(geocodes.get(i));
            updateForegroundNotification();

            Geocache.storeCache(null, geocodes.get(i), listIds, forceRedownload, null);
        }

        notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                this, NotificationChannels.DOWNLOADER_RESULT_NOTIFICATION, R.string.caches_store_background_title, geocodes.size() + " caches downloaded")
                .build());
    }
}
