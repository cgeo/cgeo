package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogTypeTrackable;

public final class TrackableLog {
    public TrackableLog(String trackCode, String name, int id, int ctl) {
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
}
