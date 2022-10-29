package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import org.oscim.core.BoundingBox;


public abstract class AbstractUnifiedMapView<T> {

    protected WeakReference<UnifiedMapActivity> activityRef;
    protected AbstractTileProvider currentTileProvider;
    protected AbstractPositionLayer<T> positionLayer;
    protected Action1<UnifiedMapPosition> activityMapChangeListener = null;
    protected Runnable resetFollowMyLocationListener = null;
    protected int mapRotation = Settings.MAPROTATION_OFF;
    protected int delayedZoomTo = -1;
    protected Geopoint delayedCenterTo = null;
    protected Runnable onMapReadyTasks = null;
    protected boolean usesOwnBearingIndicator = true;

    protected View mMapView = null;
    protected View rootView = null;

    public void init(final UnifiedMapActivity activity, final int delayedZoomTo, @Nullable final Geopoint delayedCenterTo, final Runnable onMapReadyTasks) {
        activityRef = new WeakReference<>(activity);
        mapRotation = Settings.getMapRotation();
        this.delayedZoomTo = delayedZoomTo;
        this.delayedCenterTo = delayedCenterTo;
        this.onMapReadyTasks = onMapReadyTasks;
    }

    public void prepareForTileSourceChange() {
        positionLayer = configPositionLayer(false);
    }

    public void setTileSource(final AbstractTileProvider newSource) {
        currentTileProvider = newSource;
    }

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

    public void setResetFollowMyLocationListener(@Nullable final Runnable listener) {
        resetFollowMyLocationListener = listener;
    }

    public abstract void setCenter(Geopoint geopoint);

    public abstract Geopoint getCenter();

    public abstract BoundingBox getBoundingBox();

    public Viewport getViewport() {
        final BoundingBox bb = getBoundingBox();
        return new Viewport(new Geopoint(bb.getMinLatitude(), bb.getMinLongitude()), new Geopoint(bb.getMaxLatitude(), bb.getMaxLongitude()));
    }

    protected abstract AbstractPositionLayer<T> configPositionLayer(boolean create);

    protected void setDelayedCenterTo() {
        if (delayedCenterTo != null) {
            setCenter(delayedCenterTo);
            delayedCenterTo = null;
        }
    }

    protected abstract AbstractGeoitemLayer createGeoitemLayers(AbstractTileProvider tileProvider);

    // ========================================================================
    // theme related methods

    public void selectTheme(final Activity activity) {
        // default is empty
    }

    public void selectThemeOptions(final Activity activity) {
        // default is empty
    }

    public void applyTheme() {
        // default is empty
    }

    // ========================================================================
    // zoom & heading methods

    public abstract void zoomToBounds(Viewport bounds);

    public int getZoomMin() {
        return currentTileProvider == null ? 0 : currentTileProvider.getZoomMin();
    }

    public int getZoomMax() {
        return currentTileProvider == null ? 0 : currentTileProvider.getZoomMax();
    }

    public abstract int getCurrentZoom();

    public abstract void setZoom(int zoomLevel);

    public float getHeading() {
        return positionLayer != null ? positionLayer.getCurrentHeading() : 0.0f;
    }

    /**
     * adjust zoom to be in allowed zoom range for current map
     */
    protected void setDelayedZoomTo() {
        if (delayedZoomTo != -1) {
            setZoom(Math.max(Math.min(delayedZoomTo, getZoomMax()), getZoomMin()));
            delayedZoomTo = -1;
        }
    }

    // ========================================================================
    // Map progressbar handling

    public void showSpinner() {
        final UnifiedMapActivity activity = activityRef.get();
        if (activity != null) {
            final View spinner = activity.findViewById(R.id.map_progressbar);
            if (spinner != null) {
                spinner.setVisibility(View.VISIBLE);
            }
        }
    }

    public void hideSpinner() {
        final UnifiedMapActivity activity = activityRef.get();
        if (activity != null) {
            final View spinner = activity.findViewById(R.id.map_progressbar);
            if (spinner != null) {
                spinner.setVisibility(View.GONE);
            }
        }
    }

    // ========================================================================
    // Map tap handling

    /** transmits tap on map to activity */
    protected void onTapCallback(final int latitudeE6, final int longitudeE6, final boolean isLongTap) {
        final UnifiedMapActivity activity = activityRef.get();
        if (activity == null) {
            throw new IllegalStateException("map tap handler: lost connection to map activity");
        }
        activity.onTap(latitudeE6, longitudeE6, isLongTap);
    }

    // ========================================================================
    // additional menu entries

    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        return false;
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
