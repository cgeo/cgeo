package cgeo.geocaching.unifiedmap.tiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * The text form of a single configured tile source, for passing one to somebody else through a
 * chat message.
 *
 * <p>Deliberately plain text rather than json: several chat clients replace straight quotes with
 * typographic ones, which would silently break json at the receiving end, and a plain line stays
 * readable to whoever has to decide whether to trust the link.</p>
 */
public final class TileUrlShare {

    private static final char SEPARATOR = ';';

    private TileUrlShare() {
        //no instance
    }

    /** the shareable form of an entry: {@code name;uri}, or just the uri if it has no name */
    @NonNull
    public static String format(@NonNull final String name, @NonNull final String url) {
        return StringUtils.isBlank(name) ? url : name.trim() + SEPARATOR + url;
    }

    /**
     * Reads a shared entry back.
     *
     * <p>The split happens at the start of the uri, not at the separator, so that neither side has
     * to avoid a ';'. A name may contain one, and so may a url - it is a legal query sub-delimiter,
     * as in {@code https://example.com/{z}/{x}/{y}.png?a=1;b=2}. Splitting on the separator would
     * mangle both cases; looking for the scheme instead needs no escaping at all.</p>
     *
     * @return the name (possibly empty) and the normalised uri, or {@code null} if there is no
     *         usable uri in the text
     */
    @Nullable
    public static String[] parse(@Nullable final String text) {
        final String trimmed = StringUtils.trimToNull(text);
        if (trimmed == null) {
            return null;
        }

        final int uriStart = indexOfScheme(trimmed);
        if (uriStart < 0) {
            return null;
        }

        final String url = TileUrlUtils.normalize(trimmed.substring(uriStart));
        if (url == null) {
            return null;
        }
        final String name = StringUtils.stripEnd(trimmed.substring(0, uriStart).trim(), String.valueOf(SEPARATOR)).trim();
        return new String[]{name, url};
    }

    /** position of the first {@code http://} or {@code https://} in the text, or -1 */
    private static int indexOfScheme(@NonNull final String text) {
        final String lower = text.toLowerCase(Locale.US);
        final int http = lower.indexOf("http://");
        final int https = lower.indexOf("https://");
        if (http < 0) {
            return https;
        }
        return https < 0 ? http : Math.min(http, https);
    }

}
