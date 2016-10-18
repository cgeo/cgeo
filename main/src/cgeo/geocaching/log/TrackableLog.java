package cgeo.geocaching.log;

import cgeo.geocaching.connector.trackable.TrackableBrand;

import org.apache.commons.lang3.StringUtils;

public final class TrackableLog {
    public final int ctl;
    public final int id;
    public final String geocode;
    public final String trackCode;
    public final String name;
    public final TrackableBrand brand;
    public LogTypeTrackable action = LogTypeTrackable.DO_NOTHING; // base.logTrackablesAction - no action

    public TrackableLog(final String geocode, final String trackCode, final String name, final int id, final int ctl, final TrackableBrand brand) {
        this.geocode = geocode;
        this.trackCode = trackCode;
        this.name = name;
        this.id = id;
        this.ctl = ctl;
        this.brand = brand;
    }

    public void setAction(final LogTypeTrackable logTypeTrackable) {
        action = logTypeTrackable;
    }

    @Override
    public int hashCode() {
        return StringUtils.defaultString(trackCode).hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof TrackableLog)) {
            return false;
        }
        final TrackableLog tb = (TrackableLog) obj;
        return StringUtils.defaultString(tb.trackCode).equals(trackCode);
    }
}
