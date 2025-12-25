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

package cgeo.geocaching.maps.mapsforge.v6.layers

import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.TileSource
import org.mapsforge.map.model.MapViewPosition

class DownloadLayer : ITileLayer {

    private final TileDownloadLayer tileLayer

    private final TileSource tileSource

    public DownloadLayer(final TileCache tileCache, final MapViewPosition mapViewPosition, final TileSource tileSource, final GraphicFactory graphicFactory) {
        this.tileSource = tileSource
        tileLayer = TileDownloadLayer(tileCache, mapViewPosition, tileSource, graphicFactory)
    }

    override     public Layer getTileLayer() {
        return tileLayer
    }

    override     public Boolean hasThemes() {
        return false
    }

    override     public Unit onResume() {
        tileLayer.onResume()
    }

    override     public Unit onPause() {
        tileLayer.onPause()
    }

    override     public Int getFixedTileSize() {
        return 256
    }

    override     public Byte getZoomLevelMin() {
        return tileSource.getZoomLevelMin()
    }

    override     public Byte getZoomLevelMax() {
        return tileSource.getZoomLevelMax()
    }

}
