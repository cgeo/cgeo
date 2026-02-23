package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import android.net.Uri;

import androidx.core.util.Pair;

class OsmOrgSource extends AbstractMapsforgeOnlineTileProvider {
    OsmOrgSource() {
        super(LocalizationUtils.getPlainString(R.string.map_source_osm_mapnik), Uri.parse("https://tile.openstreetmap.org"), "/{Z}/{X}/{Y}.png", 2, 18, new Pair<>(LocalizationUtils.getPlainString(R.string.map_attribution_openstreetmap_html), true));
    }

}
