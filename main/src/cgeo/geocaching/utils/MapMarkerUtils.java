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
import cgeo.geocaching.utils.builders.InsetsBuilder;
import static cgeo.geocaching.utils.DisplayUtils.SIZE_CACHE_MARKER_DP;
import static cgeo.geocaching.utils.DisplayUtils.SIZE_LIST_MARKER_DP;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Gravity;

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
            .append(showPin(cacheListType))
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
     * Build the drawable for penzlina given cache.
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

        // marker shape
        final Drawable marker = ResourcesCompat.getDrawable(res, cache.getMapMarkerId(), null);
        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        if (showPin(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_pin));
        }
        insetsBuilder.withInset(new InsetBuilder(marker));

        // marker foreground
        final int mainMarkerId = getMainMarkerId(cache, cacheListType);
        // doubleSize = "Big icons for logged caches" enabled AND no offline log
        final boolean doubleSize = showBigSmileys(cacheListType) && (mainMarkerId != cache.getType().markerId);
        if (useEmoji > 0 && !doubleSize) {
            // custom icon
            if (cPaint == null) {
                final int markerAvailable = DisplayUtils.getPxFromDp(res, SIZE_CACHE_MARKER_DP, (float)(Math.sqrt(2)*0.5*1.15)); // 1 fits for a round icon; to fit a square icon into the same space calculate the sqrt; then a little bit larger (1.2) to make both square and round icons look ok
                cPaint = new EmojiUtils.EmojiPaint(res, new Pair<>(markerAvailable, markerAvailable), markerAvailable, 0, DisplayUtils.calculateMaxFontsize(35, 10, 100, markerAvailable));
            }
            insetsBuilder.withInset(new InsetBuilder(EmojiUtils.getEmojiDrawable(cPaint, useEmoji), Gravity.CENTER | Gravity.CENTER));
        } else {
            // cache type background color
            final int tintColor = (cache.isArchived() || cache.isDisabled()) ? R.color.cacheType_disabled : cache.getType().typeColor;
            final Drawable background = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, cache.getMapMarkerBackgroundId(), null));
            DrawableCompat.setTint(background, ResourcesCompat.getColor(res, tintColor, null));
            insetsBuilder.withInset(new InsetBuilder(background, Gravity.CENTER));
            // main icon (type icon / custom cache icon)
            insetsBuilder.withInset(new InsetBuilder(mainMarkerId, Gravity.CENTER, doubleSize));
        }

        // overlays
        // center: archived
        if (cache.isArchived()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.type_overlay_archived, Gravity.CENTER, false));
        }
        // top-right: owned / stored
        if (cache.isOwner()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_own, Gravity.TOP | Gravity.RIGHT));
            // if not, checked if stored
        } else if (!cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_stored, Gravity.TOP | Gravity.RIGHT));
        }
        // top-left: will attend / found / not found / offline-logs
        if (cache.hasWillAttendForFutureEvent() && !doubleSize) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_calendar, Gravity.TOP | Gravity.LEFT));
        } else if (!showBigSmileys(cacheListType)) {
            final Integer loggedMarkerId = getMarkerIdIfLogged(cache);
            if (loggedMarkerId != null) {
                insetsBuilder.withInset(new InsetBuilder(loggedMarkerId, Gravity.TOP | Gravity.LEFT));
            }
        }
        // bottom-right: user modified coords
        if (showUserModifiedCoords(cache)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_usermodifiedcoords, Gravity.BOTTOM | Gravity.RIGHT));
        }
        // bottom-left: personal note
        if (cache.getPersonalNote() != null) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_personalnote, Gravity.BOTTOM | Gravity.LEFT));
        }
        // center-left/center-right: list markers
        addListMarkers(res, insetsBuilder, assignedMarkers);

        return buildLayerDrawable(insetsBuilder, 11, 10);
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
        boolean cacheIsDisabled = false;
        boolean cacheIsArchived = false;
        final String geocode = waypoint.getGeocode();
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
            .append(waypoint.getMapMarkerId())
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

        final Drawable marker = ResourcesCompat.getDrawable(res, waypoint.getMapMarkerId(), null);
        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_pin));
        insetsBuilder.withInset(new InsetBuilder(marker));

        final Drawable mainMarker = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, null == waypointType ? WaypointType.WAYPOINT.markerId : waypoint.getWaypointType().markerId, null));
        if (cacheIsDisabled || cacheIsArchived) {
            DrawableCompat.setTint(mainMarker, ResourcesCompat.getColor(res, R.color.cacheType_disabled, null));
        }
        insetsBuilder.withInset(new InsetBuilder(mainMarker, Gravity.CENTER));

        if (cacheIsArchived) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.type_overlay_archived, Gravity.CENTER, false));
        }
        // bottom-right: visited
        if (waypoint.isVisited()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_visited, Gravity.BOTTOM | Gravity.RIGHT));
        }

        addListMarkers(res, insetsBuilder, assignedMarkers);

        return buildLayerDrawable(insetsBuilder, 2, 2);
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
        int dotIcon = -1;
        int tintColor;

        tintColor = cache.getType().typeColor;

        if (cache.isArchived() || cache.isDisabled()) {
            tintColor = R.color.cacheType_disabled;
        }

        if (cache.isFound()) {
            dotIcon = R.drawable.dot_found;
            tintColor = R.color.dotBg_found;
        } else if (cache.hasLogOffline()) {
            final LogType offlineLogType = cache.getOfflineLogType();
            // logs of type NOTE may have a NA/NM log attached to them
            if (offlineLogType.isFoundLog()) {
                dotIcon = R.drawable.dot_found_offline;
                tintColor = R.color.dotBg_found;
            } else if (offlineLogType == LogType.DIDNT_FIND_IT) {
                dotIcon = R.drawable.dot_not_found_offline;
                tintColor = R.color.dotBg_notFound;
            } else if (cache.hasWillAttendForFutureEvent()) {
                dotIcon = R.drawable.dot_marker_calendar;
                tintColor = R.color.dotBg_calendar;
            } else if (offlineLogType == LogType.NOTE) {
                final LogEntry offlineLog = cache.getOfflineLog();
                if (offlineLog.reportProblem == ReportProblemType.NO_PROBLEM) {
                    dotIcon = R.drawable.dot_note_offline;
                    tintColor = R.color.dotBg_offlineLogNote;
                } else if (offlineLog.reportProblem == ReportProblemType.ARCHIVE) {
                    dotIcon = R.drawable.dot_marker_archive_offline;
                    tintColor = R.color.dotBg_offlineLogArchive;
                } else {
                    dotIcon = R.drawable.dot_marker_maintenance_offline;
                    tintColor = R.color.dotBg_offlineLogMaintanance;
                }
            }
        } else if (cache.hasUserModifiedCoords()) {
            dotIcon = R.drawable.dot_marker_usermodifiedcoords;
        }

        final Drawable dotMarker = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, cache.getMapDotMarkerId(), null));
        final Drawable dotBackground = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, cache.getMapDotMarkerBackgroundId(), null));
        if (tintColor != -1) {
            DrawableCompat.setTint(dotBackground, ResourcesCompat.getColor(res, tintColor, null));
        }

        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, dotMarker.getIntrinsicWidth(), dotMarker.getIntrinsicHeight());
        insetsBuilder.withInset(new InsetBuilder(dotMarker));
        insetsBuilder.withInset(new InsetBuilder(dotBackground, Gravity.CENTER));
        if (dotIcon != -1) {
            insetsBuilder.withInset(new InsetBuilder(dotIcon, Gravity.CENTER));
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
        final Drawable dotMarker = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, waypoint.getMapDotMarkerId(), null));
        DrawableCompat.setTint(dotMarker, ResourcesCompat.getColor(res, R.color.dotBg_waypointOutline, null));
        final Drawable dotBackground = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, waypoint.getMapDotMarkerBackgroundId(), null));
        DrawableCompat.setTint(dotBackground, ResourcesCompat.getColor(res, R.color.dotBg_waypointBg, null));

        final Drawable dotIcon = DrawableCompat.wrap(ResourcesCompat.getDrawable(res, waypoint.getWaypointType().dotMarkerId, null));

        // Tint disabled waypoints
        String geocode = waypoint.getGeocode();
        if (StringUtils.isNotBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (null != cache) {
                if (cache.isDisabled() || cache.isArchived()) {
                    DrawableCompat.setTint(dotIcon, ResourcesCompat.getColor(res, R.color.cacheType_disabled, null));
                }
            }
        }

        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, dotMarker.getIntrinsicWidth(), dotMarker.getIntrinsicHeight());
        insetsBuilder.withInset(new InsetBuilder(dotMarker));
        insetsBuilder.withInset(new InsetBuilder(dotBackground, Gravity.CENTER));
        insetsBuilder.withInset(new InsetBuilder(dotIcon, Gravity.CENTER));
        return buildLayerDrawable(insetsBuilder, 2, 2);
    }

    /**
     * Clear the cache of drawable items.
     */
    public static void clearCachedItems() {
        synchronized (overlaysCache) {
            overlaysCache.clear();
        }
    }

    private static LayerDrawable buildLayerDrawable(final InsetsBuilder insetsBuilder, final int layersInitialCapacity, final int insetsInitialCapacity) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        final List<Drawable> layers = new ArrayList<>(layersInitialCapacity);
        final List<int[]> insets = new ArrayList<>(insetsInitialCapacity);

        insetsBuilder.build(layers, insets);
        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));

        int index = 0;
        for (final int[] temp : insets) {
            if (Build.VERSION.SDK_INT > 22) {
                if (temp[0] > 0) {
                    ld.setLayerSize(index, temp[0], temp[0]);
                }
                ld.setLayerGravity(index, temp[1]);
            } else {
                ld.setLayerInset(index, temp[0], temp[1], temp[2], temp[3]);
            }
            index++;
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
        return Settings.isBigSmileysEnabled() && showPin(cacheListType);
    }

    /**
     * adds list markers to drawable given by insetsBuilder
     */
    private static void addListMarkers(final Resources res, final InsetsBuilder insetsBuilder, final ArrayList<Integer> assignedMarkers) {
        if (assignedMarkers.size() > 0) {
            if (lPaint == null) {
                final int markerAvailable = DisplayUtils.getPxFromDp(res, SIZE_LIST_MARKER_DP, 1.2f);
                lPaint = new EmojiUtils.EmojiPaint(res, new Pair<>(markerAvailable, markerAvailable), markerAvailable, 0, DisplayUtils.calculateMaxFontsize(10, 5, 100, markerAvailable));
            }
            insetsBuilder.withInset(new InsetBuilder(EmojiUtils.getEmojiDrawable(lPaint, assignedMarkers.get(0)), Gravity.CENTER_VERTICAL | Gravity.LEFT));
            if (assignedMarkers.size() > 1) {
                insetsBuilder.withInset(new InsetBuilder(EmojiUtils.getEmojiDrawable(lPaint, assignedMarkers.get(1)), Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }
        }
    }

    /**
     * Conditional expression to choose if we need the pin on markers (on map).
     *
     * @param cacheListType
     *            The cache list currently used
     * @return
     *         True if the background circle should be displayed
     */
    private static boolean showPin(@Nullable final CacheListType cacheListType) {
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
