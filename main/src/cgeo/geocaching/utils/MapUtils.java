package cgeo.geocaching.utils;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.enumerations.CacheListType;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

public final class MapUtils {

    // data for overlays
    private static final int[][] INSET_RELIABLE = { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }; // center, 33x40 / 45x51 / 60x68 / 90x102 / 120x136
    private static final int[][] INSET_TYPE = { { 5, 8, 6, 10 }, { 4, 4, 4, 11 }, { 6, 6, 6, 14 }, { 9, 9, 9, 21 }, { 12, 12, 12, 28 } }; // center, 22x22 / 36x36
    private static final int[][] INSET_OWN = { { 21, 0, 0, 28 }, { 29, 0, 0, 35 }, { 40, 0, 0, 48 }, { 58, 0, 0, 70 }, { 80, 0, 0, 96 } }; // top right, 12x12 / 16x16 / 20x20 / 32x32 / 40x40
    private static final int[][] INSET_FOUND = { { 0, 0, 21, 28 }, { 0, 0, 29, 35 }, { 0, 0, 40, 48 }, { 0, 0, 58, 70 }, { 0, 0, 80, 96 } }; // top left, 12x12 / 16x16 / 20x20 / 32x32 / 40x40
    private static final int[][] INSET_USERMODIFIEDCOORDS = { { 21, 28, 0, 0 }, { 29, 35, 0, 0 }, { 40, 48, 0, 0 }, { 58, 70, 0, 0 }, { 80, 96, 0, 0 } }; // bottom right, 12x12 / 16x16 / 20x20 / 32x32 / 40x40
    private static final int[][] INSET_PERSONALNOTE = { { 0, 28, 21, 0 }, { 0, 35, 29, 0 }, { 0, 48, 40, 0 }, { 0, 70, 58, 0 }, { 0, 96, 80, 0 } }; // bottom left, 12x12 / 16x16 / 20x20 / 32x32 / 40x40

    private static final SparseArray<LayerDrawable> overlaysCache = new SparseArray<>();

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
    public static LayerDrawable getCacheMarker(final Resources res, final Geocache cache) {
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
    public static LayerDrawable getCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType) {
        final int hashcode = new HashCodeBuilder()
                .append(cache.isReliableLatLon())
                .append(cache.getType().id)
                .append(cache.isDisabled() || cache.isArchived())
                .append(cache.getMapMarkerId())
                .append(cache.isOwner())
                .append(cache.isFound())
                .append(cache.hasUserModifiedCoords())
                .append(cache.getPersonalNote())
                .append(cache.isLogOffline())
                .append(cache.getListId() > 0)
                .append(showBackground(cacheListType))
                .append(showFloppyOverlay(cacheListType))
                .toHashCode();

        synchronized (overlaysCache) {
            LayerDrawable drawable = overlaysCache.get(hashcode);
            if (drawable == null) {
                drawable = createCacheMarker(res, cache, cacheListType);
                overlaysCache.put(hashcode, drawable);
            }
            return drawable;
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
    public static LayerDrawable getWaypointMarker(final Resources res, final Waypoint waypoint) {
        final int hashcode = new HashCodeBuilder()
        .append(waypoint.isVisited())
        .append(waypoint.getWaypointType().id)
        .toHashCode();

        synchronized (overlaysCache) {
            LayerDrawable drawable = overlaysCache.get(hashcode);
            if (drawable == null) {
                drawable = createWaypointMarker(res, waypoint);
                overlaysCache.put(hashcode, drawable);
            }
            return drawable;
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
    private static LayerDrawable createCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        final List<Drawable> layers = new ArrayList<>(9);
        final List<int[]> insets = new ArrayList<>(8);

        // background: disabled or not
        final Drawable marker = Compatibility.getDrawable(res, cache.getMapMarkerId());
        // Show the background circle only on map
        if (showBackground(cacheListType)) {
            layers.add(marker);
        }
        final int resolution = calculateResolution(marker);
        // reliable or not
        if (!cache.isReliableLatLon()) {
            insets.add(INSET_RELIABLE[resolution]);
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_notreliable));
        }
        // cache type
        layers.add(Compatibility.getDrawable(res, cache.getType().markerId));
        insets.add(INSET_TYPE[resolution]);
        // own
        if (cache.isOwner()) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_own));
            insets.add(INSET_OWN[resolution]);
            // if not, checked if stored
        } else if (cache.getListId() > 0 && showFloppyOverlay(cacheListType)) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_stored));
            insets.add(INSET_OWN[resolution]);
        }
        // found
        if (cache.isFound()) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_found));
            insets.add(INSET_FOUND[resolution]);
            // if not, perhaps logged offline
        } else if (cache.isLogOffline()) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_found_offline));
            insets.add(INSET_FOUND[resolution]);
        }
        // user modified coords
        if (cache.hasUserModifiedCoords()) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_usermodifiedcoords));
            insets.add(INSET_USERMODIFIEDCOORDS[resolution]);
        }
        // personal note
        if (cache.getPersonalNote() != null) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_personalnote));
            insets.add(INSET_PERSONALNOTE[resolution]);
        }

        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

        int index = showBackground(cacheListType) ? 1 : 0;
        for (final int[] inset : insets) {
            ld.setLayerInset(index++, inset[0], inset[1], inset[2], inset[3]);
        }

        return ld;
    }

    /**
     * Get the resolution index used for positionning the overlays elements.
     *
     * @param marker
     *          The Drawable reference
     * @return
     *          an index for the overlays positions
     */
    private static int calculateResolution(final Drawable marker) {
        return marker.getIntrinsicWidth() > 40 ? (marker.getIntrinsicWidth() > 50 ? (marker.getIntrinsicWidth() > 70 ? (marker.getIntrinsicWidth() > 100 ? 4 : 3) : 2) : 1) : 0;
    }

    /**
     * Conditionnal expression to choose if we need the background circle or not.
     *
     * @param cacheListType
     *          The cache list currently used
     * @return
     *          True if the background circle should be displayed
     */
    private static boolean showBackground(final CacheListType cacheListType) {
        return cacheListType == null;
    }

    /**
     * Conditionnal expression to choose if we need the floppy overlay or not.
     *
     * @param cacheListType
     *          The cache list currently used
     * @return
     *          True if the floppy overlay should be displayed
     */
    private static boolean showFloppyOverlay(final CacheListType cacheListType) {
        return cacheListType == null || cacheListType != CacheListType.OFFLINE;
    }
}
