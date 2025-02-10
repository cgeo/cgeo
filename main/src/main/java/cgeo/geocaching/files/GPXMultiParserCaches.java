package cgeo.geocaching.files;

import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.WaypointUserNoteCombiner;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
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
    private Geocache cache;
    private String name = null;
    private String type = null;
    private String subtype = null;
    private String sym = null;
    private String cmt = null;
    private String desc = null;
    private String scriptUrl; // URL contained in the header of the GPX file. Used to guess where the file is coming from.

    protected final String[] userData = new String[5]; // take 5 cells, that makes indexing 1..4 easier
    private String parentCacheCode = null;
    private boolean wptVisited = false;
    private boolean wptUserDefined = false;
    private boolean wptEmptyCoordinates = false;
    private int cacheAssignedEmoji = 0;
    private List<LogEntry> logs = new ArrayList<>();

    private boolean terraChildWaypoint = false;
    private boolean logPasswordRequired = false;
    private String descriptionPrefix = "";
    // ---------------------------------------------------------------------------------------------


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

    private void findGeoCode(final String input, final Boolean useUnknownConnector) {

    }

    protected static String validate(final String input) {
        return "";
    }

    protected void setUrl(final String url) {
    }

}
