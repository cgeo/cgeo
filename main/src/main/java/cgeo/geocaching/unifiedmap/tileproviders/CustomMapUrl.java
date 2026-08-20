package cgeo.geocaching.unifiedmap.tileproviders;

import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

public final class CustomMapUrl {

    private static final Pattern BASE_URL_PATTERN = Pattern.compile("(?i)^(https?://[^/?#]+)(/[^#]*)$");

    private CustomMapUrl() {
        // utility class
    }

    public static String normalizeTemplate(final String template) {
        if (template == null) {
            return StringUtils.EMPTY;
        }
        String normalized = template.trim();
        normalized = Strings.CI.replace(normalized, "{z}", "{Z}");
        normalized = Strings.CI.replace(normalized, "{x}", "{X}");
        return Strings.CI.replace(normalized, "{y}", "{Y}");
    }

    public static String formatTileUrl(final String template, final int zoom, final int tileX, final int tileY) {
        return normalizeTemplate(template)
                .replace("{Z}", String.valueOf(zoom))
                .replace("{X}", String.valueOf(tileX))
                .replace("{Y}", String.valueOf(tileY));
    }

    public static String migrateLegacyTemplate(final String template) {
        final String normalized = normalizeTemplate(template);
        if (isValidTemplate(normalized) || normalized.contains("{X}") || normalized.contains("{Y}") || normalized.contains("{Z}")) {
            return normalized;
        }
        final int queryStart = normalized.indexOf('?');
        String baseUrl = queryStart < 0 ? normalized : normalized.substring(0, queryStart);
        final String query = queryStart < 0 ? StringUtils.EMPTY : normalized.substring(queryStart);
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        final String migrated = baseUrl + "{Z}/{X}/{Y}.png" + query;
        return isValidTemplate(migrated) ? migrated : normalized;
    }

    public static boolean isValidTemplate(final String template) {
        final String normalized = normalizeTemplate(template);
        if (!(normalized.contains("{Z}") && normalized.contains("{X}") && normalized.contains("{Y}"))) {
            return false;
        }
        if (!BASE_URL_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        try {
            final URI uri = new URI(formatTileUrl(normalized, 0, 0, 0));
            final String protocol = StringUtils.defaultString(uri.getScheme()).toLowerCase(Locale.US);
            return ("http".equals(protocol) || "https".equals(protocol)) && StringUtils.isNotBlank(uri.getHost());
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    @Nullable
    public static String getBaseUrl(final String template) {
        final Matcher matcher = BASE_URL_PATTERN.matcher(normalizeTemplate(template));
        return matcher.matches() ? matcher.group(1) : null;
    }

    @Nullable
    public static String getTilePath(final String template) {
        final Matcher matcher = BASE_URL_PATTERN.matcher(normalizeTemplate(template));
        return matcher.matches() ? matcher.group(2) : null;
    }
}
