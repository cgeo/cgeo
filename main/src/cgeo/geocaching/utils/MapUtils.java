package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class MapUtils {

    // data for overlays
    private static final int[][] INSET_RELIABLE = { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }; // center, 25x30 / 33x40 / 45x51 / 60x68 / 90x102 / 120x136
    private static final int[][] INSET_TYPE = { { 3, 6, 4, 7 }, { 5, 8, 6, 10 }, { 4, 4, 5, 11 }, { 5, 5, 6, 14 }, { 8, 8, 10, 22 }, { 10, 10, 13, 29 } };
    private static final int[][] INSET_TYPE_LIST = { { 1, 1, 1, 1 }, { 2, 2, 2, 2 }, { 3, 3, 3, 3 }, { 4, 4, 4, 4 }, { 6, 6, 6, 6 }, { 8, 8, 8, 8 } };
    private static final int[][] INSET_OWN = { { 15, 0, 0, 21 }, { 21, 0, 0, 28 }, { 27, 0, 0, 33 }, { 36, 0, 0, 44 }, { 54, 0, 0, 66 }, { 72, 0, 0, 88 } };
    private static final int[][] INSET_OWN_LIST = { { 16, 0, 0, 16 }, { 22, 0, 0, 22 }, { 33, 0, 0, 33 }, { 44, 0, 0, 44 }, { 66, 0, 0, 66 }, { 88, 0, 0, 88 } };
    private static final int[][] INSET_FOUND = { { 0, 0, 15, 21 }, { 0, 0, 21, 28 }, { 0, 0, 27, 33 }, { 0, 0, 36, 44 }, { 0, 0, 54, 66 }, { 0, 0, 72, 88 } };
    private static final int[][] INSET_FOUND_LIST = { { 0, 0, 16, 16 }, { 0, 0, 22, 22 }, { 0, 0, 33, 33 }, { 0, 0, 44, 44 }, { 0, 0, 66, 66 }, { 0, 0, 88, 88 } };
    private static final int[][] INSET_USERMODIFIEDCOORDS = { { 12, 17, 0, 0 }, { 16, 23, 0, 0 }, { 19, 25, 0, 0 }, { 26, 34, 0, 0 }, { 39, 51, 0, 0 }, { 52, 68, 0, 0 } };
    private static final int[][] INSET_USERMODIFIEDCOORDS_LIST = { { 16, 14, 0, 2 }, { 22, 19, 0, 3 }, { 33, 28, 0, 4 }, { 44, 38, 0, 6 }, { 66, 57, 0, 9 }, { 88, 76, 0, 12 } };
    private static final int[][] INSET_PERSONALNOTE = { { 0, 17, 12, 0 }, { 0, 23, 16, 0 }, { 0, 25, 19, 0 }, { 0, 34, 26, 0 }, { 0, 51, 39, 0 }, { 0, 68, 52, 0 } };
    private static final int[][] INSET_PERSONALNOTE_LIST = { { 0, 14, 16, 2 }, { 0, 19, 22, 3 }, { 0, 28, 33, 4 }, { 0, 38, 44, 6 }, { 0, 57, 66, 9 }, { 0, 76, 88, 12 } };

    private static final SparseArray<CacheMarker> overlaysCache = new SparseArray<>();

    private MapUtils() {
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
                .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createCacheMarker(res, cache, cacheListType));
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
        final int hashcode = new HashCodeBuilder()
        .append(waypoint.isVisited())
        .append(waypoint.getWaypointType().id)
        .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createWaypointMarker(res, waypoint));
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
    private static LayerDrawable createWaypointMarker(final Resources res, final Waypoint waypoint) {
        final Drawable marker = Compatibility.getDrawable(res, !waypoint.isVisited() ? R.drawable.marker : R.drawable.marker_transparent);
        final Drawable[] layers = {
                marker,
                Compatibility.getDrawable(res, waypoint.getWaypointType().markerId)
        };
        final LayerDrawable drawable = new LayerDrawable(layers);
        final int resolution = calculateResolution(marker);
        drawable.setLayerInset(1, INSET_TYPE[resolution][0], INSET_TYPE[resolution][1], INSET_TYPE[resolution][2], INSET_TYPE[resolution][3]);
        return drawable;
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
    private static LayerDrawable createCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        final List<Drawable> layers = new ArrayList<>(9);
        final List<int[]> insets = new ArrayList<>(8);

        // background: disabled or not
        final Drawable marker = Compatibility.getDrawable(res, cache.getMapMarkerId());
        final int resolution = calculateResolution(marker);
        // Show the background circle only on map
        if (showBackground(cacheListType)) {
            layers.add(marker);
            insets.add(INSET_RELIABLE[resolution]);
        }
        // reliable or not
        if (!cache.isReliableLatLon() && showUnreliableLatLon(cacheListType)) {
            insets.add(INSET_RELIABLE[resolution]);
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_notreliable));
        }
        // cache type
        layers.add(Compatibility.getDrawable(res, cache.getType().markerId));
        insets.add(getTypeInset(cacheListType)[resolution]);
        // own
        if (cache.isOwner()) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_own));
            insets.add(getOwnInset(cacheListType)[resolution]);
            // if not, checked if stored
        } else if (!cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_stored));
            insets.add(getOwnInset(cacheListType)[resolution]);
        }
        // found
        if (cache.isFound()) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_found));
            insets.add(getFoundInset(cacheListType)[resolution]);
            // if not, perhaps logged offline
        } else if (cache.isLogOffline()) {
            final LogType offlineLogType = cache.getOfflineLogType();
            if (offlineLogType == null) {
                // Default, backward compatible
                layers.add(Compatibility.getDrawable(res, R.drawable.marker_found_offline));
            } else {
                layers.add(Compatibility.getDrawable(res, offlineLogType.getOfflineLogOverlay()));
            }
            insets.add(getFoundInset(cacheListType)[resolution]);
        }
        // user modified coords
        if (showUserModifiedCoords(cache)) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_usermodifiedcoords));
            insets.add(getUMCInset(cacheListType)[resolution]);
        }
        // personal note
        if (cache.getPersonalNote() != null) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_personalnote));
            insets.add(getPNInset(cacheListType)[resolution]);
        }

        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

        int index = 0;
        for (final int[] inset : insets) {
            ld.setLayerInset(index++, inset[0], inset[1], inset[2], inset[3]);
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
     * Get the resolution index used for positioning the overlays elements.
     *
     * @param marker
     *            The Drawable reference
     * @return
     *         an index for the overlays positions
     */
    private static int calculateResolution(final Drawable marker) {
        return marker.getIntrinsicWidth() >= 30 ? (marker.getIntrinsicWidth() >= 40 ? (marker.getIntrinsicWidth() >= 50 ? (marker.getIntrinsicWidth() >= 70 ? (marker.getIntrinsicWidth() >= 100 ? 5 : 4) : 3) : 2) : 1) : 0;
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
        return cacheListType == null || cacheListType != CacheListType.OFFLINE;
    }

    private static int[][] getTypeInset(@Nullable final CacheListType cacheListType) {
        return cacheListType == null ?
                INSET_TYPE :
                INSET_TYPE_LIST;
    }

    private static int[][] getOwnInset(@Nullable final CacheListType cacheListType) {
        return cacheListType == null ?
                INSET_OWN :
                INSET_OWN_LIST;
    }

    private static int[][] getFoundInset(@Nullable final CacheListType cacheListType) {
        return cacheListType == null ?
                INSET_FOUND :
                INSET_FOUND_LIST;
    }

    private static int[][] getUMCInset(@Nullable final CacheListType cacheListType) {
        return cacheListType == null ?
                INSET_USERMODIFIEDCOORDS :
                INSET_USERMODIFIEDCOORDS_LIST;
    }

    private static int[][] getPNInset(@Nullable final CacheListType cacheListType) {
        return cacheListType == null ?
                INSET_PERSONALNOTE :
                INSET_PERSONALNOTE_LIST;
    }
}
