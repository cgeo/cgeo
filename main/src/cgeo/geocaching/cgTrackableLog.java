package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogTypeTrackable;

public class cgTrackableLog {
    public int ctl = -1;
    public int id = -1;
    public String trackCode = null;
    public String name = null;
    public LogTypeTrackable action = LogTypeTrackable.DO_NOTHING; // base.logTrackablesAction - no action
}
