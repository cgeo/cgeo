package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.functions.Function;

public class CacheDownloaderService extends AbstractForegroundIntentService {
    static {
        logTag = "CacheDownloaderService";
    }

    private static final String EXTRA_GEOCODES = "extra_geocodes";

    private static volatile boolean shouldStop = false;
    private static final Map<String, DownloadTaskProperties> downloadQuery = new HashMap<>();

    final AtomicInteger cachesDownloaded = new AtomicInteger();

    public static boolean isDownloadPending(final String geocode) {
        return downloadQuery.containsKey(geocode);
    }

    public static boolean isDownloadPending(final Geocache geocache) {
        return isDownloadPending(geocache.getGeocode());
    }

    public static void downloadCaches(final Activity context, final Set<String> geocodes, final boolean defaultForceRedownload, final boolean isOffline, @Nullable final Runnable onStartCallback) {
        if (geocodes.isEmpty()) {
            ActivityMixin.showToast(context, context.getString(R.string.warn_save_nothing));
            return;
        }
        if (isOffline) {
            downloadCachesInternal(context, geocodes, null, defaultForceRedownload, onStartCallback);
            return;
        }
        if (DataStore.getUnsavedGeocodes(geocodes).size() == geocodes.size()) {
            askForListsIfNecessaryAndDownload(context, geocodes, false, false, onStartCallback);
            return;
        }

        // some caches are already stored offline, thus show the advanced selection dialog

        final View content = LayoutInflater.from(context).inflate(R.layout.dialog_background_download_config, null);
        final RadioGroup radioGroup = (RadioGroup) content.findViewById(R.id.radioGroup);

        Dialogs.newBuilder(context)
                .setView(content)
                .setTitle(R.string.caches_store_background_title)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final int id = radioGroup.getCheckedRadioButtonId();
                    if (id == R.id.radio_button_refresh) {
                        askForListsIfNecessaryAndDownload(context, geocodes, true, false, onStartCallback);
                    } else if (id == R.id.radio_button_add_to_list) {
                        askForListsIfNecessaryAndDownload(context, geocodes, false, false, onStartCallback);
                    } else {
                        askForListsIfNecessaryAndDownload(context, DataStore.getUnsavedGeocodes(geocodes), false, false, onStartCallback);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

    }

    public static void refreshCache(final Activity context, final String geocode, final boolean isOffline, @Nullable final Runnable onStartCallback) {
        askForListsIfNecessaryAndDownload(context, Collections.singleton(geocode), true, isOffline, onStartCallback);
    }

    private static void askForListsIfNecessaryAndDownload(final Activity context, final Set<String> geocodes, final boolean forceRedownload, final boolean isOffline, @Nullable final Runnable onStartCallback) {
        if (isOffline) {
            downloadCachesInternal(context, geocodes, null, forceRedownload, onStartCallback);
        } else if (Settings.getChooseList()) {
            // let user select list to store cache in
            new StoredList.UserInterface(context).promptForMultiListSelection(R.string.lists_title, selectedListIds -> downloadCachesInternal(context, geocodes, selectedListIds, forceRedownload, onStartCallback), true, Collections.emptySet(), false);
        } else {
            downloadCachesInternal(context, geocodes, Collections.singleton(StoredList.STANDARD_LIST_ID), forceRedownload, onStartCallback);
        }
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

        if (newGeocodes.isEmpty()) {
            return;
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
                .addAction(R.drawable.ic_menu_cancel, getString(android.R.string.cancel), actionCancelIntent);
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

        // schedule download on multiple threads...

        Log.d("Download task started");

        final Observable<String> geocodes = Observable.fromIterable(intent.getStringArrayListExtra(EXTRA_GEOCODES));
        geocodes.flatMap((Function<String, Observable<String>>) geocode -> Observable.create((ObservableOnSubscribe<String>) emitter -> {
            handleDownload(geocode);
            emitter.onComplete();
        }).subscribeOn(AndroidRxUtils.refreshScheduler)).blockingSubscribe();

        Log.d("Download task completed");
    }

    private void handleDownload(final String geocode) {
        try {
            if (shouldStop) {
                Log.i("download canceled");
                return;
            }

            Log.d("Download #" + cachesDownloaded.get() + " " + geocode + " started");

            final DownloadTaskProperties properties;
            synchronized (downloadQuery) {
                properties = downloadQuery.put(geocode, null); // set the properties to null, to point out that the download is currently ongoing

            }
            if (properties == null) {
                throw new IllegalStateException("The cache is not present in the download query");
            }

            // update foreground service notification
            notification.setProgress(downloadQuery.size() + cachesDownloaded.get(), cachesDownloaded.get(), false);
            notification.setContentText(cachesDownloaded.get() + "/" + (downloadQuery.size() + cachesDownloaded.get()));
            updateForegroundNotification();

            // merge current lists and additional lists
            final Set<Integer> combinedListIds = new HashSet<>(properties.listIds);
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (cache != null && !cache.getLists().isEmpty()) {
                combinedListIds.addAll(cache.getLists());
            }

            // download...
            if (Geocache.storeCache(null, geocode, combinedListIds, properties.forceDownload, null)) {
                // send a broadcast so that foreground activities know that they might need to update their content
                GeocacheChangedBroadcastReceiver.sendBroadcast(this, geocode);
                // check whether the download properties are still null,
                // otherwise there is a new download task...
                synchronized (downloadQuery) {
                    if (downloadQuery.get(geocode) == null) {
                        downloadQuery.remove(geocode);
                    }
                }
                Log.d("Download #" + cachesDownloaded.get() + " " + geocode + " completed");
                cachesDownloaded.incrementAndGet();
            } else {
                Log.d("Download #" + cachesDownloaded.get() + " " + geocode + " failed");
            }
        } catch (Exception ex) {
            Log.e("exception while background download", ex);
        }
    }

    @Override
    public void onDestroy() {
        if (downloadQuery.size() > 0) {
            showEndNotification(getString(shouldStop ? R.string.caches_store_background_result_canceled : R.string.caches_store_background_result_failed,
                    cachesDownloaded.get(), cachesDownloaded.get() + downloadQuery.size()));
        } else {
            showEndNotification(getResources().getQuantityString(R.plurals.caches_store_background_result, cachesDownloaded.get(), cachesDownloaded.get()));
        }
        downloadQuery.clear();
        super.onDestroy();
    }

    private void showEndNotification(final String text) {
        notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                this, NotificationChannels.CACHES_DOWNLOADED_NOTIFICATION, R.string.caches_store_background_title, text).setSilent(true).build());

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
