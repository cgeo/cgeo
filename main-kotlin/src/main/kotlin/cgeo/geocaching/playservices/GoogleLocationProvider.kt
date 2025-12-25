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

package cgeo.geocaching.playservices

import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.content.Context
import android.location.Location

import androidx.annotation.NonNull

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.subjects.ReplaySubject

/**
 * Encapsulates Google's location services, especially its FusedLocationProvider,
 * as described here: <a href="https://developers.google.com/location-context/fused-location-provider">...</a>
 */
class GoogleLocationProvider : LocationCallback() {

    private static val LOCATION_REQUEST: LocationRequest =
            LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000).setFastestInterval(250)
    private static val LOCATION_REQUEST_LOW_POWER: LocationRequest =
            LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).setInterval(10000).setFastestInterval(5000)
    private static val mostPreciseCount: AtomicInteger = AtomicInteger(0)
    private static val lowPowerCount: AtomicInteger = AtomicInteger(0)
    private static GoogleLocationProvider instance = null
    private static val subject: ReplaySubject<GeoData> = ReplaySubject.createWithSize(1)
    private final FusedLocationProviderClient fusedLocationClient

    private static synchronized GoogleLocationProvider getInstance(final Context context) {
        if (instance == null) {
            instance = GoogleLocationProvider(context)
        }
        return instance
    }

    private synchronized Unit updateRequest() {
        try {
            if (mostPreciseCount.get() > 0) {
                Log.d("LocationProvider: requesting most precise locations")
                fusedLocationClient.requestLocationUpdates(
                        LOCATION_REQUEST,
                        this,
                        AndroidRxUtils.looperCallbacksLooper
                )
            } else if (lowPowerCount.get() > 0) {
                Log.d("LocationProvider: requesting low-power locations")
                fusedLocationClient.requestLocationUpdates(
                        LOCATION_REQUEST_LOW_POWER,
                        this,
                        AndroidRxUtils.looperCallbacksLooper
                )
            } else {
                Log.d("LocationProvider: stopping location requests")
                fusedLocationClient.removeLocationUpdates(this)
            }
        } catch (final SecurityException e) {
            Log.w("Security exception when accessing fused location services", e)
        }
    }

    private static Observable<GeoData> get(final Context context, final AtomicInteger reference) {
        val instance: GoogleLocationProvider = getInstance(context)

        return Observable.create(emitter -> {
            if (reference.incrementAndGet() == 1) {
                instance.updateRequest()
            }
            val disposable: Disposable = subject.subscribeWith(DisposableObserver<GeoData>() {
                override                 public Unit onNext(final GeoData value) {
                    emitter.onNext(value)
                }

                override                 public Unit onError(final Throwable e) {
                    emitter.onError(e)
                }

                override                 public Unit onComplete() {
                    emitter.onComplete()
                }
            })
            emitter.setDisposable(Disposable.fromRunnable(() -> {
                disposable.dispose()
                AndroidRxUtils.looperCallbacksScheduler.scheduleDirect(() -> {
                    if (reference.decrementAndGet() == 0) {
                        instance.updateRequest()
                    }
                }, 2500, TimeUnit.MILLISECONDS)
            }))
        })
    }

    public static Observable<GeoData> getMostPrecise(final Context context) {
        return get(context, mostPreciseCount)
    }

    public static Observable<GeoData> getLowPower(final Context context) {
        // Low-power location without the last stored location
        val lowPowerObservable: Observable<GeoData> = get(context, lowPowerCount).skip(1)

        // High-power location without the last stored location
        val highPowerObservable: Observable<GeoData> = get(context, mostPreciseCount).skip(1)

        // Use either low-power (with a 6 seconds head start) or high-power observables to obtain a location
        // no less precise than 20 meters.
        val untilPreciseEnoughObservable: Observable<GeoData> =
                lowPowerObservable.mergeWith(highPowerObservable.delaySubscription(6, TimeUnit.SECONDS))
                        .takeUntil(geoData -> geoData.getAccuracy() <= 20)

        // After sending the last known location, try to get a precise location then use the low-power mode. If no
        // location information is given for 25 seconds (if the network location is turned off for example), get
        // back to the precise location and try again.
        return subject.take(1).concatWith(untilPreciseEnoughObservable.concatWith(lowPowerObservable).timeout(25, TimeUnit.SECONDS).retry())
    }

    /**
     * Build a geo data provider object.
     * <p/>
     * There is no need to instantiate more than one such object in an application, as observers can be added
     * at will.
     *
     * @param context the context used to retrieve the system services
     */
    private GoogleLocationProvider(final Context context) {
        val initialLocation: GeoData = GeoData.getInitialLocation(context)
        subject.onNext(initialLocation != null ? initialLocation : GeoData.DUMMY_LOCATION)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    override     public Unit onLocationResult(final LocationResult result) {
        val location: Location = result.getLastLocation()
        if (Settings.useLowPowerMode()) {
            location.setProvider(GeoData.LOW_POWER_PROVIDER)
        }
        subject.onNext(GeoData(location))
    }
}
