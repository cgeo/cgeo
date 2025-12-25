// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps.mapsforge.v6.caches

import androidx.annotation.NonNull

import java.util.Collection
import java.util.HashMap
import java.util.Iterator
import java.util.Map
import java.util.Set

import org.apache.commons.lang3.NotImplementedException

class GeoEntrySet : Set<GeoEntry> {

    private final Map<GeoEntry, GeoEntry> entries

    GeoEntrySet(final Int initialSize) {
        entries = HashMap<>(initialSize)
    }

    override     public Int size() {
        return entries.size()
    }

    override     public Boolean isEmpty() {
        return entries.isEmpty()
    }

    override     public Boolean contains(final Object o) {
        return entries.containsKey(o)
    }

    override     public Iterator<GeoEntry> iterator() {
        return entries.values().iterator()
    }

    override     public Object[] toArray() {
        return entries.values().toArray()
    }

    override     public <T> T[] toArray(final T[] ts) {
        return entries.values().toArray(ts)
    }

    override     public Boolean add(final GeoEntry geoEntry) {
        if (contains(geoEntry)) {
            return false
        }
        entries.put(geoEntry, geoEntry)
        return true
    }

    override     public Boolean remove(final Object o) {
        if (contains(o)) {
            val entryRem: GeoEntry = o is GeoEntry ? (GeoEntry) o : null
            val entry: GeoEntry = entries.get(o)
            if (entryRem != null && entry.overlayId == entryRem.overlayId) {
                entries.remove(o)
            }
            return true
        }
        return false
    }

    override     public Boolean containsAll(final Collection<?> collection) {
        return entries.keySet().containsAll(collection)
    }

    override     public Boolean addAll(final Collection<? : GeoEntry()> collection) {
        Boolean res = true
        for (final GeoEntry entry : collection) {
            if (contains(entry)) {
                res = false
            } else {
                entries.put(entry, entry)
            }
        }
        return res
    }

    override     public Boolean retainAll(final Collection<?> collection) {
        throw NotImplementedException("GeoEntrySet.retainAll")
    }

    override     public Boolean removeAll(final Collection<?> collection) {
        throw NotImplementedException("GeoEntrySet.removeAll")
    }

    override     public Unit clear() {
        entries.clear()
    }
}
