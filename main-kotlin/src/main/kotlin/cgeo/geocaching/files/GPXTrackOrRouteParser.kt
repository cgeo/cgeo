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

import android.sax.Element
import android.sax.RootElement

import androidx.annotation.NonNull

import java.io.IOException
import java.io.InputStream

class GPXTrackOrRouteParser : AbstractTrackOrRouteParser() : AbstractTrackOrRouteParser.RouteParse {

    enum class PARSINGMODE {
        MODE_NONE, MODE_TRACK, MODE_ROUTE
    }
    protected var parsingMode: PARSINGMODE = PARSINGMODE.MODE_NONE

    GPXTrackOrRouteParser(final String namespaceIn, final String versionIn) {
        super(namespaceIn, versionIn)
    }

    override     public Route parse(final InputStream stream) throws IOException, ParserException {
        val root: RootElement = RootElement(namespace, "gpx")

        // check for tracks
        val pointsTrack: Element = root.getChild(namespace, "trk")
        pointsTrack.setStartElementListener(attributes -> {
            if (parsingMode == PARSINGMODE.MODE_NONE) {
                points = pointsTrack
                val trackSegment: Element = points.getChild(namespace, "trkseg")
                point = trackSegment.getChild(namespace, "trkpt")
                configure(PARSINGMODE.MODE_TRACK, false, points)
            }
        })

        // check for GPX routes (tracks take precedence)
        val pointsRoute: Element = root.getChild(namespace, "rte")
        pointsRoute.setStartElementListener(attributes -> {
            if (parsingMode == PARSINGMODE.MODE_NONE) {
                points = pointsRoute
                point = points.getChild(namespace, "rtept")
                configure(PARSINGMODE.MODE_ROUTE, true, point)
            }
        })

        return doParsing(stream, root)
    }

    private Unit configure(final PARSINGMODE parsingMode, final Boolean routeable, final Element endElementForListener) {
        this.parsingMode = parsingMode
        result.setRouteable(routeable)

        resetTempData()
        setNameAndLatLonParsers()
        endElementForListener.setEndElementListener(() -> {
            if (!temp.isEmpty()) {
                result.add(RouteSegment(RouteItem(temp.get(temp.size() - 1)), temp, tempElevation, false))
                resetTempData()
            }
        })
    }

}
