package cgeo.geocaching.log;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

public enum LogTypeTrackable {
    DO_NOTHING(1, -1, "", R.string.log_tb_nothing, LogType.UNKNOWN),
    VISITED(1001, 5, "_Visited", R.string.log_tb_visit, LogType.VISIT, 75),
    DROPPED_OFF(14, 0, "_DroppedOff", R.string.log_tb_drop, LogType.PLACED_IT),
    RETRIEVED_IT(13, 1, "", R.string.log_retrieved, R.drawable.mark_green_more, LogType.RETRIEVED_IT),
    GRABBED_IT(19, -1, "", R.string.log_tb_grabbed, R.drawable.mark_green_more, LogType.GRABBED_IT),
    NOTE(4, 2, "", R.string.log_tb_note, LogType.NOTE),
    DISCOVERED_IT(48, 3, "", R.string.log_tb_discovered, R.drawable.mark_green, LogType.DISCOVERED_IT),
    ARCHIVED(5, 4, "", R.string.log_tb_archived, R.drawable.mark_red_more, LogType.UNKNOWN),
    MOVE_COLLECTION(69, -1, "unused_collection", R.string.log_movecollection, LogType.MOVE_COLLECTION),
    MOVE_INVENTORY(70, -1, "unused_inventory", R.string.log_moveinventory, LogType.MOVE_INVENTORY),
    UNKNOWN(0, -1, "", R.string.err_unknown, LogType.UNKNOWN);

    public final int id; // id matching LogTypes
    public final int gcApiId;
    public final int gkid; // This is the id from GeoKrety
    @NonNull public final String action;
    @StringRes
    private final int resourceId;
    @DrawableRes
    public final int markerId;
    // A link to the old LogType. This is done while Twitter still only handle LogType
    public final LogType oldLogtype;

    LogTypeTrackable(final int id, final int gkid, @NonNull final String action, @StringRes final int resourceId, @DrawableRes final int markerId, final LogType oldLogtype, final int gcApiId) {
        this.id = id;
        this.gcApiId = gcApiId;
        this.gkid = gkid;
        this.action = action;
        this.resourceId = resourceId;
        this.markerId = markerId;
        this.oldLogtype = oldLogtype;
    }

    LogTypeTrackable(final int id, final int gkid, @NonNull final String action, @StringRes final int resourceId, @DrawableRes final int markerId, final LogType oldLogtype) {
        this(id, gkid, action, resourceId, markerId, oldLogtype, id);
    }

    LogTypeTrackable(final int id, final int gkid, final String action, @StringRes final int resourceId, final LogType oldLogtype, final int gcApiId) {
        this(id, gkid, action, resourceId, R.drawable.mark_gray, oldLogtype, gcApiId);
    }

    LogTypeTrackable(final int id, final int gkid, final String action, @StringRes final int resourceId, final LogType oldLogtype) {
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
        Log.e("LogTypeTrackable.getById(): Failed to lookup id:" + id);
        return UNKNOWN;
    }

    // Specify the list of trackable action when in LogCacheActivity
    public static List<LogTypeTrackable> getLogTypeTrackableForLogCache() {
        final List<LogTypeTrackable> list = new ArrayList<>();
        list.add(DO_NOTHING);
        list.add(VISITED);
        list.add(DROPPED_OFF);
        return list;
    }

    // Some log type doesn't need Coordinates
    public static boolean isCoordinatesNeeded(final LogTypeTrackable typeSelected) {
        return !(typeSelected == RETRIEVED_IT || typeSelected == NOTE);
    }

}
