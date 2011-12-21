package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

public enum LogTypeTrackable {
    DO_NOTHING(0, "", R.string.log_tb_nothing),
    VISITED(1, "_Visited", R.string.log_tb_visit),
    DROPPED_OFF(2, "_DroppedOff", R.string.log_tb_drop);

    final public int id;
    final public String action;
    final public int resourceId;

    private LogTypeTrackable(int id, String action, int resourceId) {
        this.id = id;
        this.action = action;
        this.resourceId = resourceId;
    }

    public static LogTypeTrackable findById(int id) {
        for (LogTypeTrackable logType : values()) {
            if (logType.id == id) {
                return logType;
            }
        }
        return DO_NOTHING;
    }

}
