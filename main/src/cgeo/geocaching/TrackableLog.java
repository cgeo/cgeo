package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.TrackableBrand;

public final class TrackableLog {
    public TrackableLog(final String trackCode, final String name, final int id, final int ctl, final TrackableBrand brand) {
        this.trackCode = trackCode;
        this.name = name;
        this.id = id;
        this.ctl = ctl;
        this.brand = brand;
    }

    public final int ctl;
    public final int id;
    public final String trackCode;
    public final String name;
    public final TrackableBrand brand;
    public LogTypeTrackable action = LogTypeTrackable.DO_NOTHING; // base.logTrackablesAction - no action

    public void setAction(final LogTypeTrackable logTypeTrackable) {
        action = logTypeTrackable;
    }
}
