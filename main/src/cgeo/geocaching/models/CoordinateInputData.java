package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;

import android.os.Parcel;
import android.os.Parcelable;

public class CoordinateInputData implements Parcelable {

    private Geopoint geopoint;
    private String geocode;
    private CalculatedCoordinate calculatedCoordinate;
    private String notes;

    public CoordinateInputData() {
        //empty on purpose
    }

    public Geopoint getGeopoint() {
        return geopoint;
    }

    public void setGeopoint(final Geopoint geopoint) {
        this.geopoint = geopoint;
    }

    public CalculatedCoordinate getCalculatedCoordinate() {
        return calculatedCoordinate;
    }

    public void setCalculatedCoordinate(final CalculatedCoordinate calculatedCoordinate) {
        this.calculatedCoordinate = calculatedCoordinate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(final String geocode) {
        this.geocode = geocode;
    }

    protected CoordinateInputData(final Parcel in) {
        geopoint = in.readParcelable(Geopoint.class.getClassLoader());
        calculatedCoordinate = in.readParcelable(CalculatedCoordinate.class.getClassLoader());
        notes = in.readString();
        geocode = in.readString();
    }

    public static final Creator<CoordinateInputData> CREATOR = new Creator<CoordinateInputData>() {
        @Override
        public CoordinateInputData createFromParcel(final Parcel in) {
            return new CoordinateInputData(in);
        }

        @Override
        public CoordinateInputData[] newArray(final int size) {
            return new CoordinateInputData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(geopoint, flags);
        dest.writeParcelable(calculatedCoordinate, flags);
        dest.writeString(notes);
        dest.writeString(geocode);
    }
}
