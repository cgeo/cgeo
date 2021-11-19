package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Geocache;

import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public final class CheckerUtils {
    private static GeoChecker[] CHECKERS = {
        new GeoChecker("certitudes.org/certitude?"),
        new GeoChecker("gc-apps.com/checker/"),
        new GeoChecker("geocheck.org/geo_inputchkcoord.php?", "&coord=", GeopointFormatter.Format.GEOCHECKORG),
        new GeoChecker("geochecker.com/index.php?", "&lastcoords=", GeopointFormatter.Format.GEOCHECKERCOM),
        new GeoChecker("geochecker.gps-cache.de/check.aspx?"),
        new GeoChecker("geotjek.dk/geo_inputchkcoord.php?", "&coord=", GeopointFormatter.Format.GEOCHECKORG),
        new GeoChecker("geocache-planer.de/CAL/checker.php?")
    };

    private CheckerUtils() {
        // utility class
    }

    @Nullable
    public static String getCheckerUrl(@NonNull final Geocache cache) {
        final String description = cache.getDescription();
        final Matcher matcher = Patterns.WEB_URL.matcher(description);
        while (matcher.find()) {
            String url = matcher.group();
            for (final GeoChecker checker : CHECKERS) {
                if (StringUtils.containsIgnoreCase(url, checker.getUrlPattern())) {
                    if (checker.getCoordinateFormat() != null) {
                        url = url + checker.getUrlCoordinateParam() + cache.getCoords().format(checker.getCoordinateFormat());
                    }
                    return StringEscapeUtils.unescapeHtml4(url);
                }
            }
        }
        // GC's own checker
        if (cache.getDescription().contains(CgeoApplication.getInstance().getString(R.string.link_gc_checker))) {
            return cache.getUrl();
        }
        return null;
    }

    public static class GeoChecker {
        private String urlPattern;
        private String urlCoordinateParam = "";
        private GeopointFormatter.Format coordinateFormat = null;

        public GeoChecker(String urlPattern) {
            this.urlPattern = urlPattern;
        }

        public GeoChecker(String urlPattern, String coordinateParam, GeopointFormatter.Format coordinateFormat) {
            this.urlPattern = urlPattern;
            this.urlCoordinateParam = coordinateParam;
            this.coordinateFormat = coordinateFormat;
        }

        public String getUrlPattern() {
            return urlPattern;
        }

        public String getUrlCoordinateParam() {
            return urlCoordinateParam;
        }

        public GeopointFormatter.Format getCoordinateFormat() {
            return coordinateFormat;
        }
    }
}
