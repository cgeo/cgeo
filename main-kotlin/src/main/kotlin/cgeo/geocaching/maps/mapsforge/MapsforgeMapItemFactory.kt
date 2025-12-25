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

package cgeo.geocaching.maps.mapsforge

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl
import cgeo.geocaching.maps.interfaces.GeoPointImpl
import cgeo.geocaching.maps.interfaces.MapItemFactory
import cgeo.geocaching.maps.mapsforge.v6.MapsforgeGeoPoint
import cgeo.geocaching.models.INamedGeoCoordinate

import org.mapsforge.core.model.LatLong

class MapsforgeMapItemFactory : MapItemFactory {

    override     public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return MapsforgeGeoPoint(LatLong(coords.getLatitude(), coords.getLongitude()))
    }

    override     public CachesOverlayItemImpl getCachesOverlayItem(final INamedGeoCoordinate coordinate, final Boolean applyDistanceRule, final Boolean setDraggable) {
        return null
        // @todo
        // return MapsforgeCacheOverlayItem(coordinate, applyDistanceRule)
    }

}
