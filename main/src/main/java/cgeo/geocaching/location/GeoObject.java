package cgeo.geocaching.location;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Represents a drawable GeoObject such as a point, polyline or polygon */
public class GeoObject implements Parcelable {

    public enum GeoType { POINT, POLYLINE, POLYGON }

    //immutable
    @NonNull public final GeoType type;
    @NonNull public final List<Geopoint> points;
    @Nullable public final GeoObjectStyle style;

    //lazy-calculated
    private Viewport viewport;

    private GeoObject(@Nullable final GeoType type, @NonNull final List<Geopoint> points, @Nullable final GeoObjectStyle style) {
        this.type = type == null ? GeoType.POLYLINE : type;
        this.points = Collections.unmodifiableList(points);
        this.style = style;
    }

    @Nullable
    public Viewport getViewport() {
        if (viewport == null) {
            viewport = Viewport.containing(this.points);
        }
        return viewport;
    }

    @Nullable
    public Geopoint getCenter() {
        final Viewport vp = getViewport();
        return vp == null ? null : vp.getCenter();
    }

    public Builder buildUpon() {
        return new Builder().setType(type).addPoints(points).setStyle(style);
    }

    public static GeoObject createPoint(final Geopoint p, final GeoObjectStyle style) {
        return new Builder().setType(GeoType.POINT).addPoints(p).setStyle(style).build();
    }

    public static GeoObject createPolyline(final Collection<Geopoint> p, final GeoObjectStyle style) {
        return new Builder().setType(GeoType.POLYLINE).addPoints(p).setStyle(style).build();
    }

    public static GeoObject createPolygon(final Collection<Geopoint> p, final GeoObjectStyle style) {
        return new Builder().setType(GeoType.POLYGON).addPoints(p).setStyle(style).build();
    }

    //equals/HashCode

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeoObject)) {
            return false;
        }
        final GeoObject other = (GeoObject) o;
        return
            Objects.equals(type, other.type) &&
            Objects.equals(points, other.points) &&
            Objects.equals(style, other.style);
    }

    @Override
    public int hashCode() {
        return type.ordinal() * 13 + (points.isEmpty() || points.get(0) == null ? 7 : points.get(0).hashCode());
    }

    //implements Builder

    public static class Builder {
        private GeoType type;
        private final List<Geopoint> points = new ArrayList<>();
        private GeoObjectStyle style;

        public Builder setType(final GeoType type) {
            this.type = type;
            return this;
        }

        public Builder addPoints(final Collection<Geopoint> gps) {
            points.addAll(gps);
            return this;
        }

        public Builder addPoints(final Geopoint ... gps) {
            points.addAll(Arrays.asList(gps));
            return this;
        }

        public Builder setStyle(final GeoObjectStyle style) {
            this.style = style;
            return this;
        }

        public GeoObject build() {
            return new GeoObject(type, points, style);
        }

    }


    // implements Parcelable

    protected GeoObject(final Parcel in) {
        type = GeoType.values()[in.readInt()];
        final List<Geopoint> pointsReadWrite = new ArrayList<>();
        in.readList(pointsReadWrite, Geopoint.class.getClassLoader());
        points = Collections.unmodifiableList(pointsReadWrite);
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
