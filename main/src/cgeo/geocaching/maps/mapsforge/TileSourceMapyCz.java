package cgeo.geocaching.maps.mapsforge;

import java.net.MalformedURLException;
import java.net.URL;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;

public class TileSourceMapyCz extends AbstractTileSource {

    public static final TileSourceMapyCz INSTANCE =
            new TileSourceMapyCz(new String[]
                    {
                            "m1.mapserver.mapy.cz",
                            "m2.mapserver.mapy.cz",
                            "m3.mapserver.mapy.cz",
                            "m4.mapserver.mapy.cz"
                    }
                    , 443
            );

    private static final int PARALLEL_REQUESTS_LIMIT = 8;

    private static final String PROTOCOL = "https";

    private static final int ZOOM_LEVEL_MAX = 18;

    private static final int ZOOM_LEVEL_MIN = 5;

    public TileSourceMapyCz(final String[] hostNames, final int port) {
        super(hostNames, port);
    }

    @Override
    public int getParallelRequestsLimit() {
        return PARALLEL_REQUESTS_LIMIT;
    }

    @Override
    public URL getTileUrl(final Tile tile) throws MalformedURLException {

        return new URL(PROTOCOL, getHostName(), this.port, "/turist-m/" + tile.zoomLevel + '-' + tile.tileX + '-' + tile.tileY + ".png");
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
