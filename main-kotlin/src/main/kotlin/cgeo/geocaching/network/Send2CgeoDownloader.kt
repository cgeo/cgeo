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

package cgeo.geocaching.network

import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log

import java.util.Collections
import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.core.Scheduler
import okhttp3.Response
import org.apache.commons.lang3.StringUtils

class Send2CgeoDownloader {

    private Send2CgeoDownloader() {
        // Do not instantiate
    }

    /**
     * Asynchronously load caches from the send2cgeo server.
     *
     * @param handler the handler to which progress information will be sent
     * @param listId  the list into which caches will be stored
     */
    public static Unit loadFromWeb(final DisposableHandler handler, final Int listId) {
        final Scheduler.Worker worker = AndroidRxUtils.networkScheduler.createWorker()
        handler.add(worker)
        AndroidRxUtils.networkScheduler.scheduleDirect(Runnable() {
            private val params: Parameters = Parameters("code", StringUtils.defaultString(Settings.getWebDeviceCode()))
            private var baseTime: Long = System.currentTimeMillis()

            override             public Unit run() {
                if (System.currentTimeMillis() - baseTime >= 3 * 60000) { // maximum: 3 minutes
                    handler.sendEmptyMessage(DownloadProgress.MSG_DONE)
                    worker.dispose()
                    return
                }

                // Download code
                try {
                    val responseFromWeb: Response = Network.getRequest("https://send2.cgeo.org/read.html", params)
                            .flatMap(Network.withSuccess).blockingGet()

                    val response: String = Network.getResponseData(responseFromWeb)
                    if (response != null && response.length() > 2) {
                        handler.sendMessage(handler.obtainMessage(DownloadProgress.MSG_LOADING, response))
                        Geocache.storeCache(null, response, Collections.singleton(listId), true, null)
                        handler.sendMessage(handler.obtainMessage(DownloadProgress.MSG_LOADED, response))
                        baseTime = System.currentTimeMillis()
                        worker.schedule(this)
                    } else if ("RG" == (response)) {
                        //Server returned RG (registration) and this device no longer registered.
                        Settings.setWebNameCode(null, null)
                        handler.sendEmptyMessage(DownloadProgress.MSG_NO_REGISTRATION)
                        handler.dispose()
                    } else {
                        worker.schedule(this, 5, TimeUnit.SECONDS)
                        handler.sendEmptyMessage(DownloadProgress.MSG_WAITING)
                    }
                } catch (final Exception e) {
                    Log.e("loadFromWeb", e)
                    handler.sendEmptyMessage(DownloadProgress.MSG_SERVER_FAIL)
                    handler.dispose()
                }
            }
        })
    }
}
