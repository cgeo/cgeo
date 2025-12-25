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

import java.net.MalformedURLException
import java.net.URL

import org.mapsforge.core.model.Tile
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource

class TileSourceCyclosm : AbstractTileSource() {
    /**
     * A tile source which fetches cyclosm tiles from OpenStreetMap.fr.
     * Requires a valid HTTP User-Agent identifying application: <a href="https://operations.osmfoundation.org/policies/tiles/">policies</a>
     */
    public static val INSTANCE: TileSourceCyclosm = TileSourceCyclosm(String[]{
            "a.tile-cyclosm.openstreetmap.fr", "b.tile-cyclosm.openstreetmap.fr", "c.tile-cyclosm.openstreetmap.fr"}, 443)
    private static val PARALLEL_REQUESTS_LIMIT: Int = 8
    private static val PROTOCOL: String = "https"
    private static val ZOOM_LEVEL_MAX: Int = 18
    private static val ZOOM_LEVEL_MIN: Int = 0

    public TileSourceCyclosm(final String[] hostNames, final Int port) {
        super(hostNames, port)
        /* Default TTL: 8279 seconds (the TTL currently set by the OSM server). */
        defaultTimeToLive = 8279000
    }

    override     public Int getParallelRequestsLimit() {
        return PARALLEL_REQUESTS_LIMIT
    }

    override     public URL getTileUrl(final Tile tile) throws MalformedURLException {
        return URL(PROTOCOL, getHostName(), this.port, "/cyclosm/" + tile.zoomLevel + '/' + tile.tileX + '/' + tile.tileY + ".png")
    }

    override     public Byte getZoomLevelMax() {
        return ZOOM_LEVEL_MAX
    }

    override     public Byte getZoomLevelMin() {
        return ZOOM_LEVEL_MIN
    }

    override     public Boolean hasAlpha() {
        return false
    }

}
