package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.net.Uri;

import androidx.core.util.Pair;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

public class OsmOrgVTMSource extends AbstractMapsforgeVTMOnlineTileProvider {
    OsmOrgVTMSource() {
        super(CgeoApplication.getInstance().getString(R.string.map_source_osm_mapnik), Uri.parse("https://tile.openstreetmap.org"), "/{Z}/{X}/{Y}.png", MIN_ZOOM_LEVEL, 18, new Pair<>(CgeoApplication.getInstance().getString(R.string.map_attribution_openstreetmap_html), true));
        supportsHillshading = true;
    }
}
