package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.IGeoData;
import cgeo.geocaching.settings.Settings;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.util.functions.Action1;

import java.util.concurrent.TimeUnit;

/**
 * GeoData and Direction handler.
 * <p>
 * To use this class, override at least one of {@link #updateDirection(float)} or {@link #updateGeoData(IGeoData)}. You
 * need to start the handler using one of
 * <ul>
 * <li>{@link #startDir()}</li>
 * <li>{@link #startGeo()}</li>
 * <li>{@link #startGeoAndDir()}</li>
 * </ul>
 * A good place might be the {@code onResume} method of the Activity. Stop the Handler accordingly in {@code onPause}.
 * </p>
 */
public abstract class GeoDirHandler {
    private static final CgeoApplication app = CgeoApplication.getInstance();

    private Subscription dirSubscription = null;
    private Subscription geoSubscription = null;

    /**
     * Update method called when new IGeoData is available.
     *
     * @param data
     *            the new data
     */
    public void updateGeoData(final IGeoData data) {
        // Override this in children
    }

    /**
     * Update method called when new direction data is available.
     *
     * @param direction
     *            the new direction
     */
    public void updateDirection(final float direction) {
        // Override this in children
    }

    /**
     * Register the current GeoDirHandler for GeoData information.
     */
    public synchronized void startGeo() {
        geoSubscription = app.currentGeoObject()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<IGeoData>() {
                    @Override
                    public void call(final IGeoData geoData) {
                        updateGeoData(geoData);
                    }
                });
    }

    /**
     * Register the current GeoDirHandler for direction information if the preferences
     * allow it.
     */
    public synchronized void startDir() {
        if (Settings.isUseCompass()) {
            dirSubscription = app.currentDirObject()
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Float>() {
                        @Override
                        public void call(final Float direction) {
                            updateDirection(direction);
                        }
                    });
        }
    }

    /**
     * Register the current GeoDirHandler for GeoData and direction information (if the
     * preferences allow it).
     */
    public void startGeoAndDir() {
        startGeo();
        startDir();
    }

    /**
     * Unregister the current GeoDirHandler for GeoData information.
     */
    public synchronized void stopGeo() {
        // Delay the unsubscription by 2.5 seconds, so that another activity has
        // the time to subscribe and the GPS receiver will not be turned down.
        if (geoSubscription != null) {
            final Subscription subscription = geoSubscription;
            geoSubscription = null;
            Observable.interval(2500, TimeUnit.MILLISECONDS).take(1).subscribe(new Action1<Long>() {
                @Override
                public void call(final Long aLong) {
                    subscription.unsubscribe();
                }
            });
        }
    }

    /**
     * Unregister the current GeoDirHandler for direction information.
     */
    public synchronized void stopDir() {
        if (dirSubscription != null) {
            dirSubscription.unsubscribe();
            dirSubscription = null;
        }
    }

    /**
     * Unregister the current GeoDirHandler for GeoData and direction information.
     */
    public void stopGeoAndDir() {
        stopGeo();
        stopDir();
    }
}
