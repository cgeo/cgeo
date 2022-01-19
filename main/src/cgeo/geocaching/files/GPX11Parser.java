package cgeo.geocaching.files;

import cgeo.geocaching.utils.Log;

import android.sax.Element;

import androidx.annotation.NonNull;

public final class GPX11Parser extends GPXParser {

    public GPX11Parser(final int listIdIn) {
        super(listIdIn, "http://www.topografix.com/GPX/1/1", "1.1");
    }

    @Override
    protected Element getNodeForExtension(@NonNull final Element waypoint) {
        return waypoint.getChild(namespace, "extensions");
    }

    @Override
    protected void registerUrlAndUrlName(@NonNull final Element element) {
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
        // only to support other formats, standard is href as attribute
        linkElement.getChild(namespace, "href").setEndTextElementListener(this::setUrl);
        linkElement.getChild(namespace, "text").setEndTextElementListener(this::setUrlName);
    }

    @Override
    protected void registerScriptUrl(@NonNull final Element element) {
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
