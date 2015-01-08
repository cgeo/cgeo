package cgeo.geocaching.sensors;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.playservices.LocationProvider;
import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import org.eclipse.jdt.annotation.NonNull;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;

import java.util.concurrent.atomic.AtomicBoolean;

public class Sensors {

    private Observable<GeoData> geoDataObservable;
    private Observable<GeoData> geoDataObservableLowPower;
    private Observable<Float> directionObservable;
    private Observable<Status> gpsStatusObservable;
    @NonNull private volatile GeoData currentGeo = GeoData.DUMMY_LOCATION;
    private volatile float currentDirection = 0.0f;
    private volatile boolean hasValidLocation = false;
    private final boolean hasCompassCapabilities;
    private final CgeoApplication app = CgeoApplication.getInstance();

    private static class InstanceHolder {
        static final Sensors INSTANCE = new Sensors();
    }

    private final Action1<GeoData> rememberGeodataAction = new Action1<GeoData>() {
        @Override
        public void call(final GeoData geoData) {
            currentGeo = geoData;
            hasValidLocation = true;
        }
    };

    private final Action1<Float> onNextrememberDirectionAction = new Action1<Float>() {
        @Override
        public void call(final Float direction) {
            currentDirection = direction;
        }
    };

    private Sensors() {
        gpsStatusObservable = GpsStatusProvider.create(app).replay(1).refCount();
        final Context context = CgeoApplication.getInstance().getApplicationContext();
        hasCompassCapabilities = RotationProvider.hasRotationSensor(context) || OrientationProvider.hasOrientationSensor(context);
    }

    public static final Sensors getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void setupGeoDataObservables(final boolean useGooglePlayServices, final boolean useLowPowerLocation) {
        if (useGooglePlayServices) {
            geoDataObservable = LocationProvider.getMostPrecise(app).doOnNext(rememberGeodataAction);
            if (useLowPowerLocation) {
                geoDataObservableLowPower = LocationProvider.getLowPower(app).doOnNext(rememberGeodataAction);
            } else {
                geoDataObservableLowPower = geoDataObservable;
            }
        } else {
            geoDataObservable = RxUtils.rememberLast(GeoDataProvider.create(app).doOnNext(rememberGeodataAction), null);
            geoDataObservableLowPower = geoDataObservable;
        }
    }

    private static final Func1<GeoData, Float> GPS_TO_DIRECTION = new Func1<GeoData, Float>() {
        @Override
        public Float call(final GeoData geoData) {
            return AngleUtils.reverseDirectionNow(geoData.getBearing());
        }
    };

    public void setupDirectionObservable(final boolean useLowPower) {
        // If we have no magnetic sensor, there is no point in trying to setup any, we will always get the direction from the GPS.
        if (!hasCompassCapabilities) {
            Log.i("No compass capabilities, using only the GPS for the orientation");
            directionObservable = RxUtils.rememberLast(geoDataObservableLowPower.map(GPS_TO_DIRECTION).doOnNext(onNextrememberDirectionAction), 0f);
            return;
        }

        // Combine the magnetic direction observable with the GPS when compass is disabled or speed is high enough.
        final AtomicBoolean useDirectionFromGps = new AtomicBoolean(false);

        // The rotation sensor seems to be bogus on some devices. We should start with the orientation one, except when we explicitely
        // want to use the low-power geomagnetic rotation sensor or when we do not have an orientation sensor.
        final boolean useRotationSensor = (useLowPower && RotationProvider.hasGeomagneticRotationSensor(app)) || !OrientationProvider.hasOrientationSensor(app);
        final Observable<Float> sensorDirectionObservable = useRotationSensor ? RotationProvider.create(app, useLowPower) : OrientationProvider.create(app);
        final Observable<Float> magneticDirectionObservable = sensorDirectionObservable.onErrorResumeNext(new Func1<Throwable, Observable<? extends Float>>() {
            @Override
            public Observable<? extends Float> call(final Throwable throwable) {
                Log.e("Device orientation is not available due to sensors error, disabling compass", throwable);
                Settings.setUseCompass(false);
                return Observable.<Float>never().startWith(0.0f);
            }
        }).filter(new Func1<Float, Boolean>() {
            @Override
            public Boolean call(final Float aFloat) {
                return Settings.isUseCompass() && !useDirectionFromGps.get();
            }
        });

        final Observable<Float> directionFromGpsObservable = geoDataObservableLowPower.filter(new Func1<GeoData, Boolean>() {
            @Override
            public Boolean call(final GeoData geoData) {
                final boolean useGps = geoData.getSpeed() > 5.0f;
                useDirectionFromGps.set(useGps);
                return useGps || !Settings.isUseCompass();
            }
        }).map(GPS_TO_DIRECTION);

        directionObservable = RxUtils.rememberLast(Observable.merge(magneticDirectionObservable, directionFromGpsObservable).doOnNext(onNextrememberDirectionAction), 0f);
    }

    public Observable<GeoData> geoDataObservable(final boolean lowPower) {
        return lowPower ? geoDataObservableLowPower : geoDataObservable;
    }

    public Observable<Float> directionObservable() {
        return directionObservable;
    }

    public Observable<Status> gpsStatusObservable() {
        if (gpsStatusObservable == null) {
            gpsStatusObservable = GpsStatusProvider.create(app).share();
        }
        return gpsStatusObservable;
    }

    @NonNull
    public GeoData currentGeo() {
        return currentGeo;
    }

    public boolean hasValidLocation() {
        return hasValidLocation;
    }

    public float currentDirection() {
        return currentDirection;
    }

    public boolean hasCompassCapabilities() {
        return hasCompassCapabilities;
    }

}
