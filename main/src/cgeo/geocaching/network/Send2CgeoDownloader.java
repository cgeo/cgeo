package cgeo.geocaching.network;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.RxUtils;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;

import rx.Scheduler.Worker;
import rx.functions.Action0;

import java.util.concurrent.TimeUnit;

public class Send2CgeoDownloader {

    public static final int MSG_DONE = -1;
    public static final int MSG_SERVER_FAIL = -2;
    public static final int MSG_NO_REGISTRATION = -3;
    public static final int MSG_WAITING = 0;
    public static final int MSG_LOADING = 1;
    public static final int MSG_LOADED = 2;

    private Send2CgeoDownloader() {
        // Do not instantiate
    }

    /**
     * Asynchronously load caches from the send2cgeo server.
     *
     * @param handler the handler to which progress information will be sent
     * @param listId the list into which caches will be stored
     */
    public static void loadFromWeb(final CancellableHandler handler, final int listId) {
        final Worker worker = RxUtils.networkScheduler.createWorker();
        handler.unsubscribeIfCancelled(worker);
        worker.schedule(new Action0() {
            private final Parameters PARAMS = new Parameters("code", StringUtils.defaultString(Settings.getWebDeviceCode()));
            private long baseTime = System.currentTimeMillis();

            @Override
            public void call() {
                if (System.currentTimeMillis() - baseTime >= 3 * 60000) { // maximum: 3 minutes
                    handler.sendEmptyMessage(MSG_DONE);
                    return;
                }

                // Download new code
                final HttpResponse responseFromWeb = Network.getRequest("http://send2.cgeo.org/read.html", PARAMS);

                if (responseFromWeb != null && responseFromWeb.getStatusLine().getStatusCode() == 200) {
                    final String response = Network.getResponseData(responseFromWeb);
                    if (response != null && response.length() > 2) {
                        handler.sendMessage(handler.obtainMessage(MSG_LOADING, response));
                        Geocache.storeCache(null, response, listId, false, null);
                        handler.sendMessage(handler.obtainMessage(MSG_LOADED, response));
                        baseTime = System.currentTimeMillis();
                        worker.schedule(this);
                    } else if ("RG".equals(response)) {
                        //Server returned RG (registration) and this device no longer registered.
                        Settings.setWebNameCode(null, null);
                        handler.sendEmptyMessage(MSG_NO_REGISTRATION);
                        handler.cancel();
                    } else {
                        worker.schedule(this, 5, TimeUnit.SECONDS);
                        handler.sendEmptyMessage(MSG_WAITING);
                    }
                } else {
                    handler.sendEmptyMessage(MSG_SERVER_FAIL);
                    handler.cancel();
                }
            }
        });
    }
}
