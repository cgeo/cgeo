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
import rx.functions.Func1;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class LocationProvider extends LooperCallbacks<IGeoData> implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private final LocationClient locationClient;
    private final boolean lowPower;
    private final boolean withInitialLocation;
    private boolean onlyInitialLocation;
    private static final LocationRequest LOCATION_REQUEST =
            LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000).setFastestInterval(250);
    private static final LocationRequest LOCATION_REQUEST_LOW_POWER =
            LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).setInterval(10000).setFastestInterval(5000);

    public static Observable<IGeoData> getInitialLocation(final Context context, final boolean lowPower) {
        return Observable.create(new LocationProvider(context, lowPower, true, true));
    }

    public static Observable<IGeoData> getMostPrecise(Context context, boolean withInitialLocation) {
        return Observable.create(new LocationProvider(context, false, withInitialLocation, false));
    }

    public static Observable<IGeoData> getLowPower(Context context, boolean withInitialLocation) {
        final Observable<IGeoData> initialLocationObservable = withInitialLocation ? getInitialLocation(context, true) : Observable.<IGeoData>empty();
        final Observable<IGeoData> lowPowerObservable = Observable.create(new LocationProvider(context, true, false, false));
        final Observable<IGeoData> lowPowerObservable2 = Observable.create(new LocationProvider(context, true, false, false));
        final Observable<IGeoData> gpsFixObservable = getMostPrecise(context, false).takeFirst(new Func1<IGeoData, Boolean>() {
            @Override
            public Boolean call(final IGeoData geoData) {
                return geoData.getAccuracy() < 20;
            }
        }).delaySubscription(2, TimeUnit.SECONDS);
        return initialLocationObservable.concatWith(lowPowerObservable.mergeWith(gpsFixObservable).first()
                .concatWith(lowPowerObservable2).timeout(60, TimeUnit.SECONDS).retry());
    }

    /**
     * Build a new geo data provider object.
     * <p/>
     * There is no need to instantiate more than one such object in an application, as observers can be added
     * at will.
     *
     * @param context the context used to retrieve the system services
     * @param lowPower <tt>true</tt> if the GPS must not be used
     * @param withInitialLocation <tt>true</tt> if the initial location must be provided
     * @param onlyInitialLocation <tt>true</tt> if the observable must be closed after providing the initial location
     */
    protected LocationProvider(final Context context, final boolean lowPower, final boolean withInitialLocation, final boolean onlyInitialLocation) {
        super(lowPower ? 0 : 2500, TimeUnit.MILLISECONDS);
        if (onlyInitialLocation && !withInitialLocation) {
            throw new IllegalArgumentException("LocationProvider: cannot request only initial location without requesting it at all");
        }
        locationClient = new LocationClient(context, this, this);
        this.withInitialLocation = withInitialLocation;
        this.lowPower = lowPower;
        this.onlyInitialLocation = onlyInitialLocation;
    }

    @Override
    public void onStart() {
        Log.d("LocationProvider: starting the location listener - lowPower: " + lowPower);
        locationClient.connect();
    }

    @Override
    public void onStop() {
        Log.d("LocationProvider: stopping the location listener - lowPower: " + lowPower);
        if (!onlyInitialLocation) {
            locationClient.removeLocationUpdates(this);
        }
        locationClient.disconnect();
    }

    @Override
    public void onConnected(final Bundle bundle) {
        if (withInitialLocation) {
            final Location initialLocation = locationClient.getLastLocation();
            Log.d("LocationProvider: starting with " + (initialLocation == null ? "dummy" : "previous") + " location");
            subscriber.onNext(initialLocation != null ? new GeoData(initialLocation) : GeoData.dummyLocation());
        }
        if (onlyInitialLocation) {
            subscriber.onCompleted();
        } else {
            locationClient.requestLocationUpdates(lowPower ? LOCATION_REQUEST_LOW_POWER : LOCATION_REQUEST, this);
        }
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
        location.setProvider(lowPower ? GeoData.LOW_POWER_PROVIDER : GeoData.FUSED_PROVIDER);
        subscriber.onNext(new GeoData(location));
    }
}
