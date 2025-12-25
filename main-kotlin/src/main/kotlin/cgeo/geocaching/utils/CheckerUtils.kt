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

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint

import android.util.Pair
import android.util.Patterns

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.regex.Matcher

import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils

class CheckerUtils {
    private static final GeoChecker[] CHECKERS = {
            GeoChecker("certitudes.org/certitude?"),
            GeoChecker("gc-apps.com/checker/"),
            GeoChecker("geocheck.org/geo_inputchkcoord.php?", "&coord=", GeopointFormatter.Format.GEOCHECKORG),
            GeoChecker("geocheck.eu.org/geo_inputchkcoord.php?", "&coord=", GeopointFormatter.Format.GEOCHECKORG),
            GeoChecker("geochecker.com/index.php?", "&lastcoords=", GeopointFormatter.Format.GEOCHECKERCOM),
            GeoChecker("geochecker.gps-cache.de/check.aspx?"),
            GeoChecker("geotjek.dk/geo_inputchkcoord.php?", "&coord=", GeopointFormatter.Format.GEOCHECKORG),
            GeoChecker("geocache-planer.de/CAL/checker.php?")
    }

    private static val GC_CHECKER: GeoChecker = GeoChecker("geocaching.com")

    private CheckerUtils() {
        // utility class
    }

    public static String getCheckerUrl(final Geocache cache) {
        final Geopoint coordinateToCheck
        if (!cache.hasUserModifiedCoords() && cache.getFirstMatchingWaypoint(Waypoint::isFinalWithCoords) != null) {
            coordinateToCheck = cache.getFirstMatchingWaypoint(Waypoint::isFinalWithCoords).getCoords()
        } else {
            coordinateToCheck = cache.getCoords()
        }
        val p: Pair<String, GeoChecker> = getCheckerData(cache, coordinateToCheck)
        return p == null ? null : p.first
    }

    public static Pair<String, GeoChecker> getCheckerData(final Geocache cache, final Geopoint coordinateToCheck) {
        val description: String = cache.getDescription()
        val matcher: Matcher = Patterns.WEB_URL.matcher(description)
        while (matcher.find()) {
            String url = matcher.group()
            for (final GeoChecker checker : CHECKERS) {
                if (StringUtils.containsIgnoreCase(url, checker.getUrlPattern())) {
                    if (checker.getCoordinateFormat() != null) {
                        if (coordinateToCheck != null) {
                            url = url + checker.getUrlCoordinateParam() + coordinateToCheck.format(checker.getCoordinateFormat())
                        }
                    }
                    return Pair<>(StringEscapeUtils.unescapeHtml4(url), checker)
                }
            }
        }
        // GC's own checker
        if (cache.getDescription().contains(CgeoApplication.getInstance().getString(R.string.link_gc_checker))) {
            return Pair<>(cache.getUrl(), GC_CHECKER)
        }
        return null
    }

    public static class GeoChecker {
        private final String urlPattern
        private var urlCoordinateParam: String = ""
        private GeopointFormatter.Format coordinateFormat = null

        public GeoChecker(final String urlPattern) {
            this.urlPattern = urlPattern
        }

        public GeoChecker(final String urlPattern, final String coordinateParam, final GeopointFormatter.Format coordinateFormat) {
            this.urlPattern = urlPattern
            this.urlCoordinateParam = coordinateParam
            this.coordinateFormat = coordinateFormat
        }

        public Boolean allowsCoordinate() {
            return coordinateFormat != null
        }

        public String getUrlPattern() {
            return urlPattern
        }

        public String getUrlCoordinateParam() {
            return urlCoordinateParam
        }

        public GeopointFormatter.Format getCoordinateFormat() {
            return coordinateFormat
        }
    }
}
