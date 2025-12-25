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

package cgeo.geocaching.maps

import cgeo.geocaching.maps.interfaces.MapProvider
import cgeo.geocaching.maps.interfaces.MapSource

abstract class AbstractMapProvider : MapProvider {

    override     public Unit registerMapSource(final MapSource mapSource) {
        MapProviderFactory.registerMapSource(mapSource)
    }
}
