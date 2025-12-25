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

package cgeo.geocaching.connector.tc

import cgeo.geocaching.enumerations.CacheType

import androidx.annotation.NonNull

/**
 * Adapter for cache types used on TerraCaching
 */
class TerraCachingType {

    private TerraCachingType() {
        // utility class
    }

    public static CacheType getCacheType(final String style) {
        switch (style) {
            case "Classic":
                return CacheType.TRADITIONAL
            case "Virtual":
                return CacheType.VIRTUAL
            case "Puzzle":
                return CacheType.MYSTERY
            case "Offset":
                return CacheType.MULTI
            case "Event":
                return CacheType.EVENT
        }
        return CacheType.UNKNOWN
    }
}
