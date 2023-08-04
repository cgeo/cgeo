package cgeo.geocaching.unifiedmap.tileproviders;

import android.net.Uri;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

class OpenTopoMapSource extends AbstractMapsforgeOnlineTileProvider {
    OpenTopoMapSource() {
        super("OpenTopoMap", Uri.parse("https://c.tile.opentopomap.org"), "/{Z}/{X}/{Y}.png", MIN_ZOOM_LEVEL, 18);
    }

}
