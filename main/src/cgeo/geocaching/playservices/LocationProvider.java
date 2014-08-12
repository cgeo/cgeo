package cgeo.geocaching.playservices;

import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils.ConnectableLooperCallbacks;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class LocationProvider implements OnSubscribe<IGeoData>, ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private final BehaviorSubject<IGeoData> subject;
    private final LocationClient locationClient;
    private static final LocationRequest LOCATION_REQUEST =
            LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000).setInterval(1000);
    private static boolean firstLocation = true;

    /**
     * Build a new geo data provider object.
     * <p/>
     * There is no need to instantiate more than one such object in an application, as observers can be added
     * at will.
     *
     * @param context the context used to retrieve the system services
     */
    protected LocationProvider(final Context context) {
        locationClient = new LocationClient(context, this, this);
        subject = BehaviorSubject.create();
    }

    public static Observable<IGeoData> create(final Context context) {
        final LocationProvider provider = new LocationProvider(context);
        return provider.worker.refCount();
    }

    @Override
    public void call(final Subscriber<? super IGeoData> subscriber) {
        subject.subscribe(subscriber);
    }

    final ConnectableObservable<IGeoData> worker = new ConnectableLooperCallbacks<IGeoData>(this, 2500, TimeUnit.MILLISECONDS) {
        @Override
        protected void onStart() {
            Log.d("LocationProvider: starting the location listener");
            locationClient.connect();
        }

        @Override
        protected void onStop() {
            Log.d("LocationProvider: stopping the location listener");
            locationClient.removeLocationUpdates(LocationProvider.this);
            locationClient.disconnect();
        }
    };

    @Override
    public void onConnected(final Bundle bundle) {
        if (firstLocation) {
            final Location initialLocation = locationClient.getLastLocation();
            subject.onNext(initialLocation != null ? new GeoData(initialLocation) : GeoData.dummyLocation());
            firstLocation = false;
        }
        locationClient.requestLocationUpdates(LOCATION_REQUEST, this);
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(final ConnectionResult connectionResult) {
        Log.e("cannot connect to Google Play location service: " + connectionResult);
        subject.onError(new RuntimeException("Connection failed: " + connectionResult));
    }

    @Override
    public void onLocationChanged(final Location location) {
        subject.onNext(new GeoData(location));
    }
}
