package cgeo.geocaching.files;

import cgeo.geocaching.utils.Log;

import android.sax.Element;

public final class GPX11Parser extends GPXParser {

    public GPX11Parser(final int listIdIn) {
        super(listIdIn, "http://www.topografix.com/GPX/1/1", "1.1");
    }

    @Override
    protected Element getCacheParent(final Element waypoint) {
        return waypoint.getChild(namespace, "extensions");
    }

    @Override
    protected void registerUrlAndUrlName(final Element element) {
        final Element linkElement = element.getChild(namespace, "link");
        linkElement.setStartElementListener(attrs -> {
            try {
                if (attrs.getIndex("href") > -1) {
                    setUrl(attrs.getValue("href"));
                }

            } catch (final RuntimeException e) {
                Log.w("Failed to parse link attributes", e);
            }
        });
        linkElement.getChild(namespace, "text").setEndTextElementListener(body -> {
            setUrlName(body);
        });
    }

    @Override
    protected void registerScriptUrl(final Element element) {
        element.getChild(namespace, "metadata").getChild(namespace, "link").setStartElementListener(attrs -> {
            try {
                if (attrs.getIndex("href") > -1) {
                    scriptUrl = attrs.getValue("href");
                }

            } catch (final RuntimeException e) {
                Log.w("Failed to parse link attributes", e);
            }
        });
    }
}
