package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.AbstractMapSource;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.mapsforge.v6.layers.DownloadLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;


import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.v3.android.maps.mapgenerator.MapGeneratorInternal;

public class MapsforgeMapSource extends AbstractMapSource {

    private final MapGeneratorInternal generator;

    MapsforgeMapSource(final String id, final MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
        super(id, mapProvider, name);
        this.generator = generator;
    }

    public MapGeneratorInternal getGenerator() {
        return generator;
    }

    public ITileLayer createTileLayer(final TileCache tileCache, final MapViewPosition mapViewPosition) {
        return new DownloadLayer(tileCache, mapViewPosition, OpenStreetMapMapnik.INSTANCE, AndroidGraphicFactory.INSTANCE);
    }

}
