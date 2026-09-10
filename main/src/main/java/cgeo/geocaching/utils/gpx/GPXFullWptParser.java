package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinate;
import cgeo.geocaching.models.NamedGeoCoordinate;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.WaypointUserNoteCombiner;
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


    //Extensions
    private final List<IGPXExtension> gpxExtensions = List.of(
        new GroundspeakGPXExtension(),
        new GSAKGPXExtension(),
        new TerraCachingGPXExtension(),
        new CgeoGPXExtension(),
        new OpenCachingGPXExtension()
    );

    private static final Set<String> CGEO_NS = Set.of(
        "http://www.cgeo.org/wptext/1/0"
    );

    private static final Set<String> OPENCACHING_NS = Set.of(
        "https://github.com/opencaching/gpx-extension-v1"
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
        final Float elevation = GPXUtils.gpxNodeChildFloat(wptNode, "ele", null, null);
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
            cache.setShortDescription(XmlUtils.validate(desc));
        }
        if (StringUtils.isNotBlank(cmt)) {
            cache.setDescription(XmlUtils.validate(cmt));
        }
        final Date hidden = GPXUtils.gpxNodeChildDate(wptNode, "time", null, null);
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
                : XmlUtils.validate(StringUtils.defaultIfBlank(description, StringUtils.trimToEmpty(rawName)));
        final XmlNode base = GPXUtils.extensionsBase(wptNode);
        final Waypoint waypoint = new Waypoint(name, WaypointType.fromGPXString(sym == null ? "" : sym, subtype), false);
        waypoint.setId(Waypoint.NEW_ID);
        waypoint.setCoords(coords);
        waypoint.setLookup("---"); // GPX has no lookup code
        if (parentGeocodeCandidate != null) {
            waypoint.setGeocode(parentGeocodeCandidate);
        }

        for (IGPXExtension extension : gpxExtensions) {
            extension.enrichWaypoint(base, waypoint);
        }

        if (!waypoint.isUserDefined() && coords == null) {
            waypoint.setOriginalCoordsEmpty(true);
        }
        waypoint.setPrefix(resolveWaypointPrefix(rawName, parentGeocodeCandidate, waypoint.isUserDefined()));

        final String note = wptNode.getChildValue("cmt");
        if (StringUtils.isNotBlank(note)) {
            new WaypointUserNoteCombiner(waypoint).updateNoteAndUserNote(XmlUtils.validate(note));
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
        final XmlNode gsakExt = GPXUtils.gpxNodeChild(extensions, "wptExtension", GSAKGPXExtension.GSAK_NS);
        final String gsakParent = GPXUtils.gpxNodeChildText(gsakExt, "Parent", GSAKGPXExtension.GSAK_NS);
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
        for (final IGPXExtension extension : gpxExtensions) {
            extension.enrichGeocache(base, cache);
        }

        final List<LogEntry> logs = new ArrayList<>();
        for (IGPXExtension extension : gpxExtensions) {
            final List<LogEntry> candidate = extension.extractLogs(base, cache);
            if (candidate != null) {
                logs.addAll(candidate);
            }
        }
        return logs.isEmpty() ? null : logs;
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
