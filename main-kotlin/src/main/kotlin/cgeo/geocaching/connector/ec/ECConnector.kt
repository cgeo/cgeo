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

package cgeo.geocaching.connector.ec

import cgeo.geocaching.R
import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.List
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

class ECConnector : AbstractConnector() {

    private static val CACHE_URL: String = "https://extremcaching.com/index.php/output-2/"

    /**
     * Pattern for EC codes
     */
    private static val PATTERN_EC_CODE: Pattern = Pattern.compile("EC[0-9]+", Pattern.CASE_INSENSITIVE)

    private ECConnector() {
        // singleton
        prefKey = R.string.preference_screen_ec
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static val INSTANCE: ECConnector = ECConnector()
    }

    public static ECConnector getInstance() {
        return Holder.INSTANCE
    }

    override     public Boolean canHandle(final String geocode) {
        return PATTERN_EC_CODE.matcher(geocode).matches()
    }

    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{"EC%"}
    }


    override     public String getCacheUrl(final Geocache cache) {
        return CACHE_URL + cache.getGeocode().replace("EC", "")
    }

    override     public String getName() {
        return "extremcaching.com"
    }

    override     public String getNameAbbreviated() {
        return "EC"
    }

    override     public String getHost() {
        return "extremcaching.com"
    }

    override     public Boolean isOwner(final Geocache cache) {
        return false
    }

    override     protected String getCacheUrlPrefix() {
        return CACHE_URL
    }

    override     public Boolean isActive() {
        return Settings.isECConnectorActive()
    }

    override     public Int getCacheMapMarkerId() {
        val icons: String = Settings.getECIconSet()
        if (StringUtils == (icons, "1")) {
            return R.drawable.marker_other
        }
        return R.drawable.marker_oc
    }

    override     public Int getCacheMapMarkerBackgroundId() {
        val icons: String = Settings.getECIconSet()
        if (StringUtils == (icons, "1")) {
            return R.drawable.background_other
        }
        return R.drawable.background_oc
    }

    override     public Int getCacheMapDotMarkerId() {
        val icons: String = Settings.getECIconSet()
        if (StringUtils == (icons, "1")) {
            return R.drawable.dot_marker_other
        }
        return R.drawable.dot_marker_oc
    }

    override     public Int getCacheMapDotMarkerBackgroundId() {
        val icons: String = Settings.getECIconSet()
        if (StringUtils == (icons, "1")) {
            return R.drawable.dot_background_other
        }
        return R.drawable.dot_background_oc
    }

    override     public String getLicenseText(final Geocache cache) {
        // NOT TO BE TRANSLATED
        return "© " + cache.getOwnerDisplayName() + ", <a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a>, CC BY-NC-ND 3.0, alle Logeinträge © jeweiliger Autor"
    }

    override     public List<LogType> getPossibleLogTypes(final Geocache geocache) {
        val logTypes: List<LogType> = ArrayList<>()
        if (geocache.isEventCache()) {
            logTypes.add(LogType.WILL_ATTEND)
            logTypes.add(LogType.ATTENDED)
        } else {
            logTypes.add(LogType.FOUND_IT)
        }
        if (!geocache.isEventCache()) {
            logTypes.add(LogType.DIDNT_FIND_IT)
        }
        logTypes.add(LogType.NOTE)
        return logTypes
    }

    override     public Int getMaxTerrain() {
        return 7
    }

    override     public String getGeocodeFromUrl(final String url) {
        val geocode: String = "EC" + StringUtils.substringAfter(url, "extremcaching.com/index.php/output-2/")
        if (canHandle(geocode)) {
            return geocode
        }
        return super.getGeocodeFromUrl(url)
    }
}
