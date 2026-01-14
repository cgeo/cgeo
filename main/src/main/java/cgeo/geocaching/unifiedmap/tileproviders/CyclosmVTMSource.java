package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import android.net.Uri;

import androidx.core.util.Pair;

class CyclosmVTMSource extends AbstractMapsforgeVTMOnlineTileProvider {
    CyclosmVTMSource() {
        super(LocalizationUtils.getString(R.string.map_source_osm_cyclosm), Uri.parse("https://a.tile-cyclosm.openstreetmap.fr"), "/cyclosm/{Z}/{X}/{Y}.png", 0, 18, new Pair<>(LocalizationUtils.getString(R.string.map_attribution_cyclosm_html), true));
    }
}
