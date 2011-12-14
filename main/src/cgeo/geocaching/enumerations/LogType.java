package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;



/**
 * Different log types
 *
 * @author blafoo
 */
public enum LogType {

    LOG_FOUND_IT(2,"icon_smile","found it",R.string.log_found),
    LOG_DIDNT_FIND_IT(3, "icon_sad", "didn't find it", R.string.log_dnf),
    LOG_NOTE(4,"icon_note","write note",R.string.log_note),
    LOG_PUBLISH_LISTING(1003,"icon_greenlight","publish listing",R.string.log_published), // unknown ID), used number doesn't match any GC.com's ID
    LOG_ENABLE_LISTING(23,"icon_enabled","enable listing",R.string.log_enabled),
    LOG_ARCHIVE(5,"traffic_cone","archive",R.string.log_archived),
    LOG_TEMP_DISABLE_LISTING(22,"icon_disabled","temporarily disable listing",R.string.log_disabled),
    LOG_NEEDS_ARCHIVE(7,"icon_remove","needs archived",R.string.log_needs_archived),
    LOG_WILL_ATTEND(9,"icon_rsvp","will attend",R.string.log_attend),
    LOG_ATTENDED(10,"icon_attended","attended",R.string.log_attended),
    LOG_RETRIEVED_IT(13,"picked_up","retrieved it",R.string.log_retrieved),
    LOG_PLACED_IT(14,"dropped_off","placed it",R.string.log_placed),
    LOG_GRABBED_IT(19,"transfer","grabbed it",R.string.log_grabbed),
    LOG_NEEDS_MAINTENANCE(45,"icon_needsmaint","needs maintenance",R.string.log_maintenance_needed),
    LOG_OWNER_MAINTENANCE(46,"icon_maint","owner maintenance",R.string.log_maintained),
    LOG_UPDATE_COORDINATES(47,"coord_update","update coordinates",R.string.log_update),
    LOG_DISCOVERED_IT(48,"icon_discovered","discovered it",R.string.log_discovered),
    LOG_POST_REVIEWER_NOTE(18,"big_smile","post reviewer note",R.string.log_reviewed),
    LOG_VISIT(1001,"icon_visited","visit",R.string.log_reviewed), // unknown ID), used number doesn't match any GC.com's ID
    LOG_WEBCAM_PHOTO_TAKEN(11,"icon_camera","webcam photo taken",R.string.log_webcam), // unknown ID; used number doesn't match any GC.com's ID
    LOG_ANNOUNCEMENT(74, "icon_announcement", "announcement", R.string.log_announcement), // unknown ID; used number doesn't match any GC.com's ID
    LOG_UNKNOWN(0, "unknown", "", R.string.err_unknown); // LogType not init. yet

    public final int id;
    private final String iconName;
    private final String type;
    private final int stringId;
    private String l10n; // not final because the locale can be changed

    private LogType(int id, String iconName, String type, int stringId) {
        this.id = id;
        this.iconName = iconName;
        this.type = type;
        this.stringId = stringId;
        setL10n();
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

    public final static LogType getById(final int id) {
        for (LogType logType : values()) {
            if (logType.id == id) {
                return logType;
            }
        }
        return LOG_UNKNOWN;
    }

    public static LogType getByIconName(final String imageType) {
        final LogType result = imageType != null ? LogType.FIND_BY_ICONNAME.get(imageType.toLowerCase().trim()) : null;
        if (result == null) {
            return LOG_UNKNOWN;
        }
        return result;
    }

    public static LogType getByType(final String type) {
        final LogType result = type != null ? LogType.FIND_BY_TYPE.get(type.toLowerCase().trim()) : null;
        if (result == null) {
            return LOG_UNKNOWN;
        }
        return result;
    }

    public final String getL10n() {
        return l10n;
    }

    public void setL10n() {
        this.l10n = cgeoapplication.getInstance().getBaseContext().getResources().getString(this.stringId);
    }

}

