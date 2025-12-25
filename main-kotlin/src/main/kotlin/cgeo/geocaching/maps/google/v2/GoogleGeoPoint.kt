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
import cgeo.geocaching.maps.interfaces.GeoPointImpl

import com.google.android.gms.maps.model.LatLng

class GoogleGeoPoint : GeoPointImpl {

    protected final Int latE6, longE6

    public GoogleGeoPoint(final Int latitudeE6, final Int longitudeE6) {
        latE6 = latitudeE6
        longE6 = longitudeE6
    }

    public GoogleGeoPoint(final LatLng latlng) {
        this((Int) (latlng.latitude * 1e6), (Int) (latlng.longitude * 1e6))
    }

    override     public Geopoint getCoords() {
        return Geopoint(getLatitudeE6() / 1e6, getLongitudeE6() / 1e6)
    }

    override     public Int getLatitudeE6() {
        return latE6
    }

    override     public Int getLongitudeE6() {
        return longE6
    }
}
