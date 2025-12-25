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

package cgeo.geocaching.brouter.codec

/**
 * a waypoint matcher gets way geometries
 * from the decoder to find the closest
 * matches to the waypoints
 */
interface WaypointMatcher {
    Boolean start(Int ilonStart, Int ilatStart, Int ilonTarget, Int ilatTarget)

    Unit transferNode(Int ilon, Int ilat)

    Unit end()
}
