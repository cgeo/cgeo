package cgeo.geocaching.playservices;

import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LocationProvider implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final LocationRequest LOCATION_REQUEST =
            LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000).setFastestInterval(250);
    private static final LocationRequest LOCATION_REQUEST_LOW_POWER =
            LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).setInterval(10000).setFastestInterval(5000);
    private static final AtomicInteger mostPreciseCount = new AtomicInteger(0);
    private static final AtomicInteger lowPowerCount = new AtomicInteger(0);
    private static LocationProvider instance = null;
    private static ReplaySubject<IGeoData> subject = ReplaySubject.createWithSize(1);
    private final LocationClient locationClient;

    private static synchronized LocationProvider getInstance(final Context context) {
        if (instance == null) {
            instance = new LocationProvider(context);
        }
        return instance;
    }

    private synchronized void updateRequest() {
        if (locationClient.isConnected()) {
            if (mostPreciseCount.get() > 0) {
                Log.d("LocationProvider: requesting most precise locations");
                locationClient.requestLocationUpdates(LOCATION_REQUEST, this, RxUtils.looperCallbacksLooper);
            } else if (lowPowerCount.get() > 0) {
                Log.d("LocationProvider: requesting low-power locations");
                locationClient.requestLocationUpdates(LOCATION_REQUEST_LOW_POWER, this, RxUtils.looperCallbacksLooper);
            } else {
                Log.d("LocationProvider: stopping location requests");
                locationClient.removeLocationUpdates(this);
            }
        }
    }

    private static Observable<IGeoData> get(final Context context, final AtomicInteger reference) {
        final LocationProvider instance = getInstance(context);
        return Observable.create(new OnSubscribe<IGeoData>() {
            @Override
            public void call(final Subscriber<? super IGeoData> subscriber) {
                if (reference.incrementAndGet() == 1) {
                    instance.updateRequest();
                }
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        RxUtils.looperCallbacksWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                if (reference.decrementAndGet() == 0) {
                                    instance.updateRequest();
                                }
                            }
                        }, 2500, TimeUnit.MILLISECONDS);
                    }
                }));
                subscriber.add(subject.subscribe(new Action1<IGeoData>() {
                    @Override
                    public void call(final IGeoData geoData) {
                        subscriber.onNext(geoData);
                    }
                }));
            }
        });
    }

    private static Observable<IGeoData> getInitialLocation(final Context context, final boolean lowPower) {
        return get(context, lowPower ? lowPowerCount : mostPreciseCount).first();
    }

    public static Observable<IGeoData> getMostPrecise(final Context context) {
        return get(context, mostPreciseCount);
    }

    public static Observable<IGeoData> getLowPower(Context context, boolean withInitialLocation) {
        final Observable<IGeoData> initialLocationObservable = withInitialLocation ? getInitialLocation(context, true) : Observable.<IGeoData>empty();
        final Observable<IGeoData> lowPowerObservable = get(context, lowPowerCount).skip(1);
        final Observable<IGeoData> gpsFixObservable = get(context, mostPreciseCount).skip(1).takeFirst(new Func1<IGeoData, Boolean>() {
            @Override
            public Boolean call(final IGeoData geoData) {
                return geoData.getAccuracy() < 20;
            }
        });
        return initialLocationObservable.concatWith(lowPowerObservable.mergeWith(gpsFixObservable.delaySubscription(6, TimeUnit.SECONDS)).first()
                .concatWith(lowPowerObservable).timeout(60, TimeUnit.SECONDS).retry());
    }

    /**
     * Build a new geo data provider object.
     * <p/>
     * There is no need to instantiate more than one such object in an application, as observers can be added
     * at will.
     *
     * @param context the context used to retrieve the system services
     */
    private LocationProvider(final Context context) {
        subject.onNext(GeoData.dummyLocation());
        locationClient = new LocationClient(context, this, this);
        locationClient.connect();
    }

    @Override
    public void onConnected(final Bundle bundle) {
        updateRequest();
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
