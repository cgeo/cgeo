package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

import androidx.annotation.NonNull;

public abstract class AbstractMapSource implements MapSource {

    private final String name;
    @NonNull
    private final MapProvider mapProvider;
    private final String id;

    protected AbstractMapSource(final String id, @NonNull final MapProvider mapProvider, final String name) {
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
    @NonNull
    public String toString() {
        // needed for adapter in selection lists
        return getName();
    }

    @Override
    public int getNumericalId() {
        return id.hashCode();
    }

    @Override
    @NonNull
    public MapProvider getMapProvider() {
        return mapProvider;
    }
}
