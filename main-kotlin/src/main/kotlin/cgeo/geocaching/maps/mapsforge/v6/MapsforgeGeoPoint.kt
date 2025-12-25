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

package cgeo.geocaching.maps.mapsforge.v6

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.interfaces.GeoPointImpl

import org.mapsforge.core.model.LatLong

class MapsforgeGeoPoint : GeoPointImpl {

    private final LatLong latLong

    public MapsforgeGeoPoint(final LatLong latLong) {
        this.latLong = latLong
    }

    override     public Geopoint getCoords() {
        return Geopoint(latLong.latitude, latLong.longitude)
    }

    override     public Int getLatitudeE6() {
        return (Int) (latLong.latitude * 1e6)
    }

    override     public Int getLongitudeE6() {
        return (Int) (latLong.longitude * 1e6)
    }

}
