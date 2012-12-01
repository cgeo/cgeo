package cgeo.geocaching.downloadservice;

import cgeo.geocaching.DownloadManagerActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

public class Send2CgeoService extends Service {

    private RemoteCallbackList<ISend2CgeoServiceCallback> callbackList = new RemoteCallbackList<ISend2CgeoServiceCallback>();
    private volatile boolean running = true;
    public static int CGEO_SEND2CGEO_NOTIFICATION_ID = 9455242;

    /**
     * IPC interface for binding the service
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return new ISend2CgeoService.Stub() {
            @Override
            public void unregisterStatusCallback(ISend2CgeoServiceCallback cdsc) throws RemoteException {
                if (cdsc != null) {
                    callbackList.unregister(cdsc);
                }

            }

            @Override
            public void registerStatusCallback(ISend2CgeoServiceCallback cdsc) throws RemoteException {
                if (cdsc != null) {
                    callbackList.register(cdsc);
                }
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        s2cgeoTask.execute();
        showNotification("Started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        s2cgeoTask.restart(startId);
        return START_STICKY;
    }

    /**
     * display statusbar notification depending on service state
     */
    @TargetApi(5)
    private void showNotification(String status) {
        Notification notification = new Notification(R.drawable.icon_sync, null, System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, DownloadManagerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(getApplicationContext(), "Send2cgeo service", status, contentIntent);
        startForeground(CGEO_SEND2CGEO_NOTIFICATION_ID, notification);
    }

    private void notifySend2CgeoStatus(int status) {
        notifySend2CgeoStatus(status, null);
    }

    private void notifySend2CgeoStatus(int status, String geocode) {
        synchronized (callbackList) {
            final int N = callbackList.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    ISend2CgeoServiceCallback cb = callbackList.getBroadcastItem(i);
                    cb.notifySend2CgeoStatus(status, geocode);
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            callbackList.finishBroadcast();
        }
    }

    public static final int SEND2CGEO_WAITING = 0;
    public static final int SEND2CGEO_DOWNLOAD_START = 1;
    public static final int SEND2CGEO_DOWNLOAD_FAILED = 2;
    public static final int SEND2CGEO_REGISTER = 3;
    public static final int SEND2CGEO_DONE = 4;
    public static final int SEND2CGEO_STARTED = 5;

    private final Send2CgeoRequestTask s2cgeoTask = new Send2CgeoRequestTask();

    class Send2CgeoRequestTask extends AsyncTask<Void, String, Void> {
        volatile int times = 0;
        int ret = SEND2CGEO_DONE;
        private int startId;
        private static final int maxTimes = 3 * 60 / 5; // maximum: 3 minutes, every 5 seconds

        @Override
        protected Void doInBackground(Void... arg0) {

            int delay = -1;

            while (running)
            {
                //download new code
                String deviceCode = Settings.getWebDeviceCode();
                if (deviceCode == null) {
                    deviceCode = "";
                }
                final Parameters params = new Parameters("code", deviceCode);
                HttpResponse responseFromWeb = Network.getRequest("http://send2.cgeo.org/read.html", params);

                if (responseFromWeb != null && responseFromWeb.getStatusLine().getStatusCode() == 200) {
                    final String response = Network.getResponseData(responseFromWeb);
                    if (response.length() > 2) {
                        delay = 1;

                        Intent i = new Intent(Send2CgeoService.this, CacheDownloadService.class);
                        i.putExtra(CacheDownloadService.EXTRA_GEOCODE, response);
                        //TODO: put list ID ... which ?
                        startService(i);
                        notifySend2CgeoStatus(SEND2CGEO_DOWNLOAD_START, response);
                    } else if ("RG".equals(response)) {
                        //Server returned RG (registration) and this device no longer registered.
                        Settings.setWebNameCode(null, null);
                        ret = SEND2CGEO_REGISTER;
                        publishProgress(getString(R.string.sendToCgeo_no_registration));
                        running = false;
                        break;
                    } else {
                        delay = 0;
                        notifySend2CgeoStatus(SEND2CGEO_WAITING, "" + (maxTimes - times));
                    }
                }
                if (responseFromWeb == null || responseFromWeb.getStatusLine().getStatusCode() != 200) {
                    running = false;
                    ret = SEND2CGEO_DOWNLOAD_FAILED;
                    break;
                }

                if (delay == 0)
                {
                    sleep(5000); //No caches 5s
                    times++;
                } else {
                    sleep(500); //Cache was loaded 0.5s
                    times = 0;
                }

                if (times >= maxTimes) {
                    // stop when no cache comes in last 3 minutes
                    stopSelf();
                }
            }

            notifySend2CgeoStatus(ret);
            if (ret == SEND2CGEO_DONE) {
                publishProgress(getString(R.string.download_service_send2cgefinished));
            }
            Log.d("Send2cgeo stopping");
            stopSelf();
            return null;
        }

        public void restart(int startId) {
            times = 0;
            this.startId = startId;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            ActivityMixin.showShortToast(Send2CgeoService.this, values[0]);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }
}
