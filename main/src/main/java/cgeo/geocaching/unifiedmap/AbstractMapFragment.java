package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.oscim.core.BoundingBox;

public abstract class AbstractMapFragment extends Fragment {
    protected int zoomLevel = -1;
    protected Geopoint position = null;
    protected Runnable onMapReadyTasks = null;

    protected AbstractTileProvider currentTileProvider;
    protected UnifiedMapViewModel viewModel;


    public AbstractMapFragment(final @LayoutRes int contentLayoutId) {
        super(contentLayoutId);
    }


    public void setTileSource(final AbstractTileProvider newSource) {
        currentTileProvider = newSource;
    }

    public abstract boolean supportsTileSource(AbstractTileProvider newSource);

    public void init(final int initialZoomLevel, @Nullable final Geopoint initialPosition, final Runnable onMapReadyTasks) {
        zoomLevel = initialZoomLevel;
        position = initialPosition;
        this.onMapReadyTasks = onMapReadyTasks;
    }

    protected void initLayers() {
        forEveryLayer(layer -> layer.setProvider(createGeoItemProviderLayer(), 0));
    }

    public void prepareForTileSourceChange() {
        forEveryLayer(GeoItemLayer::destroy);
    }

    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(UnifiedMapViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        forEveryLayer(GeoItemLayer::destroy);
    }

    private void forEveryLayer(final Consumer<GeoItemLayer<?>> consumer) {
        final UnifiedMapActivity activity = (UnifiedMapActivity) requireActivity();
        for (GeoItemLayer<?> layer : activity.getLayers()) {
            consumer.accept(layer);
        }
    }

    public abstract IProviderGeoItemLayer<?> createGeoItemProviderLayer();


    // ========================================================================
    // position related methods

    public abstract void setCenter(Geopoint geopoint);

    public abstract Geopoint getCenter();

    public abstract BoundingBox getBoundingBox();

    public Viewport getViewport() {
        final BoundingBox bb = getBoundingBox();
        return new Viewport(new Geopoint(bb.getMinLatitude(), bb.getMinLongitude()), new Geopoint(bb.getMaxLatitude(), bb.getMaxLongitude()));
    }

    /** map "center" should be at app. 25% from bottom if in driving mode (if supported by map), centered otherwise */
    public void setDrivingMode(final boolean enabled) {
        // do nothing per default
    }

//    protected void setDelayedCenterTo() {
//        if (delayedCenterTo != null) {
//            setCenter(delayedCenterTo);
//            delayedCenterTo = null;
//        }
//    }

//    public void setResetFollowMyLocationListener(@Nullable final Runnable listener) {
//        resetFollowMyLocationListener = listener;
//    }

    // ========================================================================
    // zoom, bearing & heading methods

    public abstract void zoomToBounds(Viewport bounds);

    public int getZoomMin() {
        return currentTileProvider == null ? 0 : currentTileProvider.getZoomMin();
    }

    public int getZoomMax() {
        return currentTileProvider == null ? 0 : currentTileProvider.getZoomMax();
    }

    public abstract int getCurrentZoom();

    public abstract void setZoom(int zoomLevel);

    public abstract void zoomInOut(boolean zoomIn);

    public void setMapRotation(final int mapRotation) {
        if (mapRotation == Settings.MAPROTATION_OFF) {
            setBearing(0);
        }
    }

    public abstract float getCurrentBearing();

    public abstract void setBearing(float bearing);

//    /**
//     * adjust zoom to be in allowed zoom range for current map
//     */
//    protected void setDelayedZoomTo() {
//        if (delayedZoomTo != -1) {
//            setZoom(Math.max(Math.min(delayedZoomTo, getZoomMax()), getZoomMin()));
//            delayedZoomTo = -1;
//        }
//    }

    // ========================================================================
    // theme & language related methods

    public void selectTheme(final Activity activity) {
        // default is empty
    }

    public void selectThemeOptions(final Activity activity) {
        // default is empty
    }

    public void applyTheme() {
        // default is empty
    }

    public void setPreferredLanguage(final String language) {
        // default: do nothing
    }


    // ========================================================================
    // Tap handling methods

    /**
     * transmits tap on map to activity
     */
    protected void onTapCallback(final int latitudeE6, final int longitudeE6, final int x, final int y, final boolean isLongTap) {
        Log.d("registered " + (isLongTap ? "long " : "") + " tap on map @ (" + latitudeE6 + ", " + longitudeE6 + ")");
        ((UnifiedMapActivity) requireActivity()).onTap(latitudeE6, longitudeE6, x, y, isLongTap);
    }

    protected void adaptLayoutForActionbar(final boolean actionBarShowing) {
        // default is empty
    }

}
