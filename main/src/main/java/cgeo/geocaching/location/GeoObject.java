package cgeo.geocaching.location;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** An abstract GeoObject */
public class GeoObject implements Parcelable {

    public enum GeoType { POINT, POLYLINE, POLYGON }

    private final GeoType type;
    private final List<Geopoint> points = new ArrayList<>();
    private final List<Geopoint> pointsReadOnly = Collections.unmodifiableList(points);

    private final GeoObjectStyle style;

    private Viewport viewport;

    private GeoObject(final GeoType type, final Collection<Geopoint> points, final GeoObjectStyle style) {
        this.type = type == null ? GeoType.POLYLINE : type;
        if (points != null) {
            this.points.addAll(points);
        }
        this.style = style;
    }

    public GeoType getType() {
        return type;
    }

    public List<Geopoint> getPoints() {
        return pointsReadOnly;
    }

    @NonNull
    public GeoObjectStyle getStyle() {
        return style == null ? GeoObjectStyle.DEFAULT : style;
    }

    @Nullable
    public Viewport getViewport() {
        if (viewport == null) {
            viewport = Viewport.containing(getPoints());
        }
        return viewport;
    }

    @Nullable
    public Geopoint getCenter() {
        if (viewport == null) {
            viewport = Viewport.containing(getPoints());
        }
        return viewport == null ? null : viewport.getCenter();
    }

    public static GeoObject createPoint(final Geopoint p, final GeoObjectStyle style) {
        return new GeoObject(GeoType.POINT, Collections.singleton(p), style);
    }

    public static GeoObject createPolyline(final Collection<Geopoint> p, final GeoObjectStyle style) {
        return new GeoObject(GeoType.POLYLINE, p, style);
    }

    public static GeoObject createPolygon(final Collection<Geopoint> p, final GeoObjectStyle style) {
        return new GeoObject(GeoType.POLYGON, p, style);
    }

    // implements Parcelable

    protected GeoObject(final Parcel in) {
        type = GeoType.values()[in.readInt()];
        in.readList(points, Geopoint.class.getClassLoader());
        style = in.readParcelable(GeoObjectStyle.class.getClassLoader());
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
        dest.writeParcelable(style, flags);
    }



}
