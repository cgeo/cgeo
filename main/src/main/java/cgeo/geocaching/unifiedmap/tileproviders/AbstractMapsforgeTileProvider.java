package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.view.MapView;

public abstract class AbstractMapsforgeTileProvider extends AbstractTileProvider {

    protected Uri mapUri;
    protected TileLayer tileLayer; // either TileRendererLayer or TileDownloadLayer

    public AbstractMapsforgeTileProvider(final String name, final Uri uri, final int zoomMin, final int zoomMax, final Pair<String, Boolean> mapAttribution) {
        super(zoomMin, zoomMax, mapAttribution);
        this.tileProviderName = name;
        this.mapUri = uri;
    }

    protected void setMapUri(final Uri mapUri) {
        this.mapUri = mapUri;
    }

    public abstract void addTileLayer(MapsforgeFragment fragment, MapView map);

    @Override
    public AbstractMapFragment createMapFragment() {
        return new MapsforgeFragment();
    }

    protected Uri getMapUri() {
        return mapUri;
    }

    @Override
    @NonNull
    public String getId() {
        return super.getId() + ":" + mapUri.getLastPathSegment();
    }

    public void prepareForTileSourceChange(final MapView mapView) {
        if (tileLayer != null) {
            onPause(); // notify tileProvider
            mapView.getLayerManager().getLayers().remove(tileLayer);
            tileLayer.onDestroy();
            tileLayer.getTileCache().purge();
            tileLayer = null;
        }
    }

    public TileLayer getTileLayer() {
        return tileLayer;
    }

}
