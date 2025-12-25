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

import cgeo.geocaching.unifiedmap.AbstractMapFragment
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment

import android.net.Uri

import androidx.annotation.NonNull
import androidx.core.util.Pair

import org.mapsforge.map.layer.TileLayer
import org.mapsforge.map.view.MapView

abstract class AbstractMapsforgeTileProvider : AbstractTileProvider() {

    protected Uri mapUri
    protected TileLayer tileLayer; // either TileRendererLayer or TileDownloadLayer

    public AbstractMapsforgeTileProvider(final String name, final Uri uri, final Int zoomMin, final Int zoomMax, final Pair<String, Boolean> mapAttribution) {
        super(zoomMin, zoomMax, mapAttribution)
        this.tileProviderName = name
        this.mapUri = uri
    }

    protected Unit setMapUri(final Uri mapUri) {
        this.mapUri = mapUri
    }

    public abstract Unit addTileLayer(MapsforgeFragment fragment, MapView map)

    override     public AbstractMapFragment createMapFragment() {
        return MapsforgeFragment()
    }

    protected Uri getMapUri() {
        return mapUri
    }

    override     public String getId() {
        return super.getId() + ":" + mapUri.getLastPathSegment()
    }

    public Unit prepareForTileSourceChange(final MapView mapView) {
        if (tileLayer != null) {
            onPause(); // notify tileProvider
            mapView.getLayerManager().getLayers().remove(tileLayer)
            tileLayer.onDestroy()
            tileLayer.getTileCache().purge()
            tileLayer = null
        }
    }

    public TileLayer getTileLayer() {
        return tileLayer
    }

}
