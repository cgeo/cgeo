package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;



/**
 * Different log types
 */
public enum LogType {

    FOUND_IT(2,"icon_smile","found it",R.string.log_found),
    DIDNT_FIND_IT(3, "icon_sad", "didn't find it", R.string.log_dnf),
    NOTE(4,"icon_note","write note",R.string.log_note),
    PUBLISH_LISTING(1003, "icon_greenlight", "publish listing", R.string.log_published),
    ENABLE_LISTING(23,"icon_enabled","enable listing",R.string.log_enabled),
    ARCHIVE(5,"traffic_cone","archive",R.string.log_archived),
    TEMP_DISABLE_LISTING(22,"icon_disabled","temporarily disable listing",R.string.log_disabled),
    NEEDS_ARCHIVE(7,"icon_remove","needs archived",R.string.log_needs_archived),
    WILL_ATTEND(9,"icon_rsvp","will attend",R.string.log_attend),
    ATTENDED(10,"icon_attended","attended",R.string.log_attended),
    RETRIEVED_IT(13,"picked_up","retrieved it",R.string.log_retrieved),
    PLACED_IT(14,"dropped_off","placed it",R.string.log_placed),
    GRABBED_IT(19,"transfer","grabbed it",R.string.log_grabbed),
    NEEDS_MAINTENANCE(45,"icon_needsmaint","needs maintenance",R.string.log_maintenance_needed),
    OWNER_MAINTENANCE(46,"icon_maint","owner maintenance",R.string.log_maintained),
    UPDATE_COORDINATES(47,"coord_update","update coordinates",R.string.log_update),
    DISCOVERED_IT(48,"icon_discovered","discovered it",R.string.log_discovered),
    POST_REVIEWER_NOTE(18,"big_smile","post reviewer note",R.string.log_reviewed),
    VISIT(1001, "icon_visited", "visit", R.string.log_tb_visit),
    WEBCAM_PHOTO_TAKEN(11, "icon_camera", "webcam photo taken", R.string.log_webcam),
    ANNOUNCEMENT(74, "icon_announcement", "announcement", R.string.log_announcement),
    MOVE_COLLECTION(69, "conflict_collection_icon_note", "unused_collection", R.string.log_movecollection),
    MOVE_INVENTORY(70, "conflict_inventory_icon_note", "unused_inventory", R.string.log_moveinventory),
    UNKNOWN(0, "unknown", "", R.string.err_unknown); // LogType not init. yet

    public final int id;
    public final String iconName;
    public final String type;
    private final int stringId;

    private LogType(int id, String iconName, String type, int stringId) {
        this.id = id;
        this.iconName = iconName;
        this.type = type;
        this.stringId = stringId;
    }

    private final static Map<String, LogType> FIND_BY_ICONNAME;
    private final static Map<String, LogType> FIND_BY_TYPE;
    static {
        final HashMap<String, LogType> mappingPattern = new HashMap<String, LogType>();
        final HashMap<String, LogType> mappingType = new HashMap<String, LogType>();
        for (LogType lt : values()) {
            mappingPattern.put(lt.iconName, lt);
            mappingType.put(lt.type, lt);
        }
        FIND_BY_ICONNAME = Collections.unmodifiableMap(mappingPattern);
        FIND_BY_TYPE = Collections.unmodifiableMap(mappingType);
    }

    public static LogType getById(final int id) {
        for (LogType logType : values()) {
            if (logType.id == id) {
                return logType;
            }
        }
        return UNKNOWN;
    }

    public static LogType getByIconName(final String imageType) {
        final LogType result = imageType != null ? LogType.FIND_BY_ICONNAME.get(imageType.toLowerCase().trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    public static LogType getByType(final String type) {
        final LogType result = type != null ? LogType.FIND_BY_TYPE.get(type.toLowerCase().trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    public final String getL10n() {
        return cgeoapplication.getInstance().getBaseContext().getResources().getString(stringId);
    }
}
