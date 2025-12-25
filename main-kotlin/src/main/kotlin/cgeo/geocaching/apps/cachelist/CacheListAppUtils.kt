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

package cgeo.geocaching.apps.cachelist

import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.List

class CacheListAppUtils {

    private CacheListAppUtils() {
        // utility class
    }

    public static List<Geocache> filterCoords(final List<Geocache> caches) {
        val cachesWithCoords: List<Geocache> = ArrayList<>(caches.size())
        for (final Geocache geocache : caches) {
            if (geocache.getCoords() != null) {
                cachesWithCoords.add(geocache)
            }
        }
        return cachesWithCoords
    }

}
