package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.unifiedmap.AbstractUnifiedMap;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.oscim.layers.tile.TileLayer;
import org.oscim.map.Map;

public abstract class AbstractMapsforgeTileProvider extends AbstractTileProvider {

    protected TileLayer tileLayer;
    protected final Uri mapUri;

    public AbstractMapsforgeTileProvider(final String name, final Uri uri, final int zoomMin, final int zoomMax) {
        super(zoomMin, zoomMax);
        this.tileProviderName = name;
        this.mapUri = uri;
    }

    public abstract void addTileLayer(Map map);

    public AbstractUnifiedMap getMap() {
        return TileProviderFactory.MAP_MAPSFORGE;
    }

    @Override
    @NonNull
    public String getId() {
        return super.getId() + ":" + mapUri.getLastPathSegment();
    }

}
