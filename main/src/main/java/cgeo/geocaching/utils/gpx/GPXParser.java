package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.connector.tc.TerraCachingLogType;
import cgeo.geocaching.connector.tc.TerraCachingType;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.NamedGeoCoordinate;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.EmojiUtilsLegacyMigration;
import cgeo.geocaching.utils.html.HtmlUtils;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Unified, namespace-tolerant "business element" pull-parser for GPX files relevant to Geocaching. Reacts on
 * Geocaches, Waypoints, (Named) Coordinates, Routes and Tracks instead of raw XML elements, via
 * {@link IGPXParseHooks}.
 * <br>
 * <ul>
 *     <li>Handles GPX 1.0 and GPX 1.1 (and mixtures thereof) in one unified code path (no version-specific subclasses).</li>
 *     <li>Does not rely on namespaces being correctly declared; elements/attributes are identified by local
 *     name only (namespace prefixes, if any, are simply stripped).</li>
 *     <li>Self-contained: will only use information from GPX file(s) in created objects.</li>
 *     <li>Multi-file-capability: multiple calls to "parse" may be used to scan related GPX files (eg from a ZIP file)</li>
 *     <li>Handling of enhancements such as GSAP, Groundspeak and c:geo's own tags (via dedicated methods).</li>
 * </ul>
 * <br>
 * {@link #parse} can be called repeatedly, in any order, on the same instance to handle multi-GPX-file-situations (eg from ZIP file).
 * Session-scoped state (esp the cross-file name→geocode index) is <em>not</em> cleared automatically between calls,
 * so resolution works cross-file. Call {@link #reset()} to explicitly start a new, unrelated session on a reused instance.
 */
public class GPXParser {

    private static final Pattern PATTERN_GEOCODE = Pattern.compile("[0-9A-Z]{5,}");

    //Namespaces of extensions
    private static final Set<String> GROUNDSPEAK_NS = Set.of(
        "http://www.groundspeak.com/cache/1/1", // PQ 1.1
        "http://www.groundspeak.com/cache/1/0/1", // PQ 1.0.1
        "http://www.groundspeak.com/cache/1/0" // PQ 1.0
    );

    private static final Set<String> GSAK_NS = Set.of(
        "http://www.gsak.net/xmlv1/1",
        "http://www.gsak.net/xmlv1/2",
        "http://www.gsak.net/xmlv1/3",
        "http://www.gsak.net/xmlv1/4",
        "http://www.gsak.net/xmlv1/5",
        "http://www.gsak.net/xmlv1/6"
    );

    private static final Set<String> CGEO_NS = Set.of(
        "http://www.cgeo.org/wptext/1/0"
    );

    private static final Set<String> OPENCACHING_NS = Set.of(
        "https://github.com/opencaching/gpx-extension-v1"
    );

    private static final Set<String> TERRA_NS = Set.of(
            "http://www.TerraCaching.com/GPX/1/0"
    );

    private WptParseMode geocacheWptParseMode = WptParseMode.FULL;
    private WptParseMode waypointWptParseMode = WptParseMode.FULL;
    private WptParseMode defaultRouteWptParseMode = WptParseMode.FULL;
    private WptParseMode defaultTrackWptParseMode = WptParseMode.FULL;

    /** parse-session-scoped index: (lower-cased, trimmed) cache name/title -&gt; geocode */
    private final Map<String, String> nameToGeocodeIndex = new HashMap<>();

    /**
     * Sticky, single-file-scoped flag:
     * we can only detect TerraCaching child waypoints by remembering the state of the parent". TerraCaching
     * marks the cache entry that precedes its own child-stage waypoints with {@code <desc>GC_WayPoint1</desc>};
     * every following {@code wptType} entry with a "terracache" {@code sym} in the SAME document is then a
     * child waypoint of that cache, not a new cache itself, until end of file (there is no reset marker).
     */
    private boolean terraChildWaypoint;

    private IGPXParseHooks hooks;

    /** Controls how wptTypes are parsed. */
    public enum WptParseMode {
        /** Parse/collect the complete information. */
        FULL,
        /** only basic data is parsed per wptType (name + coordinate) */
        COORDINATES_ONLY,
        /** Do not collect wptType data.*/
        SKIP
    }

    public GPXParser setGeocacheParseMode(final WptParseMode mode) {
        this.geocacheWptParseMode = mode == null ? WptParseMode.FULL : mode;
        return this;
    }

    public GPXParser setWaypointParseMode(final WptParseMode mode) {
        this.waypointWptParseMode = mode == null ? WptParseMode.FULL : mode;
        return this;
    }

    public GPXParser setDefaultRouteParseMode(final WptParseMode mode) {
        this.defaultRouteWptParseMode = mode == null ? WptParseMode.FULL : mode;
        return this;
    }

    public GPXParser setDefaultTrackParseMode(final WptParseMode mode) {
        this.defaultTrackWptParseMode = mode == null ? WptParseMode.FULL : mode;
        return this;
    }

    /** Clears this instance's session-scoped state (esp the cross-file name→geocode index). */
    public GPXParser reset() {
        nameToGeocodeIndex.clear();
        terraChildWaypoint = false;
        return this;
    }

    /** Parses the given stream as GPX, notifying {@code hooks} about business elements found. */
    public void parse(@NonNull final InputStream stream, @NonNull final IGPXParseHooks hooks) throws IOException, XmlPullParserException {
        parse(XmlUtils.createParser(stream, false), hooks);
    }

    /**
     * Parses the given, already-positioned-or-fresh pull parser as GPX, notifying {@code hooks} about business
     * elements found. The parser is driven forward (single, forward-only pass). May be called
     * repeatedly on the same instance to parse several related GPX documents as one logical unit
     */
    public void parse(@NonNull final XmlPullParser parser, @NonNull final IGPXParseHooks hooks) throws IOException, XmlPullParserException {
        this.hooks = hooks;
        this.terraChildWaypoint = false;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "gpx".equals(localName(parser.getName()))) {
                handleGpx(parser);
                return;
            }
            event = parser.next();
        }
    }

    private void handleGpx(final XmlPullParser parser) throws IOException, XmlPullParserException {
        final String creator = attr(parser, "creator");
        hooks.onInit(creator);

        int event = parser.next();
        while (!(event == XmlPullParser.END_TAG && "gpx".equals(localName(parser.getName())))) {
            if (event == XmlPullParser.START_TAG) {
                final String name = localName(parser.getName());
                if ("wpt".equals(name)) {
                    handleTopLevelWpt(parser);
                } else if ("rte".equals(name)) {
                    handleRoute(parser);
                } else if ("trk".equals(name)) {
                    handleTrack(parser);
                } else {
                    skipSubtree(parser);
                }
            }
            event = parser.next();
        }
    }

    private void handleTopLevelWpt(final XmlPullParser parser) throws IOException, XmlPullParserException {
        // note: can't cheaply pre-filter by lat/lon here (unlike route/track points) - a geocache-classified
        // entry without valid coordinates may still be reportable, see the ZZ (c:geo "cache without known
        // coordinates") exception: https://github.com/cgeo/cgeo/issues/9305
        dispatchWptType(parser);
    }

    // ---------------------------------------------------------------------------------------------------
    // routes
    // ---------------------------------------------------------------------------------------------------

    private void handleRoute(final XmlPullParser parser) throws IOException, XmlPullParserException {
        final WptParseMode mode = orDefault(hooks.onRouteStart(), defaultRouteWptParseMode);
        String routeName = null;
        int pointCount = 0;

        int event = parser.next();
        while (!(event == XmlPullParser.END_TAG && "rte".equals(localName(parser.getName())))) {
            if (event == XmlPullParser.START_TAG) {
                final String name = localName(parser.getName());
                if ("name".equals(name)) {
                    routeName = readText(parser);
                } else if ("rtept".equals(name)) {
                    if (handleRouteOrTrackPoint(parser, mode)) {
                        pointCount++;
                    }
                } else {
                    skipSubtree(parser);
                }
            }
            event = parser.next();
        }
        hooks.onRouteEnd(routeName, pointCount);
    }

    // ---------------------------------------------------------------------------------------------------
    // tracks
    // ---------------------------------------------------------------------------------------------------

    private void handleTrack(final XmlPullParser parser) throws IOException, XmlPullParserException {
        final WptParseMode trackMode = orDefault(hooks.onTrackStart(), defaultTrackWptParseMode);
        String trackName = null;
        int segmentCount = 0;
        int totalPointCount = 0;

        int event = parser.next();
        while (!(event == XmlPullParser.END_TAG && "trk".equals(localName(parser.getName())))) {
            if (event == XmlPullParser.START_TAG) {
                final String name = localName(parser.getName());
                if ("name".equals(name)) {
                    trackName = readText(parser);
                } else if ("trkseg".equals(name)) {
                    totalPointCount += handleTrackSegment(parser, trackMode);
                    segmentCount++;
                } else {
                    skipSubtree(parser);
                }
            }
            event = parser.next();
        }
        hooks.onTrackEnd(trackName, segmentCount, totalPointCount);
    }

    /** @return number of valid points found in this segment */
    private int handleTrackSegment(final XmlPullParser parser, final WptParseMode trackMode) throws IOException, XmlPullParserException {
        final WptParseMode mode = orDefault(hooks.onTrackSegmentStart(), trackMode);
        String segmentName = null;
        int pointCount = 0;

        int event = parser.next();
        while (!(event == XmlPullParser.END_TAG && "trkseg".equals(localName(parser.getName())))) {
            if (event == XmlPullParser.START_TAG) {
                final String name = localName(parser.getName());
                if ("name".equals(name)) {
                    segmentName = readText(parser);
                } else if ("trkpt".equals(name)) {
                    if (handleRouteOrTrackPoint(parser, mode)) {
                        pointCount++;
                    }
                } else {
                    skipSubtree(parser);
                }
            }
            event = parser.next();
        }
        hooks.onTrackSegmentEnd(segmentName, pointCount);
        return pointCount;
    }

    /**
     * Handles a single {@code rtept}/{@code trkpt} according to the given (already-resolved) {@link WptParseMode}.
     * Fires the appropriate hook(s) for the point, if any.
     *
     * @return {@code true} if the point had valid lat/lon and therefore counts towards the enclosing
     * route's/segment's point count; {@code false} otherwise
     */
    private boolean handleRouteOrTrackPoint(final XmlPullParser parser, final WptParseMode mode) throws IOException, XmlPullParserException {
        if (mode == WptParseMode.SKIP) {
            // cheap: only check lat/lon (attributes on the start tag) for counting purposes
            final boolean valid = xmlNodeReadLatLon(parser) != null;
            skipSubtree(parser);
            return valid;
        }
        if (mode == WptParseMode.COORDINATES_ONLY) {
            return dispatchCoordinateOnly(parser);
        }
        // FULL: full classification, same as for a top-level wpt; cannot cheaply pre-filter by lat/lon
        // here either, for the same reason as handleTopLevelWpt (ZZ exception)
        return dispatchWptType(parser);
    }

    /**
     * lightweight, non-buffering read of a wpt/rtept/trkpt when only coordinates are wanted (ParseMode.COORDINATES_ONLY)
     * @return {@code true} if the point had valid lat/lon, {@code false} if it was ignored entirely
     */
    private boolean dispatchCoordinateOnly(final XmlPullParser parser) throws IOException, XmlPullParserException {
        final Geopoint coords = xmlNodeReadLatLon(parser);
        if (coords == null) {
            skipSubtree(parser);
            return false;
        }

        String name = null;
        Float elevation = null;
        int event = parser.next();
        while (event != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.START_TAG) {
                final String childName = localName(parser.getName());
                if ("name".equals(childName)) {
                    name = readText(parser);
                } else if ("ele".equals(childName)) {
                    elevation = parseFloatSafe(readText(parser));
                } else {
                    skipSubtree(parser);
                }
            }
            event = parser.next();
        }

        if (StringUtils.isBlank(name) && elevation == null) {
            hooks.onCoordinate(coords);
        } else {
            final NamedGeoCoordinate named = new NamedGeoCoordinate();
            named.setCoords(coords);
            if (elevation != null) {
                named.setElevation(elevation);
            }
            if (StringUtils.isNotBlank(name)) {
                named.setName(name.trim());
            }
            hooks.onNamedCoordinate(named);
        }
        return true;
    }

    /**
     * Full scan of a wpt/rtept/trkpt subtree, classifying it as geocache / waypoint / named coordinate / plain coordinate and firing the appropriate hook.
     * @return {@code true} if the element had valid lat/lon and was processed; {@code false} otherwise
     */
    private boolean dispatchWptType(final XmlPullParser parser) throws XmlPullParserException, IOException {
        final XmlNode wptNode = XmlNode.scanNode(parser);
        final Geopoint coords = xmlNodeReadLatLon(wptNode);

        final String rawName = wptNode.getChildValue("name");
        final String desc = wptNode.getChildValue("desc");
        final String cmt = wptNode.getChildValue("cmt");
        final String symRaw = wptNode.getChildValue("sym");
        final String sym = symRaw == null ? null : symRaw.toLowerCase(Locale.US);
        final String typeField = wptNode.getChildValue("type");
        String type = null;
        String subtype = null;
        if (StringUtils.isNotBlank(typeField)) {
            final String[] parts = StringUtils.split(typeField, '|');
            if (parts.length > 0) {
                type = parts[0].toLowerCase(Locale.US).trim();
            }
            if (parts.length > 1) {
                subtype = parts[1].toLowerCase(Locale.US).trim();
            }
        }

        // sticky state carried over from a PRECEDING sibling wpt (see terraChildWaypoint field doc)
        final boolean wasTerraChildWaypoint = this.terraChildWaypoint;
        final boolean isTerraChildWaypointMarker = "GC_WayPoint1".equals(StringUtils.trim(desc));

        final boolean isGeocache = Strings.CI.contains(type, "geocache")
                || Strings.CI.contains(sym, "geocache")
                || Strings.CI.contains(sym, "waymark")
                || (Strings.CI.contains(sym, "terracache") && !wasTerraChildWaypoint);
        final boolean isWaypoint = !isGeocache && (Strings.CI.contains(type, "waypoint") || wasTerraChildWaypoint);

        if (isTerraChildWaypointMarker) {
            this.terraChildWaypoint = true;
        }

        if (coords == null) {
            // no valid coordinates -> ignore entirely, as if this wptType didn't exist...
            if (!isGeocache || geocacheWptParseMode != WptParseMode.FULL) {
                return false;
            }
            final String geocode = resolveGeocode(rawName, desc, cmt);
            // ...EXCEPT: the ZZ-case with no coordinates (see https://github.com/cgeo/cgeo/issues/9305)
            if (geocode == null || !InternalConnector.getInstance().canHandle(geocode)) {
                return false;
            }
            dispatchGeocache(wptNode, null, rawName, desc, cmt);
            return true;
        }

        if (isGeocache) {
            dispatchGeocache(wptNode, coords, rawName, desc, cmt);
        } else if (isWaypoint) {
            dispatchWaypoint(wptNode, coords, rawName, sym, subtype, wasTerraChildWaypoint);
        } else {
            // fallback: named or plain coordinate
            dispatchFallbackCoordinate(coords, rawName, wptNode);
        }
        return true;
    }

    /** Fires {@link IGPXParseHooks#onCoordinate}/{@link IGPXParseHooks#onNamedCoordinate} as appropriate. */
    private void dispatchFallbackCoordinate(final Geopoint coords, final String rawName, final XmlNode wptNode) {
        final Float elevation = parseFloatSafe(wptNode.getChildValue("ele"));
        if (StringUtils.isBlank(rawName) && elevation == null) {
            hooks.onCoordinate(coords);
            return;
        }
        final NamedGeoCoordinate named = new NamedGeoCoordinate();
        named.setCoords(coords);
        if (elevation != null) {
            named.setElevation(elevation);
        }
        if (StringUtils.isNotBlank(rawName)) {
            named.setName(rawName.trim());
        }
        hooks.onNamedCoordinate(named);
    }

    /**
     * A {@code wptType} classified as a geocache.
     *
     * @param coords may be {@code null} (only possible via the ZZ exception)
     */
    private void dispatchGeocache(final XmlNode wptNode, @Nullable final Geopoint coords, final String rawName,
                                   final String desc, final String cmt) {
        if (geocacheWptParseMode == WptParseMode.SKIP) {
            return;
        }
        if (geocacheWptParseMode == WptParseMode.COORDINATES_ONLY) {
            dispatchFallbackCoordinate(coords, rawName, wptNode);
            return;
        }

        final Geocache cache = createCache();
        cache.setCoords(coords);
        final String geocode = resolveGeocode(rawName, desc, cmt);
        cache.setGeocode(geocode == null ? "" : geocode);
        if (StringUtils.isNotBlank(rawName)) {
            cache.setName(rawName.trim());
        }
        if (StringUtils.isNotBlank(desc)) {
            cache.setShortDescription(validate(desc));
        }
        if (StringUtils.isNotBlank(cmt)) {
            cache.setDescription(validate(cmt));
        }
        final String timeText = wptNode.getChildValue("time");
        final Date hidden = parseDate(timeText);
        if (hidden != null) {
            cache.setHidden(hidden);
        }

        final List<LogEntry> logs = parseGeocacheExtensions(wptNode, cache);

        if (StringUtils.isNotBlank(cache.getGeocode()) && StringUtils.isNotBlank(cache.getName())) {
            nameToGeocodeIndex.put(cache.getName().trim().toLowerCase(Locale.US), cache.getGeocode());
        }

        hooks.onGeocache(cache, logs);
    }

    /**
     * Resolves a geocode purely from GPX-local information: first a geocode-looking pattern in the
     * name, then in {@code desc}, then in {@code cmt}, then (as last resort) the trimmed name verbatim.
     */
    @Nullable
    private static String resolveGeocode(final String rawName, final String desc, final String cmt) {
        String geocode = findGeoCode(rawName);
        if (geocode == null) {
            geocode = findGeoCode(desc);
        }
        if (geocode == null) {
            geocode = findGeoCode(cmt);
        }
        if (geocode == null && StringUtils.isNotBlank(rawName)) {
            geocode = rawName.trim();
        }
        return geocode;
    }

    /** handles a {@code wptType} classified as a waypoint. */
    private void dispatchWaypoint(final XmlNode wptNode, final Geopoint coords, final String rawName,
                                   final String sym, final String subtype, final boolean isTerraChildWaypoint) {
        if (waypointWptParseMode == WptParseMode.SKIP) {
            return;
        }
        if (waypointWptParseMode == WptParseMode.COORDINATES_ONLY) {
            dispatchFallbackCoordinate(coords, rawName, wptNode);
            return;
        }

        final String parentGeocodeCandidate = resolveParentGeocode(wptNode, rawName, isTerraChildWaypoint);

        final Waypoint waypoint = new Waypoint(StringUtils.trim(rawName), WaypointType.fromGPXString(sym == null ? "" : sym, subtype), false);
        waypoint.setId(Waypoint.NEW_ID);
        waypoint.setCoords(coords);
        if (parentGeocodeCandidate != null) {
            waypoint.setGeocode(parentGeocodeCandidate);
        }

        final String note = wptNode.getChildValue("cmt");
        if (StringUtils.isNotBlank(note)) {
            waypoint.setNote(validate(note));
        }

        hooks.onWaypoint(waypoint, parentGeocodeCandidate);
    }

    /**
     * Best-effort resolution of a waypoint's parent geocache geocodefrom information available in the GPX document itself
     */
    @Nullable
    private String resolveParentGeocode(final XmlNode wptNode, final String rawName, final boolean isTerraChildWaypoint) {
        final XmlNode extensions = extensionsBase(wptNode);
        final XmlNode gsakExt = extensions == null ? null : extensions.getChild("wptExtension");
        final String gsakParent = gsakExt == null ? null : gsakExt.getChildValue("Parent");
        if (StringUtils.isNotBlank(gsakParent)) {
            return gsakParent.trim();
        }

        final String trimmedName = StringUtils.trim(rawName);
        if (StringUtils.isBlank(trimmedName)) {
            return null;
        }

        if (isTerraChildWaypoint) {
            return trimmedName.length() > 1 ? trimmedName.substring(0, trimmedName.length() - 1) : null;
        }

        if (trimmedName.length() > 2) {
            return "GC" + trimmedName.substring(2).toUpperCase(Locale.US);
        }

        return nameToGeocodeIndex.get(trimmedName.toLowerCase(Locale.US));
    }

    // ---------------------------------------------------------------------------------------------------
    // extension parsing, one method per source
    // ---------------------------------------------------------------------------------------------------

    @Nullable
    private List<LogEntry> parseGeocacheExtensions(final XmlNode wptNode, final Geocache cache) {
        final XmlNode base = extensionsBase(wptNode);
        if (base == null) {
            return null;
        }
        final List<LogEntry> groundspeakLogs = parseGroundspeakExtension(base, cache);
        parseGsakExtension(base, cache);
        final List<LogEntry> terraLogs = parseTerraCachingExtension(base, cache);
        parseCgeoExtension(base, cache);
        parseOpenCachingExtension(base, cache);
        return groundspeakLogs != null ? groundspeakLogs : terraLogs;
    }

    /**
     * Groundspeak cache extension, used by geocaching.com pocket queries and most third-party tools (GSAK, ...).
     * Schema/namespace (any of 3 historic versions, unified here by local name only): PQ 1.1
     * {@code http://www.groundspeak.com/cache/1/1}, PQ 1.0.1 {@code http://www.groundspeak.com/cache/1/0/1},
     * PQ 1.0 {@code http://www.groundspeak.com/cache/1/0}. Element {@code <cache>}.
     */
    @Nullable
    private List<LogEntry> parseGroundspeakExtension(final XmlNode base, final Geocache cache) {
        final XmlNode gcCache = xmlNodeChild(base, "cache", GROUNDSPEAK_NS);
        if (gcCache == null) {
            return null;
        }
        final String id = xmlNodeAttrValue(gcCache, "id", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(id)) {
            cache.setCacheId(id);
        }
        final String archived = xmlNodeAttrValue(gcCache, "archived", GROUNDSPEAK_NS);
        if (archived != null) {
            cache.setArchived("true".equalsIgnoreCase(archived));
        }
        final String available = xmlNodeAttrValue(gcCache, "available", GROUNDSPEAK_NS);
        if (available != null) {
            cache.setDisabled(!"true".equalsIgnoreCase(available));
        }

        final String name = xmlNodeChildText(gcCache, "name", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(name)) {
            cache.setName(validate(name));
        }
        final String owner = xmlNodeChildText(gcCache, "owner", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(owner)) {
            cache.setOwnerUserId(validate(owner));
        }
        final String placedBy = xmlNodeChildText(gcCache, "placed_by", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(placedBy)) {
            cache.setOwnerDisplayName(validate(placedBy));
        }
        final String gcType = xmlNodeChildText(gcCache, "type", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(gcType)) {
            String body = validate(gcType);
            if (body.startsWith("Geocache|")) {
                body = StringUtils.substringAfter(body, "Geocache|").trim();
            }
            cache.setType(CacheType.getByPattern(body));
        }
        final String container = xmlNodeChildText(gcCache, "container", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(container)) {
            cache.setSize(CacheSize.getById(validate(container)));
        }
        final Float difficulty = parseFloatSafe(xmlNodeChildText(gcCache, "difficulty", GROUNDSPEAK_NS));
        if (difficulty != null) {
            cache.setDifficulty(difficulty);
        }
        final Float terrain = parseFloatSafe(xmlNodeChildText(gcCache, "terrain", GROUNDSPEAK_NS));
        if (terrain != null) {
            cache.setTerrain(terrain);
        }
        final String country = xmlNodeChildText(gcCache, "country", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(country)) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(country) : cache.getLocation() + ", " + country.trim());
        }
        final String state = xmlNodeChildText(gcCache, "state", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(state) && StringUtils.isNotEmpty(state.trim())) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(state) : state.trim() + ", " + cache.getLocation());
        }
        final String hints = xmlNodeChildText(gcCache, "encoded_hints", GROUNDSPEAK_NS);
        if (hints != null) {
            cache.setHint(validate(hints));
        }
        final String shortDesc = xmlNodeChildText(gcCache, "short_description", GROUNDSPEAK_NS);
        if (shortDesc != null) {
            cache.setShortDescription(validate(shortDesc));
        }
        final String longDesc = xmlNodeChildText(gcCache, "long_description", GROUNDSPEAK_NS);
        if (longDesc != null) {
            cache.setDescription(validate(longDesc));
        }

        parseGroundspeakAttributes(gcCache, cache);
        parseGroundspeakTravelbugs(gcCache, cache);
        return parseGroundspeakLogs(gcCache);
    }

    /**
     * Groundspeak logs ({@code <groundspeak:logs><groundspeak:log>...}). Same schema/namespace as
     * {@link #parseGroundspeakExtension}.
     */
    @Nullable
    private List<LogEntry> parseGroundspeakLogs(final XmlNode gcCache) {
        final XmlNode logsNode = xmlNodeChild(gcCache, "logs", GROUNDSPEAK_NS);
        if (logsNode == null) {
            return null;
        }
        final List<XmlNode> logNodes = xmlNodeChildren(logsNode, "log");
        if (logNodes == null) {
            return null;
        }
        final List<LogEntry> result = new ArrayList<>();
        for (final XmlNode logNode : logNodes) {
            final LogEntry.Builder builder = new LogEntry.Builder();
            final String idText = xmlNodeAttrValue(logNode, "id", GROUNDSPEAK_NS);
            if (idText != null) {
                try {
                    builder.setId(Integer.parseInt(idText.trim()));
                } catch (final NumberFormatException ignored) {
                    // ignore malformed id
                }
            }
            final Date date = parseDate(xmlNodeChildText(logNode, "date", GROUNDSPEAK_NS));
            if (date != null) {
                builder.setDate(date.getTime());
            }
            final String typeText = xmlNodeChildText(logNode, "type", GROUNDSPEAK_NS);
            if (typeText != null) {
                builder.setLogType(LogType.getByType(validate(typeText)));
            }
            final String finder = xmlNodeChildText(logNode, "finder", GROUNDSPEAK_NS);
            if (finder != null) {
                builder.setAuthor(validate(finder));
            }
            final String text = xmlNodeChildText(logNode, "text", GROUNDSPEAK_NS);
            if (text != null) {
                builder.setLog(validate(text));
            }
            result.add(builder.build());
        }
        return result.isEmpty() ? null : result;
    }

    private void parseGroundspeakAttributes(final XmlNode gcCache, final Geocache cache) {
        final XmlNode attributes = xmlNodeChild(gcCache, "attributes", GROUNDSPEAK_NS);
        if (attributes == null) {
            return;
        }
        final List<XmlNode> attributeNodes = xmlNodeChildren(attributes, "attribute");
        if (attributeNodes == null) {
            return;
        }
        final List<String> result = new ArrayList<>();
        for (final XmlNode attributeNode : attributeNodes) {
            final String idText = xmlNodeAttrValue(attributeNode, "id", GROUNDSPEAK_NS);
            final String incText = xmlNodeAttrValue(attributeNode, "inc", GROUNDSPEAK_NS);
            if (idText == null || incText == null) {
                continue;
            }
            try {
                final int attributeId = Integer.parseInt(idText.trim());
                final boolean active = Integer.parseInt(incText.trim()) != 0;
                final CacheAttribute attribute = CacheAttribute.getById(attributeId);
                if (attribute != null) {
                    result.add(attribute.getValue(active));
                }
            } catch (final NumberFormatException ignored) {
                // ignore malformed attribute entries
            }
        }
        cache.setAttributes(result);
    }

    private void parseGroundspeakTravelbugs(final XmlNode gcCache, final Geocache cache) {
        final XmlNode travelbugs = xmlNodeChild(gcCache, "travelbugs", GROUNDSPEAK_NS);
        if (travelbugs == null) {
            return;
        }
        final List<XmlNode> tbNodes = xmlNodeChildren(travelbugs, "travelbug");
        if (tbNodes == null) {
            return;
        }
        for (final XmlNode tbNode : tbNodes) {
            final Trackable trackable = new Trackable();
            final String ref = xmlNodeAttrValue(tbNode, "ref", GROUNDSPEAK_NS);
            if (ref != null) {
                trackable.setGeocode(ref);
            }
            final String tbName = xmlNodeChildText(tbNode, "name", GROUNDSPEAK_NS);
            if (tbName != null) {
                trackable.setName(validate(tbName));
            }
            if (StringUtils.isNotBlank(trackable.getGeocode()) && StringUtils.isNotBlank(trackable.getName())) {
                cache.addInventoryItem(trackable);
            }
        }
    }

    /**
     * GSAK ("Geocaching Swiss Army Knife") wptExtension. Schema/namespace (6 historic versions, unified here by
     * local name only): {@code http://www.gsak.net/xmlv1/1} through {@code /6}. Element {@code <wptExtension>}.
     */
    private void parseGsakExtension(final XmlNode base, final Geocache cache) {
        final XmlNode gsak = xmlNodeChild(base, "wptExtension", GSAK_NS);
        if (gsak == null) {
            return;
        }
        final String watch = xmlNodeChildText(gsak, "Watch", GSAK_NS);
        if (watch != null) {
            cache.setOnWatchlist(Boolean.parseBoolean(watch.trim()));
        }
        final String favPoints = xmlNodeChildText(gsak, "FavPoints", GSAK_NS);
        if (favPoints != null) {
            try {
                cache.setFavoritePoints(Integer.parseInt(favPoints.trim()));
            } catch (final NumberFormatException ignored) {
                // ignore malformed favorite points
            }
        }
        final String gcNote = xmlNodeChildText(gsak, "GcNote", GSAK_NS);
        if (StringUtils.isNotBlank(gcNote)) {
            cache.setPersonalNote(StringUtils.trim(gcNote), true);
        }
        final String isPremium = xmlNodeChildText(gsak, "IsPremium", GSAK_NS);
        if (isPremium != null) {
            cache.setPremiumMembersOnly(Boolean.parseBoolean(isPremium.trim()));
        }
        final String code = xmlNodeChildText(gsak, "Code", GSAK_NS);
        if (StringUtils.isNotBlank(code)) {
            cache.setGeocode(StringUtils.trim(code));
        }
        final String dnf = xmlNodeChildText(gsak, "DNF", GSAK_NS);
        if (dnf != null && !cache.isFound()) {
            cache.setDNF(Boolean.parseBoolean(dnf.trim()));
        }
        final String dnfDate = xmlNodeChildText(gsak, "DNFDate", GSAK_NS);
        if (dnfDate != null && cache.getVisitedDate() == 0) {
            final Date parsed = parseDate(dnfDate);
            if (parsed != null) {
                cache.setVisitedDate(parsed.getTime());
            }
        }
        final String userFound = xmlNodeChildText(gsak, "UserFound", GSAK_NS);
        if (userFound != null && cache.getVisitedDate() == 0) {
            final Date parsed = parseDate(userFound);
            if (parsed != null) {
                cache.setVisitedDate(parsed.getTime());
            }
        }

        final StringBuilder userDataNote = new StringBuilder();
        appendUserData(userDataNote, xmlNodeChildText(gsak, "UserData", GSAK_NS));
        for (int i = 2; i <= 4; i++) {
            appendUserData(userDataNote, xmlNodeChildText(gsak, "User" + i, GSAK_NS));
        }
        if (StringUtils.isBlank(cache.getPersonalNote()) && userDataNote.length() > 0) {
            cache.setPersonalNote(userDataNote.toString().trim(), true);
        }
    }

    private static void appendUserData(final StringBuilder buffer, final String userData) {
        if (StringUtils.isNotBlank(userData)) {
            buffer.append(' ').append(userData);
        }
    }

    /** TerraCaching extension. */
    @Nullable
    private List<LogEntry> parseTerraCachingExtension(final XmlNode base, final Geocache cache) {
        final XmlNode terraCache = xmlNodeChild(base, "terracache", TERRA_NS);
        if (terraCache == null) {
            return null;
        }
        final String name = xmlNodeChildText(terraCache, "name", TERRA_NS);
        if (StringUtils.isNotBlank(name)) {
            cache.setName(StringUtils.trim(name));
        }
        final String owner = xmlNodeChildText(terraCache, "owner", TERRA_NS);
        if (StringUtils.isNotBlank(owner)) {
            cache.setOwnerDisplayName(validate(owner));
        }
        final String style = xmlNodeChildText(terraCache, "style", TERRA_NS);
        if (StringUtils.isNotBlank(style)) {
            cache.setType(TerraCachingType.getCacheType(style));
        }
        final String size = xmlNodeChildText(terraCache, "size", TERRA_NS);
        if (StringUtils.isNotBlank(size)) {
            cache.setSize(CacheSize.getById(size));
        }
        final String country = xmlNodeChildText(terraCache, "country", TERRA_NS);
        if (StringUtils.isNotBlank(country)) {
            cache.setLocation(StringUtils.trim(country));
        }
        final String state = xmlNodeChildText(terraCache, "state", TERRA_NS);
        if (StringUtils.isNotBlank(state) && StringUtils.isNotEmpty(state.trim())) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(state) : state.trim() + ", " + cache.getLocation());
        }
        final String description = xmlNodeChildText(terraCache, "description", TERRA_NS);
        if (description != null) {
            cache.setDescription(trimHtml(description));
        }
        final String hint = xmlNodeChildText(terraCache, "hint", TERRA_NS);
        if (hint != null) {
            cache.setHint(HtmlUtils.extractText(hint));
        }
        return parseTerraCachingLogs(terraCache);
    }

    /** TerraCaching logs ({@code <terracache><logs><log>...}). */
    @Nullable
    private List<LogEntry> parseTerraCachingLogs(final XmlNode terraCache) {
        final XmlNode logsNode = xmlNodeChild(terraCache, "logs", TERRA_NS);
        if (logsNode == null) {
            return null;
        }
        final List<XmlNode> logNodes = xmlNodeChildren(logsNode, "log");
        if (logNodes == null) {
            return null;
        }
        final List<LogEntry> result = new ArrayList<>();
        for (final XmlNode logNode : logNodes) {
            final LogEntry.Builder builder = new LogEntry.Builder();
            final String idText = xmlNodeAttrValue(logNode, "id", TERRA_NS);
            if (idText != null) {
                try {
                    builder.setId(Integer.parseInt(idText.trim()));
                } catch (final NumberFormatException ignored) {
                    // ignore malformed id
                }
            }
            final Date date = parseDate(xmlNodeChildText(logNode, "date", TERRA_NS));
            if (date != null) {
                builder.setDate(date.getTime());
            }
            final String typeText = xmlNodeChildText(logNode, "type", TERRA_NS);
            if (typeText != null) {
                builder.setLogType(TerraCachingLogType.getLogType(validate(typeText)));
            }
            final String finder = xmlNodeChildText(logNode, "user", TERRA_NS);
            if (finder != null) {
                builder.setAuthor(validate(finder));
            }
            final String text = xmlNodeChildText(logNode, "entry", TERRA_NS);
            if (text != null) {
                builder.setLog(trimHtml(validate(text)));
            }
            result.add(builder.build());
        }
        return result.isEmpty() ? null : result;
    }

    /** c:geo's own extension */
    private void parseCgeoExtension(final XmlNode base, final Geocache cache) {
        final String assignedEmojiText = xmlNodeChildText(xmlNodeChild(base, "cacheExtension", CGEO_NS), "assignedEmoji", CGEO_NS);
        if (StringUtils.isNotBlank(assignedEmojiText)) {
            cache.setAssignedEmoji(EmojiUtilsLegacyMigration.parseGpxAssignedEmoji(assignedEmojiText));
        }
    }

    /**
     * Opencaching extension. Schema/namespace: {@code https://github.com/opencaching/gpx-extension-v1}. Element
     * {@code <cache>}.
     */
    private void parseOpenCachingExtension(final XmlNode base, final Geocache cache) {
        final XmlNode ocCache = xmlNodeChild(base, "cache", OPENCACHING_NS);
        if (ocCache == null) {
            return;
        }
        final String size = xmlNodeChildText(ocCache, "size", OPENCACHING_NS);
        if (StringUtils.isNotBlank(size)) {
            final CacheSize cacheSize = CacheSize.getById(size);
            if (cacheSize != CacheSize.UNKNOWN) {
                cache.setSize(cacheSize);
            }
        }
    }

    private static String trimHtml(final String html) {
        return StringUtils.trim(Strings.CS.removeEnd(Strings.CS.removeStart(html, "<br>"), "<br>"));
    }

    private static Geocache createCache() {
        final Geocache newCache = new Geocache();
        // explicitly set all properties which could otherwise lead to lazy database access on first read
        newCache.setLocation("");
        newCache.setDescription("");
        newCache.setShortDescription("");
        newCache.setHint("");
        newCache.setAttributes(Collections.emptyList());
        newCache.setWaypoints(Collections.emptyList());
        return newCache;
    }

    private static String validate(final String input) {
        if ("nil".equalsIgnoreCase(input)) {
            return "";
        }
        return input.trim();
    }

    @Nullable
    private static String findGeoCode(final String input) {
        if (input == null) {
            return null;
        }
        final String trimmed = input.trim();
        final java.util.regex.Matcher matcher = PATTERN_GEOCODE.matcher(trimmed);
        if (matcher.find()) {
            final String geocode = matcher.group();
            if (geocode.length() == trimmed.length() || Character.isWhitespace(trimmed.charAt(geocode.length()))) {
                return geocode;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------
    // date parsing: tolerant of the handful of date formats found in real-world GPX files
    // ---------------------------------------------------------------------------------------------------

    private static final Pattern PATTERN_MILLISECONDS = Pattern.compile("\\.\\d{3,7}");

    @Nullable
    private static Date parseDate(final String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        String body = input.trim();
        body = PATTERN_MILLISECONDS.matcher(body).replaceFirst("");
        final String[] patterns = {
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
        };
        for (final String pattern : patterns) {
            try {
                final SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                return format.parse(body);
            } catch (final ParseException ignored) {
                // try next pattern
            }
        }
        return null;
    }

    @Nullable
    private static Float parseFloatSafe(final String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        try {
            return Float.parseFloat(input.trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // XmlNode helpers: namespace-tolerant lookup by local name (D-namespace tolerance, point 34)
    // ---------------------------------------------------------------------------------------------------

    /**
     * For GPX 1.1 files, extension fields live inside a {@code <extensions>} child; for GPX 1.0 files, they are
     * direct children of the {@code wpt}/{@code rtept}/{@code trkpt} element itself. .
     */
    @Nullable
    private static XmlNode extensionsBase(final XmlNode wptNode) {
        final XmlNode extensions = xmlNodeChild(wptNode, "extensions", null);
        return extensions != null ? extensions : wptNode;
    }

    @Nullable
    private static XmlNode xmlNodeChild(final XmlNode node, final String name, final Set<String> namespaces) {
        return XmlNode.getChild(node, name, namespaces);
    }

    @Nullable
    private static String xmlNodeChildText(final XmlNode node, final String name, final Set<String> namespaces) {
        final XmlNode c = xmlNodeChild(node, name, namespaces);
        return c == null ? null : c.getValue();
    }

    /** Like {@link #xmlNodeChild}, but returns ALL matching children (namespace-tolerant, by local name), not just the first. */
    @Nullable
    private static List<XmlNode> xmlNodeChildren(final XmlNode node, final String name) {
        return node == null ? null : node.getChildrenAsList(name);
    }

    @Nullable
    private static String xmlNodeAttrValue(final XmlNode node, final String attributeName, final Set<String> namespaces) {
        return xmlNodeChildText(node, XmlNode.ATTRIBUTE_PRAEFIX + attributeName, namespaces);
    }

    @Nullable
    private static Geopoint xmlNodeReadLatLon(final XmlNode node) {
        final String lat = xmlNodeAttrValue(node, "lat", null);
        final String lon = xmlNodeAttrValue(node, "lon", null);
        return toGeopoint(lat, lon);
    }

    @Nullable
    private static Geopoint xmlNodeReadLatLon(final XmlPullParser parser) {
        return toGeopoint(attr(parser, "lat"), attr(parser, "lon"));
    }

    @Nullable
    private static Geopoint toGeopoint(final String lat, final String lon) {
        if (StringUtils.isBlank(lat) || StringUtils.isBlank(lon)) {
            return null;
        }
        try {
            final double latD = Double.parseDouble(lat);
            final double lonD = Double.parseDouble(lon);
            if (latD == 0 && lonD == 0) {
                return null;
            }
            return new Geopoint(latD, lonD);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static String attr(final XmlPullParser parser, final String name) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (name.equals(localName(parser.getAttributeName(i)))) {
                return parser.getAttributeValue(i);
            }
        }
        return null;
    }

    @Nullable
    private static String localName(final String raw) {
        return XmlUtils.getLocalName(raw);
    }

    private static WptParseMode orDefault(final WptParseMode candidate, final WptParseMode fallback) {
        return candidate == null ? fallback : candidate;
    }

    /** Reads the text content of the element the parser is currently positioned at (a {@code START_TAG}). */
    private static String readText(final XmlPullParser parser) throws IOException, XmlPullParserException {
        String text = null;
        int event = parser.next();
        while (event != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.TEXT) {
                text = parser.getText();
            } else if (event == XmlPullParser.START_TAG) {
                skipSubtree(parser);
            }
            event = parser.next();
        }
        return text;
    }

    /**
     * Skips the subtree of the element the parser is currently positioned at (a {@code START_TAG}), leaving the
     * parser positioned at the corresponding {@code END_TAG}.
     */
    private static void skipSubtree(final XmlPullParser parser) throws IOException, XmlPullParserException {
        int depth = 1;
        while (depth > 0) {
            final int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new XmlPullParserException("Unexpected end of document while skipping subtree");
            }
        }
    }
}













