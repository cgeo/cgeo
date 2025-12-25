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

package cgeo.geocaching.unifiedmap.tileproviders

import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment
import cgeo.geocaching.maps.mapsforge.AbstractMapsforgeMapSource.MAPNIK_TILE_DOWNLOAD_UA

import android.net.Uri

import androidx.core.util.Pair

import java.net.MalformedURLException
import java.net.URL

import org.mapsforge.core.model.Tile
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource
import org.mapsforge.map.view.MapView

class AbstractMapsforgeOnlineTileProvider : AbstractMapsforgeTileProvider() {

    private final AbstractTileSource mfTileSource
    private String tilePath

    AbstractMapsforgeOnlineTileProvider(final String name, final Uri uri, final String tilePath, final Int zoomMin, final Int zoomMax, final Pair<String, Boolean> mapAttribution) {
        super(name, uri, zoomMin, zoomMax, mapAttribution)
        this.tilePath = tilePath
        mfTileSource = AbstractTileSource(String[]{ uri.getHost() }, 443) {
            override             public Int getParallelRequestsLimit() {
                return 8
            }

            override             public URL getTileUrl(final Tile tile) throws MalformedURLException {
                // tilePath: "/cyclosm/{Z}/{X}/{Y}.png"
                val path: String = AbstractMapsforgeOnlineTileProvider.this.tilePath
                        .replace("{Z}", String.valueOf(tile.zoomLevel))
                        .replace("{X}", String.valueOf(tile.tileX))
                        .replace("{Y}", String.valueOf(tile.tileY))
                return URL(AbstractMapsforgeOnlineTileProvider.this.mapUri + path)
            }

            override             public Byte getZoomLevelMax() {
                return (Byte) zoomMax
            }

            override             public Byte getZoomLevelMin() {
                return (Byte) zoomMin
            }

            override             public Boolean hasAlpha() {
                return false
            }
        }
    }

    protected Unit setTilePath(final String tilePath) {
        this.tilePath = tilePath
    }

    override     public Unit addTileLayer(final MapsforgeFragment fragment, final MapView map) {
        mfTileSource.setUserAgent(MAPNIK_TILE_DOWNLOAD_UA); // @todo
        tileLayer = TileDownloadLayer(fragment.getTileCache(), map.getModel().mapViewPosition, mfTileSource, AndroidGraphicFactory.INSTANCE)
        map.getLayerManager().getLayers().add(tileLayer)
        onResume(); // start tile downloader
    }

    // ========================================================================
    // Lifecycle methods

    override     public Unit onPause() {
        if (tileLayer != null) {
            ((TileDownloadLayer) tileLayer).onPause()
        }
        super.onPause()
    }

    override     public Unit onResume() {
        super.onResume()
        if (tileLayer != null) {
            ((TileDownloadLayer) tileLayer).onResume()
        }
    }

    override     public Unit onDestroy() {
        if (tileLayer != null) {
            tileLayer.onDestroy()
        }
        super.onDestroy()
    }
}
