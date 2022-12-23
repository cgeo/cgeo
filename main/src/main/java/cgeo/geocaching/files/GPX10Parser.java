package cgeo.geocaching.files;

import android.sax.Element;

import androidx.annotation.NonNull;

public class GPX10Parser extends GPXParser {

    public GPX10Parser(final int listIdIn) {
        super(listIdIn, "http://www.topografix.com/GPX/1/0", "1.0");
    }

    @Override
    protected Element getNodeForExtension(@NonNull final Element waypoint) {
        return waypoint;
    }

    @Override
    protected void registerUrlAndUrlName(@NonNull final Element element) {
        element.getChild(namespace, "url").setEndTextElementListener(this::setUrl);
        element.getChild(namespace, "urlname").setEndTextElementListener(this::setUrlName);
    }

    @Override
    protected void registerScriptUrl(@NonNull final Element element) {
        element.getChild(namespace, "url").setEndTextElementListener(body -> scriptUrl = body);
    }
}
