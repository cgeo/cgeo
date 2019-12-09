package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public final class CheckerUtils {
    private static final String[] CHECKERS = {
            "certitudes.org/certitude?",
            "gc-apps.com/checker/",
            "gccounter.com/gcchecker.php?",
            "geocheck.org/geo_inputchkcoord.php?",
            "geochecker.com/index.php?",
            "geochecker.gps-cache.de/check.aspx?",
            "geotjek.dk/geo_inputchkcoord.php?",
            "geocache-planer.de/CAL/checker.php?",
    };

    private CheckerUtils() {
        // utility class
    }

    @Nullable
    public static String getCheckerUrl(@NonNull final Geocache cache) {
        final String description = cache.getDescription();
        final Matcher matcher = Patterns.WEB_URL.matcher(description);
        while (matcher.find()) {
            final String url = matcher.group();
            for (final String checker : CHECKERS) {
                if (StringUtils.containsIgnoreCase(url, checker)) {
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
}
