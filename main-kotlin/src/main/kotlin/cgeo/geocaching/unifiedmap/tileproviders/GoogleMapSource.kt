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

package cgeo.geocaching.unifiedmap.tileproviders

import cgeo.geocaching.R

import com.google.android.gms.maps.GoogleMap

class GoogleMapSource : AbstractGoogleTileProvider() {

    GoogleMapSource() {
        super(GoogleMap.MAP_TYPE_NORMAL, R.string.map_source_google_map)
        supportsThemes = true
    }
}
