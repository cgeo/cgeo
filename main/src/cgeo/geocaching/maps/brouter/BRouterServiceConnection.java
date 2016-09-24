package cgeo.geocaching.maps.brouter;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import btools.routingapp.IBRouterService;

public class BRouterServiceConnection implements ServiceConnection {
    private IBRouterService brouterService;

    @Override
    public void onServiceConnected(final ComponentName className, final IBinder service) {
        brouterService = IBRouterService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(final ComponentName className) {
        brouterService = null;
    }

    public boolean isConnected() {
        return brouterService != null;
    }

    public String getTrackFromParams(final Bundle params) {
        if (!isConnected()) {
            return null;
        }

        try {
            return brouterService.getTrackFromParams(params);
        } catch (final RemoteException e) {
            return null;
        }
    }
}