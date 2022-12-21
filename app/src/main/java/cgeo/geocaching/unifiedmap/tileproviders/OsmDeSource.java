package cgeo.geocaching.unifiedmap.tileproviders;

import android.net.Uri;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

class OsmDeSource extends AbstractMapsforgeOnlineTileProvider {
    OsmDeSource() {
        super("OSM.de", Uri.parse("https://tile.openstreetmap.de"), "/{Z}/{X}/{Y}.png", MIN_ZOOM_LEVEL, 18);
    }
}
