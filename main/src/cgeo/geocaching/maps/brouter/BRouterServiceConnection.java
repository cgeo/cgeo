package cgeo.geocaching.maps.brouter;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import btools.routingapp.IBRouterService;


/**
 * Created by lukeIam on 11.09.2016.
 */
public class BRouterServiceConnection implements ServiceConnection {
    private IBRouterService brouterService;

    public void onServiceConnected(ComponentName className, IBinder service) {
        brouterService = IBRouterService.Stub.asInterface(service);
    }

    public void onServiceDisconnected(ComponentName className) {
        brouterService = null;
    }
    public boolean isConnected(){
        return brouterService != null;
    }

    public String getTrackFromParams(Bundle params){
        if(!isConnected()){
            return null;
        }

        try {
            return brouterService.getTrackFromParams(params);
        } catch (RemoteException e) {
            return null;
        }
    }
}