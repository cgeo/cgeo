package cgeo.geocaching.sensors;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.settings.Settings;

import org.apache.commons.lang3.tuple.ImmutablePair;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;

/**
 * GeoData and Direction handler.
 * <p>
 * To use this class, override {@link #updateGeoDir(IGeoData, float)}. You need to start the handler using
 * {@link #start()}. A good place to do so might be the {@code onResume} method of the Activity. Stop the Handler
 * accordingly in {@code onPause}.
 *
 * The direction is always relative to the top of the device (natural direction), and that it must
 * be fixed using {@link DirectionProvider#getDirectionNow(float)}. When the direction is derived from the GPS,
 * it is altered so that the fix can still be applied as if the information came from the compass.
 */
public abstract class GeoDirHandler {

    public static final int UPDATE_GEODATA = 1 << 1;
    public static final int UPDATE_DIRECTION = 1 << 2;
    public static final int UPDATE_GEODIR = 1 << 3;

    private static final CgeoApplication app = CgeoApplication.getInstance();

    /**
     * Update method called when new geodata is available. This method is called on the UI thread.
     * {@link #start(int)} must be called with the {@link #UPDATE_GEODATA} flag set.
     *
     * @param geoData the new geographical data
     */
    public void updateGeoData(@SuppressWarnings("unused") final IGeoData geoData) {
    }

    /**
     * Update method called when new direction is available. This method is called on the UI thread.
     * {@link #start(int)} must be called with the {@link #UPDATE_DIRECTION} flag set.
     *
     * @param direction the new direction
     */
    public void updateDirection(@SuppressWarnings("unused") final float direction) {
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
    public void updateGeoDir(@SuppressWarnings("unused") final IGeoData geoData, @SuppressWarnings("unused") final float direction) {
    }

    private static Observable<Float> fixedDirection() {
        return app.directionObservable().map(new Func1<Float, Float>() {
            @Override
            public Float call(final Float direction) {
                final IGeoData geoData = app.currentGeo();
                return fixDirection(geoData, direction);
            }
        });

    }

    private static float fixDirection(final IGeoData geoData, final float direction) {
        final boolean useGPSBearing = !Settings.isUseCompass() || geoData.getSpeed() > 5;
        return useGPSBearing ? DirectionProvider.reverseDirectionNow(geoData.getBearing()) : direction;
    }

    /**
     * Register the current GeoDirHandler for GeoData and direction information (if the
     * preferences allow it).
     */
    public Subscription start(final int flags) {
        final CompositeSubscription subscriptions = new CompositeSubscription();
        if ((flags & UPDATE_GEODATA) != 0) {
            subscriptions.add(app.geoDataObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<IGeoData>() {
                @Override
                public void call(final IGeoData geoData) {
                    updateGeoData(geoData);
                }
            }));
        }
        if ((flags & UPDATE_DIRECTION) != 0) {
            subscriptions.add(fixedDirection().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Float>() {
                @Override
                public void call(final Float direction) {
                    updateDirection(direction);
                }
            }));
        }
        if ((flags & UPDATE_GEODIR) != 0) {
            subscriptions.add(Observable.combineLatest(app.geoDataObservable(), app.directionObservable(), new Func2<IGeoData, Float, ImmutablePair<IGeoData, Float>>() {
                @Override
                public ImmutablePair<IGeoData, Float> call(final IGeoData geoData, final Float direction) {
                    return ImmutablePair.of(geoData, fixDirection(geoData, direction));
                }
            }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<ImmutablePair<IGeoData, Float>>() {
                @Override
                public void call(final ImmutablePair<IGeoData, Float> geoDir) {
                    updateGeoDir(geoDir.left, geoDir.right);
                }
            }));
        }
        return subscriptions;
    }

}
