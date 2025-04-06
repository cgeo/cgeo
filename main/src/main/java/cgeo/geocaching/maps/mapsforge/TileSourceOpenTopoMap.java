package cgeo.geocaching.maps.mapsforge;

import java.net.MalformedURLException;
import java.net.URL;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;

public class TileSourceOpenTopoMap extends AbstractTileSource {
    /**
     * A tile source which fetches tiles from OpenTopoMap.org.
     * License is CC-BY-SA
     * <a href="https://opentopomap.org/about">OpenTopoMap</a>
     */
    public static final TileSourceOpenTopoMap INSTANCE = new TileSourceOpenTopoMap(new String[]{"a.tile.opentopomap.org", "b.tile.opentopomap.org", "c.tile.opentopomap.org"}, 443);
    private static final int PARALLEL_REQUESTS_LIMIT = 8;
    private static final String PROTOCOL = "https";
    private static final int ZOOM_LEVEL_MAX = 18;
    private static final int ZOOM_LEVEL_MIN = 0;

    public TileSourceOpenTopoMap(final String[] hostNames, final int port) {
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
        return new URL(PROTOCOL, getHostName(), this.port, "/" + tile.zoomLevel + '/' + tile.tileX + '/' + tile.tileY + ".png");
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
