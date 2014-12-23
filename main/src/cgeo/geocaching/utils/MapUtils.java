package cgeo.geocaching.utils;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.SparseArray;

import java.util.ArrayList;

public final class MapUtils {

    // data for overlays
    private static final int[][] INSET_RELIABLE = { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }; // center, 33x40 / 45x51 / 60x68 / 90x102 / 120x136
    private static final int[][] INSET_TYPE = { { 5, 8, 6, 10 }, { 4, 4, 4, 11 }, { 6, 6, 6, 14 }, { 9, 9, 9, 21 }, { 12, 12, 12, 28 } }; // center, 22x22 / 36x36
    private static final int[][] INSET_OWN = { { 21, 0, 0, 28 }, { 29, 0, 0, 35 }, { 40, 0, 0, 48 }, { 58, 0, 0, 70 }, { 80, 0, 0, 96 } }; // top right, 12x12 / 16x16 / 20x20 / 32x32 / 40x40
    private static final int[][] INSET_FOUND = { { 0, 0, 21, 28 }, { 0, 0, 29, 35 }, { 0, 0, 40, 48 }, { 0, 0, 58, 70 }, { 0, 0, 80, 96 } }; // top left, 12x12 / 16x16 / 20x20 / 32x32 / 40x40
    private static final int[][] INSET_USERMODIFIEDCOORDS = { { 21, 28, 0, 0 }, { 19, 25, 0, 0 }, { 25, 33, 0, 0 }, { 38, 50, 0, 0 }, { 50, 66, 0, 0 } }; // bottom right, 12x12 / 26x26 / 35x35 / 52x52 / 70x70
    private static final int[][] INSET_PERSONALNOTE = { { 0, 28, 21, 0 }, { 0, 25, 19, 0 }, { 0, 33, 25, 0 }, { 0, 50, 38, 0 }, { 0, 66, 50, 0 } }; // bottom left, 12x12 / 26x26 / 35x35 / 52x52 / 70x70

    private static final SparseArray<LayerDrawable> overlaysCache = new SparseArray<>();

    private MapUtils() {
        // Do not instantiate
    }

    /**
     * Build the drawable for a given cache.
     *
     * @param res the resources to use
     * @param cache the cache to build the drawable for
     * @return a drawable representing the current cache status
     */
    public static LayerDrawable getCacheMarker(final Resources res, final Geocache cache) {
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
                .toHashCode();

        synchronized (overlaysCache) {
            LayerDrawable drawable = overlaysCache.get(hashcode);
            if (drawable == null) {
                drawable = createCacheMarker(res, cache);
                overlaysCache.put(hashcode, drawable);
            }
            return drawable;
        }
    }

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

    private static LayerDrawable createWaypointMarker(final Resources res, final Waypoint waypoint) {
        final Drawable marker = res.getDrawable(!waypoint.isVisited() ? R.drawable.marker : R.drawable.marker_transparent);
        final Drawable[] layers = new Drawable[] {
                marker,
                res.getDrawable(waypoint.getWaypointType().markerId)
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

    private static LayerDrawable createCacheMarker(final Resources res, final Geocache cache) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        final ArrayList<Drawable> layers = new ArrayList<>(9);
        final ArrayList<int[]> insets = new ArrayList<>(8);

        // background: disabled or not
        final Drawable marker = res.getDrawable(cache.getMapMarkerId());
        layers.add(marker);
        final int resolution = calculateResolution(marker);
        // reliable or not
        if (!cache.isReliableLatLon()) {
            insets.add(INSET_RELIABLE[resolution]);
            layers.add(res.getDrawable(R.drawable.marker_notreliable));
        }
        // cache type
        layers.add(res.getDrawable(cache.getType().markerId));
        insets.add(INSET_TYPE[resolution]);
        // own
        if (cache.isOwner()) {
            layers.add(res.getDrawable(R.drawable.marker_own));
            insets.add(INSET_OWN[resolution]);
            // if not, checked if stored
        } else if (cache.getListId() > 0) {
            layers.add(res.getDrawable(R.drawable.marker_stored));
            insets.add(INSET_OWN[resolution]);
        }
        // found
        if (cache.isFound()) {
            layers.add(res.getDrawable(R.drawable.marker_found));
            insets.add(INSET_FOUND[resolution]);
            // if not, perhaps logged offline
        } else if (cache.isLogOffline()) {
            layers.add(res.getDrawable(R.drawable.marker_found_offline));
            insets.add(INSET_FOUND[resolution]);
        }
        // user modified coords
        if (cache.hasUserModifiedCoords()) {
            layers.add(res.getDrawable(R.drawable.marker_usermodifiedcoords));
            insets.add(INSET_USERMODIFIEDCOORDS[resolution]);
        }
        // personal note
        if (cache.getPersonalNote() != null) {
            layers.add(res.getDrawable(R.drawable.marker_personalnote));
            insets.add(INSET_PERSONALNOTE[resolution]);
        }

        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

        int index = 1;
        for (final int[] inset : insets) {
            ld.setLayerInset(index++, inset[0], inset[1], inset[2], inset[3]);
        }

        return ld;
    }

    private static int calculateResolution(final Drawable marker) {
        return marker.getIntrinsicWidth() > 40 ? (marker.getIntrinsicWidth() > 50 ? (marker.getIntrinsicWidth() > 70 ? (marker.getIntrinsicWidth() > 100 ? 4 : 3) : 2) : 1) : 0;
    }
}
