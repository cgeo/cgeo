package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import androidx.annotation.Nullable;

import java.util.HashMap;

public abstract class AbstractGeoitemLayer<T> {

    protected final HashMap<String, T> items = new HashMap<>();

    @Nullable
    protected abstract T add(Geocache cache);

    @Nullable
    T add(final String geocode) {
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);  // @todo check load flags
        if (cache == null) {
            return null;
        }

        final Geopoint coords = cache.getCoords();
        if (coords == null) {
            return null;
        }

        return add(cache);
    }

    /** removes item from internal data structure, needs to be removed from actual layer by superclass */
    protected void remove(final String geocode) {
        synchronized (items) {
            items.remove(geocode);
        }
    }

}
