package cgeo.geocaching.files;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.connector.tc.TerraCachingLogType;
import cgeo.geocaching.connector.tc.TerraCachingType;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.INamedGeoCoordinate;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.html.HtmlUtils;
import cgeo.geocaching.utils.xml.XmlNode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class GPXMultiParserWPT extends GPXMultiParserBase {

    private static final SynchronizedDateFormat formatSimple = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US); // 2010-04-20T07:00:00
    private static final SynchronizedDateFormat formatSimpleNoTime = new SynchronizedDateFormat("yyyy-MM-dd", Locale.US); // 2010-04-20
    private static final SynchronizedDateFormat formatSimpleZ = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // 2010-04-20T07:00:00Z
    private static final SynchronizedDateFormat formatTimezone = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US); // 2010-04-20T01:01:03-04:00

    /**
     * Attention: case sensitive geocode pattern to avoid matching normal words in the name or description of the cache.
     */
    private static final Pattern PATTERN_GEOCODE = Pattern.compile("[0-9A-Z]{5,}");
    private static final Pattern PATTERN_GUID = Pattern.compile(".*" + Pattern.quote("guid=") + "([0-9a-z\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_URL_GEOCODE = Pattern.compile(".*" + Pattern.quote("wp=") + "([A-Z][0-9A-Z]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_MILLISECONDS = Pattern.compile("\\.\\d{3,7}");

    private final ArrayList<INamedGeoCoordinate> result = new ArrayList<>();
    private final int listId;

    private Geocache cache;
    private String name = null;
    private String type = null;
    private String subtype = null;
    private String sym = null;
    private String cmt = null;
    private String desc = null;
    private String scriptUrl; // URL contained in the header of the GPX file. Used to guess where the file is coming from.

    private Trackable trackable = new Trackable();
    private LogEntry.Builder logBuilder = null;

    protected final String[] userData = new String[5]; // take 5 cells, that makes indexing 1..4 easier
    private String parentCacheCode = null;
    private boolean wptVisited = false;
    private boolean wptUserDefined = false;
    private boolean wptEmptyCoordinates = false;
    private int cacheAssignedEmoji = 0;
    private List<LogEntry> logs = new ArrayList<>();

    /**
     * original longitude in case of modified coordinates
     */
    @Nullable protected String originalLon;
    /**
     * original latitude in case of modified coordinates
     */
    @Nullable protected String originalLat;
    /**
     * Unfortunately we can only detect terracaching child waypoints by remembering the state of the parent
     */
    private boolean terraChildWaypoint = false;
    private boolean logPasswordRequired = false;

    /**
     * prefix of the long description. used for adding alternative geocodes.
     */
    private String descriptionPrefix = "";

    GPXMultiParserWPT(@Nullable final XmlNode gpx, final int listId) {
        try {
            scriptUrl = gpx.get("metadata").get("link").getAttribute("href");
        } catch (NullPointerException ignore) {
            try {
                scriptUrl = gpx.get("url").getValue();
            } catch (NullPointerException ignore2) {
                try {
                    scriptUrl = gpx.get("creator").getValue();
                } catch (NullPointerException e) {
                    Log.w("Failed to parse link attributes", e);
                }
            }
        }
        this.listId = listId;

        // when importing a ZIP, reset the child waypoint state
        terraChildWaypoint = false;

        resetCache();
    }

    @Override
    public String getNodeName() {
        return "wpt";
    }

    @Override
    void addNode(@NonNull final XmlNode node) {
        // coords
        final Double lat = node.getAttributeAsDouble("lat");
        final Double lon = node.getAttributeAsDouble("lon");
        if (lat != null && lat != 0d && lon != null && lon != 0d) {
            cache.setCoords(new Geopoint(lat, lon));
        } else {
            Log.w("Failed to parse waypoint's latitude and/or longitude");
        }

        // waypoint.time
        try {
            cache.setHidden(parseDate(node.get("time").getValue()));
        } catch (final Exception e) {
            Log.w("Failed to parse cache date", e);
        }

        // waypoint.name
        String name = node.getValueAsString("name");
        if (StringUtils.isNotBlank(name)) {
            // extremcaching.com manipulates the GC code by adding GC in front of ECxxx
            if (StringUtils.startsWithIgnoreCase(name, "GCEC") && StringUtils.containsIgnoreCase(scriptUrl, "extremcaching")) {
                name = name.substring(2);
            }
            cache.setName(name);
            findGeoCode(cache.getName(), true);
        }

        // waypoint.desc
        desc = node.getValueAsString("desc");
        cache.setShortDescription(validate(desc));

        // waypoint.cmt
        cmt = node.getValueAsString("cmt");
        cache.setDescription(validate(cmt));

        // waypoint.getType()
        final String tempType = node.getValueAsString("type");
        final String[] content = StringUtils.split(tempType, '|');
        if (content.length > 0) {
            type = content[0].toLowerCase(Locale.US).trim();
            if (content.length > 1) {
                subtype = content[1].toLowerCase(Locale.US).trim();
            }
        }

        // waypoint.sym
        final String tempSym = node.getValueAsString("sym");
        sym = tempSym.toLowerCase(Locale.US);
        if (sym.contains("geocache") && sym.contains("found")) {
            cache.setFound(true);
            cache.setDNF(false);
        }

        // waypoint.url and waypoint.urlname (name for waymarks)
        XmlNode.forEach(node.getAsList("url"), this::setUrl);
        XmlNode.forEach(node.getAsList("urlname"), this::setUrlName);
        try {
            setUrl(node.get("link").getAttribute("href"));
        } catch (NullPointerException ignore) {
            // ignore
        }
        // only to support other formats, standard is href as attribute
        XmlNode.forEach(node.getAsList("href"), this::setUrl);
        XmlNode.forEach(node.getAsList("text"), this::setUrlName);

        // waypoint extensions
        // for GPX 1.0, cache info comes from waypoint node (so called private children)
        // for GPX 1.1 from extensions node
        registerExtensions(node.get("extensions"));
        registerExtensions(node);

    }

    private void registerExtensions(@Nullable final XmlNode cacheParent) {
        if (cacheParent == null) {
            return;
        }
        registerGsakExtensions(cacheParent);
        registerTerraCachingExtensions(cacheParent);
        registerCgeoExtensions(cacheParent);
        registerOpenCachingExtensions(cacheParent);
        registerGroundspeakExtensions(cacheParent);
    }

    /** Add listeners for groundspeak extensions */
    private void registerGroundspeakExtensions(final XmlNode cacheParent) {
        // waypoints.cache
        final XmlNode gcCache = cacheParent.get("cache");
        registerGsakExtensionsCache(gcCache);
        registerGsakExtensionsAttribute(gcCache);
        registerGsakExtensionsTb(gcCache);
        registerGsakExtensionsLog(gcCache);
    }

    /** Add listeners for Groundspeak cache */
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private void registerGsakExtensionsCache(final XmlNode gcCache) {
        try {
            gcCache.onNonBlankAttribute("id", value -> cache.setCacheId(value));
            gcCache.onNonBlankAttribute("archived", value -> cache.setArchived(StringUtils.equalsIgnoreCase(value, "true")));
            gcCache.onNonBlankAttribute("available", value -> cache.setDisabled(!StringUtils.equalsIgnoreCase(value, "true")));
        } catch (final RuntimeException e) {
            Log.w("Failed to parse cache attributes", e);
        }

        // waypoint.cache.getName()
        gcCache.onNonBlankValue("name", value -> cache.setName(validate(value)));

        // waypoint.cache.getOwner()
        gcCache.onNonBlankValue("owner", value -> cache.setOwnerUserId(validate(value)));

        // waypoint.cache.getOwner()
        gcCache.onNonBlankValue("placed_by", value -> cache.setOwnerDisplayName(validate(value)));

        // waypoint.cache.getType()
        String tempType = validate(gcCache.getValueAsString("type"));
        // lab caches wrongly contain a prefix in the type
        if (tempType.startsWith("Geocache|")) {
            tempType = StringUtils.substringAfter(tempType, "Geocache|").trim();
        }
        cache.setType(CacheType.getByPattern(tempType));

        // waypoint.cache.container
        gcCache.onNonBlankValue("container", value -> cache.setSize(CacheSize.getById(validate(value))));

        // waypoint.cache.getDifficulty()
        final Float tempDifficulty = gcCache.getValueAsFloat("difficulty");
        if (tempDifficulty != null) {
            cache.setDifficulty(tempDifficulty);
        }

        // waypoint.cache.getTerrain()
        final Float tempTerrain = gcCache.getValueAsFloat("terrain");
        if (tempTerrain != null) {
            cache.setTerrain(tempTerrain);
        }

        // waypoint.cache.country
        gcCache.onNonBlankValue("country", value -> cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(value) : cache.getLocation() + ", " + value.trim()));

        // waypoint.cache.state
        gcCache.onNonBlankValue("state", value -> cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(value) : value + ", " + cache.getLocation()));

        // waypoint.cache.encoded_hints
        gcCache.onNonBlankValue("encoded_hints", value -> cache.setHint(validate(value)));

        gcCache.onNonBlankValue("short_description", value -> cache.setShortDescription(validate(value)));
        gcCache.onNonBlankValue("long_description", value -> cache.setDescription(validate(value)));
    }

    /** Add listeners for Groundspeak attributes */
    private void registerGsakExtensionsAttribute(final XmlNode gcCache) {
        // waypoint.cache.attribute
        // <groundspeak:attributes>
        //   <groundspeak:attribute id="32" inc="1">Bicycles</groundspeak:attribute>
        //   <groundspeak:attribute id="13" inc="1">Available at all times</groundspeak:attribute>
        // where inc = 0 => _no, inc = 1 => _yes
        // IDs see array CACHE_ATTRIBUTES
        final XmlNode gcAttributes = gcCache.get("attributes");
        if (gcAttributes != null) {
            for (XmlNode attribute : gcAttributes.getAsList("attribute")) {
                try {
                    final Integer id = attribute.getAttributeAsInteger("id");
                    final Integer inc = attribute.getAttributeAsInteger("inc");
                    if (id != null && inc != null) {
                        final CacheAttribute attr = CacheAttribute.getById(id);
                        if (attr != null) {
                            cache.getAttributes().add(attr.getValue(inc != 0));
                        }
                    }
                } catch (final NumberFormatException ignore) {
                    // ignored
                }
            }
        }
    }

    /** Add listeners for Groundspeak TBs */
    private void registerGsakExtensionsTb(final XmlNode gcCache) {
        // waypoint.cache.travelbugs
        final XmlNode gcTBs = gcCache.get("travelbugs");
        if (gcTBs == null) {
            return;
        }

        // waypoint.cache.travelbugs.travelbug
        for (XmlNode gcTB : gcTBs.getAsList("travelbug")) {
            trackable = new Trackable();
            gcTB.onNonBlankValue("ref", value -> trackable.setGeocode(value));
            gcTB.onNonBlankValue("name", value -> trackable.setName(validate(value)));
            if (StringUtils.isNotBlank(trackable.getGeocode()) && StringUtils.isNotBlank(trackable.getName())) {
                cache.addInventoryItem(trackable);
            }
        }
    }

    /** Add listeners for Groundspeak logs */
    private void registerGsakExtensionsLog(final XmlNode gcCache) {
        // waypoint.cache.logs
        final XmlNode gcLogs = gcCache.get("logs");
        if (gcLogs == null) {
            return;
        }

        // waypoint.cache.log
        for (XmlNode gcLog : gcLogs.getAsList("log")) {
            logBuilder = new LogEntry.Builder();
            gcLog.onDefinedIntegerAttribute("id", value -> {
                logBuilder.setId(value);
                final IConnector connector = ConnectorFactory.getConnector(cache);
                if (connector instanceof GCConnector) {
                    logBuilder.setServiceLogId(GCUtils.logIdToLogCode(logBuilder.getId()));
                }
            });

            // waypoint.cache.logs.log.date
            gcLog.onNonBlankValue("date", value -> {
                try {
                    logBuilder.setDate(parseDate(value).getTime());
                } catch (final Exception e) {
                    Log.w("Failed to parse log date", e);
                }
            });

            // waypoint.cache.logs.log.getType()
            gcLog.onNonBlankValue("type", value -> logBuilder.setLogType(LogType.getByType(validate(value))));

            // waypoint.cache.logs.log.finder
            gcLog.onNonBlankValue("finder", value -> logBuilder.setAuthor(validate(value)));

            // waypoint.cache.logs.log.text
            gcLog.onNonBlankValue("text", value -> logBuilder.setLog(validate(value)));

            final LogEntry log = logBuilder.build();
            if (log.logType != LogType.UNKNOWN) {
                if (log.logType.isFoundLog() && StringUtils.isNotBlank(log.author)) {
                    final IConnector connector = ConnectorFactory.getConnector(cache);
                    if (connector instanceof ILogin && StringUtils.equals(log.author, ((ILogin) connector).getUserName())) {
                        cache.setFound(true);
                        cache.setVisitedDate(log.date);
                    }
                }
                logs.add(log);
            }
        }
    }

    /** Add listeners for GSAK extensions */
    private void registerGsakExtensions(final XmlNode cacheParent) {
        final XmlNode gsak = cacheParent.get("wptExtension");
        if (gsak == null) {
            return;
        }

        gsak.onNonBlankValue("Watch", value -> cache.setOnWatchlist(Boolean.parseBoolean(value.trim())));
/* todo
        gsak.getChild(gsakNamespace, "UserData").setEndTextElementListener(new GPXMultiParserCaches.UserDataListener(1));

        for (int i = 2; i <= 4; i++) {
            gsak.getChild(gsakNamespace, "User" + i).setEndTextElementListener(new GPXMultiParserCaches.UserDataListener(i));
        }
*/
        gsak.onNonBlankValue("Parent", value -> parentCacheCode = value);
        gsak.onDefinedIntegerValue("FavPoints", value -> cache.setFavoritePoints(value));
        gsak.onNonBlankValue("GcNote", value -> cache.setPersonalNote(StringUtils.trim(value), true));
        gsak.onNonBlankValue("IsPremium", value -> cache.setPremiumMembersOnly(Boolean.parseBoolean(value)));
        gsak.onNonBlankValue("LatBeforeCorrect", value -> {
            originalLat = value;
            addOriginalCoordinates();
        });
        gsak.onNonBlankValue("LonBeforeCorrect", value -> {
            originalLon = value;
            addOriginalCoordinates();
        });
        gsak.onNonBlankValue("Code", value -> cache.setGeocode(value));
        gsak.onNonBlankValue("DNF", value -> {
            if (!cache.isFound()) {
                cache.setDNF(Boolean.parseBoolean(value));
            }
        });
        gsak.onNonBlankValue("DNFDate", value -> {
            if (0 == cache.getVisitedDate()) {
                try {
                    cache.setVisitedDate(parseDate(value).getTime());
                } catch (final Exception e) {
                    Log.w("Failed to parse visited date 'gsak:DNFDate'", e);
                }
            }
        });
        gsak.onNonBlankValue("UserFound", value -> {
            if (0 == cache.getVisitedDate()) {
                try {
                    cache.setVisitedDate(parseDate(value).getTime());
                } catch (final Exception e) {
                    Log.w("Failed to parse visited date 'gsak:UserFound'", e);
                }
            }
        });
        gsak.onNonBlankValue("Child_ByGSAK", value -> wptUserDefined |= Boolean.parseBoolean(value));
    }

    /** Add listeners for TerraCaching extensions */
    private void registerTerraCachingExtensions(final XmlNode cacheParent) {
        final XmlNode terraCache = cacheParent.get("terracache");
        if (terraCache == null) {
            return;
        }

        cache.setName(terraCache.getValueAsString("name").trim());
        cache.setOwnerDisplayName(validate(terraCache.getValueAsString("owner")));
        cache.setType(TerraCachingType.getCacheType(terraCache.getValueAsString("style")));
        cache.setSize(CacheSize.getById(terraCache.getValueAsString("size")));
        terraCache.onNonBlankValue("country", value -> cache.setLocation(value));
        terraCache.onNonBlankValue("state", value -> cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? validate(value) : value + ", " + cache.getLocation()));
        terraCache.onNonBlankValue("description", value -> cache.setDescription(trimHtml(value)));
        terraCache.onNonBlankValue("hint", value -> cache.setHint(HtmlUtils.extractText(value)));

        final XmlNode terraLogs = terraCache.get("logs");
        if (terraLogs != null) {
            for (XmlNode terraLog : terraLogs.getAsList("log")) {
                logBuilder = new LogEntry.Builder();
                terraLog.onDefinedIntegerAttribute("id", value -> logBuilder.setId(value));

                // waypoint.cache.logs.log.date
                terraLog.onNonBlankValue("date", value -> {
                    try {
                        logBuilder.setDate(parseDate(value).getTime());
                    } catch (final Exception e) {
                        Log.w("Failed to parse log date", e);
                    }
                });

                // waypoint.cache.logs.log.type
                terraLog.onNonBlankValue("type", value -> logBuilder.setLogType(TerraCachingLogType.getLogType(validate(value))));

                // waypoint.cache.logs.log.finder
                terraLog.onNonBlankValue("user", value -> logBuilder.setAuthor(validate(value)));

                // waypoint.cache.logs.log.text
                terraLog.onNonBlankValue("entry", value -> logBuilder.setLog(trimHtml(validate(value))));

                final LogEntry log = logBuilder.build();
                if (log.logType != LogType.UNKNOWN) {
                    if (log.logType.isFoundLog() && StringUtils.isNotBlank(log.author)) {
                        final IConnector connector = ConnectorFactory.getConnector(cache);
                        if (connector instanceof ILogin && StringUtils.equals(log.author, ((ILogin) connector).getUserName())) {
                            cache.setFound(true);
                            cache.setVisitedDate(log.date);
                        }
                    }
                    logs.add(log);
                }
            }
        }
    }

    private static String trimHtml(final String html) {
        return StringUtils.trim(StringUtils.removeEnd(StringUtils.removeStart(html, "<br>"), "<br>"));
    }

    protected void addOriginalCoordinates() {
        if (StringUtils.isNotEmpty(originalLat) && StringUtils.isNotEmpty(originalLon)) {
            cache.createOriginalWaypoint(new Geopoint(Double.parseDouble(originalLat), Double.parseDouble(originalLon)));
        }
    }

    /** Add listeners for c:geo extensions */
    private void registerCgeoExtensions(final XmlNode cacheParent) {
        cacheParent.onNonBlankValue("visited", value -> wptVisited = Boolean.parseBoolean(value));
        cacheParent.onNonBlankValue("userdefined", value -> wptUserDefined = Boolean.parseBoolean(value));
        cacheParent.onNonBlankValue("originalCoordsEmpty", value -> wptEmptyCoordinates = Boolean.parseBoolean(value));

        final XmlNode extension = cacheParent.get("cacheExtension");
        if (extension != null) {
            extension.onDefinedIntegerValue("assignedEmoji", value -> cacheAssignedEmoji = value);
        }
    }

    /** Add listeners for opencaching extensions */
    private void registerOpenCachingExtensions(final XmlNode cacheParent) {
        // waypoints.oc:cache
        final XmlNode ocCache = cacheParent.get("cache");
        if (ocCache == null) {
            return;
        }

        ocCache.onNonBlankValue("requires_password", value -> logPasswordRequired = Boolean.parseBoolean(value));
        ocCache.onNonBlankValue("other_code", value -> descriptionPrefix = Geocache.getAlternativeListingText(value));
        ocCache.onNonBlankValue("size", value -> {
            final CacheSize size = CacheSize.getById(value);
            if (size != CacheSize.UNKNOWN) {
                cache.setSize(size);
            }
        });
    }





    @Override
    void onParsingDone(@NonNull final Collection<Object> result) {
        result.addAll(this.result);
    }




    protected void setUrl(final String url) {
        // try to find guid somewhere else
        if (StringUtils.isBlank(cache.getGuid()) && url != null) {
            final MatcherWrapper matcherGuid = new MatcherWrapper(PATTERN_GUID, url);
            if (matcherGuid.matches()) {
                final String guid = matcherGuid.group(1);
                if (StringUtils.isNotBlank(guid)) {
                    cache.setGuid(guid);
                }
            }
        }

        // try to find geocode somewhere else
        if (StringUtils.isBlank(cache.getGeocode()) && url != null) {
            final MatcherWrapper matcherCode = new MatcherWrapper(PATTERN_URL_GEOCODE, url);
            if (matcherCode.matches()) {
                final String geocode = matcherCode.group(1);
                cache.setGeocode(geocode);
            }
        }
    }

    protected void setUrlName(final String urlName) {
        if (StringUtils.isNotBlank(urlName) && StringUtils.startsWith(cache.getGeocode(), "WM") && cache.getName().equals(cache.getGeocode())) {
            cache.setName(StringUtils.trim(urlName));
        }
    }

    /**
     * reset all fields that are used to store cache fields over the duration of parsing a single cache
     */
    private void resetCache() {
        type = null;
        subtype = null;
        sym = null;
        name = null;
        desc = null;
        cmt = null;
        parentCacheCode = null;
        wptVisited = false;
        wptUserDefined = false;
        wptEmptyCoordinates = false;
        cacheAssignedEmoji = 0;
        logs = new ArrayList<>();

        cache = createCache();

        // explicitly set all properties which could lead to database access, if left as null value
        cache.setLocation("");
        cache.setDescription("");
        cache.setShortDescription("");
        cache.setHint("");

        Arrays.fill(userData, null);
        originalLon = null;
        originalLat = null;
        logPasswordRequired = false;
        descriptionPrefix = "";
    }

    /**
     * Geocache factory method. This explicitly sets several members to empty lists, which does not happen with the
     * default constructor.
     */
    private static Geocache createCache() {
        final Geocache newCache = new Geocache();

        newCache.setAttributes(Collections.emptyList()); // override the lazy initialized list
        newCache.setWaypoints(Collections.emptyList(), false); // override the lazy initialized list

        return newCache;
    }

    static Date parseDate(final String inputUntrimmed) throws ParseException {
        // remove milliseconds to reduce number of needed patterns
        final MatcherWrapper matcher = new MatcherWrapper(PATTERN_MILLISECONDS, inputUntrimmed.trim());
        final String input = matcher.replaceFirst("");
        if (input.contains("Z")) {
            return formatSimpleZ.parse(input);
        }
        if (StringUtils.countMatches(input, ":") == 3) {
            final String removeColon = input.substring(0, input.length() - 3) + input.substring(input.length() - 2);
            return formatTimezone.parse(removeColon);
        }
        if (input.contains("T")) {
            return formatSimple.parse(input);
        }
        return formatSimpleNoTime.parse(input);
    }

    private void findGeoCode(final String input, final Boolean useUnknownConnector) {
        if (input == null || StringUtils.isNotBlank(cache.getGeocode())) {
            return;
        }
        final String trimmed = input.trim();
        final MatcherWrapper matcherGeocode = new MatcherWrapper(PATTERN_GEOCODE, trimmed);
        if (matcherGeocode.find()) {
            final String geocode = matcherGeocode.group();
            // a geocode should not be part of a word
            if (geocode.length() == trimmed.length() || Character.isWhitespace(trimmed.charAt(geocode.length()))) {
                final IConnector foundConnector = ConnectorFactory.getConnector(geocode);
                if (!foundConnector.equals(ConnectorFactory.UNKNOWN_CONNECTOR)) {
                    cache.setGeocode(geocode);
                } else if (useUnknownConnector) {
                    cache.setGeocode(trimmed);
                }
            }
        }
    }

    protected static String validate(@NonNull final String input) {
        if ("nil".equalsIgnoreCase(input)) {
            return "";
        }
        return input.trim();
    }


}
