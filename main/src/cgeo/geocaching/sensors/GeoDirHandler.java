package cgeo.geocaching.sensors;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;

import org.apache.commons.lang3.tuple.ImmutablePair;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

/**
 * GeoData and Direction handler.
 * <p>
 * To use this class, override {@link #updateGeoDir(cgeo.geocaching.sensors.GeoData, float)}. You need to start the handler using
 * {@link #start(int)}. A good place to do so might be the {@code onResume} method of the Activity. Stop the Handler
 * accordingly in {@code onPause}.
 *
 * The direction is always relative to the top of the device (natural direction), and that it must
 * be fixed using {@link cgeo.geocaching.utils.AngleUtils#getDirectionNow(float)}. When the direction is derived from the GPS,
 * it is altered so that the fix can still be applied as if the information came from the compass.
 */
public abstract class GeoDirHandler {

    public static final int UPDATE_GEODATA = 1 << 1;
    public static final int UPDATE_DIRECTION = 1 << 2;
    public static final int UPDATE_GEODIR = 1 << 3;
    public static final int LOW_POWER = 1 << 4;

    private static final CgeoApplication app = CgeoApplication.getInstance();

    /**
     * Update method called when new geodata is available. This method is called on the UI thread.
     * {@link #start(int)} must be called with the {@link #UPDATE_GEODATA} flag set.
     *
     * @param geoData the new geographical data
     */
    public void updateGeoData(final GeoData geoData) {
    }

    /**
     * Update method called when new direction is available. This method is called on the UI thread.
     * {@link #start(int)} must be called with the {@link #UPDATE_DIRECTION} flag set.
     *
     * @param direction the new direction
     */
    public void updateDirection(final float direction) {
    }

    /**
     * Update method called when new data is available. This method is called on the UI thread.
     * {@link #start(int)} must be called with the {@link #UPDATE_GEODIR} flag set.
     *
     * @param geoData the new geographical data
     * @param direction the new direction
     *
     * If the device goes fast enough, or if the compass use is not enabled in the settings,
     * the GPS direction information will be used instead of the compass one.
     */
    public void updateGeoDir(final GeoData geoData, final float direction) {
    }

    private static Observable<Float> fixedDirection() {
        return app.directionObservable().map(new Func1<Float, Float>() {
            @Override
            public Float call(final Float direction) {
                final GeoData geoData = app.currentGeo();
                return fixDirection(geoData, direction);
            }
        });

    }

    private static float fixDirection(final GeoData geoData, final float direction) {
        final boolean useGPSBearing = !Settings.isUseCompass() || geoData.getSpeed() > 5;
        return useGPSBearing ? AngleUtils.reverseDirectionNow(geoData.getBearing()) : direction;
    }

    private static <T> Observable<T> throttleIfNeeded(final Observable<T> observable, final long windowDuration, final TimeUnit unit) {
        return windowDuration > 0 ? observable.throttleFirst(windowDuration, unit) : observable;
    }

    /**
     * Register the current GeoDirHandler for GeoData and direction information (if the preferences allow it).
     *
     * @param flags a combination of UPDATE_GEODATA, UPDATE_DIRECTION, UPDATE_GEODIR, and LOW_POWER
     * @return a subscription which can be used to stop the handler
     */
    public Subscription start(final int flags) {
        return start(flags, 0, TimeUnit.SECONDS);
    }

    /**
     * Register the current GeoDirHandler for GeoData and direction information (if the preferences allow it).
     *
     * @param flags a combination of UPDATE_GEODATA, UPDATE_DIRECTION, UPDATE_GEODIR, and LOW_POWER
     * @param windowDuration if greater than 0, the size of the window duration during which no new value will be presented
     * @param unit the unit for the windowDuration
     * @return a subscription which can be used to stop the handler
     */
    public Subscription start(final int flags, final long windowDuration, final TimeUnit unit) {
        final CompositeSubscription subscriptions = new CompositeSubscription();
        final boolean lowPower = (flags & LOW_POWER) != 0;
        if ((flags & UPDATE_GEODATA) != 0) {
            subscriptions.add(throttleIfNeeded(app.geoDataObservable(lowPower), windowDuration, unit).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<GeoData>() {
                @Override
                public void call(final GeoData geoData) {
                    updateGeoData(geoData);
                }
            }));
        }
        if ((flags & UPDATE_DIRECTION) != 0) {
            subscriptions.add(throttleIfNeeded(fixedDirection(), windowDuration, unit).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Float>() {
                @Override
                public void call(final Float direction) {
                    updateDirection(direction);
                }
            }));
        }
        if ((flags & UPDATE_GEODIR) != 0) {
            // combineOnLatest() does not implement backpressure handling, so we need to explicitely use a backpressure operator there.
            subscriptions.add(throttleIfNeeded(Observable.combineLatest(app.geoDataObservable(lowPower), app.directionObservable(), new Func2<GeoData, Float, ImmutablePair<GeoData, Float>>() {
                @Override
                public ImmutablePair<GeoData, Float> call(final GeoData geoData, final Float direction) {
                    return ImmutablePair.of(geoData, fixDirection(geoData, direction));
                }
            }), windowDuration, unit).onBackpressureDrop().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<ImmutablePair<GeoData, Float>>() {
                @Override
                public void call(final ImmutablePair<GeoData, Float> geoDir) {
                    updateGeoDir(geoDir.left, geoDir.right);
                }
            }));
        }
        return subscriptions;
    }

}
