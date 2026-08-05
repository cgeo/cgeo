package cgeo.geocaching.files.unifiedgpxparser;

import cgeo.geocaching.files.InvalidXMLCharacterFilterReader;
import cgeo.geocaching.files.ParserException;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.xml.XmlUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Unified, single-pass GPX parser based on {@link XmlPullParser}.
 * <p>
 * Reads a GPX file once and collects every supported top-level element type it finds:
 * waypoints ({@code <wpt>}) as {@link Geocache}, routes ({@code <rte>}) as routable
 * {@link Route}s, and tracks ({@code <trk>}) as non-routable {@link Route}s. A single
 * file may contribute results to all three lists at the same time.
 * <p>
 * Namespace-agnostic on element local names — accepts GPX 1.0, GPX 1.1 and files that
 * omit the namespace declaration. Extension dialects (Groundspeak, GSAK, c:geo,
 * OpenCaching, TerraCaching) are understood and mapped onto the corresponding
 * {@link Geocache} fields by {@link UnifiedGPXWaypointParser}.
 * <p>
 * The result is a {@link Result} object exposing the four output collections as
 * separate named fields ({@link Result#waypoints}, {@link Result#routes},
 * {@link Result#tracks}, {@link Result#logsByGeocode}, {@link Result#orphanWaypoints}).
 * <p>
 * Segment mapping:
 * <ul>
 *   <li>each {@code <rte>} produces one {@link Route} with one {@link RouteSegment}
 *       per {@code <rtept>} (each segment holds a single point), matching the layout
 *       expected by {@link Route#calculateNavigationRoute()};</li>
 *   <li>each {@code <trk>} produces one {@link Route} with one {@link RouteSegment}
 *       per {@code <trkseg>}; multiple track segments therefore stay separate
 *       instead of being merged into a single segment. Track parsing lives in
 *       {@link UnifiedGPXTrackParser}.</li>
 * </ul>
 * <p>
 * Child waypoints (parking, stage, final, ...) found in the same file are attached to
 * their parent cache after the file has been fully read; child waypoints whose parent
 * is not in the file end up in {@link Result#orphanWaypoints}. Groundspeak and
 * TerraCaching logs are returned via {@link Result#logsByGeocode}.
 */
public final class UnifiedGPXParser {

    private UnifiedGPXParser() {
        // utility class
    }

    @NonNull
    public static Result parse(@NonNull final InputStream stream) throws IOException, ParserException {
        final Reader reader = new InvalidXMLCharacterFilterReader(
                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        try {
            final XmlPullParser parser = XmlUtils.createParser(stream, true);
            // XmlUtils.createParser sets the stream as input; replace it with our filtered reader
            // so that invalid XML chars are stripped (same behaviour as the SAX-based parsers).
            parser.setInput(reader);

            final Result result = new Result();
            findAndParseGpxRoot(parser, result);
            return result;
        } catch (final XmlPullParserException e) {
            throw new ParserException("Cannot parse .gpx file: invalid XML", e);
        }
    }

    private static void findAndParseGpxRoot(final XmlPullParser parser, final Result result)
            throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "gpx".equals(parser.getName())) {
                parseGpx(parser, result);
                return;
            }
            eventType = parser.next();
        }
    }

    private static void parseGpx(final XmlPullParser parser, final Result result)
            throws XmlPullParserException, IOException {
        final ParseContext ctx = new ParseContext();
        // The GPX creator attribute is the standard way to identify the file's origin;
        // some non-standard files put it inside a <creator> element instead — handled below.
        final String creatorAttr = parser.getAttributeValue(null, "creator");
        if (creatorAttr != null) {
            ctx.scriptUrl = creatorAttr;
        }

        final Map<String, Geocache> cachesByCode = new HashMap<>();
        final List<ChildWaypoint> pendingChildWaypoints = new ArrayList<>();

        final int gpxDepth = parser.getDepth();
        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == gpxDepth) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "wpt":
                    final Parsed parsed = UnifiedGPXWaypointParser.parseWaypoint(parser, ctx);
                    handleParsedWpt(parsed, result, cachesByCode, pendingChildWaypoints);
                    break;
                case "rte":
                    result.routes.add(parseRoute(parser));
                    break;
                case "trk":
                    result.tracks.add(UnifiedGPXTrackParser.parseTrack(parser));
                    break;
                case "url":
                    // GPX 1.0: <gpx><url>...</url></gpx> — used by Groundspeak PQs
                    final String urlText = readText(parser);
                    if (urlText != null) {
                        ctx.scriptUrl = urlText;
                    }
                    break;
                case "metadata":
                    // GPX 1.1: <gpx><metadata><link href="..."/></metadata>
                    parseMetadataForScriptUrl(parser, ctx);
                    break;
                case "creator":
                    // non-standard files put the creator as element text
                    final String creator = readText(parser);
                    if (creator != null) {
                        ctx.scriptUrl = creator;
                    }
                    break;
                default:
                    skipSubtree(parser);
                    break;
            }
        }

        attachChildWaypoints(pendingChildWaypoints, cachesByCode, result);
    }

    private static void handleParsedWpt(
            @NonNull final Parsed parsed,
            @NonNull final Result result,
            @NonNull final Map<String, Geocache> cachesByCode,
            @NonNull final List<ChildWaypoint> pendingChildWaypoints) {
        if (parsed.cache != null) {
            result.waypoints.add(parsed.cache);
            final String code = parsed.cache.getGeocode();
            if (StringUtils.isNotBlank(code)) {
                cachesByCode.put(code, parsed.cache);
                if (!parsed.logs.isEmpty()) {
                    result.logsByGeocode.put(code, parsed.logs);
                }
            }
        } else if (parsed.childWaypoint != null) {
            pendingChildWaypoints.add(parsed.childWaypoint);
        }
    }

    private static void parseMetadataForScriptUrl(final XmlPullParser parser,
                                                  final ParseContext ctx)
            throws XmlPullParserException, IOException {
        final int startDepth = parser.getDepth();
        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            if ("link".equals(parser.getName())) {
                final String href = parser.getAttributeValue(null, "href");
                if (href != null) {
                    ctx.scriptUrl = href;
                }
                skipSubtree(parser);
            } else {
                skipSubtree(parser);
            }
        }
    }

    private static void attachChildWaypoints(
            @NonNull final List<ChildWaypoint> pendingChildWaypoints,
            @NonNull final Map<String, Geocache> cachesByCode,
            @NonNull final Result result) {
        for (final ChildWaypoint cw : pendingChildWaypoints) {
            final Geocache parent = cachesByCode.get(cw.parentGeocode);
            if (parent == null) {
                result.orphanWaypoints.add(new OrphanWaypoint(cw.waypoint, cw.parentGeocode));
                continue;
            }

            // Existing parser tweaks the name for user-defined waypoints before deriving the prefix
            String nameForPrefix = cw.wptName;
            if (cw.userDefined && cw.parentGeocode.length() >= 2) {
                final String tail = cw.parentGeocode.substring(2);
                if (Strings.CI.endsWith(nameForPrefix, tail)) {
                    nameForPrefix = nameForPrefix.substring(
                            0, nameForPrefix.length() - cw.parentGeocode.length() + 2);
                }
                if (Strings.CI.startsWith(nameForPrefix, Waypoint.PREFIX_OWN + "-")) {
                    nameForPrefix = nameForPrefix.substring(4);
                }
            }
            try {
                cw.waypoint.setPrefix(parent.getWaypointPrefix(nameForPrefix));
            } catch (final Throwable t) {
                // getWaypointPrefix delegates through the connector — fall back to OWN
                // if no connector is available (e.g. in a JVM test context).
                Log.w("UnifiedGPXParser: could not derive waypoint prefix: " + t.getMessage());
                cw.waypoint.setPrefix(Waypoint.PREFIX_OWN);
            }
            if (cw.markParentUserModifiedCoords) {
                parent.setUserModifiedCoords(true);
            }

            final List<Waypoint> merged = new ArrayList<>(parent.getWaypoints());
            final List<Waypoint> newPoints = new ArrayList<>();
            newPoints.add(cw.waypoint);
            Waypoint.mergeWayPoints(newPoints, merged, true);
            parent.setWaypoints(newPoints);
        }
    }

    private static Route parseRoute(final XmlPullParser parser) throws XmlPullParserException, IOException {
        final int startDepth = parser.getDepth();
        final Route route = new Route(true);

        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "name":
                    final String routeName = readText(parser);
                    if (routeName != null) {
                        route.setName(routeName);
                    }
                    break;
                case "rtept":
                    final RouteSegment segment = parseRoutePoint(parser);
                    if (segment != null) {
                        route.add(segment);
                    }
                    break;
                default:
                    skipSubtree(parser);
                    break;
            }
        }
        return route;
    }

    @Nullable
    private static RouteSegment parseRoutePoint(final XmlPullParser parser)
            throws XmlPullParserException, IOException {
        final Geopoint coords = readLatLon(parser);
        final int startDepth = parser.getDepth();
        String name = null;

        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            if ("name".equals(parser.getName())) {
                name = readText(parser);
            } else {
                skipSubtree(parser);
            }
        }

        if (coords == null) {
            return null;
        }
        final ArrayList<Geopoint> points = new ArrayList<>();
        points.add(coords);
        return new RouteSegment(new RouteItem(name == null ? "" : name, coords), points, true);
    }

    @Nullable
    static Geopoint readLatLon(final XmlPullParser parser) {
        final String lat = parser.getAttributeValue(null, "lat");
        final String lon = parser.getAttributeValue(null, "lon");
        if (lat == null || lon == null || lat.isEmpty() || lon.isEmpty()) {
            return null;
        }
        try {
            return new Geopoint(Double.parseDouble(lat), Double.parseDouble(lon));
        } catch (final NumberFormatException e) {
            Log.w("UnifiedGPXParser: invalid coordinates lat='" + lat + "' lon='" + lon + "'");
            return null;
        }
    }

    /**
     * Read the text content of the current START_TAG and consume up to (and including) its END_TAG.
     * Returns {@code null} for elements without text content.
     */
    @Nullable
    static String readText(final XmlPullParser parser) throws XmlPullParserException, IOException {
        final int startDepth = parser.getDepth();
        final StringBuilder sb = new StringBuilder();
        boolean any = false;
        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) {
                break;
            }
            if (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT) {
                sb.append(parser.getText());
                any = true;
            } else if (event == XmlPullParser.START_TAG) {
                // ignore nested elements but still consume them
                skipSubtree(parser);
            }
        }
        return any ? sb.toString() : null;
    }

    /**
     * The parser is currently positioned on a START_TAG. Advance past the matching END_TAG so the
     * caller can continue with the next sibling.
     */
    static void skipSubtree(final XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            return;
        }
        final int startDepth = parser.getDepth();
        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                return;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) {
                return;
            }
        }
    }
}
