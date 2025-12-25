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

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.INamedGeoCoordinate

interface MapItemFactory {

    GeoPointImpl getGeoPointBase(Geopoint coords)

    CachesOverlayItemImpl getCachesOverlayItem(INamedGeoCoordinate iWaypoint, Boolean applyDistanceRule, Boolean setDraggable)

}
