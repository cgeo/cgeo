package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.CheckboxDialogConfig;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO: Currently this is an ALPHA feature. Before releasing, all hardcoded strings should be localized.
 */
public class CacheDownloaderService extends AbstractForegroundIntentService {
    static {
        logTag = "CacheDownloaderService";
    }

    private static final String GEOCODES = "extra_geocodes";
    private static final String LIST_IDS = "extra_list_ids";
    private static final String REDOWNLOAD = "extra_redownload";

    private static volatile boolean shouldStop = false;

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

    public static void requestStopService() {
        shouldStop = true;
    }

    @Override
    public NotificationCompat.Builder createInitialNotification() {
        shouldStop = false;
        final PendingIntent actionCancelIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, StopCacheDownloadServiceReceiver.class),
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0) | PendingIntent.FLAG_UPDATE_CURRENT);

        return Notifications.createNotification(this, NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION, R.string.caches_store_background_title)
                .setProgress(100, 0, true)
                .addAction(R.drawable.ic_menu_cancel, getString(android.R.string.cancel), actionCancelIntent)
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

        try {
            final ArrayList<String> geocodes = intent.getStringArrayListExtra(GEOCODES);

            final ArrayList<Integer> listIdArray = intent.getIntegerArrayListExtra(LIST_IDS);
            final Set<Integer> listIds = listIdArray != null ? new HashSet<>(intent.getIntegerArrayListExtra(LIST_IDS)) : new HashSet<>();
            final boolean forceRedownload = intent.getBooleanExtra(REDOWNLOAD, false);

            int cachesDownloaded = 0;
            while (cachesDownloaded < geocodes.size() && !shouldStop) {
                notification.setProgress(geocodes.size(), cachesDownloaded, false);
                notification.setContentText(geocodes.get(cachesDownloaded));
                updateForegroundNotification();

                final Set<Integer> combinedListIds = new HashSet<>(listIds);
                final Geocache cache = DataStore.loadCache(geocodes.get(cachesDownloaded), LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null && !cache.getLists().isEmpty()) {
                    combinedListIds.addAll(cache.getLists());
                }

                Geocache.storeCache(null, geocodes.get(cachesDownloaded), combinedListIds, forceRedownload, null);

                cachesDownloaded++;
            }

            if (shouldStop) {
                showEndNotification("Download canceled, " + cachesDownloaded + "/" + geocodes.size() + " caches were downloaded");
            } else {
                showEndNotification(geocodes.size() + " caches downloaded");
            }
        } catch (Exception ignored) {
            showEndNotification("download failed");
        }
    }

    private void showEndNotification(final String text) {
        notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                this, NotificationChannels.DOWNLOADER_RESULT_NOTIFICATION, R.string.caches_store_background_title, text).build());

    }
}
