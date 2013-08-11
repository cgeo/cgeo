package cgeo.geocaching.export;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.cgData;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.XmlUtils;
import cgeo.org.kxml2.io.KXmlSerializer;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GpxSerializer {

    private static final SimpleDateFormat dateFormatZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    public static final String PREFIX_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String PREFIX_GPX = "http://www.topografix.com/GPX/1/0";
    public static final String PREFIX_GROUNDSPEAK = "http://www.groundspeak.com/cache/1/0";

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

    protected static interface ProgressListener {

        void publishProgress(int countExported);

    }

    public void writeGPX(List<String> allGeocodesIn, Writer writer, final ProgressListener progressListener) throws IOException {
        // create a copy of the geocode list, as we need to modify it, but it might be immutable
        final ArrayList<String> allGeocodes = new ArrayList<String>(allGeocodesIn);

        this.progressListener = progressListener;
        gpx.setOutput(writer);

        gpx.startDocument(CharEncoding.UTF_8, true);
        gpx.setPrefix("", PREFIX_GPX);
        gpx.setPrefix("xsi", PREFIX_XSI);
        gpx.setPrefix("groundspeak", PREFIX_GROUNDSPEAK);
        gpx.startTag(PREFIX_GPX, "gpx");
        gpx.attribute("", "version", "1.0");
        gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/");
        gpx.attribute(PREFIX_XSI, "schemaLocation",
                PREFIX_GPX + " http://www.topografix.com/GPX/1/0/gpx.xsd " +
                        PREFIX_GROUNDSPEAK + " http://www.groundspeak.com/cache/1/0/1/cache.xsd");

        // Split the overall set of geocodes into small chunks. That is a compromise between memory efficiency (because
        // we don't load all caches fully into memory) and speed (because we don't query each cache separately).
        while (!allGeocodes.isEmpty()) {
            final List<String> batch = allGeocodes.subList(0, Math.min(CACHES_PER_BATCH, allGeocodes.size()));
            exportBatch(gpx, batch);
            batch.clear();
        }

        gpx.endTag(PREFIX_GPX, "gpx");
        gpx.endDocument();
    }

    private void exportBatch(final XmlSerializer gpx, Collection<String> geocodesOfBatch) throws IOException {
        final Set<Geocache> caches = cgData.loadCaches(geocodesOfBatch, LoadFlags.LOAD_ALL_DB_ONLY);
        for (final Geocache cache : caches) {
            gpx.startTag(PREFIX_GPX, "wpt");
            gpx.attribute("", "lat", Double.toString(cache.getCoords().getLatitude()));
            gpx.attribute("", "lon", Double.toString(cache.getCoords().getLongitude()));

            final Date hiddenDate = cache.getHiddenDate();
            if (hiddenDate != null) {
                XmlUtils.simpleText(gpx, PREFIX_GPX, "time", dateFormatZ.format(hiddenDate));
            }

            XmlUtils.multipleTexts(gpx, PREFIX_GPX,
                    "name", cache.getGeocode(),
                    "desc", cache.getName(),
                    "url", cache.getUrl(),
                    "urlname", cache.getName(),
                    "sym", cache.isFound() ? "Geocache Found" : "Geocache",
                    "type", "Geocache|" + cache.getType().pattern);

            gpx.startTag(PREFIX_GROUNDSPEAK, "cache");
            gpx.attribute("", "id", cache.getCacheId());
            gpx.attribute("", "available", !cache.isDisabled() ? "True" : "False");
            gpx.attribute("", "archives", cache.isArchived() ? "True" : "False");

            XmlUtils.multipleTexts(gpx, PREFIX_GROUNDSPEAK,
                    "name", cache.getName(),
                    "placed_by", cache.getOwnerDisplayName(),
                    "owner", cache.getOwnerUserId(),
                    "type", cache.getType().pattern,
                    "container", cache.getSize().id,
                    "difficulty", Float.toString(cache.getDifficulty()),
                    "terrain", Float.toString(cache.getTerrain()),
                    "country", cache.getLocation(),
                    "state", "",
                    "encoded_hints", cache.getHint());

            writeAttributes(cache);

            gpx.startTag(PREFIX_GROUNDSPEAK, "short_description");
            gpx.attribute("", "html", TextUtils.containsHtml(cache.getShortDescription()) ? "True" : "False");
            gpx.text(cache.getShortDescription());
            gpx.endTag(PREFIX_GROUNDSPEAK, "short_description");

            gpx.startTag(PREFIX_GROUNDSPEAK, "long_description");
            gpx.attribute("", "html", TextUtils.containsHtml(cache.getDescription()) ? "True" : "False");
            gpx.text(cache.getDescription());
            gpx.endTag(PREFIX_GROUNDSPEAK, "long_description");

            writeLogs(cache);

            gpx.endTag(PREFIX_GROUNDSPEAK, "cache");
            gpx.endTag(PREFIX_GPX, "wpt");

            writeWaypoints(cache);

            countExported++;
            if (progressListener != null) {
                progressListener.publishProgress(countExported);
            }
        }
    }

    private void writeWaypoints(final Geocache cache) throws IOException {
        final List<Waypoint> waypoints = cache.getWaypoints();
        final List<Waypoint> ownWaypoints = new ArrayList<Waypoint>(waypoints.size());
        final List<Waypoint> originWaypoints = new ArrayList<Waypoint>(waypoints.size());
        for (final Waypoint wp : cache.getWaypoints()) {
            if (wp.isUserDefined()) {
                ownWaypoints.add(wp);
            } else {
                originWaypoints.add(wp);
            }
        }
        int maxPrefix = 0;
        for (final Waypoint wp : originWaypoints) {
            final String prefix = wp.getPrefix();
            try {
                final int numericPrefix = Integer.parseInt(prefix);
                maxPrefix = Math.max(numericPrefix, maxPrefix);
            } catch (final NumberFormatException ex) {
                // ignore non numeric prefix, as it should be unique in the list of non-own waypoints already
            }
            writeCacheWaypoint(wp, prefix);
        }
        // Prefixes must be unique. There use numeric strings as prefixes in OWN waypoints
        for (final Waypoint wp : ownWaypoints) {
            maxPrefix++;
            final String prefix = StringUtils.leftPad(String.valueOf(maxPrefix), 2, '0');
            writeCacheWaypoint(wp, prefix);
        }
    }

    /**
     * Writes one waypoint entry for cache waypoint.
     *
     * @param cache
     *            The
     * @param wp
     * @param prefix
     * @throws IOException
     */
    private void writeCacheWaypoint(final Waypoint wp, final String prefix) throws IOException {
        final Geopoint coords = wp.getCoords();
        // TODO: create some extension to GPX to include waypoint without coords
        if (coords != null) {
            gpx.startTag(PREFIX_GPX, "wpt");
            gpx.attribute("", "lat", Double.toString(coords.getLatitude()));
            gpx.attribute("", "lon", Double.toString(coords.getLongitude()));
            XmlUtils.multipleTexts(gpx, PREFIX_GPX,
                    "name", prefix + wp.getGeocode().substring(2),
                    "cmt", wp.getNote(),
                    "desc", wp.getName(),
                    "sym", wp.getWaypointType().toString(), //TODO: Correct identifier string
                    "type", "Waypoint|" + wp.getWaypointType().toString()); //TODO: Correct identifier string
            gpx.endTag(PREFIX_GPX, "wpt");
        }
    }

    private void writeLogs(final Geocache cache) throws IOException {
        if (cache.getLogs().isEmpty()) {
            return;
        }
        gpx.startTag(PREFIX_GROUNDSPEAK, "logs");

        for (final LogEntry log : cache.getLogs()) {
            gpx.startTag(PREFIX_GROUNDSPEAK, "log");
            gpx.attribute("", "id", Integer.toString(log.id));

            XmlUtils.multipleTexts(gpx, PREFIX_GROUNDSPEAK,
                    "date", dateFormatZ.format(new Date(log.date)),
                    "type", log.type.type);

            gpx.startTag(PREFIX_GROUNDSPEAK, "finder");
            gpx.attribute("", "id", "");
            gpx.text(log.author);
            gpx.endTag(PREFIX_GROUNDSPEAK, "finder");

            gpx.startTag(PREFIX_GROUNDSPEAK, "text");
            gpx.attribute("", "encoded", "False");
            gpx.text(log.log);
            gpx.endTag(PREFIX_GROUNDSPEAK, "text");

            gpx.endTag(PREFIX_GROUNDSPEAK, "log");
        }

        gpx.endTag(PREFIX_GROUNDSPEAK, "logs");
    }

    private void writeAttributes(final Geocache cache) throws IOException {
        if (cache.getAttributes().isEmpty()) {
            return;
        }
        //TODO: Attribute conversion required: English verbose name, gpx-id
        gpx.startTag(PREFIX_GROUNDSPEAK, "attributes");

        for (final String attribute : cache.getAttributes()) {
            final CacheAttribute attr = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attribute));
            if (attr == null) {
                continue;
            }
            final boolean enabled = CacheAttribute.isEnabled(attribute);

            gpx.startTag(PREFIX_GROUNDSPEAK, "attribute");
            gpx.attribute("", "id", Integer.toString(attr.gcid));
            gpx.attribute("", "inc", enabled ? "1" : "0");
            gpx.text(attr.getL10n(enabled));
            gpx.endTag(PREFIX_GROUNDSPEAK, "attribute");
        }

        gpx.endTag(PREFIX_GROUNDSPEAK, "attributes");
    }

}
