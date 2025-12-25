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

import cgeo.geocaching.models.Route
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.RouteSegment

import android.sax.RootElement

import androidx.annotation.NonNull

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

class GPXIndividualRouteParser : AbstractTrackOrRouteParser() : AbstractTrackOrRouteParser.RouteParse {
    private var tempName: String = ""

    protected GPXIndividualRouteParser(final String namespaceIn, final String versionIn) {
        super(namespaceIn, versionIn, true)
    }

    public Route parse(final InputStream stream) throws IOException, ParserException {
        val root: RootElement = RootElement(namespace, "gpx")
        points = root.getChild(namespace, "rte")
        point = points.getChild(namespace, "rtept")

        point.getChild(namespace, "name").setEndTextElementListener(body -> tempName = body)
        point.setEndElementListener(() -> {
            if (!temp.isEmpty()) {
                result.add(RouteSegment(RouteItem(tempName, temp.get(temp.size() - 1)), temp, true))
                temp = ArrayList<>()
            }
        })

        return super.parse(stream, root)
    }
}
