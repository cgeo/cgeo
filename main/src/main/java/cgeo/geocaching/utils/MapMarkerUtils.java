package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.INamedGeoCoordinate;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.service.CacheDownloaderService;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.builders.InsetBuilder;
import cgeo.geocaching.utils.builders.InsetsBuilder;
import static cgeo.geocaching.utils.DisplayUtils.SIZE_CACHE_MARKER_DP;
import static cgeo.geocaching.utils.DisplayUtils.SIZE_LIST_MARKER_DP;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class MapMarkerUtils {

    private static final String CACHE_WAYPOINT_HIGHLIGHTER_BACKGROUND = "cacheWaypointHighlighterBackground";
    private static final String CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM = "cacheWaypointHighlighterGeoitem";

    private static final Map<Integer, Integer> list2marker = new TreeMap<>();
    private static Boolean listsRead = false;

    // the following vars depend on cache/wp scaling factor and need to be part of resetCache()
    private static final SparseArray<CacheMarker> overlaysCache = new SparseArray<>();
    private static final Map<String, EmojiUtils.EmojiPaint> emojiPaintMap = new HashMap<>();
    private static float scalingFactorCacheIcons;
    private static float scalingFactorWpIcons;

    static {
        resetAllCaches();
    }

    private MapMarkerUtils() {
        // Do not instantiate
    }

    /**
     * clear all caches and reset scaling-related variables
     */
    public static synchronized void resetAllCaches() {
        overlaysCache.clear();
        emojiPaintMap.clear();
        scalingFactorCacheIcons = Settings.getInt(R.string.pref_mapCacheScaling, 100) / 100.0f;
        scalingFactorWpIcons = Settings.getInt(R.string.pref_mapWpScaling, 100) / 100.0f;
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
     * Obtain the drawable for a given cache.
     * Return a drawable from the cache, if a similar drawable was already generated.
     * <br>
     * cacheListType should be Null if the requesting activity is Map.
     * <br>
     * @param res           the resources to use
     * @param cache         the cache to build the drawable for
     * @param cacheListType the current CacheListType or Null
     * @return a drawable representing the current cache status
     */
    @NonNull
    public static CacheMarker getCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType, final boolean applyScaling) {
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
                .append(cache.hasUserModifiedCoords())
                .append(cache.hasFinalDefined())
                .append(cache.getPersonalNote())
                .append(cache.hasLogOffline())
                .append(cache.getLists().isEmpty())
                .append(cache.getOfflineLogType())
                .append(showPin(cacheListType))
                .append(showFloppyOverlay(cacheListType))
                .append(assignedMarkers)
                .append(Settings.isDTMarkerEnabled() ? cache.getTerrain() : false)
                .append(Settings.isDTMarkerEnabled() ? cache.getDifficulty() : false)
                .append(CacheDownloaderService.isDownloadPending(cache))
                .append(applyScaling)
                .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createCacheMarker(res, cache, cacheListType, assignedMarkers, applyScaling));
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
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private static LayerDrawable createCacheMarker(final Resources res, final Geocache cache, @Nullable final CacheListType cacheListType, final ArrayList<Integer> assignedMarkers, final boolean applyScaling) {
        final int useEmoji = cache.getAssignedEmoji();

        // marker shape
        final Drawable marker = new ScalableDrawable(ResourcesCompat.getDrawable(res, cache.getMapMarkerId(), null), getCacheScalingFactor(applyScaling));
        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, marker.getIntrinsicWidth(), marker.getIntrinsicHeight(), true);
        if (showPin(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_pin, getCacheScalingFactor(applyScaling)));
        }
        insetsBuilder.withInset(new InsetBuilder(marker));

        // marker foreground
        final int mainMarkerId = getMainMarkerId(cache, cacheListType);

        // Main icon
        if (showBigSmileys(cacheListType) && !mainIconIsTypeIcon(cache, cacheListType)) {
            // log icon in bigSmiley mode
            insetsBuilder.withInset(new InsetBuilder(mainMarkerId, Gravity.CENTER, getCacheScalingFactor(applyScaling), true));
        } else if (cache.getAssignedEmoji() != 0) {
            // custom icon
            insetsBuilder.withInset(new InsetBuilder(getScaledEmojiDrawable(res, useEmoji, "mainIconForCache", applyScaling), Gravity.CENTER));
        } else {
            // type icon
            // cache type background color
            final int tintColor = (cache.isArchived() || cache.isDisabled()) ? R.color.cacheType_disabled : cache.getType().typeColor;
            // make drawable mutatable, as setting tint will otherwise change the background for all markers (on Android 7-9)!
            final Drawable backgroundTemp = ViewUtils.getDrawable(cache.getMapMarkerBackgroundId(), true);
            DrawableCompat.setTint(backgroundTemp, ResourcesCompat.getColor(res, tintColor, null));
            insetsBuilder.withInset(new InsetBuilder(new ScalableDrawable(backgroundTemp, getCacheScalingFactor(applyScaling)), Gravity.CENTER));
            // main icon (type icon / custom cache icon)
            insetsBuilder.withInset(new InsetBuilder(mainMarkerId, Gravity.CENTER, getCacheScalingFactor(applyScaling)));
        }

        // overlays
        // center: archived
        if (cache.isArchived()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.type_overlay_archived, Gravity.CENTER, getCacheScalingFactor(applyScaling)));
        }
        // top-right: DT marker / sync / stored
        if (Settings.isDTMarkerEnabled()) {
            insetsBuilder.withInset(new InsetBuilder(getDTRatingMarker(res, cache.supportsDifficultyTerrain(), cache.getDifficulty(), cache.getTerrain(), applyScaling), Gravity.TOP | Gravity.RIGHT));
        } else if (CacheDownloaderService.isDownloadPending(cache)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_storing, Gravity.TOP | Gravity.RIGHT, getCacheScalingFactor(applyScaling)));
        } else if (!cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_stored, Gravity.TOP | Gravity.RIGHT, getCacheScalingFactor(applyScaling)));
        }
        // top-center: sync / stored (if DT marker enabled)
        if (Settings.isDTMarkerEnabled() && CacheDownloaderService.isDownloadPending(cache)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_storing, Gravity.TOP | Gravity.CENTER_HORIZONTAL, getCacheScalingFactor(applyScaling)));
        } else if (Settings.isDTMarkerEnabled() && !cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_stored, Gravity.TOP | Gravity.CENTER_HORIZONTAL, getCacheScalingFactor(applyScaling)));
        }
        // top-left: will attend / found / not found / offline-logs - if not logged and custom emoji assigned or bigSmileyMode - show icon
        if (!showBigSmileys(cacheListType)) {
            final Integer loggedMarkerId = getMarkerIdIfLogged(cache);
            if (loggedMarkerId != null) {
                insetsBuilder.withInset(new InsetBuilder(loggedMarkerId, Gravity.TOP | Gravity.LEFT, getCacheScalingFactor(applyScaling)));
            } else if (cache.getAssignedEmoji() != 0) {
                insetsBuilder.withInset(new InsetBuilder(getTypeMarker(res, cache, true, applyScaling, true), Gravity.TOP | Gravity.LEFT));
            }
        } else if (!mainIconIsTypeIcon(cache, cacheListType) || cache.getAssignedEmoji() != 0) {
            insetsBuilder.withInset(new InsetBuilder(getTypeMarker(res, cache, true, applyScaling, true), Gravity.TOP | Gravity.LEFT));
        }

        // bottom-right: user modified coords / final waypoint defined
        if (cache.hasUserModifiedCoords() && mainMarkerId != R.drawable.marker_usermodifiedcoords) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_usermodifiedcoords, Gravity.BOTTOM | Gravity.RIGHT, getCacheScalingFactor(applyScaling)));
        } else if (cache.hasFinalDefined() && !cache.hasUserModifiedCoords()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_hasfinal, Gravity.BOTTOM | Gravity.RIGHT, getCacheScalingFactor(applyScaling)));
        }
        // bottom-left: personal note
        if (cache.getPersonalNote() != null) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_personalnote, Gravity.BOTTOM | Gravity.LEFT, getCacheScalingFactor(applyScaling)));
        }
        // center-left/center-right: list markers
        addListMarkers(res, insetsBuilder, assignedMarkers, true, applyScaling);

        return buildLayerDrawable(insetsBuilder, 12, 13);
    }

    /**
     * Obtain the drawable for a given waypoint.
     * Return a drawable from the cache, if a similar drawable was already generated.
     *
     * @param res      the resources to use
     * @param waypoint the waypoint to build the drawable for
     * @return a drawable representing the current waypoint status
     */
    @NonNull
    public static CacheMarker getWaypointMarker(final Resources res, final Waypoint waypoint, final boolean showPin, final boolean applyScaling) {
        final WaypointType waypointType = waypoint.getWaypointType();
        final String id = null == waypointType ? WaypointType.WAYPOINT.id : waypointType.id;

        final HashCodeBuilder hcb = new HashCodeBuilder()
                .append(waypoint.isVisited())
                .append(id)
                .append(waypoint.getMapMarkerId())
                .append(showPin)
                .append(applyScaling);
        final Geocache cache = waypoint.getParentGeocache();
        if (null != cache) {
            hcb.append(getAssignedMarkers(cache))
                    .append(getMarkerIdIfLogged(cache))
                    .append(cache.isDisabled())
                    .append(cache.isArchived())
                    .append(cache.isLinearAlc() ? waypoint.getPrefix() : false)
                    .append(cache.getAssignedEmoji())
                    .append(cache.getType())
                    .append(cache.isFound());
        }
        final int hashcode = hcb.toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createWaypointMarker(res, waypoint, cache, showPin, applyScaling));
                overlaysCache.put(hashcode, marker);
            }
            return marker;
        }
    }

    /**
     * Build the drawable for a given waypoint.
     *
     * @param res      the resources to use
     * @param waypoint the waypoint to build the drawable for
     * @return a drawable representing the current waypoint status
     */
    @NonNull
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private static LayerDrawable createWaypointMarker(final Resources res, final Waypoint waypoint, @Nullable final Geocache cache, final boolean forMap, final boolean applyScaling) {
        final WaypointType waypointType = waypoint.getWaypointType();

        final Drawable marker = new ScalableDrawable(ResourcesCompat.getDrawable(res, waypoint.getMapMarkerId(), null), getWaypointScalingFactor(applyScaling));
        final int size = marker.getIntrinsicWidth();

        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, size, size, true);
        if (forMap) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_pin, getWaypointScalingFactor(applyScaling)));
        }
        insetsBuilder.withInset(new InsetBuilder(marker));

        if (cache != null && cache.isLinearAlc()) {
            try {
                insetsBuilder.withInset(new InsetBuilder(getStageNumberMarker(res, Integer.parseInt(waypoint.getPrefix()), getWaypointScalingFactor(applyScaling)), Gravity.CENTER));
            } catch (NumberFormatException ignore) {
                insetsBuilder.withInset(new InsetBuilder(new ScalableDrawable(ViewUtils.getDrawable(waypointType.markerId, true), getWaypointScalingFactor(applyScaling)), Gravity.CENTER));
            }
        } else {
            // make drawable mutatable before setting a tint, as otherwise it will change the background for all markers (on Android 7-9)!
            final Drawable waypointTypeIcon = ViewUtils.getDrawable(waypointType.markerId, true);
            if (cache != null && (cache.isDisabled() || cache.isArchived())) {
                DrawableCompat.setTint(waypointTypeIcon, ResourcesCompat.getColor(res, R.color.cacheType_disabled, null));
            }
            insetsBuilder.withInset(new InsetBuilder(new ScalableDrawable(waypointTypeIcon, getWaypointScalingFactor(applyScaling)), Gravity.CENTER));
        }

        if (cache != null && cache.isArchived()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.type_overlay_archived, Gravity.CENTER, getWaypointScalingFactor(applyScaling)));
        }
        // bottom-right: visited
        if (waypoint.isVisited()) {
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_visited, Gravity.BOTTOM | Gravity.RIGHT, getWaypointScalingFactor(applyScaling)));
        }

        // top-left: emoji
        if (cache != null) {
            final Integer logMarker = getMarkerIdIfLogged(cache);
            if (forMap && logMarker != null) {
                insetsBuilder.withInset(new InsetBuilder(logMarker, Gravity.TOP | Gravity.LEFT));
            } else if (forMap && cache.getAssignedEmoji() != 0) {
                insetsBuilder.withInset(new InsetBuilder(getEmojiMarker(res, cache.getAssignedEmoji(), applyScaling), Gravity.TOP | Gravity.LEFT));
            } else if (forMap) {
                insetsBuilder.withInset(new InsetBuilder(getTypeMarker(res, cache, true, applyScaling, false), Gravity.TOP | Gravity.LEFT));
            }
            addListMarkers(res, insetsBuilder, getAssignedMarkers(cache), false, applyScaling);
        }
        final LayerDrawable ld = buildLayerDrawable(insetsBuilder, 8, 8);
        if ((waypoint.isVisited() || (cache != null && cache.isFound())) && Settings.getVisitedWaypointsSemiTransparent()) {
            ld.setAlpha(144);
        }
        return ld;
    }

    /**
     * Obtain the drawable for a given cache.
     * Return a drawable from the cache, if a similar drawable was already generated.
     * <br>
     * cacheListType should be Null if the requesting activity is Map.
     *
     * @param res   the resources to use
     * @param cache the cache to build the drawable for
     * @return a drawable representing the current cache status
     */
    @NonNull
    public static CacheMarker getCacheDotMarker(final Resources res, final Geocache cache) {
        final int hashcode = new HashCodeBuilder()
                .append(cache.getType().typeColor)
                .append(cache.getMapDotMarkerId())
                .append(cache.isFound())
                .append(cache.isDisabled())
                .append(cache.isArchived())
                .append(cache.hasLogOffline())
                .append(cache.getOfflineLogType())
                .append(cache.hasUserModifiedCoords())
                .append(cache.hasFinalDefined())
                .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createCacheDotMarker(res, cache));
                overlaysCache.put(hashcode, marker);
            }
            return marker;
        }
    }

    /**
     * Build the drawable for a given cache.
     *
     * @param res   the resources to use
     * @param cache the cache to build the drawable for
     * @return a drawable representing the current cache status
     */
    @NonNull
    private static LayerDrawable createCacheDotMarker(final Resources res, final Geocache cache) {
        int dotIcon = -1;
        int tintColor;

        // Background color: Cache type color / disabled
        tintColor = cache.getType().typeColor;
        if (cache.isArchived() || cache.isDisabled()) {
            tintColor = R.color.cacheType_disabled;
        }

        // Overlay icon: 1. Found, 2. Offline Log, 3. Modified Coordinates, 4. Has Final Waypoint
        if (cache.isFound()) {
            dotIcon = R.drawable.dot_found;
            tintColor = R.color.dotBg_found;
        } else if (cache.hasLogOffline()) {
            final LogType offlineLogType = cache.getOfflineLogType();
            // logs of type NOTE may have a NA/NM log attached to them
            if (offlineLogType.isFoundLog()) {
                dotIcon = R.drawable.dot_found;
                tintColor = R.color.dotBg_foundOffline;
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
        } else if (cache.hasFinalDefined()) {
            dotIcon = R.drawable.dot_marker_hasfinal;
        }

        final Drawable dotMarker = ResourcesCompat.getDrawable(res, cache.getMapDotMarkerId(), null);
        // make drawable mutatable, as setting tint will otherwise change the background for all markers (on Android 7-9)!
        final Drawable dotBackground = ViewUtils.getDrawable(cache.getMapDotMarkerBackgroundId(), true);
        DrawableCompat.setTint(dotBackground, ResourcesCompat.getColor(res, tintColor, null));

        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, dotMarker.getIntrinsicWidth(), dotMarker.getIntrinsicHeight(), true);
        insetsBuilder.withInset(new InsetBuilder(dotMarker));
        insetsBuilder.withInset(new InsetBuilder(dotBackground, Gravity.CENTER));
        if (dotIcon != -1) {
            insetsBuilder.withInset(new InsetBuilder(dotIcon, Gravity.CENTER));
        }
        return buildLayerDrawable(insetsBuilder, 3, 3);
    }

    /**
     * Obtain the drawable for a given waypoint.
     * Return a drawable from the cache, if a similar drawable was already generated.
     *
     * @param res      the resources to use
     * @param waypoint the waypoint to build the drawable for
     * @return a drawable representing the current waypoint status
     */
    @NonNull
    public static CacheMarker getWaypointDotMarker(final Resources res, final Waypoint waypoint) {
        boolean cacheIsDisabled = false;
        boolean cacheIsArchived = false;
        final String geocode = waypoint.getGeocode();
        if (StringUtils.isNotBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (null != cache) {
                cacheIsDisabled = cache.isDisabled();
                cacheIsArchived = cache.isArchived();
            }
        }
        final int hashcode = new HashCodeBuilder()
                .append(waypoint.getMapDotMarkerId())
                .append(waypoint.getWaypointType())
                .append(cacheIsDisabled)
                .append(cacheIsArchived)
                .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createWaypointDotMarker(res, waypoint));
                overlaysCache.put(hashcode, marker);
            }
            return marker;
        }
    }

    /**
     * Build the drawable for a given waypoint.
     *
     * @param res      the resources to use
     * @param waypoint the waypoint to build the drawable for
     * @return a drawable representing the current waypoint status
     */
    @NonNull
    private static LayerDrawable createWaypointDotMarker(final Resources res, final Waypoint waypoint) {
        final Drawable dotMarker = ViewUtils.getDrawable(waypoint.getMapDotMarkerId(), true);
        DrawableCompat.setTint(dotMarker, ResourcesCompat.getColor(res, R.color.dotBg_waypointOutline, null));
        final Drawable dotBackground = ViewUtils.getDrawable(waypoint.getMapDotMarkerBackgroundId(), true);
        DrawableCompat.setTint(dotBackground, ResourcesCompat.getColor(res, R.color.dotBg_waypointBg, null));

        final Drawable dotIcon = ViewUtils.getDrawable(waypoint.getWaypointType().dotMarkerId, true);

        // Tint disabled waypoints
        final String geocode = waypoint.getGeocode();
        if (StringUtils.isNotBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (cache != null && (cache.isDisabled() || cache.isArchived())) {
                DrawableCompat.setTint(dotIcon, ResourcesCompat.getColor(res, R.color.cacheType_disabled, null));
            }
        }

        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, dotMarker.getIntrinsicWidth(), dotMarker.getIntrinsicHeight(), true);
        insetsBuilder.withInset(new InsetBuilder(dotMarker));
        insetsBuilder.withInset(new InsetBuilder(dotBackground, Gravity.CENTER));
        insetsBuilder.withInset(new InsetBuilder(dotIcon, Gravity.CENTER));
        return buildLayerDrawable(insetsBuilder, 3, 3);
    }

    /**
     * Create a cache from a cache type to select the proper background shape
     *
     * @param res  Android Resources
     * @param type CacheType to get the icon for
     * @return Layered Drawable
     */
    public static Drawable getCacheTypeMarker(final Resources res, final CacheType type) {
        final Geocache tempCache = new Geocache();
        tempCache.setType(type);
        // user-defined should always use the hexagonal icon
        tempCache.setGeocode(type == CacheType.USER_DEFINED ? "ZZ1" : "GC1");
        return getTypeMarker(res, tempCache, false, false, true);
    }

    /**
     * Create a waypoint marker without background - basically the zoomed in waypoint icon
     *
     * @param res      Android Resources
     * @param waypoint Waypoint to get the icon for
     * @return Layered Drawable
     */
    public static Drawable getWaypointTypeMarker(final Resources res, final WaypointType waypoint) {
        final int hashcode = new HashCodeBuilder().append(waypoint.markerId).toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createWaypointTypeMarker(res, waypoint));
                overlaysCache.put(hashcode, marker);
            }
            return marker.getDrawable();
        }
    }

    /**
     * Build the layered drawable for a waypoint marker without background - basically the zoomed in waypoint icon
     *
     * @param res      Android Resources
     * @param waypoint Waypoint to get the icon for
     * @return Layered Drawable
     */
    private static Drawable createWaypointTypeMarker(final Resources res, final WaypointType waypoint) {
        final Drawable waypointMarker = ViewUtils.getDrawable(waypoint.markerId, true);
        final LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{waypointMarker});

        // "zoom" into the cache icon by setting negative offsets to hide the empty space (drawable is 36dp but icon only 17,25dp). Drawable must be square!
        final int diffWidth = waypointMarker.getIntrinsicWidth() - DisplayUtils.getPxFromDp(res, 19f, 1);
        final int offsetLeftTop = diffWidth - diffWidth / 2;
        final int offsetRightBottom = diffWidth - offsetLeftTop;
        layerDrawable.setLayerInset(0, -offsetLeftTop, -offsetLeftTop, -offsetRightBottom, -offsetRightBottom);
        return layerDrawable;
    }

    private static LayerDrawable buildLayerDrawable(final InsetsBuilder insetsBuilder, final int layersInitialCapacity, final int insetsInitialCapacity) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        final List<Drawable> layers = new ArrayList<>(layersInitialCapacity);
        final List<int[]> insets = new ArrayList<>(insetsInitialCapacity);

        insetsBuilder.build(layers, insets);
        final LayerDrawable ld = new LayerDrawable(layers.toArray(new Drawable[0]));

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
            } else if (cache.hasUserModifiedCoords()) {
                return R.drawable.marker_usermodifiedcoords;
            }
        }
        return cache.getType().iconId;
    }

    @Nullable
    private static Integer getMarkerIdIfLogged(final Geocache cache) {
        if (cache.isOwner() && !cache.hasLogOffline()) {
            return R.drawable.marker_own;
        } else if (cache.isFound()) {
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
    private static void addListMarkers(final Resources res, final InsetsBuilder insetsBuilder, final ArrayList<Integer> assignedMarkers, final boolean forCaches, final boolean applyScaling) {
        if (!assignedMarkers.isEmpty()) {
            insetsBuilder.withInset(new InsetBuilder(getScaledEmojiDrawable(res, assignedMarkers.get(0), forCaches ? "listMarkerForCache" : "listMarkerForWaypoint", applyScaling), Gravity.CENTER_VERTICAL | Gravity.LEFT));
            if (assignedMarkers.size() > 1) {
                insetsBuilder.withInset(new InsetBuilder(getScaledEmojiDrawable(res, assignedMarkers.get(1), forCaches ? "listMarkerForCache" : "listMarkerForWaypoint", applyScaling), Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }
        }
    }

    /**
     * Conditional expression to choose if we need the pin on markers (on map).
     *
     * @param cacheListType The cache list currently used
     * @return True if the background circle should be displayed
     */
    private static boolean showPin(@Nullable final CacheListType cacheListType) {
        return cacheListType == null;
    }

    /**
     * Conditional expression to choose if we need the floppy overlay or not.
     *
     * @param cacheListType The cache list currently used
     * @return True if the floppy overlay should be displayed
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

    private static ArrayList<Integer> getAssignedMarkers(final Geocache cache) {
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

    private static Drawable getDTRatingMarker(final Resources res, final boolean supportsRating, final float difficulty, final float terrain, final boolean applyScaling) {
        final int hashcode = new HashCodeBuilder().append(difficulty + "" + terrain).append(applyScaling).append(supportsRating).toHashCode(); // due to -1*-1 being the same as 1*1 this needs to be a string

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createDTRatingMarker(res, supportsRating, difficulty, terrain, applyScaling));
                overlaysCache.put(hashcode, marker);
            }
            return marker.getDrawable();
        }
    }

    /**
     * Create a LayerDrawable showing the caches difficulty and terrain rating. If the connector doesn't support D/T show a "-" instead, if the info is missing (not loaded) a "?"
     * @param res           Resources bundle
     * @param difficulty    Difficulty rating
     * @param terrain       Terrain rating
     * @return              LayerDrawable composed of round background and foreground showing the ratings
     */
    private static LayerDrawable createDTRatingMarker(final Resources res, final boolean supportsRating, final float difficulty, final float terrain, final boolean applyScaling) {
        return createDTRatingMarker(res, supportsRating, difficulty, terrain, getCacheScalingFactor(applyScaling));
    }

    public static LayerDrawable createDTRatingMarker(final Resources res, final boolean supportsRating, final float difficulty, final float terrain, final float scaling) {
        final Drawable background = new ScalableDrawable(ViewUtils.getDrawable(R.drawable.marker_empty, true), scaling);
        final InsetsBuilder insetsBuilder = new InsetsBuilder(res, background.getIntrinsicWidth(), background.getIntrinsicHeight(), true);
        insetsBuilder.withInset(new InsetBuilder(background));
        int layers = 4;

        if (!supportsRating) {
            layers = 2;
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_rating_notsupported, scaling));
        } else if (difficulty < 0.5 && terrain < 0.5) {
            layers = 2;
            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_rating_notavailable, scaling));
        } else {
            final String packageName = CgeoApplication.getInstance().getPackageName();
            insetsBuilder.withInset(new InsetBuilder(getDTRatingMarkerSection(res, packageName, "d", difficulty, scaling)));
            insetsBuilder.withInset(new InsetBuilder(getDTRatingMarkerSection(res, packageName, "t", terrain, scaling)));

            insetsBuilder.withInset(new InsetBuilder(R.drawable.marker_rating_fg, scaling));
        }

        return buildLayerDrawable(insetsBuilder, layers, 0);
    }

    @SuppressWarnings("DiscouragedApi")
    private static Drawable getDTRatingMarkerSection(final Resources res, final String packageName, final String ratingLetter, final float rating, final float scaling) {
        // ensure that rating is an integer between 0 and 50 in steps of 5
        final int r = Math.max(0, Math.min(Math.round(rating * 2) * 5, 50));
        return new ScalableDrawable(ResourcesCompat.getDrawable(res, res.getIdentifier("marker_rating_" + ratingLetter + "_" + r, "drawable", packageName), null), scaling);
    }

    @SuppressWarnings("DiscouragedApi")
    private static Drawable getStageNumberMarker(final Resources res, final int stageNum, final float scaling) {
        int counter = stageNum;
        while (counter > 10) {
            counter = counter - 10;
        }
        final String packageName = CgeoApplication.getInstance().getPackageName();
        return new ScalableDrawable(ResourcesCompat.getDrawable(res, res.getIdentifier("marker_stagenum_" + counter, "drawable", packageName), null), scaling);
    }

    private static BitmapDrawable getScaledEmojiDrawable(final Resources res, final int emoji, final String wantedSize, final boolean applyScaling) {
        final EmojiUtils.EmojiPaint paint;
        if (emojiPaintMap.containsKey(wantedSize + applyScaling)) {
            paint = emojiPaintMap.get(wantedSize + applyScaling);
        } else {
            final float scalingFactor;
            final float size;
            switch (wantedSize) {
                case "listMarkerForCache":
                    scalingFactor = 1.2f * getCacheScalingFactor(applyScaling);
                    size = SIZE_LIST_MARKER_DP;
                    break;
                case "listMarkerForWaypoint":
                    scalingFactor = 1.2f * getWaypointScalingFactor(applyScaling);
                    size = SIZE_LIST_MARKER_DP;
                    break;
                case "iconMarkerForWaypoint":
                    scalingFactor = 0.6f * getWaypointScalingFactor(applyScaling);
                    size = SIZE_LIST_MARKER_DP;
                    break;
                case "iconMarkerForCache":
                    scalingFactor = 0.6f * getCacheScalingFactor(applyScaling);
                    size = SIZE_LIST_MARKER_DP;
                    break;
                case "mainIconForCache":
                    scalingFactor = (float) (Math.sqrt(0.5) * 1.15 * getCacheScalingFactor(applyScaling));
                    size = SIZE_CACHE_MARKER_DP;
                    break;
                case "mainIconForWaypoint":
                    scalingFactor = (float) (Math.sqrt(0.5) * 1.15 * getWaypointScalingFactor(applyScaling));
                    size = SIZE_CACHE_MARKER_DP;
                    break;
                default:
                    scalingFactor = 1.2f * getCacheScalingFactor(applyScaling);
                    size = SIZE_CACHE_MARKER_DP;
            }

            final int availableSize = DisplayUtils.getPxFromDp(res, size, scalingFactor);
            paint = new EmojiUtils.EmojiPaint(res, new Pair<>(availableSize, availableSize), availableSize, 0, DisplayUtils.calculateMaxFontsize(10, 1, 1000, availableSize));
            emojiPaintMap.put(wantedSize + applyScaling, paint);
        }
        return EmojiUtils.getEmojiDrawable(paint, emoji);
    }

    private static float getCacheScalingFactor(final boolean applyScaling) {
        return applyScaling ? scalingFactorCacheIcons : 1;
    }

    private static float getWaypointScalingFactor(final boolean applyScaling) {
        return applyScaling ? scalingFactorWpIcons : 1;
    }

    private static boolean mainIconIsTypeIcon(final Geocache cache, final CacheListType cacheListType) {
        final int mainMarkerId = getMainMarkerId(cache, cacheListType);
        return mainMarkerId == cache.getType().iconId;
    }

    private static Drawable getEmojiMarker(final Resources res, final int emoji, final boolean applyScaling) {
        final Drawable markerBg = new ScalableDrawable(ViewUtils.getDrawable(R.drawable.marker_empty, true), getWaypointScalingFactor(applyScaling));
        final InsetsBuilder markerBuilder = new InsetsBuilder(res, markerBg.getIntrinsicWidth(), markerBg.getIntrinsicHeight(), true);
        markerBuilder.withInset(new InsetBuilder(markerBg));
        markerBuilder.withInset(new InsetBuilder(getScaledEmojiDrawable(res, emoji, "iconMarkerForWaypoint", applyScaling)));
        return buildLayerDrawable(markerBuilder, 2, 2);
    }

    /**
     * Provide the LayerDrawable representing the cache type icon
     *
     * @param res   Android Resources
     * @param cache Geocache to get the icon for
     * @param withBorder    Draw a round border around the icon (for waypoint markers)
     * @param applyScaling  S
     * @return Layered Drawable
     */
    public static Drawable getTypeMarker(final Resources res, @NonNull final Geocache cache, final boolean withBorder, final boolean applyScaling, final boolean forCache) {
        final int hashcode = new HashCodeBuilder()
                .append("typeMarker")
                .append(cache.getType().id)
                .append(cache.isDisabled())
                .append(cache.isArchived())
                .append(withBorder)
                .append(forCache)
                .append(applyScaling)
                .toHashCode();

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode);
            if (marker == null) {
                marker = new CacheMarker(hashcode, createTypeMarker(res, cache, withBorder, applyScaling, forCache));
                overlaysCache.put(hashcode, marker);
            }
            return marker.getDrawable();
        }
    }

    public static Drawable getTypeMarker(final Resources res, final Geocache cache) {
        return getTypeMarker(res, cache, false, false, false);
    }

    /**
     * Create the LayerDrawable representing the cache type icon
     *
     * @param res   Android Resources
     * @param cache Geocache to get the icon for
     * @param withBorder    Draw a round border around the icon (for waypoint markers)
     * @param applyScaling  S
     * @return Layered Drawable
     */
    private static Drawable createTypeMarker(final Resources res, final Geocache cache, final boolean withBorder, final boolean applyScaling, final boolean scaleForCache) {
        final float scalingFactor;
        if (scaleForCache) {
            scalingFactor = getCacheScalingFactor(applyScaling);
        } else {
            scalingFactor = getWaypointScalingFactor(applyScaling);
        }
        final Drawable markerBg;
        if (withBorder) {
            markerBg = ViewUtils.getDrawable(R.drawable.marker_empty, scalingFactor, true);
        } else {
            markerBg = ViewUtils.getDrawable(R.drawable.marker_background, scalingFactor, true);
        }
        final InsetsBuilder markerBuilder = new InsetsBuilder(res, markerBg.getIntrinsicWidth(), markerBg.getIntrinsicHeight(), true);
        markerBuilder.withInset(new InsetBuilder(markerBg));
        // cache type background color
        final int tintColor = (cache.isArchived() || cache.isDisabled()) ? R.color.cacheType_disabled : cache.getType().typeColor;
        final Drawable backgroundTemp;
        // special case for drawing the userdefined type icon in filter dialog
        if (!"ZZ1".equals(cache.getGeocode())) {
            backgroundTemp = ViewUtils.getDrawable(R.drawable.marker_background, true);
        } else {
            backgroundTemp = ViewUtils.getDrawable(R.drawable.dot_marker_other, true);
        }
        DrawableCompat.setTint(backgroundTemp, ResourcesCompat.getColor(res, tintColor, null));
        markerBuilder.withInset(new InsetBuilder(new ScalableDrawable(backgroundTemp, scalingFactor), Gravity.CENTER));
        markerBuilder.withInset(new InsetBuilder(cache.getType().markerId, Gravity.CENTER, scalingFactor));
        return buildLayerDrawable(markerBuilder, 3, 3);
    }

    // ------------------------------------------------------------------------
    // methods for highlighting selected cache on map (UnifiedMap)

    public static void addHighlighting(final INamedGeoCoordinate geoitem, final Resources res, final GeoItemLayer<String> nonClickableItemsLayer) {
        Bitmap b1 = null;
        float scalingFactor = 100f;
        if (geoitem instanceof Geocache) {
            b1 = MapMarkerUtils.getCacheMarker(res, (Geocache) geoitem, null, true).getBitmap();
            scalingFactor = scalingFactorCacheIcons;
        } else if (geoitem instanceof Waypoint) {
            b1 = MapMarkerUtils.getWaypointMarker(res, (Waypoint) geoitem, true, true).getBitmap();
            scalingFactor = scalingFactorWpIcons;
        }
        if (b1 != null) {
            final Bitmap b = ViewUtils.drawableToBitmap(new ScalableDrawable(ResourcesCompat.getDrawable(res, R.drawable.background_gc_hightlighted, null), scalingFactor));
            final GeoPrimitive gp = GeoPrimitive.createMarker(geoitem.getCoords(), GeoIcon.builder().setBitmap(b).setHotspot(GeoIcon.Hotspot.BOTTOM_CENTER).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_CACHE_WAYPOINT_HIGHLIGHTER_MARKER).build();
            nonClickableItemsLayer.put(CACHE_WAYPOINT_HIGHLIGHTER_BACKGROUND, gp);
            final GeoPrimitive gp1 = GeoPrimitive.createMarker(geoitem.getCoords(), GeoIcon.builder().setBitmap(b1).setHotspot(GeoIcon.Hotspot.BOTTOM_CENTER).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM).build();
            nonClickableItemsLayer.put(CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM, gp1);
        }
    }

    public static void removeHighlighting(final GeoItemLayer<String> nonClickableItemsLayer) {
        nonClickableItemsLayer.remove(CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM);
        nonClickableItemsLayer.remove(CACHE_WAYPOINT_HIGHLIGHTER_BACKGROUND);
    }

}
