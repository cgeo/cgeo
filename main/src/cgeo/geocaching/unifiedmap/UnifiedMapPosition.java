package cgeo.geocaching.unifiedmap;

public class UnifiedMapPosition {

    public double latitude;
    public double longitude;
    public int zoomLevel;
    public float bearing;

    public UnifiedMapPosition(final double latitude, final double longitude, final int zoomLevel, final float bearing) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoomLevel = zoomLevel;
        this.bearing = bearing;
    }
}
