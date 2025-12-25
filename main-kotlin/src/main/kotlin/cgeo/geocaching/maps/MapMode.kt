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

package cgeo.geocaching.maps

/**
 * Controls the behavior of the map
 */
enum class class MapMode {
    /**
     * Live Map
     */
    LIVE,
    /**
     * Map around some coordinates
     */
    COORDS,
    /**
     * Map with a single cache (no reload on move)
     */
    SINGLE,
    /**
     * Map with a list of caches (no reload on move)
     */
    LIST
}
