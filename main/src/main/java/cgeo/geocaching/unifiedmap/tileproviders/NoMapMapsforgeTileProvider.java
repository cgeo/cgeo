package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment;

import android.net.Uri;

import androidx.core.util.Pair;

import org.mapsforge.map.view.MapView;

public class NoMapMapsforgeTileProvider extends AbstractMapsforgeTileProvider {
    NoMapMapsforgeTileProvider() {
        super(CgeoApplication.getInstance().getString(R.string.map_source_nomap), Uri.parse(""), 0, 18, new Pair<>("", false));
        supportsBackgroundMaps = true;
    }

    @Override
    public void addTileLayer(final MapsforgeFragment fragment, final MapView map) {
        // nothing to do
    }
}
