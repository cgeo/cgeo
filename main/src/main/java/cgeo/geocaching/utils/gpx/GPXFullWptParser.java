package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
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
import cgeo.geocaching.models.ICoordinate;
import cgeo.geocaching.models.NamedGeoCoordinate;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.WaypointUserNoteCombiner;
import cgeo.geocaching.utils.EmojiUtilsLegacyMigration;
import cgeo.geocaching.utils.html.HtmlUtils;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

/** Parses complete geocaches, waypoints and fallback coordinates for {@link GPXParser}. */
final class GPXFullWptParser {

    // Keep GUID-like Adventure Lab codes intact; provider recognition is delegated to ConnectorFactory.
    private static final Pattern PATTERN_GEOCODE = Pattern.compile("(?<![\\p{L}\\p{N}_-])[A-Z0-9][A-Z0-9_-]*(?![\\p{L}\\p{N}_-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_URL_GEOCODE = Pattern.compile("[?&]wp=([^&#]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_URL_GUID = Pattern.compile("[?&]guid=([0-9a-f-]+)", Pattern.CASE_INSENSITIVE);

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

    // Additional data for the most recently parsed entity, not stored in the entity itself.
    private List<LogEntry> parsedLogs;
    private String parsedParentGeocode;

    /** Clears all parsing state, including the cross-file name index. */
    void reset() {
        nameToGeocodeIndex.clear();
        resetDocument();
    }

    /** Clears document-specific state while retaining the cross-file name index. */
    void resetDocument() {
        terraChildWaypoint = false;
        parsedLogs = null;
        parsedParentGeocode = null;
    }

    /** Logs from the most recently parsed geocache, or null when none were parsed. */
    @Nullable
    List<LogEntry> getParsedLogs() {
        return parsedLogs;
    }

    /** Unnormalized parent candidate for the most recently parsed waypoint, or null when unresolved. */
    @Nullable
    String getParsedParentGeocode() {
        return parsedParentGeocode;
    }

    /**
     * Parses a scanned wpt/rtept/trkpt subtree into a geocache, waypoint, named coordinate or plain coordinate.
     */
    @NonNull
    ICoordinate parse(@NonNull final XmlNode wptNode, @Nullable final String scriptUrl) {
        parsedLogs = null;
        parsedParentGeocode = null;
        final Geopoint coords = GPXUtils.gpxNodeReadLatLon(wptNode);

        final String rawName = normalizeName(wptNode.getChildValue("name"), scriptUrl);
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

        final XmlNode base = GPXUtils.extensionsBase(wptNode);
        final boolean isGeocache = Strings.CI.contains(type, "geocache")
                || Strings.CI.contains(sym, "geocache")
                || Strings.CI.contains(sym, "waymark")
                || (!wasTerraChildWaypoint && (Strings.CI.contains(sym, "terracache")
                    || base.hasChild("cache") || base.hasChild("terracache")));
        final boolean isWaypoint = !isGeocache && (Strings.CI.contains(type, "waypoint") || wasTerraChildWaypoint);

        if (isTerraChildWaypointMarker) {
            this.terraChildWaypoint = true;
        }

        if (isGeocache) {
            return parseGeocache(wptNode, coords, rawName, desc, cmt);
        } else if (isWaypoint) {
            return parseWaypoint(wptNode, coords, rawName, sym, subtype, wasTerraChildWaypoint, scriptUrl);
        } else {
            // fallback: named or plain coordinate
            return parseFallbackCoordinate(rawName, wptNode);
        }
    }

    /** Parses an entry without geocache or waypoint classification. */
    private static ICoordinate parseFallbackCoordinate(final String rawName, final XmlNode wptNode) {
        final Geopoint coords = XmlUtils.parseGeopoint(GPXUtils.gpxNodeAttrValue(wptNode, "lat", null), GPXUtils.gpxNodeAttrValue(wptNode, "lon", null), true);
        final Float elevation = XmlUtils.parseFloat(wptNode.getChildValue("ele"));
        if (StringUtils.isBlank(rawName) && elevation == null) {
            return coords;
        }
        final NamedGeoCoordinate named = new NamedGeoCoordinate();
        named.setCoords(coords);
        if (elevation != null) {
            named.setElevation(elevation);
        }
        if (StringUtils.isNotBlank(rawName)) {
            named.setName(rawName.trim());
        }
        return named;
    }

    /**
     * A {@code wptType} classified as a geocache.
     *
     * @param coords may be {@code null} when the GPX has no valid coordinate pair
     */
    private Geocache parseGeocache(final XmlNode wptNode, @Nullable final Geopoint coords, final String rawName,
                                 final String desc, final String cmt) {

        final Geocache cache = createCache();
        cache.setCoords(coords);
        final String geocode = resolveGeocode(wptNode, rawName, desc, cmt);
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
        final Date hidden = XmlUtils.parseDate(timeText);
        if (hidden != null) {
            cache.setHidden(hidden);
        }
        final String symbol = wptNode.getChildValue("sym");
        if (Strings.CI.contains(symbol, "geocache") && Strings.CI.contains(symbol, "found")) {
            cache.setFound(true);
            cache.setDNF(false);
        }
        final String url = GPXUtils.readWaypointUrl(wptNode);
        final String guid = matchUrl(PATTERN_URL_GUID, url);
        if (guid != null) {
            cache.setGuid(guid);
        }

        parsedLogs = parseGeocacheExtensions(wptNode, cache);
        final String urlName = StringUtils.defaultIfBlank(wptNode.getChildValue("urlname"), GPXUtils.gpxNodeChildText(wptNode.getChild("link"), "text", null));
        if (Strings.CI.startsWith(cache.getGeocode(), "WM") && cache.getName().equalsIgnoreCase(cache.getGeocode()) && StringUtils.isNotBlank(urlName)) {
            cache.setName(urlName.trim());
        }
        if ("GC_WayPoint1".equals(cache.getShortDescription())) {
            cache.setShortDescription("");
        }
        if (ConnectorFactory.getConnector(cache.getGeocode()) instanceof GCConnector) {
            cache.setCacheId(Long.toString(GCUtils.gcCodeToGcId(cache.getGeocode())));
        }

        if (StringUtils.isNotBlank(cache.getGeocode()) && StringUtils.isNotBlank(cache.getName())) {
            nameToGeocodeIndex.put(cache.getName().trim().toLowerCase(Locale.US), cache.getGeocode());
        }

        return cache;
    }

    /**
     * Resolves a geocode purely from GPX-local information: first a geocode-looking pattern in the
     * name, then in {@code desc}, then in {@code cmt}, then (as last resort) the trimmed name verbatim.
     */
    @Nullable
    private static String resolveGeocode(final XmlNode wptNode, final String rawName, final String desc, final String cmt) {
        String geocode = findGeoCode(rawName);
        if (geocode == null) {
            geocode = matchUrl(PATTERN_URL_GEOCODE, GPXUtils.readWaypointUrl(wptNode));
        }
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

    private static String normalizeName(final String name, final String scriptUrl) {
        final String trimmed = StringUtils.trim(name);
        return Strings.CI.contains(scriptUrl, "extremcaching") && Strings.CI.startsWith(trimmed, "GCEC") ? trimmed.substring(2) : trimmed;
    }

    @Nullable
    private static String matchUrl(final Pattern pattern, @Nullable final String url) {
        if (url == null) {
            return null;
        }
        final Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** handles a {@code wptType} classified as a waypoint. */
    private Waypoint parseWaypoint(final XmlNode wptNode, @Nullable final Geopoint coords, final String rawName,
                                   final String sym, final String subtype, final boolean isTerraChildWaypoint, final String scriptUrl) {

        final String parentGeocodeCandidate = resolveParentGeocode(wptNode, rawName, isTerraChildWaypoint, scriptUrl);

        final String description = wptNode.getChildValue("desc");
        final String name = "GC_WayPoint1".equals(StringUtils.trim(description)) ? ""
                : validate(StringUtils.defaultIfBlank(description, StringUtils.trimToEmpty(rawName)));
        final XmlNode base = GPXUtils.extensionsBase(wptNode);
        final Waypoint waypoint = new Waypoint(name, WaypointType.fromGPXString(sym == null ? "" : sym, subtype), parseWaypointUserDefined(base));
        waypoint.setId(Waypoint.NEW_ID);
        waypoint.setCoords(coords);
        waypoint.setLookup("---"); // GPX has no lookup code
        if (parentGeocodeCandidate != null) {
            waypoint.setGeocode(parentGeocodeCandidate);
        }

        parseCgeoExtension(base, waypoint);
        if (!waypoint.isUserDefined() && coords == null) {
            waypoint.setOriginalCoordsEmpty(true);
        }
        waypoint.setPrefix(resolveWaypointPrefix(rawName, parentGeocodeCandidate, waypoint.isUserDefined()));

        final String note = wptNode.getChildValue("cmt");
        if (StringUtils.isNotBlank(note)) {
            new WaypointUserNoteCombiner(waypoint).updateNoteAndUserNote(validate(note));
        }

        parsedParentGeocode = parentGeocodeCandidate;
        return waypoint;
    }

    private static String resolveWaypointPrefix(final String rawName, @Nullable final String parentGeocode, final boolean userDefined) {
        String prefix = StringUtils.trimToEmpty(rawName);
        if (userDefined) {
            if (StringUtils.length(parentGeocode) > 2 && Strings.CI.endsWith(prefix, parentGeocode.substring(2))) {
                prefix = prefix.substring(0, prefix.length() - parentGeocode.length() + 2);
            }
            prefix = Strings.CI.removeStart(prefix, Waypoint.PREFIX_OWN + "-");
        }
        return ConnectorFactory.getConnector(parentGeocode).getWaypointPrefix(prefix);
    }

    /**
     * Best-effort resolution of a waypoint's parent geocache geocodefrom information available in the GPX document itself
     */
    @Nullable
    private String resolveParentGeocode(final XmlNode wptNode, final String rawName, final boolean isTerraChildWaypoint, final String scriptUrl) {
        final XmlNode extensions = GPXUtils.extensionsBase(wptNode);
        final XmlNode gsakExt = GPXUtils.gpxNodeChild(extensions, "wptExtension", GSAK_NS);
        final String gsakParent = GPXUtils.gpxNodeChildText(gsakExt, "Parent", GSAK_NS);
        if (StringUtils.isNotBlank(gsakParent)) {
            return nameToGeocodeIndex.getOrDefault(gsakParent.trim().toLowerCase(Locale.US), gsakParent.trim());
        }

        final String trimmedName = StringUtils.trim(rawName);
        if (StringUtils.isBlank(trimmedName)) {
            return null;
        }

        if (isTerraChildWaypoint) {
            return trimmedName.length() > 1 ? trimmedName.substring(0, trimmedName.length() - 1) : null;
        }

        if (trimmedName.length() > 2) {
            if (Strings.CI.contains(scriptUrl, "extremcaching")) {
                return trimmedName.substring(2);
            }
            return "GC" + trimmedName.substring(2).toUpperCase(Locale.US);
        }

        return nameToGeocodeIndex.get(trimmedName.toLowerCase(Locale.US));
    }

    // ---------------------------------------------------------------------------------------------------
    // extension parsing, one method per source
    // ---------------------------------------------------------------------------------------------------

    @Nullable
    private List<LogEntry> parseGeocacheExtensions(final XmlNode wptNode, final Geocache cache) {
        final XmlNode base = GPXUtils.extensionsBase(wptNode);
        if (base == null) {
            return null;
        }
        parseGroundspeakExtension(base, cache);
        parseGsakExtension(base, cache);
        parseTerraCachingExtension(base, cache);
        parseCgeoExtension(base, cache);
        parseOpenCachingExtension(base, cache);
        final List<LogEntry> logs = new ArrayList<>();
        for (final XmlNode child : base.getChildrenInOrder()) {
            final List<LogEntry> sourceLogs;
            if ("cache".equals(child.getLocalName())) {
                sourceLogs = parseGroundspeakLogs(child, ConnectorFactory.getConnector(cache.getGeocode()) instanceof GCConnector);
            } else if ("terracache".equals(child.getLocalName())) {
                sourceLogs = parseTerraCachingLogs(child);
            } else {
                continue;
            }
            if (sourceLogs != null) {
                logs.addAll(sourceLogs);
            }
        }
        return logs.isEmpty() ? null : logs;
    }

    /**
     * Groundspeak cache extension, used by geocaching.com pocket queries and most third-party tools (GSAK, ...).
     * Schema/namespace (any of 3 historic versions, unified here by local name only): PQ 1.1
     * {@code http://www.groundspeak.com/cache/1/1}, PQ 1.0.1 {@code http://www.groundspeak.com/cache/1/0/1},
     * PQ 1.0 {@code http://www.groundspeak.com/cache/1/0}. Element {@code <cache>}.
     */
    private void parseGroundspeakExtension(final XmlNode base, final Geocache cache) {
        final XmlNode gcCache = GPXUtils.gpxNodeChild(base, "cache", GROUNDSPEAK_NS);
        if (gcCache == null) {
            return;
        }
        final String id = GPXUtils.gpxNodeAttrValue(gcCache, "id", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(id)) {
            cache.setCacheId(id);
        }
        final String archived = GPXUtils.gpxNodeAttrValue(gcCache, "archived", GROUNDSPEAK_NS);
        if (archived != null) {
            cache.setArchived("true".equalsIgnoreCase(archived));
        }
        final String available = GPXUtils.gpxNodeAttrValue(gcCache, "available", GROUNDSPEAK_NS);
        if (available != null) {
            cache.setDisabled(!"true".equalsIgnoreCase(available));
        }

        final String name = GPXUtils.gpxNodeChildText(gcCache, "name", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(name)) {
            cache.setName(validate(name));
        }
        final String owner = GPXUtils.gpxNodeChildText(gcCache, "owner", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(owner)) {
            cache.setOwnerUserId(validate(owner));
        }
        final String placedBy = GPXUtils.gpxNodeChildText(gcCache, "placed_by", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(placedBy)) {
            cache.setOwnerDisplayName(validate(placedBy));
        }
        final String gcType = GPXUtils.gpxNodeChildText(gcCache, "type", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(gcType)) {
            String body = validate(gcType);
            if (body.startsWith("Geocache|")) {
                body = StringUtils.substringAfter(body, "Geocache|").trim();
            }
            cache.setType(CacheType.getByPattern(body));
        }
        final String container = GPXUtils.gpxNodeChildText(gcCache, "container", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(container)) {
            cache.setSize(CacheSize.getById(validate(container)));
        }
        final Float difficulty = XmlUtils.parseFloat(GPXUtils.gpxNodeChildText(gcCache, "difficulty", GROUNDSPEAK_NS));
        if (difficulty != null) {
            cache.setDifficulty(difficulty);
        }
        final Float terrain = XmlUtils.parseFloat(GPXUtils.gpxNodeChildText(gcCache, "terrain", GROUNDSPEAK_NS));
        if (terrain != null) {
            cache.setTerrain(terrain);
        }
        final String country = GPXUtils.gpxNodeChildText(gcCache, "country", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(country)) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(country) : cache.getLocation() + ", " + country.trim());
        }
        final String state = GPXUtils.gpxNodeChildText(gcCache, "state", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(state) && StringUtils.isNotEmpty(state.trim())) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(state) : state.trim() + ", " + cache.getLocation());
        }
        final String hints = GPXUtils.gpxNodeChildText(gcCache, "encoded_hints", GROUNDSPEAK_NS);
        if (hints != null) {
            cache.setHint(validate(hints));
        }
        final String shortDesc = GPXUtils.gpxNodeChildText(gcCache, "short_description", GROUNDSPEAK_NS);
        if (shortDesc != null) {
            cache.setShortDescription(validate(shortDesc));
        }
        final String longDesc = GPXUtils.gpxNodeChildText(gcCache, "long_description", GROUNDSPEAK_NS);
        if (longDesc != null) {
            cache.setDescription(validate(longDesc));
        }

        parseGroundspeakAttributes(gcCache, cache);
        parseGroundspeakTravelbugs(gcCache, cache);
    }

    /**
     * Groundspeak logs ({@code <groundspeak:logs><groundspeak:log>...}). Same schema/namespace as
     * {@link #parseGroundspeakExtension}.
     */
    @Nullable
    private List<LogEntry> parseGroundspeakLogs(final XmlNode gcCache, final boolean gcConnector) {
        final XmlNode logsNode = GPXUtils.gpxNodeChild(gcCache, "logs", GROUNDSPEAK_NS);
        if (logsNode == null) {
            return null;
        }
        final List<XmlNode> logNodes = GPXUtils.gpxNodeChildren(logsNode, "log");
        if (logNodes == null) {
            return null;
        }
        final List<LogEntry> result = new ArrayList<>();
        for (final XmlNode logNode : logNodes) {
            final LogEntry.Builder builder = new LogEntry.Builder();
            final String idText = GPXUtils.gpxNodeAttrValue(logNode, "id", GROUNDSPEAK_NS);
            if (idText != null) {
                try {
                    builder.setId(Integer.parseInt(idText.trim()));
                    if (gcConnector) {
                        builder.setServiceLogId(GCUtils.logIdToLogCode(builder.getId()));
                    }
                } catch (final NumberFormatException ignored) {
                    // ignore malformed id
                }
            }
            final Date date = XmlUtils.parseDate(GPXUtils.gpxNodeChildText(logNode, "date", GROUNDSPEAK_NS));
            if (date != null) {
                builder.setDate(date.getTime());
            }
            final String typeText = GPXUtils.gpxNodeChildText(logNode, "type", GROUNDSPEAK_NS);
            if (typeText != null) {
                builder.setLogType(LogType.getByType(validate(typeText)));
            }
            final String finder = GPXUtils.gpxNodeChildText(logNode, "finder", GROUNDSPEAK_NS);
            if (finder != null) {
                builder.setAuthor(validate(finder));
            }
            final String text = GPXUtils.gpxNodeChildText(logNode, "text", GROUNDSPEAK_NS);
            if (text != null) {
                builder.setLog(validate(text));
            }
            final LogEntry log = builder.build();
            if (log.logType != LogType.UNKNOWN) {
                result.add(log);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private void parseGroundspeakAttributes(final XmlNode gcCache, final Geocache cache) {
        final XmlNode attributes = GPXUtils.gpxNodeChild(gcCache, "attributes", GROUNDSPEAK_NS);
        if (attributes == null) {
            return;
        }
        final List<XmlNode> attributeNodes = GPXUtils.gpxNodeChildren(attributes, "attribute");
        if (attributeNodes == null) {
            return;
        }
        final List<String> result = new ArrayList<>();
        for (final XmlNode attributeNode : attributeNodes) {
            final String idText = GPXUtils.gpxNodeAttrValue(attributeNode, "id", GROUNDSPEAK_NS);
            final String incText = GPXUtils.gpxNodeAttrValue(attributeNode, "inc", GROUNDSPEAK_NS);
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
        final XmlNode travelbugs = GPXUtils.gpxNodeChild(gcCache, "travelbugs", GROUNDSPEAK_NS);
        if (travelbugs == null) {
            return;
        }
        final List<XmlNode> tbNodes = GPXUtils.gpxNodeChildren(travelbugs, "travelbug");
        if (tbNodes == null) {
            return;
        }
        for (final XmlNode tbNode : tbNodes) {
            final Trackable trackable = new Trackable();
            final String ref = GPXUtils.gpxNodeAttrValue(tbNode, "ref", GROUNDSPEAK_NS);
            if (ref != null) {
                trackable.setGeocode(ref);
            }
            final String tbName = GPXUtils.gpxNodeChildText(tbNode, "name", GROUNDSPEAK_NS);
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
        final XmlNode gsak = GPXUtils.gpxNodeChild(base, "wptExtension", GSAK_NS);
        if (gsak == null) {
            return;
        }
        final String watch = GPXUtils.gpxNodeChildText(gsak, "Watch", GSAK_NS);
        if (watch != null) {
            cache.setOnWatchlist(Boolean.parseBoolean(watch.trim()));
        }
        final String favPoints = GPXUtils.gpxNodeChildText(gsak, "FavPoints", GSAK_NS);
        if (favPoints != null) {
            try {
                cache.setFavoritePoints(Integer.parseInt(favPoints.trim()));
            } catch (final NumberFormatException ignored) {
                // ignore malformed favorite points
            }
        }
        final String gcNote = GPXUtils.gpxNodeChildText(gsak, "GcNote", GSAK_NS);
        if (StringUtils.isNotBlank(gcNote)) {
            cache.setPersonalNote(StringUtils.trim(gcNote), true);
        }
        final String isPremium = GPXUtils.gpxNodeChildText(gsak, "IsPremium", GSAK_NS);
        if (isPremium != null) {
            cache.setPremiumMembersOnly(Boolean.parseBoolean(isPremium.trim()));
        }
        final String code = GPXUtils.gpxNodeChildText(gsak, "Code", GSAK_NS);
        if (StringUtils.isNotBlank(code)) {
            cache.setGeocode(StringUtils.trim(code));
        }
        final String dnf = GPXUtils.gpxNodeChildText(gsak, "DNF", GSAK_NS);
        if (dnf != null && !cache.isFound()) {
            cache.setDNF(Boolean.parseBoolean(dnf.trim()));
        }
        final String dnfDate = GPXUtils.gpxNodeChildText(gsak, "DNFDate", GSAK_NS);
        if (dnfDate != null && cache.getVisitedDate() == 0) {
            final Date parsed = XmlUtils.parseDate(dnfDate);
            if (parsed != null) {
                cache.setVisitedDate(parsed.getTime());
            }
        }
        final String userFound = GPXUtils.gpxNodeChildText(gsak, "UserFound", GSAK_NS);
        if (userFound != null && cache.getVisitedDate() == 0) {
            final Date parsed = XmlUtils.parseDate(userFound);
            if (parsed != null) {
                cache.setVisitedDate(parsed.getTime());
            }
        }

        final StringBuilder userDataNote = new StringBuilder();
        appendUserData(userDataNote, GPXUtils.gpxNodeChildText(gsak, "UserData", GSAK_NS));
        for (int i = 2; i <= 4; i++) {
            appendUserData(userDataNote, GPXUtils.gpxNodeChildText(gsak, "User" + i, GSAK_NS));
        }
        if (StringUtils.isBlank(cache.getPersonalNote()) && userDataNote.length() > 0) {
            cache.setPersonalNote(userDataNote.toString().trim(), true);
        }
        final Geopoint originalCoords = XmlUtils.parseGeopoint(GPXUtils.gpxNodeChildText(gsak, "LatBeforeCorrect", GSAK_NS), GPXUtils.gpxNodeChildText(gsak, "LonBeforeCorrect", GSAK_NS), false);
        if (originalCoords != null) {
            final Waypoint original = new Waypoint(WaypointType.ORIGINAL.gpx, WaypointType.ORIGINAL, false);
            original.setGeocode(cache.getGeocode());
            original.setCoords(originalCoords);
            cache.setWaypoints(Collections.singletonList(original));
            cache.setUserModifiedCoords(true);
        }
    }

    private static boolean parseWaypointUserDefined(final XmlNode base) {
        boolean userDefined = false;
        for (final XmlNode child : base.getChildrenInOrder()) {
            if ("userdefined".equals(child.getLocalName())) {
                userDefined = Boolean.parseBoolean(StringUtils.trim(child.getValue()));
            } else if ("wptExtension".equals(child.getLocalName())) {
                for (final XmlNode field : child.getChildrenInOrder()) {
                    if ("Child_ByGSAK".equals(field.getLocalName())) {
                        userDefined |= Boolean.parseBoolean(StringUtils.trim(field.getValue()));
                    }
                }
            }
        }
        return userDefined;
    }

    private static void appendUserData(final StringBuilder buffer, final String userData) {
        if (StringUtils.isNotBlank(userData)) {
            buffer.append(' ').append(userData);
        }
    }

    /** TerraCaching extension. */
    private void parseTerraCachingExtension(final XmlNode base, final Geocache cache) {
        final XmlNode terraCache = GPXUtils.gpxNodeChild(base, "terracache", TERRA_NS);
        if (terraCache == null) {
            return;
        }
        final String name = GPXUtils.gpxNodeChildText(terraCache, "name", TERRA_NS);
        if (StringUtils.isNotBlank(name)) {
            cache.setName(StringUtils.trim(name));
        }
        final String owner = GPXUtils.gpxNodeChildText(terraCache, "owner", TERRA_NS);
        if (StringUtils.isNotBlank(owner)) {
            cache.setOwnerDisplayName(validate(owner));
        }
        final String style = GPXUtils.gpxNodeChildText(terraCache, "style", TERRA_NS);
        if (StringUtils.isNotBlank(style)) {
            cache.setType(TerraCachingType.getCacheType(style));
        }
        final String size = GPXUtils.gpxNodeChildText(terraCache, "size", TERRA_NS);
        if (StringUtils.isNotBlank(size)) {
            cache.setSize(CacheSize.getById(size));
        }
        final String country = GPXUtils.gpxNodeChildText(terraCache, "country", TERRA_NS);
        if (StringUtils.isNotBlank(country)) {
            cache.setLocation(StringUtils.trim(country));
        }
        final String state = GPXUtils.gpxNodeChildText(terraCache, "state", TERRA_NS);
        if (StringUtils.isNotBlank(state) && StringUtils.isNotEmpty(state.trim())) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(state) : state.trim() + ", " + cache.getLocation());
        }
        final String description = GPXUtils.gpxNodeChildText(terraCache, "description", TERRA_NS);
        if (description != null) {
            cache.setDescription(trimHtml(description));
        }
        final String hint = GPXUtils.gpxNodeChildText(terraCache, "hint", TERRA_NS);
        if (hint != null) {
            cache.setHint(HtmlUtils.extractText(hint));
        }
    }

    /** TerraCaching logs ({@code <terracache><logs><log>...}). */
    @Nullable
    private List<LogEntry> parseTerraCachingLogs(final XmlNode terraCache) {
        final XmlNode logsNode = GPXUtils.gpxNodeChild(terraCache, "logs", TERRA_NS);
        if (logsNode == null) {
            return null;
        }
        final List<XmlNode> logNodes = GPXUtils.gpxNodeChildren(logsNode, "log");
        if (logNodes == null) {
            return null;
        }
        final List<LogEntry> result = new ArrayList<>();
        for (final XmlNode logNode : logNodes) {
            final LogEntry.Builder builder = new LogEntry.Builder();
            final String idText = GPXUtils.gpxNodeAttrValue(logNode, "id", TERRA_NS);
            if (idText != null) {
                try {
                    builder.setId(Integer.parseInt(idText.trim()));
                } catch (final NumberFormatException ignored) {
                    // ignore malformed id
                }
            }
            final Date date = XmlUtils.parseDate(GPXUtils.gpxNodeChildText(logNode, "date", TERRA_NS));
            if (date != null) {
                builder.setDate(date.getTime());
            }
            final String typeText = GPXUtils.gpxNodeChildText(logNode, "type", TERRA_NS);
            if (typeText != null) {
                builder.setLogType(TerraCachingLogType.getLogType(validate(typeText)));
            }
            final String finder = GPXUtils.gpxNodeChildText(logNode, "user", TERRA_NS);
            if (finder != null) {
                builder.setAuthor(validate(finder));
            }
            final String text = GPXUtils.gpxNodeChildText(logNode, "entry", TERRA_NS);
            if (text != null) {
                builder.setLog(trimHtml(validate(text)));
            }
            final LogEntry log = builder.build();
            if (log.logType != LogType.UNKNOWN) {
                result.add(log);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /** c:geo's own extension */
    private void parseCgeoExtension(final XmlNode base, final Geocache cache) {
        final String assignedEmojiText = GPXUtils.gpxNodeChildText(GPXUtils.gpxNodeChild(base, "cacheExtension", CGEO_NS), "assignedEmoji", CGEO_NS);
        if (StringUtils.isNotBlank(assignedEmojiText)) {
            cache.setAssignedEmoji(EmojiUtilsLegacyMigration.parseGpxAssignedEmoji(assignedEmojiText));
        }
    }

    /** c:geo waypoint fields are siblings of cacheExtension, not children of it. */
    private void parseCgeoExtension(final XmlNode base, final Waypoint waypoint) {
        for (final XmlNode child : base.getChildrenInOrder()) {
            if ("visited".equals(child.getLocalName())) {
                waypoint.setVisited(Boolean.parseBoolean(StringUtils.trim(child.getValue())));
            } else if ("originalCoordsEmpty".equals(child.getLocalName())) {
                waypoint.setOriginalCoordsEmpty(Boolean.parseBoolean(StringUtils.trim(child.getValue())));
            }
        }
    }

    /**
     * Opencaching extension. Schema/namespace: {@code https://github.com/opencaching/gpx-extension-v1}. Element
     * {@code <cache>}.
     */
    private void parseOpenCachingExtension(final XmlNode base, final Geocache cache) {
        final XmlNode ocCache = GPXUtils.gpxNodeChild(base, "cache", OPENCACHING_NS);
        if (ocCache == null) {
            return;
        }
        final String requiresPassword = GPXUtils.gpxNodeChildText(ocCache, "requires_password", OPENCACHING_NS);
        if (requiresPassword != null) {
            cache.setLogPasswordRequired(Boolean.parseBoolean(requiresPassword.trim()));
        }
        final String otherCode = GPXUtils.gpxNodeChildText(ocCache, "other_code", OPENCACHING_NS);
        if (StringUtils.isNotBlank(otherCode)) {
            cache.setDescription(Geocache.getAlternativeListingText(otherCode.trim()) + cache.getDescription());
        }
        final String size = GPXUtils.gpxNodeChildText(ocCache, "size", OPENCACHING_NS);
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
        final Matcher matcher = PATTERN_GEOCODE.matcher(input);
        while (matcher.find()) {
            final String geocode = matcher.group().toUpperCase(Locale.US);
            if (ConnectorFactory.getConnector(geocode) != ConnectorFactory.UNKNOWN_CONNECTOR) {
                return geocode;
            }
        }
        return null;
    }

}
