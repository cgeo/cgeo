package cgeo.geocaching.files;

import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;

import android.sax.Element;

public final class GPX11Parser extends GPXParser {

    public GPX11Parser(cgeoapplication appIn, int listIdIn,
            cgSearch searchIn) {
        super(appIn, listIdIn, searchIn, "http://www.topografix.com/GPX/1/1", "1.1");
    }

    @Override
    protected Element getCacheParent(Element waypoint) {
        return waypoint.getChild(namespace, "extensions");
    }

}
