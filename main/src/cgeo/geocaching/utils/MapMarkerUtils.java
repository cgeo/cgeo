package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.ListMarker;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.builders.InsetBuilder;
import cgeo.geocaching.utils.builders.InsetBuilder.HORIZONTAL;
import cgeo.geocaching.utils.builders.InsetBuilder.VERTICAL;
import cgeo.geocaching.utils.builders.InsetsBuilder;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class MapMarkerUtils {

    private static final Map<Integer, Integer> list2marker = new TreeMap<>();
    private static Boolean listsRead = false;

    private static final SparseArray<CacheMarker> overlaysCache = new SparseArray<>();

    private MapMarkerUtils() {
        // Do not instantiate
    }

    /**
     * Obtain the drawable for a given cache, with background circle.
     *
     * @param res
     *          the resources to use
     * @param cache
     *          the cache to build the drawable for
     * @return
     *          a drawable representing the current cache status
     */
    @NonNull
    public static CacheMarker getCacheMarker(final Resources res, final Geocache cache) {
        return getCacheMarker(res, cache, null);
    }

    /**
     * Obtain the drawable for a given cache.
     * Return a drawable from the cache, if a similar drawable was already generated.
     *
     * cacheListType should be Null if the requesting activity is Map.
     *
     * @param res
     *          the resources to use
     * @param cache
     *          the cache to build the drawable for
     * @param cacheListType
     *          the current CacheListType or Null
     * @return
     *          a drawable representing the current cache status
     */
    @NonNull
    public static CacheMarker getCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType) {
        final int assignedMarkers = getAssignedMarkers(cache);
        final int hashcode = new HashCodeBuilder()
                .append(cache.isReliableLatLon())
                .append(cache.getType().id)
                .append(cache.isDisabled() || cache.isArchived())
                .append(cache.getMapMarkerId())
                .append(cache.isOwner())
                .append(cache.isFound())
                .append(showUserModifiedCoords(cache))
                .append(cache.getPersonalNote())
                .append(cache.isLogOffline())
                .append(!cache.getLists().isEmpty())
                .append(cache.getOfflineLogType())
                .append(showBackground(cacheListType))
                .append(showFloppyOverlay(cacheListType))
                .append(assignedMarkers)
                .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createCacheMarker(res, cache, cacheListType, assignedMarkers));
                overlaysCache.put(hashcode, marker);
            }
            return marker;
        }
    }

    /**
     * Obtain the drawable for a given waypoint.
     * Return a drawable from the cache, if a similar drawable was already generated.
     *
     * @param res
     *          the resources to use
     * @param waypoint
     *          the waypoint to build the drawable for
     * @return
     *          a drawable representing the current waypoint status
     */
    @NonNull
    public static CacheMarker getWaypointMarker(final Resources res, final Waypoint waypoint) {
        final WaypointType waypointType = waypoint.getWaypointType();
        final String id = null == waypointType ? WaypointType.WAYPOINT.id : waypointType.id;
        int assignedMarkers = 0;
        final String geocode = waypoint.getGeocode();
        if (StringUtils.isNotBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (null != cache) {
                assignedMarkers = getAssignedMarkers(cache);
            }
        }
        final int hashcode = new HashCodeBuilder()
            .append(waypoint.isVisited())
            .append(id)
            .append(assignedMarkers)
            .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createWaypointMarker(res, waypoint, assignedMarkers));
                overlaysCache.put(hashcode, marker);
            }
            return marker;
        }
    }

    /**
     * Build the drawable for a given waypoint.
     *
     * @param res
     *          the resources to use
     * @param waypoint
     *          the waypoint to build the drawable for
     * @return
     *          a drawable representing the current waypoint status
     */
    @NonNull
    private static LayerDrawable createWaypointMarker(final Resources res, final Waypoint waypoint, final int assignedMarkers) {
        final WaypointType waypointType = waypoint.getWaypointType();


        final Drawable marker = Compatibility.getDrawable(res, !waypoint.isVisited() ? R.drawable.marker : R.drawable.marker_transparent);
        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        insetsBuilder.withInset(new InsetBuilder(marker));

        int markerId = null == waypointType ? WaypointType.WAYPOINT.markerId : waypoint.getWaypointType().markerId;
        insetsBuilder.withInset(new InsetBuilder(markerId, VERTICAL.CENTER, HORIZONTAL.CENTER));
        // assigned lists with markers
        markerId = assignedMarkers & ListMarker.BITMASK;
        if (markerId > 0) {
            insetsBuilder.withInset(new InsetBuilder(ListMarker.getResDrawable(markerId), VERTICAL.CENTER, HORIZONTAL.LEFT));
        }

        markerId = (assignedMarkers >> ListMarker.MAX_BITS_PER_MARKER) & ListMarker.BITMASK;
        if (markerId > 0) {
            insetsBuilder.withInset(new InsetBuilder(ListMarker.getResDrawable(markerId), VERTICAL.CENTER, HORIZONTAL.RIGHT));
        }

        return buildLayerDrawable(insetsBuilder, 2, 2);
    }

    /**
     * Build the drawable for a given waypoint.
     *
     * @param res
     *          the resources to use
     * @param waypoint
     *          the waypoint to build the drawable for
     * @return
     *          a drawable representing the current waypoint status
     */
    @NonNull
    public static LayerDrawable createWaypointDotMarker(final Resources res, final Waypoint waypoint) {
        final Drawable[] layers = { Compatibility.getDrawable(res, waypoint.getWaypointType().dotMarkerId) };
        return new LayerDrawable(layers);
    }

    /**
     * Clear the cache of drawable items.
     */
    public static void clearCachedItems() {
        synchronized (overlaysCache) {
            overlaysCache.clear();
        }
    }

    /**
     * Build the drawable for a given cache.
     *
     * @param res           the resources to use
     * @param cache         the cache to build the drawable for
     * @param cacheListType the current CacheListType or Null
     * @return a drawable representing the current cache status
     */
    @NonNull
    private static LayerDrawable createCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType, final int assignedMarkers) {
        // background: disabled or not
        final Drawable marker = Compatibility.getDrawable(res, cache.getMapMarkerId());
        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());

        // Show the background circle only on map
        if (showBackground(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(marker));
        }
        // reliable or not
        if (!cache.isReliableLatLon() && showUnreliableLatLon(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_notreliable));
        }
        // cache type
        final int mainMarkerId = getMainMarkerId(cache, cacheListType);
        final boolean doubleSize = showBigSmileys(cacheListType) && mainMarkerId != cache.getType().markerId;
        insetsBuilder.withInset(new InsetBuilder(mainMarkerId, VERTICAL.CENTER, HORIZONTAL.CENTER, doubleSize));
        // own
        if (cache.isOwner()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_own, VERTICAL.TOP, HORIZONTAL.RIGHT));
            // if not, checked if stored
        } else if (!cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_stored, VERTICAL.TOP, HORIZONTAL.RIGHT));
        }
        // found
        if (!showBigSmileys(cacheListType)) {
            final Integer loggedMarkerId = getMarkerIdIfLogged(cache);
            if (loggedMarkerId != null) {
                insetsBuilder.withInset(new InsetBuilder(loggedMarkerId, VERTICAL.TOP, HORIZONTAL.LEFT));
            }
        }
        // user modified coords
        if (showUserModifiedCoords(cache)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_usermodifiedcoords, VERTICAL.BOTTOM, HORIZONTAL.RIGHT));
        }
        // personal note
        if (cache.getPersonalNote() != null) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_personalnote, VERTICAL.BOTTOM, HORIZONTAL.LEFT));
        }
        // assigned lists with markers
        int markerId = assignedMarkers & ListMarker.BITMASK;
        if (markerId > 0) {
            insetsBuilder.withInset(new InsetBuilder(ListMarker.getResDrawable(markerId), VERTICAL.CENTER, HORIZONTAL.LEFT));
        }

        markerId = (assignedMarkers >> ListMarker.MAX_BITS_PER_MARKER) & ListMarker.BITMASK;
        if (markerId > 0) {
            insetsBuilder.withInset(new InsetBuilder(ListMarker.getResDrawable(markerId), VERTICAL.CENTER, HORIZONTAL.RIGHT));
        }

        return buildLayerDrawable(insetsBuilder, 11, 10);
    }

    private static LayerDrawable buildLayerDrawable(final InsetsBuilder insetsBuilder, final int layersInitialCapacity, final int insetsInitialCapacity) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        final List<Drawable> layers = new ArrayList<>(layersInitialCapacity);
        final List<int[]> insets = new ArrayList<>(insetsInitialCapacity);

        insetsBuilder.build(layers, insets);
        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

        int index = 0;
        for (final int[] temp : insets) {
            ld.setLayerInset(index++, temp[0], temp[1], temp[2], temp[3]);
        }
        return ld;
    }

    private static int getMainMarkerId(final Geocache cache, final CacheListType cacheListType) {
        if (showBigSmileys(cacheListType)) {
            final Integer offlineLogType = getMarkerIdIfLogged(cache);
            if (offlineLogType != null) {
                return offlineLogType;
            }
        }

        return cache.getType().markerId;
    }

    @org.jetbrains.annotations.Nullable
    private static Integer getMarkerIdIfLogged(final Geocache cache) {
        if (cache.isFound()) {
            return R.drawable.marker_found;
            // if not, perhaps logged offline
        } else if (cache.hasLogOffline()) {
            final LogType offlineLogType = cache.getOfflineLogType();
            return offlineLogType == null ? R.drawable.marker_found_offline : offlineLogType.getOfflineLogOverlay();
        }

        return null;
    }

    private static boolean showBigSmileys(final CacheListType cacheListType) {
        return Settings.isBigSmileysEnabled() && showBackground(cacheListType);
    }

    /**
     * Build the drawable for a given cache.
     *
     * @param res
     *          the resources to use
     * @param cache
     *          the cache to build the drawable for
     * @return
     *          a drawable representing the current cache status
     */
    @NonNull
    public static LayerDrawable createCacheDotMarker(final Resources res, final Geocache cache) {
        final Drawable[] layers = { Compatibility.getDrawable(res, cache.isFound() ? R.drawable.dot_found : cache.getType().dotMarkerId) };
        return new LayerDrawable(layers);
    }

    /**
     * Conditional expression to choose if we need the background circle or not.
     *
     * @param cacheListType
     *            The cache list currently used
     * @return
     *         True if the background circle should be displayed
     */
    private static boolean showBackground(@Nullable final CacheListType cacheListType) {
        return cacheListType == null;
    }

    /**
     * Conditional expression to choose if we need the orange circle or not.
     * The orange circle indicate an approximative cache position.
     *
     * @param cacheListType
     *          The cache list currently used
     * @return
     *          True if the background circle should be displayed
     */
    private static boolean showUnreliableLatLon(@Nullable final CacheListType cacheListType) {
        // Show only on map
        return cacheListType == null;
    }

    /**
     * Conditional expression to choose if we need the UserModifiedCoords flag or not.
     *
     * @param cache
     *            The cache currently used
     * @return
     *         True if the UserModifiedCoords flag should be displayed
     */
    private static boolean showUserModifiedCoords(final Geocache cache) {

        return cache.hasUserModifiedCoords() || cache.hasFinalDefined();
    }

    /**
     * Conditional expression to choose if we need the floppy overlay or not.
     *
     * @param cacheListType
     *            The cache list currently used
     * @return
     *         True if the floppy overlay should be displayed
     */
    private static boolean showFloppyOverlay(@Nullable final CacheListType cacheListType) {
        return cacheListType != CacheListType.OFFLINE; // also covers null check
    }

    private static void readLists() {
        if (!listsRead) {
            list2marker.clear();
            final List<StoredList> lists = DataStore.getLists();
            for (final StoredList temp : lists) {
                if (temp.markerId != ListMarker.NO_MARKER.markerId) {
                    list2marker.put(temp.id, temp.markerId);
                }
            }
            listsRead = true;
        }
    }

    public static void resetLists() {
        listsRead = false;
    }

    private static int getAssignedMarkers (final Geocache cache) {
        readLists();

        int value = 0;
        byte counter = 0; // how many markers are already assigned?
        final Set<Integer> lists = cache.getLists();
        for (final Integer list : lists) {
            final Integer markerId = list2marker.get(list);
            if (markerId != null) {
                if (counter == 0) {
                    value = markerId;
                    counter++;
                } else if (counter == 1) {
                    value |= markerId << ListMarker.MAX_BITS_PER_MARKER;
                    counter++;
                } // maximum of two markers allowed
            }
        }
        return value;
    }

}
