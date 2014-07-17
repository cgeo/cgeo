package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;



/**
 * Different log types
 */
public enum LogType {

    FOUND_IT(2, "2", "found it", "Found it", R.string.log_found, R.drawable.mark_green),
    DIDNT_FIND_IT(3, "3", "didn't find it", "Didn't find it", R.string.log_dnf, R.drawable.mark_red),
    NOTE(4, "4", "write note", "Comment", R.string.log_note),
    PUBLISH_LISTING(1003, "24", "publish listing", "", R.string.log_published, R.drawable.mark_green_more),
    ENABLE_LISTING(23, "23", "enable listing", "Ready to search", R.string.log_enabled, R.drawable.mark_green_more),
    ARCHIVE(5, "5", "archive", "Archived", R.string.log_archived, R.drawable.mark_red_more),
    UNARCHIVE(12, "12", "unarchive", "", R.string.log_unarchived, R.drawable.mark_green_more),
    TEMP_DISABLE_LISTING(22, "22", "temporarily disable listing", "Temporarily unavailable", R.string.log_disabled, R.drawable.mark_red_more),
    NEEDS_ARCHIVE(7, "7", "needs archived", "", R.string.log_needs_archived, R.drawable.mark_red),
    WILL_ATTEND(9, "9", "will attend", "Will attend", R.string.log_attend),
    ATTENDED(10, "10", "attended", "Attended", R.string.log_attended, R.drawable.mark_green),
    RETRIEVED_IT(13, "13", "retrieved it", "", R.string.log_retrieved, R.drawable.mark_green_more),
    PLACED_IT(14, "14", "placed it", "", R.string.log_placed, R.drawable.mark_green_more),
    GRABBED_IT(19, "19", "grabbed it", "", R.string.log_grabbed, R.drawable.mark_green_more),
    NEEDS_MAINTENANCE(45, "45", "needs maintenance", "Needs maintenance", R.string.log_maintenance_needed, R.drawable.mark_red),
    OWNER_MAINTENANCE(46, "46", "owner maintenance", "", R.string.log_maintained, R.drawable.mark_green_more),
    UPDATE_COORDINATES(47, "47", "update coordinates", "Moved", R.string.log_update),
    DISCOVERED_IT(48, "48", "discovered it", "", R.string.log_discovered, R.drawable.mark_green),
    POST_REVIEWER_NOTE(18, "18", "post reviewer note", "", R.string.log_reviewer),
    SUBMIT_FOR_REVIEW(76, "76", "submit for review", "", R.string.log_submit_for_review),
    VISIT(1001, "75", "visit", "", R.string.log_tb_visit, R.drawable.mark_green),
    WEBCAM_PHOTO_TAKEN(11, "11", "webcam photo taken", "", R.string.log_webcam, R.drawable.mark_green),
    ANNOUNCEMENT(74, "74", "announcement", "", R.string.log_announcement),
    MOVE_COLLECTION(69, "69", "unused_collection", "", R.string.log_movecollection),
    MOVE_INVENTORY(70, "70", "unused_inventory", "", R.string.log_moveinventory),
    RETRACT(25, "25", "retract listing", "", R.string.log_retractlisting),
    MARKED_MISSING(16, "16", "marked missing", "", R.string.log_marked_missing, R.drawable.mark_red),
    OC_TEAM_COMMENT(83, null, "X1", "OC Team comment", R.string.log_oc_team_comment),
    UNKNOWN(0, "unknown", "", "", R.string.err_unknown, R.drawable.mark_red); // LogType not init. yet

    public final int id;
    public final String iconName;
    public final String type;
    public final String oc_type;
    private final int stringId;
    public final int markerId;

    LogType(int id, String iconName, String type, String oc_type, int stringId, int markerId) {
        this.id = id;
        this.iconName = iconName;
        this.type = type;
        this.oc_type = oc_type;
        this.stringId = stringId;
        this.markerId = markerId;
    }

    LogType(int id, String iconName, String type, String oc_type, int stringId) {
        this(id, iconName, type, oc_type, stringId, R.drawable.mark_gray);
    }

    private final static Map<String, LogType> FIND_BY_ICONNAME;
    private final static Map<String, LogType> FIND_BY_TYPE;
    static {
        final HashMap<String, LogType> mappingPattern = new HashMap<>();
        final HashMap<String, LogType> mappingType = new HashMap<>();
        for (LogType lt : values()) {
            if (lt.iconName != null) {
                mappingPattern.put(lt.iconName, lt);
            }
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
        // Special case for post reviewer note, which appears sometimes as 18.png (in individual entries) or as 68.png
        // (in logs counts).
        if ("68".equals(imageType)) {
            return POST_REVIEWER_NOTE;
        }
        final LogType result = imageType != null ? LogType.FIND_BY_ICONNAME.get(imageType.toLowerCase(Locale.US).trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    public static LogType getByType(final String type) {
        final LogType result = type != null ? LogType.FIND_BY_TYPE.get(type.toLowerCase(Locale.US).trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    public final String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getResources().getString(stringId);
    }
}
