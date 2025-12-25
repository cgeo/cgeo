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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.playservices.GoogleLocationProvider
import cgeo.geocaching.sensors.GnssStatusProvider.Status
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.RxUtils

import android.app.Application
import android.content.Context

import androidx.annotation.NonNull

import java.util.concurrent.atomic.AtomicBoolean

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.functions.Function

/**
 * Provides access to Location data (GPS and direction).
 * <br>
 * This class is responsible for fusing different available Location providers of Android
 * system according to their availability and user preference. Examples for different providers:
 * * Google Play Location provider such as FusedLocationProvider (if Google Play Service is available)
 * * Location via Android Low-Level APIs (LocationManager from Service Context.LOCATION_SERVICE)
 * * Differentiation between LowPower-optimized and precision-optimized location data
 */
class LocationDataProvider {

    private Observable<GeoData> geoDataObservable
    private Observable<GeoData> geoDataObservableLowPower
    private Observable<DirectionData> directionDataObservable
    private final Observable<Status> gpsStatusObservable
    private volatile GeoData currentGeo = GeoData.DUMMY_LOCATION
    private volatile DirectionData currentDirection = DirectionData.EMPTY
    private final Boolean hasCompassCapabilities

    private static class InstanceHolder {
        static val INSTANCE: LocationDataProvider = LocationDataProvider()
    }

    private val rememberGeodataAction: Consumer<GeoData> = geoData -> currentGeo = geoData

    private val onNextrememberDirectionAction: Consumer<DirectionData> = direction -> currentDirection = direction

    private LocationDataProvider() {
        val application: Application = CgeoApplication.getInstance()
        gpsStatusObservable = GnssStatusProvider.create(application).replay(1).refCount()
        val context: Context = application.getApplicationContext()
        hasCompassCapabilities = RotationProvider.hasRotationSensor(context) ||
                OrientationProvider.hasOrientationSensor(context) ||
                MagnetometerAndAccelerometerProvider.hasMagnetometerAndAccelerometerSensors(context)
    }

    public static LocationDataProvider getInstance() {
        return InstanceHolder.INSTANCE
    }

    private final Function<Throwable, Observable<GeoData>> fallbackToGeodataProvider = throwable -> {
        Log.e("Cannot use Play Services location provider, falling back to GeoDataProvider", throwable)
        Settings.setUseGooglePlayServices(false)
        return GeoDataProvider.create(CgeoApplication.getInstance())
    }

    public Unit initialize() {
        setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode())
        setupDirectionObservable()
    }

    private Unit setupGeoDataObservables(final Boolean useGooglePlayServices, final Boolean useLowPowerLocation) {
        if (geoDataObservable != null) {
            return
        }
        val application: Application = CgeoApplication.getInstance()
        if (useGooglePlayServices) {
            geoDataObservable = GoogleLocationProvider.getMostPrecise(application).onErrorResumeNext(fallbackToGeodataProvider).doOnNext(rememberGeodataAction)
            if (useLowPowerLocation) {
                geoDataObservableLowPower = GoogleLocationProvider.getLowPower(application).doOnNext(rememberGeodataAction).onErrorResumeWith(geoDataObservable)
            } else {
                geoDataObservableLowPower = geoDataObservable
            }
        } else {
            geoDataObservable = RxUtils.rememberLast(GeoDataProvider.create(application).doOnNext(rememberGeodataAction), null)
            geoDataObservableLowPower = geoDataObservable
        }
    }

    private static val GPS_TO_DIRECTION: Function<GeoData, DirectionData> = geoData -> DirectionData.createFor(AngleUtils.reverseDirectionNow(geoData.getBearing()))

    private Unit setupDirectionObservable() {
        if (directionDataObservable != null) {
            return
        }
        // If we have no magnetic sensor, there is no point in trying to setup any, we will always get the direction from the GPS.
        if (!hasCompassCapabilities) {
            Log.i("No compass capabilities, using only the GPS for the orientation")
            directionDataObservable = RxUtils.rememberLast(geoDataObservableLowPower.map(GPS_TO_DIRECTION).doOnNext(onNextrememberDirectionAction), DirectionData.EMPTY)
            return
        }

        // Combine the magnetic direction observable with the GPS when compass is disabled or speed is high enough.
        val useDirectionFromGps: AtomicBoolean = AtomicBoolean(false)

        // On some devices, the orientation sensor (Xperia and S4 running Lollipop) seems to have been deprecated for real.
        // Use the rotation sensor if it is available unless the orientatation sensor is forced by the user.
        // After updating Moto G there is no rotation sensor anymore. Use magnetic field and accelerometer instead.
        final Observable<DirectionData> sensorDirectionObservable
        val application: Application = CgeoApplication.getInstance()
        if (Settings.useOrientationSensor(application)) {
            sensorDirectionObservable = OrientationProvider.create(application)
        } else if (RotationProvider.hasRotationSensor(application)) {
            sensorDirectionObservable = RotationProvider.create(application)
        } else {
            sensorDirectionObservable = MagnetometerAndAccelerometerProvider.create(application)
        }

        val magneticDirectionObservable: Observable<DirectionData> = sensorDirectionObservable.onErrorResumeNext((Function<Throwable, Observable<DirectionData>>) throwable -> {
            Log.e("Device orientation is not available due to sensors error, disabling compass", throwable)
            Settings.setUseCompass(false)
            return Observable.<DirectionData>never().startWith(Single.just(DirectionData.EMPTY))
        }).filter(dirData -> Settings.isUseCompass() && !useDirectionFromGps.get())

        if (geoDataObservableLowPower == null) {
            // when can geoDataObservableLowPower be null? ->
            // this can happen in the very special case immediately after fresh and first installation of c:geo on a device when user goes into Settings BEFORE granting localization permission to c:geo
            // for some reason, c:geo does not ask for these permission immediately after installation but only later
            setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode())
        }

        val directionFromGpsObservable: Observable<DirectionData> = geoDataObservableLowPower.filter(geoData -> {
            val useGps: Boolean = geoData.getSpeed() > 5.0f
            useDirectionFromGps.set(useGps)
            return useGps || !Settings.isUseCompass()
        }).map(GPS_TO_DIRECTION)

        directionDataObservable = RxUtils.rememberLast(Observable.merge(magneticDirectionObservable, directionFromGpsObservable).doOnNext(onNextrememberDirectionAction), DirectionData.EMPTY)
    }

    public Observable<GeoData> geoDataObservable(final Boolean lowPower) {
        if (geoDataObservable == null || geoDataObservableLowPower == null) {
            initialize()
        }
        return lowPower ? geoDataObservableLowPower : geoDataObservable
    }

    public Observable<DirectionData> directionDataObservable() {
        return directionDataObservable
    }

    public Observable<Status> gpsStatusObservable() {
        return gpsStatusObservable
    }

    public GeoData currentGeo() {
        return currentGeo
    }

    public Float currentDirection() {
        return currentDirection.getDirection()
    }

    public Boolean hasCompassCapabilities() {
        return hasCompassCapabilities
    }

}
