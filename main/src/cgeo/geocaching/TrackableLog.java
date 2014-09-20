package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogTypeTrackable;

public final class TrackableLog {
    public TrackableLog(final String trackCode, final String name, final int id, final int ctl) {
        this.trackCode = trackCode;
        this.name = name;
        this.id = id;
        this.ctl = ctl;
    }

    public final int ctl;
    public final int id;
    public final String trackCode;
    public final String name;
    public LogTypeTrackable action = LogTypeTrackable.DO_NOTHING; // base.logTrackablesAction - no action

    public void setAction(final LogTypeTrackable logTypeTrackable) {
        action = logTypeTrackable;
    }
}
