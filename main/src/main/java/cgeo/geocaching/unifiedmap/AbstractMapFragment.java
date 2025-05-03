package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapSettingsUtils;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.HideActionBarUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.storage.extension.OneTimeDialogs.DialogType.MAP_AUTOROTATION_DISABLE;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class AbstractMapFragment extends Fragment {
    protected static final String BUNDLE_ZOOMLEVEL = "zoomlevel";
    protected static final String BUNDLE_POSITION = "position";

    protected int zoomLevel = -1;
    protected Geopoint position = null;
    protected Runnable onMapReadyTasks = null;

    protected AbstractTileProvider currentTileProvider;
    protected UnifiedMapViewModel viewModel;
    private ViewportUpdater viewportUpdater;

    private final class ViewportUpdater implements Runnable {

        private static final int CHECK_RATE_MS = 250;
        private static final int IDLE_STABLE_RATE_MS = 500;
        private static final int IDLE_MOVING_RATE_MS = 1000;

        private final Disposable disposable;
        private Viewport lastViewport;
        private Viewport lastIdleViewport;
        private long viewportStableSince = -1;
        private long lastIdleViewportTime;

        @Override
        public void run() {
            final Viewport viewport = getViewport();
            if (!Viewport.isValid(viewport)) {
                return;
            }
            final long currentTime = System.currentTimeMillis();
            //if viewport changed, then set it
            if (!viewport.equals(lastViewport)) {
                viewModel.viewport.postValue(viewport);
                lastViewport = viewport;
                viewportStableSince = currentTime;
            }
            //if viewport remained stable for some time OR a long time has passed then change it

            if (!viewport.equals(lastIdleViewport) && ((currentTime - viewportStableSince > IDLE_STABLE_RATE_MS) || (currentTime - lastIdleViewportTime > IDLE_MOVING_RATE_MS))) {
                viewModel.viewportIdle.postValue(viewport);
                lastIdleViewport = viewport;
                lastIdleViewportTime = currentTime;
            }
        }

        private ViewportUpdater() {
            this.disposable = Schedulers.newThread().schedulePeriodicallyDirect(this, 0, CHECK_RATE_MS, TimeUnit.MILLISECONDS);
        }

        public void destroy() {
            this.disposable.dispose();
        }
    }


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

    /** @noinspection EmptyMethod*/
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        forEveryLayer(GeoItemLayer::destroy);
    }

    @Override
    public void onSaveInstanceState(final @NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_ZOOMLEVEL, zoomLevel);
        outState.putParcelable(BUNDLE_POSITION, position);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            position = savedInstanceState.getParcelable(BUNDLE_POSITION);
            zoomLevel = savedInstanceState.getInt(BUNDLE_ZOOMLEVEL);
        }
        viewportUpdater = new ViewportUpdater();
    }

    @Override
    public void onDestroyView() {
        viewportUpdater.destroy();
        super.onDestroyView();
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
    public Viewport getViewportNonNull() {
        final Viewport raw = getViewport();
        return raw == null ? Viewport.EMPTY : raw;
    }

    @Nullable
    public abstract Viewport getViewport();

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
        return currentTileProvider == null ? 18 : currentTileProvider.getZoomMax();
    }

    public abstract int getCurrentZoom();

    public abstract void setZoom(int zoomLevel);

    public abstract void zoomInOut(boolean zoomIn);

    public void setMapRotation(final int mapRotation) {
        if (mapRotation == Settings.MAPROTATION_OFF) {
            setBearing(0);
        }
        repaintRotationIndicator(getCurrentBearing());
    }

    public abstract float getCurrentBearing();

    public abstract void setBearing(float bearing);

    public void repaintRotationIndicator(final float bearing) {
        if (getActivity() == null) {
            return;
        }
        final ImageView compassrose = getActivity().findViewById(R.id.map_compassrose);
        if (compassrose == null) { // can be null after screen rotation
            return;
        }
        compassrose.setRotation(AngleUtils.normalize(360f - bearing));
        compassrose.setOnClickListener(v -> {
            final boolean isRotated = getCurrentBearing() != 0f;
            setBearing(0.0f);
            repaintRotationIndicator(0.0f);
            if (isRotated && (Settings.getMapRotation() == Settings.MAPROTATION_AUTO_LOWPOWER || Settings.getMapRotation() == Settings.MAPROTATION_AUTO_PRECISE)) {
                Dialogs.advancedOneTimeMessage(getActivity(), MAP_AUTOROTATION_DISABLE, getString(MAP_AUTOROTATION_DISABLE.messageTitle), getString(MAP_AUTOROTATION_DISABLE.messageText), "", true, null, () -> Settings.setMapRotation(Settings.MAPROTATION_MANUAL));
            }
        });
        compassrose.setOnLongClickListener(v -> {
            final UnifiedMapActivity activity = (UnifiedMapActivity) getActivity();
            activity.findViewById(R.id.container_rotationmenu).setVisibility(View.VISIBLE);
            MapSettingsUtils.showRotationMenu(activity, newRotationMode -> {
                activity.setMapRotation(newRotationMode);
                activity.findViewById(R.id.container_rotationmenu).setVisibility(View.GONE);
            });
            return true;
        });
    }


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

    public void adaptLayoutForActionBar(final @Nullable Boolean actionBarShowing) {
        HideActionBarUtils.adaptLayoutForActionBarHelper((AppCompatActivity) requireActivity(), actionBarShowing, null);
    }

}
