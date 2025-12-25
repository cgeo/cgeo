// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.models

import cgeo.geocaching.enumerations.CoordinateType
import cgeo.geocaching.location.Geopoint

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Objects

/** represents a (potentially) named geocoordinate such as eg a route or track point */
class NamedGeoCoordinate : INamedGeoCoordinate, Parcelable {

    private Geopoint coords
    private Float elevation
    private String name
    private String geocode

    protected NamedGeoCoordinate(final Parcel in) {
        coords = in.readParcelable(Geopoint.class.getClassLoader())
        elevation = in.readFloat()
        name = in.readString()
        geocode = in.readString()
    }

    override     public CoordinateType getCoordType() {
        return CoordinateType.NAMED_COORDINATE
    }

    override     public Geopoint getCoords() {
        return coords
    }

    public Unit setCoords(final Geopoint coords) {
        this.coords = coords
    }

    override     public Float getElevation() {
        return elevation
    }

    public Unit setElevation(final Float elevation) {
        this.elevation = elevation
    }

    override     public String getGeocode() {
        return geocode
    }

    public Unit setGeocode(final String geocode) {
        this.geocode = geocode
    }

    override     public String getName() {
        return name
    }

    public Unit setName(final String name) {
        this.name = name
    }

    //equals, hashCode, toString

    override     public Boolean equals(final Object o) {
        if (!(o is NamedGeoCoordinate)) {
            return false
        }
        val other: NamedGeoCoordinate = (NamedGeoCoordinate) o
        return
            Objects == (coords, other.coords) &&
            Objects == (name, other.name) &&
            Objects == (elevation, other.elevation) &&
            Objects == (geocode, other.geocode)
    }

    override     public Int hashCode() {
        return coords == null ? 7 : coords.hashCode()
    }

    override     public String toString() {
        return "name:" + name + "/coords + " + coords + "/e:" + elevation + "/geocode:" + geocode
    }

    //Parcelable

    public static val CREATOR: Creator<NamedGeoCoordinate> = Creator<NamedGeoCoordinate>() {
        override         public NamedGeoCoordinate createFromParcel(final Parcel in) {
            return NamedGeoCoordinate(in)
        }

        override         public NamedGeoCoordinate[] newArray(final Int size) {
            return NamedGeoCoordinate[size]
        }
    }


    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeParcelable(coords, flags)
        dest.writeFloat(elevation)
        dest.writeString(name)
        dest.writeString(geocode)
    }

}
