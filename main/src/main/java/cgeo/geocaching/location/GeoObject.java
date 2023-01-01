package cgeo.geocaching.location;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import java.util.Collection;

/** An abstract GeoObject */
public class GeoObject {

    public enum GeoType { POINT, POLYLINE, POLYGON }

    @ColorInt private static final int STROKE_COLOR_DEFAULT = Color.BLACK;
    @ColorInt private static final int FILL_COLOR_DEFAULT = Color.TRANSPARENT;

    private final GeoType type;
    private final Geopoint[] points;

    @ColorInt private final int strokeColor;
    private final float strokeWidth;
    @ColorInt private final int fillColor;

    private GeoObject(final GeoType type, final Geopoint[] points, final Integer strokeColor, final Float strokeWidth, final Integer fillColor) {
        this.type = type == null ? GeoType.POLYLINE : type;
        this.points = points == null ? new Geopoint[0] : points;
        this.strokeColor = strokeColor == null ? STROKE_COLOR_DEFAULT : strokeColor;
        this.strokeWidth = strokeWidth == null ? 2f : strokeWidth;
        this.fillColor = fillColor == null ? FILL_COLOR_DEFAULT : fillColor;
    }

    public GeoType getType() {
        return type;
    }

    public Geopoint[] getPoints() {
        return points;
    }

    @ColorInt
    public int getStrokeColor() {
        return strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    @ColorInt
    public int getFillColor() {
        return fillColor;
    }

    public static GeoObject createPoint(final Geopoint p, final Integer strokeColor, final Float strokeWidth) {
        return new GeoObject(GeoType.POINT, new Geopoint[]{p}, strokeColor, strokeWidth, null);
    }

    public static GeoObject createPolyline(final Collection<Geopoint> p, final Integer strokeColor, final Float strokeWidth) {
        return new GeoObject(GeoType.POLYLINE, p.toArray(new Geopoint[0]), strokeColor, strokeWidth, null);
    }

    public static GeoObject createPolygon(final Collection<Geopoint> p, final Integer strokeColor, final Float strokeWidth, final Integer fillColor) {
        return new GeoObject(GeoType.POLYGON, p.toArray(new Geopoint[0]), strokeColor, strokeWidth, fillColor);
    }


}
