package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

public abstract class AbstractMapProvider implements MapProvider {

    @Override
    public void registerMapSource(final MapSource mapSource) {
        MapProviderFactory.registerMapSource(mapSource);
    }
}
