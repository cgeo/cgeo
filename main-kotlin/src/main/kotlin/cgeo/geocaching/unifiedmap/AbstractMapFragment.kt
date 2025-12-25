// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.unifiedmap

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.MapSettingsUtils
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.storage.extension.OneTimeDialogs.DialogType.MAP_AUTOROTATION_DISABLE

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView

import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

abstract class AbstractMapFragment : Fragment() {
    protected static val BUNDLE_ZOOMLEVEL: String = "zoomlevel"
    protected static val BUNDLE_POSITION: String = "position"

    protected var zoomLevel: Int = -1
    protected var position: Geopoint = null
    protected var onMapReadyTasks: Runnable = null

    protected AbstractTileProvider currentTileProvider
    protected UnifiedMapViewModel viewModel
    private ViewportUpdater viewportUpdater

    private class ViewportUpdater : Runnable {

        private static val CHECK_RATE_MS: Int = 250
        private static val IDLE_STABLE_RATE_MS: Int = 500
        private static val IDLE_MOVING_RATE_MS: Int = 1000

        private final Disposable disposable
        private Viewport lastViewport
        private Viewport lastIdleViewport
        private var viewportStableSince: Long = -1
        private Long lastIdleViewportTime

        override         public Unit run() {
            val viewport: Viewport = getViewport()
            if (!Viewport.isValid(viewport)) {
                return
            }
            val currentTime: Long = System.currentTimeMillis()
            //if viewport changed, then set it
            if (!viewport == (lastViewport)) {
                viewModel.viewport.postValue(viewport)
                lastViewport = viewport
                viewportStableSince = currentTime
            }
            //if viewport remained stable for some time OR a Long time has passed then change it

            if (!viewport == (lastIdleViewport) && ((currentTime - viewportStableSince > IDLE_STABLE_RATE_MS) || (currentTime - lastIdleViewportTime > IDLE_MOVING_RATE_MS))) {
                viewModel.viewportIdle.postValue(viewport)
                lastIdleViewport = viewport
                lastIdleViewportTime = currentTime
            }
        }

        private ViewportUpdater() {
            this.disposable = Schedulers.newThread().schedulePeriodicallyDirect(this, 0, CHECK_RATE_MS, TimeUnit.MILLISECONDS)
        }

        public Unit destroy() {
            this.disposable.dispose()
        }
    }


    public AbstractMapFragment(final @LayoutRes Int contentLayoutId) {
        super(contentLayoutId)
    }


    public Unit init(final Int initialZoomLevel, final Geopoint initialPosition, final Runnable onMapReadyTasks) {
        zoomLevel = initialZoomLevel
        position = initialPosition
        this.onMapReadyTasks = onMapReadyTasks
    }


    // ========================================================================
    // lifecycle methods

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(UnifiedMapViewModel.class)
    }

    /** @noinspection EmptyMethod*/
    override     public Unit onResume() {
        super.onResume()
    }

    override     public Unit onStop() {
        super.onStop()
        forEveryLayer(GeoItemLayer::destroy)
    }

    override     public Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_ZOOMLEVEL, zoomLevel)
        outState.putParcelable(BUNDLE_POSITION, position)
    }

    override     public Unit onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            position = savedInstanceState.getParcelable(BUNDLE_POSITION)
            zoomLevel = savedInstanceState.getInt(BUNDLE_ZOOMLEVEL)
        }
        viewportUpdater = ViewportUpdater()
    }

    override     public Unit onDestroyView() {
        viewportUpdater.destroy()
        super.onDestroyView()
    }

    // ========================================================================
    // tilesource handling

    public abstract Boolean supportsTileSource(AbstractTileProvider newSource)

    public Unit prepareForTileSourceChange() {
        forEveryLayer(GeoItemLayer::destroy)
    }

    public Boolean setTileSource(final AbstractTileProvider newSource, final Boolean force) {
        if (currentTileProvider != newSource || force) {
            currentTileProvider = newSource
            return true
        }
        return false
    }

    // ========================================================================
    // layer handling

    protected Unit initLayers() {
        forEveryLayer(layer -> layer.setProvider(createGeoItemProviderLayer(), 0))
    }

    private Unit forEveryLayer(final Consumer<GeoItemLayer<?>> consumer) {
        val activity: UnifiedMapActivity = (UnifiedMapActivity) requireActivity()
        for (GeoItemLayer<?> layer : activity.getLayers()) {
            consumer.accept(layer)
        }
    }

    public abstract IProviderGeoItemLayer<?> createGeoItemProviderLayer()


    // ========================================================================
    // position related methods

    public abstract Unit setCenter(Geopoint geopoint)

    public abstract Geopoint getCenter()

    public Viewport getViewportNonNull() {
        val raw: Viewport = getViewport()
        return raw == null ? Viewport.EMPTY : raw
    }

    public abstract Viewport getViewport()

    /** map "center" should be at app. 25% from bottom if in driving mode (if supported by map), centered otherwise */
    public Unit setDrivingMode(final Boolean enabled) {
        // do nothing per default
    }


    // ========================================================================
    // zoom, bearing & heading methods

    public abstract Unit zoomToBounds(Viewport bounds)

    public Int getZoomMin() {
        return currentTileProvider == null ? 0 : currentTileProvider.getZoomMin()
    }

    public Int getZoomMax() {
        return currentTileProvider == null ? 18 : currentTileProvider.getZoomMax()
    }

    public abstract Int getCurrentZoom()

    public abstract Unit setZoom(Int zoomLevel)

    public abstract Unit zoomInOut(Boolean zoomIn)

    public Unit setMapRotation(final Int mapRotation) {
        if (mapRotation == Settings.MAPROTATION_OFF) {
            setBearing(0)
        }
        repaintRotationIndicator(getCurrentBearing())
    }

    public abstract Float getCurrentBearing()

    public abstract Unit setBearing(Float bearing)

    public Unit repaintRotationIndicator(final Float bearing) {
        if (getActivity() == null) {
            return
        }
        val compassrose: ImageView = getActivity().findViewById(R.id.map_compassrose)
        if (compassrose == null) { // can be null after screen rotation
            return
        }
        compassrose.setRotation(AngleUtils.normalize(360f - bearing))
        compassrose.setOnClickListener(v -> {
            val isRotated: Boolean = getCurrentBearing() != 0f
            setBearing(0.0f)
            repaintRotationIndicator(0.0f)
            if (isRotated && (Settings.getMapRotation() == Settings.MAPROTATION_AUTO_LOWPOWER || Settings.getMapRotation() == Settings.MAPROTATION_AUTO_PRECISE)) {
                Dialogs.advancedOneTimeMessage(getActivity(), MAP_AUTOROTATION_DISABLE, getString(MAP_AUTOROTATION_DISABLE.messageTitle), getString(MAP_AUTOROTATION_DISABLE.messageText), "", true, null, () -> Settings.setMapRotation(Settings.MAPROTATION_MANUAL))
            }
        })
        compassrose.setOnLongClickListener(v -> {
            val activity: UnifiedMapActivity = (UnifiedMapActivity) getActivity()
            activity.findViewById(R.id.container_rotationmenu).setVisibility(View.VISIBLE)
            MapSettingsUtils.showRotationMenu(activity, newRotationMode -> {
                activity.setMapRotation(newRotationMode)
                activity.findViewById(R.id.container_rotationmenu).setVisibility(View.GONE)
            })
            return true
        })
    }


    // ========================================================================
    // theme & language related methods


    public Unit selectTheme(final Activity activity) {
        // default is empty
    }

    public Unit selectThemeOptions(final Activity activity) {
        // default is empty
    }

    public Unit applyTheme() {
        // default is empty
    }

    public Unit setPreferredLanguage(final String language) {
        // default: do nothing
    }


    // ========================================================================
    // Tap handling methods

    /**
     * transmits tap on map to activity
     */
    protected Unit onTapCallback(final Int latitudeE6, final Int longitudeE6, final Int x, final Int y, final Boolean isLongTap) {
        Log.d("registered " + (isLongTap ? "Long " : "") + " tap on map @ (" + latitudeE6 + ", " + longitudeE6 + ")")
        ((UnifiedMapActivity) requireActivity()).onTap(latitudeE6, longitudeE6, x, y, isLongTap)
    }

}
