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

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl
import cgeo.geocaching.maps.interfaces.GeoPointImpl
import cgeo.geocaching.maps.interfaces.MapItemFactory
import cgeo.geocaching.models.INamedGeoCoordinate

class GoogleMapItemFactory : MapItemFactory {

    private val bitmapDescriptorCache: BitmapDescriptorCache = BitmapDescriptorCache()

    override     public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return GoogleGeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6())
    }

    override     public CachesOverlayItemImpl getCachesOverlayItem(final INamedGeoCoordinate coordinate, final Boolean applyDistanceRule, final Boolean setDraggable) {
        val item: GoogleCacheOverlayItem = GoogleCacheOverlayItem(coordinate, applyDistanceRule, setDraggable)
        item.setBitmapDescriptorCache(bitmapDescriptorCache)
        return item
    }
}
