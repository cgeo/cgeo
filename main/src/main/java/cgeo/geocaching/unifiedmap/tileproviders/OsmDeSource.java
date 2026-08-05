package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.net.Uri;

import androidx.core.util.Pair;

class OsmDeSource extends AbstractMapsforgeOnlineTileProvider {
    OsmDeSource() {
        super(CgeoApplication.getInstance().getString(R.string.map_source_osm_osmde), Uri.parse("https://tile.openstreetmap.de"), "/{Z}/{X}/{Y}.png", 2, 18, new Pair<>(CgeoApplication.getInstance().getString(R.string.map_attribution_openstreetmapde_html), true));
    }
}
