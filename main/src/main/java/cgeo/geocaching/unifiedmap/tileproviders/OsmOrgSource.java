package cgeo.geocaching.unifiedmap.tileproviders;

import android.net.Uri;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

class OsmOrgSource extends AbstractMapsforgeOnlineTileProvider {
    OsmOrgSource() {
        super("OSM.org", Uri.parse("https://tile.openstreetmap.org"), "/{Z}/{X}/{Y}.png", MIN_ZOOM_LEVEL, 18);
    }

}
