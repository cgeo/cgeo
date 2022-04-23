package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.functions.Action1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.oscim.core.BoundingBox;

public abstract class AbstractUnifiedMap<T> {

    protected AbstractTileProvider currentTileProvider;
    protected AbstractPositionLayer<T> positionLayer;
    protected Action1<UnifiedMapPosition> activityMapChangeListener = null;
    protected int mapRotation = Settings.MAPROTATION_OFF;
    protected int delayedZoomTo = -1;
    protected Geopoint delayedCenterTo = null;
    protected Runnable onMapReadyTasks = null;

    public void init(final AppCompatActivity activity, final int delayedZoomTo, @Nullable final Geopoint delayedCenterTo, final Runnable onMapReadyTasks) {
        mapRotation = Settings.getMapRotation();
        this.delayedZoomTo = delayedZoomTo;
        this.delayedCenterTo = delayedCenterTo;
        this.onMapReadyTasks = onMapReadyTasks;
    };

    public void prepareForTileSourceChange() {
        positionLayer = configPositionLayer(false);
    };

    public void setTileSource(final AbstractTileProvider newSource) {
        currentTileProvider = newSource;
    };

    public void applyTheme() {
        // default is empty
    };

    public void setPreferredLanguage(final String language) {
        // default: do nothing
    }

    public abstract float getCurrentBearing();
    public abstract void setBearing(float bearing);

    public void setMapRotation(final int mapRotation) {
        this.mapRotation = mapRotation;
        if (mapRotation == Settings.MAPROTATION_OFF) {
            setBearing(0);
        }
    }

    protected abstract void configMapChangeListener(boolean enable);
    public void setActivityMapChangeListener(@Nullable final Action1<UnifiedMapPosition> listener) {
        activityMapChangeListener = listener;
    }

    public abstract void setCenter(Geopoint geopoint);
    public abstract Geopoint getCenter();
    public abstract BoundingBox getBoundingBox();
    protected abstract AbstractPositionLayer<T> configPositionLayer(boolean create);

    protected void setDelayedCenterTo() {
        if (delayedCenterTo != null) {
            setCenter(delayedCenterTo);
            delayedCenterTo = null;
        }
    }

    // ========================================================================
    // zoom & heading methods

    public abstract void zoomToBounds(BoundingBox bounds);

    public int getZoomMin() {
        return currentTileProvider == null ? 0 : currentTileProvider.getZoomMin();
    };

    public int getZoomMax() {
        return currentTileProvider == null ? 0 : currentTileProvider.getZoomMax();
    };

    public abstract int getCurrentZoom();

    public abstract void setZoom(int zoomLevel);

    public float getHeading() {
        return positionLayer != null ? positionLayer.getCurrentHeading() : 0.0f;
    }

    /** adjust zoom to be in allowed zoom range for current map */
    protected void setDelayedZoomTo() {
        if (delayedZoomTo != -1) {
            setZoom(Math.max(Math.min(delayedZoomTo, getZoomMax()), getZoomMin()));
            delayedZoomTo = -1;
        }
    }

    // ========================================================================
    // Lifecycle methods

    protected void onResume() {
        positionLayer = configPositionLayer(true);
        configMapChangeListener(true);
    }

    protected void onPause() {
        positionLayer = configPositionLayer(false);
        configMapChangeListener(false);
    }

    protected void onDestroy() {
        // default is empty
    }

}
