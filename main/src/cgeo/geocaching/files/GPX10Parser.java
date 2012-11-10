package cgeo.geocaching.files;

import android.sax.Element;

public class GPX10Parser extends GPXParser {

    public GPX10Parser(int listIdIn) {
        super(listIdIn, "http://www.topografix.com/GPX/1/0", "1.0");
    }

    @Override
    protected Element getCacheParent(Element waypoint) {
        return waypoint;
    }

}
