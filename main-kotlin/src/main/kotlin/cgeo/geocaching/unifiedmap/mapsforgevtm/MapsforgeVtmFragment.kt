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

package cgeo.geocaching.unifiedmap.mapsforgevtm

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.unifiedmap.AbstractMapFragment
import cgeo.geocaching.unifiedmap.LayerHelper
import cgeo.geocaching.unifiedmap.UnifiedMapActivity
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer
import cgeo.geocaching.unifiedmap.geoitemlayer.MapsforgeVtmGeoItemLayer
import cgeo.geocaching.unifiedmap.layers.HillShadingLayerHelper
import cgeo.geocaching.unifiedmap.layers.MBTilesLayerHelper
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeVTMTileProvider
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.GroupedList

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

import java.util.ArrayList

import org.apache.commons.lang3.StringUtils
import org.oscim.android.MapView
import org.oscim.backend.CanvasAdapter
import org.oscim.core.BoundingBox
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
import org.oscim.event.Event
import org.oscim.event.Gesture
import org.oscim.event.GestureListener
import org.oscim.event.MotionEvent
import org.oscim.layers.Layer
import org.oscim.layers.tile.TileLayer
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.layers.tile.vector.OsmTileLayer
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.map.Map
import org.oscim.renderer.BitmapRenderer
import org.oscim.renderer.GLViewport
import org.oscim.scalebar.DefaultMapScaleBar
import org.oscim.scalebar.ImperialUnitAdapter
import org.oscim.scalebar.MapScaleBar
import org.oscim.scalebar.MapScaleBarLayer
import org.oscim.scalebar.MetricUnitAdapter
import org.oscim.tiling.TileSource
import org.oscim.utils.animation.Easing
import org.oscim.map.Animator.ANIM_ROTATE

class MapsforgeVtmFragment : AbstractMapFragment() {

    private MapView mMapView
    private Map mMap
    private GroupedList<Layer> mMapLayers
    protected TileLayer baseMap
    protected val layers: ArrayList<Layer> = ArrayList<>()
    protected MapsforgeThemeHelper themeHelper
    protected Map.UpdateListener mapUpdateListener
    private View mapAttribution
    private var doReapplyTheme: Boolean = false

    private var lastEvent: Event = null

    public MapsforgeVtmFragment() {
        super(R.layout.unifiedmap_mapsforgevtm_fragment)
    }

    override     public Unit onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState)

        mMapView = requireView().findViewById(R.id.mapViewVTM)
        mMap = mMapView.map()
        mMapLayers = GroupedList<>(mMap.layers(), 4)
        setMapRotation(Settings.getMapRotation())
        mapAttribution = requireView().findViewById(R.id.map_attribution)
        themeHelper = MapsforgeThemeHelper(requireActivity())
        if (position != null) {
            setCenter(position)
        }
        setZoom(zoomLevel)
        mapUpdateListener = (event, mapPosition) -> {
            if (event == Map.ROTATE_EVENT || event == Map.POSITION_EVENT) {
                repaintRotationIndicator(AngleUtils.normalize(360 - mapPosition.bearing))
            }
            if (event == Map.MOVE_EVENT || (lastEvent == Map.SCALE_EVENT && event == Map.POSITION_EVENT /* see #15590 */)) {
                // SCALE event is sent only on manually scaling the map, not when using zoom controls
                // (which send a POSITION event)
                // moving while zooming sends a SCALE event first, then one or more POSITION events,
                // while moving without zooming sends a MOVE event
                if (Boolean.TRUE == (viewModel.followMyLocation.getValue())) {
                    viewModel.followMyLocation.setValue(false)
                }
            }
            if (event == Map.SCALE_EVENT || event == Map.POSITION_EVENT) {
                ((UnifiedMapActivity) requireActivity()).notifyZoomLevel(mMap.getMapPosition().zoomLevel)
            }
            lastEvent = event; // remember to detect scaling combined with panning
        }
        mMap.events.bind(mapUpdateListener)

        if (onMapReadyTasks != null) {
            onMapReadyTasks.run()
        }
    }

    private Unit startMap() {
        val mapScaleBar: DefaultMapScaleBar = DefaultMapScaleBar(mMap)
        mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH)
        mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE)
        mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE)
        mapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT)

        val mapScaleBarLayer: MapScaleBarLayer = MapScaleBarLayer(mMap, mapScaleBar)
        val renderer: BitmapRenderer = mapScaleBarLayer.getRenderer()
        renderer.setPosition(GLViewport.Position.BOTTOM_LEFT)
        renderer.setOffset(30 * CanvasAdapter.getScale(), 0); // make room for attribution
        addLayer(LayerHelper.ZINDEX_SCALEBAR, mapScaleBarLayer)

        if (Settings.getMapBackgroundMapLayer() && currentTileProvider.supportsBackgroundMaps()) {
            for (BitmapTileLayer backgroundMap : MBTilesLayerHelper.getBitmapTileLayersVTM(requireActivity(), mMap)) {
                addLayer(LayerHelper.ZINDEX_BASEMAP, backgroundMap)
            }
        }
        if (Settings.getMapShadingShowLayer()) {
            addLayer(2, HillShadingLayerHelper.getBitmapTileLayer(getContext(), mMap))
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
        mMapLayers.add(MapsforgeVtmFragment.MapEventsReceiver(mMap))
    }

    override     public Unit onResume() {
        super.onResume()
        mMapView.onResume()

    }

    override     public Unit onPause() {
        mMapView.onPause()
        super.onPause()
    }

    override     public Unit onDestroyView() {
        mMapView.onDestroy()
        themeHelper.disposeTheme()
        super.onDestroyView()
    }


    // ========================================================================
    // tilesource handling

    override     public Boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource is AbstractMapsforgeVTMTileProvider
    }

    override     public Unit prepareForTileSourceChange() {
        // remove layers from currently displayed Mapsforge map
        removeBaseMap()
        synchronized (layers) {
            for (Layer layer : layers) {
                layer.setEnabled(false)
                try {
                    mMapLayers.remove(layer)
                } catch (IndexOutOfBoundsException ignore) {
                    // ignored
                }
            }
            layers.clear()
        }
        mMap.clearMap()
        super.prepareForTileSourceChange()
    }

    override     public Boolean setTileSource(final AbstractTileProvider newSource, final Boolean force) {
        val needsUpdate: Boolean = super.setTileSource(newSource, force)
        if (needsUpdate) {
            ((AbstractMapsforgeVTMTileProvider) currentTileProvider).addTileLayer(this, mMap)
            startMap()
        }
        return needsUpdate
    }


    // ========================================================================
    // layer handling

    override     public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return MapsforgeVtmGeoItemLayer(mMap, mMapLayers)
    }

    /**
     * call this instead of VTM.setBaseMap so that we can keep track of baseMap set by tile provider
     */
    public synchronized TileLayer setBaseMap(final TileSource tileSource) {
        removeBaseMap()

        val l: VectorTileLayer = OsmTileLayer(mMap)
        l.setTileSource(tileSource)
        addLayerToGroup(l, LayerHelper.ZINDEX_BASEMAP)
        baseMap = l
        return baseMap
    }

    private Unit removeBaseMap() {
        if (baseMap != null) {
            mMapLayers.removeGroup(LayerHelper.ZINDEX_BASEMAP)
        }
        baseMap = null
    }

    /**
     * call this instead of VTM.layers().add so that we can keep track of layers added by the tile provider
     */
    public synchronized Unit addLayer(final Int index, final Layer layer) {
        layers.add(layer)
        mMapLayers.addToGroup(layer, index)
    }

    public synchronized Unit addLayerToGroup(final Layer layer, final Int groupIndex) {
        mMapLayers.addToGroup(layer, groupIndex)
    }


    // ========================================================================
    // position related methods

    override     public Unit setCenter(final Geopoint geopoint) {
        val pos: MapPosition = mMap.getMapPosition()
        pos.setPosition(geopoint.getLatitude(), geopoint.getLongitude())
        mMap.setMapPosition(pos)
    }

    override     public Geopoint getCenter() {
        val pos: MapPosition = mMap.getMapPosition()
        return Geopoint(pos.getLatitude(), pos.getLongitude())
    }

    override     public Viewport getViewport() {
        val bb: BoundingBox = mMap == null ? null : mMap.getBoundingBox(0)
        if (bb == null) {
            return null
        }
        return Viewport.forE6(bb.minLatitudeE6, bb.minLongitudeE6, bb.maxLatitudeE6, bb.maxLongitudeE6)
    }

    override     public Unit setDrivingMode(final Boolean enabled) {
        mMap.viewport().setMapViewCenter(0f, enabled ? 0.5f : 0f)
    }


    // ========================================================================
    // zoom, bearing & heading methods

    override     public Unit zoomToBounds(final Viewport bounds) {
        zoomToBounds(BoundingBox(bounds.bottomLeft.getLatitudeE6(), bounds.bottomLeft.getLongitudeE6(), bounds.topRight.getLatitudeE6(), bounds.topRight.getLongitudeE6()))
    }

    public Unit zoomToBounds(final BoundingBox bounds) {
        if (bounds.getLatitudeSpan() == 0 && bounds.getLongitudeSpan() == 0) {
            mMap.animator().animateTo(GeoPoint(bounds.getMaxLatitude(), bounds.getMaxLongitude()))
        } else {
            // add some margin to not cut-off items at the edge
            // Google Maps does this implicitly, so we need to add it here map-specific
            val extendedBounds: BoundingBox = bounds.extendMargin(1.1f)
            if (mMap.getWidth() == 0 || mMap.getHeight() == 0) {
                //See Bug #14948: w/o map width/height the bounds can't be calculated
                // -> postpone animation to later on UI thread (where map width/height will be set)
                mMap.post(() -> zoomToBoundsDirect(extendedBounds))
            } else {
                zoomToBoundsDirect(extendedBounds)
            }
        }
    }

    private Unit zoomToBoundsDirect(final BoundingBox bounds) {
        val mp: MapPosition = mMap.getMapPosition()
        mp.setByBoundingBox(bounds, mMapView.getWidth(), mMapView.getHeight())
        mMap.setMapPosition(mp)
    }

    override     public Int getCurrentZoom() {
        return mMap.getMapPosition().getZoomLevel()
    }

    override     public Unit setZoom(final Int zoomLevel) {
        val pos: MapPosition = mMap.getMapPosition()
        pos.setZoomLevel(zoomLevel)
        mMap.setMapPosition(pos)
    }

    public Unit zoomInOut(final Boolean zoomIn) {
        mMap.animator().animateZoom(300, zoomIn ? 2 : 0.5, 0f, 0f)
    }

    override     public Unit setMapRotation(final Int mapRotation) {
        super.setMapRotation(mapRotation)
        mMap.getEventLayer().enableRotation(mapRotation == Settings.MAPROTATION_MANUAL)
    }

    override     public Float getCurrentBearing() {
        return AngleUtils.normalize(360 - mMap.getMapPosition().bearing); // VTM uses opposite way of calculating bearing compared to GM
    }

    override     public Unit setBearing(final Float bearing) {
        val adjustedBearing: Float = AngleUtils.normalize(360 - bearing); // VTM uses opposite way of calculating bearing compared to GM
        val pos: MapPosition = mMap.getMapPosition()
        pos.setBearing(adjustedBearing)
        val bearingDelta: Float = Math.abs(AngleUtils.difference(360 - pos.bearing, bearing))
        if (bearingDelta > 10.f) {
            mMap.animator().animateTo(5000, pos, Easing.Type.QUAD_INOUT, ANIM_ROTATE)
        } else {
            mMap.setMapPosition(pos)
        }
    }


    // ========================================================================
    // theme & language related methods

    override     public Unit selectTheme(final Activity activity) {
        themeHelper.selectMapTheme(activity, mMap, currentTileProvider)
    }

    override     public Unit selectThemeOptions(final Activity activity) {
        themeHelper.selectMapThemeOptions(activity, currentTileProvider)
        doReapplyTheme = true
    }

    override     public Unit applyTheme() {
        themeHelper.reapplyMapTheme(mMap, currentTileProvider)
    }

    override     public Unit setPreferredLanguage(final String language) {
        currentTileProvider.setPreferredLanguage(language)
        mMap.clearMap()
    }

    // ========================================================================
    // additional menu entries


    // ========================================================================
    // Tap handling methods

    class MapEventsReceiver : Layer() : GestureListener {

        MapEventsReceiver(final Map map) {
            super(map)
        }

        override         public Boolean onGesture(final Gesture g, final MotionEvent e) {
            if (g is Gesture.Tap) {
                val p: GeoPoint = mMap.viewport().fromScreenPoint(e.getX(), e.getY())
                final Int[] location = Int[2]
                mMapView.getLocationOnScreen(location)
                onTapCallback(p.latitudeE6, p.longitudeE6, (Int) e.getX() + location[0], (Int) e.getY() + location[1], false)
                return true
            } else if (g is Gesture.LongPress) {
                val p: GeoPoint = mMap.viewport().fromScreenPoint(e.getX(), e.getY())
                final Int[] location = Int[2]
                mMapView.getLocationOnScreen(location)
                onTapCallback(p.latitudeE6, p.longitudeE6, (Int) e.getX() + location[0], (Int) e.getY() + location[1], true)
                return true
            }
            return false
        }
    }

}
