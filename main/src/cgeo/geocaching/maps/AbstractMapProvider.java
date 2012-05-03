package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

public abstract class AbstractMapProvider implements MapProvider {

    @Override
    public boolean isMySource(int sourceId) {
        final MapSource source = MapProviderFactory.getMapSource(sourceId);
        return source != null && source.hasMapProvider(this);
    }
}
