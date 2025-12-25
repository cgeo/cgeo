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

import cgeo.geocaching.location.Geopoint

import android.os.Parcel
import android.os.Parcelable

class CoordinateInputData : Parcelable {

    private Geopoint geopoint
    private String geocode
    private CalculatedCoordinate calculatedCoordinate
    private String notes

    public CoordinateInputData() {
        //empty on purpose
    }

    public Geopoint getGeopoint() {
        return geopoint
    }

    public Unit setGeopoint(final Geopoint geopoint) {
        this.geopoint = geopoint
    }

    public CalculatedCoordinate getCalculatedCoordinate() {
        return calculatedCoordinate
    }

    public Unit setCalculatedCoordinate(final CalculatedCoordinate calculatedCoordinate) {
        this.calculatedCoordinate = calculatedCoordinate
    }

    public String getNotes() {
        return notes
    }

    public Unit setNotes(final String notes) {
        this.notes = notes
    }

    public String getGeocode() {
        return geocode
    }

    public Unit setGeocode(final String geocode) {
        this.geocode = geocode
    }

    protected CoordinateInputData(final Parcel in) {
        geopoint = in.readParcelable(Geopoint.class.getClassLoader())
        calculatedCoordinate = in.readParcelable(CalculatedCoordinate.class.getClassLoader())
        notes = in.readString()
        geocode = in.readString()
    }

    public static val CREATOR: Creator<CoordinateInputData> = Creator<CoordinateInputData>() {
        override         public CoordinateInputData createFromParcel(final Parcel in) {
            return CoordinateInputData(in)
        }

        override         public CoordinateInputData[] newArray(final Int size) {
            return CoordinateInputData[size]
        }
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeParcelable(geopoint, flags)
        dest.writeParcelable(calculatedCoordinate, flags)
        dest.writeString(notes)
        dest.writeString(geocode)
    }
}
