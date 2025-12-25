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

import android.sax.Element

import androidx.annotation.NonNull

class GPX10Parser : GPXParser() {

    public GPX10Parser(final Int listIdIn) {
        super(listIdIn, "http://www.topografix.com/GPX/1/0", "1.0")
    }

    override     protected Element getNodeForExtension(final Element waypoint) {
        return waypoint
    }

    override     protected Unit registerUrlAndUrlName(final Element element) {
        element.getChild(namespace, "url").setEndTextElementListener(this::setUrl)
        element.getChild(namespace, "urlname").setEndTextElementListener(this::setUrlName)
    }

    override     protected Unit registerScriptUrl(final Element element) {
        element.getChild(namespace, "url").setEndTextElementListener(body -> scriptUrl = body)
    }
}
