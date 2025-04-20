package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CoordinateType;
import cgeo.geocaching.location.Geopoint;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** represents a (potentially) named geocoordinate such as eg a route or track point */
public class NamedGeoCoordinate implements INamedGeoCoordinate, Parcelable {

    private Geopoint coords;
    private float elevation;
    private String name;
    private String geocode;

    protected NamedGeoCoordinate(final Parcel in) {
        coords = in.readParcelable(Geopoint.class.getClassLoader());
        elevation = in.readFloat();
        name = in.readString();
        geocode = in.readString();
    }

    @Override
    public CoordinateType getCoordType() {
        return CoordinateType.NAMED_COORDINATE;
    }

    @Override
    public Geopoint getCoords() {
        return coords;
    }

    public void setCoords(final Geopoint coords) {
        this.coords = coords;
    }

    @Override
    public float getElevation() {
        return elevation;
    }

    public void setElevation(final float elevation) {
        this.elevation = elevation;
    }

    @NonNull
    @Override
    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(final String geocode) {
        this.geocode = geocode;
    }

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    //equals, hashCode, toString

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof NamedGeoCoordinate)) {
            return false;
        }
        final NamedGeoCoordinate other = (NamedGeoCoordinate) o;
        return
            Objects.equals(coords, other.coords) &&
            Objects.equals(name, other.name) &&
            Objects.equals(elevation, other.elevation) &&
            Objects.equals(geocode, other.geocode);
    }

    @Override
    public int hashCode() {
        return coords == null ? 7 : coords.hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return "name:" + name + "/coords + " + coords + "/e:" + elevation + "/geocode:" + geocode;
    }

    //Parcelable

    public static final Creator<NamedGeoCoordinate> CREATOR = new Creator<NamedGeoCoordinate>() {
        @Override
        public NamedGeoCoordinate createFromParcel(final Parcel in) {
            return new NamedGeoCoordinate(in);
        }

        @Override
        public NamedGeoCoordinate[] newArray(final int size) {
            return new NamedGeoCoordinate[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeParcelable(coords, flags);
        dest.writeFloat(elevation);
        dest.writeString(name);
        dest.writeString(geocode);
    }

}
