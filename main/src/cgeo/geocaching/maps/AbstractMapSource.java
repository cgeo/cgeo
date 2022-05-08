package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

public abstract class AbstractMapSource implements MapSource {

    private static final Map<String, Integer> mapSourceIds = new HashMap<>();

    private final String name;
    @NonNull
    private final MapProvider mapProvider;
    private Integer numericId;


    protected AbstractMapSource(@NonNull final MapProvider mapProvider, final String name) {
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
    public int getNumericalId() {
        if (numericId == null) {
            final String id = getId();
            //produce a guaranteed unique numerical id for the string id
            synchronized (mapSourceIds) {
                if (mapSourceIds.containsKey(id)) {
                    numericId = mapSourceIds.get(id);
                } else {
                    numericId = -1000000000 + mapSourceIds.size();
                    mapSourceIds.put(id, numericId);
                }
            }
        }
        return numericId;
    }


    @Override
    @NonNull
    public String toString() {
        // needed for adapter in selection lists
        return getName();
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
