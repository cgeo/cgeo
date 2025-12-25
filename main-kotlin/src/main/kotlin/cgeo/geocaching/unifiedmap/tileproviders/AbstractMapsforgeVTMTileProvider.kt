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

import cgeo.geocaching.settings.Settings
import cgeo.geocaching.unifiedmap.AbstractMapFragment
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeVtmFragment

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Pair

import org.oscim.map.Map

abstract class AbstractMapsforgeVTMTileProvider : AbstractTileProvider() {

    protected Uri mapUri

    public AbstractMapsforgeVTMTileProvider(final String name, final Uri uri, final Int zoomMin, final Int zoomMax, final Pair<String, Boolean> mapAttribution) {
        super(zoomMin, zoomMax, mapAttribution)
        this.tileProviderName = name + (Settings.showMapsforgeInUnifiedMap() ? " (VTM)" : "")
        this.mapUri = uri
    }

    protected Unit setMapUri(final Uri mapUri) {
        this.mapUri = mapUri
    }

    public abstract Unit addTileLayer(MapsforgeVtmFragment fragment, Map map)

    override     public AbstractMapFragment createMapFragment() {
        return MapsforgeVtmFragment()
    }

    protected Unit parseZoomLevel(final Int[] zoomLevel) {
        if (zoomLevel != null) {
            for (Int level : zoomLevel) {
                zoomMin = Math.min(zoomMin, level)
                zoomMax = Math.max(zoomMax, level)
            }
        }
    }

    protected Uri getMapUri() {
        return mapUri
    }

    override     public String getId() {
        return super.getId() + ":" + mapUri.getLastPathSegment()
    }

}
