package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.filters.NamedFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.formulas.Formula;
import cgeo.geocaching.utils.formulas.FormulaException;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.functions.Action1;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

public final class GeoItemUtils {

    private static final int MIN_DP_TOUCH_WIDTH = 30; // 30dp ~ 4.7mm

    public static final Map<String, BiFunction<Integer, Integer, Boolean>> RULE_OPERATORS = Map.of(
                    ">",  (a, b) -> a > b,
                    ">=", (a, b) -> a >= b,
                    "<",  (a, b) -> a < b,
                    "<=", (a, b) -> a <= b,
                    "==", Integer::equals,
                    "!=", (a, b) -> !a.equals(b)
            );
    private GeoItemUtils() {
        //no instance
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

    public static boolean matchesCondition(final GeoItem item, final String condition) {
        if (condition == null || condition.isEmpty() || item == null) {
            return true;
        }
        try {
            final int p = Settings.getCompactIconMode();
            final Formula f = Formula.compile(condition);
            return f.evaluate(x -> switch (x.toLowerCase(Locale.ROOT)) {
                case "p" -> Value.of(p);
                case "i" -> Value.of(item);
                default -> null;
            }).getAsBoolean();
        } catch (final FormulaException fe) {
            Log.w("Couldn't parse formula " + condition, fe);
            return false;
        }
    }

    public static void foreach(final GeoItem item, final Action1<GeoItem> action) {
        if (item == null || action == null) {
            return;
        }
        action.call(item);
        if (item instanceof GeoGroup) {
            for (GeoItem child : ((GeoGroup) item).getItems()) {
                foreach(child, action);
            }
        }
    }

    public static int countGeocachesInArea(final GeoItem item, final String filterName, final int limit) {
        if (item == null) {
            return 0;
        }
        final ToScreenProjector projector = coord -> new int[]{coord.getLatitudeE6(), coord.getLongitudeE6()};

        //try to find named filter
        final NamedFilter nf = filterName == null ? null : NamedFilter.getFirstByName(filterName);
        final GeocacheFilter filter = nf == null  ? null : nf.getFilter();

        //call DB
        final int limitToUse = limit > 0 && limit < 1000 ? limit : 10;
        final List<Geopoint> coords = DataStore.loadCacheCoordinates(filter, item.getViewport(), limitToUse);

        //count points inside
        final int[] count = new int[]{ 0 };
        for (Geopoint coord : coords) {
            foreach(item, i -> {
                if (i instanceof GeoPrimitive && i.getType().equals(GeoItem.GeoType.POLYGON)) {
                    if (touchesPolygon(((GeoPrimitive) i).getPoints(), coord, 0, true, projector)) {
                        count[0]++;
                    }
                }
            });
        }
        return count[0];
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
