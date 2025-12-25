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

import org.mapsforge.core.model.LatLong

class MapsforgeUtils {

    private MapsforgeUtils() {
        // Do not instantiate, utility class
    }

    public static LatLong toLatLong(final Geopoint geopoint) {
        return LatLong(geopoint.getLatitude(), geopoint.getLongitude())
    }

    public static Geopoint toGeopoint(final LatLong latLong) {
        return Geopoint(latLong.getLatitude(), latLong.getLongitude())
    }
}
