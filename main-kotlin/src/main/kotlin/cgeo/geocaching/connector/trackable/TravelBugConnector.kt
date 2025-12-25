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

package cgeo.geocaching.connector.trackable

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.connector.UserAction
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCParser
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.settings.Settings

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

class TravelBugConnector : AbstractTrackableConnector() {

    /**
     * TB codes really start with TB1, there is no padding or minimum length
     */
    private static val PATTERN_TB_CODE: Pattern = Pattern.compile("(TB[0-9A-Z&&[^ILOSU]]+)|([0-9A-Z]{6})|([0-9]{3}[A-Z]{6})|([0-9]{5})", Pattern.CASE_INSENSITIVE)

    override     public Int getPreferenceActivity() {
        return R.string.preference_screen_gc
    }

    override     public Boolean canHandleTrackable(final String geocode) {
        if (geocode == null) {
            return false
        }
        return PATTERN_TB_CODE.matcher(geocode).matches()
    }

    override     public String getServiceTitle() {
        return CgeoApplication.getInstance().getString(R.string.settings_title_gc)
    }

    override     public String getUrl(final Trackable trackable) {
        return getHostUrl() + "/track/details.aspx?tracker=" + trackable.getGeocode()
    }

    override     public String getLogUrl(final LogEntry logEntry) {
        if (StringUtils.isNotBlank(logEntry.serviceLogId)) {
            return "https://www.geocaching.com/live/log/" + logEntry.serviceLogId
        }
        return null
    }

    override     public Boolean isLoggable() {
        return true
    }

    override     public Boolean isRegistered() {
        return Settings.hasGCCredentials()
    }

    override     public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        return GCParser.searchTrackable(geocode, guid, id)
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static val INSTANCE: TravelBugConnector = TravelBugConnector()
    }

    private TravelBugConnector() {
        // singleton
    }

    public static TravelBugConnector getInstance() {
        return Holder.INSTANCE
    }

    override     public String getTrackableCodeFromUrl(final String url) {
        // coord.info URLs
        val code1: String = StringUtils.substringAfterLast(url, "coord.info/")
        if (canHandleTrackable(code1)) {
            return code1
        }
        val code2: String = StringUtils.substringAfterLast(url, "?tracker=")
        if (canHandleTrackable(code2)) {
            return code2
        }
        return null
    }

    override     public List<UserAction> getUserActions(final UserAction.UAContext user) {
        // travel bugs should have the same actions as GC caches
        return GCConnector.getInstance().getUserActions(user)
    }

    override     public TrackableBrand getBrand() {
        return TrackableBrand.TRAVELBUG
    }

    override     public TravelBugLoggingManager getTrackableLoggingManager(final String tbCode) {
        return TravelBugLoggingManager(tbCode)
    }

    override     public String getHost() {
        return "www.geocaching.com"
    }

    override     public String getTestUrl() {
        return "https://" + getHost() + "/play"
    }
}
