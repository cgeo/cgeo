package cgeo.geocaching.maps.mapsforge.v6.caches;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.mapsforge.map.layer.Layer;

public class GeoitemLayers implements Collection<GeoitemLayer> {

    private final HashMap<String, GeoitemLayer> geoitems = new HashMap<>();

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

    private boolean addInternal(final GeoitemLayer geoitem) {
        return geoitems.put(geoitem.getItemCode(), geoitem) != null;
    }

    @Override
    public synchronized boolean add(final GeoitemLayer geoitem) {
        return addInternal(geoitem);
    }

    @Override
    public synchronized boolean addAll(@NonNull final Collection<? extends GeoitemLayer> geoitemsIn) {
        boolean result = true;
        for (final GeoitemLayer geoitem : geoitemsIn) {
            if (!this.addInternal(geoitem)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public synchronized void clear() {
        this.geoitems.clear();
    }

    @Override
    public synchronized boolean contains(final Object object) {
        return this.geoitems.containsValue(object);
    }

    @Override
    public synchronized boolean containsAll(@NonNull final Collection<?> items) {
        return this.geoitems.values().containsAll(items);
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.geoitems.isEmpty();
    }

    @Override
    @NonNull
    public synchronized Iterator<GeoitemLayer> iterator() {
        return new ArrayList<>(this.geoitems.values()).iterator();
    }

    private boolean removeInternal(final Object object) {
        if (object instanceof GeoitemLayer) {
            final GeoitemLayer item = (GeoitemLayer) object;
            return this.geoitems.remove(item.getItem().getItemCode()) != null;

        }

        return false;
    }

    @Override
    public synchronized boolean remove(final Object object) {
        return removeInternal(object);
    }

    @Override
    public synchronized boolean removeAll(@NonNull final Collection<?> items) {
        boolean result = true;
        for (final Object item : items) {
            if (!this.removeInternal(item)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public synchronized boolean retainAll(@NonNull final Collection<?> items) {

        return this.geoitems.values().retainAll(items);
    }

    @Override
    public synchronized int size() {

        return this.geoitems.size();
    }

    @Override
    @NonNull
    public synchronized Object[] toArray() {

        return this.geoitems.values().toArray();
    }

    @SuppressWarnings("hiding")
    @Override
    @NonNull
    public synchronized <GeoitemLayer> GeoitemLayer[] toArray(@NonNull final GeoitemLayer[] array) {

        return this.geoitems.values().toArray(array);
    }

}
