// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.sensors

import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.RxUtils

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

import androidx.annotation.NonNull

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import org.apache.commons.lang3.StringUtils

/**
 * Provides access to Location Data (GNSS) via Android's Location Manager (Context.LOCATION_SERVICE)
 */
class GeoDataProvider {

    private GeoDataProvider() {
        // Utility class, not to be instantiated
    }

    public static Observable<GeoData> create(final Context context) {
        val geoManager: LocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE)
        val latestGPSLocation: AtomicReference<Location> = AtomicReference<>(null)
        val observable: Observable<GeoData> = Observable.create(emitter -> {
            val networkListener: Listener = Listener(emitter, latestGPSLocation)
            val gpsListener: Listener = Listener(emitter, latestGPSLocation)
            val initialLocation: GeoData = GeoData.getInitialLocation(context)
            if (initialLocation != null) {
                emitter.onNext(initialLocation)
            }
            Log.d("GeoDataProvider: starting the GPS and network listeners")
            try {
                geoManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener)
            } catch (final IllegalArgumentException | SecurityException e) {
                Log.w("Unable to create GPS location provider: " + e.getMessage())
            }
            try {
                geoManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener)
            } catch (final IllegalArgumentException | SecurityException e) {
                Log.w("Unable to create network location provider: " + e.getMessage())
            }
            emitter.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(() -> {
                geoManager.removeUpdates(networkListener)
                geoManager.removeUpdates(gpsListener)
            }))
        })
        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler).share().lift(RxUtils.DelayedUnsubscription<>(2500, TimeUnit.MILLISECONDS))
    }

    private static class Listener : LocationListener {

        final ObservableEmitter<GeoData> emitter
        final AtomicReference<Location> latestGPSLocation

        Listener(final ObservableEmitter<GeoData> emitter, final AtomicReference<Location> latestGPSLocation) {
            this.emitter = emitter
            this.latestGPSLocation = latestGPSLocation
        }

        override         public Unit onLocationChanged(final Location location) {
            if (StringUtils == (location.getProvider(), LocationManager.GPS_PROVIDER)) {
                latestGPSLocation.set(location)
                assign(latestGPSLocation.get())
            } else {
                assign(GeoData.determineBestLocation(latestGPSLocation.get(), location))
            }
        }

        override         public Unit onStatusChanged(final String provider, final Int status, final Bundle extras) {
            // nothing
        }

        override         public Unit onProviderDisabled(final String provider) {
            // nothing
        }

        override         public Unit onProviderEnabled(final String provider) {
            // nothing
        }

        private Unit assign(final Location location) {
            // We do not necessarily get signaled when satellites go to 0/0.
            val current: GeoData = GeoData(location)
            emitter.onNext(current)
        }

    }

}
