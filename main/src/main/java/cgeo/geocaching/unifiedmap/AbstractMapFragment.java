package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
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


    public void init(final int initialZoomLevel, @Nullable final Geopoint initialPosition, final Runnable onMapReadyTasks) {
        zoomLevel = initialZoomLevel;
        position = initialPosition;
        this.onMapReadyTasks = onMapReadyTasks;
    }


    // ========================================================================
    // lifecycle methods

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


    // ========================================================================
    // tilesource handling

    public abstract boolean supportsTileSource(AbstractTileProvider newSource);

    public void prepareForTileSourceChange() {
        forEveryLayer(GeoItemLayer::destroy);
    }

    public boolean setTileSource(final AbstractTileProvider newSource, final boolean force) {
        if (currentTileProvider != newSource || force) {
            currentTileProvider = newSource;
            return true;
        }
        return false;
    }

    // ========================================================================
    // layer handling

    protected void initLayers() {
        forEveryLayer(layer -> layer.setProvider(createGeoItemProviderLayer(), 0));
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

    @NonNull
    public abstract BoundingBox getBoundingBox();

    public Viewport getViewport() {
        final BoundingBox bb = getBoundingBox();
        return new Viewport(new Geopoint(bb.getMinLatitude(), bb.getMinLongitude()), new Geopoint(bb.getMaxLatitude(), bb.getMaxLongitude()));
    }

    /** map "center" should be at app. 25% from bottom if in driving mode (if supported by map), centered otherwise */
    public void setDrivingMode(final boolean enabled) {
        // do nothing per default
    }


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

    public abstract void adaptLayoutForActionBar(@Nullable Boolean actionBarShowing);

    protected void adaptLayoutForActionBar(final View compassRose, @Nullable final Boolean actionBarShowing) {
        final UnifiedMapActivity activity = ((UnifiedMapActivity) requireActivity());
        int minHeight = 0;

        Boolean abs = actionBarShowing;
        if (actionBarShowing == null) {
            final ActionBar actionBar = activity.getSupportActionBar();
            abs = actionBar != null && actionBar.isShowing();
        }
        if (abs) {
            minHeight = activity.findViewById(R.id.actionBarSpacer).getHeight();
        }

        View v = activity.findViewById(R.id.distanceSupersize);
        if (v.getVisibility() != View.VISIBLE) {
            v = activity.findViewById(R.id.target);
        }
        if (v.getVisibility() == View.VISIBLE) {
            minHeight += v.getHeight();
        }

        compassRose.animate().translationY(minHeight).start(); // + ViewUtils.dpToPixel(25)).start();
    }

}
