package cgeo.geocaching.log;

import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.CommonUtils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class TrackableLogEntry implements Parcelable {

    public final String geocode; // The public one, e.g. starting with "TB" for gc.com trackables
    public final String trackingCode; // The secret one
    public final TrackableBrand brand;

    private LogTypeTrackable action = LogTypeTrackable.DO_NOTHING; // base.logTrackablesAction - no action
    private String log;
    private Date date;

    public static TrackableLogEntry of(final Trackable trackable) {
        return new TrackableLogEntry(trackable.getGeocode(), trackable.getTrackingcode(), trackable.getBrand());
    }

    public static TrackableLogEntry of(final String geocode, final String trackingCode, final TrackableBrand brand) {
        return new TrackableLogEntry(geocode, trackingCode, brand);
    }


    private TrackableLogEntry(final String geocode, final String trackingCode, final TrackableBrand brand) {
        this.geocode = geocode;
        this.trackingCode = trackingCode;
        this.brand = brand;
    }

    public TrackableLogEntry setAction(final LogTypeTrackable logTypeTrackable) {
        action = logTypeTrackable == null ? LogTypeTrackable.DO_NOTHING : logTypeTrackable;
        return this;
    }

    @NonNull
    public LogTypeTrackable getAction() {
        return action;
    }

    @Nullable
    public String getLog() {
        return log;
    }

    public TrackableLogEntry setLog(final String log) {
        this.log = log;
        return this;
    }

    @Nullable
    public Date getDate() {
        return date;
    }

    @Nullable
    public Calendar getCalendar() {
        if (date == null) {
            return null;
        }
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    public TrackableLogEntry setDate(final Date date) {
        this.date = date;
        return this;
    }

    // --- equals/hashCode/toString ---


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TrackableLogEntry that = (TrackableLogEntry) o;
        return Objects.equals(geocode, that.geocode)
            && Objects.equals(trackingCode, that.trackingCode)
            && brand == that.brand
            && action == that.action
            && Objects.equals(log, that.log)
            && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geocode, trackingCode, brand, action, log, date);
    }

    @Override
    @NonNull
    public String toString() {
        return "TrackableLogEntry{" +
            "geocode='" + geocode + '\'' +
            ", trackingCode='" + trackingCode + '\'' +
            ", brand=" + brand +
            ", action=" + action +
            ", log='" + log + '\'' +
            ", date=" + date +
            '}';
    }

    // --- Parcelable ---

    protected TrackableLogEntry(final Parcel in) {
        geocode = in.readString();
        brand = CommonUtils.intToEnum(TrackableBrand.class, in.readInt());
        trackingCode = in.readString();
        action = CommonUtils.intToEnum(LogTypeTrackable.class, in.readInt());
        log = in.readString();
        final long dateLong = in.readLong();
        date = dateLong < 0 ? null : new Date(dateLong);
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(geocode);
        dest.writeInt(CommonUtils.enumToInt(brand));
        dest.writeString(trackingCode);
        dest.writeInt(CommonUtils.enumToInt(action));
        dest.writeString(log);
        dest.writeLong(date == null ? -1 : date.getTime());

    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TrackableLogEntry> CREATOR = new Creator<TrackableLogEntry>() {
        @Override
        public TrackableLogEntry createFromParcel(final Parcel in) {
            return new TrackableLogEntry(in);
        }

        @Override
        public TrackableLogEntry[] newArray(final int size) {
            return new TrackableLogEntry[size];
        }
    };


}
