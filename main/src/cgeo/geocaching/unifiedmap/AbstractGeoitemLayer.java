package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import java.util.HashMap;
import java.util.LinkedList;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;

public abstract class AbstractGeoitemLayer<T> {

    protected final HashMap<String, GeoItemCache<T>> items = new HashMap<>();

    public static class GeoItemCache<T> {
        public GeoPoint geopoint;
        public T mapItem;

        public GeoItemCache(final Geopoint geopoint, final T mapItem) {
            this.geopoint = new GeoPoint(geopoint.getLatitudeE6(), geopoint.getLongitudeE6());
            this.mapItem = mapItem;
        }
    }

    protected abstract void add(Geocache cache);

    void add(final String geocode) {
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);  // @todo check load flags
        if (cache == null) {
            return;
        }

        final Geopoint coords = cache.getCoords();
        if (coords == null) {
            return;
        }

        add(cache);
    }

    /** removes item from internal data structure, needs to be removed from actual layer by superclass */
    protected void remove(final String geocode) {
        synchronized (items) {
            items.remove(geocode);
        }
    }

    public LinkedList<String> find(final BoundingBox boundingBox) {
        final LinkedList<String> result = new LinkedList<>();
        synchronized (items) {
            for (String geocode : items.keySet()) {
                final GeoItemCache<T> item = items.get(geocode);
                if (item != null && boundingBox.contains(item.geopoint)) {
                    result.add(geocode);
                }
            }
        }
        return result;
    }

}
