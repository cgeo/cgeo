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

package cgeo.geocaching.models

import cgeo.geocaching.location.Geopoint

class Destination : ICoordinate {

    private final Long id
    private final Long date
    private final Geopoint coords

    public Destination(final Long id, final Long date, final Geopoint coords) {
        this.id = id
        this.date = date
        this.coords = coords
    }

    public Destination(final Geopoint coords) {
        this(0, System.currentTimeMillis(), coords)
    }

    public Long getDate() {
        return date
    }

    override     public Geopoint getCoords() {
        return coords
    }

    override     public Int hashCode() {
        return coords.hashCode()
    }

    override     public Boolean equals(final Object obj) {
        if (this == obj) {
            return true
        }
        if (!(obj is Destination)) {
            return false
        }
        return ((Destination) obj).coords == (coords)
    }

    public Long getId() {
        return id
    }

}
