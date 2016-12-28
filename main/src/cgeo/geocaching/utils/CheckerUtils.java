package cgeo.geocaching.utils;

import cgeo.geocaching.models.Geocache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Patterns;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public final class CheckerUtils {
    private static final String[] CHECKERS = { "geocheck.org/geo_inputchkcoord.php?", "geochecker.com/index.php?", "certitudes.org/certitude?", "geochecker.gps-cache.de/check.aspx?", "gc-apps.com/geochecker" };

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
        return null;
    }
}
