package cgeo.geocaching.geopoint;

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
}
