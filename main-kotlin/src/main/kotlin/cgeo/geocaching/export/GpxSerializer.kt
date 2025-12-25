// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.export

import cgeo.geocaching.connector.gc.GCUtils
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.enumerations.CacheAttribute
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.WaypointUserNoteCombiner
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.SynchronizedDateFormat
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.xml.XmlUtils
import cgeo.org.kxml2.io.KXmlSerializer

import androidx.annotation.NonNull

import java.io.IOException
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.ArrayList
import java.util.Collection
import java.util.Date
import java.util.List
import java.util.Locale
import java.util.Set

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.xmlpull.v1.XmlSerializer

class GpxSerializer {

    private static val dateFormatZ: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    private static val PREFIX_XSI: String = "xsi"
    private static val NS_XSI: String = "http://www.w3.org/2001/XMLSchema-instance"

    private static val PREFIX_GPX: String = ""
    private static val NS_GPX: String = "http://www.topografix.com/GPX/1/0"
    private static val GPX_SCHEMA: String = NS_GPX + "/gpx.xsd"

    private static val PREFIX_GROUNDSPEAK: String = "groundspeak"
    private static val NS_GROUNDSPEAK: String = "http://www.groundspeak.com/cache/1/0/1"
    private static val GROUNDSPEAK_SCHEMA: String = NS_GROUNDSPEAK + "/cache.xsd"

    private static val PREFIX_GSAK: String = "gsak"
    private static val NS_GSAK: String = "http://www.gsak.net/xmlv1/6"
    private static val GSAK_SCHEMA: String = NS_GSAK + "/gsak.xsd"

    private static val PREFIX_CGEO: String = "cgeo"
    private static val NS_CGEO: String = "http://www.cgeo.org/wptext/1/0"

    /**
     * During the export, only this number of geocaches is fully loaded into memory.
     */
    public static val CACHES_PER_BATCH: Int = 100

    /**
     * counter for exported caches, used for progress reporting
     */
    private Int countExported
    private ProgressListener progressListener
    private val gpx: XmlSerializer = KXmlSerializer()

    protected interface ProgressListener {

        Unit publishProgress(Int countExported)

    }

    public Unit writeGPX(final List<String> allGeocodesIn, final Writer writer, final ProgressListener progressListener) throws IOException {
        // create a copy of the geocode list, as we need to modify it, but it might be immutable
        val allGeocodes: List<String> = ArrayList<>(allGeocodesIn)

        this.progressListener = progressListener
        gpx.setOutput(writer)
        gpx.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

        gpx.startDocument(StandardCharsets.UTF_8.name(), true)
        gpx.setPrefix(PREFIX_GPX, NS_GPX)
        gpx.setPrefix(PREFIX_XSI, NS_XSI)
        gpx.setPrefix(PREFIX_GROUNDSPEAK, NS_GROUNDSPEAK)
        gpx.setPrefix(PREFIX_GSAK, NS_GSAK)
        gpx.setPrefix(PREFIX_CGEO, NS_CGEO)

        gpx.startTag(NS_GPX, "gpx")
        gpx.attribute("", "version", "1.0")
        gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/")
        gpx.attribute(NS_XSI, "schemaLocation", NS_GPX + " " + GPX_SCHEMA + " " + NS_GROUNDSPEAK + " " + GROUNDSPEAK_SCHEMA + " " + NS_GSAK + " " + GSAK_SCHEMA)

        // Split the overall set of geocodes into small chunks. That is a compromise between memory efficiency (because
        // we don't load all caches fully into memory) and speed (because we don't query each cache separately).
        while (!allGeocodes.isEmpty()) {
            val batch: List<String> = allGeocodes.subList(0, Math.min(CACHES_PER_BATCH, allGeocodes.size()))
            exportBatch(gpx, batch)
            batch.clear()
        }

        gpx.endTag(NS_GPX, "gpx")
        gpx.endDocument()
    }

    private Unit exportBatch(final XmlSerializer gpx, final Collection<String> geocodesOfBatch) throws IOException {
        val caches: Set<Geocache> = DataStore.loadCaches(geocodesOfBatch, LoadFlags.LOAD_ALL_DB_ONLY)

        // Define the DecimalFormat for corrected output (DDD.DDDDDD format)
        val dfs: DecimalFormatSymbols = DecimalFormatSymbols.getInstance()
        dfs.setDecimalSeparator('.')
        val df: DecimalFormat = DecimalFormat("#.######", dfs)
        df.setMinimumFractionDigits(1)
        df.setMaximumFractionDigits(6)

        for (final Geocache cache : caches) {
            if (cache == null) {
                continue
            }

            val isInternal: Boolean = InternalConnector.getInstance().canHandle(cache.getGeocode())
            val coords: Geopoint = cache.getCoords()
            if (coords == null && !isInternal) {
                // Export would be invalid without coordinates.
                continue
            }
            gpx.startTag(NS_GPX, "wpt")
            gpx.attribute("", "lat", df.format(coords == null ? 0 : coords.getLatitude()))
            gpx.attribute("", "lon", df.format(coords == null ? 0 : coords.getLongitude()))

            val hiddenDate: Date = cache.getHiddenDate()
            if (hiddenDate != null) {
                XmlUtils.simpleText(gpx, NS_GPX, "time", dateFormatZ.format(hiddenDate))
            }

            XmlUtils.multipleTexts(gpx, NS_GPX, "name", cache.getGeocode(), "desc", cache.getName(), "url", cache.getUrl(), "urlname", cache.getName(), "sym", cache.isFound() && Settings.getIncludeFoundStatus() ? "Geocache Found" : "Geocache", "type", "Geocache|" + cache.getType().pattern)

            gpx.startTag(NS_GROUNDSPEAK, "cache")
            gpx.attribute("", "id", cache.getCacheId())
            gpx.attribute("", "available", !cache.isDisabled() ? "True" : "False")
            gpx.attribute("", "archived", cache.isArchived() ? "True" : "False")

            XmlUtils.multipleTexts(gpx, NS_GROUNDSPEAK, "name", cache.getName(), "placed_by", cache.getOwnerDisplayName(), "owner", cache.getOwnerUserId(), "type", cache.getType().pattern, "container", cache.getSize().id)

            writeAttributes(cache)

            XmlUtils.multipleTexts(gpx, NS_GROUNDSPEAK, "difficulty", integerIfPossible(cache.getDifficulty()), "terrain", integerIfPossible(cache.getTerrain()), "country", getCountry(cache), "state", getState(cache))

            gpx.startTag(NS_GROUNDSPEAK, "short_description")
            gpx.attribute("", "html", TextUtils.containsHtml(cache.getShortDescription()) ? "True" : "False")
            gpx.text(cache.getShortDescription())
            gpx.endTag(NS_GROUNDSPEAK, "short_description")

            gpx.startTag(NS_GROUNDSPEAK, "long_description")
            gpx.attribute("", "html", TextUtils.containsHtml(cache.getDescription()) ? "True" : "False")
            gpx.text(cache.getDescription())
            gpx.endTag(NS_GROUNDSPEAK, "long_description")

            XmlUtils.simpleText(gpx, NS_GROUNDSPEAK, "encoded_hints", cache.getHint())

            if (Settings.getIncludeLogs()) {
                writeLogs(cache)
            }
            if (Settings.getIncludeTravelBugs()) {
                writeTravelBugs(cache)
            }

            gpx.endTag(NS_GROUNDSPEAK, "cache")

            writeGsakExtensions(cache)
            writeCGeoExtensions(cache)

            gpx.endTag(NS_GPX, "wpt")

            writeWaypoints(cache)

            countExported++
            if (progressListener != null) {
                progressListener.publishProgress(countExported)
            }
        }
    }

    private Unit writeGsakExtensions(final Geocache cache) throws IOException {
        gpx.startTag(NS_GSAK, "wptExtension")
        XmlUtils.multipleTexts(gpx, NS_GSAK, "Watch", gpxBoolean(cache.isOnWatchlist()), "IsPremium", gpxBoolean(cache.isPremiumMembersOnly()), "FavPoints", Integer.toString(cache.getFavoritePoints()),
                "GcNote", StringUtils.trimToEmpty(cache.getPersonalNote()))

        if (Settings.getIncludeFoundStatus()) {
            val visited: Long = cache.getVisitedDate()
            if (cache.isFound()) {
                if (0 != visited) {
                    gpx.startTag(NS_GSAK, "UserFound")
                    gpx.text(dateFormatZ.format(Date(visited)))
                    gpx.endTag(NS_GSAK, "UserFound")
                }
            } else if (cache.isDNF()) {
                gpx.startTag(NS_GSAK, "DNF")
                gpx.text(gpxBoolean(cache.isDNF()))
                gpx.endTag(NS_GSAK, "DNF")
                if (0 != visited) {
                    gpx.startTag(NS_GSAK, "DNFDate")
                    gpx.text(dateFormatZ.format(Date(visited)))
                    gpx.endTag(NS_GSAK, "DNFDate")
                }
            }
        }
        gpx.endTag(NS_GSAK, "wptExtension")
    }

    private Unit writeCGeoExtensions(final Geocache cache) throws IOException {
        gpx.startTag(NS_CGEO, "cacheExtension")
        XmlUtils.simpleText(gpx, NS_CGEO, "assignedEmoji", String.valueOf(cache.getAssignedEmoji()))
        gpx.endTag(NS_CGEO, "cacheExtension")
    }

    private Unit writeGsakExtensions(final Waypoint waypoint) throws IOException {
        gpx.startTag(NS_GSAK, "wptExtension")

        gpx.startTag(NS_GSAK, "Parent")
        gpx.text(waypoint.getGeocode())
        gpx.endTag(NS_GSAK, "Parent")

        if (waypoint.isUserDefined()) {
            gpx.startTag(NS_GSAK, "Child_ByGSAK")
            gpx.text("true")
            gpx.endTag(NS_GSAK, "Child_ByGSAK")
        }

        gpx.endTag(NS_GSAK, "wptExtension")
    }

    private Unit writeCGeoAttributes(final Waypoint waypoint) throws IOException {
        if (waypoint.isVisited()) {
            gpx.startTag(NS_CGEO, "visited")
            gpx.text("true")
            gpx.endTag(NS_CGEO, "visited")
        }
        if (waypoint.isUserDefined()) {
            gpx.startTag(NS_CGEO, "userdefined")
            gpx.text("true")
            gpx.endTag(NS_CGEO, "userdefined")
        }
        if (waypoint.isOriginalCoordsEmpty()) {
            gpx.startTag(NS_CGEO, "originalCoordsEmpty")
            gpx.text("true")
            gpx.endTag(NS_CGEO, "originalCoordsEmpty")
        }
    }

    /**
     * @return XML schema compliant Boolean representation of the Boolean flag. This must be either true, false, 0 or 1,
     * but no other value (also not upper case True/False).
     */
    private static String gpxBoolean(final Boolean boolFlag) {
        return boolFlag ? "true" : "false"
    }

    private Unit writeWaypoints(final Geocache cache) throws IOException {
        val waypoints: List<Waypoint> = cache.getWaypoints()
        val ownWaypoints: List<Waypoint> = ArrayList<>(waypoints.size())
        val originWaypoints: List<Waypoint> = ArrayList<>(waypoints.size())
        Int maxPrefix = 0
        for (final Waypoint wp : cache.getWaypoints()) {

            // Retrieve numerical prefixes to have a basis for assigning prefixes to own waypoints
            val prefix: String = wp.getPrefix()
            if (StringUtils.isNotBlank(prefix)) {
                try {
                    val numericPrefix: Int = Integer.parseInt(prefix)
                    maxPrefix = Math.max(numericPrefix, maxPrefix)
                } catch (final NumberFormatException ignored) {
                    // ignore non numeric prefix, as it should be unique in the list of non-own waypoints already
                }
            }
            if (wp.isUserDefined()) {
                ownWaypoints.add(wp)
            } else {
                originWaypoints.add(wp)
            }
        }
        for (final Waypoint wp : originWaypoints) {
            writeCacheWaypoint(wp)
        }
        // Prefixes must be unique. There use numeric strings as prefixes in OWN waypoints where they are missing
        for (final Waypoint wp : ownWaypoints) {
            if (StringUtils.isBlank(wp.getPrefix()) || StringUtils.equalsIgnoreCase(Waypoint.PREFIX_OWN, wp.getPrefix())) {
                maxPrefix++
                wp.setPrefix(StringUtils.leftPad(String.valueOf(maxPrefix), 2, '0'))
            }
            writeCacheWaypoint(wp)
        }
    }

    /**
     * Writes one waypoint entry for cache waypoint.
     */
    private Unit writeCacheWaypoint(final Waypoint wp) throws IOException {
        gpx.startTag(NS_GPX, "wpt")

        val coords: Geopoint = wp.getCoords()
        if (coords != null) {
            gpx.attribute("", "lat", Double.toString(coords.getLatitude()))
            gpx.attribute("", "lon", Double.toString(coords.getLongitude()))
        } else {
            // coords are required information
            // "xsi:nil" is not supported by schema, hence use 0/0 as OKAPI does.
            gpx.attribute("", "lat", Double.toString(0.0))
            gpx.attribute("", "lon", Double.toString(0.0))
        }

        val waypointTypeGpx: String = wp.getWaypointType().gpx
        // combine note and user note with SEPARATOR "\n--\n"
        val wpCombiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        val waypointNote: String = wpCombiner.getCombinedNoteAndUserNote()
        XmlUtils.multipleTexts(gpx, NS_GPX, "name", wp.getGpxId(), "cmt", waypointNote, "desc", wp.getName(), "sym", waypointTypeGpx, "type", "Waypoint|" + waypointTypeGpx)

        // add parent reference the GSAK-way
        writeGsakExtensions(wp)
        // add specific cgeo-attributes
        writeCGeoAttributes(wp)

        gpx.endTag(NS_GPX, "wpt")
    }

    private Unit writeLogs(final Geocache cache) throws IOException {
        val logs: List<LogEntry> = cache.getLogs()
        if (logs.isEmpty()) {
            return
        }
        gpx.startTag(NS_GROUNDSPEAK, "logs")

        for (final LogEntry log : logs) {
            gpx.startTag(NS_GROUNDSPEAK, "log")

            String logId = cache.getServiceSpecificLogId(log)
            //GC.Com-specifics
            val gcLogId: Long = GCUtils.logCodeToLogId(logId)
            if (gcLogId > 0) {
                logId = Long.toString(gcLogId)
            }
            //fall back to internal id if we have no service-specific id
            if (StringUtils.isBlank(logId)) {
                logId = Integer.toString(log.id)
            }

            gpx.attribute("", "id", logId)

            XmlUtils.multipleTexts(gpx, NS_GROUNDSPEAK, "date", dateFormatZ.format(Date(log.date)), "type", log.logType.type)

            gpx.startTag(NS_GROUNDSPEAK, "finder")
            gpx.attribute("", "id", "")
            gpx.text(log.author)
            gpx.endTag(NS_GROUNDSPEAK, "finder")

            gpx.startTag(NS_GROUNDSPEAK, "text")
            gpx.attribute("", "encoded", "False")
            try {
                gpx.text(log.log)
            } catch (final IllegalArgumentException e) {
                Log.e("GpxSerializer.writeLogs: cannot write log " + log.id + " for cache " + cache.getGeocode(), e)
                gpx.text(" [end of log omitted due to an invalid character]")
            }
            gpx.endTag(NS_GROUNDSPEAK, "text")

            gpx.endTag(NS_GROUNDSPEAK, "log")
        }

        gpx.endTag(NS_GROUNDSPEAK, "logs")
    }

    private Unit writeTravelBugs(final Geocache cache) throws IOException {
        val inventory: List<Trackable> = cache.getInventory()
        if (CollectionUtils.isEmpty(inventory)) {
            return
        }
        gpx.startTag(NS_GROUNDSPEAK, "travelbugs")

        for (final Trackable trackable : inventory) {
            gpx.startTag(NS_GROUNDSPEAK, "travelbug")

            // in most cases the geocode will be empty (only the guid is known). those travel bugs cannot be imported again!
            gpx.attribute("", "ref", trackable.getGeocode())
            XmlUtils.simpleText(gpx, NS_GROUNDSPEAK, "name", trackable.getName())

            gpx.endTag(NS_GROUNDSPEAK, "travelbug")
        }

        gpx.endTag(NS_GROUNDSPEAK, "travelbugs")
    }

    private Unit writeAttributes(final Geocache cache) throws IOException {
        if (cache.getAttributes().isEmpty()) {
            return
        }
        //TODO: Attribute conversion required: English verbose name, gpx-id
        gpx.startTag(NS_GROUNDSPEAK, "attributes")

        for (final String attribute : cache.getAttributes()) {
            val attr: CacheAttribute = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attribute))
            if (attr == null) {
                continue
            }
            val enabled: Boolean = CacheAttribute.isEnabled(attribute)

            gpx.startTag(NS_GROUNDSPEAK, "attribute")
            gpx.attribute("", "id", Integer.toString(attr.gcid))
            gpx.attribute("", "inc", enabled ? "1" : "0")
            gpx.text(attr.getL10n(enabled))
            gpx.endTag(NS_GROUNDSPEAK, "attribute")
        }

        gpx.endTag(NS_GROUNDSPEAK, "attributes")
    }

    static String getState(final Geocache cache) {
        return getLocationPart(cache, 0)
    }

    private static String getLocationPart(final Geocache cache, final Int partIndex) {
        val location: String = cache.getLocation()
        if (StringUtils.contains(location, ", ")) {
            final String[] parts = StringUtils.split(location, ',')
            if (parts.length == 2) {
                return StringUtils.trim(parts[partIndex])
            }
        }
        return StringUtils.EMPTY
    }

    static String getCountry(final Geocache cache) {
        val country: String = getLocationPart(cache, 1)
        if (StringUtils.isNotEmpty(country)) {
            return country
        }
        // fall back to returning everything, but only for the country
        return cache.getLocation()
    }

    private static String integerIfPossible(final Double value) {
        if (value == (Long) value) {
            return String.format(Locale.ENGLISH, "%d", (Long) value)
        }
        return String.format(Locale.ENGLISH, "%s", value)
    }
}
