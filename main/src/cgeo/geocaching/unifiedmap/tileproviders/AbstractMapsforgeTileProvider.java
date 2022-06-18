package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.unifiedmap.AbstractUnifiedMapView;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.oscim.map.Map;

public abstract class AbstractMapsforgeTileProvider extends AbstractTileProvider {

    protected final Uri mapUri;

    public AbstractMapsforgeTileProvider(final String name, final Uri uri, final int zoomMin, final int zoomMax) {
        super(zoomMin, zoomMax);
        this.tileProviderName = name;
        this.mapUri = uri;
    }

    public abstract void addTileLayer(Map map);

    public AbstractUnifiedMapView getMap() {
        return TileProviderFactory.MAP_MAPSFORGE;
    }

    protected void parseZoomLevel(@Nullable final int[] zoomLevel) {
        if (zoomLevel != null) {
            for (int level : zoomLevel) {
                zoomMin = Math.min(zoomMin, level);
                zoomMax = Math.max(zoomMax, level);
            }
        }
    }

    protected Uri getMapUri() {
        return mapUri;
    }

    @Override
    @NonNull
    public String getId() {
        return super.getId() + ":" + mapUri.getLastPathSegment();
    }

}
