package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.AndroidRx2Utils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils.DelayedUnsubscription;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import org.apache.commons.lang3.StringUtils;

public class GeoDataProvider {

    private GeoDataProvider() {
        // Utility class, not to be instantiated
    }

    public static Observable<GeoData> create(final Context context) {
        final LocationManager geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        final AtomicReference<Location> latestGPSLocation = new AtomicReference<>(null);
        final Observable<GeoData> observable = Observable.create(new ObservableOnSubscribe<GeoData>() {
            @Override
            public void subscribe(final ObservableEmitter<GeoData> emitter) throws Exception {
                final Listener networkListener = new Listener(emitter, latestGPSLocation);
                final Listener gpsListener = new Listener(emitter, latestGPSLocation);
                final GeoData initialLocation = GeoData.getInitialLocation(context);
                if (initialLocation != null) {
                    emitter.onNext(initialLocation);
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
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        AndroidRx2Utils.looperCallbacksScheduler.scheduleDirect(new Runnable() {
                            @Override
                            public void run() {
                                geoManager.removeUpdates(networkListener);
                                geoManager.removeUpdates(gpsListener);
                            }
                        });
                    }
                });
            }
        });
        return observable.subscribeOn(AndroidRx2Utils.looperCallbacksScheduler).share().lift(new DelayedUnsubscription<GeoData>(2500, TimeUnit.MILLISECONDS));
    }

    private static class Listener implements LocationListener {

        final ObservableEmitter<GeoData> emitter;
        final AtomicReference<Location> latestGPSLocation;

        Listener(final ObservableEmitter<GeoData> emitter, final AtomicReference<Location> latestGPSLocation) {
            this.emitter = emitter;
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
            emitter.onNext(current);
        }

    }

}
