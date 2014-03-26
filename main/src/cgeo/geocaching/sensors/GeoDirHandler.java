package cgeo.geocaching.sensors;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.settings.Settings;

import org.apache.commons.lang3.tuple.ImmutablePair;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * GeoData and Direction handler.
 * <p>
 * To use this class, override {@link #updateGeoDir(IGeoData, float)}. You need to start the handler using
 * {@link #start()}. A good place to do so might be the {@code onResume} method of the Activity. Stop the Handler
 * accordingly in {@code onPause}.
 */
public abstract class GeoDirHandler {
    private static final CgeoApplication app = CgeoApplication.getInstance();

    /**
     * Update method called when new data is available. This method is called on the UI thread.
     *
     * @param geoData the new geographical data
     * @param direction the new direction
     *
     * If the device goes fast enough, or if the compass use is not enabled in the settings,
     * the GPS direction information will be used instead of the compass one.
     */
    public void updateGeoDir(@SuppressWarnings("unused") final IGeoData geoData, @SuppressWarnings("unused") final float direction) {
    }

    private void handleGeoDir(final ImmutablePair<IGeoData, Float> geoDir) {
        final IGeoData geoData = geoDir.left;
        final boolean useGPSBearing = !Settings.isUseCompass() || geoData.getSpeed() > 5;
        updateGeoDir(geoData, useGPSBearing ? geoData.getBearing() : geoDir.right);
    }

    /**
     * Register the current GeoDirHandler for GeoData and direction information (if the
     * preferences allow it).
     */
    public Subscription start() {
        return app.geoDirObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<ImmutablePair<IGeoData, Float>>() {
            @Override
            public void call(final ImmutablePair<IGeoData, Float> geoDir) {
                handleGeoDir(geoDir);
            }
        });
    }

}
