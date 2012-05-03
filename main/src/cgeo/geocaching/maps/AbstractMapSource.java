package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

public class AbstractMapSource implements MapSource {

    private final String name;
    private MapProvider mapProvider;

    public AbstractMapSource(MapProvider mapProvider, final String name) {
        this.mapProvider = mapProvider;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String toString() {
        // needed for adapter in selection lists
        return getName();
    }

}
