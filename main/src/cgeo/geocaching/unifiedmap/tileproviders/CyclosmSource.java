package cgeo.geocaching.unifiedmap.tileproviders;

import android.net.Uri;

class CyclosmSource extends AbstractMapsforgeOnlineTileProvider {
    CyclosmSource() {
        super("CyclOSM", Uri.parse("https://a.tile-cyclosm.openstreetmap.fr"), "/cyclosm/{Z}/{X}/{Y}.png", 0, 18);
    }
}
