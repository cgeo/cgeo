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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R

import android.net.Uri

import androidx.core.util.Pair

class CyclosmSource : AbstractMapsforgeOnlineTileProvider() {
    CyclosmSource() {
        super(CgeoApplication.getInstance().getString(R.string.map_source_osm_cyclosm), Uri.parse("https://a.tile-cyclosm.openstreetmap.fr"), "/cyclosm/{Z}/{X}/{Y}.png", 0, 18, Pair<>(CgeoApplication.getInstance().getString(R.string.map_attribution_cyclosm_html), true))
    }
}
