package cgeo.geocaching.playservices;

import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils.LooperCallbacks;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import rx.Observable;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class LocationProvider extends LooperCallbacks<IGeoData> implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private final LocationClient locationClient;
    private static final LocationRequest LOCATION_REQUEST =
            LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000).setInterval(1000);

    public static Observable<IGeoData> create(final Context context) {
        return Observable.create(new LocationProvider(context));
    }

    /**
     * Build a new geo data provider object.
     * <p/>
     * There is no need to instantiate more than one such object in an application, as observers can be added
     * at will.
     *
     * @param context the context used to retrieve the system services
     */
    protected LocationProvider(final Context context) {
        super(2500, TimeUnit.MILLISECONDS);
        locationClient = new LocationClient(context, this, this);
    }

    @Override
    public void onStart() {
        Log.d("LocationProvider: starting the location listener");
        locationClient.connect();
    }

    @Override
    public void onStop() {
        Log.d("LocationProvider: stopping the location listener");
        locationClient.removeLocationUpdates(this);
        locationClient.disconnect();
    }

    @Override
    public void onConnected(final Bundle bundle) {
        final Location initialLocation = locationClient.getLastLocation();
        subscriber.onNext(initialLocation != null ? new GeoData(initialLocation) : GeoData.dummyLocation());
        locationClient.requestLocationUpdates(LOCATION_REQUEST, this);
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(final ConnectionResult connectionResult) {
        Log.e("cannot connect to Google Play location service: " + connectionResult);
        subscriber.onError(new RuntimeException("Connection failed: " + connectionResult));
    }

    @Override
    public void onLocationChanged(final Location location) {
        subscriber.onNext(new GeoData(location));
    }
}
