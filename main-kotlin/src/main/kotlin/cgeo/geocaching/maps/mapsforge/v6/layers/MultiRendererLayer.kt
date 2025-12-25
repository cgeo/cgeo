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

import java.util.List

import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.model.MapViewPosition
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.reader.header.MapFileInfo


class MultiRendererLayer : ITileLayer {
    private final TileRendererLayer tileLayer
    private var zoomLevelMin: Byte = 0
    private var zoomLevelMax: Byte = 0


    public MultiRendererLayer(final TileCache tileCache, final List<MapFile> mapFiles, final MapViewPosition mapViewPosition, final Boolean isTransparent, final Boolean renderLabels, final Boolean cacheLabels, final GraphicFactory graphicFactory) {
        val mapDataStore: MultiMapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL)

        for (MapFile f : mapFiles) {
            mapDataStore.addMapDataStore(f, false, false)
            val headerInfo: MapFileInfo = f.getMapFileHeader().getMapFileInfo()
            zoomLevelMin = (Byte) Math.max(headerInfo.zoomLevelMin, zoomLevelMin)
            // intentionally return the max zoomLevelMax level any of the maps support
            // to support combining maps with extremely different max zoom levels
            // like a country map combined with world map
            zoomLevelMax = (Byte) Math.max(headerInfo.zoomLevelMax, zoomLevelMax)
        }

        tileLayer = TileRendererLayer(tileCache, mapDataStore, mapViewPosition, isTransparent, renderLabels, cacheLabels, graphicFactory, HillShadingLayerHelper.getHillsRenderConfig())
    }

    override     public Layer getTileLayer() {
        return tileLayer
    }

    override     public Boolean hasThemes() {
        return true
    }

    override     public Unit onResume() {

    }

    override     public Unit onPause() {

    }

    override     public Int getFixedTileSize() {
        return 0
    }

    override     public Byte getZoomLevelMin() {
        return zoomLevelMin
    }

    override     public Byte getZoomLevelMax() {
        return zoomLevelMax
    }
}
