package cgeo.geocaching.maps.mapsforge.v6.caches;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

class GeoEntrySet implements Set<GeoEntry> {

    private final Map<GeoEntry, GeoEntry> entries;

    GeoEntrySet(final int initialSize) {
        entries = new HashMap<>(initialSize);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return entries.containsKey(o);
    }

    @Override
    public Iterator<GeoEntry> iterator() {
        return entries.values().iterator();
    }

    @Override
    @NonNull
    public Object[] toArray() {
        return entries.values().toArray();
    }

    @Override
    @NonNull
    public <T> T[] toArray(final T[] ts) {
        return entries.values().toArray(ts);
    }

    @Override
    public boolean add(final GeoEntry geoEntry) {
        if (contains(geoEntry)) {
            return false;
        }
        entries.put(geoEntry, geoEntry);
        return true;
    }

    @Override
    public boolean remove(final Object o) {
        if (contains(o)) {
            final GeoEntry entryRem = o instanceof GeoEntry ? (GeoEntry) o : null;
            final GeoEntry entry = entries.get(o);
            if (entryRem != null && entry.overlayId == entryRem.overlayId) {
                entries.remove(o);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(final Collection<?> collection) {
        return entries.keySet().containsAll(collection);
    }

    @Override
    public boolean addAll(final Collection<? extends GeoEntry> collection) {
        boolean res = true;
        for (final GeoEntry entry : collection) {
            if (contains(entry)) {
                res = false;
            } else {
                entries.put(entry, entry);
            }
        }
        return res;
    }

    @Override
    public boolean retainAll(final Collection<?> collection) {
        throw new NotImplementedException("GeoEntrySet.retainAll");
    }

    @Override
    public boolean removeAll(final Collection<?> collection) {
        throw new NotImplementedException("GeoEntrySet.removeAll");
    }

    @Override
    public void clear() {
        entries.clear();
    }
}
