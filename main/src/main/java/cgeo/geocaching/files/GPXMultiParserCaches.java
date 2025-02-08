package cgeo.geocaching.files;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.connector.tc.TerraCachingLogType;
import cgeo.geocaching.connector.tc.TerraCachingType;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.WaypointUserNoteCombiner;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.html.HtmlUtils;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class GPXMultiParserCaches extends GPXMultiParserAbstractFiles /*implements GPXMultiParserBase */ {

    // ---------------------------------------------------------------------------------------------
    // dummy declarations for already migrated ones
    // ---------------------------------------------------------------------------------------------
    private static final SynchronizedDateFormat formatSimple = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US); // 2010-04-20T07:00:00
    private static final SynchronizedDateFormat formatSimpleNoTime = new SynchronizedDateFormat("yyyy-MM-dd", Locale.US); // 2010-04-20
    private static final SynchronizedDateFormat formatSimpleZ = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); // 2010-04-20T07:00:00Z
    private static final SynchronizedDateFormat formatTimezone = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US); // 2010-04-20T01:01:03-04:00
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

    @Nullable protected String originalLon;
    @Nullable protected String originalLat;
    private boolean terraChildWaypoint = false;
    private boolean logPasswordRequired = false;
    private String descriptionPrefix = "";
    // ---------------------------------------------------------------------------------------------


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
            "http://www.gsak.net/xmlv1/1",
            "http://www.gsak.net/xmlv1/2",
            "http://www.gsak.net/xmlv1/3",
            "http://www.gsak.net/xmlv1/4",
            "http://www.gsak.net/xmlv1/5",
            "http://www.gsak.net/xmlv1/6"
    };

    /**
     * c:geo extensions of the gpx format
     */
    private static final String[] CGEO_NS = {
            "http://www.cgeo.org/wptext/1/0"
    };

    /**
     * opencaching extensions of the gpx format
     */
    private static final String[] OPENCACHING_NS = {
            "https://github.com/opencaching/gpx-extension-v1"
    };

    private int listId = StoredList.STANDARD_LIST_ID;
    protected final String namespace;
    protected final boolean version11;


    /**
     * Parser result. Maps geocode to cache.
     */
    private final Set<String> result = new HashSet<>(100);
    private ProgressInputStream progressStream;

    private final class UserDataListener implements EndTextElementListener {
        private final int index;

        UserDataListener(final int index) {
            this.index = index;
        }

        @Override
        public void end(final String user) {
            userData[index] = validate(user);
        }
    }

    GPXMultiParserCaches(@NonNull final Element root, @NonNull final String namespace, final boolean version11, final int listId, @Nullable final DisposableHandler progressHandler) {
        this.namespace = namespace;
        this.version11 = version11;
        this.listId = listId;

        final Element waypoint = root.getChild(namespace, "wpt");

        // waypoint - attributes
        // (done)

        // waypoint
        waypoint.setEndElementListener(new EndElementListener() {

            @Override
            public void end() {
                // try to find geocode somewhere else
                if (StringUtils.isBlank(cache.getGeocode())) {
                    findGeoCode(name, true);
                    findGeoCode(desc, false);
                    findGeoCode(cmt, false);
                }

                // take the name as code, if nothing else is available
                if (StringUtils.isBlank(cache.getGeocode()) && StringUtils.isNotBlank(name)) {
                    cache.setGeocode(name.trim());
                }

                if (isValidForImport()) {
                    fixCache(cache);
                    if (listId != StoredList.TEMPORARY_LIST.id) {
                        cache.getLists().add(listId);
                    }
                    cache.setDetailed(true);
                    cache.setLogPasswordRequired(logPasswordRequired);
                    if (StringUtils.isNotBlank(descriptionPrefix)) {
                        cache.setDescription(descriptionPrefix + cache.getDescription());
                    }

                    createNoteFromGSAKUserdata();

                    cache.setAssignedEmoji(cacheAssignedEmoji);

                    final String geocode = cache.getGeocode();
                    if (result.contains(geocode)) {
                        Log.w("Duplicate geocode during GPX import: " + geocode);
                    }
                    // modify cache depending on the use case/connector
                    afterParsing(cache);

                    // finally store the cache in the database
                    result.add(geocode);
                    DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB));
                    DataStore.saveLogs(cache.getGeocode(), logs, false);

                    // avoid the cachecache using lots of memory for caches which the user did not actually look at
                    DataStore.removeCache(geocode, EnumSet.of(LoadFlags.RemoveFlag.CACHE));
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
                        } else if (terraChildWaypoint) {
                            parentCacheCode = StringUtils.left(cache.getGeocode(), cache.getGeocode().length() - 1);
                        } else {
                            parentCacheCode = "GC" + cache.getName().substring(2).toUpperCase(Locale.US);
                        }
                    }

                    if ("GC_WayPoint1".equals(cache.getShortDescription())) {
                        cache.setShortDescription("");
                    }

                    final Geocache cacheForWaypoint = findParentCache();
                    if (cacheForWaypoint != null) {
                        final Waypoint waypoint = new Waypoint(cache.getShortDescription(), WaypointType.fromGPXString(sym, subtype), false);
                        if (wptUserDefined) {
                            waypoint.setUserDefined();
                        }
                        waypoint.setId(-1);
                        waypoint.setGeocode(parentCacheCode);
                        String cacheName = cache.getName();
                        if (wptUserDefined) {
                            // try to deduct original prefix from wpt name
                            if (StringUtils.endsWithIgnoreCase(cacheName, parentCacheCode.substring(2))) {
                                cacheName = cacheName.substring(0, cacheName.length() - parentCacheCode.length() + 2);
                            }
                            if (StringUtils.startsWithIgnoreCase(cacheName, Waypoint.PREFIX_OWN + "-")) {
                                cacheName = cacheName.substring(4);
                            }
                        }
                        waypoint.setPrefix(cacheForWaypoint.getWaypointPrefix(cacheName));
                        waypoint.setLookup("---");
                        // there is no lookup code in gpx file

                        waypoint.setCoords(cache.getCoords());

                        // set flag for user-modified coordinates of cache
                        if (waypoint.getWaypointType() == WaypointType.ORIGINAL) {
                            cacheForWaypoint.setUserModifiedCoords(true);
                        }

                        // user defined waypoint does not have original empty coordinates
                        if (wptEmptyCoordinates || (!waypoint.isUserDefined() && null == waypoint.getCoords())) {
                            waypoint.setOriginalCoordsEmpty(true);
                        }

                        final WaypointUserNoteCombiner wpCombiner = new WaypointUserNoteCombiner(waypoint);
                        wpCombiner.updateNoteAndUserNote(cache.getDescription());

                        waypoint.setVisited(wptVisited);
                        final List<Waypoint> mergedWayPoints = new ArrayList<>(cacheForWaypoint.getWaypoints());

                        final List<Waypoint> newPoints = new ArrayList<>();
                        newPoints.add(waypoint);
                        Waypoint.mergeWayPoints(newPoints, mergedWayPoints, true);
                        cacheForWaypoint.setWaypoints(newPoints, false);
                        DataStore.saveCache(cacheForWaypoint, EnumSet.of(LoadFlags.SaveFlag.DB));
                        showProgressMessage(progressHandler, progressStream.getProgress());
                    }
                }
            }

        });

        // waypoint.name
        // (done)

        // for GPX 1.0, cache info comes from waypoint node (so called private children)
        // for GPX 1.1 from extensions node
        final Element extensionNode = getNodeForExtension(waypoint);
        if (extensionNode != null) {
            registerExtensions(extensionNode);
        } else {
            //  only to support other formats for GPX1.1, standard is extension
            registerExtensions(waypoint);
        }

    }

    private void registerExtensions(@NonNull final Element cacheParent) {
        registerGsakExtensions(cacheParent);
        registerTerraCachingExtensions(cacheParent);
        registerCgeoExtensions(cacheParent);
        registerOpenCachingExtensions(cacheParent);
        registerGroundspeakExtensions(cacheParent);
    }

    /**
     * Add listeners for groundspeak extensions
     */
    private void registerGroundspeakExtensions(final Element cacheParent) {
        // 3 different versions of the GC schema
        for (final String nsGC : GROUNDSPEAK_NAMESPACE) {
            // waypoints.cache
            final Element gcCache = cacheParent.getChild(nsGC, "cache");

            registerGsakExtensionsCache(nsGC, gcCache);
            registerGsakExtensionsAttribute(nsGC, gcCache);
            registerGsakExtensionsTb(nsGC, gcCache);
            registerGsakExtensionsLog(nsGC, gcCache);
        }
    }

    /**
     * Add listeners for Groundspeak cache
     */
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private void registerGsakExtensionsCache(final String nsGC, final Element gcCache) {
        gcCache.setStartElementListener(attrs -> {
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
        });

        // waypoint.cache.getName()
        gcCache.getChild(nsGC, "name").setEndTextElementListener(cacheName -> cache.setName(validate(cacheName)));

        // waypoint.cache.getOwner()
        gcCache.getChild(nsGC, "owner").setEndTextElementListener(ownerUserId -> cache.setOwnerUserId(validate(ownerUserId)));

        // waypoint.cache.getOwner()
        gcCache.getChild(nsGC, "placed_by").setEndTextElementListener(ownerDisplayName -> cache.setOwnerDisplayName(validate(ownerDisplayName)));

        // waypoint.cache.getType()
        gcCache.getChild(nsGC, "type").setEndTextElementListener(bodyIn -> {
            String body = validate(bodyIn);
            // lab caches wrongly contain a prefix in the type
            if (body.startsWith("Geocache|")) {
                body = StringUtils.substringAfter(body, "Geocache|").trim();
            }
            cache.setType(CacheType.getByPattern(body));
        });

        // waypoint.cache.container
        gcCache.getChild(nsGC, "container").setEndTextElementListener(body -> cache.setSize(CacheSize.getById(validate(body))));

        // waypoint.cache.getDifficulty()
        gcCache.getChild(nsGC, "difficulty").setEndTextElementListener(body -> {
            try {
                cache.setDifficulty(Float.parseFloat(body));
            } catch (final NumberFormatException e) {
                Log.w("Failed to parse difficulty", e);
            }
        });

        // waypoint.cache.getTerrain()
        gcCache.getChild(nsGC, "terrain").setEndTextElementListener(body -> {
            try {
                cache.setTerrain(Float.parseFloat(body));
            } catch (final NumberFormatException e) {
                Log.w("Failed to parse terrain", e);
            }
        });

        // waypoint.cache.country
        gcCache.getChild(nsGC, "country").setEndTextElementListener(country -> {
            if (StringUtils.isBlank(cache.getLocation())) {
                cache.setLocation(validate(country));
            } else {
                cache.setLocation(cache.getLocation() + ", " + country.trim());
            }
        });

        // waypoint.cache.state
        gcCache.getChild(nsGC, "state").setEndTextElementListener(state -> {
            final String trimmedState = state.trim();
            if (StringUtils.isNotEmpty(trimmedState)) { // state can be completely empty
                if (StringUtils.isBlank(cache.getLocation())) {
                    cache.setLocation(validate(state));
                } else {
                    cache.setLocation(trimmedState + ", " + cache.getLocation());
                }
            }
        });

        // waypoint.cache.encoded_hints
        gcCache.getChild(nsGC, "encoded_hints").setEndTextElementListener(encoded -> cache.setHint(validate(encoded)));

        gcCache.getChild(nsGC, "short_description").setEndTextElementListener(shortDesc -> cache.setShortDescription(validate(shortDesc)));

        gcCache.getChild(nsGC, "long_description").setEndTextElementListener(desc -> cache.setDescription(validate(desc)));
    }

    /**
     * Add listeners for Groundspeak attributes
     */
    private void registerGsakExtensionsAttribute(final String nsGC, final Element gcCache) {
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

        gcAttribute.setStartElementListener(attrs -> {
            try {
                if (attrs.getIndex("id") > -1 && attrs.getIndex("inc") > -1) {
                    final int attributeId = Integer.parseInt(attrs.getValue("id"));
                    final boolean attributeActive = Integer.parseInt(attrs.getValue("inc")) != 0;
                    final CacheAttribute attribute = CacheAttribute.getById(attributeId);
                    if (attribute != null) {
                        cache.getAttributes().add(attribute.getValue(attributeActive));
                    }
                }
            } catch (final NumberFormatException ignored) {
                // nothing
            }
        });
    }

    /**
     * Add listeners for Groundspeak TBs
     */
    private void registerGsakExtensionsTb(final String nsGC, final Element gcCache) {
        // waypoint.cache.travelbugs
        final Element gcTBs = gcCache.getChild(nsGC, "travelbugs");

        // waypoint.cache.travelbug
        final Element gcTB = gcTBs.getChild(nsGC, "travelbug");

        // waypoint.cache.travelbugs.travelbug
        gcTB.setStartElementListener(attrs -> {
            trackable = new Trackable();

            try {
                if (attrs.getIndex("ref") > -1) {
                    trackable.setGeocode(attrs.getValue("ref"));
                }
            } catch (final RuntimeException ignored) {
                // nothing
            }
        });

        gcTB.setEndElementListener(() -> {
            if (StringUtils.isNotBlank(trackable.getGeocode()) && StringUtils.isNotBlank(trackable.getName())) {
                cache.addInventoryItem(trackable);
            }
        });

        // waypoint.cache.travelbugs.travelbug.getName()
        gcTB.getChild(nsGC, "name").setEndTextElementListener(tbName -> trackable.setName(validate(tbName)));
    }

    /**
     * Add listeners for Groundspeak logs
     */
    private void registerGsakExtensionsLog(final String nsGC, final Element gcCache) {
        // waypoint.cache.logs
        final Element gcLogs = gcCache.getChild(nsGC, "logs");

        // waypoint.cache.log
        final Element gcLog = gcLogs.getChild(nsGC, "log");

        gcLog.setStartElementListener(attrs -> {
            logBuilder = new LogEntry.Builder();

            try {
                if (attrs.getIndex("id") > -1) {
                    logBuilder.setId(Integer.parseInt(attrs.getValue("id")));

                    final IConnector connector = ConnectorFactory.getConnector(cache);
                    if (connector instanceof GCConnector) {
                        logBuilder.setServiceLogId(GCUtils.logIdToLogCode(logBuilder.getId()));
                    }
                }
            } catch (final Exception ignored) {
                // nothing
            }
        });

        gcLog.setEndElementListener(() -> {
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
        });

        // waypoint.cache.logs.log.date
        gcLog.getChild(nsGC, "date").setEndTextElementListener(body -> {
            try {
                logBuilder.setDate(parseDate(body).getTime());
            } catch (final Exception e) {
                Log.w("Failed to parse log date", e);
            }
        });

        // waypoint.cache.logs.log.getType()
        gcLog.getChild(nsGC, "type").setEndTextElementListener(body -> {
            final String logType = validate(body);
            logBuilder.setLogType(LogType.getByType(logType));
        });

        // waypoint.cache.logs.log.finder
        gcLog.getChild(nsGC, "finder").setEndTextElementListener(finderName -> logBuilder.setAuthor(validate(finderName)));

        // waypoint.cache.logs.log.text
        gcLog.getChild(nsGC, "text").setEndTextElementListener(logText -> logBuilder.setLog(validate(logText)));
    }

    /**
     * Add listeners for GSAK extensions
     */
    private void registerGsakExtensions(final Element cacheParent) {
        for (final String gsakNamespace : GSAK_NS) {
            final Element gsak = cacheParent.getChild(gsakNamespace, "wptExtension");
            gsak.getChild(gsakNamespace, "Watch").setEndTextElementListener(watchList -> cache.setOnWatchlist(Boolean.parseBoolean(watchList.trim())));

            gsak.getChild(gsakNamespace, "UserData").setEndTextElementListener(new UserDataListener(1));

            for (int i = 2; i <= 4; i++) {
                gsak.getChild(gsakNamespace, "User" + i).setEndTextElementListener(new UserDataListener(i));
            }

            gsak.getChild(gsakNamespace, "Parent").setEndTextElementListener(body -> parentCacheCode = body);

            gsak.getChild(gsakNamespace, "FavPoints").setEndTextElementListener(favoritePoints -> {
                try {
                    cache.setFavoritePoints(Integer.parseInt(favoritePoints));
                } catch (final NumberFormatException e) {
                    Log.w("Failed to parse favorite points", e);
                }
            });

            gsak.getChild(gsakNamespace, "GcNote").setEndTextElementListener(personalNote -> cache.setPersonalNote(StringUtils.trim(personalNote), true));

            gsak.getChild(gsakNamespace, "IsPremium").setEndTextElementListener(premium -> cache.setPremiumMembersOnly(Boolean.parseBoolean(premium)));

            gsak.getChild(gsakNamespace, "LatBeforeCorrect").setEndTextElementListener(latitude -> {
                originalLat = latitude;
                addOriginalCoordinates();
            });

            gsak.getChild(gsakNamespace, "LonBeforeCorrect").setEndTextElementListener(longitude -> {
                originalLon = longitude;
                addOriginalCoordinates();
            });

            gsak.getChild(gsakNamespace, "Code").setEndTextElementListener(geocode -> {
                if (StringUtils.isNotBlank(geocode)) {
                    cache.setGeocode(StringUtils.trim(geocode));
                }
            });

            gsak.getChild(gsakNamespace, "DNF").setEndTextElementListener(dnfState -> {
                if (!cache.isFound()) {
                    cache.setDNF(Boolean.parseBoolean(dnfState));
                }
            });
            gsak.getChild(gsakNamespace, "DNFDate").setEndTextElementListener(dnfDate -> {
                if (0 == cache.getVisitedDate()) {
                    try {
                        cache.setVisitedDate(parseDate(dnfDate).getTime());
                    } catch (final Exception e) {
                        Log.w("Failed to parse visited date 'gsak:DNFDate'", e);
                    }
                }
            });

            gsak.getChild(gsakNamespace, "UserFound").setEndTextElementListener(foundDate -> {
                if (0 == cache.getVisitedDate()) {
                    try {
                        cache.setVisitedDate(parseDate(foundDate).getTime());
                    } catch (final Exception e) {
                        Log.w("Failed to parse visited date 'gsak:UserFound'", e);
                    }
                }
            });

            gsak.getChild(gsakNamespace, "Child_ByGSAK").setEndTextElementListener(userDefined -> wptUserDefined |= Boolean.parseBoolean(userDefined.trim()));
        }
    }

    /**
     * Add listeners for TerraCaching extensions
     */
    private void registerTerraCachingExtensions(final Element cacheParent) {
        final String terraNamespace = "http://www.TerraCaching.com/GPX/1/0";
        final Element terraCache = cacheParent.getChild(terraNamespace, "terracache");

        terraCache.getChild(terraNamespace, "name").setEndTextElementListener(name -> cache.setName(StringUtils.trim(name)));

        terraCache.getChild(terraNamespace, "owner").setEndTextElementListener(ownerName -> cache.setOwnerDisplayName(validate(ownerName)));

        terraCache.getChild(terraNamespace, "style").setEndTextElementListener(style -> cache.setType(TerraCachingType.getCacheType(style)));

        terraCache.getChild(terraNamespace, "size").setEndTextElementListener(size -> cache.setSize(CacheSize.getById(size)));

        terraCache.getChild(terraNamespace, "country").setEndTextElementListener(country -> {
            if (StringUtils.isNotBlank(country)) {
                cache.setLocation(StringUtils.trim(country));
            }
        });

        terraCache.getChild(terraNamespace, "state").setEndTextElementListener(state -> {
            final String trimmedState = state.trim();
            if (StringUtils.isNotEmpty(trimmedState)) {
                if (StringUtils.isBlank(cache.getLocation())) {
                    cache.setLocation(validate(state));
                } else {
                    cache.setLocation(trimmedState + ", " + cache.getLocation());
                }
            }
        });

        terraCache.getChild(terraNamespace, "description").setEndTextElementListener(description -> cache.setDescription(trimHtml(description)));

        terraCache.getChild(terraNamespace, "hint").setEndTextElementListener(hint -> cache.setHint(HtmlUtils.extractText(hint)));

        final Element terraLogs = terraCache.getChild(terraNamespace, "logs");
        final Element terraLog = terraLogs.getChild(terraNamespace, "log");

        terraLog.setStartElementListener(attrs -> {
            logBuilder = new LogEntry.Builder();

            try {
                if (attrs.getIndex("id") > -1) {
                    logBuilder.setId(Integer.parseInt(attrs.getValue("id")));
                }
            } catch (final NumberFormatException ignored) {
                // nothing
            }
        });

        terraLog.setEndElementListener(() -> {
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
        });

        // waypoint.cache.logs.log.date
        terraLog.getChild(terraNamespace, "date").setEndTextElementListener(body -> {
            try {
                logBuilder.setDate(parseDate(body).getTime());
            } catch (final Exception e) {
                Log.w("Failed to parse log date", e);
            }
        });

        // waypoint.cache.logs.log.type
        terraLog.getChild(terraNamespace, "type").setEndTextElementListener(body -> {
            final String logType = validate(body);
            logBuilder.setLogType(TerraCachingLogType.getLogType(logType));
        });

        // waypoint.cache.logs.log.finder
        terraLog.getChild(terraNamespace, "user").setEndTextElementListener(finderName -> logBuilder.setAuthor(validate(finderName)));

        // waypoint.cache.logs.log.text
        terraLog.getChild(terraNamespace, "entry").setEndTextElementListener(entry -> logBuilder.setLog(trimHtml(validate(entry))));
    }

    private static String trimHtml(final String html) {
        return StringUtils.trim(StringUtils.removeEnd(StringUtils.removeStart(html, "<br>"), "<br>"));
    }

    protected void addOriginalCoordinates() {
        if (StringUtils.isNotEmpty(originalLat) && StringUtils.isNotEmpty(originalLon)) {
            cache.createOriginalWaypoint(new Geopoint(Double.parseDouble(originalLat), Double.parseDouble(originalLon)));
        }
    }

    /**
     * Add listeners for c:geo extensions
     */
    private void registerCgeoExtensions(final Element cacheParent) {
        for (final String cgeoNamespace : CGEO_NS) {
            final Element cgeoVisited = cacheParent.getChild(cgeoNamespace, "visited");
            cgeoVisited.setEndTextElementListener(visited -> wptVisited = Boolean.parseBoolean(visited.trim()));

            final Element cgeoUserDefined = cacheParent.getChild(cgeoNamespace, "userdefined");
            cgeoUserDefined.setEndTextElementListener(userDefined -> wptUserDefined = Boolean.parseBoolean(userDefined.trim()));

            final Element cgeoEmptyCoords = cacheParent.getChild(cgeoNamespace, "originalCoordsEmpty");
            cgeoEmptyCoords.setEndTextElementListener(originalCoordsEmpty -> wptEmptyCoordinates = Boolean.parseBoolean(originalCoordsEmpty.trim()));

            final Element cgeo = cacheParent.getChild(cgeoNamespace, "cacheExtension");
            final Element cgeoAssignedEmoji = cgeo.getChild(cgeoNamespace, "assignedEmoji");
            cgeoAssignedEmoji.setEndTextElementListener(assignedEmoji -> cacheAssignedEmoji = Integer.parseInt(assignedEmoji.trim()));
        }
    }

    /**
     * Add listeners for opencaching extensions
     */
    private void registerOpenCachingExtensions(final Element cacheParent) {
        for (final String namespace : OPENCACHING_NS) {
            // waypoints.oc:cache
            final Element ocCache = cacheParent.getChild(namespace, "cache");
            final Element requiresPassword = ocCache.getChild(namespace, "requires_password");

            requiresPassword.setEndTextElementListener(requiresPassword1 -> logPasswordRequired = Boolean.parseBoolean(requiresPassword1.trim()));

            final Element otherCode = ocCache.getChild(namespace, "other_code");
            otherCode.setEndTextElementListener(otherCode1 -> descriptionPrefix = Geocache.getAlternativeListingText(otherCode1.trim()));

            final Element ocSize = ocCache.getChild(namespace, "size");
            ocSize.setEndTextElementListener(ocSize1 -> {
                final CacheSize size = CacheSize.getById(ocSize1);
                if (size != CacheSize.UNKNOWN) {
                    cache.setSize(size);
                }
            });
        }
    }

    /**
     * Overwrite this method in a GPX parser sub class to modify the {@link Geocache}, after it has been fully parsed
     * from the GPX file and before it gets stored.
     *
     * @param cache currently imported cache
     */
    protected void afterParsing(final Geocache cache) {
        if ("GC_WayPoint1".equals(cache.getShortDescription())) {
            cache.setShortDescription("");
        }
    }

    /**
     * GPX 1.0 and 1.1 use different XML elements to put the cache into
     */
    @Nullable
    protected Element getNodeForExtension(@NonNull final Element waypoint) {
        return version11 ? waypoint.getChild(namespace, "extensions") : waypoint;
    };

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
                cache.setPersonalNote(note, true);
            }
        }
    }

    private boolean isValidForImport() {
        final String geocode = cache.getGeocode();
        if (StringUtils.isBlank(geocode)) {
            return false;
        }

        final boolean isInternal = InternalConnector.getInstance().canHandle(geocode);
        if (cache.getCoords() == null && !isInternal) {
            return false;
        }
        final boolean valid = (type == null && subtype == null && sym == null)
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

    //@Override
    public void onParsingDone(@NonNull final Collection<Object> result) {
        result.addAll(DataStore.loadCaches(this.result, EnumSet.of(LoadFlags.LoadFlag.DB_MINIMAL)));
    }

    // ---------------------------------------------------------------------------------------------
    // placeholders for already migrated methods
    // ---------------------------------------------------------------------------------------------

    private void resetCache() {
    }

    static Date parseDate(final String inputUntrimmed) throws ParseException {
        return new Date();
    }

    private void findGeoCode(final String input, final Boolean useUnknownConnector) {

    }

    protected static String validate(final String input) {
        return "";
    }

    protected void setUrl(final String url) {
    }

    protected void setUrlName(final String urlName) {
    }
}
