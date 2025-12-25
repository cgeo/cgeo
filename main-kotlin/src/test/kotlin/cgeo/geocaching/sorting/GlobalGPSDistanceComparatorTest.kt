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

package cgeo.geocaching.sorting

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache

import java.util.ArrayList
import java.util.Collections
import java.util.List

import org.junit.Test

class GlobalGPSDistanceComparatorTest {

    @Test
    public Unit testCompareCaches() {
        val caches: List<Geocache> = ArrayList<>()
        for (Int i = 0; i < 37; i++) {
            val cache: Geocache = Geocache()
            if (i % 3 == 0) {
                cache.setCoords(Geopoint(i, i))
            }
            caches.add(cache)
        }
        Collections.sort(caches, GlobalGPSDistanceComparator())
    }

}
