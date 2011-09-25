package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgLog;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgTrackable;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.os.Handler;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GPXParser extends FileParser {

    private static final SimpleDateFormat formatSimple = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // 2010-04-20T07:00:00Z
    private static final SimpleDateFormat formatTimezone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000'Z"); // 2010-04-20T01:01:03.000-04:00

    private static final Pattern patternGeocode = Pattern.compile("([A-Z]{2}[0-9A-Z]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternGuid = Pattern.compile(".*" + Pattern.quote("guid=") + "([0-9a-z\\-]+)", Pattern.CASE_INSENSITIVE);
    /**
     * supported groundspeak extensions of the GPX format
     */
    private static final String[] nsGCList = new String[] {
            "http://www.groundspeak.com/cache/1/1", // PQ 1.1
            "http://www.groundspeak.com/cache/1/0/1", // PQ 1.0.1
            "http://www.groundspeak.com/cache/1/0", // PQ 1.0
    };

    /**
     * supported GSAK extension of the GPX format
     */
    private static final String GSAK_NS = "http://www.gsak.net/xmlv1/5";

    private int listId = 1;
    final protected String namespace;
    final private String version;
    private Handler handler = null;

    private cgCache cache = new cgCache();
    private cgTrackable trackable = new cgTrackable();
    private cgLog log = new cgLog();

    private String type = null;
    private String sym = null;
    private String name = null;
    private String cmt = null;
    private String desc = null;
    protected final String[] userData = new String[5]; // take 5 cells, that makes indexing 1..4 easier

    private final class UserDataListener implements EndTextElementListener {
        private final int index;

        public UserDataListener(int index) {
            this.index = index;
        }

        @Override
        public void end(String user) {
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
        };
        private static final String YES = "_yes";
        private static final String NO = "_no";
        private static final Pattern BASENAME_PATTERN = Pattern.compile("^.*attribute_(.*)(_yes|_no)");

        // map GPX-Attribute-Id to baseName
        public static String getBaseName(final int id) {
            // get String out of array
            if (CACHE_ATTRIBUTES.length <= id) {
                return null;
            }
            final int stringId = CACHE_ATTRIBUTES[id];
            if (stringId == -1) {
                return null; // id not found
            }
            // get text for string
            String stringName = null;
            try {
                stringName = cgeoapplication.getInstance().getResources().getResourceName(stringId);
            } catch (NullPointerException e) {
                return null;
            }
            if (stringName == null) {
                return null;
            }
            // cut out baseName
            final Matcher m = BASENAME_PATTERN.matcher(stringName);
            if (!m.matches()) {
                return null;
            }
            return m.group(1);
        }

        // @return  baseName + "_yes" or "_no" e.g. "food_no" or "uv_yes"
        public static String getInternalId(final int attributeId, final boolean active) {
            final String baseName = CacheAttributeTranslator.getBaseName(attributeId);
            if (baseName == null) {
                return null;
            }
            return baseName + (active ? YES : NO);
        }
    }

    protected GPXParser(int listIdIn, String namespaceIn, String versionIn) {
        listId = listIdIn;
        namespace = namespaceIn;
        version = versionIn;
    }

    private static Date parseDate(String inputUntrimmed) throws ParseException {
        final String input = inputUntrimmed.trim();
        if (input.length() >= 3 && input.charAt(input.length() - 3) == ':') {
            final String removeColon = input.substring(0, input.length() - 3) + input.substring(input.length() - 2);
            return formatTimezone.parse(removeColon);
        }
        return formatSimple.parse(input);
    }

    public List<cgCache> parse(final InputStream stream, Handler handlerIn) {
        handler = handlerIn;
        final List<cgCache> result = new ArrayList<cgCache>();

        final RootElement root = new RootElement(namespace, "gpx");
        final Element waypoint = root.getChild(namespace, "wpt");

        // waypoint - attributes
        waypoint.setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attrs) {
                try {
                    if (attrs.getIndex("lat") > -1 && attrs.getIndex("lon") > -1) {
                        cache.coords = new Geopoint(new Double(attrs.getValue("lat")),
                                new Double(attrs.getValue("lon")));
                    }
                } catch (Exception e) {
                    Log.w(cgSettings.tag, "Failed to parse waypoint's latitude and/or longitude.");
                }
            }
        });

        // waypoint
        waypoint.setEndElementListener(new EndElementListener() {

            @Override
            public void end() {
                if (StringUtils.isBlank(cache.geocode)) {
                    // try to find geocode somewhere else
                    findGeoCode(name);
                    findGeoCode(desc);
                    findGeoCode(cmt);
                }

                if (StringUtils.isNotBlank(cache.geocode)
                        && cache.coords != null
                        && ((type == null && sym == null)
                                || StringUtils.contains(type, "geocache")
                                || StringUtils.contains(sym, "geocache"))) {
                    fixCache(cache);
                    cache.reason = listId;
                    cache.detailed = true;

                    createNoteFromGSAKUserdata();

                    result.add(cache);
                    showCountMessage(handler, result.size());
                } else if (StringUtils.isNotBlank(cache.name)
                        && cache.coords != null
                        && StringUtils.contains(type, "waypoint")) {
                    addWaypointToCache();
                }

                resetCache();
            }

            private void addWaypointToCache() {
                fixCache(cache);

                if (cache.name.length() > 2) {
                    String cacheGeocodeForWaypoint = "GC" + cache.name.substring(2);

                    cgCache cacheForWaypoint = getCacheFromResultOrDb(cacheGeocodeForWaypoint);
                    if (cacheForWaypoint != null) {
                        final cgWaypoint waypoint = new cgWaypoint();
                        waypoint.id = -1;
                        waypoint.type = sym;
                        waypoint.geocode = cacheGeocodeForWaypoint;
                        waypoint.setPrefix(cache.name.substring(0, 2));
                        waypoint.lookup = "---";
                        // there is no lookup code in gpx file
                        waypoint.name = cache.shortdesc;
                        waypoint.coords = cache.coords;
                        waypoint.latitudeString = cache.latitudeString;
                        waypoint.longitudeString = cache.longitudeString;
                        waypoint.note = cache.description;

                        if (cacheForWaypoint.waypoints == null) {
                            cacheForWaypoint.waypoints = new ArrayList<cgWaypoint>();
                        }
                        cgWaypoint.mergeWayPoints(cacheForWaypoint.waypoints, Collections.singletonList(waypoint));
                        // need to remove from cache because id changes on store
                        cgeoapplication.getInstance().removeCacheFromCache(cacheForWaypoint.geocode);
                        result.add(cacheForWaypoint);
                        showCountMessage(handler, result.size());
                    }
                }
            }

            /**
             * Retrieves cache data from result or from DB.
             * 
             * @param geocode
             * @return cache or <code>null</code> if cache doesn't exist
             */
            private cgCache getCacheFromResultOrDb(String geocode) {
                for (cgCache cache : result) {
                    if (geocode.equals(cache.geocode))
                        return cache;
                }
                return cgeoapplication.getInstance().getCacheByGeocode(geocode, true, true, true, true, true, true);
            }
        });

        // waypoint.time
        waypoint.getChild(namespace, "time").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                try {
                    cache.hidden = parseDate(body);
                } catch (Exception e) {
                    Log.w(cgSettings.tag, "Failed to parse cache date: " + e.toString());
                }
            }
        });

        // waypoint.name
        waypoint.getChild(namespace, "name").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                name = body;

                final String content = body.trim();
                cache.name = content;

                findGeoCode(cache.name);
                findGeoCode(cache.description);
            }
        });

        // waypoint.desc
        waypoint.getChild(namespace, "desc").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                desc = body;

                cache.shortdesc = validate(body);
            }
        });

        // waypoint.cmt
        waypoint.getChild(namespace, "cmt").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                cmt = body;

                cache.description = validate(body);
            }
        });

        // waypoint.type
        waypoint.getChild(namespace, "type").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                final String[] content = body.split("\\|");
                if (content.length > 0) {
                    type = content[0].toLowerCase().trim();
                }
            }
        });

        // waypoint.sym
        waypoint.getChild(namespace, "sym").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                body = body.toLowerCase();
                sym = body;
                if (body.contains("geocache") && body.contains("found")) {
                    cache.found = true;
                }
            }
        });

        // waypoint.url
        waypoint.getChild(namespace, "url").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String url) {
                final Matcher matcher = patternGuid.matcher(url);
                if (matcher.matches()) {
                    final String guid = matcher.group(1);
                    if (StringUtils.isNotBlank(guid)) {
                        cache.guid = guid;
                    }
                }
            }
        });

        // for GPX 1.0, cache info comes from waypoint node (so called private children,
        // for GPX 1.1 from extensions node
        final Element cacheParent = getCacheParent(waypoint);

        // GSAK extensions
        final Element gsak = cacheParent.getChild(GSAK_NS, "wptExtension");
        gsak.getChild(GSAK_NS, "Watch").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String watchList) {
                cache.onWatchlist = Boolean.valueOf(watchList.trim());
            }
        });

        gsak.getChild(GSAK_NS, "UserData").setEndTextElementListener(new UserDataListener(1));

        for (int i = 2; i <= 4; i++) {
            gsak.getChild(GSAK_NS, "User" + i).setEndTextElementListener(new UserDataListener(i));
        }

        // 3 different versions of the GC schema
        for (String nsGC : nsGCList) {
            // waypoints.cache
            final Element gcCache = cacheParent.getChild(nsGC, "cache");

            gcCache.setStartElementListener(new StartElementListener() {

                @Override
                public void start(Attributes attrs) {
                    try {
                        if (attrs.getIndex("id") > -1) {
                            cache.cacheId = attrs.getValue("id");
                        }
                        if (attrs.getIndex("archived") > -1) {
                            cache.archived = attrs.getValue("archived").equalsIgnoreCase("true");
                        }
                        if (attrs.getIndex("available") > -1) {
                            cache.disabled = !attrs.getValue("available").equalsIgnoreCase("true");
                        }
                    } catch (Exception e) {
                        Log.w(cgSettings.tag, "Failed to parse cache attributes.");
                    }
                }
            });

            // waypoint.cache.name
            gcCache.getChild(nsGC, "name").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String cacheName) {
                    cache.name = validate(cacheName);
                }
            });

            // waypoint.cache.owner
            gcCache.getChild(nsGC, "owner").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String cacheOwner) {
                    cache.owner = validate(cacheOwner);
                }
            });

            // waypoint.cache.type
            gcCache.getChild(nsGC, "type").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String body) {
                    setType(validate(body.toLowerCase()));
                }
            });

            // waypoint.cache.container
            gcCache.getChild(nsGC, "container").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String body) {
                    cache.size = CacheSize.FIND_BY_ID.get(validate(body.toLowerCase()));
                }
            });

            // waypoint.cache.attributes
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
                public void start(Attributes attrs) {
                    try {
                        if (attrs.getIndex("id") > -1 && attrs.getIndex("inc") > -1) {
                            int attributeId = Integer.parseInt(attrs.getValue("id"));
                            boolean attributeActive = Integer.parseInt(attrs.getValue("inc")) != 0;
                            String internalId = CacheAttributeTranslator.getInternalId(attributeId, attributeActive);
                            if (internalId != null) {
                                if (cache.attributes == null) {
                                    cache.attributes = new ArrayList<String>();
                                }
                                cache.attributes.add(internalId);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // nothing
                    }
                }
            });

            // waypoint.cache.difficulty
            gcCache.getChild(nsGC, "difficulty").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String body) {
                    try {
                        cache.difficulty = new Float(body);
                    } catch (Exception e) {
                        Log.w(cgSettings.tag, "Failed to parse difficulty: " + e.toString());
                    }
                }
            });

            // waypoint.cache.terrain
            gcCache.getChild(nsGC, "terrain").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String body) {
                    try {
                        cache.terrain = new Float(body);
                    } catch (Exception e) {
                        Log.w(cgSettings.tag, "Failed to parse terrain: " + e.toString());
                    }
                }
            });

            // waypoint.cache.country
            gcCache.getChild(nsGC, "country").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String country) {
                    if (StringUtils.isBlank(cache.location)) {
                        cache.location = validate(country);
                    } else {
                        cache.location = cache.location + ", " + country.trim();
                    }
                }
            });

            // waypoint.cache.state
            gcCache.getChild(nsGC, "state").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String state) {
                    if (StringUtils.isBlank(cache.location)) {
                        cache.location = validate(state);
                    } else {
                        cache.location = state.trim() + ", " + cache.location;
                    }
                }
            });

            // waypoint.cache.encoded_hints
            gcCache.getChild(nsGC, "encoded_hints").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String encoded) {
                    cache.hint = validate(encoded);
                }
            });

            gcCache.getChild(nsGC, "short_description").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String shortDesc) {
                    cache.shortdesc = validate(shortDesc);
                }
            });

            gcCache.getChild(nsGC, "long_description").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String desc) {
                    cache.description = validate(desc);
                }
            });

            // waypoint.cache.travelbugs
            final Element gcTBs = gcCache.getChild(nsGC, "travelbugs");

            // waypoint.cache.travelbugs.travelbug
            gcTBs.getChild(nsGC, "travelbug").setStartElementListener(new StartElementListener() {

                @Override
                public void start(Attributes attrs) {
                    trackable = new cgTrackable();

                    try {
                        if (attrs.getIndex("ref") > -1) {
                            trackable.geocode = attrs.getValue("ref").toUpperCase();
                        }
                    } catch (Exception e) {
                        // nothing
                    }
                }
            });

            // waypoint.cache.travelbug
            final Element gcTB = gcTBs.getChild(nsGC, "travelbug");

            gcTB.setEndElementListener(new EndElementListener() {

                @Override
                public void end() {
                    if (StringUtils.isNotBlank(trackable.geocode) && StringUtils.isNotBlank(trackable.name)) {
                        if (cache.inventory == null) {
                            cache.inventory = new ArrayList<cgTrackable>();
                        }
                        cache.inventory.add(trackable);
                    }
                }
            });

            // waypoint.cache.travelbugs.travelbug.name
            gcTB.getChild(nsGC, "name").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String tbName) {
                    trackable.name = validate(tbName);
                }
            });

            // waypoint.cache.logs
            final Element gcLogs = gcCache.getChild(nsGC, "logs");

            // waypoint.cache.log
            final Element gcLog = gcLogs.getChild(nsGC, "log");

            gcLog.setStartElementListener(new StartElementListener() {

                @Override
                public void start(Attributes attrs) {
                    log = new cgLog();

                    try {
                        if (attrs.getIndex("id") > -1) {
                            log.id = Integer.parseInt(attrs.getValue("id"));
                        }
                    } catch (Exception e) {
                        // nothing
                    }
                }
            });

            gcLog.setEndElementListener(new EndElementListener() {

                @Override
                public void end() {
                    if (StringUtils.isNotBlank(log.log)) {
                        if (cache.logs == null) {
                            cache.logs = new ArrayList<cgLog>();
                        }
                        cache.logs.add(log);
                    }
                }
            });

            // waypoint.cache.logs.log.date
            gcLog.getChild(nsGC, "date").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String body) {
                    try {
                        log.date = parseDate(body).getTime();
                    } catch (Exception e) {
                        Log.w(cgSettings.tag, "Failed to parse log date: " + e.toString());
                    }
                }
            });

            // waypoint.cache.logs.log.type
            gcLog.getChild(nsGC, "type").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String body) {
                    final String logType = validate(body).toLowerCase();
                    if (cgBase.logTypes0.containsKey(logType)) {
                        log.type = cgBase.logTypes0.get(logType);
                    } else {
                        log.type = cgBase.LOG_NOTE;
                    }
                }
            });

            // waypoint.cache.logs.log.finder
            gcLog.getChild(nsGC, "finder").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String finderName) {
                    log.author = validate(finderName);
                }
            });

            // waypoint.cache.logs.log.text
            gcLog.getChild(nsGC, "text").setEndTextElementListener(new EndTextElementListener() {

                @Override
                public void end(String logText) {
                    log.log = validate(logText);
                }
            });
        }
        boolean parsed = false;
        try {
            Xml.parse(stream, Xml.Encoding.UTF_8, root.getContentHandler());
            parsed = true;
        } catch (IOException e) {
            Log.e(cgSettings.tag, "Cannot parse .gpx file as GPX " + version + ": could not read file!");
        } catch (SAXException e) {
            Log.e(cgSettings.tag, "Cannot parse .gpx file as GPX " + version + ": could not parse XML - " + e.toString());
        }

        return parsed ? result : null;
    }

    private List<cgCache> parse(final File file, final Handler handlerIn) {
        if (file == null) {
            return null;
        }

        FileInputStream fis = null;
        List<cgCache> result = null;
        try {
            fis = new FileInputStream(file);
            result = parse(fis, handlerIn);
        } catch (FileNotFoundException e) {
            Log.e(cgSettings.tag, "Cannot parse .gpx file " + file.getAbsolutePath() + " as GPX " + version + ": file not found!");
        }
        try {
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e) {
            Log.e(cgSettings.tag, "Error after parsing .gpx file " + file.getAbsolutePath() + " as GPX " + version + ": could not close file!");
        }
        return result;
    }

    /**
     * GPX 1.0 and 1.1 use different XML elements to put the cache into, therefore needs to be overwritten in the
     * version specific subclasses
     *
     * @param waypoint
     * @return
     */
    protected abstract Element getCacheParent(Element waypoint);

    protected static String validate(String input) {
        if ("nil".equalsIgnoreCase(input)) {
            return "";
        }
        return input.trim();
    }

    private void setType(final String parsedString) {
        String lowercase = parsedString.toLowerCase().trim();
        final String knownType = cgBase.cacheTypes.get(lowercase);
        if (knownType != null) {
            cache.type = knownType;
        }
        else {
            if (StringUtils.isBlank(cache.type)) {
                cache.type = "mystery"; // default for not recognized types
            }
        }
    }

    private void findGeoCode(final String input) {
        if (input == null || StringUtils.isNotBlank(cache.geocode)) {
            return;
        }
        final Matcher matcherGeocode = patternGeocode.matcher(input);
        if (matcherGeocode.find()) {
            final String geocode = matcherGeocode.group(1);
            if (ConnectorFactory.canHandle(geocode)) {
                cache.geocode = geocode;
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

        cache = new cgCache();
        for (int i = 0; i < userData.length; i++) {
            userData[i] = null;
        }
    }

    /**
     * create a cache note from the UserData1 to UserData4 fields supported by GSAK
     */
    private void createNoteFromGSAKUserdata() {
        if (StringUtils.isBlank(cache.personalNote)) {
            final StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < userData.length; i++) {
                if (StringUtils.isNotBlank(userData[i])) {
                    buffer.append(' ').append(userData[i]);
                }
            }
            final String note = buffer.toString().trim();
            if (StringUtils.isNotBlank(note)) {
                cache.personalNote = note;
            }
        }
    }

    public static UUID parseGPX(File file, int listId, Handler handler) {
        try {
            GPXParser parser = new GPX10Parser(listId);
            List<cgCache> caches = parser.parse(file, handler);
            if (caches == null) {
                parser = new GPX11Parser(listId);
                caches = parser.parse(file, handler);
            }

            if (caches != null) {
                final cgSearch search = new cgSearch();
                final cgeoapplication app = cgeoapplication.getInstance();
                for (cgCache cache : caches) {
                    app.addCacheToSearch(search, cache);
                }
                Log.i(cgSettings.tag, "Caches found in .gpx file: " + caches.size());
                return search.getCurrentId();
            }

        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.parseGPX: " + e.toString());
        }

        return null;
    }
}
