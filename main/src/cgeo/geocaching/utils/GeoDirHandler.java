package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.IGeoData;
import cgeo.geocaching.settings.Settings;

import android.os.Handler;
import android.os.Message;

/**
 * GeoData and Direction handler. Manipulating geodata and direction information
 * through a GeoDirHandler ensures that all listeners are registered from a {@link android.os.Looper} thread.
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
public abstract class GeoDirHandler extends Handler implements IObserver<Object> {

    private static final int OBSERVABLE = 1 << 1;
    private static final int START_GEO = 1 << 2;
    private static final int START_DIR = 1 << 3;
    private static final int STOP_GEO = 1 << 4;
    private static final int STOP_DIR = 1 << 5;

    private static final CgeoApplication app = CgeoApplication.getInstance();

    @Override
    final public void handleMessage(final Message message) {
        if ((message.what & START_GEO) != 0) {
            app.addGeoObserver(this);
        }

        if ((message.what & START_DIR) != 0) {
            app.addDirectionObserver(this);
        }

        if ((message.what & STOP_GEO) != 0) {
            app.deleteGeoObserver(this);
        }

        if ((message.what & STOP_DIR) != 0) {
            app.deleteDirectionObserver(this);
        }

        if ((message.what & OBSERVABLE) != 0) {
            if (message.obj instanceof IGeoData) {
                updateGeoData((IGeoData) message.obj);
            } else {
                updateDirection((Float) message.obj);
            }
        }
    }

    @Override
    final public void update(final Object o) {
        obtainMessage(OBSERVABLE, o).sendToTarget();
    }

    /**
     * Update method called when new IGeoData is available.
     *
     * @param data
     *            the new data
     */
    protected void updateGeoData(final IGeoData data) {
        // Override this in children
    }

    /**
     * Update method called when new direction data is available.
     *
     * @param direction
     *            the new direction
     */
    protected void updateDirection(final float direction) {
        // Override this in children
    }

    /**
     * Register the current GeoDirHandler for GeoData information.
     */
    public void startGeo() {
        sendEmptyMessage(START_GEO);
    }

    /**
     * Register the current GeoDirHandler for direction information if the preferences
     * allow it.
     */
    public void startDir() {
        if (Settings.isUseCompass()) {
            sendEmptyMessage(START_DIR);
        }
    }

    /**
     * Register the current GeoDirHandler for GeoData and direction information (if the
     * preferences allow it).
     */
    public void startGeoAndDir() {
        sendEmptyMessage(START_GEO | (Settings.isUseCompass() ? START_DIR : 0));
    }

    /**
     * Unregister the current GeoDirHandler for GeoData information.
     */
    public void stopGeo() {
        sendEmptyMessage(STOP_GEO);
    }

    /**
     * Unregister the current GeoDirHandler for direction information.
     */
    public void stopDir() {
        sendEmptyMessage(STOP_DIR);
    }

    /**
     * Unregister the current GeoDirHandler for GeoData and direction information.
     */
    public void stopGeoAndDir() {
        sendEmptyMessage(STOP_GEO | STOP_DIR);
    }
}
