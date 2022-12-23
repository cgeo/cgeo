package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.AbstractMapSource;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.mapsforge.v6.layers.DownloadLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;

import android.content.Context;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.model.IMapViewPosition;

public abstract class AbstractMapsforgeMapSource extends AbstractMapSource {

    public static final String MAPNIK_TILE_DOWNLOAD_UA = "cgeo";

    private final AbstractTileSource source;

    AbstractMapsforgeMapSource(final MapProvider mapProvider, final String name, final AbstractTileSource source) {
        super(mapProvider, name);
        this.source = source;
    }

    AbstractMapsforgeMapSource(final MapProvider mapProvider, final String name) {
        this(mapProvider, name, null);
    }


    public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
        source.setUserAgent(MAPNIK_TILE_DOWNLOAD_UA);
        return new DownloadLayer(tileCache, mapViewPosition, source, AndroidGraphicFactory.INSTANCE);
    }

    @Override
    public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
        return new ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmap_html), false);
    }

}
