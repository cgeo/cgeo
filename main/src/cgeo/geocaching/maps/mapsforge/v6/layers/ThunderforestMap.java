package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import java.net.MalformedURLException;
import java.net.URL;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;

/**
 * Like {@link org.mapsforge.map.layer.download.tilesource.OpenCycleMap} but loading tiles from thunderforest servers with apikey.
 */
public class ThunderforestMap extends AbstractTileSource {

    public static final ThunderforestMap INSTANCE = new ThunderforestMap(new String[]{"a.tile.thunderforest.com", "b.tile.thunderforest.com", "c.tile.thunderforest.com"}, 443);
    private final String apiKey;

    public ThunderforestMap(final String[] hostNames, final int port) {
        super(hostNames, port);
        apiKey = CgeoApplication.getInstance().getString(R.string.thunderforest_api_key);
    }

    public int getParallelRequestsLimit() {
        return 8;
    }

    public URL getTileUrl(final Tile tile) throws MalformedURLException {
        return new URL("https", this.getHostName(), this.port, "/cycle/" + tile.zoomLevel + '/' + tile.tileX + '/' + tile.tileY + ".png?apikey=" + apiKey);
    }

    public byte getZoomLevelMax() {
        return 22;
    }

    public byte getZoomLevelMin() {
        return 0;
    }

    public boolean hasAlpha() {
        return false;
    }
}
