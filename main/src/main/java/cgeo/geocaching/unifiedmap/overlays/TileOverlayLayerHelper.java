package cgeo.geocaching.unifiedmap.overlays;

import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.unifiedmap.tiles.TileUrlUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.UrlTileProvider;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.view.MapView;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

/**
 * Builds the renderer specific layers for the user-defined tile overlays.
 *
 * <p>Overlays are drawn on top of the current map source and are expected to deliver tiles that are
 * transparent where they carry no data, so no additional alpha blending is applied.</p>
 */
public final class TileOverlayLayerHelper {

    /** matches the range used by the user-defined tile providers */
    private static final int ZOOM_MIN = 2;
    private static final int ZOOM_MAX = 18;

    /** side length of a tile in pixels, as the XYZ scheme defines it */
    private static final int TILE_SIZE = 256;

    private static final int VTM_CACHE_SIZE = 20 * 1024 * 1024;

    private TileOverlayLayerHelper() {
        //no instance
    }

    /**
     * Returns a TileDownloadLayer for every enabled overlay (Mapsforge variant).
     *
     * @param cacheByKey caches per overlay key, owned by the caller and reused across rebuilds.
     *                   Each overlay needs a cache of its own so that tiles of different overlays
     *                   cannot collide, but creating them anew on every rebuild would leak them.
     */
    public static List<TileDownloadLayer> getTileLayersMapsforge(final Context context, final MapView mapView, final java.util.Map<String, TileCache> cacheByKey) {
        final List<TileOverlay> overlays = TileOverlays.getEnabled();
        if (overlays.isEmpty()) {
            return Collections.emptyList();
        }

        final List<TileDownloadLayer> result = new ArrayList<>();
        for (TileOverlay overlay : overlays) {
            final URL sample = toUrl(overlay);
            if (sample == null) {
                continue;
            }
            final AbstractTileSource tileSource = new AbstractTileSource(new String[]{sample.getHost()}, portOf(sample)) {
                @Override
                public int getParallelRequestsLimit() {
                    return 8;
                }

                @Override
                public URL getTileUrl(final Tile tile) throws MalformedURLException {
                    return new URL(TileUrlUtils.forTile(overlay.getUrl(), tile.zoomLevel, tile.tileX, tile.tileY));
                }

                @Override
                public byte getZoomLevelMax() {
                    return (byte) ZOOM_MAX;
                }

                @Override
                public byte getZoomLevelMin() {
                    return (byte) ZOOM_MIN;
                }

                @Override
                public boolean hasAlpha() {
                    // overlays must keep their transparency, otherwise they would hide the map below
                    return true;
                }
            };
            tileSource.setUserAgent("cgeo");

            final TileCache tileCache = cacheByKey.computeIfAbsent(overlay.getKey(), key ->
                    AndroidUtil.createTileCache(context, "overlaycache-" + key,
                            mapView.getModel().displayModel.getTileSize(), 2f, mapView.getModel().frameBufferModel.getOverdrawFactor()));
            result.add(new TileDownloadLayer(tileCache, mapView.getModel().mapViewPosition, tileSource, AndroidGraphicFactory.INSTANCE));
        }
        return result;
    }

    /** returns a BitmapTileLayer for every enabled overlay (VTM variant) */
    public static List<BitmapTileLayer> getTileLayersVTM(final Map map) {
        final List<TileOverlay> overlays = TileOverlays.getEnabled();
        if (overlays.isEmpty()) {
            return Collections.emptyList();
        }

        final List<BitmapTileLayer> result = new ArrayList<>();
        for (TileOverlay overlay : overlays) {
            final String[] parts = TileUrlUtils.split(overlay.getUrl());
            if (parts == null) {
                continue;
            }
            final OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
            httpBuilder.cache(new Cache(new File(LocalStorage.getExternalPrivateCgeoDirectory(), "overlaytiles"), VTM_CACHE_SIZE));
            final BitmapTileSource tileSource = BitmapTileSource.builder()
                    .url(parts[0])
                    .tilePath(parts[1])
                    .zoomMax(ZOOM_MAX)
                    .zoomMin(ZOOM_MIN)
                    .build();
            tileSource.setHttpEngine(new OkHttpEngine.OkHttpFactory(httpBuilder));
            tileSource.setHttpRequestHeaders(Collections.singletonMap("User-Agent", "cgeo-android"));
            result.add(new BitmapTileLayer(map, tileSource));
        }
        return result;
    }

    /** returns a TileOverlayOptions for every enabled overlay (Google Maps variant) */
    public static List<TileOverlayOptions> getTileLayersGoogle() {
        final List<TileOverlay> overlays = TileOverlays.getEnabled();
        if (overlays.isEmpty()) {
            return Collections.emptyList();
        }

        final List<TileOverlayOptions> result = new ArrayList<>();
        for (TileOverlay overlay : overlays) {
            result.add(new TileOverlayOptions().tileProvider(new UrlTileProvider(TILE_SIZE, TILE_SIZE) {
                @Override
                public URL getTileUrl(final int x, final int y, final int zoom) {
                    if (zoom < ZOOM_MIN || zoom > ZOOM_MAX) {
                        return null; // nothing to draw outside the range the overlay declares
                    }
                    try {
                        return new URL(TileUrlUtils.forTile(overlay.getUrl(), zoom, x, y));
                    } catch (final MalformedURLException e) {
                        Log.w("Cannot use tile overlay '" + overlay.getName() + "': " + e.getMessage());
                        return null;
                    }
                }
            }).transparency(0f).fadeIn(false));
        }
        return result;
    }

    private static URL toUrl(final TileOverlay overlay) {
        try {
            return new URL(overlay.getUrl());
        } catch (final MalformedURLException e) {
            Log.w("Cannot use tile overlay '" + overlay.getName() + "': " + e.getMessage());
            return null;
        }
    }

    private static int portOf(final URL url) {
        if (url.getPort() > 0) {
            return url.getPort();
        }
        return "http".equals(url.getProtocol()) ? 80 : 443;
    }
}
