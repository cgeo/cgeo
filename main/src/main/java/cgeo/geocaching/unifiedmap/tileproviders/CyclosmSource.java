package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.net.Uri;

import androidx.core.util.Pair;

class CyclosmSource extends AbstractMapsforgeOnlineTileProvider {
    CyclosmSource() {
        super(CgeoApplication.getInstance().getString(R.string.map_source_osm_cyclosm), Uri.parse("https://a.tile-cyclosm.openstreetmap.fr"), "/cyclosm/{Z}/{X}/{Y}.png", 0, 18, new Pair<>(CgeoApplication.getInstance().getString(R.string.map_attribution_cyclosm_html), true));
    }
}
