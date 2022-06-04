package cgeo.geocaching.service;

import cgeo.geocaching.Intents;
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
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: Currently this is an ALPHA feature. Before releasing, all hardcoded strings should be localized.
 */
public class CacheDownloaderService extends AbstractForegroundIntentService {
    static {
        logTag = "CacheDownloaderService";
    }

    private static final String EXTRA_GEOCODES = "extra_geocodes";

    private static volatile boolean shouldStop = false;
    private static final Map<String, DownloadTaskProperties> downloadQuery = new HashMap<>();

    int cachesDownloaded = 0;

    public static boolean isDownloadPending(final String geocode) {
        return downloadQuery.containsKey(geocode);
    }

    public static boolean isDownloadPending(final Geocache geocache) {
        return downloadQuery.containsKey(geocache.getGeocode());
    }

    public static void downloadCaches(final Activity context, final Set<String> geocodes, final boolean defaultForceRedownload, final boolean isOffline, @Nullable final Runnable onStartCallback) {
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
                        downloadCachesInternal(context, geocodes, null, forceRedownload, onStartCallback);
                    } else if (Settings.getChooseList()) {
                        // let user select list to store cache in
                        new StoredList.UserInterface(context).promptForMultiListSelection(R.string.lists_title, selectedListIds -> downloadCachesInternal(context, geocodes, selectedListIds, forceRedownload, onStartCallback), true, Collections.emptySet(), false);
                    } else {
                        downloadCachesInternal(context, geocodes, Collections.singleton(StoredList.STANDARD_LIST_ID), forceRedownload, onStartCallback);
                    }
                }, null);
    }

    private static void downloadCachesInternal(final Activity context, final Set<String> geocodes, @Nullable final Set<Integer> listIds, final boolean forceRedownload, @Nullable final Runnable onStartCallback) {

        final ArrayList<String> newGeocodes = new ArrayList<>();

        for (String geocode : geocodes) {
            final DownloadTaskProperties properties = new DownloadTaskProperties(listIds, forceRedownload);
            final boolean isNewGeocode;
            synchronized (downloadQuery) {
                isNewGeocode = downloadQuery.get(geocode) == null;
                properties.merge(downloadQuery.get(geocode));
                downloadQuery.put(geocode, properties);
            }
            if (isNewGeocode) {
                newGeocodes.add(geocode);
            }
        }

        final Intent intent = new Intent(context, CacheDownloaderService.class);
        intent.putStringArrayListExtra(EXTRA_GEOCODES, newGeocodes);
        ContextCompat.startForegroundService(context, intent);
        Toast.makeText(context, R.string.download_started, Toast.LENGTH_LONG).show();

        if (onStartCallback != null) {
            onStartCallback.run();
        }
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

        for (String geocode : intent.getStringArrayListExtra(EXTRA_GEOCODES)) {
            handleDownload(geocode);
        }
    }

    private void handleDownload(final String geocode) {
        try {
            if (shouldStop) {
                Log.i("download canceled");
                return;
            }

            final DownloadTaskProperties properties;
            synchronized (downloadQuery) {
                properties = downloadQuery.put(geocode, null); // set the properties to null, to point out that the download is currently ongoing

            }
            if (properties == null) {
                throw new IllegalStateException("The cache is not present in the download query");
            }

            // update foreground service notification
            notification.setProgress(downloadQuery.size() + cachesDownloaded, cachesDownloaded, false);
            notification.setContentText(cachesDownloaded + "/" + (downloadQuery.size() + cachesDownloaded));
            updateForegroundNotification();

            // merge current lists and additional lists
            final Set<Integer> combinedListIds = new HashSet<>(properties.listIds);
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (cache != null && !cache.getLists().isEmpty()) {
                combinedListIds.addAll(cache.getLists());
            }

            // download...
            Geocache.storeCache(null, geocode, combinedListIds, properties.forceDownload, null);

            // send a broadcast so that foreground activities know that they might need to update their content
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Intents.ACTION_GEOCACHE_REFRESHED).putExtra(Intents.EXTRA_GEOCODE, geocode));

            // check whether the download properties are still null,
            // otherwise there is a new download task...
            synchronized (downloadQuery) {
                if (downloadQuery.get(geocode) == null) {
                    downloadQuery.remove(geocode);
                }
            }
            cachesDownloaded++;
        } catch (Exception ex) {
            Log.e("background download failed", ex);
        }
    }

    @Override
    public void onDestroy() {
        if (downloadQuery.size() > 0) {
            showEndNotification("Download " + (shouldStop ? "canceled" : "failed") + ", " + cachesDownloaded + "/" + (cachesDownloaded + downloadQuery.size()) + " caches were downloaded");
        } else {
            showEndNotification(cachesDownloaded + " caches downloaded");
        }
        downloadQuery.clear();
        super.onDestroy();
    }

    private void showEndNotification(final String text) {
        notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                this, NotificationChannels.DOWNLOADER_RESULT_NOTIFICATION, R.string.caches_store_background_title, text).build());

    }

    private static class DownloadTaskProperties {
        final Set<Integer> listIds = new HashSet<>();
        boolean forceDownload;

        private DownloadTaskProperties(@Nullable final Set<Integer> listIds, final boolean forceDownload) {
            if (listIds != null) {
                this.listIds.addAll(listIds);
            }
            this.forceDownload = forceDownload;
        }

        public DownloadTaskProperties merge(@Nullable final DownloadTaskProperties additionalProperties) {
            if (additionalProperties != null) {
                this.listIds.addAll(additionalProperties.listIds);
                this.forceDownload |= additionalProperties.forceDownload;
            }
            return this;
        }
    }
}
