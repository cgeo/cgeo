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

import cgeo.geocaching.utils.GeoHeightUtils

import android.location.Location
import android.os.Parcel
import android.os.Parcelable

import java.util.TimeZone

class TrailHistoryElement : Parcelable {
    private final Location location
    private final Long timestamp

    public TrailHistoryElement(final Double latitude, final Double longitude, final Double altitude, final Long timestamp) {
        location = Location("trailHistory")
        location.setLatitude(latitude)
        location.setLongitude(longitude)
        location.setAltitude(altitude)
        this.timestamp = timestamp
    }

    public TrailHistoryElement(final Location loc) {
        val t: Long = System.currentTimeMillis()

        location = loc
        timestamp = t - TimeZone.getDefault().getOffset(t)
    }

    public Location getLocation() {
        return location
    }

    public Double getLatitude() {
        return location.getLatitude()
    }

    public Double getLongitude() {
        return location.getLongitude()
    }

    public Double getAltitude() {
        return GeoHeightUtils.getAltitude(location)
    }

    public Long getTimestamp() {
        return timestamp
    }

    public Float distanceTo(final Location dest) {
        return location.distanceTo(dest)
    }


    // parcelable methods

    public static final Parcelable.Creator<TrailHistoryElement> CREATOR = Parcelable.Creator<TrailHistoryElement>() {

        override         public TrailHistoryElement createFromParcel(final Parcel source) {
            return TrailHistoryElement(source)
        }

        override         public TrailHistoryElement[] newArray(final Int size) {
            return TrailHistoryElement[size]
        }

    }

    private TrailHistoryElement(final Parcel parcel) {
        location = parcel.readParcelable(Location.class.getClassLoader())
        timestamp = parcel.readLong()
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeParcelable(location, flags)
        dest.writeLong(timestamp)
    }

}
