package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

public abstract class AbstractMapSource implements MapSource {

    private final String name;
    private final MapProvider mapProvider;
    private final String id;

    public AbstractMapSource(final String id, final MapProvider mapProvider, final String name) {
        this.id = id;
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

    @Override
    public int getNumericalId() {
        return id.hashCode();
    }

    @Override
    public MapProvider getMapProvider() {
        return mapProvider;
    }
}
