package cgeo.geocaching.maps.routing;

import cgeo.geocaching.brouter.IInternalRoutingService;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

public class InternalServiceConnection extends AbstractServiceConnection {

    InternalServiceConnection(final @Nullable Runnable onServiceConnectedCallback) {
        super(onServiceConnectedCallback);
    }

    @Override
    public void onServiceConnected(final ComponentName className, final IBinder service) {
        super.onServiceConnected(className, service);
        routingService = IInternalRoutingService.Stub.asInterface(service);
    }

    @Override
    public String getTrackFromParams(final Bundle params) {
        if (!isConnected()) {
            return null;
        }

        try {
            return ((IInternalRoutingService) routingService).getTrackFromParams(params);
        } catch (final RemoteException | NullPointerException e) {
            return null;
        }
    }

}
