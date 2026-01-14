package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import android.net.Uri;

import androidx.core.util.Pair;

class OpenTopoMapSource extends AbstractMapsforgeOnlineTileProvider {
    OpenTopoMapSource() {
        super(LocalizationUtils.getString(R.string.map_source_osm_opentopomap), Uri.parse("https://c.tile.opentopomap.org"), "/{Z}/{X}/{Y}.png", 2, 18, new Pair<>(LocalizationUtils.getString(R.string.map_attribution_opentopomap_html), true));
    }

}
