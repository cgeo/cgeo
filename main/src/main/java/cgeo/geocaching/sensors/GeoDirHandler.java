package cgeo.geocaching.sensors;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.lang3.tuple.ImmutablePair;

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

    public static final int UPDATE_GEODATA = 1 << 0;
    public static final int UPDATE_DIRECTION = 1 << 1;
    public static final int UPDATE_GEODIR = 1 << 2;
    public static final int LOW_POWER = 1 << 3;

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

    public void updateDirectionData(final DirectionData directionData) {
        updateDirection(directionData.getDirection());
    }


    /**
     * Update method called when new data is available. This method is called on the UI thread.
     * {@link #start(int)} must be called with the {@link #UPDATE_GEODIR} flag set.
     *
     * @param geoData   the new geographical data
     * @param direction the new direction
     *
     *                  If the device goes fast enough, or if the compass use is not enabled in the settings,
     *                  the GPS direction information will be used instead of the compass one.
     */
    public void updateGeoDir(@NonNull final GeoData geoData, final float direction) {
    }

    public void updateGeoDirData(@NonNull final GeoData geoData, final DirectionData directionData) {
        updateGeoDir(geoData, directionData.getDirection());
    }


    private static <T> Flowable<T> throttleIfNeeded(final Observable<T> observable, final long windowDuration, final TimeUnit unit) {
        return (windowDuration > 0 ? observable.throttleFirst(windowDuration, unit) : observable).toFlowable(BackpressureStrategy.LATEST);
    }

    /**
     * Register the current GeoDirHandler for GeoData and direction information (if the preferences allow it).
     *
     * @param flags a combination of UPDATE_GEODATA, UPDATE_DIRECTION, UPDATE_GEODIR, and LOW_POWER
     * @return a disposable which can be used to stop the handler
     */
    public Disposable start(final int flags) {
        return start(flags, 0, TimeUnit.SECONDS);
    }

    /**
     * Register the current GeoDirHandler for GeoData and direction information (if the preferences allow it).
     *
     * @param flags          a combination of UPDATE_GEODATA, UPDATE_DIRECTION, UPDATE_GEODIR, and LOW_POWER
     * @param windowDuration if greater than 0, the size of the window duration during which no new value will be presented
     * @param unit           the unit for the windowDuration
     * @return a disposable which can be used to stop the handler
     */
    public Disposable start(final int flags, final long windowDuration, final TimeUnit unit) {
        final CompositeDisposable disposables = new CompositeDisposable();
        final boolean lowPower = (flags & LOW_POWER) != 0;
        final Sensors sensors = Sensors.getInstance();

        if ((flags & UPDATE_GEODATA) != 0) {
            disposables.add(throttleIfNeeded(sensors.geoDataObservable(lowPower).observeOn(AndroidSchedulers.mainThread()), windowDuration, unit).subscribe(this::updateGeoData));
        }
        if ((flags & UPDATE_DIRECTION) != 0) {
            disposables.add(throttleIfNeeded(sensors.directionDataObservable().observeOn(AndroidSchedulers.mainThread()), windowDuration, unit).subscribe(this::updateDirectionData));
        }
        if ((flags & UPDATE_GEODIR) != 0) {
            // combineOnLatest() does not implement backpressure handling, so we need to explicitly use a backpressure operator there.
            disposables.add(throttleIfNeeded(Observable.combineLatest(sensors.geoDataObservable(lowPower), sensors.directionDataObservable(), ImmutablePair::of).observeOn(AndroidSchedulers.mainThread()), windowDuration, unit).subscribe(geoDir -> updateGeoDirData(geoDir.left, geoDir.right)));
        }
        return disposables;
    }

}
