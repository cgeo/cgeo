package cgeo.geocaching.maps.routing;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import btools.routingapp.IBRouterService;

public class BRouterServiceConnection extends AbstractServiceConnection {

    BRouterServiceConnection(final @Nullable Runnable onServiceConnectedCallback) {
        super(onServiceConnectedCallback);
    }

    @Override
    public void onServiceConnected(final ComponentName className, final IBinder service) {
        super.onServiceConnected(className, service);
        routingService = IBRouterService.Stub.asInterface(service);
    }

    @Override
    public String getTrackFromParams(final Bundle params) {
        if (!isConnected()) {
            return null;
        }

        try {
            return ((IBRouterService) routingService).getTrackFromParams(params);
        } catch (final RemoteException e) {
            return null;
        }
    }
}
