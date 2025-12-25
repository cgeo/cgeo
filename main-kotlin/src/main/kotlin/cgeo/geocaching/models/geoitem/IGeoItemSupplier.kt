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

package cgeo.geocaching.models.geoitem

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport

interface IGeoItemSupplier {

    Boolean isHidden()

    Unit setHidden(Boolean isHidden)

    Boolean hasData()

    //Collection<GeoPrimitive> getGeoData()

    default Viewport getViewport() {
        return getItem().getViewport()
    }

    default Geopoint getCenter() {
        return getViewport().getCenter()
    }

    GeoItem getItem()

}
