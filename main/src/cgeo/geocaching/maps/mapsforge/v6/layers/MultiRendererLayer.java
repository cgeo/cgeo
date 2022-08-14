package cgeo.geocaching.maps.mapsforge.v6.layers;

import java.util.List;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileInfo;


public class MultiRendererLayer implements ITileLayer {
    private final TileRendererLayer tileLayer;
    private byte zoomLevelMin = 0;
    private byte zoomLevelMax = 0;


    public MultiRendererLayer(final TileCache tileCache, final List<MapFile> mapFiles, final IMapViewPosition mapViewPosition, final boolean isTransparent, final boolean renderLabels, final boolean cacheLabels, final GraphicFactory graphicFactory) {
        final MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);

        for (MapFile f : mapFiles) {
            mapDataStore.addMapDataStore(f, false, false);
            final MapFileInfo headerInfo = f.getMapFileHeader().getMapFileInfo();
            zoomLevelMin = (byte) Math.max(headerInfo.zoomLevelMin, zoomLevelMin);
            // intentionally return the max zoomLevelMax level any of the maps support
            // to support combining maps with extremely different max zoom levels
            // like a country map combined with world map
            zoomLevelMax = (byte) Math.max(headerInfo.zoomLevelMax, zoomLevelMax);
        }

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

    }

    @Override
    public void onPause() {

    }

    @Override
    public int getFixedTileSize() {
        return 0;
    }

    @Override
    public byte getZoomLevelMin() {
        return zoomLevelMin;
    }

    @Override
    public byte getZoomLevelMax() {
        return zoomLevelMax;
    }
}
