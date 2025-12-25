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

package cgeo.geocaching.maps.mapsforge.v6.layers

import org.mapsforge.map.layer.Layer

interface ITileLayer {

    Layer getTileLayer()

    Boolean hasThemes()

    Unit onResume()

    Unit onPause()

    Int getFixedTileSize()

    Byte getZoomLevelMin()

    Byte getZoomLevelMax()

}
