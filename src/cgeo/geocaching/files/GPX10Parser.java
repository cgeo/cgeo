package cgeo.geocaching.files;

import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;

import android.sax.Element;

public final class GPX10Parser extends GPXParser {

    public GPX10Parser(cgeoapplication appIn, int listIdIn,
            cgSearch searchIn) {
        super(appIn, listIdIn, searchIn, "http://www.topografix.com/GPX/1/0", "1.0");
    }

    @Override
    protected Element getCacheParent(Element waypoint) {
        return waypoint;
    }

}
