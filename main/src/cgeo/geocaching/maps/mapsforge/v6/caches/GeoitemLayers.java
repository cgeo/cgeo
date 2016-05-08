package cgeo.geocaching.maps.mapsforge.v6.caches;

import org.mapsforge.map.layer.Layer;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class GeoitemLayers implements Collection<GeoitemLayer> {

    private final HashMap<String, GeoitemLayer> geoitems = new HashMap<>();

    public Collection<String> getGeocodes() {
        return new ArrayList<>(geoitems.keySet());
    }

    public Collection<Layer> getAsLayers() {
        return new ArrayList<Layer>(this.geoitems.values());
    }

    public GeoitemLayer getItem(final String itemCode) {
        return geoitems.get(itemCode);
    }

    public Collection<Layer> getMatchingLayers(final Collection<String> newCodes) {
        final ArrayList<Layer> result = new ArrayList<>();
        for (final String code : newCodes) {
            if (geoitems.containsKey(code)) {
                result.add(geoitems.get(code));
            }
        }
        return result;
    }

    @Override
    public boolean add(final GeoitemLayer geoitem) {
        return geoitems.put(geoitem.getItemCode(), geoitem) != null;
    }

    @Override
    public boolean addAll(@NonNull final Collection<? extends GeoitemLayer> geoitemsIn) {
        boolean result = true;
        for (final GeoitemLayer geoitem : geoitemsIn) {
            if (!this.add(geoitem)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public void clear() {
        this.geoitems.clear();
    }

    @Override
    public boolean contains(final Object object) {
        return this.geoitems.containsValue(object);
    }

    @Override
    public boolean containsAll(@NonNull final Collection<?> items) {
        return this.geoitems.values().containsAll(items);
    }

    @Override
    public boolean isEmpty() {
        return this.geoitems.isEmpty();
    }

    @Override
    @NonNull
    public Iterator<GeoitemLayer> iterator() {
        return this.geoitems.values().iterator();
    }

    @Override
    public boolean remove(final Object object) {
        if (object instanceof GeoitemLayer) {
            final GeoitemLayer item = (GeoitemLayer) object;
            return this.geoitems.remove(item.getItem().getItemCode()) != null;

        }

        return false;
    }

    @Override
    public boolean removeAll(@NonNull final Collection<?> items) {
        boolean result = true;
        for (final Object item : items) {
            if (!this.remove(item)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(@NonNull final Collection<?> items) {

        return this.geoitems.values().retainAll(items);
    }

    @Override
    public int size() {

        return this.geoitems.size();
    }

    @Override
    @NonNull
    public Object[] toArray() {

        return this.geoitems.values().toArray();
    }

    @SuppressWarnings("hiding")
    @Override
    @NonNull
    public <GeoitemLayer> GeoitemLayer[] toArray(@NonNull final GeoitemLayer[] array) {

        return this.geoitems.values().toArray(array);
    }

}
