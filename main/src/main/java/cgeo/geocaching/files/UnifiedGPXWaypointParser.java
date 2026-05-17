package cgeo.geocaching.files;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
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
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.WaypointUserNoteCombiner;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Parses a single {@code <wpt>} element into either a fully-populated {@link Geocache}
 * or a {@link ChildWaypoint} that belongs to another cache. Brings over every extension
 * dialect today's {@code GPXParser} understands: Groundspeak, GSAK, c:geo, OpenCaching
 * and TerraCaching.
 * <p>
 * Namespace-agnostic on element local names. The only place namespace info is consulted
 * is the {@code <cache>} element, which is shared between Groundspeak and OpenCaching;
 * the namespace URI is matched on a substring to pick the right parser.
 * <p>
 * Output is wrapped in {@link Parsed}. Exactly one of {@code Parsed.cache} or
 * {@code Parsed.childWaypoint} is non-null. Logs are returned separately so the
 * integration layer can persist them via {@code DataStore.saveLogs}.
 */
final class UnifiedGPXWaypointParser {

    private static final Pattern PATTERN_GEOCODE = Pattern.compile("[0-9A-Z]{5,}");
    private static final Pattern PATTERN_GUID = Pattern.compile(".*"
            + Pattern.quote("guid=") + "([0-9a-z\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_URL_GEOCODE = Pattern.compile(".*"
            + Pattern.quote("wp=") + "([A-Z][0-9A-Z]+)", Pattern.CASE_INSENSITIVE);

    /** State that lives across multiple wpts in the same file. */
    static final class ParseContext {
        @Nullable String scriptUrl;
        /**
         * Set once we encounter a TerraCaching parent waypoint (identified by the
         * "GC_WayPoint1" sentinel in its short description). The next wpt is then
         * known to be a TerraCaching child.
         */
        boolean terraChildWaypoint;
    }

    /** Parse result for a single wpt. Exactly one of {@code cache} / {@code childWaypoint} is non-null. */
    static final class Parsed {
        @Nullable final Geocache cache;
        @Nullable final ChildWaypoint childWaypoint;
        @NonNull final List<LogEntry> logs;

        private Parsed(@Nullable final Geocache cache,
                       @Nullable final ChildWaypoint childWaypoint,
                       @NonNull final List<LogEntry> logs) {
            this.cache = cache;
            this.childWaypoint = childWaypoint;
            this.logs = logs;
        }

        static Parsed empty() {
            return new Parsed(null, null, Collections.emptyList());
        }
    }

    /** A child waypoint that needs to be matched to its parent cache. */
    static final class ChildWaypoint {
        @NonNull final Waypoint waypoint;
        @NonNull final String parentGeocode;
        /** Raw wpt name; used to compute the per-parent prefix later. */
        @NonNull final String wptName;
        final boolean userDefined;
        /** If true the parent's {@code userModifiedCoords} flag should be set on attach. */
        final boolean markParentUserModifiedCoords;

        ChildWaypoint(@NonNull final Waypoint waypoint, @NonNull final String parentGeocode,
                      @NonNull final String wptName, final boolean userDefined,
                      final boolean markParentUserModifiedCoords) {
            this.waypoint = waypoint;
            this.parentGeocode = parentGeocode;
            this.wptName = wptName;
            this.userDefined = userDefined;
            this.markParentUserModifiedCoords = markParentUserModifiedCoords;
        }
    }

    private UnifiedGPXWaypointParser() {
        // utility class
    }

    /**
     * Parse the {@code <wpt>} element the supplied parser is currently positioned on.
     * On return the parser is positioned on the matching {@code </wpt>} end tag.
     */
    @NonNull
    static Parsed parseWaypoint(@NonNull final XmlPullParser parser, @NonNull final ParseContext ctx)
            throws XmlPullParserException, IOException {
        final Builder b = new Builder(ctx);
        b.parse(parser);
        return b.build();
    }

    // ------------------------------------------------------------------------
    // Builder: accumulates every field we can find inside a wpt, then decides
    // whether to emit a Geocache or a child Waypoint at build() time.
    // ------------------------------------------------------------------------
    private static final class Builder {
        @NonNull private final ParseContext ctx;

        // GPX core
        @Nullable Geopoint coords;
        @Nullable String time;
        @Nullable String name;
        @Nullable String desc;
        @Nullable String cmt;
        @Nullable String sym;
        @Nullable String type;       // local part of wpt:type, e.g. "geocache" / "waypoint"
        @Nullable String subtype;    // after "|", e.g. "traditional cache"
        @Nullable String url;
        @Nullable String urlName;

        // Groundspeak
        @Nullable String gsCacheId;
        @Nullable Boolean gsArchived;
        @Nullable Boolean gsAvailable;
        @Nullable String gsName;
        @Nullable String gsOwnerUserId;
        @Nullable String gsOwnerDisplayName;
        @Nullable CacheType gsType;
        @Nullable CacheSize gsSize;
        @Nullable Float gsDifficulty;
        @Nullable Float gsTerrain;
        @Nullable String gsCountry;
        @Nullable String gsState;
        @Nullable String gsHint;
        @Nullable String gsShortDesc;
        @Nullable String gsLongDesc;
        final List<String> gsAttributes = new ArrayList<>();
        final List<Trackable> gsTrackables = new ArrayList<>();
        final List<LogEntry> gsLogs = new ArrayList<>();

        // GSAK
        @Nullable Boolean watchlist;
        // userData[1..4] — index 0 is unused so indices match GSAK names
        final String[] userData = new String[5];
        @Nullable String gsakParent;
        @Nullable Integer favPoints;
        @Nullable String gsakPersonalNote;
        @Nullable Boolean premium;
        @Nullable String originalLat;
        @Nullable String originalLon;
        @Nullable String gsakGeocodeOverride;
        @Nullable Boolean dnf;
        @Nullable Long visitedDate;

        // c:geo
        boolean wptVisited;
        boolean wptUserDefined;
        boolean wptEmptyCoordinates;
        int assignedEmoji;

        // OpenCaching
        boolean logPasswordRequired;
        @Nullable String descriptionPrefix;
        @Nullable CacheSize ocSize;

        // TerraCaching
        @Nullable String terraName;
        @Nullable String terraOwner;
        @Nullable CacheType terraType;
        @Nullable CacheSize terraSize;
        @Nullable String terraCountry;
        @Nullable String terraState;
        @Nullable String terraDescription;
        @Nullable String terraHint;
        boolean isTerraCache;

        Builder(@NonNull final ParseContext ctx) {
            this.ctx = ctx;
        }

        // --- main parse loop ---------------------------------------------------
        void parse(final XmlPullParser parser) throws XmlPullParserException, IOException {
            coords = UnifiedGPXParser.readLatLon(parser);
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
                handleWptChild(parser);
            }
        }

        private void handleWptChild(final XmlPullParser parser) throws XmlPullParserException, IOException {
            switch (parser.getName()) {
                case "time": time = UnifiedGPXParser.readText(parser); break;
                case "name": name = UnifiedGPXParser.readText(parser); break;
                case "desc": desc = UnifiedGPXParser.readText(parser); break;
                case "cmt":  cmt  = UnifiedGPXParser.readText(parser); break;
                case "sym":
                    final String symText = UnifiedGPXParser.readText(parser);
                    if (symText != null) {
                        sym = symText.toLowerCase(Locale.US);
                    }
                    break;
                case "type":
                    final String typeText = UnifiedGPXParser.readText(parser);
                    if (typeText != null) {
                        final String[] parts = StringUtils.split(typeText, '|');
                        if (parts.length > 0) {
                            type = parts[0].toLowerCase(Locale.US).trim();
                            if (parts.length > 1) {
                                subtype = parts[1].toLowerCase(Locale.US).trim();
                            }
                        }
                    }
                    break;
                case "url":      url     = UnifiedGPXParser.readText(parser); break;
                case "urlname":  urlName = UnifiedGPXParser.readText(parser); break;
                case "link":     parseLink(parser); break;
                case "extensions": parseExtensions(parser); break;
                // extension elements found as direct children (GPX 1.0 style or non-standard):
                case "cache":          parseCacheElement(parser); break;
                case "wptExtension":   parseGsak(parser); break;
                case "visited":        wptVisited = parseBoolText(parser); break;
                case "userdefined":    wptUserDefined |= parseBoolText(parser); break;
                case "originalCoordsEmpty": wptEmptyCoordinates = parseBoolText(parser); break;
                case "cacheExtension": parseCgeoCacheExtension(parser); break;
                case "terracache":     parseTerracache(parser); break;
                default:
                    UnifiedGPXParser.skipSubtree(parser);
                    break;
            }
        }

        private void parseLink(final XmlPullParser parser) throws XmlPullParserException, IOException {
            // GPX 1.1: <link href="..."><text>...</text></link>
            final String hrefAttr = parser.getAttributeValue(null, "href");
            if (hrefAttr != null) {
                url = hrefAttr;
            }
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                switch (parser.getName()) {
                    case "href": url = UnifiedGPXParser.readText(parser); break;
                    case "text": urlName = UnifiedGPXParser.readText(parser); break;
                    default: UnifiedGPXParser.skipSubtree(parser); break;
                }
            }
        }

        private void parseExtensions(final XmlPullParser parser) throws XmlPullParserException, IOException {
            // GPX 1.1: extensions live under <wpt><extensions>...
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                switch (parser.getName()) {
                    case "cache":              parseCacheElement(parser); break;
                    case "wptExtension":       parseGsak(parser); break;
                    case "visited":            wptVisited = parseBoolText(parser); break;
                    case "userdefined":        wptUserDefined |= parseBoolText(parser); break;
                    case "originalCoordsEmpty": wptEmptyCoordinates = parseBoolText(parser); break;
                    case "cacheExtension":     parseCgeoCacheExtension(parser); break;
                    case "terracache":         parseTerracache(parser); break;
                    default: UnifiedGPXParser.skipSubtree(parser); break;
                }
            }
        }

        /** {@code <cache>} is shared between Groundspeak and OpenCaching — disambiguate by namespace. */
        private void parseCacheElement(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final String ns = parser.getNamespace();
            if (ns != null && ns.contains("opencaching")) {
                parseOpenCachingCache(parser);
            } else {
                // default to Groundspeak — they own the bulk of "<cache>" usage in the wild
                parseGroundspeakCache(parser);
            }
        }

        // --- Groundspeak ------------------------------------------------------
        private void parseGroundspeakCache(final XmlPullParser parser) throws XmlPullParserException, IOException {
            gsCacheId = parser.getAttributeValue(null, "id");
            final String archivedAttr = parser.getAttributeValue(null, "archived");
            if (archivedAttr != null) gsArchived = archivedAttr.equalsIgnoreCase("true");
            final String availableAttr = parser.getAttributeValue(null, "available");
            if (availableAttr != null) gsAvailable = availableAttr.equalsIgnoreCase("true");

            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                switch (parser.getName()) {
                    case "name":         gsName = validate(UnifiedGPXParser.readText(parser)); break;
                    case "owner":        gsOwnerUserId = validate(UnifiedGPXParser.readText(parser)); break;
                    case "placed_by":    gsOwnerDisplayName = validate(UnifiedGPXParser.readText(parser)); break;
                    case "type":
                        String typeBody = validate(UnifiedGPXParser.readText(parser));
                        // lab caches wrongly include "Geocache|" prefix
                        if (typeBody.startsWith("Geocache|")) {
                            typeBody = StringUtils.substringAfter(typeBody, "Geocache|").trim();
                        }
                        gsType = CacheType.getByPattern(typeBody);
                        break;
                    case "container":    gsSize = CacheSize.getById(validate(UnifiedGPXParser.readText(parser))); break;
                    case "difficulty":   gsDifficulty = parseFloatOrNull(UnifiedGPXParser.readText(parser)); break;
                    case "terrain":      gsTerrain = parseFloatOrNull(UnifiedGPXParser.readText(parser)); break;
                    case "country":      gsCountry = validate(UnifiedGPXParser.readText(parser)); break;
                    case "state":        gsState = validate(UnifiedGPXParser.readText(parser)); break;
                    case "encoded_hints": gsHint = validate(UnifiedGPXParser.readText(parser)); break;
                    case "short_description": gsShortDesc = validate(UnifiedGPXParser.readText(parser)); break;
                    case "long_description":  gsLongDesc = validate(UnifiedGPXParser.readText(parser)); break;
                    case "attributes":   parseGroundspeakAttributes(parser); break;
                    case "travelbugs":   parseGroundspeakTravelbugs(parser); break;
                    case "logs":         parseGroundspeakLogs(parser); break;
                    default: UnifiedGPXParser.skipSubtree(parser); break;
                }
            }
        }

        private void parseGroundspeakAttributes(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                if ("attribute".equals(parser.getName())) {
                    final String idAttr = parser.getAttributeValue(null, "id");
                    final String incAttr = parser.getAttributeValue(null, "inc");
                    UnifiedGPXParser.readText(parser); // consume + ignore body
                    if (idAttr != null && incAttr != null) {
                        try {
                            final int attrId = Integer.parseInt(idAttr);
                            final boolean active = Integer.parseInt(incAttr) != 0;
                            final CacheAttribute attr = CacheAttribute.getById(attrId);
                            if (attr != null) {
                                gsAttributes.add(attr.getValue(active));
                            }
                        } catch (NumberFormatException ignored) {
                            // skip malformed
                        }
                    }
                } else {
                    UnifiedGPXParser.skipSubtree(parser);
                }
            }
        }

        private void parseGroundspeakTravelbugs(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                if ("travelbug".equals(parser.getName())) {
                    parseGroundspeakTravelbug(parser);
                } else {
                    UnifiedGPXParser.skipSubtree(parser);
                }
            }
        }

        private void parseGroundspeakTravelbug(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final Trackable tb = new Trackable();
            final String ref = parser.getAttributeValue(null, "ref");
            if (ref != null) tb.setGeocode(ref);

            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                if ("name".equals(parser.getName())) {
                    tb.setName(validate(UnifiedGPXParser.readText(parser)));
                } else {
                    UnifiedGPXParser.skipSubtree(parser);
                }
            }
            if (StringUtils.isNotBlank(tb.getGeocode()) && StringUtils.isNotBlank(tb.getName())) {
                gsTrackables.add(tb);
            }
        }

        private void parseGroundspeakLogs(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                if ("log".equals(parser.getName())) {
                    final LogEntry log = parseGroundspeakLog(parser);
                    if (log != null && log.logType != LogType.UNKNOWN) {
                        gsLogs.add(log);
                    }
                } else {
                    UnifiedGPXParser.skipSubtree(parser);
                }
            }
        }

        @Nullable
        private LogEntry parseGroundspeakLog(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final LogEntry.Builder lb = new LogEntry.Builder();
            final String idAttr = parser.getAttributeValue(null, "id");
            if (idAttr != null) {
                try {
                    lb.setId(Integer.parseInt(idAttr));
                } catch (NumberFormatException ignored) {
                    // skip
                }
            }
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                switch (parser.getName()) {
                    case "date":
                        try {
                            lb.setDate(GPXParser.parseDate(UnifiedGPXParser.readText(parser)).getTime());
                        } catch (Exception ignored) {
                            // skip
                        }
                        break;
                    case "type":
                        lb.setLogType(LogType.getByType(validate(UnifiedGPXParser.readText(parser))));
                        break;
                    case "finder":
                        lb.setAuthor(validate(UnifiedGPXParser.readText(parser)));
                        break;
                    case "text":
                        lb.setLog(validate(UnifiedGPXParser.readText(parser)));
                        break;
                    default:
                        UnifiedGPXParser.skipSubtree(parser);
                        break;
                }
            }
            return lb.build();
        }

        // --- GSAK -------------------------------------------------------------
        private void parseGsak(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                final String n = parser.getName();
                if ("Watch".equals(n)) {
                    watchlist = parseBoolText(parser);
                } else if ("UserData".equals(n)) {
                    userData[1] = validate(UnifiedGPXParser.readText(parser));
                } else if ("User2".equals(n) || "User3".equals(n) || "User4".equals(n)) {
                    final int idx = Character.digit(n.charAt(4), 10);
                    userData[idx] = validate(UnifiedGPXParser.readText(parser));
                } else if ("Parent".equals(n)) {
                    gsakParent = UnifiedGPXParser.readText(parser);
                } else if ("FavPoints".equals(n)) {
                    final String v = UnifiedGPXParser.readText(parser);
                    if (v != null) {
                        try {
                            favPoints = Integer.parseInt(v.trim());
                        } catch (NumberFormatException ignored) {
                            // skip
                        }
                    }
                } else if ("GcNote".equals(n)) {
                    gsakPersonalNote = StringUtils.trim(UnifiedGPXParser.readText(parser));
                } else if ("IsPremium".equals(n)) {
                    premium = parseBoolText(parser);
                } else if ("LatBeforeCorrect".equals(n)) {
                    originalLat = UnifiedGPXParser.readText(parser);
                } else if ("LonBeforeCorrect".equals(n)) {
                    originalLon = UnifiedGPXParser.readText(parser);
                } else if ("Code".equals(n)) {
                    final String code = UnifiedGPXParser.readText(parser);
                    if (StringUtils.isNotBlank(code)) {
                        gsakGeocodeOverride = StringUtils.trim(code);
                    }
                } else if ("DNF".equals(n)) {
                    dnf = parseBoolText(parser);
                } else if ("DNFDate".equals(n) || "UserFound".equals(n)) {
                    try {
                        visitedDate = GPXParser.parseDate(UnifiedGPXParser.readText(parser)).getTime();
                    } catch (Exception ignored) {
                        // skip
                    }
                } else if ("Child_ByGSAK".equals(n)) {
                    wptUserDefined |= parseBoolText(parser);
                } else {
                    UnifiedGPXParser.skipSubtree(parser);
                }
            }
        }

        // --- c:geo cacheExtension -------------------------------------------
        private void parseCgeoCacheExtension(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                if ("assignedEmoji".equals(parser.getName())) {
                    final String v = UnifiedGPXParser.readText(parser);
                    if (v != null) {
                        try {
                            assignedEmoji = Integer.parseInt(v.trim());
                        } catch (NumberFormatException ignored) {
                            // skip
                        }
                    }
                } else {
                    UnifiedGPXParser.skipSubtree(parser);
                }
            }
        }

        // --- OpenCaching ------------------------------------------------------
        private void parseOpenCachingCache(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                switch (parser.getName()) {
                    case "requires_password":
                        logPasswordRequired = parseBoolText(parser);
                        break;
                    case "other_code":
                        final String code = StringUtils.trim(UnifiedGPXParser.readText(parser));
                        if (StringUtils.isNotBlank(code)) {
                            descriptionPrefix = Geocache.getAlternativeListingText(code);
                        }
                        break;
                    case "size":
                        final CacheSize s = CacheSize.getById(UnifiedGPXParser.readText(parser));
                        if (s != CacheSize.UNKNOWN) {
                            ocSize = s;
                        }
                        break;
                    default:
                        UnifiedGPXParser.skipSubtree(parser);
                        break;
                }
            }
        }

        // --- TerraCaching -----------------------------------------------------
        private void parseTerracache(final XmlPullParser parser) throws XmlPullParserException, IOException {
            isTerraCache = true;
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                switch (parser.getName()) {
                    case "name":        terraName = StringUtils.trim(UnifiedGPXParser.readText(parser)); break;
                    case "owner":       terraOwner = validate(UnifiedGPXParser.readText(parser)); break;
                    case "style":       terraType = TerraCachingType.getCacheType(UnifiedGPXParser.readText(parser)); break;
                    case "size":        terraSize = CacheSize.getById(UnifiedGPXParser.readText(parser)); break;
                    case "country":     terraCountry = StringUtils.trim(UnifiedGPXParser.readText(parser)); break;
                    case "state":       terraState = StringUtils.trim(UnifiedGPXParser.readText(parser)); break;
                    case "description": terraDescription = trimHtml(UnifiedGPXParser.readText(parser)); break;
                    case "hint":
                        final String h = UnifiedGPXParser.readText(parser);
                        if (h != null) terraHint = cgeo.geocaching.utils.html.HtmlUtils.extractText(h);
                        break;
                    case "logs":        parseTerracachingLogs(parser); break;
                    default: UnifiedGPXParser.skipSubtree(parser); break;
                }
            }
        }

        private void parseTerracachingLogs(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                if ("log".equals(parser.getName())) {
                    final LogEntry log = parseTerracachingLog(parser);
                    if (log != null && log.logType != LogType.UNKNOWN) {
                        gsLogs.add(log);
                    }
                } else {
                    UnifiedGPXParser.skipSubtree(parser);
                }
            }
        }

        @Nullable
        private LogEntry parseTerracachingLog(final XmlPullParser parser) throws XmlPullParserException, IOException {
            final LogEntry.Builder lb = new LogEntry.Builder();
            final String idAttr = parser.getAttributeValue(null, "id");
            if (idAttr != null) {
                try {
                    lb.setId(Integer.parseInt(idAttr));
                } catch (NumberFormatException ignored) {
                    // skip
                }
            }
            final int startDepth = parser.getDepth();
            while (true) {
                final int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) break;
                if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) break;
                if (event != XmlPullParser.START_TAG) continue;
                switch (parser.getName()) {
                    case "date":
                        try {
                            lb.setDate(GPXParser.parseDate(UnifiedGPXParser.readText(parser)).getTime());
                        } catch (Exception ignored) {
                            // skip
                        }
                        break;
                    case "type":
                        lb.setLogType(TerraCachingLogType.getLogType(validate(UnifiedGPXParser.readText(parser))));
                        break;
                    case "user":
                        lb.setAuthor(validate(UnifiedGPXParser.readText(parser)));
                        break;
                    case "entry":
                        lb.setLog(trimHtml(validate(UnifiedGPXParser.readText(parser))));
                        break;
                    default:
                        UnifiedGPXParser.skipSubtree(parser);
                        break;
                }
            }
            return lb.build();
        }

        // --- build (decide cache vs waypoint, populate Geocache) -------------
        Parsed build() {
            // Populate a Geocache with everything we collected, even if it later turns out
            // to be a child waypoint — the existing parser does the same and inspects the
            // partial cache to make its decision.
            final Geocache cache = new Geocache();
            cache.setAttributes(Collections.emptyList());
            cache.setWaypoints(Collections.emptyList());
            cache.setLocation("");
            cache.setDescription("");
            cache.setShortDescription("");
            cache.setHint("");

            if (coords != null) {
                final Geopoint zero = new Geopoint(0, 0);
                // GPX exports use (0,0) as a sentinel for "no coordinates"
                if (!coords.equals(zero)) {
                    cache.setCoords(coords);
                }
            }
            if (time != null) {
                try {
                    cache.setHidden(GPXParser.parseDate(time));
                } catch (ParseException e) {
                    Log.w("UnifiedGPXWaypointParser: invalid time '" + time + "'");
                }
            }
            if (name != null) {
                String content = name.trim();
                if (Strings.CI.startsWith(content, "GCEC") && Strings.CI.contains(ctx.scriptUrl, "extremcaching")) {
                    content = content.substring(2);
                }
                cache.setName(content);
                findGeoCode(cache, cache.getName(), true);
            }
            if (desc != null) {
                cache.setShortDescription(validate(desc));
            }
            if (cmt != null) {
                cache.setDescription(validate(cmt));
            }
            if (sym != null && sym.contains("geocache") && sym.contains("found")) {
                cache.setFound(true);
                cache.setDNF(false);
            }
            setUrl(cache, url);
            setUrlName(cache, urlName);

            // Apply Groundspeak
            if (gsCacheId != null) cache.setCacheId(gsCacheId);
            if (gsArchived != null) cache.setArchived(gsArchived);
            if (gsAvailable != null) cache.setDisabled(!gsAvailable);
            if (gsName != null) cache.setName(gsName);
            if (gsOwnerUserId != null) cache.setOwnerUserId(gsOwnerUserId);
            if (gsOwnerDisplayName != null) cache.setOwnerDisplayName(gsOwnerDisplayName);
            if (gsType != null) cache.setType(gsType);
            if (gsSize != null) cache.setSize(gsSize);
            if (gsDifficulty != null) cache.setDifficulty(gsDifficulty);
            if (gsTerrain != null) cache.setTerrain(gsTerrain);
            applyLocation(cache, gsCountry, gsState);
            if (gsHint != null) cache.setHint(gsHint);
            if (gsShortDesc != null) cache.setShortDescription(gsShortDesc);
            if (gsLongDesc != null) cache.setDescription(gsLongDesc);
            for (final String attr : gsAttributes) {
                cache.getAttributes().add(attr);
            }
            for (final Trackable tb : gsTrackables) {
                cache.addInventoryItem(tb);
            }

            // Apply GSAK
            if (watchlist != null) cache.setOnWatchlist(watchlist);
            if (favPoints != null) cache.setFavoritePoints(favPoints);
            if (gsakPersonalNote != null) cache.setPersonalNote(gsakPersonalNote, true);
            if (premium != null) cache.setPremiumMembersOnly(premium);
            if (gsakGeocodeOverride != null) cache.setGeocode(gsakGeocodeOverride);
            if (dnf != null && !cache.isFound()) cache.setDNF(dnf);
            if (visitedDate != null && cache.getVisitedDate() == 0) cache.setVisitedDate(visitedDate);
            applyOriginalCoordinates(cache);

            // Apply TerraCaching
            if (isTerraCache) {
                if (terraName != null) cache.setName(terraName);
                if (terraOwner != null) cache.setOwnerDisplayName(terraOwner);
                if (terraType != null) cache.setType(terraType);
                if (terraSize != null) cache.setSize(terraSize);
                applyLocation(cache, terraCountry, terraState);
                if (terraDescription != null) cache.setDescription(terraDescription);
                if (terraHint != null) cache.setHint(terraHint);
            }

            // Apply OpenCaching size (only if not already a known size from elsewhere)
            if (ocSize != null) cache.setSize(ocSize);
            cache.setLogPasswordRequired(logPasswordRequired);
            if (StringUtils.isNotBlank(descriptionPrefix)) {
                cache.setDescription(descriptionPrefix + cache.getDescription());
            }

            // Apply c:geo
            if (assignedEmoji != 0) cache.setAssignedEmoji(assignedEmoji);

            // Fall-through geocode resolution from name/desc/cmt if still blank
            if (StringUtils.isBlank(cache.getGeocode())) {
                findGeoCode(cache, name, true);
                findGeoCode(cache, desc, false);
                findGeoCode(cache, cmt, false);
            }
            if (StringUtils.isBlank(cache.getGeocode()) && StringUtils.isNotBlank(name)) {
                cache.setGeocode(name.trim());
            }

            // GSAK UserData1..4 collapses into the personal note when none is set yet
            applyUserDataAsPersonalNote(cache);

            // "GC_WayPoint1" sentinel: clears short description AND sets file-wide flag so
            // the next wpt is recognized as a TerraCaching child.
            if ("GC_WayPoint1".equals(cache.getShortDescription())) {
                cache.setShortDescription("");
                ctx.terraChildWaypoint = true;
            }

            // --- decide: cache or child waypoint? --------------------------
            if (isCache(cache)) {
                return new Parsed(cache, null, new ArrayList<>(gsLogs));
            }
            if (looksLikeChildWaypoint(cache)) {
                final ChildWaypoint cw = buildChildWaypoint(cache);
                if (cw != null) {
                    return new Parsed(null, cw, Collections.emptyList());
                }
            }
            // Not enough information to do anything useful with this wpt
            return Parsed.empty();
        }

        private boolean isCache(final Geocache cache) {
            if (StringUtils.isBlank(cache.getGeocode())) {
                return false;
            }
            if (cache.getCoords() == null) {
                return false;
            }
            // All sym/type info missing → assume cache
            if (type == null && subtype == null && sym == null) {
                return true;
            }
            if (Strings.CS.contains(type, "geocache")) return true;
            if (Strings.CS.contains(sym, "geocache")) return true;
            if (Strings.CI.contains(sym, "waymark")) return true;
            if (Strings.CI.contains(sym, "terracache") && !ctx.terraChildWaypoint) return true;
            return false;
        }

        private boolean looksLikeChildWaypoint(final Geocache cache) {
            return StringUtils.isNotBlank(cache.getName())
                    && (Strings.CI.contains(type, "waypoint") || ctx.terraChildWaypoint);
        }

        @Nullable
        private ChildWaypoint buildChildWaypoint(final Geocache cache) {
            final String cacheName = cache.getName();
            if (cacheName == null || (cacheName.length() <= 2 && StringUtils.isBlank(gsakParent))) {
                return null;
            }
            String parentCode = gsakParent;
            if (StringUtils.isBlank(parentCode)) {
                if (Strings.CI.contains(ctx.scriptUrl, "extremcaching")) {
                    parentCode = cacheName.substring(2);
                } else if (ctx.terraChildWaypoint) {
                    final String code = cache.getGeocode();
                    parentCode = code != null && code.length() > 1
                            ? StringUtils.left(code, code.length() - 1) : code;
                } else {
                    parentCode = "GC" + cacheName.substring(2).toUpperCase(Locale.US);
                }
            }
            if (StringUtils.isBlank(parentCode)) {
                return null;
            }
            final WaypointType wpType = WaypointType.fromGPXString(
                    sym == null ? "" : sym, subtype == null ? "" : subtype);
            final String wpName = StringUtils.defaultString(cache.getShortDescription());
            final Waypoint wp = new Waypoint(wpName, wpType, false);
            if (wptUserDefined) {
                wp.setUserDefined();
            }
            wp.setId(-1);
            wp.setGeocode(parentCode);
            wp.setLookup("---");
            wp.setCoords(cache.getCoords());

            // user-defined waypoints don't have "original empty" coordinates
            if (wptEmptyCoordinates || (!wp.isUserDefined() && wp.getCoords() == null)) {
                wp.setOriginalCoordsEmpty(true);
            }

            // split combined note/user-note (separated by "\n--\n")
            new WaypointUserNoteCombiner(wp).updateNoteAndUserNote(cache.getDescription());
            wp.setVisited(wptVisited);

            final boolean markUserModifiedCoords = wpType == WaypointType.ORIGINAL;
            return new ChildWaypoint(wp, parentCode, cacheName, wptUserDefined, markUserModifiedCoords);
        }

        // --- helpers ---------------------------------------------------------
        private void applyLocation(final Geocache cache, @Nullable final String country, @Nullable final String state) {
            // Existing parser appends country and prepends state.
            if (StringUtils.isNotBlank(country)) {
                if (StringUtils.isBlank(cache.getLocation())) {
                    cache.setLocation(validate(country));
                } else {
                    cache.setLocation(cache.getLocation() + ", " + country.trim());
                }
            }
            if (StringUtils.isNotBlank(state)) {
                if (StringUtils.isBlank(cache.getLocation())) {
                    cache.setLocation(validate(state));
                } else {
                    cache.setLocation(state.trim() + ", " + cache.getLocation());
                }
            }
        }

        private void applyOriginalCoordinates(final Geocache cache) {
            if (StringUtils.isNotEmpty(originalLat) && StringUtils.isNotEmpty(originalLon)) {
                try {
                    final Geopoint original = new Geopoint(
                            Double.parseDouble(originalLat), Double.parseDouble(originalLon));
                    // Create the waypoint manually to avoid LocalizationUtils dependency
                    // on the cache.createOriginalWaypoint helper.
                    final Waypoint wp = new Waypoint("Original Coordinates", WaypointType.ORIGINAL, false);
                    wp.setCoords(original);
                    cache.addOrChangeWaypoint(wp, false);
                    cache.setUserModifiedCoords(true);
                } catch (NumberFormatException e) {
                    Log.w("UnifiedGPXWaypointParser: invalid original coords '" + originalLat + "/" + originalLon + "'");
                }
            }
        }

        private void applyUserDataAsPersonalNote(final Geocache cache) {
            if (StringUtils.isNotBlank(cache.getPersonalNote())) {
                return;
            }
            final StringBuilder buffer = new StringBuilder();
            for (final String d : userData) {
                if (StringUtils.isNotBlank(d)) {
                    buffer.append(' ').append(d);
                }
            }
            final String note = buffer.toString().trim();
            if (StringUtils.isNotBlank(note)) {
                cache.setPersonalNote(note, true);
            }
        }

        private void setUrl(final Geocache cache, @Nullable final String urlValue) {
            if (urlValue == null) return;
            if (StringUtils.isBlank(cache.getGuid())) {
                final MatcherWrapper m = new MatcherWrapper(PATTERN_GUID, urlValue);
                if (m.matches()) {
                    final String guid = m.group(1);
                    if (StringUtils.isNotBlank(guid)) cache.setGuid(guid);
                }
            }
            if (StringUtils.isBlank(cache.getGeocode())) {
                final MatcherWrapper m = new MatcherWrapper(PATTERN_URL_GEOCODE, urlValue);
                if (m.matches()) {
                    final String code = m.group(1);
                    if (StringUtils.isNotBlank(code)) cache.setGeocode(code);
                }
            }
        }

        private void setUrlName(final Geocache cache, @Nullable final String urlNameValue) {
            // Waymarks: when geocode is WMxxxx and name == geocode, prefer the link text
            if (StringUtils.isNotBlank(urlNameValue)
                    && Strings.CS.startsWith(cache.getGeocode(), "WM")
                    && cache.getName().equals(cache.getGeocode())) {
                cache.setName(StringUtils.trim(urlNameValue));
            }
        }

        private void findGeoCode(final Geocache cache, @Nullable final String input,
                                 final boolean useUnknownConnector) {
            if (input == null || StringUtils.isNotBlank(cache.getGeocode())) {
                return;
            }
            final String trimmed = input.trim();
            final MatcherWrapper m = new MatcherWrapper(PATTERN_GEOCODE, trimmed);
            if (!m.find()) {
                return;
            }
            final String geocode = m.group();
            if (geocode.length() != trimmed.length()
                    && !Character.isWhitespace(trimmed.charAt(geocode.length()))) {
                // geocode appears glued to following text — reject (matches existing behavior)
                return;
            }
            try {
                final IConnector connector = ConnectorFactory.getConnector(geocode);
                if (!connector.equals(ConnectorFactory.UNKNOWN_CONNECTOR)) {
                    cache.setGeocode(geocode);
                    return;
                }
            } catch (Throwable t) {
                // ConnectorFactory may need Android context; fall through to the trimmed
                // name fallback rather than crash the entire parse.
                Log.w("UnifiedGPXWaypointParser: ConnectorFactory unavailable: " + t.getMessage());
            }
            if (useUnknownConnector) {
                cache.setGeocode(trimmed);
            }
        }
    }

    // --- shared static helpers -----------------------------------------------
    @NonNull
    private static String validate(@Nullable final String input) {
        if (input == null) {
            return "";
        }
        if ("nil".equalsIgnoreCase(input)) {
            return "";
        }
        return input.trim();
    }

    @Nullable
    private static Float parseFloatOrNull(@Nullable final String input) {
        if (input == null) return null;
        try {
            return Float.parseFloat(input.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean parseBoolText(final XmlPullParser parser) throws XmlPullParserException, IOException {
        final String txt = UnifiedGPXParser.readText(parser);
        return Boolean.parseBoolean(txt == null ? "" : txt.trim());
    }

    private static String trimHtml(@Nullable final String html) {
        return html == null ? "" :
                StringUtils.trim(Strings.CS.removeEnd(Strings.CS.removeStart(html, "<br>"), "<br>"));
    }
}
