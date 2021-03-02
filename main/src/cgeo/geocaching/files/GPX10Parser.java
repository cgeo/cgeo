package cgeo.geocaching.files;

import android.sax.Element;

public class GPX10Parser extends GPXParser {

    public GPX10Parser(final int listIdIn) {
        super(listIdIn, "http://www.topografix.com/GPX/1/0", "1.0");
    }

    @Override
    protected Element getCacheParent(final Element waypoint) {
        return waypoint;
    }

    @Override
    protected void registerUrlAndUrlName(final Element element) {
        element.getChild(namespace, "url").setEndTextElementListener(body -> {
            setUrl(body);
        });
        element.getChild(namespace, "urlname").setEndTextElementListener(body -> {
            setUrlName(body);
        });
    }

    @Override
    protected void registerScriptUrl(final Element element) {
        element.getChild(namespace, "url").setEndTextElementListener(body -> {
            scriptUrl = body;
        });
    }
}
