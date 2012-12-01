package cgeo.geocaching.downloadservice;

import cgeo.geocaching.DownloadManagerActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.Log;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CacheDownloadService extends Service {

    private volatile boolean downloadTaskRunning = true;

    private QueueItem actualCache;

    private long lastCacheAdded = 0;

    private RemoteCallbackList<ICacheDownloadServiceCallback> callbackList = new RemoteCallbackList<ICacheDownloadServiceCallback>();

    public static final int CGEO_DOWNLOAD_NOTIFICATION_ID = 91258;

    public static final String EXTRA_LIST_ID = "LIST_ID";
    public static final String EXTRA_GEOCODE = "GEOCODE";
    public static final String EXTRA_REFRESH = "REFRESH";

    private int downloadedCaches = 0;

    LinkedBlockingQueue<QueueItem> queue = new LinkedBlockingQueue<QueueItem>();

    private enum OperationType {
        STORE,
        REFRESH
    }

    /**
     * item container for processing queue
     */
    private class QueueItem {
        private final String geocode;
        private final int startId;
        private final int listId;
        private final OperationType operation;

        public QueueItem(String cacheCode, int startId, int listId, OperationType type) {
            this.startId = startId;
            this.geocode = cacheCode;
            this.listId = listId;
            this.operation = type;
        }

        public QueueItem(String geocode) {
            this(geocode, 0, 0, OperationType.STORE);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) { // same references
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            QueueItem other = (QueueItem) obj;
            if ((geocode == null) && (other.geocode == null)) {
                return true; // both geocodes are null
            }
            if (geocode == null) {
                return false;
            }
            if (!geocode.equals(other.geocode)) {
                return false;
            }
            return true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        if (!intent.hasExtra(EXTRA_GEOCODE)) {
            return START_NOT_STICKY;
        }

        int listId = intent.getIntExtra(EXTRA_LIST_ID, StoredList.STANDARD_LIST_ID);
        QueueItem item =
                new QueueItem(intent.getStringExtra(EXTRA_GEOCODE),
                        startId,
                        listId,
                        intent.hasExtra(EXTRA_REFRESH) ? OperationType.REFRESH : OperationType.STORE);
        try {
            if (!queue.contains(item)) {
                queue.put(item);
                if (lastCacheAdded + 1000 < System.currentTimeMillis()) {
                    ActivityMixin.showShortToast(this, (getString(R.string.download_service_queued_cache, item.geocode)));
                    lastCacheAdded = System.currentTimeMillis();
                }
                notifyChanges();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!downloadTaskRunning) {
            downloadTaskRunning = true;
            new DownloadCachesTask().execute();
        }
        return START_STICKY;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Used to notify connected DownloadManagerActivity via callback
     *
     * @param finish
     *            - true for sending finish event, otherwise refresh
     */
    private void notifyClients(boolean finish) {
        synchronized (callbackList) {
            final int maxClientId = callbackList.beginBroadcast();
            for (int i = 0; i < maxClientId; i++) {
                try {
                    ICacheDownloadServiceCallback cb = callbackList.getBroadcastItem(i);
                    if (finish) {
                        cb.notifyFinish();
                    } else {
                        cb.notifyRefresh(queue.size());
                    }
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            callbackList.finishBroadcast();
        }
    }

    /**
     * Used to notify client that state has changed
     *
     * @param finish
     *            - true for finishing of app
     */
    private void notifyChanges(boolean finish) {
        notifyClients(finish);
        showNotification();
    }

    /**
     * @see notifyChanges
     *      with default finish value (false)
     */
    private synchronized void notifyChanges() {
        notifyChanges(false);
    }

    /**
     * display statusbar notification depending on service state
     */
    @TargetApi(5)
    private void showNotification() {
        if (actualCache == null) {
            return;
        }
        String status = getResources().getQuantityString(R.plurals.download_service_status, queue.size(), actualCache.geocode, queue.size());
        Notification notification = new Notification(R.drawable.icon_sync, null, System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, DownloadManagerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(getApplicationContext(), getString(R.string.download_service_name), status, contentIntent);
        startForeground(CGEO_DOWNLOAD_NOTIFICATION_ID, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("CACHEDOWNLOADSERVICE START");
        notifyChanges();
        new DownloadCachesTask().execute();
    }

    /**
     * Transforms queue to array of geocodes
     *
     * @return transformed array
     */
    public String[] queueAsArray() {
        Object[] queueAsArray = queue.toArray();
        String[] result = new String[queueAsArray.length];

        for (int i = 0; i < queueAsArray.length; i++) {
            result[i] = ((QueueItem) (queueAsArray[i])).geocode;
        }
        return result;
    }

    /**
     * IPC interface for binding the service
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return new ICacheDownloadService.Stub() {
            @Override
            public int queueStatus() throws RemoteException {
                return queue.size();
            }

            @Override
            public String[] queuedCodes() throws RemoteException {
                return queueAsArray();
            }

            @Override
            public String actualDownload() throws RemoteException {
                if (actualCache != null) {
                    return actualCache.geocode;
                }
                return null;
            }

            @Override
            public void registerStatusCallback(ICacheDownloadServiceCallback cdsc) throws RemoteException {
                if (cdsc != null) {
                    callbackList.register(cdsc);
                }
            }

            @Override
            public void unregisterStatusCallback(ICacheDownloadServiceCallback cdsc) throws RemoteException {
                if (cdsc != null) {
                    callbackList.unregister(cdsc);
                }
            }

            @Override
            public void removeFromQueue(String geocode) throws RemoteException {
                if (queue.remove(new QueueItem(geocode))) {
                    notifyClients(false);

                    ActivityMixin.showShortToast(CacheDownloadService.this, getString(R.string.download_service_removed_cache, geocode));
                } else {
                    ActivityMixin.showShortToast(CacheDownloadService.this, getString(R.string.download_service_not_removed_cache, geocode));
                }
            }

            @Override
            public void flushQueueAndStopService() throws RemoteException {
                queue.clear();
                notifyClients(true);
                ActivityMixin.showShortToast(CacheDownloadService.this, getString(R.string.download_service_flushed_queue_and_exiting));
                CacheDownloadService.this.stopSelf();
            }

            @Override
            public void pauseDownloading() throws RemoteException {
                throw new UnsupportedOperationException("NOT IMPLEMENTED");
            }

            @Override
            public void resumeDownloading() throws RemoteException {
                throw new UnsupportedOperationException("NOT IMPLEMENTED");
            }
        };
    }

    @Override
    public void onDestroy() {
        notifyClients(true);
        downloadTaskRunning = false;
        Log.d("CACHEDOWNLOADSERVICE STOP");
        ActivityMixin.showShortToast(this, getString(R.string.download_service_finished));
        callbackList.kill();
        super.onDestroy();
    }

    /**
     * Background downloading of caches and queue processing
     */
    private class DownloadCachesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            do {
                try {
                    actualCache = queue.poll(5, TimeUnit.SECONDS);
                    //Random wait between caches to prevent hogging of servers
                    Thread.sleep((long) (Math.random() * 5000) + 1000);
                } catch (InterruptedException e) {
                    actualCache = null;
                }

                if (actualCache != null) {
                    Log.d("CACHEDOWNLOAD STARTING DOWNLOAD" + actualCache.geocode);
                    notifyChanges();
                    cgCache cache = new cgCache();
                    cache.setGeocode(actualCache.geocode);
                    cache.setListId(actualCache.listId);

                    switch (actualCache.operation) {
                        case STORE:
                            //TODO: use handler in the future to report more granular progress
                            cache.store(null);
                            break;
                        case REFRESH:
                            //TODO: use handler in the future to report more granular progress
                            cache.refresh(actualCache.listId, null);
                            break;
                    }

                    Log.d("CACHEDOWNLOAD FINISHED DOWNLOAD" + actualCache.geocode);

                    downloadedCaches++;

                    notifyChanges(queue.isEmpty());
                    sleep((int) (Math.random() * 3000) + 500);
                    stopSelf(actualCache.startId);
                }

            } while (actualCache != null || downloadTaskRunning);
            return null;
        }
    }
}