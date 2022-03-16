package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.AbstractUnifiedMap;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import org.oscim.android.MapView;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
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
import org.oscim.theme.IRenderTheme;
import org.oscim.tiling.TileSource;

public class MapsforgeVTM extends AbstractUnifiedMap {

    private IRenderTheme mTheme;
    private MapView mMapView;
    private Map mMap;

    protected AbstractMapsforgeTileProvider tileProvider;
    protected TileLayer baseMap;
    protected final ArrayList<Layer> layers = new ArrayList<>();

    @Override
    public void init(final AppCompatActivity activity) {
        activity.setContentView(R.layout.unifiedmap_mapsforgevtm);
        mMapView = activity.findViewById(R.id.mapViewVTM);
        mMap = mMapView.map();
        activity.findViewById(R.id.map_zoomin).setOnClickListener(v -> zoomInOut(true));
        activity.findViewById(R.id.map_zoomout).setOnClickListener(v -> zoomInOut(false));
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

    /** call this instead of VTM.setBaseMap so that we can keep track of baseMap set by tile provider */
    public synchronized TileLayer setBaseMap(final TileSource tileSource) {
        removeBaseMap();
        baseMap = mMap.setBaseMap(tileSource);
        return baseMap;
    }

    /** call this instead of VTM.setBaseMap so that we can keep track of layers added by the tile provider */
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
        tileProvider = (AbstractMapsforgeTileProvider) newSource;
        tileProvider.addTileLayer(mMap);
        startMap();
    }

    @Override
    public void setPreferredLanguage(final String language) {
        tileProvider.setPreferredLanguage(language);
        mMap.clearMap();
    }

    @Override
    public void applyTheme() {
        if (mTheme != null) {
            mTheme.dispose();
        }

        /*
        // zip based theme
        try {
            final List<String> xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new BufferedInputStream(ContentStorage.get().openForRead(PersistableFolder.OFFLINE_MAP_THEMES.getFolder(), "Elevate.zip"))));
            StringBuilder sb = new StringBuilder();
            for (String s : xmlThemes) {
                sb.append('"').append(s).append("\" ");
            }
            Log.e("found themes: " + sb.toString());
        } catch (IOException e) {
            Log.e(e.getMessage());
        }


        ThemeFile theme = null;
        try {
            theme = new ZipRenderTheme("Elevate.xml", new ZipXmlThemeResourceProvider(new ZipInputStream(new BufferedInputStream(ContentStorage.get().openForRead(PersistableFolder.OFFLINE_MAP_THEMES.getFolder(), "Elevate.zip")))));
        } catch (Exception ignore) {
            Log.e(ignore.getMessage());
        }
        mTheme = mMap.setTheme(theme);
        */

        mTheme = mMap.setTheme(VtmThemes.OSMARENDER);

    }

    @Override
    public void zoomToBounds(final BoundingBox bounds) {
        final MapPosition pos = new MapPosition();
        pos.setByBoundingBox(bounds, Tile.SIZE * 4, Tile.SIZE * 4);
        mMap.setMapPosition(pos);
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

    public int getCurrentZoom() {
        return mMap.getMapPosition().getZoomLevel();
    };

    public void setZoom(final int zoomLevel) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setZoomLevel(zoomLevel);
        mMap.setMapPosition(pos);
    };

    private void zoomInOut(final boolean zoomIn) {
        final int zoom = getCurrentZoom();
        setZoom(zoomIn ? zoom + 1 : zoom - 1);
    }

    @Override
    protected AbstractPositionLayer configPositionLayer(final boolean create) {
        if (create) {
            return positionLayer != null ? positionLayer : new MapsforgePositionLayer(mMap);
        } else if (positionLayer != null) {
            ((MapsforgePositionLayer) positionLayer).destroyLayer(mMap);
        }
        return null;
    }


    // ========================================================================
    // Lifecycle methods

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        mTheme.dispose();
        super.onDestroy();
    }
}
