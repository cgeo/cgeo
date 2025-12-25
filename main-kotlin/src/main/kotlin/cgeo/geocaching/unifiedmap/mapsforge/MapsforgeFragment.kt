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

package cgeo.geocaching.unifiedmap.mapsforge

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.unifiedmap.AbstractMapFragment
import cgeo.geocaching.unifiedmap.UnifiedMapActivity
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer
import cgeo.geocaching.unifiedmap.geoitemlayer.MapsforgeV6GeoItemLayer
import cgeo.geocaching.unifiedmap.layers.MBTilesLayerHelper
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeTileProvider
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.util.Pair

import org.apache.commons.lang3.StringUtils
import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.core.util.Parameters
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.mbtiles.TileMBTilesLayer
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.model.Model
import org.mapsforge.map.model.common.Observer
import org.mapsforge.map.view.InputListener
import org.oscim.core.BoundingBox

class MapsforgeFragment : AbstractMapFragment() : Observer {

    private MapView mMapView
    private TileCache tileCache
    private MapsforgeThemeHelper themeHelper
    private View mapAttribution
    private var doReapplyTheme: Boolean = false

    public MapsforgeFragment() {
        super(R.layout.unifiedmap_mapsforge_fragment)
        AndroidGraphicFactory.createInstance(CgeoApplication.getInstance())
    }

    override     public Unit onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState)

        // set some parameters for Mapsforge library
        // Support for multi-threaded map painting
        Parameters.NUMBER_OF_THREADS = Settings.getMapOsmThreads()
        // Use fast parent tile rendering to increase performance when zooming in
        Parameters.PARENT_TILES_RENDERING = Parameters.ParentTilesRendering.SPEED

        mMapView = requireView().findViewById(R.id.mapViewMapsforge)
        setMapRotation(Settings.getMapRotation())
        mapAttribution = requireView().findViewById(R.id.map_attribution)

        // create tile cache (taking driving mode into account when calculating tile cache size)
        setDrivingMode(true)
        tileCache = AndroidUtil.createTileCache(requireContext(), "mapcache", mMapView.getModel().displayModel.getTileSize(), 2f, mMapView.getModel().frameBufferModel.getOverdrawFactor())
        setDrivingMode(false)

        themeHelper = MapsforgeThemeHelper(requireActivity())

        if (position != null) {
            setCenter(position)
        }
        setZoom(zoomLevel)

        mMapView.addInputListener(InputListener() {
            override             public Unit onMoveEvent() {
                if (Boolean.TRUE == (viewModel.followMyLocation.getValue())) {
                    viewModel.followMyLocation.setValue(false)
                }
            }

            override             public Unit onZoomEvent() {

            }
        })

        if (onMapReadyTasks != null) {
            onMapReadyTasks.run()
        }
    }

    override     public Unit onChange() {
        repaintRotationIndicator(getCurrentBearing())
        ((UnifiedMapActivity) requireActivity()).notifyZoomLevel(getCurrentZoom())
    }

    private Unit startMap() {
        mMapView.getMapScaleBar().setVisible(true)
        mMapView.getMapScaleBar().setDistanceUnitAdapter(mMapView.getMapScaleBar().getDistanceUnitAdapter())
        mMapView.setBuiltInZoomControls(false)
        mMapView.setZoomLevelMax((Byte) currentTileProvider.getZoomMax())

        //make room for map attribution icon button
        val mapAttPx: Int = Math.round(this.getResources().getDisplayMetrics().density * 30)
        mMapView.getMapScaleBar().setMarginHorizontal(mapAttPx)

        if (Settings.getMapBackgroundMapLayer() && currentTileProvider.supportsBackgroundMaps()) {
            for (TileMBTilesLayer backgroundMap : MBTilesLayerHelper.getBitmapTileLayersMapsforge(requireActivity(), mMapView)) {
                mMapView.getLayerManager().getLayers().add(backgroundMap)
            }
        }

        if (this.mapAttribution != null) {
            this.mapAttribution.setOnClickListener(v -> displayMapAttribution())
        }
    }

    private Unit displayMapAttribution() {
        val mapAttribution: Pair<String, Boolean> = currentTileProvider.getMapAttribution()
        if (mapAttribution == null || StringUtils.isBlank(mapAttribution.first)) {
            return
        }

        //create text message
        CharSequence message = HtmlCompat.fromHtml(mapAttribution.first, HtmlCompat.FROM_HTML_MODE_LEGACY)
        if (mapAttribution.second) {
            val s: SpannableString = SpannableString(message)
            ViewUtils.safeAddLinks(s, Linkify.ALL)
            message = s
        }

        val alertDialog: AlertDialog = Dialogs.newBuilder(getContext())
                .setTitle(requireContext().getString(R.string.map_source_attribution_dialog_title))
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, pos) -> dialog.dismiss())
                .create()
        alertDialog.show()

        // Make the URLs in TextView clickable. Must be called after show()
        // Note: we do NOT use the "setView()" option of AlertDialog because this screws up the layout
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance())
    }


    // ========================================================================
    // lifecycle methods

    override     public Unit onStart() {
        super.onStart()
        initLayers()
        if (doReapplyTheme) {
            applyTheme(); // @todo: There must be a less resource-intensive way of applying style-changes...
            doReapplyTheme = false
        }
        mMapView.getLayerManager().getLayers().add(MapEventsReceiver())
    }

    override     public Unit onResume() {
        super.onResume()
        // mMapView.onResume()
        mMapView.getModel().mapViewPosition.addObserver(this)
    }

    override     public Unit onPause() {
        // mMapView.onPause()
        super.onPause()

        mMapView.getModel().mapViewPosition.removeObserver(this)
    }

    override     public Unit onDestroyView() {
//        themeHelper.disposeTheme();
        mMapView.destroyAll()
        super.onDestroyView()
    }


    // ========================================================================
    // tilesource handling

    public TileCache getTileCache() {
        return tileCache
    }

    override     public Boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource is AbstractMapsforgeTileProvider
    }

    override     public Unit prepareForTileSourceChange() {
        ((AbstractMapsforgeTileProvider) currentTileProvider).prepareForTileSourceChange(mMapView)
        super.prepareForTileSourceChange()
    }

    override     public Boolean setTileSource(final AbstractTileProvider newSource, final Boolean force) {
        val needsUpdate: Boolean = super.setTileSource(newSource, force)
        if (needsUpdate) {
            ((AbstractMapsforgeTileProvider) currentTileProvider).addTileLayer(this, mMapView)
            startMap()
        }
        return needsUpdate
    }


    // ========================================================================
    // layer handling

    override     public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return MapsforgeV6GeoItemLayer(mMapView)
    }


    // ========================================================================
    // position related methods

    override     public Unit setCenter(final Geopoint geopoint) {
        mMapView.setCenter(LatLong(geopoint.getLatitude(), geopoint.getLongitude()))
    }

    override     public Geopoint getCenter() {
        val pos: LatLong = mMapView.getModel().mapViewPosition.getMapPosition().latLong
        return Geopoint(pos.getLatitude(), pos.getLongitude())
    }

    override     public Viewport getViewport() {
        if (mMapView == null) {
            return null
        }
        val model: Model = mMapView.getModel()
        if (model == null) {
            return null
        }
        val center: LatLong = model.mapViewPosition.getCenter()
        return Viewport(Geopoint(center.latitude, center.longitude), getLatitudeSpan(), getLongitudeSpan())
    }

    private Double getLatitudeSpan() {
        Double span = 0
        val mapSize: Long = MercatorProjection.getMapSize(mMapView.getModel().mapViewPosition.getZoomLevel(), mMapView.getModel().displayModel.getTileSize())
        val center: Point = MercatorProjection.getPixelAbsolute(mMapView.getModel().mapViewPosition.getCenter(), mapSize)

        if (mMapView.getHeight() > 0) {
            try {
                val low: LatLong = mercatorFromPixels(center.x, center.y - mMapView.getHeight() / 2.0, mapSize)
                val high: LatLong = mercatorFromPixels(center.x, center.y + mMapView.getHeight() / 2.0, mapSize)
                span = Math.abs(high.latitude - low.latitude)
            } catch (final IllegalArgumentException ex) {
                //should never happen due to handling in "mercatorFromPixels", but leave it here just in case
                Log.w("Exception when calculating longitude span (center:" + center + ", h/w:" + mMapView.getDimension(), ex)
            }
        }
        return span
    }

    private Double getLongitudeSpan() {
        Double span = 0
        val mapSize: Long = MercatorProjection.getMapSize(mMapView.getModel().mapViewPosition.getZoomLevel(), mMapView.getModel().displayModel.getTileSize())
        val center: Point = MercatorProjection.getPixelAbsolute(mMapView.getModel().mapViewPosition.getCenter(), mapSize)

        if (mMapView.getWidth() > 0) {
            try {
                val low: LatLong = mercatorFromPixels(center.x - mMapView.getWidth() / 2.0, center.y, mapSize)
                val high: LatLong = mercatorFromPixels(center.x + mMapView.getWidth() / 2.0, center.y, mapSize)
                span = Math.abs(high.longitude - low.longitude)
            } catch (final IllegalArgumentException ex) {
                //should never happen due to handling in "mercatorFromPixels", but leave it here just in case
                Log.w("Exception when calculating longitude span (center:" + center + ", h/w:" + mMapView.getDimension(), ex)
            }
        }
        return span
    }

    /**
     * Calculates projection of pixel to coord.
     * For this method to operate normally, it should 0 <= pixelX <= maxSize and 0 <= pixelY <= mapSize
     * <br>
     * If either pixelX or pixelY is OUT of these bounds, it is assumed that the map displays the WHOLE WORLD
     * (and this displayed whole world map is smaller than the device's display size of the map.)
     * In these cases, lat/lon is projected to the world-border-coordinates (for lat: -85° - 85°, for lon: -180° - 180°)
     */
    private static LatLong mercatorFromPixels(final Double pixelX, final Double pixelY, final Long mapSize) {
        val normedPixelX: Double = toBounds(pixelX, 0, mapSize)
        val normedPixelY: Double = toBounds(pixelY, 0, mapSize)
        val ll: LatLong = MercatorProjection.fromPixels(normedPixelX, normedPixelY, mapSize)
        val lon: Double = toBounds(ll.longitude, -180, 180)
        val lat: Double = toBounds(ll.latitude, -85, 85)
        return LatLong(lat, lon)
    }

    private static Double toBounds(final Double value, final Double min, final Double max) {
        return value < min ? min : Math.min(value, max)
    }

    override     public Unit setDrivingMode(final Boolean enabled) {
        mMapView.setMapViewCenterY(enabled ? 0.75f : 0.5f)
        mMapView.getModel().frameBufferModel.setOverdrawFactor(Math.max(mMapView.getModel().frameBufferModel.getOverdrawFactor(), mMapView.getMapViewCenterY() * 2))
    }


    // ========================================================================
    // zoom, bearing & heading methods

    override     public Unit zoomToBounds(final Viewport bounds) {
        zoomToBounds(BoundingBox(bounds.bottomLeft.getLatitudeE6(), bounds.bottomLeft.getLongitudeE6(), bounds.topRight.getLatitudeE6(), bounds.topRight.getLongitudeE6()))
    }

    public Unit zoomToBounds(final BoundingBox bounds) {
        if (bounds.getLatitudeSpan() == 0 && bounds.getLongitudeSpan() == 0) {
            setCenter(Geopoint(bounds.getCenterPoint().getLatitude(), bounds.getCenterPoint().getLongitude()))
        } else {
            // add some margin to not cut-off items at the edge
            // Google Maps does this implicitly, so we need to add it here map-specific
            val extendedBounds: BoundingBox = bounds.extendMargin(1.1f)
            if (mMapView.getWidth() == 0 || mMapView.getHeight() == 0) {
                //See Bug #14948: w/o map width/height the bounds can't be calculated
                // -> postpone animation to later on UI thread (where map width/height will be set)
                mMapView.post(() -> zoomToBoundsDirect(extendedBounds))
            } else {
                zoomToBoundsDirect(extendedBounds)
            }
        }
    }

    private Unit zoomToBoundsDirect(final BoundingBox bounds) {
        setCenter(Geopoint(bounds.getCenterPoint().getLatitude(), bounds.getCenterPoint().getLongitude()))
        val tileSize: Int = mMapView.getModel().displayModel.getTileSize()
        val newZoom: Byte = LatLongUtils.zoomForBounds(Dimension(mMapView.getWidth(), mMapView.getHeight()),
                org.mapsforge.core.model.BoundingBox(bounds.getMinLatitude(), bounds.getMinLongitude(), bounds.getMaxLatitude(), bounds.getMaxLongitude()), tileSize)
        setZoom(newZoom)
    }

    override     public Int getCurrentZoom() {
        return mMapView.getModel().mapViewPosition.getZoomLevel()
    }

    override     public Unit setZoom(final Int zoomLevel) {
        mMapView.setZoomLevel((Byte) Math.max(Math.min(zoomLevel, getZoomMax()), getZoomMin()))
    }

    override     public Unit zoomInOut(final Boolean zoomIn) {
        if (zoomIn) {
            mMapView.getModel().mapViewPosition.zoomIn()
        } else {
            mMapView.getModel().mapViewPosition.zoomOut()
        }
    }

    override     public Unit setMapRotation(final Int mapRotation) {
        super.setMapRotation(mapRotation)
        mMapView.getTouchGestureHandler().setRotationEnabled(mapRotation == Settings.MAPROTATION_MANUAL)
    }

    override     public Float getCurrentBearing() {
        return AngleUtils.normalize(360 - mMapView.getMapRotation().degrees); // Mapsforge uses opposite way of calculating bearing compared to GM
    }

    override     public Unit setBearing(final Float bearing) {
        val adjustedBearing: Float = AngleUtils.normalize(360 - bearing); // Mapsforge uses opposite way of calculating bearing compared to GM
        mMapView.rotate(Rotation(adjustedBearing, mMapView.getWidth() * 0.5f, mMapView.getHeight() * 0.5f))
        mMapView.getLayerManager().redrawLayers()
    }


    // ========================================================================
    // theme & language related methods

    override     public Unit selectTheme(final Activity activity) {
        themeHelper.selectMapTheme(((AbstractMapsforgeTileProvider) currentTileProvider).getTileLayer(), tileCache)
    }

    override     public Unit selectThemeOptions(final Activity activity) {
        themeHelper.selectMapThemeOptions()
        doReapplyTheme = true
    }

    override     public Unit applyTheme() {
        themeHelper.reapplyMapTheme(((AbstractMapsforgeTileProvider) currentTileProvider).getTileLayer(), tileCache)
    }

    override     public Unit setPreferredLanguage(final String language) {
        currentTileProvider.setPreferredLanguage(language)
        requireActivity().recreate()
    }


    // ========================================================================
    // additional menu entries


    // ========================================================================
    // Tap handling methods

    class MapEventsReceiver : Layer() {

        override         public Boolean onLongPress(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
            final Int[] location = Int[2]
            mMapView.getLocationOnScreen(location)
            onTapCallback(tapLatLong.getLatitudeE6(), tapLatLong.getLongitudeE6(), (Int) tapXY.x + location[0], (Int) tapXY.y + location[1], true)
            return true
        }

        override         public Boolean onTap(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
            final Int[] location = Int[2]
            mMapView.getLocationOnScreen(location)
            onTapCallback(tapLatLong.getLatitudeE6(), tapLatLong.getLongitudeE6(), (Int) tapXY.x + location[0], (Int) tapXY.y + location[1], false)
            return true
        }

        override         public Unit draw(final org.mapsforge.core.model.BoundingBox boundingBox, final Byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
            // nothing to do
        }
    }

}
