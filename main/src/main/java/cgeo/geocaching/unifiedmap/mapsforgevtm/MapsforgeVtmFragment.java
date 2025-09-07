package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapActivity;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.MapsforgeVtmGeoItemLayer;
import cgeo.geocaching.unifiedmap.layers.HillShadingLayerHelper;
import cgeo.geocaching.unifiedmap.layers.MBTilesLayerHelper;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeVTMTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.GroupedList;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.oscim.android.MapView;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.ImperialUnitAdapter;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.tiling.TileSource;
import org.oscim.utils.animation.Easing;
import static org.oscim.map.Animator.ANIM_ROTATE;

public class MapsforgeVtmFragment extends AbstractMapFragment {

    private MapView mMapView;
    private Map mMap;
    private GroupedList<Layer> mMapLayers;
    protected TileLayer baseMap;
    protected final ArrayList<Layer> layers = new ArrayList<>();
    protected MapsforgeThemeHelper themeHelper;
    protected Map.UpdateListener mapUpdateListener;
    private View mapAttribution;
    private boolean doReapplyTheme = false;

    private Event lastEvent = null;

    public MapsforgeVtmFragment() {
        super(R.layout.unifiedmap_mapsforgevtm_fragment);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMapView = requireView().findViewById(R.id.mapViewVTM);
        mMap = mMapView.map();
        mMapLayers = new GroupedList<>(mMap.layers(), 4);
        setMapRotation(Settings.getMapRotation());
        mapAttribution = requireView().findViewById(R.id.map_attribution);
        themeHelper = new MapsforgeThemeHelper(requireActivity());
        if (position != null) {
            setCenter(position);
        }
        setZoom(zoomLevel);
        mapUpdateListener = (event, mapPosition) -> {
            if (event == Map.ROTATE_EVENT || event == Map.POSITION_EVENT) {
                repaintRotationIndicator(AngleUtils.normalize(360 - mapPosition.bearing));
            }
            if (event == Map.MOVE_EVENT || (lastEvent == Map.SCALE_EVENT && event == Map.POSITION_EVENT /* see #15590 */)) {
                // SCALE event is sent only on manually scaling the map, not when using zoom controls
                // (which send a POSITION event)
                // moving while zooming sends a SCALE event first, then one or more POSITION events,
                // while moving without zooming sends a MOVE event
                if (Boolean.TRUE.equals(viewModel.followMyLocation.getValue())) {
                    viewModel.followMyLocation.setValue(false);
                }
            }
            if (event == Map.SCALE_EVENT || event == Map.POSITION_EVENT) {
                ((UnifiedMapActivity) requireActivity()).notifyZoomLevel(mMap.getMapPosition().zoomLevel);
            }
            lastEvent = event; // remember to detect scaling combined with panning
        };
        mMap.events.bind(mapUpdateListener);

        if (onMapReadyTasks != null) {
            onMapReadyTasks.run();
        }
    }

    private void startMap() {
        final DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mMap);
        mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH);
        mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
        mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);
        mapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT);

        final MapScaleBarLayer mapScaleBarLayer = new MapScaleBarLayer(mMap, mapScaleBar);
        final BitmapRenderer renderer = mapScaleBarLayer.getRenderer();
        renderer.setPosition(GLViewport.Position.BOTTOM_LEFT);
        renderer.setOffset(30 * CanvasAdapter.getScale(), 0); // make room for attribution
        addLayer(LayerHelper.ZINDEX_SCALEBAR, mapScaleBarLayer);

        if (Settings.getMapBackgroundMapLayer() && currentTileProvider.supportsBackgroundMaps()) {
            for (BitmapTileLayer backgroundMap : MBTilesLayerHelper.getBitmapTileLayersVTM(requireActivity(), mMap)) {
                addLayer(LayerHelper.ZINDEX_BASEMAP, backgroundMap);
            }
        }
        if (Settings.getMapShadingShowLayer()) {
            addLayer(2, HillShadingLayerHelper.getBitmapTileLayer(getContext(), mMap));
        }

        if (this.mapAttribution != null) {
            this.mapAttribution.setOnClickListener(v -> displayMapAttribution());
        }
    }

    private void displayMapAttribution() {
        final Pair<String, Boolean> mapAttribution = currentTileProvider.getMapAttribution();
        if (mapAttribution == null || StringUtils.isBlank(mapAttribution.first)) {
            return;
        }

        //create text message
        CharSequence message = HtmlCompat.fromHtml(mapAttribution.first, HtmlCompat.FROM_HTML_MODE_LEGACY);
        if (mapAttribution.second) {
            final SpannableString s = new SpannableString(message);
            ViewUtils.safeAddLinks(s, Linkify.ALL);
            message = s;
        }

        final AlertDialog alertDialog = Dialogs.newBuilder(getContext())
                .setTitle(requireContext().getString(R.string.map_source_attribution_dialog_title))
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, pos) -> dialog.dismiss())
                .create();
        alertDialog.show();

        // Make the URLs in TextView clickable. Must be called after show()
        // Note: we do NOT use the "setView()" option of AlertDialog because this screws up the layout
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }


    // ========================================================================
    // lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        initLayers();
        if (doReapplyTheme) {
            applyTheme(); // @todo: There must be a less resource-intensive way of applying style-changes...
            doReapplyTheme = false;
        }
        mMapLayers.add(new MapsforgeVtmFragment.MapEventsReceiver(mMap));
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();

    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mMapView.onDestroy();
        themeHelper.disposeTheme();
        super.onDestroyView();
    }


    // ========================================================================
    // tilesource handling

    @Override
    public boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource instanceof AbstractMapsforgeVTMTileProvider;
    }

    @Override
    public void prepareForTileSourceChange() {
        // remove layers from currently displayed Mapsforge map
        removeBaseMap();
        synchronized (layers) {
            for (Layer layer : layers) {
                layer.setEnabled(false);
                try {
                    mMapLayers.remove(layer);
                } catch (IndexOutOfBoundsException ignore) {
                    // ignored
                }
            }
            layers.clear();
        }
        mMap.clearMap();
        super.prepareForTileSourceChange();
    }

    @Override
    public boolean setTileSource(final AbstractTileProvider newSource, final boolean force) {
        final boolean needsUpdate = super.setTileSource(newSource, force);
        if (needsUpdate) {
            ((AbstractMapsforgeVTMTileProvider) currentTileProvider).addTileLayer(this, mMap);
            startMap();
        }
        return needsUpdate;
    }


    // ========================================================================
    // layer handling

    @Override
    public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return new MapsforgeVtmGeoItemLayer(mMap, mMapLayers);
    }

    /**
     * call this instead of VTM.setBaseMap so that we can keep track of baseMap set by tile provider
     */
    public synchronized TileLayer setBaseMap(final TileSource tileSource) {
        removeBaseMap();

        final VectorTileLayer l = new OsmTileLayer(mMap);
        l.setTileSource(tileSource);
        addLayerToGroup(l, LayerHelper.ZINDEX_BASEMAP);
        baseMap = l;
        return baseMap;
    }

    private void removeBaseMap() {
        if (baseMap != null) {
            mMapLayers.removeGroup(LayerHelper.ZINDEX_BASEMAP);
        }
        baseMap = null;
    }

    /**
     * call this instead of VTM.layers().add so that we can keep track of layers added by the tile provider
     */
    public synchronized void addLayer(final int index, final Layer layer) {
        layers.add(layer);
        mMapLayers.addToGroup(layer, index);
    }

    public synchronized void addLayerToGroup(final Layer layer, final int groupIndex) {
        mMapLayers.addToGroup(layer, groupIndex);
    }


    // ========================================================================
    // position related methods

    @Override
    public void setCenter(final Geopoint geopoint) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setPosition(geopoint.getLatitude(), geopoint.getLongitude());
        mMap.setMapPosition(pos);
    }

    @Override
    public Geopoint getCenter() {
        final MapPosition pos = mMap.getMapPosition();
        return new Geopoint(pos.getLatitude(), pos.getLongitude());
    }

    @Override
    @Nullable
    public Viewport getViewport() {
        final BoundingBox bb = mMap == null ? null : mMap.getBoundingBox(0);
        if (bb == null) {
            return null;
        }
        return Viewport.forE6(bb.minLatitudeE6, bb.minLongitudeE6, bb.maxLatitudeE6, bb.maxLongitudeE6);
    }

    @Override
    public void setDrivingMode(final boolean enabled) {
        mMap.viewport().setMapViewCenter(0f, enabled ? 0.5f : 0f);
    }


    // ========================================================================
    // zoom, bearing & heading methods

    @Override
    public void zoomToBounds(final Viewport bounds) {
        zoomToBounds(new BoundingBox(bounds.bottomLeft.getLatitudeE6(), bounds.bottomLeft.getLongitudeE6(), bounds.topRight.getLatitudeE6(), bounds.topRight.getLongitudeE6()));
    }

    public void zoomToBounds(final BoundingBox bounds) {
        if (bounds.getLatitudeSpan() == 0 && bounds.getLongitudeSpan() == 0) {
            mMap.animator().animateTo(new GeoPoint(bounds.getMaxLatitude(), bounds.getMaxLongitude()));
        } else {
            // add some margin to not cut-off items at the edge
            // Google Maps does this implicitly, so we need to add it here map-specific
            final BoundingBox extendedBounds = bounds.extendMargin(1.1f);
            if (mMap.getWidth() == 0 || mMap.getHeight() == 0) {
                //See Bug #14948: w/o map width/height the bounds can't be calculated
                // -> postpone animation to later on UI thread (where map width/height will be set)
                mMap.post(() -> zoomToBoundsDirect(extendedBounds));
            } else {
                zoomToBoundsDirect(extendedBounds);
            }
        }
    }

    private void zoomToBoundsDirect(final BoundingBox bounds) {
        final MapPosition mp = mMap.getMapPosition();
        mp.setByBoundingBox(bounds, mMapView.getWidth(), mMapView.getHeight());
        mMap.setMapPosition(mp);
    }

    @Override
    public int getCurrentZoom() {
        return mMap.getMapPosition().getZoomLevel();
    }

    @Override
    public void setZoom(final int zoomLevel) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setZoomLevel(zoomLevel);
        mMap.setMapPosition(pos);
    }

    public void zoomInOut(final boolean zoomIn) {
        mMap.animator().animateZoom(300, zoomIn ? 2 : 0.5, 0f, 0f);
    }

    @Override
    public void setMapRotation(final int mapRotation) {
        super.setMapRotation(mapRotation);
        mMap.getEventLayer().enableRotation(mapRotation == Settings.MAPROTATION_MANUAL);
    }

    @Override
    public float getCurrentBearing() {
        return AngleUtils.normalize(360 - mMap.getMapPosition().bearing); // VTM uses opposite way of calculating bearing compared to GM
    }

    @Override
    public void setBearing(final float bearing) {
        final float adjustedBearing = AngleUtils.normalize(360 - bearing); // VTM uses opposite way of calculating bearing compared to GM
        final MapPosition pos = mMap.getMapPosition();
        pos.setBearing(adjustedBearing);
        final float bearingDelta = Math.abs(AngleUtils.difference(360 - pos.bearing, bearing));
        if (bearingDelta > 10.f) {
            mMap.animator().animateTo(5000, pos, Easing.Type.QUAD_INOUT, ANIM_ROTATE);
        } else {
            mMap.setMapPosition(pos);
        }
    }


    // ========================================================================
    // theme & language related methods

    @Override
    public void selectTheme(final Activity activity) {
        themeHelper.selectMapTheme(activity, mMap, currentTileProvider);
    }

    @Override
    public void selectThemeOptions(final Activity activity) {
        themeHelper.selectMapThemeOptions(activity, currentTileProvider);
        doReapplyTheme = true;
    }

    @Override
    public void applyTheme() {
        themeHelper.reapplyMapTheme(mMap, currentTileProvider);
    }

    @Override
    public void setPreferredLanguage(final String language) {
        currentTileProvider.setPreferredLanguage(language);
        mMap.clearMap();
    }

    // ========================================================================
    // additional menu entries


    // ========================================================================
    // Tap handling methods

    class MapEventsReceiver extends Layer implements GestureListener {

        MapEventsReceiver(final Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(final Gesture g, final MotionEvent e) {
            if (g instanceof Gesture.Tap) {
                final GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                onTapCallback(p.latitudeE6, p.longitudeE6, (int) e.getX(), (int) e.getY(), false);
                return true;
            } else if (g instanceof Gesture.LongPress) {
                final GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                onTapCallback(p.latitudeE6, p.longitudeE6, (int) e.getX(), (int) e.getY(), true);
                return true;
            }
            return false;
        }
    }

}
