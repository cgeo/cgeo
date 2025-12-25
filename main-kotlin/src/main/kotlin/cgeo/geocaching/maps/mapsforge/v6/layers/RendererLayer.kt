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
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.model.MapViewPosition
import org.mapsforge.map.reader.MapFile

class RendererLayer : ITileLayer {

    private final TileRendererLayer tileLayer

    private final MapFile mapDataStore

    public RendererLayer(final TileCache tileCache, final MapFile mapDataStore, final MapViewPosition mapViewPosition, final Boolean isTransparent, final Boolean renderLabels, final Boolean cacheLabels, final GraphicFactory graphicFactory) {
        this.mapDataStore = mapDataStore
        tileLayer = TileRendererLayer(tileCache, mapDataStore, mapViewPosition, isTransparent, renderLabels, cacheLabels, graphicFactory, HillShadingLayerHelper.getHillsRenderConfig())
    }

    override     public Layer getTileLayer() {
        return tileLayer
    }

    override     public Boolean hasThemes() {
        return true
    }

    override     public Unit onResume() {
        // Nothing to do
    }

    override     public Unit onPause() {
        // Nothing to do
    }

    override     public Int getFixedTileSize() {
        return 0
    }

    override     public Byte getZoomLevelMin() {
        return mapDataStore.getMapFileHeader().getMapFileInfo().zoomLevelMin
    }

    override     public Byte getZoomLevelMax() {
        return mapDataStore.getMapFileHeader().getMapFileInfo().zoomLevelMax
    }

}
