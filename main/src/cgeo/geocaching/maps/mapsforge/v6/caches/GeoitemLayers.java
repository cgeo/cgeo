package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.enumerations.CoordinatesType;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class GeoitemLayers implements Iterable<GeoitemLayer> {

    /**
     * ordered set of items to be displayed
     */
    private final HashMap<String, GeoitemLayer> geoitems = new LinkedHashMap<>();
    private final Set<String> cacheCodes = new HashSet<>();

    public synchronized Collection<String> getGeocodes() {
        return new ArrayList<>(geoitems.keySet());
    }

    public synchronized Collection<String> getCacheGeocodes() {
        return new ArrayList<>(cacheCodes);
    }

    public synchronized int getCacheCount() {
        return cacheCodes.size();
    }

    public synchronized GeoitemLayer getItem(final String itemCode) {
        return geoitems.get(itemCode);
    }

    public synchronized void add(final GeoitemLayer geoitem) {
        final boolean result = geoitems.put(geoitem.getItemCode(), geoitem) == null;
        if (result && geoitem.getItem().getType() == CoordinatesType.CACHE) {
            cacheCodes.add(geoitem.getItemCode());
        }
    }

    public synchronized void clear() {
        this.geoitems.clear();
        this.cacheCodes.clear();
    }

    @Override
    @NonNull
    public synchronized Iterator<GeoitemLayer> iterator() {
        return new ArrayList<>(this.geoitems.values()).iterator();
    }

    public synchronized void remove(final Object object) {
        if (object instanceof GeoitemLayer) {
            final GeoitemLayer item = (GeoitemLayer) object;
            final boolean result = this.geoitems.remove(item.getItem().getItemCode()) != null;
            if (result && item.getItem().getType() == CoordinatesType.CACHE) {
                cacheCodes.remove(item.getItemCode());
            }
        }
    }

    public synchronized int size() {

        return this.geoitems.size();
    }

}
