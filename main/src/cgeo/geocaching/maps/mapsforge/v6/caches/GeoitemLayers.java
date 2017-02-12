package cgeo.geocaching.maps.mapsforge.v6.caches;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.mapsforge.map.layer.Layer;

public class GeoitemLayers implements Iterable<GeoitemLayer> {

    /**
     * ordered set of items to be displayed
     */
    private final HashMap<String, GeoitemLayer> geoitems = new LinkedHashMap<>();

    public synchronized Collection<String> getGeocodes() {
        return new ArrayList<>(geoitems.keySet());
    }

    public synchronized Collection<Layer> getAsLayers() {
        return new ArrayList<Layer>(this.geoitems.values());
    }

    public synchronized GeoitemLayer getItem(final String itemCode) {
        return geoitems.get(itemCode);
    }

    public synchronized Collection<Layer> getMatchingLayers(final Collection<String> newCodes) {
        final ArrayList<Layer> result = new ArrayList<>();
        for (final String code : newCodes) {
            if (geoitems.containsKey(code)) {
                result.add(geoitems.get(code));
            }
        }
        return result;
    }

    public synchronized boolean add(final GeoitemLayer geoitem) {
        return geoitems.put(geoitem.getItemCode(), geoitem) != null;
    }

    public synchronized void clear() {
        this.geoitems.clear();
    }

    @Override
    @NonNull
    public synchronized Iterator<GeoitemLayer> iterator() {
        return new ArrayList<>(this.geoitems.values()).iterator();
    }

    public synchronized boolean remove(final Object object) {
        if (object instanceof GeoitemLayer) {
            final GeoitemLayer item = (GeoitemLayer) object;
            return this.geoitems.remove(item.getItem().getItemCode()) != null;

        }

        return false;
    }

    public synchronized int size() {

        return this.geoitems.size();
    }

}
