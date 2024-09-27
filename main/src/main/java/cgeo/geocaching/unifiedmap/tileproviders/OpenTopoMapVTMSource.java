package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.net.Uri;

import androidx.core.util.Pair;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

class OpenTopoMapVTMSource extends AbstractMapsforgeVTMOnlineTileProvider {
    OpenTopoMapVTMSource() {
        super("OpenTopoMap", Uri.parse("https://c.tile.opentopomap.org"), "/{Z}/{X}/{Y}.png", MIN_ZOOM_LEVEL, 18, new Pair<>(CgeoApplication.getInstance().getString(R.string.map_attribution_opentopomap_html), true));
        setSupportsHillshading(false);
    }

}
