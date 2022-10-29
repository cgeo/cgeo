package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.AbstractGeoitemLayer;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.AbstractUnifiedMapView;
import cgeo.geocaching.unifiedmap.UnifiedMapActivity;
import cgeo.geocaching.unifiedmap.UnifiedMapPosition;
import cgeo.geocaching.unifiedmap.mapsforgevtm.legend.RenderThemeLegend;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;

import android.app.Activity;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import org.oscim.android.MapView;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.TileLayer;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.ImperialUnitAdapter;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.tiling.TileSource;

/**
 * MapsforgeVtmView - Contains the view handling parts specific to MapsforgeVtm
 * To be called by UnifiedMapActivity (mostly)
 */
public class MapsforgeVtmView extends AbstractUnifiedMapView<GeoPoint> {

    private MapView mMapView;
    private Map mMap;

    protected TileLayer baseMap;
    protected final ArrayList<Layer> layers = new ArrayList<>();
    protected Map.UpdateListener mapUpdateListener;
    protected MapsforgeThemeHelper themeHelper;

    @Override
    public void init(final UnifiedMapActivity activity, final int delayedZoomTo, final Geopoint delayedCenterTo, final Runnable onMapReadyTasks) {
        super.init(activity, delayedZoomTo, delayedCenterTo, onMapReadyTasks);
        activity.setContentView(R.layout.unifiedmap_mapsforgevtm);
        rootView = activity.findViewById(R.id.unifiedmap_vtm);
        mMapView = activity.findViewById(R.id.mapViewVTM);
        super.mMapView = mMapView;
        mMap = mMapView.map();
        setMapRotation(mapRotation);
        usesOwnBearingIndicator = false; // let UnifiedMap handle bearing indicator
        activity.findViewById(R.id.map_zoomin).setOnClickListener(v -> zoomInOut(true));
        activity.findViewById(R.id.map_zoomout).setOnClickListener(v -> zoomInOut(false));
        themeHelper = new MapsforgeThemeHelper(activity);
        onMapReadyTasks.run();
    }

    @Override
    public synchronized void prepareForTileSourceChange() {
        // remove layers from currently displayed Mapsforge map
        removeBaseMap();
        synchronized (layers) {
            for (Layer layer : layers) {
                layer.setEnabled(false);
                try {
                    mMap.layers().remove(layer);
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
    public void setMapRotation(final int mapRotation) {
        mMap.getEventLayer().enableRotation(mapRotation != Settings.MAPROTATION_OFF);
        super.setMapRotation(mapRotation);
    }

    @Override
    public float getCurrentBearing() {
        return mMap.getMapPosition().bearing;
    }

    @Override
    public void setBearing(final float bearing) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setBearing(bearing);
        mMap.setMapPosition(pos);
    }

    /**
     * keep track of rotation and zoom level changes
     **/
    protected void configMapChangeListener(final boolean enable) {
        if (mapUpdateListener != null) {
            mMap.events.unbind(mapUpdateListener);
            mapUpdateListener = null;
        }
        if (enable) {
            mapUpdateListener = (event, mapPosition) -> {
                if ((activityMapChangeListener != null) && (event == Map.POSITION_EVENT || event == Map.ROTATE_EVENT)) {
                    activityMapChangeListener.call(new UnifiedMapPosition(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.zoomLevel, mapPosition.bearing));
                }
            };
            mMap.events.bind(mapUpdateListener);
        }
    }

    /**
     * call this instead of VTM.setBaseMap so that we can keep track of baseMap set by tile provider
     */
    public synchronized TileLayer setBaseMap(final TileSource tileSource) {
        removeBaseMap();
        baseMap = mMap.setBaseMap(tileSource);
        return baseMap;
    }

    /**
     * call this instead of VTM.layers().add so that we can keep track of layers added by the tile provider
     */
    public synchronized void addLayer(final Layer layer) {
        layers.add(layer);
        mMap.layers().add(layer);
    }

    private void removeBaseMap() {
        if (baseMap != null) {
            try {
                mMap.layers().remove(1);
            } catch (IndexOutOfBoundsException ignore) {
                // ignored
            }
        }
        baseMap = null;
    }

    @Override
    protected AbstractGeoitemLayer createGeoitemLayers(final AbstractTileProvider tileProvider) {
        return new MapsforgeGeoitemLayer(tileProvider, mMap);
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
        renderer.setOffset(5 * CanvasAdapter.getScale(), 0);
        addLayer(mapScaleBarLayer);
    }

    @Override
    public void setTileSource(final AbstractTileProvider newSource) {
        super.setTileSource(newSource);
        ((AbstractMapsforgeTileProvider) currentTileProvider).addTileLayer(mMap);
        startMap();
    }

    @Override
    public void setPreferredLanguage(final String language) {
        currentTileProvider.setPreferredLanguage(language);
        mMap.clearMap();
    }

    @Override
    protected AbstractPositionLayer<GeoPoint> configPositionLayer(final boolean create) {
        if (create) {
            if (positionLayer == null) {
                positionLayer = new MapsforgePositionLayer(mMap, rootView);
            }
            return positionLayer;
        } else if (positionLayer != null) {
            ((MapsforgePositionLayer) positionLayer).destroyLayer(mMap);
            positionLayer = null;
        }
        return null;
    }

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
    public BoundingBox getBoundingBox() {
        return mMap.getBoundingBox(0);
    }

    // ========================================================================
    // theme related methods

    @Override
    public void selectTheme(final Activity activity) {
        themeHelper.selectMapTheme(activity, mMap, currentTileProvider);
    }

    @Override
    public void selectThemeOptions(final Activity activity) {
        themeHelper.selectMapThemeOptions(activity, currentTileProvider);
    }

    @Override
    public void applyTheme() {
        themeHelper.reapplyMapTheme(mMap, currentTileProvider);
    }

    // ========================================================================
    // zoom & heading methods

    @Override
    public void zoomToBounds(final Viewport bounds) {
        final MapPosition pos = new MapPosition();
        pos.setByBoundingBox(new BoundingBox(bounds.bottomLeft.getLatitudeE6(), bounds.bottomLeft.getLongitudeE6(), bounds.topRight.getLatitudeE6(), bounds.topRight.getLongitudeE6()), Tile.SIZE * 4, Tile.SIZE * 4);
        mMap.setMapPosition(pos);
    }

    public void zoomToBounds(final BoundingBox bounds) {
        final MapPosition pos = new MapPosition();
        pos.setByBoundingBox(bounds, Tile.SIZE * 4, Tile.SIZE * 4);
        mMap.setMapPosition(pos);
    }

    public int getCurrentZoom() {
        return mMap.getMapPosition().getZoomLevel();
    }

    public void setZoom(final int zoomLevel) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setZoomLevel(zoomLevel);
        mMap.setMapPosition(pos);
    }

    private void zoomInOut(final boolean zoomIn) {
        final int zoom = getCurrentZoom();
        setZoom(zoomIn ? zoom + 1 : zoom - 1);
    }

    // ========================================================================
    // map events receiver

    class MapEventsReceiver extends Layer implements GestureListener {

        MapEventsReceiver(final Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(final Gesture g, final MotionEvent e) {
            if (g instanceof Gesture.Tap) {
                final GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                onTapCallback(p.latitudeE6, p.longitudeE6, false);
                return true;
            } else if (g instanceof Gesture.LongPress) {
                final GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                onTapCallback(p.latitudeE6, p.longitudeE6, true);
                return true;
            }
            return false;
        }
    }

    // ========================================================================
    // additional menu entries

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.menu_theme_legend) {
            RenderThemeLegend.showLegend(activityRef.get(), themeHelper);
            return true;
        }
        return false;
    }

    // ========================================================================
    // Lifecycle methods

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme(); // @todo: There must be a less resource-intensive way of applying style-changes...
        mMapView.onResume();
        mMap.layers().add(new MapEventsReceiver(mMap));
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        themeHelper.disposeTheme();
        super.onDestroy();
    }
}
