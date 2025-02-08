package cgeo.geocaching.files;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.INamedGeoCoordinate;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.SynchronizedDateFormat;
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

    protected static String validate(final String input) {
        if ("nil".equalsIgnoreCase(input)) {
            return "";
        }
        return input.trim();
    }


}
