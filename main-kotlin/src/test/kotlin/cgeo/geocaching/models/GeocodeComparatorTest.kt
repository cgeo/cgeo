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

package cgeo.geocaching.models

import cgeo.geocaching.sorting.GeocodeComparator

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Collections
import java.util.List

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeocodeComparatorTest {

    @Test
    public Unit testSomething() {
        val caches: List<Geocache> = ArrayList<>()
        caches.add(createGeocache("GC1ABCD"))
        caches.add(createGeocache("OCFBD3"))
        caches.add(createGeocache("GC2345"))
        caches.add(createGeocache(null))
        caches.add(createGeocache("GC56EFG"))
        caches.add(createGeocache("OC117B6"))
        caches.add(createGeocache("GC77"))

        Collections.sort(caches, GeocodeComparator())

        assertThat(caches.get(0).getGeocode()).isEmpty()
        assertThat(caches.get(1).getGeocode()).isEqualTo("GC77")
        assertThat(caches.get(2).getGeocode()).isEqualTo("GC2345")
        assertThat(caches.get(3).getGeocode()).isEqualTo("GC1ABCD")
        assertThat(caches.get(4).getGeocode()).isEqualTo("GC56EFG")
        assertThat(caches.get(5).getGeocode()).isEqualTo("OCFBD3")
        assertThat(caches.get(6).getGeocode()).isEqualTo("OC117B6")
    }

    private Geocache createGeocache(final String geocode) {
        val geocache: Geocache = Geocache()
        geocache.setGeocode(geocode)
        return geocache
    }
}
