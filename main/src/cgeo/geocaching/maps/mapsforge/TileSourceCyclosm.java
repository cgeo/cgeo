package cgeo.geocaching.maps.mapsforge;

import java.net.MalformedURLException;
import java.net.URL;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;

public class TileSourceCyclosm extends AbstractTileSource {
    /**
     * A tile source which fetches cyclosm tiles from OpenStreetMap.fr.
     * Requires a valid HTTP User-Agent identifying application: https://operations.osmfoundation.org/policies/tiles/
     */
    public static final TileSourceCyclosm INSTANCE = new TileSourceCyclosm(new String[]{
            "a.tile-cyclosm.openstreetmap.fr", "b.tile-cyclosm.openstreetmap.fr", "c.tile-cyclosm.openstreetmap.fr"}, 443);
    private static final int PARALLEL_REQUESTS_LIMIT = 8;
    private static final String PROTOCOL = "https";
    private static final int ZOOM_LEVEL_MAX = 18;
    private static final int ZOOM_LEVEL_MIN = 0;

    public TileSourceCyclosm(final String[] hostNames, final int port) {
        super(hostNames, port);
        /* Default TTL: 8279 seconds (the TTL currently set by the OSM server). */
        defaultTimeToLive = 8279000;
    }

    @Override
    public int getParallelRequestsLimit() {
        return PARALLEL_REQUESTS_LIMIT;
    }

    @Override
    public URL getTileUrl(final Tile tile) throws MalformedURLException {
        return new URL(PROTOCOL, getHostName(), this.port, "/cyclosm/" + tile.zoomLevel + '/' + tile.tileX + '/' + tile.tileY + ".png");
    }

    @Override
    public byte getZoomLevelMax() {
        return ZOOM_LEVEL_MAX;
    }

    @Override
    public byte getZoomLevelMin() {
        return ZOOM_LEVEL_MIN;
    }

    @Override
    public boolean hasAlpha() {
        return false;
    }

}
