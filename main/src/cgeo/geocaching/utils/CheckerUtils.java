package cgeo.geocaching.utils;

import cgeo.geocaching.Geocache;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.util.Patterns;

import java.util.regex.Matcher;

public final class CheckerUtils {
    private static final String[] CHECKERS = { "geocheck.org", "geochecker.com", "certitudes.org" };

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
