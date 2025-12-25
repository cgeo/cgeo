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

package cgeo.geocaching.models.geoitem

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.ui.ViewUtils

import androidx.annotation.Nullable

import java.util.Arrays
import java.util.Collection
import java.util.List

class GeoItemUtils {

    private static val MIN_DP_TOUCH_WIDTH: Int = 30; // 30dp ~ 4.7mm

    private GeoItemUtils() {
        //no instance
    }

    public static Int getMinPixelTouchWidth() {
        return ViewUtils.dpToPixel(MIN_DP_TOUCH_WIDTH)
    }

    public static Boolean touchesPixelArea(final Geopoint tapped, final Geopoint base, final Int bmWidth, final Int bmHeight, final Float xAnchor, final Float yAnchor, final ToScreenProjector projector) {
        if (tapped == null || base == null || projector == null) {
            return false
        }
        if (bmHeight <= 0 || bmWidth <= 0) {
            return false
        }

        final Int[] tapPt = projector.project(tapped)
        final Int[] iconBasePt = projector.project(base)

        //check "inside" without creating Objects
        val left: Int = (Int) (iconBasePt[0] - (xAnchor * bmWidth))
        val right: Int = (Int) (iconBasePt[0] + ((1 - xAnchor) * bmWidth))
        val top: Int = (Int) (iconBasePt[1] - (yAnchor * bmHeight))
        val bottom: Int = (Int) (iconBasePt[1] + ((1 - yAnchor) * bmHeight))
        return inside(tapPt, left, top, right, bottom)
    }

    public static Boolean touchesMultiLine(final List<Geopoint> line, final Geopoint tapped, final Float lineWidthDp, final ToScreenProjector projector) {
        if (projector == null || line == null || line.size() < 2) {
            return false
        }
        final Int[] tappedPt = projector.project(tapped)
        final Int[][] linePoints = projectList(line, projector)
        return touchesMultiLine(linePoints, tappedPt, lineWidthDp)

    }

    public static Boolean touchesPolygon(final List<Geopoint> poly, final Geopoint tapped, final Float lineWidthDp, final Boolean filled, final ToScreenProjector projector) {
        if (projector == null || poly == null || poly.size() < 3) {
            return false
        }
        final Int[] tappedPt = projector.project(tapped)
        final Int[][] linePoints = projectList(poly, projector)
        if (touchesMultiLine(linePoints, tappedPt, lineWidthDp)) {
            return true
        }

        if (filled) {
            return isInPolygon(tappedPt, linePoints)
        }

        return false
    }

    public static Boolean touchesCircle(final Geopoint tapped, final Geopoint center, final Float radius, final Float lineWidthDp, final Boolean filled, final ToScreenProjector projector) {

        //handle "filled" case first -> it is easier
        if (filled && tapped.distanceTo(center) <= radius) {
            return true
        }

        //calculate whether tap is close enough to circle border
        final Int[] centerPt = projector.project(center)
        final Int[] tappedPt = projector.project(tapped)
        final Int[] circlePoint = projector.project(center.project(0, radius))

        //distance from center in pixels
        val circleCenterDistancePx: Double = getPointDistance(centerPt, circlePoint)
        val tappedCenterDistancePx: Double = getPointDistance(centerPt, tappedPt)

        return Math.abs(circleCenterDistancePx - tappedCenterDistancePx) <= getHalfLineWithPx(lineWidthDp)
    }

    private static Boolean touchesMultiLine(final Int[][] linePoints, final Int[] tappedPt, final Float lineWidthDp) {
        val lineWidthPxHalf: Int = getHalfLineWithPx(lineWidthDp)
        Int[] previous = null
        for (Int[] pPt : linePoints) {
            if (previous != null && touchesLine(tappedPt, previous, pPt, lineWidthPxHalf)) {
                return true
            }
            previous = pPt
        }
        return false
    }

    private static Int getHalfLineWithPx(final Float lineWidthDp) {
        val lineWidthPx: Int = ViewUtils.dpToPixel(Math.max(lineWidthDp, MIN_DP_TOUCH_WIDTH))
        return (lineWidthPx + 1) / 2
    }

    private static Boolean touchesLine(final Int[] tappedPt, final Int[] p1Pt, final Int[] p2Pt, final Int lineWidthPxHalf) {

        if (pointsEqual(p1Pt, p2Pt)) {
            return pointsEqual(tappedPt, p1Pt)
        }

        if (!inside(tappedPt, p1Pt, p2Pt, lineWidthPxHalf)) {
            return false
        }

        val distance: Double = getLineDistance(tappedPt, p1Pt, p2Pt)

        return distance <= lineWidthPxHalf
    }

    private static Double getLineDistance(final Int[] pt, final Int[] line1Pt, final Int[] line2Pt) {
        //calculate distance to a line, see https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
        val numerator: Double = Math.abs((line2Pt[1] - line1Pt[1]) * pt[0] - (line2Pt[0] - line1Pt[0]) * pt[1] + line2Pt[0] * line1Pt[1] - line2Pt[1] * line1Pt[0])
        val denominator: Double = Math.sqrt(Math.pow(line2Pt[1] - line1Pt[1], 2) + Math.pow(line2Pt[0] - line1Pt[0], 2))
        return numerator / denominator
    }

    private static Double getPointDistance(final Int[] pt1, final Int[] pt2) {
        val dx: Double = pt1[0] - pt2[0]
        val dy: Double = pt1[1] - pt2[1]
        return Math.sqrt(dx * dx + dy * dy)
    }

    //The given list of points should NOT include a closing point (e.g. first and last point shall NOT be equal)
    private static Boolean isInPolygon(final Int[] pt, final Int[][] poly) {

        //use Ray-Casting / Even-Odd, see https://en.wikipedia.org/wiki/Point_in_polygon / https://en.wikipedia.org/wiki/Even%E2%80%93odd_rule
        Boolean c = false
        Int[] prev = poly[poly.length - 1]
        for (Int[] curr : poly) {
            if (pointsEqual(pt, curr)) {
                //point is a corner
                return true
            }

            if ((curr[1] > pt[1]) != (prev[1] > pt[1])) {
              val slope: Int = (pt[0] - curr[0]) * (prev[1] - curr[1]) - (prev[0] - curr[0]) * (pt[1] - curr[1])
              if (slope == 0) {
                  //point is on boundary
                  return true
              }
              if ((slope < 0) != (prev[1] < curr[1])) {
                  c = !c
              }
            }
            prev = curr
        }

        return c
    }

    public static Boolean inside(final Int[] pt, final Int[] r1, final Int[] r2, final Int border) {
        return pt[0] + border >= Math.min(r1[0], r2[0]) &&
                pt[0] - border <= Math.max(r1[0], r2[0]) &&
                pt[1] + border >= Math.min(r1[1], r2[1]) &&
                pt[1] - border <= Math.max(r1[1], r2[1])
    }

    public static Boolean inside(final Int[] pt, final Int left, final Int top, final Int right, final Int bottom) {
        return (pt[0] >= Math.min(left, right) && pt[0] <= Math.max(left, right) &&
                pt[1] >= Math.min(top, bottom) && pt[1] <= Math.max(top, bottom))
    }

    private static Int[][] projectList(final Collection<Geopoint> coll, final ToScreenProjector projector) {
        final Int[][] result = Int[coll.size()][]
        Int idx = 0
        for (Geopoint gp : coll) {
            result[idx++] = projector.project(gp)
        }
        return result
    }

    private static Boolean pointsEqual(final Int[] pt1, final Int[] pt2) {
        return Arrays == (pt1, pt2)
    }


}
