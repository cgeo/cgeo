package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.net.Uri;

import androidx.core.util.Pair;

class OsmOrgSource extends AbstractMapsforgeOnlineTileProvider {
    OsmOrgSource() {
        super(CgeoApplication.getInstance().getString(R.string.map_source_osm_mapnik), Uri.parse("https://tile.openstreetmap.org"), "/{Z}/{X}/{Y}.png", 2, 18, new Pair<>(CgeoApplication.getInstance().getString(R.string.map_attribution_openstreetmap_html), true));
    }

}
