package cgeo.geocaching.maps.routing;

import cgeo.geocaching.utils.Log;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

class AbstractServiceConnection implements ServiceConnection {
    protected android.os.IInterface routingService;
    private Runnable onServiceConnectedCallback = null;

    AbstractServiceConnection(final @Nullable Runnable onServiceConnectedCallback) {
        this.onServiceConnectedCallback = onServiceConnectedCallback;
    }

    @Override
    public void onServiceConnected(final ComponentName className, final IBinder service) {
        Log.d("connection to brouter established");
        if (null != onServiceConnectedCallback) {
            onServiceConnectedCallback.run();
        }
    }

    @Override
    public void onServiceDisconnected(final ComponentName className) {
        Log.d("connection to brouter disconnected");
        this.onServiceConnectedCallback = null;
        routingService = null;
    }

    public boolean isConnected() {
        return routingService != null;
    }

    @Nullable
    public String getTrackFromParams(final Bundle params) {
        return null;
    }

}
