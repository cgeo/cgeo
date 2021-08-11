package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
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
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

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
    private static EmojiUtils.EmojiPaint cPaint = null; // cache icons
    private static EmojiUtils.EmojiPaint lPaint = null; // list markers

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
        final ArrayList<Integer> assignedMarkers = getAssignedMarkers(cache);
        final int hashcode = new HashCodeBuilder()
            .append(cache.getAssignedEmoji())
            .append(cache.getType().id)
            .append(cache.isDisabled())
            .append(cache.isArchived())
            .append(cache.getMapMarkerId())
            .append(cache.isOwner())
            .append(cache.isFound())
            .append(cache.isDNF())
            .append(cache.hasWillAttendForFutureEvent())
            .append(showUserModifiedCoords(cache))
            .append(cache.getPersonalNote())
            .append(cache.hasLogOffline())
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
        ArrayList<Integer> assignedMarkers = new ArrayList<>();
        final String geocode = waypoint.getGeocode();
        boolean cacheIsDisabled = false;
        boolean cacheIsArchived = false;
        if (StringUtils.isNotBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (null != cache) {
                assignedMarkers = getAssignedMarkers(cache);
                cacheIsDisabled = cache.isDisabled();
                cacheIsArchived = cache.isArchived();
            }
        }
        final int hashcode = new HashCodeBuilder()
            .append(waypoint.isVisited())
            .append(id)
            .append(assignedMarkers)
            .append(cacheIsDisabled)
            .append(cacheIsArchived)
            .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createWaypointMarker(res, waypoint, assignedMarkers, cacheIsDisabled, cacheIsArchived));
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
    private static LayerDrawable createWaypointMarker(final Resources res, final Waypoint waypoint, final ArrayList<Integer> assignedMarkers, final boolean cacheIsDisabled, final boolean cacheIsArchived) {
        final WaypointType waypointType = waypoint.getWaypointType();

        //final Drawable marker = ResourcesCompat.getDrawable(res, waypoint.isVisited() ? R.drawable.marker : cacheIsDisabled || cacheIsArchived ? R.drawable.marker : R.drawable.marker, null);
        final Drawable marker = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, R.drawable.marker, null));

        if (cacheIsDisabled || cacheIsArchived) {
            DrawableCompat.setTint(marker, ResourcesCompat.getColor(res, R.color.cacheType_disabled, null));
        }

        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        insetsBuilder.withInset(new InsetBuilder(marker));

        final int markerId = null == waypointType ? WaypointType.WAYPOINT.markerId : waypoint.getWaypointType().markerId;
        insetsBuilder.withInset(new InsetBuilder(markerId, VERTICAL.CENTER, HORIZONTAL.CENTER));

        if (cacheIsArchived) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.type_overlay_archived, VERTICAL.CENTER, HORIZONTAL.CENTER, false));
        }
        if (waypoint.isVisited()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.type_overlay_visited, VERTICAL.CENTER, HORIZONTAL.CENTER, false));
        }

        addListMarkers(res, insetsBuilder, assignedMarkers);

        return buildLayerDrawable(insetsBuilder, 2, 2);
    }

    /**
     * adds list markers to drawable given by insetsBuilder
     */
    private static void addListMarkers(final Resources res, final InsetsBuilder insetsBuilder, final ArrayList<Integer> assignedMarkers) {
        if (assignedMarkers.size() > 0) {
            if (lPaint == null) {
                final Drawable marker = ResourcesCompat.getDrawable(res, R.drawable.dot_black, null);
                assert marker != null;
                final Pair<Integer, Integer> markerDimensions = new Pair<>((int) (marker.getIntrinsicWidth() * 1.2), (int) (marker.getIntrinsicHeight() * 1.2));
                final int markerAvailable = markerDimensions.first;
                lPaint = new EmojiUtils.EmojiPaint(res, markerDimensions, markerAvailable, 0, DisplayUtils.calculateMaxFontsize(10, 5, 100, markerAvailable));
            }
            insetsBuilder.withInset(new InsetBuilder(EmojiUtils.getEmojiDrawable(lPaint, assignedMarkers.get(0)), VERTICAL.CENTER, HORIZONTAL.LEFT));
            if (assignedMarkers.size() > 1) {
                insetsBuilder.withInset(new InsetBuilder(EmojiUtils.getEmojiDrawable(lPaint, assignedMarkers.get(1)), VERTICAL.CENTER, HORIZONTAL.RIGHT));
            }
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
    public static LayerDrawable createWaypointDotMarker(final Resources res, final Waypoint waypoint) {
        final Drawable[] layers = { ResourcesCompat.getDrawable(res, waypoint.getWaypointType().dotMarkerId, null) };
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
    @SuppressWarnings("PMD.NPathComplexity") // method readability will not improve by splitting it up
    private static LayerDrawable createCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType, final ArrayList<Integer> assignedMarkers) {
        final int useEmoji = cache.getAssignedEmoji();

        final Drawable marker = ResourcesCompat.getDrawable(res, cache.getMapMarkerId(), null);
        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        if (showBackground(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_pin));
        }
        insetsBuilder.withInset(new InsetBuilder(marker));

        final int tintColor = (cache.isArchived() || cache.isDisabled()) ? R.color.cacheType_disabled : cache.getType().typeColor;
        final Drawable background = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, cache.getMapMarkerBackgroundId(), null));
        DrawableCompat.setTint(background, ResourcesCompat.getColor(res, tintColor, null));
        insetsBuilder.withInset(new InsetBuilder(background, VERTICAL.CENTER, HORIZONTAL.CENTER));

        // cache type
        final int mainMarkerId = getMainMarkerId(cache, cacheListType);

        final boolean doubleSize = showBigSmileys(cacheListType) && mainMarkerId != cache.getType().markerId;
        if (useEmoji > 0 && !doubleSize) {
            if (cPaint == null) {
                final Pair<Integer, Integer> markerDimensions = DisplayUtils.getDrawableDimensions(res, R.drawable.marker_oc);
                final int markerAvailable = (int) (markerDimensions.first * 0.6);
                cPaint = new EmojiUtils.EmojiPaint(res, markerDimensions, markerAvailable, (int) (markerDimensions.second * 0.05), DisplayUtils.calculateMaxFontsize(35, 10, 100, markerAvailable));
            }
            insetsBuilder.withInset(new InsetBuilder(EmojiUtils.getEmojiDrawable(cPaint, useEmoji)));
        } else if (doubleSize) {
            insetsBuilder.withInset(new InsetBuilder(mainMarkerId, VERTICAL.CENTER, HORIZONTAL.CENTER, true));
        } else {
            final Drawable mainIcon = ResourcesCompat.getDrawable(res, mainMarkerId, null);
            insetsBuilder.withInset(new InsetBuilder(mainIcon, VERTICAL.CENTER, HORIZONTAL.CENTER));

        }
        // archived
        if (cache.isArchived()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.type_overlay_archived, VERTICAL.CENTER, HORIZONTAL.CENTER, false));
        }
        // own
        if (cache.isOwner()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_own, VERTICAL.TOP, HORIZONTAL.RIGHT));
            // if not, checked if stored
        } else if (!cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_stored, VERTICAL.TOP, HORIZONTAL.RIGHT));
        }
        // will attend / found
        if (cache.hasWillAttendForFutureEvent() && !doubleSize) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_calendar, VERTICAL.TOP, HORIZONTAL.LEFT));
        } else if (!showBigSmileys(cacheListType)) {
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
        // list markers
        addListMarkers(res, insetsBuilder, assignedMarkers);

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

    @Nullable
    private static Integer getMarkerIdIfLogged(final Geocache cache) {
        if (cache.isFound()) {
            return R.drawable.marker_found;
            // if not, perhaps logged offline
        } else if (cache.hasLogOffline()) {
            final LogType offlineLogType = cache.getOfflineLogType();
            // logs of type NOTE may have a NA/NM log attached to them
            if (offlineLogType == LogType.NOTE) {
                final LogEntry offlineLog = cache.getOfflineLog();
                if (offlineLog.reportProblem == ReportProblemType.ARCHIVE) {
                    return R.drawable.marker_archive;
                } else if (offlineLog.reportProblem != ReportProblemType.NO_PROBLEM) {
                    return R.drawable.marker_maintenance;
                }
            }
            return offlineLogType == null ? R.drawable.marker_found_offline : offlineLogType.getOfflineLogOverlay();
            // an offline log is more important than a DNF
        } else if (cache.isDNF()) {
            return R.drawable.marker_not_found_offline;
        } else if (cache.hasWillAttendForFutureEvent()) {
            return R.drawable.marker_calendar;
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
        int dotDrw = -1;
        int tintColor = -1;
        if (cache.isFound()) {
            dotDrw = R.drawable.dot_found;
        } else if (cache.hasLogOffline()) {
            final LogType offlineLogType = cache.getOfflineLogType();
            // logs of type NOTE may have a NA/NM log attached to them
            if (offlineLogType.isFoundLog()) {
                dotDrw = R.drawable.dot_found_offline;
            } else if (offlineLogType == LogType.DIDNT_FIND_IT) {
                dotDrw = R.drawable.dot_not_found_offline;
            } else if (cache.hasWillAttendForFutureEvent()) {
                dotDrw = R.drawable.dot_marker_calendar;
            } else if (offlineLogType == LogType.NOTE) {
                final LogEntry offlineLog = cache.getOfflineLog();
                if (offlineLog.reportProblem == ReportProblemType.NO_PROBLEM) {
                    dotDrw = R.drawable.dot_note_offline;
                } else if (offlineLog.reportProblem == ReportProblemType.ARCHIVE) {
                    dotDrw = R.drawable.dot_marker_archive_offline;
                } else {
                    dotDrw = R.drawable.dot_marker_maintenance_offline;
                }
            }
        } else if (cache.hasUserModifiedCoords()) {
            dotDrw = R.drawable.dot_marker_usermodifiedcoords;
        }

        if (dotDrw == -1) {
            dotDrw = R.drawable.dot_foreground;
            if (cache.isArchived() || cache.isDisabled()) {
                tintColor = R.color.cacheType_disabled;
            } else {
                tintColor = cache.getType().typeColor;
            }
        }

        final Drawable dotMarker = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, R.drawable.dot_background, null));
        final Drawable dotIcon = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, dotDrw, null));
        if (tintColor != -1) {
            DrawableCompat.setTint(dotIcon, ResourcesCompat.getColor(res, tintColor, null));
        }

        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, dotMarker.getIntrinsicWidth(), dotMarker.getIntrinsicHeight());
        insetsBuilder.withInset(new InsetBuilder(dotMarker));
        insetsBuilder.withInset(new InsetBuilder(dotIcon, VERTICAL.CENTER, HORIZONTAL.CENTER));
        return buildLayerDrawable(insetsBuilder, 2, 2);
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
                if (temp.markerId != EmojiUtils.NO_EMOJI) {
                    list2marker.put(temp.id, temp.markerId);
                }
            }
            listsRead = true;
        }
    }

    public static void resetLists() {
        listsRead = false;
    }

    private static ArrayList<Integer> getAssignedMarkers (final Geocache cache) {
        readLists();

        final ArrayList<Integer> result = new ArrayList<>();
        final Set<Integer> lists = cache.getLists();
        for (final Integer list : lists) {
            final Integer markerId = list2marker.get(list);
            if (markerId != null) {
                result.add(markerId);
            }
        }
        return result;
    }

}
