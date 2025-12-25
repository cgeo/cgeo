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

package cgeo.geocaching.maps.mapsforge.v6.caches

import androidx.annotation.NonNull

class GeoEntry {

    public final String geocode
    public final Int overlayId

    public GeoEntry(final String geocode, final Int overlayId) {
        this.geocode = geocode
        this.overlayId = overlayId
    }

    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }

        val geoEntry: GeoEntry = (GeoEntry) o

        return geocode == (geoEntry.geocode)

    }

    override     public Int hashCode() {
        return geocode.hashCode()
    }
}
