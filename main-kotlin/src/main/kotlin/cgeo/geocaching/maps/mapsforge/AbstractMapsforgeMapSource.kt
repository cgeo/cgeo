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

package cgeo.geocaching.maps.mapsforge

import cgeo.geocaching.R
import cgeo.geocaching.maps.AbstractMapSource
import cgeo.geocaching.maps.interfaces.MapProvider
import cgeo.geocaching.maps.mapsforge.v6.layers.DownloadLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer

import android.content.Context

import org.apache.commons.lang3.tuple.ImmutablePair
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource
import org.mapsforge.map.model.MapViewPosition

abstract class AbstractMapsforgeMapSource : AbstractMapSource() {

    public static val MAPNIK_TILE_DOWNLOAD_UA: String = "cgeo"

    private final AbstractTileSource source

    AbstractMapsforgeMapSource(final MapProvider mapProvider, final String name, final AbstractTileSource source) {
        super(mapProvider, name)
        this.source = source
    }

    AbstractMapsforgeMapSource(final MapProvider mapProvider, final String name) {
        this(mapProvider, name, null)
    }


    public ITileLayer createTileLayer(final TileCache tileCache, final MapViewPosition mapViewPosition) {
        source.setUserAgent(MAPNIK_TILE_DOWNLOAD_UA)
        return DownloadLayer(tileCache, mapViewPosition, source, AndroidGraphicFactory.INSTANCE)
    }

    override     public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
        return ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmap_html), false)
    }

}
