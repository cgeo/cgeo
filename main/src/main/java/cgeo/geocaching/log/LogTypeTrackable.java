package cgeo.geocaching.log;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

public enum LogTypeTrackable {
    DO_NOTHING(1, -1, -1, R.string.log_tb_nothing, 0, true),
    VISITED(1001, 75, 5, R.string.log_tb_visit, 0, true),
    DROPPED_OFF(14, -1, 0, R.string.log_tb_drop, 0, true),
    RETRIEVED_IT(13, -1, 1, R.string.log_retrieved, R.drawable.mark_green_more, false),
    GRABBED_IT(19, -1, -1, R.string.log_tb_grabbed, R.drawable.mark_green_more, false),
    NOTE(4, -1, 2, R.string.log_tb_note),
    DISCOVERED_IT(48, -1, 3, R.string.log_tb_discovered, R.drawable.mark_green, false),
    ARCHIVED(5, -1, 4, R.string.log_tb_archived, R.drawable.mark_red_more, false),
    MOVE_COLLECTION(69, -1, -1, R.string.log_movecollection),
    MOVE_INVENTORY(70, -1, -1, R.string.log_moveinventory),
    UNKNOWN(0, -1, -1, R.string.err_unknown);

    public final int id; // id matching LogTypes. Used e.g. fir database/settings storage -> DON'T CHANGE!
    public final int gcApiId; // This is the id from gc.com/travelbug
    public final int gkid; // This is the id from GeoKrety
    //@NonNull public final String action;
    @StringRes
    private final int resourceId;
    @DrawableRes
    public final int markerId;

    public final boolean allowedForInventory;

    private static final List<LogTypeTrackable> ALLOWED_FOR_INVENTORY = new ArrayList<>();
    private static final EnumValueMapper<Integer, LogTypeTrackable> GET_BY_ID = new EnumValueMapper<>();

    static {
        GET_BY_ID.addAll(values(), lt -> lt.id);
        for (LogTypeTrackable lt : values()) {
            if (lt.allowedForInventory) {
                ALLOWED_FOR_INVENTORY.add(lt);
            }
        }
    }

    LogTypeTrackable(final int id, final int gcApiId, final int gkid, @StringRes final int resourceId, @DrawableRes final int markerId, final boolean allowedForInventory) {
        this.id = id;
        this.gcApiId = gcApiId < 0 ? id : gcApiId;
        this.gkid = gkid;
        //this.action = action;
        this.resourceId = resourceId;
        this.markerId = markerId == 0 ? R.drawable.mark_gray : markerId;
        this.allowedForInventory = allowedForInventory;
    }

    LogTypeTrackable(final int id, final int gcApiId, final int gkid, @StringRes final int resourceId) {
        this(id, gcApiId, gkid, resourceId, 0, false);
    }

    @NonNull
    public String getLabel() {
        return CgeoApplication.getInstance().getString(resourceId);
    }

    public static LogTypeTrackable getById(final int id) {
        return GET_BY_ID.get(id, UNKNOWN);
    }

    // Specify the list of trackable action when in LogCacheActivity
    public static List<LogTypeTrackable> getLogTypesAllowedForInventory() {
        return ALLOWED_FOR_INVENTORY;
    }

    // Some log type doesn't need Coordinates
    public static boolean isCoordinatesNeeded(final LogTypeTrackable typeSelected) {
        return !(typeSelected == RETRIEVED_IT || typeSelected == NOTE);
    }

}
