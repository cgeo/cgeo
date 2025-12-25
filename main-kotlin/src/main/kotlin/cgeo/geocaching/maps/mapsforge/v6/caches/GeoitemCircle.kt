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

import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.layer.overlay.Circle

class GeoitemCircle : Circle() {
    public GeoitemCircle(final LatLong latLong, final Float radius, final Paint paintFill, final Paint paintStroke) {
        super(latLong, radius, paintFill, paintStroke)
    }
}
