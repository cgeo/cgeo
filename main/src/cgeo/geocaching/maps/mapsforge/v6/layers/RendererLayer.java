package cgeo.geocaching.maps.mapsforge.v6.layers;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;

public class RendererLayer implements ITileLayer {

    private final TileRendererLayer tileLayer;

    public RendererLayer(final TileCache tileCache, final MapDataStore mapDataStore, final MapViewPosition mapViewPosition, final boolean isTransparent, final boolean renderLabels, final boolean cacheLabels, final GraphicFactory graphicFactory) {
        tileLayer = new TileRendererLayer(tileCache, mapDataStore, mapViewPosition, isTransparent, renderLabels, cacheLabels, graphicFactory);
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

}
