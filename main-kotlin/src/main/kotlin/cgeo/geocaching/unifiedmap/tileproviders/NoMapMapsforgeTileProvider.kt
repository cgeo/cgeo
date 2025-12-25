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
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment

import android.net.Uri

import androidx.core.util.Pair

import org.mapsforge.map.view.MapView

class NoMapMapsforgeTileProvider : AbstractMapsforgeTileProvider() {
    NoMapMapsforgeTileProvider() {
        super(CgeoApplication.getInstance().getString(R.string.map_source_nomap), Uri.parse(""), 0, 18, Pair<>("", false))
    }

    override     public Unit addTileLayer(final MapsforgeFragment fragment, final MapView map) {
        // nothing to do
    }
}
