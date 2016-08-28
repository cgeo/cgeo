package cgeo.geocaching.export;

import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.LogEntry;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.XmlUtils;
import cgeo.org.kxml2.io.KXmlSerializer;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlSerializer;

public final class GpxSerializer {

    private static final SynchronizedDateFormat dateFormatZ = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private static final String PREFIX_XSI = "xsi";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String PREFIX_GPX = "";
    private static final String NS_GPX = "http://www.topografix.com/GPX/1/0";
    private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

    private static final String PREFIX_GROUNDSPEAK = "groundspeak";
    private static final String NS_GROUNDSPEAK = "http://www.groundspeak.com/cache/1/0/1";
    private static final String GROUNDSPEAK_SCHEMA = NS_GROUNDSPEAK + "/cache.xsd";

    private static final String PREFIX_GSAK = "gsak";
    private static final String NS_GSAK = "http://www.gsak.net/xmlv1/6";
    private static final String GSAK_SCHEMA = NS_GSAK + "/gsak.xsd";

    private static final String PREFIX_CGEO = "cgeo";
    private static final String NS_CGEO = "http://www.cgeo.org/wptext/1/0";

    /**
     * During the export, only this number of geocaches is fully loaded into memory.
     */
    public static final int CACHES_PER_BATCH = 100;

    /**
     * counter for exported caches, used for progress reporting
     */
    private int countExported;
    private ProgressListener progressListener;
    private final XmlSerializer gpx = new KXmlSerializer();

    protected interface ProgressListener {

        void publishProgress(int countExported);

    }

    public void writeGPX(@NonNull final List<String> allGeocodesIn, final Writer writer, final ProgressListener progressListener) throws IOException {
        // create a copy of the geocode list, as we need to modify it, but it might be immutable
        final List<String> allGeocodes = new ArrayList<>(allGeocodesIn);

        this.progressListener = progressListener;
        gpx.setOutput(writer);

        gpx.startDocument(CharEncoding.UTF_8, true);
        gpx.setPrefix(PREFIX_GPX, NS_GPX);
        gpx.setPrefix(PREFIX_XSI, NS_XSI);
        gpx.setPrefix(PREFIX_GROUNDSPEAK, NS_GROUNDSPEAK);
        gpx.setPrefix(PREFIX_GSAK, NS_GSAK);
        gpx.setPrefix(PREFIX_CGEO, NS_CGEO);

        gpx.startTag(NS_GPX, "gpx");
        gpx.attribute("", "version", "1.0");
        gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/");
        gpx.attribute(NS_XSI, "schemaLocation", NS_GPX + " " + GPX_SCHEMA + " " + NS_GROUNDSPEAK + " " + GROUNDSPEAK_SCHEMA + " " + NS_GSAK + " " + GSAK_SCHEMA);

        // Split the overall set of geocodes into small chunks. That is a compromise between memory efficiency (because
        // we don't load all caches fully into memory) and speed (because we don't query each cache separately).
        while (!allGeocodes.isEmpty()) {
            final List<String> batch = allGeocodes.subList(0, Math.min(CACHES_PER_BATCH, allGeocodes.size()));
            exportBatch(gpx, batch);
            batch.clear();
        }

        gpx.endTag(NS_GPX, "gpx");
        gpx.endDocument();
    }

    private void exportBatch(final XmlSerializer gpx, @NonNull final Collection<String> geocodesOfBatch) throws IOException {
        final Set<Geocache> caches = DataStore.loadCaches(geocodesOfBatch, LoadFlags.LOAD_ALL_DB_ONLY);
        for (final Geocache cache : caches) {
            if (cache == null) {
                continue;
            }
            final Geopoint coords = cache.getCoords();
            if (coords == null) {
                // Export would be invalid without coordinates.
                continue;
            }
            gpx.startTag(NS_GPX, "wpt");
            gpx.attribute("", "lat", Double.toString(coords.getLatitude()));
            gpx.attribute("", "lon", Double.toString(coords.getLongitude()));

            final Date hiddenDate = cache.getHiddenDate();
            if (hiddenDate != null) {
                XmlUtils.simpleText(gpx, NS_GPX, "time", dateFormatZ.format(hiddenDate));
            }

            XmlUtils.multipleTexts(gpx, NS_GPX, "name", cache.getGeocode(), "desc", cache.getName(), "url", cache.getUrl(), "urlname", cache.getName(), "sym", cache.isFound() && Settings.getIncludeFoundStatus() ? "Geocache Found" : "Geocache", "type", "Geocache|" + cache.getType().pattern);

            gpx.startTag(NS_GROUNDSPEAK, "cache");
            gpx.attribute("", "id", cache.getCacheId());
            gpx.attribute("", "available", !cache.isDisabled() ? "True" : "False");
            gpx.attribute("", "archived", cache.isArchived() ? "True" : "False");

            XmlUtils.multipleTexts(gpx, NS_GROUNDSPEAK, "name", cache.getName(), "placed_by", cache.getOwnerDisplayName(), "owner", cache.getOwnerUserId(), "type", cache.getType().pattern, "container", cache.getSize().id);

            writeAttributes(cache);

            XmlUtils.multipleTexts(gpx, NS_GROUNDSPEAK, "difficulty", integerIfPossible(cache.getDifficulty()), "terrain", integerIfPossible(cache.getTerrain()), "country", getCountry(cache), "state", getState(cache));

            gpx.startTag(NS_GROUNDSPEAK, "short_description");
            gpx.attribute("", "html", TextUtils.containsHtml(cache.getShortDescription()) ? "True" : "False");
            gpx.text(cache.getShortDescription());
            gpx.endTag(NS_GROUNDSPEAK, "short_description");

            gpx.startTag(NS_GROUNDSPEAK, "long_description");
            gpx.attribute("", "html", TextUtils.containsHtml(cache.getDescription()) ? "True" : "False");
            gpx.text(cache.getDescription());
            gpx.endTag(NS_GROUNDSPEAK, "long_description");

            XmlUtils.simpleText(gpx, NS_GROUNDSPEAK, "encoded_hints", cache.getHint());

            writeLogs(cache);
            writeTravelBugs(cache);

            gpx.endTag(NS_GROUNDSPEAK, "cache");

            writeGsakExtensions(cache);

            gpx.endTag(NS_GPX, "wpt");

            writeWaypoints(cache);

            countExported++;
            if (progressListener != null) {
                progressListener.publishProgress(countExported);
            }
        }
    }

    private void writeGsakExtensions(@NonNull final Geocache cache) throws IOException {
        gpx.startTag(NS_GSAK, "wptExtension");
        XmlUtils.multipleTexts(gpx, NS_GSAK, "Watch", gpxBoolean(cache.isOnWatchlist()), "IsPremium", gpxBoolean(cache.isPremiumMembersOnly()), "FavPoints", Integer.toString(cache.getFavoritePoints()), "GcNote", StringUtils.trimToEmpty(cache.getPersonalNote()));
        gpx.endTag(NS_GSAK, "wptExtension");
    }

    /**
     * @return XML schema compliant boolean representation of the boolean flag. This must be either true, false, 0 or 1,
     *         but no other value (also not upper case True/False).
     */
    private static String gpxBoolean(final boolean boolFlag) {
        return boolFlag ? "true" : "false";
    }

    private void writeWaypoints(@NonNull final Geocache cache) throws IOException {
        final List<Waypoint> waypoints = cache.getWaypoints();
        final List<Waypoint> ownWaypoints = new ArrayList<>(waypoints.size());
        final List<Waypoint> originWaypoints = new ArrayList<>(waypoints.size());
        int maxPrefix = 0;
        for (final Waypoint wp : cache.getWaypoints()) {

            // Retrieve numerical prefixes to have a basis for assigning prefixes to own waypoints
            final String prefix = wp.getPrefix();
            if (StringUtils.isNotBlank(prefix)) {
                try {
                    final int numericPrefix = Integer.parseInt(prefix);
                    maxPrefix = Math.max(numericPrefix, maxPrefix);
                } catch (final NumberFormatException ignored) {
                    // ignore non numeric prefix, as it should be unique in the list of non-own waypoints already
                }
            }
            if (wp.isUserDefined()) {
                ownWaypoints.add(wp);
            } else {
                originWaypoints.add(wp);
            }
        }
        for (final Waypoint wp : originWaypoints) {
            writeCacheWaypoint(wp);
        }
        // Prefixes must be unique. There use numeric strings as prefixes in OWN waypoints where they are missing
        for (final Waypoint wp : ownWaypoints) {
            if (StringUtils.isBlank(wp.getPrefix()) || StringUtils.equalsIgnoreCase(Waypoint.PREFIX_OWN, wp.getPrefix())) {
                maxPrefix++;
                wp.setPrefix(StringUtils.leftPad(String.valueOf(maxPrefix), 2, '0'));
            }
            writeCacheWaypoint(wp);
        }
    }

    /**
     * Writes one waypoint entry for cache waypoint.
     */
    private void writeCacheWaypoint(@NonNull final Waypoint wp) throws IOException {
        final Geopoint coords = wp.getCoords();
        // TODO: create some extension to GPX to include waypoint without coords
        if (coords != null) {
            gpx.startTag(NS_GPX, "wpt");
            gpx.attribute("", "lat", Double.toString(coords.getLatitude()));
            gpx.attribute("", "lon", Double.toString(coords.getLongitude()));
            final String waypointTypeGpx = wp.getWaypointType().gpx;
            XmlUtils.multipleTexts(gpx, NS_GPX, "name", wp.getGpxId(), "cmt", wp.getNote(), "desc", wp.getName(), "sym", waypointTypeGpx, "type", "Waypoint|" + waypointTypeGpx);
            // add parent reference the GSAK-way
            gpx.startTag(NS_GSAK, "wptExtension");
            gpx.startTag(NS_GSAK, "Parent");
            gpx.text(wp.getGeocode());
            gpx.endTag(NS_GSAK, "Parent");
            gpx.endTag(NS_GSAK, "wptExtension");

            if (wp.isVisited()) {
                gpx.startTag(NS_CGEO, "visited");
                gpx.text("true");
                gpx.endTag(NS_CGEO, "visited");
            }
            if (wp.isUserDefined()) {
                gpx.startTag(NS_CGEO, "userdefined");
                gpx.text("true");
                gpx.endTag(NS_CGEO, "userdefined");
            }
            gpx.endTag(NS_GPX, "wpt");
        }
    }

    private void writeLogs(@NonNull final Geocache cache) throws IOException {
        final List<LogEntry> logs = cache.getLogs();
        if (logs.isEmpty()) {
            return;
        }
        gpx.startTag(NS_GROUNDSPEAK, "logs");

        for (final LogEntry log : logs) {
            gpx.startTag(NS_GROUNDSPEAK, "log");
            gpx.attribute("", "id", Integer.toString(log.id));

            XmlUtils.multipleTexts(gpx, NS_GROUNDSPEAK, "date", dateFormatZ.format(new Date(log.date)), "type", log.getType().type);

            gpx.startTag(NS_GROUNDSPEAK, "finder");
            gpx.attribute("", "id", "");
            gpx.text(log.author);
            gpx.endTag(NS_GROUNDSPEAK, "finder");

            gpx.startTag(NS_GROUNDSPEAK, "text");
            gpx.attribute("", "encoded", "False");
            try {
                gpx.text(log.log);
            } catch (final IllegalArgumentException e) {
                Log.e("GpxSerializer.writeLogs: cannot write log " + log.id + " for cache " + cache.getGeocode(), e);
                gpx.text(" [end of log omitted due to an invalid character]");
            }
            gpx.endTag(NS_GROUNDSPEAK, "text");

            gpx.endTag(NS_GROUNDSPEAK, "log");
        }

        gpx.endTag(NS_GROUNDSPEAK, "logs");
    }

    private void writeTravelBugs(@NonNull final Geocache cache) throws IOException {
        final List<Trackable> inventory = cache.getInventory();
        if (CollectionUtils.isEmpty(inventory)) {
            return;
        }
        gpx.startTag(NS_GROUNDSPEAK, "travelbugs");

        for (final Trackable trackable : inventory) {
            gpx.startTag(NS_GROUNDSPEAK, "travelbug");

            // in most cases the geocode will be empty (only the guid is known). those travel bugs cannot be imported again!
            gpx.attribute("", "ref", trackable.getGeocode());
            XmlUtils.simpleText(gpx, NS_GROUNDSPEAK, "name", trackable.getName());

            gpx.endTag(NS_GROUNDSPEAK, "travelbug");
        }

        gpx.endTag(NS_GROUNDSPEAK, "travelbugs");
    }

    private void writeAttributes(@NonNull final Geocache cache) throws IOException {
        if (cache.getAttributes().isEmpty()) {
            return;
        }
        //TODO: Attribute conversion required: English verbose name, gpx-id
        gpx.startTag(NS_GROUNDSPEAK, "attributes");

        for (final String attribute : cache.getAttributes()) {
            final CacheAttribute attr = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attribute));
            if (attr == null) {
                continue;
            }
            final boolean enabled = CacheAttribute.isEnabled(attribute);

            gpx.startTag(NS_GROUNDSPEAK, "attribute");
            gpx.attribute("", "id", Integer.toString(attr.gcid));
            gpx.attribute("", "inc", enabled ? "1" : "0");
            gpx.text(attr.getL10n(enabled));
            gpx.endTag(NS_GROUNDSPEAK, "attribute");
        }

        gpx.endTag(NS_GROUNDSPEAK, "attributes");
    }

    static String getState(@NonNull final Geocache cache) {
        return getLocationPart(cache, 0);
    }

    private static String getLocationPart(@NonNull final Geocache cache, final int partIndex) {
        final String location = cache.getLocation();
        if (StringUtils.contains(location, ", ")) {
            final String[] parts = StringUtils.split(location, ',');
            if (parts.length == 2) {
                return StringUtils.trim(parts[partIndex]);
            }
        }
        return StringUtils.EMPTY;
    }

    static String getCountry(@NonNull final Geocache cache) {
        final String country = getLocationPart(cache, 1);
        if (StringUtils.isNotEmpty(country)) {
            return country;
        }
        // fall back to returning everything, but only for the country
        return cache.getLocation();
    }

    private static String integerIfPossible(final double value) {
        if (value == (long) value) {
            return String.format(Locale.ENGLISH, "%d", (long) value);
        }
        return String.format(Locale.ENGLISH, "%s", value);
    }
}
