package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeVtmFragment;

import android.net.Uri;

import androidx.core.util.Pair;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

class AbstractMapsforgeVTMOnlineTileProvider extends AbstractMapsforgeVTMTileProvider {

    private String tilePath;

    AbstractMapsforgeVTMOnlineTileProvider(final String name, final Uri uri, final String tilePath, final int zoomMin, final int zoomMax, final Pair<String, Boolean> mapAttribution) {
        super(name, uri, zoomMin, zoomMax, mapAttribution);
        this.tilePath = tilePath;
        // tilePath: "/cyclosm/{Z}/{X}/{Y}.png"
        new AbstractTileSource(new String[]{uri.getHost()}, 443) {
            @Override
            public int getParallelRequestsLimit() {
                return 8;
            }

            @Override
            public URL getTileUrl(final Tile tile) throws MalformedURLException {
                // tilePath: "/cyclosm/{Z}/{X}/{Y}.png"
                final String path = tilePath
                        .replace("{Z}", String.valueOf(tile.zoomLevel))
                        .replace("{X}", String.valueOf(tile.tileX))
                        .replace("{Y}", String.valueOf(tile.tileY));
                return new URL("https", getHostName(), this.port, path);
            }

            @Override
            public byte getZoomLevelMax() {
                return (byte) zoomMax;
            }

            @Override
            public byte getZoomLevelMin() {
                return (byte) zoomMin;
            }

            @Override
            public boolean hasAlpha() {
                return false;
            }
        };
    }

    protected void setTilePath(final String tilePath) {
        this.tilePath = tilePath;
    }

    @Override
    public void addTileLayer(final MapsforgeVtmFragment fragment, final Map map) {
        fragment.addLayer(LayerHelper.ZINDEX_BASEMAP, getBitmapTileLayer(map));
    }

    public BitmapTileLayer getBitmapTileLayer(final Map map) {
        final OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        final Cache cache = new Cache(new File(LocalStorage.getExternalPrivateCgeoDirectory(), "tiles"), 20 * 1024 * 1024);
        httpBuilder.cache(cache);
        final BitmapTileSource tileSource = BitmapTileSource.builder()
                .url(mapUri.toString())
                .tilePath(tilePath)
                .zoomMax(zoomMax)
                .zoomMin(zoomMin)
                .build();
        tileSource.setHttpEngine(new OkHttpEngine.OkHttpFactory(httpBuilder));
        tileSource.setHttpRequestHeaders(Collections.singletonMap("User-Agent", "cgeo-android"));
        return new BitmapTileLayer(map, tileSource);
    }

}
