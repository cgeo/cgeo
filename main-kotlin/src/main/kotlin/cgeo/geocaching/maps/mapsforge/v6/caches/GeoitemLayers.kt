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

import cgeo.geocaching.enumerations.CoordinateType

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Collection
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.LinkedHashMap
import java.util.Set

class GeoitemLayers : Iterable<GeoitemLayer> {

    /**
     * ordered set of items to be displayed
     */
    private val geoitems: HashMap<String, GeoitemLayer> = LinkedHashMap<>()
    private val cacheCodes: Set<String> = HashSet<>()

    public synchronized Collection<String> getGeocodes() {
        return ArrayList<>(geoitems.keySet())
    }

    public synchronized Collection<String> getCacheGeocodes() {
        return ArrayList<>(cacheCodes)
    }

    public synchronized Int getCacheCount() {
        return cacheCodes.size()
    }

    public synchronized GeoitemLayer getItem(final String itemCode) {
        return geoitems.get(itemCode)
    }

    public synchronized Unit add(final GeoitemLayer geoitem) {
        val result: Boolean = geoitems.put(geoitem.getItemCode(), geoitem) == null
        if (result && geoitem.getItem().getType() == CoordinateType.CACHE) {
            cacheCodes.add(geoitem.getItemCode())
        }
    }

    public synchronized Unit clear() {
        this.geoitems.clear()
        this.cacheCodes.clear()
    }

    override     public synchronized Iterator<GeoitemLayer> iterator() {
        return ArrayList<>(this.geoitems.values()).iterator()
    }

    public synchronized Unit remove(final Object object) {
        if (object is GeoitemLayer) {
            val item: GeoitemLayer = (GeoitemLayer) object
            val result: Boolean = this.geoitems.remove(item.getItem().getItemCode()) != null
            if (result && item.getItem().getType() == CoordinateType.CACHE) {
                cacheCodes.remove(item.getItemCode())
            }
        }
    }

    public synchronized Int size() {

        return this.geoitems.size()
    }

}
