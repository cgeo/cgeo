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

import android.content.Context

import androidx.annotation.NonNull

import org.apache.commons.lang3.tuple.ImmutablePair

interface MapSource {
    String getName()

    Boolean isAvailable()

    default String getId() {
        return this.getClass().getName()
    }

    Int getNumericalId()

    MapProvider getMapProvider()

    ImmutablePair<String, Boolean> calculateMapAttribution(Context ctx)

    Boolean supportsHillshading()

    Unit setSupportsHillshading(Boolean supportsHillshading)

}
