package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

import android.content.Context;

import androidx.annotation.NonNull;


import org.apache.commons.lang3.tuple.ImmutablePair;

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


    public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
        return null;
    }

}
