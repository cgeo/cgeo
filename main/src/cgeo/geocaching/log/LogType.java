package cgeo.geocaching.log;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Different log types
 */
public enum LogType {

    FOUND_IT(2, "2", "Found it", "Found it", R.string.log_found, R.drawable.mark_green, R.drawable.marker_found_offline),
    DIDNT_FIND_IT(3, "3", "Didn't find it", "Didn't find it", R.string.log_dnf, R.drawable.mark_red, R.drawable.marker_not_found_offline),
    NOTE(4, "4", "Write note", "Comment", R.string.log_note, R.drawable.mark_gray, R.drawable.marker_note),
    PUBLISH_LISTING(1003, "24", "Publish Listing", "", R.string.log_published, R.drawable.mark_green_more),
    ENABLE_LISTING(23, "23", "Enable Listing", "Ready to search", R.string.log_enabled, R.drawable.mark_green_more),
    ARCHIVE(5, "5", "Archive", "Archived", R.string.log_archived, R.drawable.mark_red_more, R.drawable.marker_archive),
    UNARCHIVE(12, "12", "Unarchive", "", R.string.log_unarchived, R.drawable.mark_green_more),
    TEMP_DISABLE_LISTING(22, "22", "Temporarily Disable Listing", "Temporarily unavailable", R.string.log_disabled, R.drawable.mark_red_more),
    NEEDS_ARCHIVE(7, "7", "Needs Archived", "", R.string.log_needs_archived, R.drawable.mark_red, R.drawable.marker_archive),
    WILL_ATTEND(9, "9", "Will Attend", "Will attend", R.string.log_attend),
    ATTENDED(10, "10", "Attended", "Attended", R.string.log_attended, R.drawable.mark_green, R.drawable.marker_found_offline),
    RETRIEVED_IT(13, "13", "Retrieved it", "", R.string.log_retrieved, R.drawable.mark_green_more),
    PLACED_IT(14, "14", "placed it", "", R.string.log_placed, R.drawable.mark_green_more),
    GRABBED_IT(19, "19", "grabbed it", "", R.string.log_grabbed, R.drawable.mark_green_more),
    NEEDS_MAINTENANCE(45, "45", "Needs Maintenance", "Needs maintenance", R.string.log_maintenance_needed, R.drawable.mark_red, R.drawable.marker_maintenance),
    OWNER_MAINTENANCE(46, "46", "Owner Maintenance", "", R.string.log_maintained, R.drawable.mark_green_more, R.drawable.marker_owner_maintenance),
    UPDATE_COORDINATES(47, "47", "Update Coordinates", "Moved", R.string.log_update, R.drawable.marker_owner_maintenance),
    DISCOVERED_IT(48, "48", "Discovered It", "", R.string.log_discovered, R.drawable.mark_green),
    POST_REVIEWER_NOTE(18, "18", "Post Reviewer Note", "", R.string.log_reviewer),
    SUBMIT_FOR_REVIEW(76, "76", "submit for review", "", R.string.log_submit_for_review),
    VISIT(1001, "75", "visit", "", R.string.log_tb_visit, R.drawable.mark_green),
    WEBCAM_PHOTO_TAKEN(11, "11", "Webcam Photo Taken", "", R.string.log_webcam, R.drawable.mark_green, R.drawable.marker_found_offline),
    ANNOUNCEMENT(74, "74", "Announcement", "", R.string.log_announcement),
    MOVE_COLLECTION(69, "69", "unused_collection", "", R.string.log_movecollection),
    MOVE_INVENTORY(70, "70", "unused_inventory", "", R.string.log_moveinventory),
    RETRACT(25, "25", "Retract Listing", "", R.string.log_retractlisting),
    MARKED_MISSING(16, "16", "marked missing", "", R.string.log_marked_missing, R.drawable.mark_red),
    OC_TEAM_COMMENT(83, null, "X1", "OC Team comment", R.string.log_oc_team_comment),
    UNKNOWN(0, "unknown", "", "", R.string.err_unknown, R.drawable.mark_red); // LogType not initialized yet

    public final int id;
    @Nullable
    public final String iconName;
    @NonNull
    public final String type;
    @NonNull
    public final String ocType;
    private final int stringId;
    public final int markerId;
    /**
     * Drawable ID for a small overlay image for this log type.
     */
    public final int overlayId;

    private static final Map<String, LogType> FIND_BY_ICONNAME = new HashMap<>();
    private static final Map<String, LogType> FIND_BY_TYPE = new HashMap<>();

    LogType(final int id, @Nullable final String iconName, @NonNull final String type, @NonNull final String ocType,
            final int stringId, final int markerId, final int overlayId) {
        this.id = id;
        this.iconName = iconName;
        this.type = type;
        this.ocType = ocType;
        this.stringId = stringId;
        this.markerId = markerId;
        this.overlayId = overlayId;
    }

    LogType(final int id, final String iconName, final String type, final String ocType, final int stringId,
            final int markerId) {
        this(id, iconName, type, ocType, stringId, markerId, 0);
    }

    LogType(final int id, final String iconName, final String type, final String ocType, final int stringId) {
        this(id, iconName, type, ocType, stringId, R.drawable.mark_gray);
    }

    static {
        for (final LogType lt : values()) {
            if (lt.iconName != null) {
                FIND_BY_ICONNAME.put(StringUtils.lowerCase(lt.iconName), lt);
            }
            FIND_BY_TYPE.put(StringUtils.lowerCase(lt.type), lt);
        }
    }

    @NonNull
    public static LogType getById(final int id) {
        for (final LogType logType : values()) {
            if (logType.id == id) {
                return logType;
            }
        }
        return UNKNOWN;
    }

    @NonNull
    public static LogType getByIconName(final String imageType) {
        // Special case for post reviewer note, which appears sometimes as
        // 18.png (in individual entries) or as 68.png
        // (in logs counts).
        if ("68".equals(imageType)) {
            return POST_REVIEWER_NOTE;
        }
        final LogType result = imageType != null ? FIND_BY_ICONNAME.get(imageType.toLowerCase(Locale.US).trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    @NonNull
    public static LogType getByType(final String type) {
        final LogType result = type != null ? FIND_BY_TYPE.get(type.toLowerCase(Locale.US).trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    @NonNull
    public final String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getString(stringId);
    }

    /**
     * Check if the Offline Log is about Archiving.
     *
     * @return True if the Offline LogEntry is about Archiving
     */
    public final boolean isArchiveLog() {
        return this == ARCHIVE || this == NEEDS_ARCHIVE;
    }

    /**
     * Check if the Offline Log is a Found Log.
     *
     * @return True if the Offline LogEntry is a Found
     */
    public final boolean isFoundLog() {
        return this == FOUND_IT || this == ATTENDED || this == WEBCAM_PHOTO_TAKEN;
    }

    /**
     * Check if the LogType is unusual. May lead to user confirmation.
     *
     * @return True if user must confirm Log
     */
    public boolean mustConfirmLog() {
        return isArchiveLog() || this == NEEDS_MAINTENANCE;
    }

    /**
     * get the overlay image ID for showing the offline log type
     */
    public int getOfflineLogOverlay() {
        if (overlayId != 0) {
            return overlayId;
        }
        return R.drawable.marker_unknown_offline;
    }

    /**
     * return the collection of found log type ids, to be used in the parser
     */
    public static Collection<String> foundLogTypes() {
        final ArrayList<String> foundLogTypes = new ArrayList<>();
        for (final LogType logType : LogType.values()) {
            if (logType.isFoundLog()) {
                foundLogTypes.add(String.valueOf(logType.id));
            }
        }
        return foundLogTypes;
    }
}
