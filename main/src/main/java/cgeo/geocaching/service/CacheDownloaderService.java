package cgeo.geocaching.service;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.functions.Function;

public class CacheDownloaderService extends AbstractGeocacheBatchService {
    static {
        logTag = "CacheDownloaderService";
    }

    private static final Map<String, DownloadTaskProperties> downloadQuery = new HashMap<>();

    public static boolean isDownloadPending(final String geocode) {
        return downloadQuery.containsKey(geocode);
    }

    public static boolean isDownloadPending(final Geocache geocache) {
        return isDownloadPending(geocache.getGeocode());
    }

    public static void downloadCaches(final Activity context, final Collection<String> geocodes, final boolean defaultForceRedownload, final boolean isOffline, @Nullable final Runnable onStartCallback) {
        if (geocodes.isEmpty()) {
            ActivityMixin.showToast(context, LocalizationUtils.getString(R.string.warn_save_nothing));
            return;
        }
        if (isOffline) {
            downloadCachesInternal(context, geocodes, null, false, defaultForceRedownload, onStartCallback);
            return;
        }
        if (DataStore.getUnsavedGeocodes(geocodes).size() == geocodes.size()) {
            askForListsIfNecessaryAndDownload(context, geocodes, false, false, false, onStartCallback);
            return;
        }

        // some caches are already stored offline, thus show the advanced selection dialog

        final View content = LayoutInflater.from(context).inflate(R.layout.dialog_background_download_config, null);
        final RadioGroup radioGroup = content.findViewById(R.id.radioGroup);

        Dialogs.newBuilder(context)
                .setView(content)
                .setTitle(R.string.caches_store_background_title)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final int id = radioGroup.getCheckedRadioButtonId();
                    if (id == R.id.radio_button_refresh_and_add) {
                        askForListsIfNecessaryAndDownload(context, geocodes, false, true, false, onStartCallback);
                    } else if (id == R.id.radio_button_refresh_and_keep) {
                        // downloadCachesInternal(context, geocodes, null, true, onStartCallback);
                        askForListsIfNecessaryAndDownload(context, geocodes, true, true, false, onStartCallback);
                    } else if (id == R.id.radio_button_add_to_list) {
                        askForListsIfNecessaryAndDownload(context, geocodes, false, false, false, onStartCallback);
                    } else {
                        askForListsIfNecessaryAndDownload(context, DataStore.getUnsavedGeocodes(geocodes), false, false, false, onStartCallback);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

    }

    public static void storeCache(final Activity context, final Geocache cache, final boolean fastStoreOnLastSelection, @Nullable final Runnable onStartCallback) {
        if (Settings.getChooseList() || cache.isOffline()) {
            // let user select list to store cache in
            new StoredList.UserInterface(context).promptForMultiListSelection(R.string.lists_title, selectedListIds -> downloadCachesInternal(context, Collections.singleton(cache.getGeocode()), selectedListIds, false, true, onStartCallback), true, cache.getLists(), fastStoreOnLastSelection);
        } else {
            downloadCachesInternal(context, Collections.singleton(cache.getGeocode()), Collections.singleton(StoredList.STANDARD_LIST_ID), false, true, onStartCallback);
        }
    }

    public static void refreshCache(final Activity context, final String geocode, final boolean isOffline, @Nullable final Runnable onStartCallback) {
        askForListsIfNecessaryAndDownload(context, Collections.singleton(geocode), isOffline, true, isOffline, onStartCallback);
    }

    private static void askForListsIfNecessaryAndDownload(final Activity context, final Collection<String> geocodes, final boolean keepExistingLists, final boolean forceRedownload, final boolean isOffline, @Nullable final Runnable onStartCallback) {
        if (isOffline) {
            downloadCachesInternal(context, geocodes, null, keepExistingLists, forceRedownload, onStartCallback);
        } else if (Settings.getChooseList()) {
            // let user select list to store cache in
            new StoredList.UserInterface(context).promptForMultiListSelection(keepExistingLists ? R.string.lists_title_new_caches : R.string.lists_title, selectedListIds -> downloadCachesInternal(context, geocodes, selectedListIds, keepExistingLists, forceRedownload, onStartCallback), true, Collections.emptySet(), false);
        } else {
            downloadCachesInternal(context, geocodes, Collections.singleton(StoredList.STANDARD_LIST_ID), keepExistingLists, forceRedownload, onStartCallback);
        }
    }

    private static void downloadCachesInternal(final Activity context, final Collection<String> geocodes, @Nullable final Set<Integer> listIds, final boolean keepExistingLists, final boolean forceRedownload, @Nullable final Runnable onStartCallback) {

        final ArrayList<String> newGeocodes = new ArrayList<>();
        int cachesSkipped = 0;

        for (String geocode : geocodes) {
            // only enqueue online connectors
            if (ConnectorFactory.getConnector(geocode).hasOnlineSource()) {
                final DownloadTaskProperties properties = new DownloadTaskProperties(listIds, keepExistingLists, forceRedownload);
                final boolean isNewGeocode;
                synchronized (downloadQuery) {
                    isNewGeocode = downloadQuery.get(geocode) == null;
                    properties.merge(downloadQuery.get(geocode));
                    downloadQuery.put(geocode, properties);
                }
                if (isNewGeocode) {
                    newGeocodes.add(geocode);
                }
            } else {
                cachesSkipped++;
            }
        }

        if (cachesSkipped > 0) {
            ViewUtils.showShortToast(context, String.format(Locale.getDefault(), context.getString(R.string.caches_store_background_skipped), cachesSkipped));
        }
        if (newGeocodes.isEmpty()) {
            return;
        }

        Log.d("DOWNLOAD: " + newGeocodes);

        addGeocodes(context, CacheDownloaderService.class, newGeocodes);
        ViewUtils.showToast(context, R.string.download_started);

        if (onStartCallback != null) {
            onStartCallback.run();
        }
    }

    public static void requestStopService() {
        requestStop(CacheDownloaderService.class);
    }

    @Override
    protected int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_CACHES_DOWNLOADER;
    }

    @Override
    protected int getTitle() {
        return R.string.caches_store_background_title;
    }

    @Override
    protected NotificationChannels getCompletionNotificationChannel() {
        return NotificationChannels.CACHES_DOWNLOADED_NOTIFICATION;
    }

    @Override
    protected String getCompletedText(final int processedCount) {
        if (processedCount == 1) {
            return null; // see #15881 — suppress notification for a single download
        }
        return LocalizationUtils.getPlural(R.plurals.caches_store_background_result, processedCount, processedCount);
    }

    @Override
    @NonNull
    protected String getInterruptedText(final boolean wasCanceled, final int processedCount, final int totalCount) {
        return LocalizationUtils.getString(
                wasCanceled ? R.string.caches_store_background_result_canceled : R.string.caches_store_background_result_failed,
                processedCount, totalCount);
    }

    @Override
    protected void onHandleIntent(final @Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        Log.d("Download task started");

        // Drain all pending geocodes in parallel, looping to pick up any that were added
        // while a previous parallel batch was in flight (same merged-batch semantics as
        // the sequential base-class loop).
        final BatchState state = getCurrentState();
        while (!state.shouldStop) {
            final List<String> batch;
            synchronized (state.pending) {
                if (state.pending.isEmpty()) {
                    break;
                }
                batch = new ArrayList<>(state.pending);
            }
            Observable.fromIterable(batch)
                    .flatMap((Function<String, Observable<String>>) geocode -> Observable.create((ObservableOnSubscribe<String>) emitter -> {
                        processGeocodeWithTracking(geocode);
                        emitter.onComplete();
                    }).subscribeOn(AndroidRxUtils.refreshScheduler))
                    .blockingSubscribe();
        }

        Log.d("Download task completed");
    }

    @Override
    protected boolean processGeocode(final String geocode) {
        if (getCurrentState().shouldStop) {
            Log.i("download canceled");
            return false;
        }

        Log.d("Download " + geocode + " started");

        final DownloadTaskProperties properties;
        synchronized (downloadQuery) {
            properties = downloadQuery.put(geocode, null); // null marks the entry as "in progress"
        }
        if (properties == null) {
            throw new IllegalStateException("The cache is not present in the download query");
        }

        // merge current lists and additional lists
        final Set<Integer> combinedListIds = new HashSet<>(properties.listIds);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache != null && !cache.getLists().isEmpty()) {
            if (properties.keepExistingLists) {
                combinedListIds.clear();
            }
            combinedListIds.addAll(cache.getLists());
        }

        if (Geocache.storeCache(null, geocode, combinedListIds, properties.forceDownload, null)) {
            // notify foreground activities that they may need to update their content
            GeocacheChangedBroadcastReceiver.sendBroadcast(this, geocode);
            synchronized (downloadQuery) {
                if (downloadQuery.get(geocode) == null) {
                    // value still null → not re-queued during download, safe to remove
                    downloadQuery.remove(geocode);
                }
            }
            Log.d("Download " + geocode + " completed");
            return true;
        }

        Log.d("Download " + geocode + " failed");
        synchronized (downloadQuery) {
            if (downloadQuery.get(geocode) == null) {
                downloadQuery.remove(geocode);
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        downloadQuery.clear();
        super.onDestroy();
    }

    private static class DownloadTaskProperties {
        final Set<Integer> listIds = new HashSet<>();
        boolean forceDownload;
        boolean keepExistingLists;

        private DownloadTaskProperties(@Nullable final Set<Integer> listIds, final boolean keepExistingLists, final boolean forceDownload) {
            if (listIds != null) {
                this.listIds.addAll(listIds);
            }
            this.keepExistingLists = keepExistingLists;
            this.forceDownload = forceDownload;
        }

        public DownloadTaskProperties merge(@Nullable final DownloadTaskProperties additionalProperties) {
            if (additionalProperties != null) {
                this.listIds.addAll(additionalProperties.listIds);
                this.keepExistingLists |= additionalProperties.keepExistingLists;
                this.forceDownload |= additionalProperties.forceDownload;
            }
            return this;
        }
    }
}
