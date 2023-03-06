package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.graphics.Point;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GeoItemUtils {

    private static final int MIN_DP_TOUCH_WIDTH = 10;

    private GeoItemUtils() {
        //no instance
    }

    public static boolean touchesPixelArea(final Geopoint tapped, final Geopoint base, final int bmWidth, final int bmHeight, final float xAnchor, final float yAnchor, @Nullable final Func1<Geopoint, Point> toScreenCoordFunc) {
        if (tapped == null || base == null || toScreenCoordFunc == null) {
            return false;
        }
        if (bmHeight <= 0 || bmWidth <= 0) {
            return false;
        }

        final Point tapPt = toScreenCoordFunc.call(tapped);
        final Point iconBasePt = toScreenCoordFunc.call(base);
        final Rect iconRect = new Rect(
                (int) (iconBasePt.x - (xAnchor * bmWidth)), // left
                (int) (iconBasePt.y - (yAnchor * bmHeight)), // top
                (int) (iconBasePt.x + ((1 - xAnchor) * bmWidth)), //right,
                (int) (iconBasePt.y + ((1 - yAnchor) * bmHeight))); // bottom
        return iconRect.contains(tapPt.x, tapPt.y);
    }

    public static boolean touchesMultiLine(final List<Geopoint> line, final Geopoint tapped, final float lineWidthDp, final Func1<Geopoint, Point> screenCoordCalc) {
        if (screenCoordCalc == null || line == null || line.size() < 2) {
            return false;
        }
        final int lineWidthPx = ViewUtils.dpToPixel(Math.max(lineWidthDp, MIN_DP_TOUCH_WIDTH));
        final int lineWidthPxHalf = (lineWidthPx + 1) / 2;
        final Point tappedPt = screenCoordCalc.call(tapped);
        Point previous = null;
        for (Geopoint p : line) {
            final Point pPt = screenCoordCalc.call(p);
            if (previous != null && touchesLine(tappedPt, previous, pPt, lineWidthPx, lineWidthPxHalf)) {
                return true;
            }
            previous = pPt;
        }
        return false;
    }

    public static boolean touchesPolygon(final List<Geopoint> poly, final Geopoint tapped, final float lineWidthDp, final Func1<Geopoint, Point> screenCoordCalc) {
        if (touchesMultiLine(poly, tapped, lineWidthDp, screenCoordCalc)) {
            return true;
        }
        if (screenCoordCalc == null || poly == null || poly.size() < 3) {
            return false;
        }
        //TODO: this must get more efficient!
        final List<Point> polyPt = new ArrayList<>();
        for (Geopoint p : poly) {
            polyPt.add(screenCoordCalc.call(p));
        }
        return isInPolygon(screenCoordCalc.call(tapped), polyPt);
    }

    public static boolean touchesCircle(final Geopoint tapped, final Geopoint center, final float radius, final float lineWidthDp, final boolean filled, final Func1<Geopoint, Point> screenCoordCalc) {
        //TODO: this must get better... Covers only "filled" for now
        return tapped.distanceTo(center) <= radius;
    }

    private static boolean touchesLine(final Point tappedPt, final Point p1Pt, final Point p2Pt, final int lineWidthPx, final int lineWidthPxHalf) {

        if (p1Pt.equals(p2Pt)) {
            return tappedPt.equals(p1Pt);
        }

        if (!inside(tappedPt, p1Pt, p2Pt, lineWidthPxHalf)) {
            return false;
        }

        final double distance = getLineDistance(tappedPt, p1Pt, p2Pt);

        return distance <= lineWidthPx;
    }

    private static double getLineDistance(final Point pt, final Point line1Pt, final Point line2Pt) {
        //calculate distance to a line, see https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
        final double numerator = Math.abs((line2Pt.y - line1Pt.y) * pt.x - (line2Pt.x - line1Pt.x) * pt.y + line2Pt.x * line1Pt.y - line2Pt.y * line1Pt.x);
        final double denominator = Math.sqrt(Math.pow(line2Pt.y - line1Pt.y, 2) + Math.pow(line2Pt.x - line1Pt.x, 2));
        return numerator / denominator;
    }

    private static boolean isInPolygon(final Point pt, final List<Point> poly) {

        //use Ray-Casting / Even-Odd, see https://en.wikipedia.org/wiki/Point_in_polygon / https://en.wikipedia.org/wiki/Even%E2%80%93odd_rule
        boolean c = false;
        Point prev = poly.get(poly.size() - 1);
        for (Point curr : poly) {
            if (pt.equals(curr)) {
                //point is a corner
                return true;
            }

            if ((curr.y > pt.y) != (prev.y > pt.y)) {
              final int slope = (pt.x - curr.x) * (prev.y - curr.y) - (prev.x - curr.x) * (pt.y - curr.y);
              if (slope == 0) {
                  //point is on boundary
                  return true;
              }
              if ((slope < 0) != (prev.y < curr.y)) {
                  c = !c;
              }
            }
            prev = curr;
        }

        return c;
    }

    public static boolean inside(final Point pt, final Point r1, final Point r2, final int border) {
        return pt.x + border >= Math.min(r1.x, r2.x) &&
               pt.x - border <= Math.max(r1.x, r2.x) &&
               pt.y + border >= Math.min(r1.y, r2.y) &&
               pt.y - border <= Math.max(r1.y, r2.y);
    }
}
