package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment;
import static cgeo.geocaching.maps.mapsforge.AbstractMapsforgeMapSource.MAPNIK_TILE_DOWNLOAD_UA;

import android.net.Uri;

import androidx.core.util.Pair;

import java.net.MalformedURLException;
import java.net.URL;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.view.MapView;

public class AbstractMapsforgeOnlineTileProvider extends AbstractMapsforgeTileProvider {

    private final AbstractTileSource mfTileSource;
    private String tilePath;

    AbstractMapsforgeOnlineTileProvider(final String name, final Uri uri, final String tilePath, final int zoomMin, final int zoomMax, final Pair<String, Boolean> mapAttribution) {
        super(name, uri, zoomMin, zoomMax, mapAttribution);
        this.tilePath = tilePath;
        mfTileSource = new AbstractTileSource(new String[]{ uri.getHost() }, 443) {
            @Override
            public int getParallelRequestsLimit() {
                return 8;
            }

            @Override
            public URL getTileUrl(final Tile tile) throws MalformedURLException {
                // tilePath: "/cyclosm/{Z}/{X}/{Y}.png"
                final String path = AbstractMapsforgeOnlineTileProvider.this.tilePath
                        .replace("{Z}", String.valueOf(tile.zoomLevel))
                        .replace("{X}", String.valueOf(tile.tileX))
                        .replace("{Y}", String.valueOf(tile.tileY));
                return new URL(AbstractMapsforgeOnlineTileProvider.this.mapUri + path);
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
    public void addTileLayer(final MapsforgeFragment fragment, final MapView map) {
        mfTileSource.setUserAgent(MAPNIK_TILE_DOWNLOAD_UA); // @todo
        tileLayer = new TileDownloadLayer(fragment.getTileCache(), map.getModel().mapViewPosition, mfTileSource, AndroidGraphicFactory.INSTANCE);
        map.getLayerManager().getLayers().add(tileLayer);
        onResume(); // start tile downloader
    }

    // ========================================================================
    // Lifecycle methods

    @Override
    public void onPause() {
        if (tileLayer != null) {
            ((TileDownloadLayer) tileLayer).onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tileLayer != null) {
            ((TileDownloadLayer) tileLayer).onResume();
        }
    }

    @Override
    public void onDestroy() {
        if (tileLayer != null) {
            tileLayer.onDestroy();
        }
        super.onDestroy();
    }
}
