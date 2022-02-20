package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.storage.LocalStorage;

import android.net.Uri;

import java.io.File;
import java.util.Collections;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

class AbstractMapsforgeOnlineTileProvider extends AbstractMapsforgeTileProvider {

    private final String tilePath;
    private final int zoomMin;
    private final int zoomMax;

    AbstractMapsforgeOnlineTileProvider(final String name, final Uri uri, final String tilePath, final int zoomMin, final int zoomMax) {
        super(name, uri);
        this.tilePath = tilePath;
        this.zoomMin = zoomMin;
        this.zoomMax = zoomMax;
    }

    @Override
    public void addTileLayer(final Map map) {
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
//        tileSource.setHttpRequestHeaders(Collections.singletonMap("User-Agent", "vtm-android-example"));
        tileLayer = new BitmapTileLayer(map, tileSource);
        map.layers().add(tileLayer);
    }

}
