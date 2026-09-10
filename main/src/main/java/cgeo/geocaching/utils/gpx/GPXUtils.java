package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Utilities around parsing GPX files.
 */
public final class GPXUtils {

    /** Groundspeak Pocket Query convention: additional waypoints for the caches in "X.gpx" are in "X-wpts.gpx". */
    private static final String WAYPOINTS_FILE_SUFFIX_AND_EXTENSION = "-wpts.gpx";
    private static final String GPX_EXTENSION = ".gpx";
    private static final String DEFAULT_ENTRY_NAME_ENCODING = "UTF-8";

    private GPXUtils() {
        // utility class
    }

    public static int parseZip(@NonNull final InputStream zipStream, @NonNull final GPXParser parser, @NonNull final IGPXParseHooks hooks) throws IOException, XmlPullParserException {
        return parseZip(zipStream, parser, hooks, null);
    }

    /**
     * Parses all {@code .gpx} entries contained in the given ZIP stream with {@code parser}, notifying
     * {@code hooks}. ZIP must be read multiple times, because the order of cache and waypoint files is not guaranteed.
     * <br>
     * This method does not close {@code zipStream} (the caller opened it, the caller closes it).
     *
     * @param zipStream         the ZIP archive to read
     * @param parser            the parser to use; reused across all entries
     * @param hooks             callback for parsed business elements
     * @param entryNameEncoding character encoding of entry names inside the ZIP;
     *                          {@code null}/blank falls back to UTF-8.
     * @return number of {@code .gpx} entries found and parsed; if 0, a warning is logged (nothing thrown)
     */
    public static int parseZip(@NonNull final InputStream zipStream, @NonNull final GPXParser parser, @NonNull final IGPXParseHooks hooks,
                                @Nullable final String entryNameEncoding) throws IOException, XmlPullParserException {
        final Map<String, byte[]> gpxEntries = bufferGpxEntries(zipStream, StringUtils.defaultIfBlank(entryNameEncoding, DEFAULT_ENTRY_NAME_ENCODING));

        if (gpxEntries.isEmpty()) {
            Log.w("GPXUtils.parseZip: ZIP contains no ." + GPX_EXTENSION.substring(1) + " entries");
            return 0;
        }

        // pass 1: everything except "-wpts" files (caches and any other GPX content), so their geocodes get
        // indexed (see GPXParser#reset()) before...
        for (final Map.Entry<String, byte[]> entry : gpxEntries.entrySet()) {
            if (!Strings.CI.endsWith(entry.getKey(), WAYPOINTS_FILE_SUFFIX_AND_EXTENSION)) {
                parseBuffered(parser, hooks, entry.getValue());
            }
        }
        // pass 2: ...the "-wpts" files are parsed, regardless of the order they actually appear in the ZIP
        for (final Map.Entry<String, byte[]> entry : gpxEntries.entrySet()) {
            if (Strings.CI.endsWith(entry.getKey(), WAYPOINTS_FILE_SUFFIX_AND_EXTENSION)) {
                parseBuffered(parser, hooks, entry.getValue());
            }
        }

        return gpxEntries.size();
    }

    private static void parseBuffered(final GPXParser parser, final IGPXParseHooks hooks, final byte[] content) throws IOException, XmlPullParserException {
        try (InputStream is = new ByteArrayInputStream(content)) {
            parser.parse(is, hooks);
        }
    }

    /** Reads {@code zipStream} once, returning the content of every {@code .gpx} entry, in encounter order. */
    @NonNull
    private static Map<String, byte[]> bufferGpxEntries(final InputStream zipStream, final String entryNameEncoding) throws IOException {
        final Map<String, byte[]> result = new LinkedHashMap<>();
        final ZipArchiveInputStream zis = new ZipArchiveInputStream(new BufferedInputStream(zipStream), entryNameEncoding);
        for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
            final String name = entry.getName();
            if (Strings.CI.endsWith(name, GPX_EXTENSION)) {
                result.put(name, IOUtils.toByteArray(zis));
            }
        }
        return result;
    }

    @Nullable
    public static String readLinkUrl(@Nullable final XmlNode link) {
        return StringUtils.defaultIfBlank(gpxNodeAttrValue(link, "href", null), gpxNodeChildText(link, "href", null));
    }

    @Nullable
    public static String readWaypointUrl(final XmlNode node) {
        return StringUtils.defaultIfBlank(node.getChildValue("url"), readLinkUrl(node.getChild("link")));
    }

    // ---------------------------------------------------------------------------------------------------
    // date parsing: tolerant of the handful of date formats found in real-world GPX files
    // ---------------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------------
    // XmlNode helpers: namespace-tolerant lookup by local name (D-namespace tolerance, point 34)
    // ---------------------------------------------------------------------------------------------------

    /**
     * For GPX 1.1 files, extension fields live inside a {@code <extensions>} child; for GPX 1.0 files, they are
     * direct children of the {@code wpt}/{@code rtept}/{@code trkpt} element itself. .
     */
    @Nullable
    public static XmlNode extensionsBase(final XmlNode wptNode) {
        final XmlNode extensions = gpxNodeChild(wptNode, "extensions", null);
        return extensions != null ? extensions : wptNode;
    }

    @Nullable
    public static XmlNode gpxNodeChild(final XmlNode node, final String name, final Set<String> namespaces) {
        return node == null ? null : node.getChild(name, namespaces, true);
    }

    @Nullable
    public static String gpxNodeChildText(final XmlNode node, final String name, final Set<String> namespaces) {
        final XmlNode c = gpxNodeChild(node, name, namespaces);
        // A present, empty element must be able to clear an earlier fallback value.
        return c == null ? null : StringUtils.defaultString(c.getValue());
    }

    @Nullable
    public static Date gpxNodeChildDate(final XmlNode node, final String name, final Set<String> namespaces, final Date defaultValue) {
        final XmlNode c = gpxNodeChild(node, name, namespaces);
        // A present, empty element must be able to clear an earlier fallback value.
        return c == null ? null : XmlUtils.toDate(c.getValue(), defaultValue);
    }

    @Nullable
    public static Float gpxNodeChildFloat(final XmlNode node, final String name, final Set<String> namespaces, final Float defaultValue) {
        final XmlNode c = gpxNodeChild(node, name, namespaces);
        // A present, empty element must be able to clear an earlier fallback value.
        return c == null ? null : XmlUtils.toFloat(c.getValue(), defaultValue);
    }

    @Nullable
    public static Integer gpxNodeChildInt(final XmlNode node, final String name, final Set<String> namespaces, final Integer defaultValue) {
        final XmlNode c = gpxNodeChild(node, name, namespaces);
        // A present, empty element must be able to clear an earlier fallback value.
        return c == null ? null : XmlUtils.toInteger(c.getValue(), defaultValue);
    }

    @Nullable
    public static Boolean gpxNodeChildBoolean(final XmlNode node, final String name, final Set<String> namespaces, final Boolean defaultValue) {
        final XmlNode c = gpxNodeChild(node, name, namespaces);
        // A present, empty element must be able to clear an earlier fallback value.
        return c == null || c.getValue() == null ? null : XmlUtils.toBoolean(c.getValue(), defaultValue);
    }

    /** Like {@link #gpxNodeChild}, but returns ALL matching children (namespace-tolerant, by local name), not just the first. */
    @Nullable
    public static List<XmlNode> gpxNodeChildren(final XmlNode node, final String name) {
        return node == null ? null : node.getChildrenAsList(name);
    }

    @Nullable
    public static String gpxNodeAttrValue(final XmlNode node, final String attributeName, final Set<String> namespaces) {
        return gpxNodeChildText(node, XmlNode.ATTRIBUTE_PRAEFIX + attributeName, namespaces);
    }

    @Nullable
    public static Geopoint gpxNodeReadLatLon(final XmlNode node) {
        final String lat = gpxNodeAttrValue(node, "lat", null);
        final String lon = gpxNodeAttrValue(node, "lon", null);
        return XmlUtils.parseGeopoint(lat, lon, false);
    }

    @Nullable
    public static String attr(final XmlPullParser parser, final String name) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (name.equals(localName(parser.getAttributeName(i)))) {
                return parser.getAttributeValue(i);
            }
        }
        return null;
    }

    @Nullable
    public static String localName(final String raw) {
        return XmlUtils.getLocalName(raw);
    }

}

