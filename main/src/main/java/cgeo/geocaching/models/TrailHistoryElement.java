package cgeo.geocaching.models;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.TimeZone;

public class TrailHistoryElement implements Parcelable {
    private final Location location;
    private final long timestamp;

    public TrailHistoryElement(final double latitude, final double longitude, final double altitude, final long timestamp) {
        location = new Location("trailHistory");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(altitude);
        this.timestamp = timestamp;
    }

    public TrailHistoryElement(final Location loc) {
        final long t = System.currentTimeMillis();

        location = loc;
        timestamp = t - TimeZone.getDefault().getOffset(t);
    }

    public Location getLocation() {
        return location;
    }

    public double getLatitude() {
        return location.getLatitude();
    }

    public double getLongitude() {
        return location.getLongitude();
    }

    public double getAltitude() {
        return location.getAltitude();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float distanceTo(final Location dest) {
        return location.distanceTo(dest);
    }


    // parcelable methods

    public static final Parcelable.Creator<TrailHistoryElement> CREATOR = new Parcelable.Creator<TrailHistoryElement>() {

        @Override
        public TrailHistoryElement createFromParcel(final Parcel source) {
            return new TrailHistoryElement(source);
        }

        @Override
        public TrailHistoryElement[] newArray(final int size) {
            return new TrailHistoryElement[size];
        }

    };

    private TrailHistoryElement(final Parcel parcel) {
        location = parcel.readParcelable(Location.class.getClassLoader());
        timestamp = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(location, flags);
        dest.writeLong(timestamp);
    }

}
