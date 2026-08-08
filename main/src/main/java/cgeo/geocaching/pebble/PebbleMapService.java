package cgeo.geocaching.pebble;

import cgeo.geocaching.MainActivity;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeOfflineTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeVTMOfflineTileProvider;
import cgeo.geocaching.utils.Log;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Foreground service that renders the current offline map from c:geo's Mapsforge
 * source and sends it to the Pebble watch. Keeps running while the phone is
 * locked or another app is in focus.
 */
public class PebbleMapService extends Service {

    private static final String LOG = "PebbleMapService";
    public static final String ACTION_STOP = "cgeo.geocaching.pebble.PebbleMapService.STOP";
    private static final String CHANNEL_ID = "cgeo_pebble_map";
    private static final int NOTIFICATION_ID = 54321;

    private final UUID uuid = PebbleMapConstants.PEBBLE_MAP_APP_UUID;

    private PebbleDataReceiver dataReceiver;
    private BroadcastReceiver registeredReceiver;
    private volatile Location currentLocation;
    private Disposable geoDataDisposable;

    private PebbleMapRenderer mapRenderer;
    private volatile int currentZoom = 15;
    private volatile boolean running = true;
    public static boolean isRunning = false;
    private HandlerThread renderThread;
    private Handler renderHandler;
    private final Runnable renderTask = this::renderAndSend;
    private final Object chunkToken = new Object();
    private static final int CHUNK_DELAY_MS = 0;
    private static final int MIN_FRAME_INTERVAL_MS = 4600;
    private boolean renderInProgress = false;
    private boolean pendingRender = false;
    private int frameId = 0;
    private long lastFrameStart;

    /**
     * Renderer used by the service to produce a 200x228 Pebble frame.
     */
    public interface PebbleMapRenderer {
        byte[] render(double latitude, double longitude, int zoom);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        Log.w(LOG + " onCreate");

        renderThread = new HandlerThread("PebbleMapRender");
        renderThread.start();
        renderHandler = new Handler(renderThread.getLooper());

        createNotificationChannel();
        startForeground();
        registerPebbleReceiver();

        final Object tileProvider = Settings.getTileProvider();
        Log.w(LOG + " tileProvider=" + (tileProvider == null ? "null" : tileProvider.getClass().getName()));
        List<Uri> mapUris = null;
        if (tileProvider instanceof AbstractMapsforgeOfflineTileProvider) {
            mapUris = ((AbstractMapsforgeOfflineTileProvider) tileProvider).getMapUris();
        } else if (tileProvider instanceof AbstractMapsforgeVTMOfflineTileProvider) {
            mapUris = ((AbstractMapsforgeVTMOfflineTileProvider) tileProvider).getMapUris();
        }
        if (mapUris != null && !mapUris.isEmpty() && !Uri.EMPTY.equals(mapUris.get(0))) {
            mapRenderer = new MapsforgePebbleRenderer(this, mapUris);
            Log.w(LOG + " Mapsforge renderer attached: " + mapUris);
        } else {
            Log.w(LOG + " no usable offline map source selected");
        }

        subscribeToLocation();

        PebbleKit.startAppOnPebble(this, uuid);
        Log.w(LOG + " launched watchapp");

        // mapRenderer and location subscription are set up above
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.w(LOG + " onStartCommand");
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w(LOG + " onDestroy");
        isRunning = false;
        running = false;
        if (registeredReceiver != null) {
            unregisterReceiver(registeredReceiver);
            registeredReceiver = null;
        }
        if (geoDataDisposable != null && !geoDataDisposable.isDisposed()) {
            geoDataDisposable.dispose();
            geoDataDisposable = null;
        }
        if (renderHandler != null) {
            renderHandler.removeCallbacksAndMessages(null);
        }
        if (renderThread != null) {
            renderThread.quitSafely();
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    private void triggerRender() {
        if (renderHandler != null) {
            renderHandler.post(this::maybeStartRender);
        }
    }

    private void maybeStartRender() {
        if (renderInProgress) {
            pendingRender = true;
            return;
        }
        final long now = SystemClock.elapsedRealtime();
        if (lastFrameStart > 0 && now < lastFrameStart + MIN_FRAME_INTERVAL_MS) {
            renderHandler.postDelayed(this::maybeStartRender, chunkToken, lastFrameStart + MIN_FRAME_INTERVAL_MS - now);
            return;
        }
        renderInProgress = true;
        pendingRender = false;
        renderHandler.removeCallbacks(renderTask);
        renderHandler.removeCallbacks(this::maybeStartRender);
        renderHandler.post(renderTask);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerPebbleReceiver() {
        dataReceiver = new PebbleDataReceiver(uuid) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                PebbleKit.sendAckToPebble(context, transactionId);
                handlePebbleData(data);
            }
        };
        final IntentFilter filter = new IntentFilter(Constants.INTENT_APP_RECEIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registeredReceiver = dataReceiver;
            registerReceiver(dataReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registeredReceiver = dataReceiver;
            registerReceiver(dataReceiver, filter);
        }
    }

    private void handlePebbleData(final PebbleDictionary data) {
        final Long cmd = data.getInteger(PebbleMapConstants.KEY_COMMAND);
        if (cmd == null) {
            return;
        }
        Log.w(LOG + " pebble command: " + cmd);
        final Long value = data.getInteger(PebbleMapConstants.KEY_VALUE);
        final int intValue = value != null ? value.intValue() : 0;
        switch (cmd.intValue()) {
            case PebbleMapConstants.CMD_REFRESH:
                triggerRender();
                break;
            case PebbleMapConstants.CMD_ZOOM_IN:
                currentZoom++;
                triggerRender();
                break;
            case PebbleMapConstants.CMD_ZOOM_OUT:
                currentZoom = Math.max(0, currentZoom - 1);
                triggerRender();
                break;
            case PebbleMapConstants.CMD_SET_ZOOM:
                currentZoom = intValue;
                triggerRender();
                break;
            default:
                Log.w(LOG + " unknown command: " + cmd);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(android.R.string.ok), // placeholder; should use a string resource
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Pebble map background service");
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForeground() {
        final Intent stopIntent = new Intent(this, PebbleMapService.class)
                .setAction(ACTION_STOP);
        final PendingIntent stopPending = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        final Intent contentIntent = new Intent(this, MainActivity.class);
        final PendingIntent contentPending = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE);

        final Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("c:geo Pebble Map")
                .setContentText("Sending live map to Pebble")
                .setContentIntent(contentPending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
                .build();

        final int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type);
    }

    private void subscribeToLocation() {
        try {
            final LocationDataProvider provider = LocationDataProvider.getInstance();
            provider.initialize();
            geoDataDisposable = provider.geoDataObservable(false)
                    .subscribe(geoData -> {
                        final GeoData safe = geoData == null ? GeoData.DUMMY_LOCATION : geoData;
                        currentLocation = safe;
                        Log.w(LOG + " location from " + safe.getProvider() + ": " + safe.getLatitude() + "," + safe.getLongitude());
                        triggerRender();
                    }, throwable -> Log.e(LOG + " location subscription error", throwable));
        } catch (final Exception e) {
            Log.e(LOG + " failed to subscribe to LocationDataProvider", e);
        }
    }

    private void renderAndSend() {
        pendingRender = false;
        if (!running || currentLocation == null) {
            Log.w(LOG + " renderAndSend skipped: running=" + running + " location=" + currentLocation);
            onFrameComplete();
            return;
        }
        lastFrameStart = SystemClock.elapsedRealtime();
        Log.w(LOG + " renderAndSend start zoom=" + currentZoom);

        final byte[] frame;
        if (mapRenderer != null) {
            frame = mapRenderer.render(currentLocation.getLatitude(), currentLocation.getLongitude(), currentZoom);
        } else {
            frame = new byte[PebbleMapConstants.MAP_WIDTH * PebbleMapConstants.MAP_HEIGHT];
        }

        if (frame == null || frame.length == 0) {
            Log.w(LOG + " renderAndSend got empty frame");
            onFrameComplete();
            return;
        }

        final int total = (frame.length + PebbleMapConstants.CHUNK_SIZE - 1) / PebbleMapConstants.CHUNK_SIZE;
        final int thisFrameId = ++frameId;
        renderHandler.removeCallbacksAndMessages(chunkToken);
        for (int i = 0; i < total; i++) {
            final int index = i;
            renderHandler.postDelayed(() -> sendChunk(index, total, thisFrameId, frame), chunkToken, (long) index * CHUNK_DELAY_MS);
        }
        renderHandler.postDelayed(this::onFrameComplete, chunkToken, (long) total * CHUNK_DELAY_MS + 100);
        Log.w(LOG + " renderAndSend scheduled " + total + " chunks");
    }

    private void onFrameComplete() {
        if (pendingRender && running && currentLocation != null) {
            pendingRender = false;
            final long delay = lastFrameStart + MIN_FRAME_INTERVAL_MS - SystemClock.elapsedRealtime();
            if (delay > 0) {
                renderHandler.postDelayed(renderTask, chunkToken, delay);
            } else {
                renderHandler.post(renderTask);
            }
        } else {
            pendingRender = false;
            renderInProgress = false;
        }
    }

    private void sendChunk(final int index, final int total, final int thisFrameId, final byte[] frame) {
        final int start = index * PebbleMapConstants.CHUNK_SIZE;
        final int end = Math.min(start + PebbleMapConstants.CHUNK_SIZE, frame.length);
        final int length = end - start;
        final byte[] chunk = new byte[length];
        System.arraycopy(frame, start, chunk, 0, length);
        final PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(PebbleMapConstants.KEY_CHUNK_INDEX, index);
        dict.addInt32(PebbleMapConstants.KEY_CHUNK_TOTAL, total);
        dict.addInt32(PebbleMapConstants.KEY_FRAME_ID, thisFrameId);
        dict.addBytes(PebbleMapConstants.KEY_CHUNK_DATA, chunk);
        PebbleKit.sendDataToPebble(this, uuid, dict);
        Log.w(LOG + " sendChunk " + (index + 1) + "/" + total + " frame=" + thisFrameId);
    }
}
