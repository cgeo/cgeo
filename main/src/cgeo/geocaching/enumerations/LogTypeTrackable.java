package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;
import java.util.ArrayList;

public enum LogTypeTrackable {
    DO_NOTHING(1, -1, "", R.string.log_tb_nothing, LogType.UNKNOWN),
    VISITED(1001, 5, "_Visited", R.string.log_tb_visit, LogType.VISIT),
    DROPPED_OFF(14, 0, "_DroppedOff", R.string.log_tb_drop, LogType.PLACED_IT),
    RETRIEVED_IT(13, 1, "", R.string.log_retrieved, R.drawable.mark_green_more, LogType.RETRIEVED_IT),
    GRABBED_IT(19, -1, "", R.string.log_tb_grabbed, R.drawable.mark_green_more, LogType.GRABBED_IT),
    NOTE(4, 2, "", R.string.log_tb_note, LogType.NOTE),
    DISCOVERED_IT(48, 3, "", R.string.log_tb_discovered, R.drawable.mark_green, LogType.DISCOVERED_IT),
    ARCHIVED(5, 4, "", R.string.log_tb_archived, R.drawable.mark_red_more, LogType.UNKNOWN),
    UNKNOWN(0, -1, "", R.string.err_unknown, LogType.UNKNOWN);

    public final int id; // id matching LogTypes
    public final int gkid; // This is the id from GeoKrety
    @NonNull final public String action;
    final private int resourceId;
    public final int markerId;
    // A link to the old LogType. This is done while Twitter still only handle LogType
    public final LogType oldLogtype;

    LogTypeTrackable(final int id, final int gkid, @NonNull final String action, final int resourceId, final int markerId, final LogType oldLogtype) {
        this.id = id;
        this.gkid = gkid;
        this.action = action;
        this.resourceId = resourceId;
        this.markerId = markerId;
        this.oldLogtype = oldLogtype;
    }

    LogTypeTrackable(final int id, final int gkid, final String action, final int resourceId, final LogType oldLogtype) {
        this(id, gkid, action, resourceId, R.drawable.mark_gray, oldLogtype);
    }

    @NonNull
    public String getLabel() {
        return CgeoApplication.getInstance().getString(resourceId);
    }

    public static LogTypeTrackable getById(final int id) {
        for (final LogTypeTrackable logTypeTrackable : values()) {
            if (logTypeTrackable.id == id) {
                return logTypeTrackable;
            }
        }
        return UNKNOWN;
    }

    // Specify the list of trackable action when in LogCacheActivity
    public static final ArrayList<LogTypeTrackable> getLogTypeTrackableForLogCache() {
        final ArrayList<LogTypeTrackable> list = new ArrayList<>();
        list.add(DO_NOTHING);
        list.add(VISITED);
        list.add(DROPPED_OFF);
        return list;
    }

    // Some log type doesn't need Coordinates
    public static boolean isCoordinatesNeeded(final LogTypeTrackable typeSelected) {
        return !(RETRIEVED_IT == typeSelected || NOTE == typeSelected);
    }

}
