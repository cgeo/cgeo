package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

public enum LogTypeTrackable {
    DO_NOTHING("", R.string.log_tb_nothing),
    VISITED("_Visited", R.string.log_tb_visit),
    DROPPED_OFF("_DroppedOff", R.string.log_tb_drop);

    @NonNull final public String action;
    final private int resourceId;

    LogTypeTrackable(@NonNull final String action, final int resourceId) {
        this.action = action;
        this.resourceId = resourceId;
    }

    public String getLabel() {
        return CgeoApplication.getInstance().getString(resourceId);
    }
}
