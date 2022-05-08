package cgeo.geocaching.unifiedmap.tileproviders;

import android.net.Uri;

class MapyCzSource extends AbstractMapsforgeOnlineTileProvider {
    MapyCzSource() {
        super("Mapy.CZ", Uri.parse("https://m1.mapserver.mapy.cz"), "/turist-m/{Z}-{X}-{Y}.png", 5, 18);
    }
}
