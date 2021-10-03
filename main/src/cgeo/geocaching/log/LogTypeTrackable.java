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
    DO_NOTHING(1, -1, "", R.string.log_tb_nothing, R.drawable.mark_gray, LogType.UNKNOWN, R.drawable.marker_unknown_offline),
    VISITED(1001, 5, "_Visited", R.string.log_tb_visit, R.drawable.mark_green, LogType.VISIT,  R.drawable.tb_visit, 75),
    DROPPED_OFF(14, 0, "_DroppedOff", R.string.log_tb_drop, R.drawable.mark_green, LogType.PLACED_IT, R.drawable.tb_drop),
    RETRIEVED_IT(13, 1, "", R.string.log_retrieved, R.drawable.mark_green_more, LogType.RETRIEVED_IT, R.drawable.tb_retrieve),
    GRABBED_IT(19, -1, "", R.string.log_tb_grabbed, R.drawable.mark_green_more, LogType.GRABBED_IT, R.drawable.tb_grab),
    NOTE(4, 2, "", R.string.log_tb_note, R.drawable.mark_gray, LogType.NOTE, R.drawable.marker_note),
    DISCOVERED_IT(48, 3, "", R.string.log_tb_discovered, R.drawable.mark_blue, LogType.DISCOVERED_IT, R.drawable.tb_discover),
    ARCHIVED(5, 4, "", R.string.log_tb_archived, R.drawable.mark_red_more, LogType.UNKNOWN, R.drawable.marker_archive),
    MOVE_COLLECTION(69, -1, "unused_collection", R.string.log_movecollection, R.drawable.mark_gray, LogType.MOVE_COLLECTION, R.drawable.tb_collection),
    MOVE_INVENTORY(70, -1, "unused_inventory", R.string.log_moveinventory, R.drawable.mark_gray, LogType.MOVE_INVENTORY, R.drawable.tb_inventory),
    UNKNOWN(0, -1, "", R.string.err_unknown, R.drawable.mark_gray, LogType.UNKNOWN, R.drawable.marker_unknown_offline);

    public final int id; // id matching LogTypes
    public final int gcApiId;
    public final int gkid; // This is the id from GeoKrety
    @NonNull public final String action;
    @StringRes
    private final int resourceId;
    @DrawableRes
    public final int markerId;
    public final int overlayId;
    // A link to the old LogType. This is done while Twitter still only handle LogType
    public final LogType oldLogtype;

    LogTypeTrackable(final int id, final int gkid, @NonNull final String action, @StringRes final int resourceId, @DrawableRes final int markerId, final LogType oldLogtype, final int overlayId, final int gcApiId) {
        this.id = id;
        this.gcApiId = gcApiId;
        this.gkid = gkid;
        this.action = action;
        this.resourceId = resourceId;
        this.markerId = markerId;
        this.overlayId = overlayId;
        this.oldLogtype = oldLogtype;
    }

    LogTypeTrackable(final int id, final int gkid, @NonNull final String action, @StringRes final int resourceId, @DrawableRes final int markerId, final LogType oldLogtype, final int overlayId) {
        this(id, gkid, action, resourceId, markerId, oldLogtype, overlayId, id);
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
