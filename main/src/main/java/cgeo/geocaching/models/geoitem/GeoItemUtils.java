package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.ui.ViewUtils;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class GeoItemUtils {

    private static final int MIN_DP_TOUCH_WIDTH = 10;

    private GeoItemUtils() {
        //no instance
    }

    /** returns an item identical to the given item but with the defaultStyle's applied for all empty style attributes */
    public static GeoItem applyDefaultStyle(final GeoItem item, final GeoStyle defaultStyle) {
        if (item instanceof GeoPrimitive) {
            if (Objects.equals(((GeoPrimitive) item).getStyle(), defaultStyle)) {
                return item;
            }
            return ((GeoPrimitive) item).buildUpon().setStyle(GeoStyle.applyAsDefault(((GeoPrimitive) item).getStyle(), defaultStyle)).build();
        }
        if (item instanceof GeoGroup) {
            final GeoGroup.Builder ggb = GeoGroup.builder();
            for (GeoItem child : ((GeoGroup) item).getItems()) {
                ggb.addItems(applyDefaultStyle(child, defaultStyle));
            }
            return ggb.build();
        }
        return item;
    }

    public static int getMinPixelTouchWidth() {
        return ViewUtils.dpToPixel(MIN_DP_TOUCH_WIDTH);
    }

    public static boolean touchesPixelArea(final Geopoint tapped, final Geopoint base, final int bmWidth, final int bmHeight, final float xAnchor, final float yAnchor, @Nullable final ToScreenProjector projector) {
        if (tapped == null || base == null || projector == null) {
            return false;
        }
        if (bmHeight <= 0 || bmWidth <= 0) {
            return false;
        }

        final int[] tapPt = projector.project(tapped);
        final int[] iconBasePt = projector.project(base);

        //check "inside" without creating new Objects
        final int left = (int) (iconBasePt[0] - (xAnchor * bmWidth));
        final int right = (int) (iconBasePt[0] + ((1 - xAnchor) * bmWidth));
        final int top = (int) (iconBasePt[1] - (yAnchor * bmHeight));
        final int bottom = (int) (iconBasePt[1] + ((1 - yAnchor) * bmHeight));
        return inside(tapPt, left, top, right, bottom);
    }

    public static boolean touchesMultiLine(final List<Geopoint> line, final Geopoint tapped, final float lineWidthDp, final ToScreenProjector projector) {
        if (projector == null || line == null || line.size() < 2) {
            return false;
        }
        final int[] tappedPt = projector.project(tapped);
        final int[][] linePoints = projectList(line, projector);
        return touchesMultiLine(linePoints, tappedPt, lineWidthDp);

    }

    public static boolean touchesPolygon(final List<Geopoint> poly, final Geopoint tapped, final float lineWidthDp, final boolean filled, final ToScreenProjector projector) {
        if (projector == null || poly == null || poly.size() < 3) {
            return false;
        }
        final int[] tappedPt = projector.project(tapped);
        final int[][] linePoints = projectList(poly, projector);
        if (touchesMultiLine(linePoints, tappedPt, lineWidthDp)) {
            return true;
        }

        if (filled) {
            return isInPolygon(tappedPt, linePoints);
        }

        return false;
    }

    public static boolean touchesCircle(final Geopoint tapped, final Geopoint center, final float radius, final float lineWidthDp, final boolean filled, final ToScreenProjector projector) {

        //handle "filled" case first -> it is easier
        if (filled && tapped.distanceTo(center) <= radius) {
            return true;
        }

        //calculate whether tap is close enough to circle border
        final int[] centerPt = projector.project(center);
        final int[] tappedPt = projector.project(tapped);
        final int[] circlePoint = projector.project(center.project(0, radius));

        //distance from center in pixels
        final double circleCenterDistancePx = getPointDistance(centerPt, circlePoint);
        final double tappedCenterDistancePx = getPointDistance(centerPt, tappedPt);

        return Math.abs(circleCenterDistancePx - tappedCenterDistancePx) <= getHalfLineWithPx(lineWidthDp);
    }

    private static boolean touchesMultiLine(final int[][] linePoints, final int[] tappedPt, final float lineWidthDp) {
        final int lineWidthPxHalf = getHalfLineWithPx(lineWidthDp);
        int[] previous = null;
        for (int[] pPt : linePoints) {
            if (previous != null && touchesLine(tappedPt, previous, pPt, lineWidthPxHalf)) {
                return true;
            }
            previous = pPt;
        }
        return false;
    }

    private static int getHalfLineWithPx(final float lineWidthDp) {
        final int lineWidthPx = ViewUtils.dpToPixel(Math.max(lineWidthDp, MIN_DP_TOUCH_WIDTH));
        return (lineWidthPx + 1) / 2;
    }


    private static boolean touchesLine(final int[] tappedPt, final int[] p1Pt, final int[] p2Pt, final int lineWidthPxHalf) {

        if (pointsEqual(p1Pt, p2Pt)) {
            return pointsEqual(tappedPt, p1Pt);
        }

        if (!inside(tappedPt, p1Pt, p2Pt, lineWidthPxHalf)) {
            return false;
        }

        final double distance = getLineDistance(tappedPt, p1Pt, p2Pt);

        return distance <= lineWidthPxHalf;
    }

    private static double getLineDistance(final int[] pt, final int[] line1Pt, final int[] line2Pt) {
        //calculate distance to a line, see https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
        final double numerator = Math.abs((line2Pt[1] - line1Pt[1]) * pt[0] - (line2Pt[0] - line1Pt[0]) * pt[1] + line2Pt[0] * line1Pt[1] - line2Pt[1] * line1Pt[0]);
        final double denominator = Math.sqrt(Math.pow(line2Pt[1] - line1Pt[1], 2) + Math.pow(line2Pt[0] - line1Pt[0], 2));
        return numerator / denominator;
    }

    private static double getPointDistance(final int[] pt1, final int[] pt2) {
        final double dx = pt1[0] - pt2[0];
        final double dy = pt1[1] - pt2[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    //The given list of points should NOT include a closing point (e.g. first and last point shall NOT be equal)
    private static boolean isInPolygon(final int[] pt, final int[][] poly) {

        //use Ray-Casting / Even-Odd, see https://en.wikipedia.org/wiki/Point_in_polygon / https://en.wikipedia.org/wiki/Even%E2%80%93odd_rule
        boolean c = false;
        int[] prev = poly[poly.length - 1];
        for (int[] curr : poly) {
            if (pointsEqual(pt, curr)) {
                //point is a corner
                return true;
            }

            if ((curr[1] > pt[1]) != (prev[1] > pt[1])) {
              final int slope = (pt[0] - curr[0]) * (prev[1] - curr[1]) - (prev[0] - curr[0]) * (pt[1] - curr[1]);
              if (slope == 0) {
                  //point is on boundary
                  return true;
              }
              if ((slope < 0) != (prev[1] < curr[1])) {
                  c = !c;
              }
            }
            prev = curr;
        }

        return c;
    }

    public static boolean inside(final int[] pt, final int[] r1, final int[] r2, final int border) {
        return pt[0] + border >= Math.min(r1[0], r2[0]) &&
                pt[0] - border <= Math.max(r1[0], r2[0]) &&
                pt[1] + border >= Math.min(r1[1], r2[1]) &&
                pt[1] - border <= Math.max(r1[1], r2[1]);
    }

    public static boolean inside(final int[] pt, final int left, final int top, final int right, final int bottom) {
        return (pt[0] >= Math.min(left, right) && pt[0] <= Math.max(left, right) &&
                pt[1] >= Math.min(top, bottom) && pt[1] <= Math.max(top, bottom));
    }

    private static int[][] projectList(final Collection<Geopoint> coll, final ToScreenProjector projector) {
        final int[][] result = new int[coll.size()][];
        int idx = 0;
        for (Geopoint gp : coll) {
            result[idx++] = projector.project(gp);
        }
        return result;
    }


    private static boolean pointsEqual(final int[] pt1, final int[] pt2) {
        return Arrays.equals(pt1, pt2);
    }
}
