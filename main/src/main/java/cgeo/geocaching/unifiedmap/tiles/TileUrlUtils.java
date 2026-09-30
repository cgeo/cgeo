package cgeo.geocaching.unifiedmap.tiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Validation and normalisation of the XYZ tile url templates that the user can configure,
 * both for tile overlays and for user-defined tile providers.
 *
 * <p>A template is accepted when it is an http(s) url containing the placeholders for zoom level,
 * tile column and tile row. Placeholders are recognised case-insensitively and are normalised to
 * the upper case spelling used throughout the tile provider classes, so that both
 * {@code https://example.com/{z}/{x}/{y}.png} and {@code https://example.com/{Z}/{X}/{Y}.png}
 * describe the same tile source. Query parameters are preserved verbatim, which is what api keys
 * usually rely on.</p>
 */
public final class TileUrlUtils {

    /** appended to a url that carries no placeholders, as the user-defined tile providers do */
    public static final String DEFAULT_TILE_PATH = "/{Z}/{X}/{Y}.png";

    /**
     * Matches {x}, {X}, {y}, ... including the surrounding braces.
     * Both braces must be escaped: Android uses ICU for regex, which rejects a lone closing
     * brace that the JVM used for unit tests still accepts as a literal.
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([xXyYzZ])\\}");

    private TileUrlUtils() {
        //no instance
    }

    /**
     * Normalises a user-entered url.
     *
     * <p>The placeholders are optional: a url without them gets the default tile path appended
     * when it is used, which is how user-defined tile providers have always worked. Use
     * {@link #withDefaultTilePath} to get the form that actually addresses a tile.</p>
     *
     * @return the normalised url, or {@code null} if it is not an http(s) url with a host
     */
    @Nullable
    public static String normalize(@Nullable final String url) {
        final String trimmed = StringUtils.trimToNull(url);
        if (trimmed == null) {
            return null;
        }

        if (!isHttpUrl(trimmed)) {
            return null;
        }

        final StringBuffer sb = new StringBuffer(trimmed.length());
        final Matcher matcher = PLACEHOLDER.matcher(trimmed);
        boolean hasX = false;
        boolean hasY = false;
        boolean hasZ = false;
        while (matcher.find()) {
            final char placeholder = Character.toUpperCase(matcher.group(1).charAt(0));
            switch (placeholder) {
                case 'X':
                    hasX = true;
                    break;
                case 'Y':
                    hasY = true;
                    break;
                default:
                    hasZ = true;
                    break;
            }
            matcher.appendReplacement(sb, "{" + placeholder + "}");
        }
        matcher.appendTail(sb);

        final String normalized = sb.toString();
        if (hasX && hasY && hasZ) {
            return normalized;
        }
        // no placeholders: still usable, as long as there is a host to append the tile path to
        try {
            return StringUtils.isBlank(new URL(normalized).getHost()) ? null : normalized;
        } catch (final MalformedURLException e) {
            return null;
        }
    }

    /** whether the given url can be used as a tile source */
    public static boolean isValid(@Nullable final String url) {
        return normalize(url) != null;
    }

    /**
     * The template a url actually resolves to, appending the default tile path when the url leaves
     * the placeholders out - mirroring what the user-defined tile providers do with such a url.
     */
    @NonNull
    public static String withDefaultTilePath(@NonNull final String url) {
        if (PLACEHOLDER.matcher(url).find()) {
            return url;
        }
        return (url.endsWith("/") ? url.substring(0, url.length() - 1) : url) + DEFAULT_TILE_PATH;
    }

    /**
     * Splits a normalised template into the part mapsforge and VTM need separately: the base url
     * ({@code scheme://host[:port]}) and the remaining path including query parameters.
     *
     * @return a two element array of base url and tile path, or {@code null} if the template is unusable
     */
    @Nullable
    public static String[] split(@Nullable final String url) {
        final String normalized = normalize(url);
        if (normalized == null) {
            return null;
        }
        final int schemeEnd = normalized.indexOf("://") + 3;
        final int pathStart = normalized.indexOf('/', schemeEnd);
        if (pathStart < 0) {
            return null;
        }
        return new String[]{normalized.substring(0, pathStart), normalized.substring(pathStart)};
    }

    private static boolean isHttpUrl(@NonNull final String url) {
        final String lower = url.toLowerCase(Locale.US);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /**
     * The host of a url template, used as display name for entries the user did not name.
     *
     * @return the host, or {@code null} if the template is unusable or carries no host
     */
    @Nullable
    public static String host(@Nullable final String url) {
        final String normalized = normalize(url);
        if (normalized == null) {
            return null;
        }
        try {
            return StringUtils.trimToNull(new URL(normalized).getHost());
        } catch (final MalformedURLException e) {
            return null;
        }
    }

    /** resolves the placeholders of a normalised template for a concrete tile */
    @NonNull
    public static String forTile(@NonNull final String template, final int zoomLevel, final int tileX, final int tileY) {
        return template
                .replace("{Z}", String.valueOf(zoomLevel))
                .replace("{X}", String.valueOf(tileX))
                .replace("{Y}", String.valueOf(tileY));
    }
}
