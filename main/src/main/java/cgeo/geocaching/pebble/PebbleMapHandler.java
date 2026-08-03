package cgeo.geocaching.pebble;

import cgeo.geocaching.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;

/**
 * Manages Pebble AppMessage communication for the c:geo live map watchapp.
 */
public class PebbleMapHandler {

    private static final String LOG = "PebbleMapHandler";

    private final Context context;
    private final PebbleMapHost host;
    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final PebbleDataReceiver dataReceiver;
    private BroadcastReceiver registeredReceiver;
    private int refreshIntervalSeconds = 5;
    private boolean autoRefreshActive = true;

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            requestMapCapture();
            if (autoRefreshActive && refreshIntervalSeconds > 0) {
                autoRefreshHandler.postDelayed(this, refreshIntervalSeconds * 1000L);
            }
        }
    };

    public PebbleMapHandler(final Context context, final PebbleMapHost host) {
        this.context = context.getApplicationContext();
        this.host = host;
        this.dataReceiver = new PebbleDataReceiver(PebbleMapConstants.PEBBLE_MAP_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                PebbleKit.sendAckToPebble(context, transactionId);
                handlePebbleData(data);
            }
        };
    }

    public void start() {
        if (registeredReceiver == null) {
            final IntentFilter filter = new IntentFilter(Constants.INTENT_APP_RECEIVE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.registerReceiver(dataReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(dataReceiver, filter);
            }
            registeredReceiver = dataReceiver;
        }
        scheduleAutoRefresh();
    }

    public void stop() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        if (registeredReceiver != null) {
            context.unregisterReceiver(registeredReceiver);
            registeredReceiver = null;
        }
    }

    public void destroy() {
        stop();
        autoRefreshHandler.removeCallbacksAndMessages(null);
    }

    private void scheduleAutoRefresh() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        if (autoRefreshActive && refreshIntervalSeconds > 0) {
            autoRefreshHandler.postDelayed(autoRefreshRunnable, refreshIntervalSeconds * 1000L);
        }
    }

    private void requestMapCapture() {
        // Map data is sent by PebbleMapService. PebbleMapHandler only relays c:geo zoom commands.
    }

    private void handlePebbleData(final PebbleDictionary data) {
        final Long cmd = data.getInteger(PebbleMapConstants.KEY_COMMAND);
        if (cmd == null) {
            return;
        }
        final Long value = data.getInteger(PebbleMapConstants.KEY_VALUE);
        final int intValue = value != null ? value.intValue() : 0;
        switch (cmd.intValue()) {
            case PebbleMapConstants.CMD_REFRESH:
                requestMapCapture();
                break;
            case PebbleMapConstants.CMD_ZOOM_IN:
            case PebbleMapConstants.CMD_ZOOM_OUT:
            case PebbleMapConstants.CMD_SET_ZOOM:
                // Watch zoom is handled by PebbleMapService; do not affect the c:geo map.
                break;
            case PebbleMapConstants.CMD_SET_INTERVAL:
                refreshIntervalSeconds = intValue;
                if (refreshIntervalSeconds < 0) {
                    refreshIntervalSeconds = 0;
                }
                autoRefreshActive = refreshIntervalSeconds > 0;
                scheduleAutoRefresh();
                break;
            default:
                Log.w(LOG + " unknown command: " + cmd);
        }
    }

    private void sendMap(final byte[] frame) {
        if (frame == null) {
            return;
        }
        final int total = (frame.length + PebbleMapConstants.CHUNK_SIZE - 1) / PebbleMapConstants.CHUNK_SIZE;
        for (int i = 0; i < total; i++) {
            final int start = i * PebbleMapConstants.CHUNK_SIZE;
            final int end = Math.min(start + PebbleMapConstants.CHUNK_SIZE, frame.length);
            final int length = end - start;
            final byte[] chunk = new byte[length];
            System.arraycopy(frame, start, chunk, 0, length);
            final PebbleDictionary dict = new PebbleDictionary();
            dict.addInt32(PebbleMapConstants.KEY_CHUNK_INDEX, i);
            dict.addInt32(PebbleMapConstants.KEY_CHUNK_TOTAL, total);
            dict.addBytes(PebbleMapConstants.KEY_CHUNK_DATA, chunk);
            PebbleKit.sendDataToPebble(context, PebbleMapConstants.PEBBLE_MAP_APP_UUID, dict);
        }
    }
}
