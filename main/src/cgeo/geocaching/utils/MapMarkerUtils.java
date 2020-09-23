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
import cgeo.geocaching.storage.DataStore;

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

    // data for overlays
    private static final int[] FULLSIZE = { 0, 0, 0, 0 };

    private enum VERTICAL { TOP, CENTER, BOTTOM }
    private enum HORIZONTAL { LEFT, CENTER, RIGHT }

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
        final List<Drawable> layers = new ArrayList<>(2);
        final List<int[]> insets = new ArrayList<>(2);

        final Drawable marker = Compatibility.getDrawable(res, !waypoint.isVisited() ? R.drawable.marker : R.drawable.marker_transparent);
        layers.add(marker);
        insets.add(FULLSIZE);

        // get actual layer size in px
        final int width = marker.getIntrinsicWidth();
        final int height = marker.getIntrinsicHeight();

        Drawable inset = Compatibility.getDrawable(res, null == waypointType ? WaypointType.WAYPOINT.markerId : waypoint.getWaypointType().markerId);
        layers.add(inset);
        insets.add(insetHelper(width, height, inset, VERTICAL.CENTER, HORIZONTAL.CENTER));

        // assigned lists with markers
        int markerId = assignedMarkers & ListMarker.BITMASK;
        if (markerId > 0) {
            inset = Compatibility.getDrawable(res, ListMarker.getResDrawable(markerId));
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.CENTER, HORIZONTAL.LEFT));
        }

        markerId = (assignedMarkers >> ListMarker.MAX_BITS_PER_MARKER) & ListMarker.BITMASK;
        if (markerId > 0) {
            inset = Compatibility.getDrawable(res, ListMarker.getResDrawable(markerId));
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.CENTER, HORIZONTAL.RIGHT));
        }

        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

        int index = 0;
        for (final int[] temp : insets) {
            ld.setLayerInset(index++, temp[0], temp[1], temp[2], temp[3]);
        }
        return ld;
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

    private static int[] insetHelper(final int width, final int height, final Drawable b, final VERTICAL vPos, final HORIZONTAL hPos) {
        final int[] insetPadding = { 0, 0, 0, 0 }; // left, top, right, bottom padding for inset
        final int iWidth = b.getIntrinsicWidth();
        final int iHeight = b.getIntrinsicHeight();

        // vertical offset from bottom:
        final int vDelta = height / 10;

        // horizontal
        if (hPos == HORIZONTAL.CENTER) {
            insetPadding[0] = (width - iWidth) / 2;
        } else if (hPos == HORIZONTAL.RIGHT) {
            insetPadding[0] = width - iWidth;
        }
        insetPadding[2] = width - iWidth - insetPadding[0];

        // vertical
        if (vPos == VERTICAL.CENTER) {
            insetPadding[1] = Math.max((height - iHeight) / 2 - vDelta, 0);
        } else if (vPos == VERTICAL.BOTTOM) {
            insetPadding[1] = Math.max(height - iHeight - vDelta, 0);
        }
        insetPadding[3] = height - iHeight - insetPadding[1];

        return insetPadding;
    }

    /**
     * Build the drawable for a given cache.
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
    private static LayerDrawable createCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType, final int assignedMarkers) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        final List<Drawable> layers = new ArrayList<>(11);
        final List<int[]> insets = new ArrayList<>(10);

        // background: disabled or not
        final Drawable marker = Compatibility.getDrawable(res, cache.getMapMarkerId());

        // get actual layer size in px
        final int width = marker.getIntrinsicWidth();
        final int height = marker.getIntrinsicHeight();

        // Show the background circle only on map
        if (showBackground(cacheListType)) {
            layers.add(marker);
            insets.add(FULLSIZE);
        }
        // reliable or not
        if (!cache.isReliableLatLon() && showUnreliableLatLon(cacheListType)) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_notreliable));
            insets.add(FULLSIZE);
        }
        // cache type
        Drawable inset = Compatibility.getDrawable(res, cache.getType().markerId);
        layers.add(inset);
        insets.add(insetHelper(width, height, inset, VERTICAL.CENTER, HORIZONTAL.CENTER));
        // own
        if (cache.isOwner()) {
            inset = Compatibility.getDrawable(res, R.drawable.marker_own);
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.TOP, HORIZONTAL.RIGHT));
            // if not, checked if stored
        } else if (!cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            inset = Compatibility.getDrawable(res, R.drawable.marker_stored);
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.TOP, HORIZONTAL.RIGHT));
        }
        // found
        if (cache.isFound()) {
            inset = Compatibility.getDrawable(res, R.drawable.marker_found);
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.TOP, HORIZONTAL.LEFT));
            // if not, perhaps logged offline
        } else if (cache.isLogOffline()) {
            final LogType offlineLogType = cache.getOfflineLogType();
            inset = Compatibility.getDrawable(res, offlineLogType == null ? R.drawable.marker_found_offline : offlineLogType.getOfflineLogOverlay());
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.TOP, HORIZONTAL.LEFT));
        }
        // user modified coords
        if (showUserModifiedCoords(cache)) {
            inset = Compatibility.getDrawable(res, R.drawable.marker_usermodifiedcoords);
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.BOTTOM, HORIZONTAL.RIGHT));
        }
        // personal note
        if (cache.getPersonalNote() != null) {
            inset = Compatibility.getDrawable(res, R.drawable.marker_personalnote);
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.BOTTOM, HORIZONTAL.LEFT));
        }
        // assigned lists with markers
        int markerId = assignedMarkers & ListMarker.BITMASK;
        if (markerId > 0) {
            inset = Compatibility.getDrawable(res, ListMarker.getResDrawable(markerId));
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.CENTER, HORIZONTAL.LEFT));
        }

        markerId = (assignedMarkers >> ListMarker.MAX_BITS_PER_MARKER) & ListMarker.BITMASK;
        if (markerId > 0) {
            inset = Compatibility.getDrawable(res, ListMarker.getResDrawable(markerId));
            layers.add(inset);
            insets.add(insetHelper(width, height, inset, VERTICAL.CENTER, HORIZONTAL.RIGHT));
        }

        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

        int index = 0;
        for (final int[] temp : insets) {
            ld.setLayerInset(index++, temp[0], temp[1], temp[2], temp[3]);
        }

        return ld;
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
