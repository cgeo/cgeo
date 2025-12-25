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

package cgeo.geocaching.maps.interfaces

/**
 * Defines the common functions of the provider-specific
 * MapController implementations
 */
interface MapControllerImpl {

    Unit setZoom(Int mapzoom)

    Unit setCenter(GeoPointImpl geoPoint)

    Unit animateTo(GeoPointImpl geoPoint)

    Unit zoomToSpan(Int latSpanE6, Int lonSpanE6)

}
