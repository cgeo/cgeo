// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.oc

import cgeo.geocaching.R
import cgeo.geocaching.connector.UserAction
import cgeo.geocaching.connector.capability.Smiley
import cgeo.geocaching.connector.capability.SmileyCapability
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.Network
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Arrays
import java.util.List
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils

class OCConnector : OCBaseConnector() : SmileyCapability {

    private static val GPX_ZIP_FILE_PATTERN: Pattern = Pattern.compile("oc[a-z]{2,3}\\d{5,}\\.zip", Pattern.CASE_INSENSITIVE)

    private static val STANDARD_LOG_TYPES: List<LogType> = Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.NOTE)
    private static val EVENT_LOG_TYPES: List<LogType> = Arrays.asList(LogType.WILL_ATTEND, LogType.ATTENDED, LogType.NOTE)

    public OCConnector(final String name, final String host, final Boolean https, final String prefix, final String abbreviation) {
        super(name, host, https, prefix, abbreviation)
    }

    override     public String getCacheLogUrl(final Geocache cache, final LogEntry logEntry) {
        val internalId: String = getServiceSpecificLogId(logEntry.serviceLogId)
        if (StringUtils.isNotBlank(internalId)) {
            return getCacheUrl(cache) + "&log=A#log" + internalId
        }
        return null
    }

    override     public String getServiceSpecificLogId(final String logId) {
        //OC serviceLogId has format: 'log_uuid:internal_id', 'internal_id' may be missing
        //the id usable in other contexts is the internal_id
        if (StringUtils.isBlank(logId)) {
            return null
        }
        val idx: Int = logId.lastIndexOf(":")
        if (idx >= 0) {
            return logId.substring(idx + 1)
        }
        return null; //do not display uuid
    }

    override     public Boolean isZippedGPXFile(final String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches()
    }

    override     public final List<LogType> getPossibleLogTypes(final Geocache cache) {
        if (cache.isEventCache()) {
            return EVENT_LOG_TYPES
        }

        return STANDARD_LOG_TYPES
    }

    override     public String getGeocodeFromText(final String text) {
        // Text containing a Geocode
        return getGeocodeFromUrl(TextUtils.getMatch(text, Pattern.compile("((https?://|)(www.|)opencach[^\\s/$.?#].[^\\s]*)", Pattern.CASE_INSENSITIVE), false, ""))
    }

    override     public String getCreateAccountUrl() {
        return getSchemeAndHost() + "/register.php"
    }

    override     public String geMyAccountUrl() {
        return getSchemeAndHost() + "/myhome.php"
    }

    override     public List<Smiley> getSmileys() {
        return OCSmileysProvider.getSmileys()
    }

    override     public Smiley getSmiley(final Int id) {
        return OCSmileysProvider.getSmiley(id)
    }

    override     public List<UserAction> getUserActions(final UserAction.UAContext user) {
        val actions: List<UserAction> = super.getUserActions(user)
        // caches stored before parsing the UserId will not have the field set, so we must check for correct existence here
        if (NumberUtils.isDigits(user.userName)) {
            actions.add(UserAction(R.string.user_menu_open_browser, R.drawable.ic_menu_face, context -> ShareUtils.openUrl(context.getContext(), getSchemeAndHost() + "/viewprofile.php?userid=" + Network.encode(context.userName))))
            actions.add(UserAction(R.string.user_menu_send_message, R.drawable.ic_menu_message, context -> ShareUtils.openUrl(context.getContext(), getSchemeAndHost() + "/mailto.php?userid=" + Network.encode(context.userName))))
        }
        return actions
    }
}
