// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.files

import cgeo.geocaching.utils.Log

import android.sax.Element

import androidx.annotation.NonNull

class GPX11Parser : GPXParser() {

    public GPX11Parser(final Int listIdIn) {
        super(listIdIn, "http://www.topografix.com/GPX/1/1", "1.1")
    }

    override     protected Element getNodeForExtension(final Element waypoint) {
        return waypoint.getChild(namespace, "extensions")
    }

    override     protected Unit registerUrlAndUrlName(final Element element) {
        val linkElement: Element = element.getChild(namespace, "link")
        linkElement.setStartElementListener(attrs -> {
            try {
                if (attrs.getIndex("href") > -1) {
                    setUrl(attrs.getValue("href"))
                }

            } catch (final RuntimeException e) {
                Log.w("Failed to parse link attributes", e)
            }
        })
        // only to support other formats, standard is href as attribute
        linkElement.getChild(namespace, "href").setEndTextElementListener(this::setUrl)
        linkElement.getChild(namespace, "text").setEndTextElementListener(this::setUrlName)
    }

    override     protected Unit registerScriptUrl(final Element element) {
        element.getChild(namespace, "metadata").getChild(namespace, "link").setStartElementListener(attrs -> {
            try {
                if (attrs.getIndex("href") > -1) {
                    scriptUrl = attrs.getValue("href")
                }

            } catch (final RuntimeException e) {
                Log.w("Failed to parse link attributes", e)
            }
        })
    }
}
