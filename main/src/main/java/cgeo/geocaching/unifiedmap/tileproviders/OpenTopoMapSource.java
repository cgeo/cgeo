package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.net.Uri;

import androidx.core.util.Pair;

class OpenTopoMapSource extends AbstractMapsforgeOnlineTileProvider {
    OpenTopoMapSource() {
        super("OpenTopoMap (MF)", Uri.parse("https://c.tile.opentopomap.org"), "/{Z}/{X}/{Y}.png", 2, 18, new Pair<>(CgeoApplication.getInstance().getString(R.string.map_attribution_opentopomap_html), true));
    }

}
