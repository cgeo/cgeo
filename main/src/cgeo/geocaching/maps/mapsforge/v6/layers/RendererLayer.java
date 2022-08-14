package cgeo.geocaching.maps.mapsforge.v6.layers;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;

public class RendererLayer implements ITileLayer {

    private final TileRendererLayer tileLayer;

    private final MapFile mapDataStore;

    public RendererLayer(final TileCache tileCache, final MapFile mapDataStore, final IMapViewPosition mapViewPosition, final boolean isTransparent, final boolean renderLabels, final boolean cacheLabels, final GraphicFactory graphicFactory) {
        this.mapDataStore = mapDataStore;
        tileLayer = new TileRendererLayer(tileCache, mapDataStore, mapViewPosition, isTransparent, renderLabels, cacheLabels, graphicFactory, HillShadingLayerHelper.getHillsRenderConfig());
    }

    @Override
    public Layer getTileLayer() {
        return tileLayer;
    }

    @Override
    public boolean hasThemes() {
        return true;
    }

    @Override
    public void onResume() {
        // Nothing to do
    }

    @Override
    public void onPause() {
        // Nothing to do
    }

    @Override
    public int getFixedTileSize() {
        return 0;
    }

    @Override
    public byte getZoomLevelMin() {
        return mapDataStore.getMapFileHeader().getMapFileInfo().zoomLevelMin;
    }

    @Override
    public byte getZoomLevelMax() {
        return mapDataStore.getMapFileHeader().getMapFileInfo().zoomLevelMax;
    }

}
