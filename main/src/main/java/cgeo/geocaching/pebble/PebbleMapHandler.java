package cgeo.geocaching.pebble;

import cgeo.geocaching.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

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
    private final PebbleDataReceiver dataReceiver;
    private BroadcastReceiver registeredReceiver;

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
    }

    public void stop() {
        if (registeredReceiver != null) {
            context.unregisterReceiver(registeredReceiver);
            registeredReceiver = null;
        }
    }

    public void destroy() {
        stop();
    }

    private void handlePebbleData(final PebbleDictionary data) {
        final Long cmd = data.getInteger(PebbleMapConstants.KEY_COMMAND);
        if (cmd == null) {
            return;
        }
        switch (cmd.intValue()) {
            case PebbleMapConstants.CMD_REFRESH:
            case PebbleMapConstants.CMD_ZOOM_IN:
            case PebbleMapConstants.CMD_ZOOM_OUT:
            case PebbleMapConstants.CMD_SET_ZOOM:
            case PebbleMapConstants.CMD_SET_INTERVAL:
                // All watch commands are handled by PebbleMapService.
                break;
            default:
                Log.w(LOG + " unknown command: " + cmd);
        }
    }

}
