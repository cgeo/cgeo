package cgeo.geocaching.files;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.tc.TerraCachingLogType;
import cgeo.geocaching.connector.tc.TerraCachingType;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class GPXParser extends FileParser {

    private static final SynchronizedDateFormat formatSimple = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US); // 2010-04-20T07:00:00
    private static final SynchronizedDateFormat formatSimpleZ = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // 2010-04-20T07:00:00Z
    private static final SynchronizedDateFormat formatTimezone = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US); // 2010-04-20T01:01:03-04:00

    /**
     * Attention: case sensitive geocode pattern to avoid matching normal words in the name or description of the cache.
     */
    private static final Pattern PATTERN_GEOCODE = Pattern.compile("([0-9A-Z]{2,})");
    private static final Pattern PATTERN_GUID = Pattern.compile(".*" + Pattern.quote("guid=") + "([0-9a-z\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_URL_GEOCODE = Pattern.compile(".*" + Pattern.quote("wp=") + "([A-Z][0-9A-Z]+)", Pattern.CASE_INSENSITIVE);
    /**
     * supported groundspeak extensions of the GPX format
     */
    private static final String[] GROUNDSPEAK_NAMESPACE = {
            "http://www.groundspeak.com/cache/1/1", // PQ 1.1
            "http://www.groundspeak.com/cache/1/0/1", // PQ 1.0.1
            "http://www.groundspeak.com/cache/1/0", // PQ 1.0
    };

    /**
     * supported GSAK extension of the GPX format
     */
    private static final String[] GSAK_NS = {
            "http://www.gsak.net/xmlv1/4",
            "http://www.gsak.net/xmlv1/5",
            "http://www.gsak.net/xmlv1/6"
    };
    /**
     * c:geo extensions of the gpx format
     */
    private static final String CGEO_NS = "http://www.cgeo.org/wptext/1/0";

    private static final Pattern PATTERN_MILLISECONDS = Pattern.compile("\\.\\d{3,7}");

    private int listId = StoredList.STANDARD_LIST_ID;
    final protected String namespace;
    final private String version;

    private Geocache cache;
    private Trackable trackable = new Trackable();
    private LogEntry log = null;

    private String type = null;
    private String sym = null;
    private String name = null;
    private String cmt = null;
    private String desc = null;
    protected final String[] userData = new String[5]; // take 5 cells, that makes indexing 1..4 easier
    private String parentCacheCode = null;
    private boolean wptVisited = false;
    private boolean wptUserDefined = false;
    private List<LogEntry> logs = new ArrayList<>();

    /**
     * Parser result. Maps geocode to cache.
     */
    private final Set<String> result = new HashSet<>(100);
    private ProgressInputStream progressStream;
    /**
     * URL contained in the header of the GPX file. Used to guess where the file is coming from.
     */
    protected String scriptUrl;
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

    private final class UserDataListener implements EndTextElementListener {
        private final int index;

        public UserDataListener(final int index) {
            this.index = index;
        }

        @Override
        public void end(final String user) {
            userData[index] = validate(user);
        }
    }

    private static final class CacheAttributeTranslator {
        // List of cache attributes matching IDs used in GPX files.
        // The ID is represented by the position of the String in the array.
        // Strings are not used as text but as resource IDs of strings, just to be aware of changes
        // made in strings.xml which then will lead to compile errors here and not to runtime errors.
        private static final int[] CACHE_ATTRIBUTES = {
                -1, // 0
                R.string.attribute_dogs_yes, // 1
                R.string.attribute_fee_yes, // 2
                R.string.attribute_rappelling_yes, // 3
                R.string.attribute_boat_yes, // 4
                R.string.attribute_scuba_yes, // 5
                R.string.attribute_kids_yes, // 6
                R.string.attribute_onehour_yes, // 7
                R.string.attribute_scenic_yes, // 8
                R.string.attribute_hiking_yes, // 9
                R.string.attribute_climbing_yes, // 10
                R.string.attribute_wading_yes, // 11
                R.string.attribute_swimming_yes, // 12
                R.string.attribute_available_yes, // 13
                R.string.attribute_night_yes, // 14
                R.string.attribute_winter_yes, // 15
                -1, // 16
                R.string.attribute_poisonoak_yes, // 17
                R.string.attribute_dangerousanimals_yes, // 18
                R.string.attribute_ticks_yes, // 19
                R.string.attribute_mine_yes, // 20
                R.string.attribute_cliff_yes, // 21
                R.string.attribute_hunting_yes, // 22
                R.string.attribute_danger_yes, // 23
                R.string.attribute_wheelchair_yes, // 24
                R.string.attribute_parking_yes, // 25
                R.string.attribute_public_yes, // 26
                R.string.attribute_water_yes, // 27
                R.string.attribute_restrooms_yes, // 28
                R.string.attribute_phone_yes, // 29
                R.string.attribute_picnic_yes, // 30
                R.string.attribute_camping_yes, // 31
                R.string.attribute_bicycles_yes, // 32
                R.string.attribute_motorcycles_yes, // 33
                R.string.attribute_quads_yes, // 34
                R.string.attribute_jeeps_yes, // 35
                R.string.attribute_snowmobiles_yes, // 36
                R.string.attribute_horses_yes, // 37
                R.string.attribute_campfires_yes, // 38
                R.string.attribute_thorn_yes, // 39
                R.string.attribute_stealth_yes, // 40
                R.string.attribute_stroller_yes, // 41
                R.string.attribute_firstaid_yes, // 42
                R.string.attribute_cow_yes, // 43
                R.string.attribute_flashlight_yes, // 44
                R.string.attribute_landf_yes, // 45
                R.string.attribute_rv_yes, // 46
                R.string.attribute_field_puzzle_yes, // 47
                R.string.attribute_uv_yes, // 48
                R.string.attribute_snowshoes_yes, // 49
                R.string.attribute_skiis_yes, // 50
                R.string.attribute_s_tool_yes, // 51
                R.string.attribute_nightcache_yes, // 52
                R.string.attribute_parkngrab_yes, // 53
                R.string.attribute_abandonedbuilding_yes, // 54
                R.string.attribute_hike_short_yes, // 55
                R.string.attribute_hike_med_yes, // 56
                R.string.attribute_hike_long_yes, // 57
                R.string.attribute_fuel_yes, // 58
                R.string.attribute_food_yes, // 59
                R.string.attribute_wirelessbeacon_yes, // 60
                R.string.attribute_partnership_yes, // 61
                R.string.attribute_seasonal_yes, // 62
                R.string.attribute_touristok_yes, // 63
                R.string.attribute_treeclimbing_yes, // 64
                R.string.attribute_frontyard_yes, // 65
                R.string.attribute_teamwork_yes, // 66
                R.string.attribute_geotour_yes, // 67
        };
        private static final String YES = "_yes";
        private static final String NO = "_no";
        private static final Pattern BASENAME_PATTERN = Pattern.compile("^.*attribute_(.*)(_yes|_no)");

        // map GPX-Attribute-Id to baseName
        public static String getBaseName(final int id) {
            if (id < 0) {
                return null;
            }
            // get String out of array
            if (CACHE_ATTRIBUTES.length <= id) {
                return null;
            }
            final int stringId = CACHE_ATTRIBUTES[id];
            if (stringId == -1) {
                return null; // id not found
            }
            // get text for string
            final String stringName;
            try {
                stringName = CgeoApplication.getInstance().getResources().getResourceName(stringId);
            } catch (final NullPointerException ignored) {
                return null;
            }
            if (stringName == null) {
                return null;
            }
            // cut out baseName
            final MatcherWrapper m = new MatcherWrapper(BASENAME_PATTERN, stringName);
            if (!m.matches()) {
                return null;
            }
            return m.group(1);
        }

        // @return  baseName + "_yes" or "_no" e.g. "food_no" or "uv_yes"
        public static String getInternalId(final int attributeId, final boolean active) {
            final String baseName = getBaseName(attributeId);
            if (baseName == null) {
                return null;
            }
            return baseName + (active ? YES : NO);
        }
    }

    protected GPXParser(final int listIdIn, final String namespaceIn, final String versionIn) {
        listId = listIdIn;
        namespace = namespaceIn;
        version = versionIn;
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
        return formatSimple.parse(input);
    }

    @Override
    @NonNull
    public Collection<Geocache> parse(@NonNull final InputStream stream, @Nullable final CancellableHandler progressHandler) throws IOException, ParserException {
        resetCache();
        final RootElement root = new RootElement(namespace, "gpx");
        final Element waypoint = root.getChild(namespace, "wpt");

        root.getChild(namespace, "url").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                scriptUrl = body;
            }
        });

        root.getChild(namespace, "creator").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                scriptUrl = body;
            }
        });

        // waypoint - attributes
        waypoint.setStartElementListener(new StartElementListener() {

            @Override
            public void start(final Attributes attrs) {
                try {
                    if (attrs.getIndex("lat") > -1 && attrs.getIndex("lon") > -1) {
                        final String latitude = attrs.getValue("lat");
                        final String longitude = attrs.getValue("lon");
                        // latitude and longitude are required attributes, but we export them empty for waypoints without coordinates
                        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
                            cache.setCoords(new Geopoint(Double.parseDouble(latitude),
                                    Double.parseDouble(longitude)));
                        }
                    }
                } catch (final NumberFormatException e) {
                    Log.w("Failed to parse waypoint's latitude and/or longitude", e);
                }
            }
        });

        // waypoint
        waypoint.setEndElementListener(new EndElementListener() {

            @Override
            public void end() {
                // try to find geocode somewhere else
                if (StringUtils.isBlank(cache.getGeocode())) {
                    findGeoCode(name);
                    findGeoCode(desc);
                    findGeoCode(cmt);
                }
                // take the name as code, if nothing else is available
                if (StringUtils.isBlank(cache.getGeocode())) {
                    if (StringUtils.isNotBlank(name)) {
                        cache.setGeocode(name.trim());
                    }
                }

                if (isValidForImport()) {
                    fixCache(cache);
                    cache.setListId(listId);
                    cache.setDetailed(true);

                    createNoteFromGSAKUserdata();

                    final String geocode = cache.getGeocode();
                    if (result.contains(geocode)) {
                        Log.w("Duplicate geocode during GPX import: " + geocode);
                    }
                    // modify cache depending on the use case/connector
                    afterParsing(cache);

                    // finally store the cache in the database
                    result.add(geocode);
                    DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                    DataStore.saveLogs(cache.getGeocode(), logs);

                    // avoid the cachecache using lots of memory for caches which the user did not actually look at
                    DataStore.removeCache(geocode, EnumSet.of(RemoveFlag.CACHE));
                    showProgressMessage(progressHandler, progressStream.getProgress());
                } else if (StringUtils.isNotBlank(cache.getName())
 && (StringUtils.containsIgnoreCase(type, "waypoint") || terraChildWaypoint)) {
                    addWaypointToCache();
                }

                resetCache();
            }

            private void addWaypointToCache() {
                fixCache(cache);

                if (cache.getName().length() > 2 || StringUtils.isNotBlank(parentCacheCode)) {
                    if (StringUtils.isBlank(parentCacheCode)) {
                        if (StringUtils.containsIgnoreCase(scriptUrl, "extremcaching")) {
                            parentCacheCode = cache.getName().substring(2);
                        }
 else if (terraChildWaypoint) {
                            parentCacheCode = StringUtils.left(cache.getGeocode(), cache.getGeocode().length() - 1);
                        }
                        else {
                            parentCacheCode = "GC" + cache.getName().substring(2).toUpperCase(Locale.US);
                        }
                    }

                    if ("GC_WayPoint1".equals(cache.getShortDescription())) {
                        cache.setShortDescription("");
                    }

                    final Geocache cacheForWaypoint = findParentCache();
                    if (cacheForWaypoint != null) {
                        final Waypoint waypoint = new Waypoint(cache.getShortDescription(), WaypointType.fromGPXString(sym), false);
                        if (wptUserDefined) {
                            waypoint.setUserDefined();
                        }
                        waypoint.setId(-1);
                        waypoint.setGeocode(parentCacheCode);
                        waypoint.setPrefix(cacheForWaypoint.getWaypointPrefix(cache.getName()));
                        waypoint.setLookup("---");
                        // there is no lookup code in gpx file
                        waypoint.setCoords(cache.getCoords());
                        waypoint.setNote(cache.getDescription());
                        waypoint.setVisited(wptVisited);
                        final List<Waypoint> mergedWayPoints = new ArrayList<>();
                        mergedWayPoints.addAll(cacheForWaypoint.getWaypoints());

                        final List<Waypoint> newPoints = new ArrayList<>();
                        newPoints.add(waypoint);
                        Waypoint.mergeWayPoints(newPoints, mergedWayPoints, true);
                        cacheForWaypoint.setWaypoints(newPoints, false);
                        DataStore.saveCache(cacheForWaypoint, EnumSet.of(SaveFlag.DB));
                        showProgressMessage(progressHandler, progressStream.getProgress());
                    }
                }
            }

        });

        // waypoint.time
        waypoint.getChild(namespace, "time").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                try {
                    cache.setHidden(parseDate(body));
                } catch (final Exception e) {
                    Log.w("Failed to parse cache date", e);
                }
            }
        });

        // waypoint.name
        waypoint.getChild(namespace, "name").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                name = body;

                String content = body.trim();

                // extremcaching.com manipulates the GC code by adding GC in front of ECxxx
                if (StringUtils.startsWithIgnoreCase(content, "GCEC") && StringUtils.containsIgnoreCase(scriptUrl, "extremcaching")) {
                    content = content.substring(2);
                }

                cache.setName(content);

                findGeoCode(cache.getName());
            }
        });

        // waypoint.desc
        waypoint.getChild(namespace, "desc").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                desc = body;

                cache.setShortDescription(validate(body));
            }
        });

        // waypoint.cmt
        waypoint.getChild(namespace, "cmt").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                cmt = body;

                cache.setDescription(validate(body));
            }
        });

        // waypoint.getType()
        waypoint.getChild(namespace, "type").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                final String[] content = StringUtils.split(body, '|');
                if (content.length > 0) {
                    type = content[0].toLowerCase(Locale.US).trim();
                }
            }
        });

        // waypoint.sym
        waypoint.getChild(namespace, "sym").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                sym = body.toLowerCase(Locale.US);
                if (sym.contains("geocache") && sym.contains("found")) {
                    cache.setFound(true);
                }
            }
        });

        // waypoint.url
        waypoint.getChild(namespace, "url").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String url) {
                final MatcherWrapper matcher = new MatcherWrapper(PATTERN_GUID, url);
                if (matcher.matches()) {
                    final String guid = matcher.group(1);
                    if (StringUtils.isNotBlank(guid)) {
                        cache.setGuid(guid);
                    }
                }
                final MatcherWrapper matcherCode = new MatcherWrapper(PATTERN_URL_GEOCODE, url);
                if (matcherCode.matches()) {
                    final String geocode = matcherCode.group(1);
                    cache.setGeocode(geocode);
                }
            }
        });

        // waypoint.urlname (name for waymarks)
        waypoint.getChild(namespace, "urlname").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String urlName) {
                if (cache.getName().equals(cache.getGeocode()) && StringUtils.startsWith(cache.getGeocode(), "WM")) {
                    cache.setName(StringUtils.trim(urlName));
                }
            }
        });

        // for GPX 1.0, cache info comes from waypoint node (so called private children,
        // for GPX 1.1 from extensions node
        final Element cacheParent = getCacheParent(waypoint);

        registerGsakExtensions(cacheParent);
        registerTerraCachingExtensions(cacheParent);
        registerCgeoExtensions(cacheParent);

        // 3 different versions of the GC schema
        for (final String nsGC : GROUNDSPEAK_NAMESPACE) {
            // waypoints.cache
            final Element gcCache = cacheParent.getChild(nsGC, "cache");

            gcCache.setStartElementListener(new StartElementListener() {

                @Override
                public void start(final Attributes attrs) {
                    try {
                        if (attrs.getIndex("id") > -1) {
                            cache.setCacheId(attrs.getValue("id"));
                        }
                        if (attrs.getIndex("archived") > -1) {
                            cache.setArchived(attrs.getValue("archived").equalsIgnoreCase("true"));
                        }
                        if (attrs.getIndex("available") > -1) {
                            cache.setDisabled(!attrs.getValue("available").equalsIgnoreCase("true"));
                        }
                    } catch (final RuntimeException e) {
                        Log.w("Failed to parse cache attributes", e);
                    }
                }
            });

            // waypoint.cache.getName()
            gcCache.getChild(nsGC, "name").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String cacheName) {
                    cache.setName(validate(cacheName));
                }
            });

            // waypoint.cache.getOwner()
            gcCache.getChild(nsGC, "owner").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String ownerUserId) {
                    cache.setOwnerUserId(validate(ownerUserId));
                }
            });

            // waypoint.cache.getOwner()
            gcCache.getChild(nsGC, "placed_by").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String ownerDisplayName) {
                    cache.setOwnerDisplayName(validate(ownerDisplayName));
                }
            });

            // waypoint.cache.getType()
            gcCache.getChild(nsGC, "type").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String bodyIn) {
                    String body = validate(bodyIn);
                    // lab caches wrongly contain a prefix in the type
                    if (body.startsWith("Geocache|")) {
                        body = StringUtils.substringAfter(body, "Geocache|").trim();
                    }
                    cache.setType(CacheType.getByPattern(body));
                }
            });

            // waypoint.cache.container
            gcCache.getChild(nsGC, "container").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String body) {
                    cache.setSize(CacheSize.getById(validate(body)));
                }
            });

            // waypoint.cache.getAttributes()
            // @see issue #299

            // <groundspeak:attributes>
            //   <groundspeak:attribute id="32" inc="1">Bicycles</groundspeak:attribute>
            //   <groundspeak:attribute id="13" inc="1">Available at all times</groundspeak:attribute>
            // where inc = 0 => _no, inc = 1 => _yes
            // IDs see array CACHE_ATTRIBUTES
            final Element gcAttributes = gcCache.getChild(nsGC, "attributes");

            // waypoint.cache.attribute
            final Element gcAttribute = gcAttributes.getChild(nsGC, "attribute");

            gcAttribute.setStartElementListener(new StartElementListener() {
                @Override
                public void start(final Attributes attrs) {
                    try {
                        if (attrs.getIndex("id") > -1 && attrs.getIndex("inc") > -1) {
                            final int attributeId = Integer.parseInt(attrs.getValue("id"));
                            final boolean attributeActive = Integer.parseInt(attrs.getValue("inc")) != 0;
                            final String internalId = CacheAttributeTranslator.getInternalId(attributeId, attributeActive);
                            if (internalId != null) {
                                cache.getAttributes().add(internalId);
                            }
                        }
                    } catch (final NumberFormatException ignored) {
                        // nothing
                    }
                }
            });

            // waypoint.cache.getDifficulty()
            gcCache.getChild(nsGC, "difficulty").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String body) {
                    try {
                        cache.setDifficulty(Float.parseFloat(body));
                    } catch (final NumberFormatException e) {
                        Log.w("Failed to parse difficulty", e);
                    }
                }
            });

            // waypoint.cache.getTerrain()
            gcCache.getChild(nsGC, "terrain").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String body) {
                    try {
                        cache.setTerrain(Float.parseFloat(body));
                    } catch (final NumberFormatException e) {
                        Log.w("Failed to parse terrain", e);
                    }
                }
            });

            // waypoint.cache.country
            gcCache.getChild(nsGC, "country").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String country) {
                    if (StringUtils.isBlank(cache.getLocation())) {
                        cache.setLocation(validate(country));
                    } else {
                        cache.setLocation(cache.getLocation() + ", " + country.trim());
                    }
                }
            });

            // waypoint.cache.state
            gcCache.getChild(nsGC, "state").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String state) {
                    final String trimmedState = state.trim();
                    if (StringUtils.isNotEmpty(trimmedState)) { // state can be completely empty
                        if (StringUtils.isBlank(cache.getLocation())) {
                            cache.setLocation(validate(state));
                        } else {
                            cache.setLocation(trimmedState + ", " + cache.getLocation());
                        }
                    }
                }
            });

            // waypoint.cache.encoded_hints
            gcCache.getChild(nsGC, "encoded_hints").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String encoded) {
                    cache.setHint(validate(encoded));
                }
            });

            gcCache.getChild(nsGC, "short_description").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String shortDesc) {
                    cache.setShortDescription(validate(shortDesc));
                }
            });

            gcCache.getChild(nsGC, "long_description").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String desc) {
                    cache.setDescription(validate(desc));
                }
            });

            // waypoint.cache.travelbugs
            final Element gcTBs = gcCache.getChild(nsGC, "travelbugs");

            // waypoint.cache.travelbug
            final Element gcTB = gcTBs.getChild(nsGC, "travelbug");

            // waypoint.cache.travelbugs.travelbug
            gcTB.setStartElementListener(new StartElementListener() {

                @Override
                public void start(final Attributes attrs) {
                    trackable = new Trackable();

                    try {
                        if (attrs.getIndex("ref") > -1) {
                            trackable.setGeocode(attrs.getValue("ref"));
                        }
                    } catch (final RuntimeException ignored) {
                        // nothing
                    }
                }
            });

            gcTB.setEndElementListener(new EndElementListener() {

                @Override
                public void end() {
                    if (StringUtils.isNotBlank(trackable.getGeocode()) && StringUtils.isNotBlank(trackable.getName())) {
                        cache.addInventoryItem(trackable);
                    }
                }
            });

            // waypoint.cache.travelbugs.travelbug.getName()
            gcTB.getChild(nsGC, "name").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String tbName) {
                    trackable.setName(validate(tbName));
                }
            });

            // waypoint.cache.logs
            final Element gcLogs = gcCache.getChild(nsGC, "logs");

            // waypoint.cache.log
            final Element gcLog = gcLogs.getChild(nsGC, "log");

            gcLog.setStartElementListener(new StartElementListener() {

                @Override
                public void start(final Attributes attrs) {
                    log = new LogEntry("", 0, LogType.UNKNOWN, "");

                    try {
                        if (attrs.getIndex("id") > -1) {
                            log.id = Integer.parseInt(attrs.getValue("id"));
                        }
                    } catch (final NumberFormatException ignored) {
                        // nothing
                    }
                }
            });

            gcLog.setEndElementListener(new EndElementListener() {

                @Override
                public void end() {
                    if (log.type != LogType.UNKNOWN) {
                        if (log.type.isFoundLog() && StringUtils.isNotBlank(log.author)) {
                            final IConnector connector = ConnectorFactory.getConnector(cache);
                            if (connector instanceof ILogin && StringUtils.equals(log.author, ((ILogin) connector).getUserName())) {
                                cache.setFound(true);
                                cache.setVisitedDate(log.date);
                            }
                        }
                        logs.add(log);
                    }
                }
            });

            // waypoint.cache.logs.log.date
            gcLog.getChild(nsGC, "date").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String body) {
                    try {
                        log.date = parseDate(body).getTime();
                    } catch (final Exception e) {
                        Log.w("Failed to parse log date", e);
                    }
                }
            });

            // waypoint.cache.logs.log.getType()
            gcLog.getChild(nsGC, "type").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String body) {
                    final String logType = validate(body);
                    log.type = LogType.getByType(logType);
                }
            });

            // waypoint.cache.logs.log.finder
            gcLog.getChild(nsGC, "finder").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String finderName) {
                    log.author = validate(finderName);
                }
            });

            // waypoint.cache.logs.log.text
            gcLog.getChild(nsGC, "text").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String logText) {
                    log.log = validate(logText);
                }
            });
        }

        try {
            progressStream = new ProgressInputStream(stream);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(progressStream, CharEncoding.UTF_8));
            Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
            return DataStore.loadCaches(result, EnumSet.of(LoadFlag.DB_MINIMAL));
        } catch (final SAXException e) {
            throw new ParserException("Cannot parse .gpx file as GPX " + version + ": could not parse XML", e);
        }
    }

    /**
     * Add listeners for GSAK extensions
     *
     */
    private void registerGsakExtensions(final Element cacheParent) {
        for (final String gsakNamespace : GSAK_NS) {
            final Element gsak = cacheParent.getChild(gsakNamespace, "wptExtension");
            gsak.getChild(gsakNamespace, "Watch").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String watchList) {
                    cache.setOnWatchlist(Boolean.parseBoolean(watchList.trim()));
                }
            });

            gsak.getChild(gsakNamespace, "UserData").setEndTextElementListener(new UserDataListener(1));

            for (int i = 2; i <= 4; i++) {
                gsak.getChild(gsakNamespace, "User" + i).setEndTextElementListener(new UserDataListener(i));
            }

            gsak.getChild(gsakNamespace, "Parent").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String body) {
                    parentCacheCode = body;
                }
            });

            gsak.getChild(gsakNamespace, "FavPoints").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String favoritePoints) {
                    try {
                        cache.setFavoritePoints(Integer.parseInt(favoritePoints));
                    }
                    catch (final NumberFormatException e) {
                        Log.w("Failed to parse favorite points", e);
                    }
                }
            });

            gsak.getChild(gsakNamespace, "GcNote").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String personalNote) {
                    cache.setPersonalNote(StringUtils.trim(personalNote));
                }
            });

            gsak.getChild(gsakNamespace, "IsPremium").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String premium) {
                    cache.setPremiumMembersOnly(Boolean.parseBoolean(premium));
                }
            });

            gsak.getChild(gsakNamespace, "LatBeforeCorrect").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String latitude) {
                    originalLat = latitude;
                    addOriginalCoordinates();
                }
            });

            gsak.getChild(gsakNamespace, "LonBeforeCorrect").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String longitude) {
                    originalLon = longitude;
                    addOriginalCoordinates();
                }
            });

            gsak.getChild(gsakNamespace, "Code").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(final String geocode) {
                    if (StringUtils.isNotBlank(geocode)) {
                        cache.setGeocode(StringUtils.trim(geocode));
                    }
                }
            });

        }
    }

    /**
     * Add listeners for TerraCaching extensions
     *
     */
    private void registerTerraCachingExtensions(final Element cacheParent) {
        final String terraNamespace = "http://www.TerraCaching.com/GPX/1/0";
        final Element terraCache = cacheParent.getChild(terraNamespace, "terracache");

        terraCache.getChild(terraNamespace, "name").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String name) {
                cache.setName(StringUtils.trim(name));
            }
        });

        terraCache.getChild(terraNamespace, "owner").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String ownerName) {
                cache.setOwnerDisplayName(validate(ownerName));
            }
        });

        terraCache.getChild(terraNamespace, "style").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String style) {
                cache.setType(TerraCachingType.getCacheType(style));
            }
        });

        terraCache.getChild(terraNamespace, "size").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String size) {
                cache.setSize(CacheSize.getById(size));
            }
        });

        terraCache.getChild(terraNamespace, "country").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String country) {
                if (StringUtils.isNotBlank(country)) {
                    cache.setLocation(StringUtils.trim(country));
                }
            }
        });

        terraCache.getChild(terraNamespace, "state").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String state) {
                final String trimmedState = state.trim();
                if (StringUtils.isNotEmpty(trimmedState)) {
                    if (StringUtils.isBlank(cache.getLocation())) {
                        cache.setLocation(validate(state));
                    } else {
                        cache.setLocation(trimmedState + ", " + cache.getLocation());
                    }
                }
            }
        });

        terraCache.getChild(terraNamespace, "description").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String description) {
                cache.setDescription(trimHtml(description));
            }
        });

        terraCache.getChild(terraNamespace, "hint").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String hint) {
                cache.setHint(HtmlUtils.extractText(hint));
            }
        });

        final Element terraLogs = terraCache.getChild(terraNamespace, "logs");
        final Element terraLog = terraLogs.getChild(terraNamespace, "log");

        terraLog.setStartElementListener(new StartElementListener() {

            @Override
            public void start(final Attributes attrs) {
                log = new LogEntry("", 0, LogType.UNKNOWN, "");

                try {
                    if (attrs.getIndex("id") > -1) {
                        log.id = Integer.parseInt(attrs.getValue("id"));
                    }
                } catch (final NumberFormatException ignored) {
                    // nothing
                }
            }
        });

        terraLog.setEndElementListener(new EndElementListener() {

            @Override
            public void end() {
                if (log.type != LogType.UNKNOWN) {
                    if (log.type.isFoundLog() && StringUtils.isNotBlank(log.author)) {
                        final IConnector connector = ConnectorFactory.getConnector(cache);
                        if (connector instanceof ILogin && StringUtils.equals(log.author, ((ILogin) connector).getUserName())) {
                            cache.setFound(true);
                            cache.setVisitedDate(log.date);
                        }
                    }
                    logs.add(log);
                }
            }
        });

        // waypoint.cache.logs.log.date
        terraLog.getChild(terraNamespace, "date").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                try {
                    log.date = parseDate(body).getTime();
                } catch (final Exception e) {
                    Log.w("Failed to parse log date", e);
                }
            }
        });

        // waypoint.cache.logs.log.type
        terraLog.getChild(terraNamespace, "type").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                final String logType = validate(body);
                log.type = TerraCachingLogType.getLogType(logType);
            }
        });

        // waypoint.cache.logs.log.finder
        terraLog.getChild(terraNamespace, "user").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String finderName) {
                log.author = validate(finderName);
            }
        });

        // waypoint.cache.logs.log.text
        terraLog.getChild(terraNamespace, "entry").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String entry) {
                log.log = trimHtml(validate(entry));
            }

        });
    }

    private static String trimHtml(final String html) {
        return StringUtils.trim(StringUtils.removeEnd(StringUtils.removeStart(html, "<br>"), "<br>"));
    }

    protected void addOriginalCoordinates() {
        if (StringUtils.isNotEmpty(originalLat) && StringUtils.isNotEmpty(originalLon)) {
            final Waypoint waypoint = new Waypoint(CgeoApplication.getInstance().getString(R.string.cache_coordinates_original), WaypointType.ORIGINAL, false);
            waypoint.setCoords(new Geopoint(originalLat, originalLon));
            cache.addOrChangeWaypoint(waypoint, false);
            cache.setUserModifiedCoords(true);
        }
    }

    /**
     * Add listeners for c:geo extensions
     *
     */
    private void registerCgeoExtensions(final Element cacheParent) {
        final Element cgeoVisited = cacheParent.getChild(CGEO_NS, "visited");

        cgeoVisited.setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String visited) {
                wptVisited = Boolean.parseBoolean(visited.trim());
            }
        });

        final Element cgeoUserDefined = cacheParent.getChild(CGEO_NS, "userdefined");

        cgeoUserDefined.setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String userDefined) {
                wptUserDefined = Boolean.parseBoolean(userDefined.trim());
            }
        });
    }

    /**
     * Overwrite this method in a GPX parser sub class to modify the {@link Geocache}, after it has been fully parsed
     * from the GPX file and before it gets stored.
     *
     * @param cache
     *            currently imported cache
     */
    @SuppressWarnings("static-method")
    protected void afterParsing(final Geocache cache) {
        if ("GC_WayPoint1".equals(cache.getShortDescription())) {
            cache.setShortDescription("");
        }
    }

    /**
     * GPX 1.0 and 1.1 use different XML elements to put the cache into, therefore needs to be overwritten in the
     * version specific subclasses
     *
     */
    protected abstract Element getCacheParent(Element waypoint);

    protected static String validate(final String input) {
        if ("nil".equalsIgnoreCase(input)) {
            return "";
        }
        return input.trim();
    }

    private void findGeoCode(final String input) {
        if (input == null || StringUtils.isNotBlank(cache.getGeocode())) {
            return;
        }
        final String trimmed = input.trim();
        final MatcherWrapper matcherGeocode = new MatcherWrapper(PATTERN_GEOCODE, trimmed);
        if (matcherGeocode.find()) {
            final String geocode = matcherGeocode.group(1);
            // a geocode should not be part of a word
            if (geocode.length() == trimmed.length() || Character.isWhitespace(trimmed.charAt(geocode.length()))) {
                if (ConnectorFactory.canHandle(geocode)) {
                    cache.setGeocode(geocode);
                }
            }
        }
    }

    /**
     * reset all fields that are used to store cache fields over the duration of parsing a single cache
     */
    private void resetCache() {
        type = null;
        sym = null;
        name = null;
        desc = null;
        cmt = null;
        parentCacheCode = null;
        wptVisited = false;
        wptUserDefined = false;
        logs = new ArrayList<>();

        cache = new Geocache(this);

        // explicitly set all properties which could lead to database access, if left as null value
        cache.setLocation("");
        cache.setDescription("");
        cache.setShortDescription("");
        cache.setHint("");

        for (int i = 0; i < userData.length; i++) {
            userData[i] = null;
        }
        originalLon = null;
        originalLat = null;
    }

    /**
     * create a cache note from the UserData1 to UserData4 fields supported by GSAK
     */
    private void createNoteFromGSAKUserdata() {
        if (StringUtils.isBlank(cache.getPersonalNote())) {
            final StringBuilder buffer = new StringBuilder();
            for (final String anUserData : userData) {
                if (StringUtils.isNotBlank(anUserData)) {
                    buffer.append(' ').append(anUserData);
                }
            }
            final String note = buffer.toString().trim();
            if (StringUtils.isNotBlank(note)) {
                cache.setPersonalNote(note);
            }
        }
    }

    private boolean isValidForImport() {
        if (StringUtils.isBlank(cache.getGeocode())) {
            return false;
        }
        if (cache.getCoords() == null) {
            return false;
        }
        final boolean valid = (type == null && sym == null)
                || StringUtils.contains(type, "geocache")
                || StringUtils.contains(sym, "geocache")
                || StringUtils.containsIgnoreCase(sym, "waymark")
 || (StringUtils.containsIgnoreCase(sym, "terracache") && !terraChildWaypoint);
        if ("GC_WayPoint1".equals(cache.getShortDescription())) {
            terraChildWaypoint = true;
        }
        return valid;
    }

    @Nullable
    private Geocache findParentCache() {
        if (StringUtils.isBlank(parentCacheCode)) {
            return null;
        }
        // first match by geocode only
        Geocache cacheForWaypoint = DataStore.loadCache(parentCacheCode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cacheForWaypoint == null) {
            // then match by title
            final String geocode = DataStore.getGeocodeForTitle(parentCacheCode);
            if (StringUtils.isNotBlank(geocode)) {
                cacheForWaypoint = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            }
        }
        return cacheForWaypoint;
    }
}
