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

import androidx.appcompat.app.AppCompatActivity

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 */
interface MapProvider {

    Boolean isSameActivity(MapSource source1, MapSource source2)

    Class<? : AppCompatActivity()> getMapClass()

    Int getMapViewId()

    default Int getMapAttributionViewId() {
        return 0
    }

    MapItemFactory getMapItemFactory()

    Unit registerMapSource(MapSource mapSource)
}
