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

import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.unifiedmap.LayerHelper
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeVtmFragment

import android.net.Uri

import androidx.core.util.Pair

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.Collections

import okhttp3.Cache
import okhttp3.OkHttpClient
import org.mapsforge.core.model.Tile
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.map.Map
import org.oscim.tiling.source.OkHttpEngine
import org.oscim.tiling.source.bitmap.BitmapTileSource

class AbstractMapsforgeVTMOnlineTileProvider : AbstractMapsforgeVTMTileProvider() {

    private String tilePath

    AbstractMapsforgeVTMOnlineTileProvider(final String name, final Uri uri, final String tilePath, final Int zoomMin, final Int zoomMax, final Pair<String, Boolean> mapAttribution) {
        super(name, uri, zoomMin, zoomMax, mapAttribution)
        this.tilePath = tilePath
        // tilePath: "/cyclosm/{Z}/{X}/{Y}.png"
        AbstractTileSource(String[]{uri.getHost()}, 443) {
            override             public Int getParallelRequestsLimit() {
                return 8
            }

            override             public URL getTileUrl(final Tile tile) throws MalformedURLException {
                // tilePath: "/cyclosm/{Z}/{X}/{Y}.png"
                val path: String = tilePath
                        .replace("{Z}", String.valueOf(tile.zoomLevel))
                        .replace("{X}", String.valueOf(tile.tileX))
                        .replace("{Y}", String.valueOf(tile.tileY))
                return URL("https", getHostName(), this.port, path)
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

    override     public Unit addTileLayer(final MapsforgeVtmFragment fragment, final Map map) {
        fragment.addLayer(LayerHelper.ZINDEX_BASEMAP, getBitmapTileLayer(map))
    }

    public BitmapTileLayer getBitmapTileLayer(final Map map) {
        final OkHttpClient.Builder httpBuilder = OkHttpClient.Builder()
        val cache: Cache = Cache(File(LocalStorage.getExternalPrivateCgeoDirectory(), "tiles"), 20 * 1024 * 1024)
        httpBuilder.cache(cache)
        val tileSource: BitmapTileSource = BitmapTileSource.builder()
                .url(mapUri.toString())
                .tilePath(tilePath)
                .zoomMax(zoomMax)
                .zoomMin(zoomMin)
                .build()
        tileSource.setHttpEngine(OkHttpEngine.OkHttpFactory(httpBuilder))
        tileSource.setHttpRequestHeaders(Collections.singletonMap("User-Agent", "cgeo-android"))
        return BitmapTileLayer(map, tileSource)
    }

}
