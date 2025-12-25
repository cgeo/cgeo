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

package cgeo.geocaching.log

import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.utils.CommonUtils

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Calendar
import java.util.Date
import java.util.Objects

class TrackableLogEntry : Parcelable {

    public final String geocode; // The public one, e.g. starting with "TB" for gc.com trackables
    public final String trackingCode; // The secret one
    public final TrackableBrand brand

    private var action: LogTypeTrackable = LogTypeTrackable.DO_NOTHING; // base.logTrackablesAction - no action
    private String log
    private Date date

    public static TrackableLogEntry of(final Trackable trackable) {
        return TrackableLogEntry(trackable.getGeocode(), trackable.getTrackingcode(), trackable.getBrand())
    }

    public static TrackableLogEntry of(final String geocode, final String trackingCode, final TrackableBrand brand) {
        return TrackableLogEntry(geocode, trackingCode, brand)
    }


    private TrackableLogEntry(final String geocode, final String trackingCode, final TrackableBrand brand) {
        this.geocode = geocode
        this.trackingCode = trackingCode
        this.brand = brand
    }

    public TrackableLogEntry setAction(final LogTypeTrackable logTypeTrackable) {
        action = logTypeTrackable == null ? LogTypeTrackable.DO_NOTHING : logTypeTrackable
        return this
    }

    public LogTypeTrackable getAction() {
        return action
    }

    public String getLog() {
        return log
    }

    public TrackableLogEntry setLog(final String log) {
        this.log = log
        return this
    }

    public Date getDate() {
        return date
    }

    public Calendar getCalendar() {
        if (date == null) {
            return null
        }
        val cal: Calendar = Calendar.getInstance()
        cal.setTime(date)
        return cal
    }

    public TrackableLogEntry setDate(final Date date) {
        this.date = date
        return this
    }

    // --- equals/hashCode/toString ---


    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }
        val that: TrackableLogEntry = (TrackableLogEntry) o
        return Objects == (geocode, that.geocode)
            && Objects == (trackingCode, that.trackingCode)
            && brand == that.brand
            && action == that.action
            && Objects == (log, that.log)
            && Objects == (date, that.date)
    }

    override     public Int hashCode() {
        return Objects.hash(geocode, trackingCode, brand, action, log, date)
    }

    override     public String toString() {
        return "TrackableLogEntry{" +
            "geocode='" + geocode + '\'' +
            ", trackingCode='" + trackingCode + '\'' +
            ", brand=" + brand +
            ", action=" + action +
            ", log='" + log + '\'' +
            ", date=" + date +
            '}'
    }

    // --- Parcelable ---

    protected TrackableLogEntry(final Parcel in) {
        geocode = in.readString()
        brand = CommonUtils.intToEnum(TrackableBrand.class, in.readInt())
        trackingCode = in.readString()
        action = CommonUtils.intToEnum(LogTypeTrackable.class, in.readInt())
        log = in.readString()
        val dateLong: Long = in.readLong()
        date = dateLong < 0 ? null : Date(dateLong)
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeString(geocode)
        dest.writeInt(CommonUtils.enumToInt(brand))
        dest.writeString(trackingCode)
        dest.writeInt(CommonUtils.enumToInt(action))
        dest.writeString(log)
        dest.writeLong(date == null ? -1 : date.getTime())

    }

    override     public Int describeContents() {
        return 0
    }

    public static val CREATOR: Creator<TrackableLogEntry> = Creator<TrackableLogEntry>() {
        override         public TrackableLogEntry createFromParcel(final Parcel in) {
            return TrackableLogEntry(in)
        }

        override         public TrackableLogEntry[] newArray(final Int size) {
            return TrackableLogEntry[size]
        }
    }


}
