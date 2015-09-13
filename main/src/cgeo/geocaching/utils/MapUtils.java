package cgeo.geocaching.utils;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LogType;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.jdt.annotation.NonNull;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MapUtils {

    // data for overlays
    private static final int[][] INSET_RELIABLE = { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }; // center, 33x40 / 45x51 / 60x68 / 90x102 / 120x136
    private static final int[][] INSET_TYPE = { { 1, 1, 1, 4 }, { 2, 2, 2, 6 }, { 3, 3, 3, 9 }, { 4, 4, 4, 12 }, { 6, 6, 6, 18 } }; // center, 22x22 / 36x36
    private static final int[][] INSET_OWN = { { 16, 0, 0, 19 }, { 22, 0, 0, 26 }, { 33, 0, 0, 39 }, { 45, 0, 0, 53 }, { 67, 0, 0, 79 } }; // top right, 12x12 / 16x16 / 20x20 / 32x32 / 40x40
    private static final int[][] INSET_FOUND = { { 0, 0, 16, 19 }, { 0, 0, 22, 26 }, { 0, 0, 33, 39 }, { 0, 0, 45, 53 }, { 0, 0, 67, 79 } }; // top left, 12x12 / 16x16 / 20x20 / 32x32 / 40x40
    private static final int[][] INSET_USERMODIFIEDCOORDS = { { 16, 19, 0, 0 }, { 22, 26, 0, 0 }, { 33, 39, 0, 0 }, { 45, 53, 0, 0 }, { 67, 79, 0, 0 } }; // bottom right, 12x12 / 16x16 / 20x20 / 32x32 / 40x40
    private static final int[][] INSET_PERSONALNOTE = { { 0, 19, 16, 0 }, { 0, 26, 22, 0 }, { 0, 39, 33, 0 }, { 0, 53, 45, 0 }, { 0, 79, 67, 0 } }; // bottom left, 12x12 / 16x16 / 20x20 / 32x32 / 40x40

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
    @NonNull
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
    @NonNull
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
                .append(cache.getOfflineLogType())
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
    @NonNull
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
            final LogType offlineLogType = cache.getOfflineLogType();
            if (offlineLogType == null) {
                // Default, backward compatible
                layers.add(Compatibility.getDrawable(res, R.drawable.marker_found_offline));
            } else {
                layers.add(Compatibility.getDrawable(res, offlineLogType.getOfflineLogOverlay()));
            }
            insets.add(INSET_FOUND[resolution]);
        }
        // user modified coords
        if (cache.hasUserModifiedCoords()) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_usermodifiedcoords));
            insets.add(driftBottomItems(INSET_USERMODIFIEDCOORDS, resolution, cacheListType));
        }
        // personal note
        if (cache.getPersonalNote() != null) {
            layers.add(Compatibility.getDrawable(res, R.drawable.marker_personalnote));
            insets.add(driftBottomItems(INSET_PERSONALNOTE, resolution, cacheListType));
        }

        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

        int index = 0;
        for (final int[] inset : insets) {
            ld.setLayerInset(index++, inset[0], inset[1], inset[2], inset[3]);
        }

        return ld;
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
        return marker.getIntrinsicWidth() >= 30 ? (marker.getIntrinsicWidth() >= 45 ? (marker.getIntrinsicWidth() >= 60 ? (marker.getIntrinsicWidth() >= 90 ? 4 : 3) : 2) : 1) : 0;
    }

    /**
     * Calculate a new position for the bottom line overlay items, when there is no background circle.
     *
     * @param inset
     *          Original inset position
     * @param resolution
     *          The current item resolution
     * @param cacheListType
     *          The current CacheListType
     * @return
     *          The new drifted inset position
     */
    private static int[] driftBottomItems(final int[][] inset, final int resolution, @Nullable final CacheListType cacheListType) {
        // Do not drift in when background is displayed
        if (showBackground(cacheListType)) {
            return inset[resolution];
        }
        final int[] newPosition = Arrays.copyOf(inset[resolution], 4);
        newPosition[1] -= INSET_TYPE[resolution][3] * 3/2;
        newPosition[3] += INSET_TYPE[resolution][3] * 3/2;
        return newPosition;
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
}
