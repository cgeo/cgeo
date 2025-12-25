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

/**
 * Interface for a position (OsmNode or OsmPath)
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess


interface OsmPos {
    Int getILat()

    Int getILon()

    Short getSElev()

    Double getElev()

    Int calcDistance(OsmPos p)

    Long getIdFromPos()

}
