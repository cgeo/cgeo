package cgeo.geocaching.location;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** An abstract GeoObject */
public class GeoObject implements Parcelable {

    public enum GeoType { POINT, POLYLINE, POLYGON }

    @ColorInt private static final int STROKE_COLOR_DEFAULT = Color.BLACK;
    @ColorInt private static final int FILL_COLOR_DEFAULT = Color.TRANSPARENT;

    private final GeoType type;
    private final List<Geopoint> points = new ArrayList<>();
    private final List<Geopoint> pointsReadOnly = Collections.unmodifiableList(points);

    private Viewport viewport;

    @ColorInt private final int strokeColor;
    private final float strokeWidth;
    @ColorInt private final int fillColor;

    private GeoObject(final GeoType type, final Collection<Geopoint> points, final Integer strokeColor, final Float strokeWidth, final Integer fillColor) {
        this.type = type == null ? GeoType.POLYLINE : type;
        if (points != null) {
            this.points.addAll(points);
        }
        this.strokeColor = strokeColor == null ? STROKE_COLOR_DEFAULT : strokeColor;
        this.strokeWidth = strokeWidth == null ? 2f : strokeWidth;
        this.fillColor = fillColor == null ? FILL_COLOR_DEFAULT : fillColor;
    }

    public GeoType getType() {
        return type;
    }

    public List<Geopoint> getPoints() {
        return pointsReadOnly;
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

    public Viewport getViewport() {
        if (viewport == null) {
            viewport = Viewport.containing(getPoints());
        }
        return viewport;
    }

    public Geopoint getCenter() {
        if (viewport == null) {
            viewport = Viewport.containing(getPoints());
        }
        return viewport == null ? null : viewport.getCenter();
    }

    public static GeoObject createPoint(final Geopoint p, final Integer strokeColor, final Float strokeWidth) {
        return new GeoObject(GeoType.POINT, Collections.singleton(p), strokeColor, strokeWidth, null);
    }

    public static GeoObject createPolyline(final Collection<Geopoint> p, final Integer strokeColor, final Float strokeWidth) {
        return new GeoObject(GeoType.POLYLINE, p, strokeColor, strokeWidth, null);
    }

    public static GeoObject createPolygon(final Collection<Geopoint> p, final Integer strokeColor, final Float strokeWidth, final Integer fillColor) {
        return new GeoObject(GeoType.POLYGON, p, strokeColor, strokeWidth, fillColor);
    }

    // implements Parcelable

    protected GeoObject(final Parcel in) {
        type = GeoType.values()[in.readInt()];
        in.readList(points, Geopoint.class.getClassLoader());
        strokeColor = in.readInt();
        strokeWidth = in.readFloat();
        fillColor = in.readInt();
    }

    public static final Creator<GeoObject> CREATOR = new Creator<GeoObject>() {
        @Override
        public GeoObject createFromParcel(final Parcel in) {
            return new GeoObject(in);
        }

        @Override
        public GeoObject[] newArray(final int size) {
            return new GeoObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(type.ordinal());
        dest.writeList(points);
        dest.writeInt(strokeColor);
        dest.writeFloat(strokeWidth);
        dest.writeInt(fillColor);
    }



}
