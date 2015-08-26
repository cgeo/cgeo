package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;
import cgeo.geocaching.utils.RxUtils.DelayedUnsubscription;

import org.apache.commons.lang3.StringUtils;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GeoDataProvider {

    private GeoDataProvider() {
        // Utility class, not to be instantiated
    }

    public static Observable<GeoData> create(final Context context) {
        final LocationManager geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        final AtomicReference<Location> latestGPSLocation = new AtomicReference<>(null);
        final Observable<GeoData> observable = Observable.create(new OnSubscribe<GeoData>() {
            @Override
            public void call(final Subscriber<? super GeoData> subscriber) {
                final Listener networkListener = new Listener(subscriber, latestGPSLocation);
                final Listener gpsListener = new Listener(subscriber, latestGPSLocation);
                final GeoData initialLocation = GeoData.getInitialLocation(context);
                if (initialLocation != null) {
                    subscriber.onNext(initialLocation);
                }
                Log.d("GeoDataProvider: starting the GPS and network listeners");
                try {
                    geoManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
                } catch (final Exception e) {
                    Log.w("Unable to create GPS location provider: " + e.getMessage());
                }
                try {
                    geoManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
                } catch (final Exception e) {
                    Log.w("Unable to create network location provider: " + e.getMessage());
                }
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        RxUtils.looperCallbacksWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                geoManager.removeUpdates(networkListener);
                                geoManager.removeUpdates(gpsListener);
                            }
                        });
                    }
                }));
            }
        });
        return observable.subscribeOn(RxUtils.looperCallbacksScheduler).share().lift(new DelayedUnsubscription<GeoData>(2500, TimeUnit.MILLISECONDS)).onBackpressureLatest();
    }

    private static class Listener implements LocationListener {

        final Subscriber<? super GeoData> subscriber;
        final AtomicReference<Location> latestGPSLocation;

        public Listener(final Subscriber<? super GeoData> subscriber, final AtomicReference<Location> latestGPSLocation) {
            this.subscriber = subscriber;
            this.latestGPSLocation = latestGPSLocation;
        }

        @Override
        public void onLocationChanged(final Location location) {
            if (StringUtils.equals(location.getProvider(), LocationManager.GPS_PROVIDER)) {
                latestGPSLocation.set(location);
                assign(latestGPSLocation.get());
            } else {
                assign(GeoData.best(latestGPSLocation.get(), location));
            }
        }

        @Override
        public void onStatusChanged(final String provider, final int status, final Bundle extras) {
            // nothing
        }

        @Override
        public void onProviderDisabled(final String provider) {
            // nothing
        }

        @Override
        public void onProviderEnabled(final String provider) {
            // nothing
        }

        private void assign(final Location location) {
            // We do not necessarily get signaled when satellites go to 0/0.
            final GeoData current = new GeoData(location);
            subscriber.onNext(current);
        }

    }

}
