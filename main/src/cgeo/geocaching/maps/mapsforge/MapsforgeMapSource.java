package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.AbstractMapSource;
import cgeo.geocaching.maps.interfaces.MapProvider;

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

}
