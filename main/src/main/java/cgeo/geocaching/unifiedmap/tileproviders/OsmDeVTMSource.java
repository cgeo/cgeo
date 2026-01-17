package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import android.net.Uri;

import androidx.core.util.Pair;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

class OsmDeVTMSource extends AbstractMapsforgeVTMOnlineTileProvider {
    OsmDeVTMSource() {
        super(LocalizationUtils.getString(R.string.map_source_osm_osmde), Uri.parse("https://tile.openstreetmap.de"), "/{Z}/{X}/{Y}.png", MIN_ZOOM_LEVEL, 18, new Pair<>(LocalizationUtils.getString(R.string.map_attribution_openstreetmapde_html), true));
        supportsHillshading = true;
    }
}
