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

package cgeo.geocaching.service

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.notifications.NotificationChannels
import cgeo.geocaching.ui.notifications.Notifications
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioGroup

import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.Set
import java.util.concurrent.atomic.AtomicInteger

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.functions.Function

class CacheDownloaderService : AbstractForegroundIntentService() {
    static {
        logTag = "CacheDownloaderService"
    }

    private static val EXTRA_GEOCODES: String = "extra_geocodes"

    private static volatile Boolean shouldStop = false
    private static val downloadQuery: Map<String, DownloadTaskProperties> = HashMap<>()

    val cachesDownloaded: AtomicInteger = AtomicInteger()

    public static Boolean isDownloadPending(final String geocode) {
        return downloadQuery.containsKey(geocode)
    }

    public static Boolean isDownloadPending(final Geocache geocache) {
        return isDownloadPending(geocache.getGeocode())
    }

    public static Unit downloadCaches(final Activity context, final Collection<String> geocodes, final Boolean defaultForceRedownload, final Boolean isOffline, final Runnable onStartCallback) {
        if (geocodes.isEmpty()) {
            ActivityMixin.showToast(context, context.getString(R.string.warn_save_nothing))
            return
        }
        if (isOffline) {
            downloadCachesInternal(context, geocodes, null, false, defaultForceRedownload, onStartCallback)
            return
        }
        if (DataStore.getUnsavedGeocodes(geocodes).size() == geocodes.size()) {
            askForListsIfNecessaryAndDownload(context, geocodes, false, false, false, onStartCallback)
            return
        }

        // some caches are already stored offline, thus show the advanced selection dialog

        val content: View = LayoutInflater.from(context).inflate(R.layout.dialog_background_download_config, null)
        val radioGroup: RadioGroup = content.findViewById(R.id.radioGroup)

        Dialogs.newBuilder(context)
                .setView(content)
                .setTitle(R.string.caches_store_background_title)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    val id: Int = radioGroup.getCheckedRadioButtonId()
                    if (id == R.id.radio_button_refresh_and_add) {
                        askForListsIfNecessaryAndDownload(context, geocodes, false, true, false, onStartCallback)
                    } else if (id == R.id.radio_button_refresh_and_keep) {
                        // downloadCachesInternal(context, geocodes, null, true, onStartCallback)
                        askForListsIfNecessaryAndDownload(context, geocodes, true, true, false, onStartCallback)
                    } else if (id == R.id.radio_button_add_to_list) {
                        askForListsIfNecessaryAndDownload(context, geocodes, false, false, false, onStartCallback)
                    } else {
                        askForListsIfNecessaryAndDownload(context, DataStore.getUnsavedGeocodes(geocodes), false, false, false, onStartCallback)
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show()

    }

    public static Unit storeCache(final Activity context, final Geocache cache, final Boolean fastStoreOnLastSelection, final Runnable onStartCallback) {
        if (Settings.getChooseList() || cache.isOffline()) {
            // let user select list to store cache in
            StoredList.UserInterface(context).promptForMultiListSelection(R.string.lists_title, selectedListIds -> downloadCachesInternal(context, Collections.singleton(cache.getGeocode()), selectedListIds, false, true, onStartCallback), true, cache.getLists(), fastStoreOnLastSelection)
        } else {
            downloadCachesInternal(context, Collections.singleton(cache.getGeocode()), Collections.singleton(StoredList.STANDARD_LIST_ID), false, true, onStartCallback)
        }
    }

    public static Unit refreshCache(final Activity context, final String geocode, final Boolean isOffline, final Runnable onStartCallback) {
        askForListsIfNecessaryAndDownload(context, Collections.singleton(geocode), isOffline, true, isOffline, onStartCallback)
    }

    private static Unit askForListsIfNecessaryAndDownload(final Activity context, final Collection<String> geocodes, final Boolean keepExistingLists, final Boolean forceRedownload, final Boolean isOffline, final Runnable onStartCallback) {
        if (isOffline) {
            downloadCachesInternal(context, geocodes, null, keepExistingLists, forceRedownload, onStartCallback)
        } else if (Settings.getChooseList()) {
            // let user select list to store cache in
            StoredList.UserInterface(context).promptForMultiListSelection(keepExistingLists ? R.string.lists_title_new_caches : R.string.lists_title, selectedListIds -> downloadCachesInternal(context, geocodes, selectedListIds, keepExistingLists, forceRedownload, onStartCallback), true, Collections.emptySet(), false)
        } else {
            downloadCachesInternal(context, geocodes, Collections.singleton(StoredList.STANDARD_LIST_ID), keepExistingLists, forceRedownload, onStartCallback)
        }
    }

    private static Unit downloadCachesInternal(final Activity context, final Collection<String> geocodes, final Set<Integer> listIds, final Boolean keepExistingLists, final Boolean forceRedownload, final Runnable onStartCallback) {

        val newGeocodes: ArrayList<String> = ArrayList<>()

        for (String geocode : geocodes) {
            val properties: DownloadTaskProperties = DownloadTaskProperties(listIds, keepExistingLists, forceRedownload)
            final Boolean isNewGeocode
            synchronized (downloadQuery) {
                isNewGeocode = downloadQuery.get(geocode) == null
                properties.merge(downloadQuery.get(geocode))
                downloadQuery.put(geocode, properties)
            }
            if (isNewGeocode) {
                newGeocodes.add(geocode)
            }
        }

        if (newGeocodes.isEmpty()) {
            return
        }

        Log.d("DOWNLOAD: " + newGeocodes)

        val intent: Intent = Intent(context, CacheDownloaderService.class)
        intent.putStringArrayListExtra(EXTRA_GEOCODES, newGeocodes)
        ContextCompat.startForegroundService(context, intent)
        ViewUtils.showToast(context, R.string.download_started)

        if (onStartCallback != null) {
            onStartCallback.run()
        }
    }

    public static Unit requestStopService() {
        shouldStop = true
    }

    override     public NotificationCompat.Builder createInitialNotification() {
        shouldStop = false
        val actionCancelIntent: PendingIntent = PendingIntent.getBroadcast(this, 0,
                Intent(this, StopCacheDownloadServiceReceiver.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)

        return Notifications.createNotification(this, NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION, R.string.caches_store_background_title)
                .setProgress(100, 0, true)
                .addAction(R.drawable.ic_menu_cancel, getString(android.R.string.cancel), actionCancelIntent)
    }

    override     protected Int getForegroundNotificationId() {
        return Notifications.ID_FOREGROUND_NOTIFICATION_CACHES_DOWNLOADER
    }

    override     protected Unit onHandleIntent(final Intent intent) {
        if (intent == null) {
            return
        }

        // schedule download on multiple threads...

        Log.d("Download task started")

        val geocodes: Observable<String> = Observable.fromIterable(intent.getStringArrayListExtra(EXTRA_GEOCODES))
        geocodes.flatMap((Function<String, Observable<String>>) geocode -> Observable.create((ObservableOnSubscribe<String>) emitter -> {
            handleDownload(geocode)
            emitter.onComplete()
        }).subscribeOn(AndroidRxUtils.refreshScheduler)).blockingSubscribe()

        Log.d("Download task completed")
    }

    private Unit handleDownload(final String geocode) {
        try {
            if (shouldStop) {
                Log.i("download canceled")
                return
            }

            Log.d("Download #" + cachesDownloaded.get() + " " + geocode + " started")

            final DownloadTaskProperties properties
            synchronized (downloadQuery) {
                properties = downloadQuery.put(geocode, null); // set the properties to null, to point out that the download is currently ongoing

            }
            if (properties == null) {
                throw IllegalStateException("The cache is not present in the download query")
            }

            // update foreground service notification
            notification.setProgress(downloadQuery.size() + cachesDownloaded.get(), cachesDownloaded.get(), false)
            notification.setContentText(cachesDownloaded.get() + "/" + (downloadQuery.size() + cachesDownloaded.get()))
            updateForegroundNotification()

            // merge current lists and additional lists
            val combinedListIds: Set<Integer> = HashSet<>(properties.listIds)
            val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
            if (cache != null && !cache.getLists().isEmpty()) {
                if (properties.keepExistingLists) {
                    combinedListIds.clear()
                }
                combinedListIds.addAll(cache.getLists())
            }

            // download...
            if (Geocache.storeCache(null, geocode, combinedListIds, properties.forceDownload, null)) {
                // send a broadcast so that foreground activities know that they might need to update their content
                GeocacheChangedBroadcastReceiver.sendBroadcast(this, geocode)
                // check whether the download properties are still null,
                // otherwise there is a download task...
                synchronized (downloadQuery) {
                    if (downloadQuery.get(geocode) == null) {
                        downloadQuery.remove(geocode)
                    }
                }
                Log.d("Download #" + cachesDownloaded.get() + " " + geocode + " completed")
                cachesDownloaded.incrementAndGet()
            } else {
                Log.d("Download #" + cachesDownloaded.get() + " " + geocode + " failed")
            }
        } catch (Exception ex) {
            Log.e("exception while background download", ex)
        }
    }

    override     public Unit onDestroy() {
        if (!downloadQuery.isEmpty()) {
            showEndNotification(getString(shouldStop ? R.string.caches_store_background_result_canceled : R.string.caches_store_background_result_failed,
                    cachesDownloaded.get(), cachesDownloaded.get() + downloadQuery.size()))
        } else if (cachesDownloaded.get() != 1) { // see #15881
            showEndNotification(getResources().getQuantityString(R.plurals.caches_store_background_result, cachesDownloaded.get(), cachesDownloaded.get()))
        }
        downloadQuery.clear()
        super.onDestroy()
    }

    private Unit showEndNotification(final String text) {
        Notifications.send(this, Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                this, NotificationChannels.CACHES_DOWNLOADED_NOTIFICATION, R.string.caches_store_background_title, text).setSilent(true))
    }

    private static class DownloadTaskProperties {
        val listIds: Set<Integer> = HashSet<>()
        Boolean forceDownload
        Boolean keepExistingLists

        private DownloadTaskProperties(final Set<Integer> listIds, final Boolean keepExistingLists, final Boolean forceDownload) {
            if (listIds != null) {
                this.listIds.addAll(listIds)
            }
            this.keepExistingLists = keepExistingLists
            this.forceDownload = forceDownload
        }

        public DownloadTaskProperties merge(final DownloadTaskProperties additionalProperties) {
            if (additionalProperties != null) {
                this.listIds.addAll(additionalProperties.listIds)
                this.keepExistingLists |= additionalProperties.keepExistingLists
                this.forceDownload |= additionalProperties.forceDownload
            }
            return this
        }
    }
}
