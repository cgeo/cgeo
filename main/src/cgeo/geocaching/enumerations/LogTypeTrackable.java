package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

public enum LogTypeTrackable {
    DO_NOTHING("", R.string.log_tb_nothing),
    VISITED("_Visited", R.string.log_tb_visit),
    DROPPED_OFF("_DroppedOff", R.string.log_tb_drop);

    final public String action;
    final private int resourceId;

    LogTypeTrackable(String action, int resourceId) {
        this.action = action;
        this.resourceId = resourceId;
    }

    public String getLabel() {
        return CgeoApplication.getInstance().getString(resourceId);
    }
}
