package cgeo.geocaching.geopoint;

import cgeo.geocaching.Settings;
import cgeo.geocaching.utils.Log;


public class Viewport {

    public final Geopoint center;
    public final Geopoint bottomLeft;
    public final Geopoint topRight;

    public Viewport(final Geopoint bottomLeft, final Geopoint topRight) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
        this.center = new Geopoint((bottomLeft.getLatitude() + topRight.getLatitude()) / 2,
                (bottomLeft.getLongitude() + topRight.getLongitude()) / 2);
    }

    public Viewport(final Geopoint center, final double latSpan, final double lonSpan) {
        this.center = center;
        final double centerLat = center.getLatitude();
        final double centerLon = center.getLongitude();
        bottomLeft = new Geopoint(centerLat - latSpan / 2, centerLon - lonSpan / 2);
        topRight = new Geopoint(centerLat + latSpan / 2, centerLon + lonSpan / 2);
    }

    public double getLatitudeMin() {
        return bottomLeft.getLatitude();
    }

    public double getLatitudeMax() {
        return topRight.getLatitude();
    }

    public double getLongitudeMin() {
        return bottomLeft.getLongitude();
    }

    public double getLongitudeMax() {
        return topRight.getLongitude();
    }

    public Geopoint getCenter() {
        return center;
    }

    @Override
    public String toString() {
        return "(" + bottomLeft.toString() + "," + topRight.toString() + ")";
    }

    /**
     * Check if coordinates are located in a viewport (defined by its center and span
     * in each direction).
     *
     * @param centerLat
     *            the viewport center latitude
     * @param centerLon
     *            the viewport center longitude
     * @param spanLat
     *            the latitude span
     * @param spanLon
     *            the longitude span
     * @param coords
     *            the coordinates to check
     * @return true if the coordinates are in the viewport
     */
    public static boolean isCacheInViewPort(int centerLat, int centerLon, int spanLat, int spanLon, final Geopoint coords) {
        return 2 * Math.abs(coords.getLatitudeE6() - centerLat) <= Math.abs(spanLat) &&
                2 * Math.abs(coords.getLongitudeE6() - centerLon) <= Math.abs(spanLon);
    }

    /**
     * Check if an area is located in a viewport (defined by its center and span
     * in each direction).
     *
     * expects coordinates in E6 format
     *
     * @param centerLat1
     * @param centerLon1
     * @param centerLat2
     * @param centerLon2
     * @param spanLat1
     * @param spanLon1
     * @param spanLat2
     * @param spanLon2
     * @return
     */
    public static boolean isInViewPort(int centerLat1, int centerLon1, int centerLat2, int centerLon2, int spanLat1, int spanLon1, int spanLat2, int spanLon2) {
        try {
            final int left1 = centerLat1 - (spanLat1 / 2);
            final int left2 = centerLat2 - (spanLat2 / 2);
            if (left2 <= left1) {
                return false;
            }

            final int right1 = centerLat1 + (spanLat1 / 2);
            final int right2 = centerLat2 + (spanLat2 / 2);
            if (right2 >= right1) {
                return false;
            }

            final int top1 = centerLon1 + (spanLon1 / 2);
            final int top2 = centerLon2 + (spanLon2 / 2);
            if (top2 >= top1) {
                return false;
            }

            final int bottom1 = centerLon1 - (spanLon1 / 2);
            final int bottom2 = centerLon2 - (spanLon2 / 2);
            if (bottom2 <= bottom1) {
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(Settings.tag, "Viewport.isInViewPort: " + e.toString());
            return false;
        }
    }
}
