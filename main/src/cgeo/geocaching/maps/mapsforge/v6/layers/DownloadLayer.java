package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.network.Network;


import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.model.MapViewPosition;

public class DownloadLayer implements ITileLayer {

    private final TileDownloadLayer tileLayer;

    public DownloadLayer(final TileCache tileCache, final MapViewPosition mapViewPosition, final AbstractTileSource tileSource, final GraphicFactory graphicFactory) {
        tileSource.setUserAgent(Network.getUserAgent());
        tileLayer = new TileDownloadLayer(tileCache, mapViewPosition, tileSource, graphicFactory);
    }

    @Override
    public Layer getTileLayer() {
        return tileLayer;
    }

    @Override
    public boolean hasThemes() {
        return false;
    }

    @Override
    public void onResume() {
        tileLayer.onResume();
    }

    @Override
    public void onPause() {
        tileLayer.onPause();
    }

}
