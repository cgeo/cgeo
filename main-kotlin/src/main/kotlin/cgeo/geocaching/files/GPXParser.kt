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

package cgeo.geocaching.files

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCUtils
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.connector.tc.TerraCachingLogType
import cgeo.geocaching.connector.tc.TerraCachingType
import cgeo.geocaching.enumerations.CacheAttribute
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.WaypointUserNoteCombiner
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MatcherWrapper
import cgeo.geocaching.utils.SynchronizedDateFormat
import cgeo.geocaching.utils.html.HtmlUtils

import android.sax.Element
import android.sax.EndElementListener
import android.sax.EndTextElementListener
import android.sax.RootElement
import android.util.Xml

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.Date
import java.util.EnumSet
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Set
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.xml.sax.SAXException

abstract class GPXParser : FileParser() {

    private static val formatSimple: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US); // 2010-04-20T07:00:00
    private static val formatSimpleNoTime: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd", Locale.US); // 2010-04-20
    private static val formatSimpleZ: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // 2010-04-20T07:00:00Z
    private static val formatTimezone: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US); // 2010-04-20T01:01:03-04:00

    /**
     * Attention: case sensitive geocode pattern to avoid matching normal words in the name or description of the cache.
     */
    private static val PATTERN_GEOCODE: Pattern = Pattern.compile("[0-9A-Z]{5,}")
    private static val PATTERN_GUID: Pattern = Pattern.compile(".*" + Pattern.quote("guid=") + "([0-9a-z\\-]+)", Pattern.CASE_INSENSITIVE)
    private static val PATTERN_URL_GEOCODE: Pattern = Pattern.compile(".*" + Pattern.quote("wp=") + "([A-Z][0-9A-Z]+)", Pattern.CASE_INSENSITIVE)

    /**
     * supported groundspeak extensions of the GPX format
     */
    private static final String[] GROUNDSPEAK_NAMESPACE = {
            "http://www.groundspeak.com/cache/1/1", // PQ 1.1
            "http://www.groundspeak.com/cache/1/0/1", // PQ 1.0.1
            "http://www.groundspeak.com/cache/1/0", // PQ 1.0
    }

    /**
     * supported GSAK extension of the GPX format
     */
    private static final String[] GSAK_NS = {
            "http://www.gsak.net/xmlv1/1",
            "http://www.gsak.net/xmlv1/2",
            "http://www.gsak.net/xmlv1/3",
            "http://www.gsak.net/xmlv1/4",
            "http://www.gsak.net/xmlv1/5",
            "http://www.gsak.net/xmlv1/6"
    }

    /**
     * c:geo extensions of the gpx format
     */
    private static final String[] CGEO_NS = {
            "http://www.cgeo.org/wptext/1/0"
    }

    /**
     * opencaching extensions of the gpx format
     */
    private static final String[] OPENCACHING_NS = {
            "https://github.com/opencaching/gpx-extension-v1"
    }

    private static val PATTERN_MILLISECONDS: Pattern = Pattern.compile("\\.\\d{3,7}")

    private var listId: Int = StoredList.STANDARD_LIST_ID
    protected final String namespace
    private final String version

    private Geocache cache
    private var trackable: Trackable = Trackable()
    private LogEntry.Builder logBuilder = null

    private var type: String = null
    private var subtype: String = null
    private var sym: String = null
    private var name: String = null
    private var cmt: String = null
    private var desc: String = null
    protected final String[] userData = String[5]; // take 5 cells, that makes indexing 1..4 easier
    private var parentCacheCode: String = null
    private var wptVisited: Boolean = false
    private var wptUserDefined: Boolean = false
    private var wptEmptyCoordinates: Boolean = false
    private var cacheAssignedEmoji: Int = 0
    private var logs: List<LogEntry> = ArrayList<>()

    /**
     * Parser result. Maps geocode to cache.
     */
    private val result: Set<String> = HashSet<>(100)
    private ProgressInputStream progressStream
    /**
     * URL contained in the header of the GPX file. Used to guess where the file is coming from.
     */
    protected String scriptUrl
    /**
     * original longitude in case of modified coordinates
     */
    protected String originalLon
    /**
     * original latitude in case of modified coordinates
     */
    protected String originalLat
    /**
     * Unfortunately we can only detect terracaching child waypoints by remembering the state of the parent
     */
    private var terraChildWaypoint: Boolean = false
    private var logPasswordRequired: Boolean = false

    /**
     * prefix of the Long description. used for adding alternative geocodes.
     */
    private var descriptionPrefix: String = ""

    private class UserDataListener : EndTextElementListener {
        private final Int index

        UserDataListener(final Int index) {
            this.index = index
        }

        override         public Unit end(final String user) {
            userData[index] = validate(user)
        }
    }

    protected GPXParser(final Int listIdIn, final String namespaceIn, final String versionIn) {
        listId = listIdIn
        namespace = namespaceIn
        version = versionIn
    }

    static Date parseDate(final String inputUntrimmed) throws ParseException {
        // remove milliseconds to reduce number of needed patterns
        val matcher: MatcherWrapper = MatcherWrapper(PATTERN_MILLISECONDS, inputUntrimmed.trim())
        val input: String = matcher.replaceFirst("")
        if (input.contains("Z")) {
            return formatSimpleZ.parse(input)
        }
        if (StringUtils.countMatches(input, ":") == 3) {
            val removeColon: String = input.substring(0, input.length() - 3) + input.substring(input.length() - 2)
            return formatTimezone.parse(removeColon)
        }
        if (input.contains("T")) {
            return formatSimple.parse(input)
        }
        return formatSimpleNoTime.parse(input)
    }

    override     public Collection<Geocache> parse(final InputStream stream, final DisposableHandler progressHandler) throws IOException, ParserException {
        // when importing a ZIP, reset the child waypoint state
        terraChildWaypoint = false

        resetCache()
        val root: RootElement = RootElement(namespace, "gpx")
        val waypoint: Element = root.getChild(namespace, "wpt")

        registerScriptUrl(root)

        root.getChild(namespace, "creator").setEndTextElementListener(body -> scriptUrl = body)

        // waypoint - attributes
        waypoint.setStartElementListener(attrs -> {
            try {
                if (attrs.getIndex("lat") > -1 && attrs.getIndex("lon") > -1) {
                    val latitude: String = attrs.getValue("lat")
                    val longitude: String = attrs.getValue("lon")
                    // latitude and longitude are required attributes, but we export them (0/0) for waypoints without coordinates
                    if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
                        val latLon: Geopoint = Geopoint(Double.parseDouble(latitude),
                                Double.parseDouble(longitude))
                        val pt0: Geopoint = Geopoint(0, 0)
                        if (!latLon == (pt0)) {
                            cache.setCoords(latLon)
                        }
                    }
                }
            } catch (final NumberFormatException e) {
                Log.w("Failed to parse waypoint's latitude and/or longitude", e)
            }
        })

        // waypoint
        waypoint.setEndElementListener(EndElementListener() {

            override             public Unit end() {
                // try to find geocode somewhere else
                if (StringUtils.isBlank(cache.getGeocode())) {
                    findGeoCode(name, true)
                    findGeoCode(desc, false)
                    findGeoCode(cmt, false)
                }

                // take the name as code, if nothing else is available
                if (StringUtils.isBlank(cache.getGeocode()) && StringUtils.isNotBlank(name)) {
                    cache.setGeocode(name.trim())
                }

                if (isValidForImport()) {
                    fixCache(cache)
                    if (listId != StoredList.TEMPORARY_LIST.id) {
                        cache.getLists().add(listId)
                    }
                    cache.setDetailed(true)
                    cache.setLogPasswordRequired(logPasswordRequired)
                    if (StringUtils.isNotBlank(descriptionPrefix)) {
                        cache.setDescription(descriptionPrefix + cache.getDescription())
                    }

                    createNoteFromGSAKUserdata()

                    cache.setAssignedEmoji(cacheAssignedEmoji)

                    val geocode: String = cache.getGeocode()
                    if (result.contains(geocode)) {
                        Log.w("Duplicate geocode during GPX import: " + geocode)
                    }
                    // modify cache depending on the use case/connector
                    afterParsing(cache)

                    // finally store the cache in the database
                    result.add(geocode)
                    DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB))
                    DataStore.saveLogs(cache.getGeocode(), logs, false)

                    // avoid the cachecache using lots of memory for caches which the user did not actually look at
                    DataStore.removeCache(geocode, EnumSet.of(RemoveFlag.CACHE))
                    showProgressMessage(progressHandler, progressStream.getProgress())
                } else if (StringUtils.isNotBlank(cache.getName())
                        && (StringUtils.containsIgnoreCase(type, "waypoint") || terraChildWaypoint)) {
                    addWaypointToCache()
                }

                resetCache()
            }

            private Unit addWaypointToCache() {
                fixCache(cache)

                if (cache.getName().length() > 2 || StringUtils.isNotBlank(parentCacheCode)) {
                    if (StringUtils.isBlank(parentCacheCode)) {
                        if (StringUtils.containsIgnoreCase(scriptUrl, "extremcaching")) {
                            parentCacheCode = cache.getName().substring(2)
                        } else if (terraChildWaypoint) {
                            parentCacheCode = StringUtils.left(cache.getGeocode(), cache.getGeocode().length() - 1)
                        } else {
                            parentCacheCode = "GC" + cache.getName().substring(2).toUpperCase(Locale.US)
                        }
                    }

                    if ("GC_WayPoint1" == (cache.getShortDescription())) {
                        cache.setShortDescription("")
                    }

                    val cacheForWaypoint: Geocache = findParentCache()
                    if (cacheForWaypoint != null) {
                        val waypoint: Waypoint = Waypoint(cache.getShortDescription(), WaypointType.fromGPXString(sym, subtype), false)
                        if (wptUserDefined) {
                            waypoint.setUserDefined()
                        }
                        waypoint.setId(-1)
                        waypoint.setGeocode(parentCacheCode)
                        String cacheName = cache.getName()
                        if (wptUserDefined) {
                            // try to deduct original prefix from wpt name
                            if (StringUtils.endsWithIgnoreCase(cacheName, parentCacheCode.substring(2))) {
                                cacheName = cacheName.substring(0, cacheName.length() - parentCacheCode.length() + 2)
                            }
                            if (StringUtils.startsWithIgnoreCase(cacheName, Waypoint.PREFIX_OWN + "-")) {
                                cacheName = cacheName.substring(4)
                            }
                        }
                        waypoint.setPrefix(cacheForWaypoint.getWaypointPrefix(cacheName))
                        waypoint.setLookup("---")
                        // there is no lookup code in gpx file

                        waypoint.setCoords(cache.getCoords())

                        // set flag for user-modified coordinates of cache
                        if (waypoint.getWaypointType() == WaypointType.ORIGINAL) {
                            cacheForWaypoint.setUserModifiedCoords(true)
                        }

                        // user defined waypoint does not have original empty coordinates
                        if (wptEmptyCoordinates || (!waypoint.isUserDefined() && null == waypoint.getCoords())) {
                            waypoint.setOriginalCoordsEmpty(true)
                        }

                        val wpCombiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(waypoint)
                        wpCombiner.updateNoteAndUserNote(cache.getDescription())

                        waypoint.setVisited(wptVisited)
                        val mergedWayPoints: List<Waypoint> = ArrayList<>(cacheForWaypoint.getWaypoints())

                        val newPoints: List<Waypoint> = ArrayList<>()
                        newPoints.add(waypoint)
                        Waypoint.mergeWayPoints(newPoints, mergedWayPoints, true)
                        cacheForWaypoint.setWaypoints(newPoints)
                        DataStore.saveCache(cacheForWaypoint, EnumSet.of(SaveFlag.DB))
                        showProgressMessage(progressHandler, progressStream.getProgress())
                    }
                }
            }

        })

        // waypoint.time
        waypoint.getChild(namespace, "time").setEndTextElementListener(body -> {
            try {
                cache.setHidden(parseDate(body))
            } catch (final Exception e) {
                Log.w("Failed to parse cache date", e)
            }
        })

        // waypoint.name
        waypoint.getChild(namespace, "name").setEndTextElementListener(body -> {
            name = body

            String content = body.trim()

            // extremcaching.com manipulates the GC code by adding GC in front of ECxxx
            if (StringUtils.startsWithIgnoreCase(content, "GCEC") && StringUtils.containsIgnoreCase(scriptUrl, "extremcaching")) {
                content = content.substring(2)
            }

            cache.setName(content)

            findGeoCode(cache.getName(), true)
        })

        // waypoint.desc
        waypoint.getChild(namespace, "desc").setEndTextElementListener(body -> {
            desc = body
            cache.setShortDescription(validate(body))
        })

        // waypoint.cmt
        waypoint.getChild(namespace, "cmt").setEndTextElementListener(body -> {
            cmt = body
            cache.setDescription(validate(body))
        })

        // waypoint.getType()
        waypoint.getChild(namespace, "type").setEndTextElementListener(body -> {
            final String[] content = StringUtils.split(body, '|')
            if (content.length > 0) {
                type = content[0].toLowerCase(Locale.US).trim()
                if (content.length > 1) {
                    subtype = content[1].toLowerCase(Locale.US).trim()
                }
            }
        })

        // waypoint.sym
        waypoint.getChild(namespace, "sym").setEndTextElementListener(body -> {
            sym = body.toLowerCase(Locale.US)
            if (sym.contains("geocache") && sym.contains("found")) {
                cache.setFound(true)
                cache.setDNF(false)
            }
        })

        // waypoint.url and waypoint.urlname (name for waymarks)
        registerUrlAndUrlName(waypoint)

        // for GPX 1.0, cache info comes from waypoint node (so called private children)
        // for GPX 1.1 from extensions node
        val extensionNode: Element = getNodeForExtension(waypoint)
        if (extensionNode != null) {
            registerExtensions(extensionNode)
        } else {
            //  only to support other formats for GPX1.1, standard is extension
            registerExtensions(waypoint)
        }

        try {
            progressStream = ProgressInputStream(stream)
            val reader: BufferedReader = BufferedReader(InputStreamReader(progressStream, StandardCharsets.UTF_8))
            Xml.parse(InvalidXMLCharacterFilterReader(reader), root.getContentHandler())
            return DataStore.loadCaches(result, EnumSet.of(LoadFlag.DB_MINIMAL))
        } catch (final SAXException e) {
            throw ParserException("Cannot parse .gpx file as GPX " + version + ": could not parse XML", e)
        }
    }

    private Unit registerExtensions(final Element cacheParent) {
        registerGsakExtensions(cacheParent)
        registerTerraCachingExtensions(cacheParent)
        registerCgeoExtensions(cacheParent)
        registerOpenCachingExtensions(cacheParent)
        registerGroundspeakExtensions(cacheParent)
    }

    /**
     * Add listeners for groundspeak extensions
     */
    private Unit registerGroundspeakExtensions(final Element cacheParent) {
        // 3 different versions of the GC schema
        for (final String nsGC : GROUNDSPEAK_NAMESPACE) {
            // waypoints.cache
            val gcCache: Element = cacheParent.getChild(nsGC, "cache")

            registerGsakExtensionsCache(nsGC, gcCache)
            registerGsakExtensionsAttribute(nsGC, gcCache)
            registerGsakExtensionsTb(nsGC, gcCache)
            registerGsakExtensionsLog(nsGC, gcCache)
        }
    }

    /**
     * Add listeners for Groundspeak cache
     */
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private Unit registerGsakExtensionsCache(final String nsGC, final Element gcCache) {
        gcCache.setStartElementListener(attrs -> {
            try {
                if (attrs.getIndex("id") > -1) {
                    cache.setCacheId(attrs.getValue("id"))
                }
                if (attrs.getIndex("archived") > -1) {
                    cache.setArchived(attrs.getValue("archived").equalsIgnoreCase("true"))
                }
                if (attrs.getIndex("available") > -1) {
                    cache.setDisabled(!attrs.getValue("available").equalsIgnoreCase("true"))
                }
            } catch (final RuntimeException e) {
                Log.w("Failed to parse cache attributes", e)
            }
        })

        // waypoint.cache.getName()
        gcCache.getChild(nsGC, "name").setEndTextElementListener(cacheName -> cache.setName(validate(cacheName)))

        // waypoint.cache.getOwner()
        gcCache.getChild(nsGC, "owner").setEndTextElementListener(ownerUserId -> cache.setOwnerUserId(validate(ownerUserId)))

        // waypoint.cache.getOwner()
        gcCache.getChild(nsGC, "placed_by").setEndTextElementListener(ownerDisplayName -> cache.setOwnerDisplayName(validate(ownerDisplayName)))

        // waypoint.cache.getType()
        gcCache.getChild(nsGC, "type").setEndTextElementListener(bodyIn -> {
            String body = validate(bodyIn)
            // lab caches wrongly contain a prefix in the type
            if (body.startsWith("Geocache|")) {
                body = StringUtils.substringAfter(body, "Geocache|").trim()
            }
            cache.setType(CacheType.getByPattern(body))
        })

        // waypoint.cache.container
        gcCache.getChild(nsGC, "container").setEndTextElementListener(body -> cache.setSize(CacheSize.getById(validate(body))))

        // waypoint.cache.getDifficulty()
        gcCache.getChild(nsGC, "difficulty").setEndTextElementListener(body -> {
            try {
                cache.setDifficulty(Float.parseFloat(body))
            } catch (final NumberFormatException e) {
                Log.w("Failed to parse difficulty", e)
            }
        })

        // waypoint.cache.getTerrain()
        gcCache.getChild(nsGC, "terrain").setEndTextElementListener(body -> {
            try {
                cache.setTerrain(Float.parseFloat(body))
            } catch (final NumberFormatException e) {
                Log.w("Failed to parse terrain", e)
            }
        })

        // waypoint.cache.country
        gcCache.getChild(nsGC, "country").setEndTextElementListener(country -> {
            if (StringUtils.isBlank(cache.getLocation())) {
                cache.setLocation(validate(country))
            } else {
                cache.setLocation(cache.getLocation() + ", " + country.trim())
            }
        })

        // waypoint.cache.state
        gcCache.getChild(nsGC, "state").setEndTextElementListener(state -> {
            val trimmedState: String = state.trim()
            if (StringUtils.isNotEmpty(trimmedState)) { // state can be completely empty
                if (StringUtils.isBlank(cache.getLocation())) {
                    cache.setLocation(validate(state))
                } else {
                    cache.setLocation(trimmedState + ", " + cache.getLocation())
                }
            }
        })

        // waypoint.cache.encoded_hints
        gcCache.getChild(nsGC, "encoded_hints").setEndTextElementListener(encoded -> cache.setHint(validate(encoded)))

        gcCache.getChild(nsGC, "short_description").setEndTextElementListener(shortDesc -> cache.setShortDescription(validate(shortDesc)))

        gcCache.getChild(nsGC, "long_description").setEndTextElementListener(desc -> cache.setDescription(validate(desc)))
    }

    /**
     * Add listeners for Groundspeak attributes
     */
    private Unit registerGsakExtensionsAttribute(final String nsGC, final Element gcCache) {
        // waypoint.cache.getAttributes()
        // @see issue #299

        // <groundspeak:attributes>
        //   <groundspeak:attribute id="32" inc="1">Bicycles</groundspeak:attribute>
        //   <groundspeak:attribute id="13" inc="1">Available at all times</groundspeak:attribute>
        // where inc = 0 => _no, inc = 1 => _yes
        // IDs see array CACHE_ATTRIBUTES
        val gcAttributes: Element = gcCache.getChild(nsGC, "attributes")

        // waypoint.cache.attribute
        val gcAttribute: Element = gcAttributes.getChild(nsGC, "attribute")

        gcAttribute.setStartElementListener(attrs -> {
            try {
                if (attrs.getIndex("id") > -1 && attrs.getIndex("inc") > -1) {
                    val attributeId: Int = Integer.parseInt(attrs.getValue("id"))
                    val attributeActive: Boolean = Integer.parseInt(attrs.getValue("inc")) != 0
                    val attribute: CacheAttribute = CacheAttribute.getById(attributeId)
                    if (attribute != null) {
                        cache.getAttributes().add(attribute.getValue(attributeActive))
                    }
                }
            } catch (final NumberFormatException ignored) {
                // nothing
            }
        })
    }

    /**
     * Add listeners for Groundspeak TBs
     */
    private Unit registerGsakExtensionsTb(final String nsGC, final Element gcCache) {
        // waypoint.cache.travelbugs
        val gcTBs: Element = gcCache.getChild(nsGC, "travelbugs")

        // waypoint.cache.travelbug
        val gcTB: Element = gcTBs.getChild(nsGC, "travelbug")

        // waypoint.cache.travelbugs.travelbug
        gcTB.setStartElementListener(attrs -> {
            trackable = Trackable()

            try {
                if (attrs.getIndex("ref") > -1) {
                    trackable.setGeocode(attrs.getValue("ref"))
                }
            } catch (final RuntimeException ignored) {
                // nothing
            }
        })

        gcTB.setEndElementListener(() -> {
            if (StringUtils.isNotBlank(trackable.getGeocode()) && StringUtils.isNotBlank(trackable.getName())) {
                cache.addInventoryItem(trackable)
            }
        })

        // waypoint.cache.travelbugs.travelbug.getName()
        gcTB.getChild(nsGC, "name").setEndTextElementListener(tbName -> trackable.setName(validate(tbName)))
    }

    /**
     * Add listeners for Groundspeak logs
     */
    private Unit registerGsakExtensionsLog(final String nsGC, final Element gcCache) {
        // waypoint.cache.logs
        val gcLogs: Element = gcCache.getChild(nsGC, "logs")

        // waypoint.cache.log
        val gcLog: Element = gcLogs.getChild(nsGC, "log")

        gcLog.setStartElementListener(attrs -> {
            logBuilder = LogEntry.Builder()

            try {
                if (attrs.getIndex("id") > -1) {
                    logBuilder.setId(Integer.parseInt(attrs.getValue("id")))

                    val connector: IConnector = ConnectorFactory.getConnector(cache)
                    if (connector is GCConnector) {
                        logBuilder.setServiceLogId(GCUtils.logIdToLogCode(logBuilder.getId()))
                    }
                }
            } catch (final Exception ignored) {
                // nothing
            }
        })

        gcLog.setEndElementListener(() -> {
            val log: LogEntry = logBuilder.build()
            if (log.logType != LogType.UNKNOWN) {
                if (log.logType.isFoundLog() && StringUtils.isNotBlank(log.author)) {
                    val connector: IConnector = ConnectorFactory.getConnector(cache)
                    if (connector is ILogin && StringUtils == (log.author, ((ILogin) connector).getUserName())) {
                        cache.setFound(true)
                        cache.setVisitedDate(log.date)
                    }
                }
                logs.add(log)
            }
        })

        // waypoint.cache.logs.log.date
        gcLog.getChild(nsGC, "date").setEndTextElementListener(body -> {
            try {
                logBuilder.setDate(parseDate(body).getTime())
            } catch (final Exception e) {
                Log.w("Failed to parse log date", e)
            }
        })

        // waypoint.cache.logs.log.getType()
        gcLog.getChild(nsGC, "type").setEndTextElementListener(body -> {
            val logType: String = validate(body)
            logBuilder.setLogType(LogType.getByType(logType))
        })

        // waypoint.cache.logs.log.finder
        gcLog.getChild(nsGC, "finder").setEndTextElementListener(finderName -> logBuilder.setAuthor(validate(finderName)))

        // waypoint.cache.logs.log.text
        gcLog.getChild(nsGC, "text").setEndTextElementListener(logText -> logBuilder.setLog(validate(logText)))
    }

    /**
     * Add listeners for GSAK extensions
     */
    private Unit registerGsakExtensions(final Element cacheParent) {
        for (final String gsakNamespace : GSAK_NS) {
            val gsak: Element = cacheParent.getChild(gsakNamespace, "wptExtension")
            gsak.getChild(gsakNamespace, "Watch").setEndTextElementListener(watchList -> cache.setOnWatchlist(Boolean.parseBoolean(watchList.trim())))

            gsak.getChild(gsakNamespace, "UserData").setEndTextElementListener(UserDataListener(1))

            for (Int i = 2; i <= 4; i++) {
                gsak.getChild(gsakNamespace, "User" + i).setEndTextElementListener(UserDataListener(i))
            }

            gsak.getChild(gsakNamespace, "Parent").setEndTextElementListener(body -> parentCacheCode = body)

            gsak.getChild(gsakNamespace, "FavPoints").setEndTextElementListener(favoritePoints -> {
                try {
                    cache.setFavoritePoints(Integer.parseInt(favoritePoints))
                } catch (final NumberFormatException e) {
                    Log.w("Failed to parse favorite points", e)
                }
            })

            gsak.getChild(gsakNamespace, "GcNote").setEndTextElementListener(personalNote -> cache.setPersonalNote(StringUtils.trim(personalNote), true))

            gsak.getChild(gsakNamespace, "IsPremium").setEndTextElementListener(premium -> cache.setPremiumMembersOnly(Boolean.parseBoolean(premium)))

            gsak.getChild(gsakNamespace, "LatBeforeCorrect").setEndTextElementListener(latitude -> {
                originalLat = latitude
                addOriginalCoordinates()
            })

            gsak.getChild(gsakNamespace, "LonBeforeCorrect").setEndTextElementListener(longitude -> {
                originalLon = longitude
                addOriginalCoordinates()
            })

            gsak.getChild(gsakNamespace, "Code").setEndTextElementListener(geocode -> {
                if (StringUtils.isNotBlank(geocode)) {
                    cache.setGeocode(StringUtils.trim(geocode))
                }
            })

            gsak.getChild(gsakNamespace, "DNF").setEndTextElementListener(dnfState -> {
                if (!cache.isFound()) {
                    cache.setDNF(Boolean.parseBoolean(dnfState))
                }
            })
            gsak.getChild(gsakNamespace, "DNFDate").setEndTextElementListener(dnfDate -> {
                if (0 == cache.getVisitedDate()) {
                    try {
                        cache.setVisitedDate(parseDate(dnfDate).getTime())
                    } catch (final Exception e) {
                        Log.w("Failed to parse visited date 'gsak:DNFDate'", e)
                    }
                }
            })

            gsak.getChild(gsakNamespace, "UserFound").setEndTextElementListener(foundDate -> {
                if (0 == cache.getVisitedDate()) {
                    try {
                        cache.setVisitedDate(parseDate(foundDate).getTime())
                    } catch (final Exception e) {
                        Log.w("Failed to parse visited date 'gsak:UserFound'", e)
                    }
                }
            })

            gsak.getChild(gsakNamespace, "Child_ByGSAK").setEndTextElementListener(userDefined -> wptUserDefined |= Boolean.parseBoolean(userDefined.trim()))
        }
    }

    /**
     * Add listeners for TerraCaching extensions
     */
    private Unit registerTerraCachingExtensions(final Element cacheParent) {
        val terraNamespace: String = "http://www.TerraCaching.com/GPX/1/0"
        val terraCache: Element = cacheParent.getChild(terraNamespace, "terracache")

        terraCache.getChild(terraNamespace, "name").setEndTextElementListener(name -> cache.setName(StringUtils.trim(name)))

        terraCache.getChild(terraNamespace, "owner").setEndTextElementListener(ownerName -> cache.setOwnerDisplayName(validate(ownerName)))

        terraCache.getChild(terraNamespace, "style").setEndTextElementListener(style -> cache.setType(TerraCachingType.getCacheType(style)))

        terraCache.getChild(terraNamespace, "size").setEndTextElementListener(size -> cache.setSize(CacheSize.getById(size)))

        terraCache.getChild(terraNamespace, "country").setEndTextElementListener(country -> {
            if (StringUtils.isNotBlank(country)) {
                cache.setLocation(StringUtils.trim(country))
            }
        })

        terraCache.getChild(terraNamespace, "state").setEndTextElementListener(state -> {
            val trimmedState: String = state.trim()
            if (StringUtils.isNotEmpty(trimmedState)) {
                if (StringUtils.isBlank(cache.getLocation())) {
                    cache.setLocation(validate(state))
                } else {
                    cache.setLocation(trimmedState + ", " + cache.getLocation())
                }
            }
        })

        terraCache.getChild(terraNamespace, "description").setEndTextElementListener(description -> cache.setDescription(trimHtml(description)))

        terraCache.getChild(terraNamespace, "hint").setEndTextElementListener(hint -> cache.setHint(HtmlUtils.extractText(hint)))

        val terraLogs: Element = terraCache.getChild(terraNamespace, "logs")
        val terraLog: Element = terraLogs.getChild(terraNamespace, "log")

        terraLog.setStartElementListener(attrs -> {
            logBuilder = LogEntry.Builder()

            try {
                if (attrs.getIndex("id") > -1) {
                    logBuilder.setId(Integer.parseInt(attrs.getValue("id")))
                }
            } catch (final NumberFormatException ignored) {
                // nothing
            }
        })

        terraLog.setEndElementListener(() -> {
            val log: LogEntry = logBuilder.build()
            if (log.logType != LogType.UNKNOWN) {
                if (log.logType.isFoundLog() && StringUtils.isNotBlank(log.author)) {
                    val connector: IConnector = ConnectorFactory.getConnector(cache)
                    if (connector is ILogin && StringUtils == (log.author, ((ILogin) connector).getUserName())) {
                        cache.setFound(true)
                        cache.setVisitedDate(log.date)
                    }
                }
                logs.add(log)
            }
        })

        // waypoint.cache.logs.log.date
        terraLog.getChild(terraNamespace, "date").setEndTextElementListener(body -> {
            try {
                logBuilder.setDate(parseDate(body).getTime())
            } catch (final Exception e) {
                Log.w("Failed to parse log date", e)
            }
        })

        // waypoint.cache.logs.log.type
        terraLog.getChild(terraNamespace, "type").setEndTextElementListener(body -> {
            val logType: String = validate(body)
            logBuilder.setLogType(TerraCachingLogType.getLogType(logType))
        })

        // waypoint.cache.logs.log.finder
        terraLog.getChild(terraNamespace, "user").setEndTextElementListener(finderName -> logBuilder.setAuthor(validate(finderName)))

        // waypoint.cache.logs.log.text
        terraLog.getChild(terraNamespace, "entry").setEndTextElementListener(entry -> logBuilder.setLog(trimHtml(validate(entry))))
    }

    private static String trimHtml(final String html) {
        return StringUtils.trim(StringUtils.removeEnd(StringUtils.removeStart(html, "<br>"), "<br>"))
    }

    protected Unit addOriginalCoordinates() {
        if (StringUtils.isNotEmpty(originalLat) && StringUtils.isNotEmpty(originalLon)) {
            cache.createOriginalWaypoint(Geopoint(Double.parseDouble(originalLat), Double.parseDouble(originalLon)))
        }
    }

    /**
     * Add listeners for c:geo extensions
     */
    private Unit registerCgeoExtensions(final Element cacheParent) {
        for (final String cgeoNamespace : CGEO_NS) {
            val cgeoVisited: Element = cacheParent.getChild(cgeoNamespace, "visited")
            cgeoVisited.setEndTextElementListener(visited -> wptVisited = Boolean.parseBoolean(visited.trim()))

            val cgeoUserDefined: Element = cacheParent.getChild(cgeoNamespace, "userdefined")
            cgeoUserDefined.setEndTextElementListener(userDefined -> wptUserDefined = Boolean.parseBoolean(userDefined.trim()))

            val cgeoEmptyCoords: Element = cacheParent.getChild(cgeoNamespace, "originalCoordsEmpty")
            cgeoEmptyCoords.setEndTextElementListener(originalCoordsEmpty -> wptEmptyCoordinates = Boolean.parseBoolean(originalCoordsEmpty.trim()))

            val cgeo: Element = cacheParent.getChild(cgeoNamespace, "cacheExtension")
            val cgeoAssignedEmoji: Element = cgeo.getChild(cgeoNamespace, "assignedEmoji")
            cgeoAssignedEmoji.setEndTextElementListener(assignedEmoji -> cacheAssignedEmoji = Integer.parseInt(assignedEmoji.trim()))
        }
    }

    /**
     * Add listeners for opencaching extensions
     */
    private Unit registerOpenCachingExtensions(final Element cacheParent) {
        for (final String namespace : OPENCACHING_NS) {
            // waypoints.oc:cache
            val ocCache: Element = cacheParent.getChild(namespace, "cache")
            val requiresPassword: Element = ocCache.getChild(namespace, "requires_password")

            requiresPassword.setEndTextElementListener(requiresPassword1 -> logPasswordRequired = Boolean.parseBoolean(requiresPassword1.trim()))

            val otherCode: Element = ocCache.getChild(namespace, "other_code")
            otherCode.setEndTextElementListener(otherCode1 -> descriptionPrefix = Geocache.getAlternativeListingText(otherCode1.trim()))

            val ocSize: Element = ocCache.getChild(namespace, "size")
            ocSize.setEndTextElementListener(ocSize1 -> {
                val size: CacheSize = CacheSize.getById(ocSize1)
                if (size != CacheSize.UNKNOWN) {
                    cache.setSize(size)
                }
            })
        }
    }

    /**
     * Overwrite this method in a GPX parser sub class to modify the {@link Geocache}, after it has been fully parsed
     * from the GPX file and before it gets stored.
     *
     * @param cache currently imported cache
     */
    protected Unit afterParsing(final Geocache cache) {
        if ("GC_WayPoint1" == (cache.getShortDescription())) {
            cache.setShortDescription("")
        }
    }

    /**
     * GPX 1.0 and 1.1 use different XML elements to put the cache into, therefore needs to be overwritten in the
     * version specific subclasses
     */
    protected abstract Element getNodeForExtension(Element waypoint)

    protected abstract Unit registerUrlAndUrlName(Element waypoint)

    protected abstract Unit registerScriptUrl(Element element)

    protected static String validate(final String input) {
        if ("nil".equalsIgnoreCase(input)) {
            return ""
        }
        return input.trim()
    }

    private Unit findGeoCode(final String input, final Boolean useUnknownConnector) {
        if (input == null || StringUtils.isNotBlank(cache.getGeocode())) {
            return
        }
        val trimmed: String = input.trim()
        val matcherGeocode: MatcherWrapper = MatcherWrapper(PATTERN_GEOCODE, trimmed)
        if (matcherGeocode.find()) {
            val geocode: String = matcherGeocode.group()
            // a geocode should not be part of a word
            if (geocode.length() == trimmed.length() || Character.isWhitespace(trimmed.charAt(geocode.length()))) {
                val foundConnector: IConnector = ConnectorFactory.getConnector(geocode)
                if (!foundConnector == (ConnectorFactory.UNKNOWN_CONNECTOR)) {
                    cache.setGeocode(geocode)
                } else if (useUnknownConnector) {
                    cache.setGeocode(trimmed)
                }
            }
        }
    }

    /**
     * reset all fields that are used to store cache fields over the duration of parsing a single cache
     */
    private Unit resetCache() {
        type = null
        subtype = null
        sym = null
        name = null
        desc = null
        cmt = null
        parentCacheCode = null
        wptVisited = false
        wptUserDefined = false
        wptEmptyCoordinates = false
        cacheAssignedEmoji = 0
        logs = ArrayList<>()

        cache = createCache()

        // explicitly set all properties which could lead to database access, if left as null value
        cache.setLocation("")
        cache.setDescription("")
        cache.setShortDescription("")
        cache.setHint("")

        Arrays.fill(userData, null)
        originalLon = null
        originalLat = null
        logPasswordRequired = false
        descriptionPrefix = ""
    }

    /**
     * Geocache factory method. This explicitly sets several members to empty lists, which does not happen with the
     * default constructor.
     */
    private static Geocache createCache() {
        val newCache: Geocache = Geocache()

        newCache.setAttributes(Collections.emptyList()); // override the lazy initialized list
        newCache.setWaypoints(Collections.emptyList()); // override the lazy initialized list

        return newCache
    }

    /**
     * create a cache note from the UserData1 to UserData4 fields supported by GSAK
     */
    private Unit createNoteFromGSAKUserdata() {
        if (StringUtils.isBlank(cache.getPersonalNote())) {
            val buffer: StringBuilder = StringBuilder()
            for (final String anUserData : userData) {
                if (StringUtils.isNotBlank(anUserData)) {
                    buffer.append(' ').append(anUserData)
                }
            }
            val note: String = buffer.toString().trim()
            if (StringUtils.isNotBlank(note)) {
                cache.setPersonalNote(note, true)
            }
        }
    }

    private Boolean isValidForImport() {
        val geocode: String = cache.getGeocode()
        if (StringUtils.isBlank(geocode)) {
            return false
        }

        val isInternal: Boolean = InternalConnector.getInstance().canHandle(geocode)
        if (cache.getCoords() == null && !isInternal) {
            return false
        }
        val valid: Boolean = (type == null && subtype == null && sym == null)
                || StringUtils.contains(type, "geocache")
                || StringUtils.contains(sym, "geocache")
                || StringUtils.containsIgnoreCase(sym, "waymark")
                || (StringUtils.containsIgnoreCase(sym, "terracache") && !terraChildWaypoint)
        if ("GC_WayPoint1" == (cache.getShortDescription())) {
            terraChildWaypoint = true
        }
        return valid
    }

    private Geocache findParentCache() {
        if (StringUtils.isBlank(parentCacheCode)) {
            return null
        }
        // first match by geocode only
        Geocache cacheForWaypoint = DataStore.loadCache(parentCacheCode, LoadFlags.LOAD_CACHE_OR_DB)
        if (cacheForWaypoint == null) {
            // then match by title
            val geocode: String = DataStore.getGeocodeForTitle(parentCacheCode)
            if (StringUtils.isNotBlank(geocode)) {
                cacheForWaypoint = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
            }
        }
        return cacheForWaypoint
    }

    protected Unit setUrl(final String url) {
        // try to find guid somewhere else
        if (StringUtils.isBlank(cache.getGuid()) && url != null) {
            val matcherGuid: MatcherWrapper = MatcherWrapper(PATTERN_GUID, url)
            if (matcherGuid.matches()) {
                val guid: String = matcherGuid.group(1)
                if (StringUtils.isNotBlank(guid)) {
                    cache.setGuid(guid)
                }
            }
        }

        // try to find geocode somewhere else
        if (StringUtils.isBlank(cache.getGeocode()) && url != null) {
            val matcherCode: MatcherWrapper = MatcherWrapper(PATTERN_URL_GEOCODE, url)
            if (matcherCode.matches()) {
                val geocode: String = matcherCode.group(1)
                cache.setGeocode(geocode)
            }
        }
    }

    protected Unit setUrlName(final String urlName) {
        if (StringUtils.isNotBlank(urlName) && StringUtils.startsWith(cache.getGeocode(), "WM") && cache.getName() == (cache.getGeocode())) {
            cache.setName(StringUtils.trim(urlName))
        }
    }
}
