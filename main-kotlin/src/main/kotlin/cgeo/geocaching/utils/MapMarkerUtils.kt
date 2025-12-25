// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.log.ReportProblemType
import cgeo.geocaching.maps.CacheMarker
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.INamedGeoCoordinate
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.geoitem.GeoIcon
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.service.CacheDownloaderService
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.unifiedmap.LayerHelper
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer
import cgeo.geocaching.utils.builders.InsetBuilder
import cgeo.geocaching.utils.builders.InsetsBuilder
import cgeo.geocaching.utils.DisplayUtils.SIZE_CACHE_MARKER_DP
import cgeo.geocaching.utils.DisplayUtils.SIZE_LIST_MARKER_DP

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.util.Pair
import android.util.SparseArray
import android.view.Gravity

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat

import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.Set
import java.util.TreeMap

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder

class MapMarkerUtils {

    private static val CACHE_WAYPOINT_HIGHLIGHTER_BACKGROUND: String = "cacheWaypointHighlighterBackground"
    private static val CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM: String = "cacheWaypointHighlighterGeoitem"

    private static val list2marker: Map<Integer, Integer> = TreeMap<>()
    private static Boolean listsRead = false

    // the following vars depend on cache/wp scaling factor and need to be part of resetCache()
    private static val overlaysCache: SparseArray<CacheMarker> = SparseArray<>()
    private static val emojiPaintMap: Map<String, EmojiUtils.EmojiPaint> = HashMap<>()
    private static Float scalingFactorCacheIcons
    private static Float scalingFactorWpIcons

    static {
        resetAllCaches()
    }

    private MapMarkerUtils() {
        // Do not instantiate
    }

    /**
     * clear all caches and reset scaling-related variables
     */
    public static synchronized Unit resetAllCaches() {
        overlaysCache.clear()
        emojiPaintMap.clear()
        scalingFactorCacheIcons = Settings.getInt(R.string.pref_mapCacheScaling, 100) / 100.0f
        scalingFactorWpIcons = Settings.getInt(R.string.pref_mapWpScaling, 100) / 100.0f
    }

    /**
     * Clear the cache of drawable items.
     */
    public static Unit clearCachedItems() {
        synchronized (overlaysCache) {
            overlaysCache.clear()
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
    public static CacheMarker getCacheMarker(final Resources res, final Geocache cache, final CacheListType cacheListType, final Boolean applyScaling) {
        val assignedMarkers: ArrayList<Integer> = getAssignedMarkers(cache)
        val hashcode: Int = HashCodeBuilder()
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
                .toHashCode()

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode)
            if (marker == null) {
                marker = CacheMarker(hashcode, createCacheMarker(res, cache, cacheListType, assignedMarkers, applyScaling))
                overlaysCache.put(hashcode, marker)
            }
            return marker
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
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private static LayerDrawable createCacheMarker(final Resources res, final Geocache cache, final CacheListType cacheListType, final ArrayList<Integer> assignedMarkers, final Boolean applyScaling) {
        val useEmoji: Int = cache.getAssignedEmoji()

        // marker shape
        val marker: Drawable = ScalableDrawable(ResourcesCompat.getDrawable(res, cache.getMapMarkerId(), null), getCacheScalingFactor(applyScaling))
        val insetsBuilder: InsetsBuilder = InsetsBuilder(res, marker.getIntrinsicWidth(), marker.getIntrinsicHeight(), true)
        if (showPin(cacheListType)) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_pin, getCacheScalingFactor(applyScaling)))
        }
        insetsBuilder.withInset(InsetBuilder(marker))

        // marker foreground
        val mainMarkerId: Int = getMainMarkerId(cache, cacheListType)

        // Main icon
        if (showBigSmileys(cacheListType) && !mainIconIsTypeIcon(cache, cacheListType)) {
            // log icon in bigSmiley mode
            insetsBuilder.withInset(InsetBuilder(mainMarkerId, Gravity.CENTER, getCacheScalingFactor(applyScaling), true))
        } else if (cache.getAssignedEmoji() != 0) {
            // custom icon
            insetsBuilder.withInset(InsetBuilder(getScaledEmojiDrawable(res, useEmoji, "mainIconForCache", applyScaling), Gravity.CENTER))
        } else {
            // type icon
            // cache type background color
            val tintColor: Int = (cache.isArchived() || cache.isDisabled()) ? R.color.cacheType_disabled : cache.getType().typeColor
            // make drawable mutatable, as setting tint will otherwise change the background for all markers (on Android 7-9)!
            val backgroundTemp: Drawable = ViewUtils.getDrawable(cache.getMapMarkerBackgroundId(), true)
            DrawableCompat.setTint(backgroundTemp, ResourcesCompat.getColor(res, tintColor, null))
            insetsBuilder.withInset(InsetBuilder(ScalableDrawable(backgroundTemp, getCacheScalingFactor(applyScaling)), Gravity.CENTER))
            // main icon (type icon / custom cache icon)
            insetsBuilder.withInset(InsetBuilder(mainMarkerId, Gravity.CENTER, getCacheScalingFactor(applyScaling)))
        }

        // overlays
        // center: archived
        if (cache.isArchived()) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.type_overlay_archived, Gravity.CENTER, getCacheScalingFactor(applyScaling)))
        }
        // top-right: DT marker / sync / stored
        if (Settings.isDTMarkerEnabled()) {
            insetsBuilder.withInset(InsetBuilder(getDTRatingMarker(res, cache.supportsDifficultyTerrain(), cache.getDifficulty(), cache.getTerrain(), applyScaling), Gravity.TOP | Gravity.RIGHT))
        } else if (CacheDownloaderService.isDownloadPending(cache)) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_storing, Gravity.TOP | Gravity.RIGHT, getCacheScalingFactor(applyScaling)))
        } else if (!cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_stored, Gravity.TOP | Gravity.RIGHT, getCacheScalingFactor(applyScaling)))
        }
        // top-center: sync / stored (if DT marker enabled)
        if (Settings.isDTMarkerEnabled() && CacheDownloaderService.isDownloadPending(cache)) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_storing, Gravity.TOP | Gravity.CENTER_HORIZONTAL, getCacheScalingFactor(applyScaling)))
        } else if (Settings.isDTMarkerEnabled() && !cache.getLists().isEmpty() && showFloppyOverlay(cacheListType)) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_stored, Gravity.TOP | Gravity.CENTER_HORIZONTAL, getCacheScalingFactor(applyScaling)))
        }
        // top-left: will attend / found / not found / offline-logs - if not logged and custom emoji assigned or bigSmileyMode - show icon
        if (!showBigSmileys(cacheListType)) {
            val loggedMarkerId: Integer = getMarkerIdIfLogged(cache)
            if (loggedMarkerId != null) {
                insetsBuilder.withInset(InsetBuilder(loggedMarkerId, Gravity.TOP | Gravity.LEFT, getCacheScalingFactor(applyScaling)))
            } else if (cache.getAssignedEmoji() != 0) {
                insetsBuilder.withInset(InsetBuilder(getTypeMarker(res, cache, true, applyScaling, true), Gravity.TOP | Gravity.LEFT))
            }
        } else if (!mainIconIsTypeIcon(cache, cacheListType) || cache.getAssignedEmoji() != 0) {
            insetsBuilder.withInset(InsetBuilder(getTypeMarker(res, cache, true, applyScaling, true), Gravity.TOP | Gravity.LEFT))
        }

        // bottom-right: user modified coords / final waypoint defined
        if (cache.hasUserModifiedCoords() && mainMarkerId != R.drawable.marker_usermodifiedcoords) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_usermodifiedcoords, Gravity.BOTTOM | Gravity.RIGHT, getCacheScalingFactor(applyScaling)))
        } else if (cache.hasFinalDefined() && !cache.hasUserModifiedCoords()) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_hasfinal, Gravity.BOTTOM | Gravity.RIGHT, getCacheScalingFactor(applyScaling)))
        }
        // bottom-left: personal note
        if (cache.getPersonalNote() != null) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_personalnote, Gravity.BOTTOM | Gravity.LEFT, getCacheScalingFactor(applyScaling)))
        }
        // center-left/center-right: list markers
        addListMarkers(res, insetsBuilder, assignedMarkers, true, applyScaling)

        return buildLayerDrawable(insetsBuilder, 12, 13)
    }

    /**
     * Obtain the drawable for a given waypoint.
     * Return a drawable from the cache, if a similar drawable was already generated.
     *
     * @param res      the resources to use
     * @param waypoint the waypoint to build the drawable for
     * @return a drawable representing the current waypoint status
     */
    public static CacheMarker getWaypointMarker(final Resources res, final Waypoint waypoint, final Boolean showPin, final Boolean applyScaling) {
        val waypointType: WaypointType = waypoint.getWaypointType()
        val id: String = null == waypointType ? WaypointType.WAYPOINT.id : waypointType.id

        val hcb: HashCodeBuilder = HashCodeBuilder()
                .append(waypoint.isVisited())
                .append(id)
                .append(waypoint.getMapMarkerId())
                .append(showPin)
                .append(applyScaling)
        val cache: Geocache = waypoint.getParentGeocache()
        if (null != cache) {
            hcb.append(getAssignedMarkers(cache))
                    .append(getMarkerIdIfLogged(cache))
                    .append(cache.isDisabled())
                    .append(cache.isArchived())
                    .append(cache.isLinearAlc() ? waypoint.getPrefix() : false)
                    .append(cache.getAssignedEmoji())
                    .append(cache.getType())
                    .append(cache.isFound())
        }
        val hashcode: Int = hcb.toHashCode()

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode)
            if (marker == null) {
                marker = CacheMarker(hashcode, createWaypointMarker(res, waypoint, cache, showPin, applyScaling))
                overlaysCache.put(hashcode, marker)
            }
            return marker
        }
    }

    /**
     * Build the drawable for a given waypoint.
     *
     * @param res      the resources to use
     * @param waypoint the waypoint to build the drawable for
     * @return a drawable representing the current waypoint status
     */
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private static LayerDrawable createWaypointMarker(final Resources res, final Waypoint waypoint, final Geocache cache, final Boolean forMap, final Boolean applyScaling) {
        val waypointType: WaypointType = waypoint.getWaypointType()

        val marker: Drawable = ScalableDrawable(ResourcesCompat.getDrawable(res, waypoint.getMapMarkerId(), null), getWaypointScalingFactor(applyScaling))
        val size: Int = marker.getIntrinsicWidth()

        val insetsBuilder: InsetsBuilder = InsetsBuilder(res, size, size, true)
        if (forMap) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_pin, getWaypointScalingFactor(applyScaling)))
        }
        insetsBuilder.withInset(InsetBuilder(marker))

        Int stagenum = 0
        if (cache != null && cache.isLinearAlc()) {
            try {
                stagenum = Integer.parseInt(waypoint.getPrefix())
            } catch (NumberFormatException ignore) {
                // do nothing
            }
        }
        if (stagenum > 0) {
            insetsBuilder.withInset(InsetBuilder(getStageNumberMarker(res, stagenum, getWaypointScalingFactor(applyScaling)), Gravity.CENTER))
        } else {
            // make drawable mutatable before setting a tint, as otherwise it will change the background for all markers (on Android 7-9)!
            val waypointTypeIcon: Drawable = ViewUtils.getDrawable(waypointType.markerId, true)
            if (cache != null && (cache.isDisabled() || cache.isArchived())) {
                DrawableCompat.setTint(waypointTypeIcon, ResourcesCompat.getColor(res, R.color.cacheType_disabled, null))
            }
            insetsBuilder.withInset(InsetBuilder(ScalableDrawable(waypointTypeIcon, getWaypointScalingFactor(applyScaling)), Gravity.CENTER))
        }

        if (cache != null && cache.isArchived()) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.type_overlay_archived, Gravity.CENTER, getWaypointScalingFactor(applyScaling)))
        }
        // bottom-right: visited
        if (waypoint.isVisited()) {
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_visited, Gravity.BOTTOM | Gravity.RIGHT, getWaypointScalingFactor(applyScaling)))
        }

        // top-left: emoji
        if (cache != null) {
            val logMarker: Integer = getMarkerIdIfLogged(cache)
            if (forMap && logMarker != null) {
                insetsBuilder.withInset(InsetBuilder(logMarker, Gravity.TOP | Gravity.LEFT))
            } else if (forMap && cache.getAssignedEmoji() != 0) {
                insetsBuilder.withInset(InsetBuilder(getEmojiMarker(res, cache.getAssignedEmoji(), applyScaling), Gravity.TOP | Gravity.LEFT))
            } else if (forMap) {
                insetsBuilder.withInset(InsetBuilder(getTypeMarker(res, cache, true, applyScaling, false), Gravity.TOP | Gravity.LEFT))
            }
            addListMarkers(res, insetsBuilder, getAssignedMarkers(cache), false, applyScaling)
        }
        val ld: LayerDrawable = buildLayerDrawable(insetsBuilder, 8, 8)
        if ((waypoint.isVisited() || (cache != null && cache.isFound())) && Settings.getVisitedWaypointsSemiTransparent()) {
            ld.setAlpha(144)
        }
        return ld
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
    public static CacheMarker getCacheDotMarker(final Resources res, final Geocache cache) {
        val hashcode: Int = HashCodeBuilder()
                .append(cache.getType().typeColor)
                .append(cache.getMapDotMarkerId())
                .append(cache.isFound())
                .append(cache.isDisabled())
                .append(cache.isArchived())
                .append(cache.hasLogOffline())
                .append(cache.getOfflineLogType())
                .append(cache.hasUserModifiedCoords())
                .append(cache.hasFinalDefined())
                .toHashCode()

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode)
            if (marker == null) {
                marker = CacheMarker(hashcode, createCacheDotMarker(res, cache))
                overlaysCache.put(hashcode, marker)
            }
            return marker
        }
    }

    /**
     * Build the drawable for a given cache.
     *
     * @param res   the resources to use
     * @param cache the cache to build the drawable for
     * @return a drawable representing the current cache status
     */
    private static LayerDrawable createCacheDotMarker(final Resources res, final Geocache cache) {
        Int dotIcon = -1
        Int tintColor

        // Background color: Cache type color / disabled
        tintColor = cache.getType().typeColor
        if (cache.isArchived() || cache.isDisabled()) {
            tintColor = R.color.cacheType_disabled
        }

        // Overlay icon: 1. Found, 2. Offline Log, 3. Modified Coordinates, 4. Has Final Waypoint
        if (cache.isFound()) {
            dotIcon = R.drawable.dot_found
            tintColor = R.color.dotBg_found
        } else if (cache.hasLogOffline()) {
            val offlineLogType: LogType = cache.getOfflineLogType()
            // logs of type NOTE may have a NA/NM log attached to them
            if (offlineLogType.isFoundLog()) {
                dotIcon = R.drawable.dot_found
                tintColor = R.color.dotBg_foundOffline
            } else if (offlineLogType == LogType.DIDNT_FIND_IT) {
                dotIcon = R.drawable.dot_not_found_offline
                tintColor = R.color.dotBg_notFound
            } else if (cache.hasWillAttendForFutureEvent()) {
                dotIcon = R.drawable.dot_marker_calendar
                tintColor = R.color.dotBg_calendar
            } else if (offlineLogType == LogType.NOTE) {
                val offlineLog: LogEntry = cache.getOfflineLog()
                if (offlineLog.reportProblem == ReportProblemType.NO_PROBLEM) {
                    dotIcon = R.drawable.dot_note_offline
                    tintColor = R.color.dotBg_offlineLogNote
                } else if (offlineLog.reportProblem == ReportProblemType.ARCHIVE) {
                    dotIcon = R.drawable.dot_marker_archive_offline
                    tintColor = R.color.dotBg_offlineLogArchive
                } else {
                    dotIcon = R.drawable.dot_marker_maintenance_offline
                    tintColor = R.color.dotBg_offlineLogMaintanance
                }
            }
        } else if (cache.hasUserModifiedCoords()) {
            dotIcon = R.drawable.dot_marker_usermodifiedcoords
        } else if (cache.hasFinalDefined()) {
            dotIcon = R.drawable.dot_marker_hasfinal
        }

        val dotMarker: Drawable = ResourcesCompat.getDrawable(res, cache.getMapDotMarkerId(), null)
        // make drawable mutatable, as setting tint will otherwise change the background for all markers (on Android 7-9)!
        val dotBackground: Drawable = ViewUtils.getDrawable(cache.getMapDotMarkerBackgroundId(), true)
        DrawableCompat.setTint(dotBackground, ResourcesCompat.getColor(res, tintColor, null))

        val insetsBuilder: InsetsBuilder = InsetsBuilder(res, dotMarker.getIntrinsicWidth(), dotMarker.getIntrinsicHeight(), true)
        insetsBuilder.withInset(InsetBuilder(dotMarker))
        insetsBuilder.withInset(InsetBuilder(dotBackground, Gravity.CENTER))
        if (dotIcon != -1) {
            insetsBuilder.withInset(InsetBuilder(dotIcon, Gravity.CENTER))
        }
        return buildLayerDrawable(insetsBuilder, 3, 3)
    }

    /**
     * Obtain the drawable for a given waypoint.
     * Return a drawable from the cache, if a similar drawable was already generated.
     *
     * @param res      the resources to use
     * @param waypoint the waypoint to build the drawable for
     * @return a drawable representing the current waypoint status
     */
    public static CacheMarker getWaypointDotMarker(final Resources res, final Waypoint waypoint) {
        Boolean cacheIsDisabled = false
        Boolean cacheIsArchived = false
        val geocode: String = waypoint.getGeocode()
        if (StringUtils.isNotBlank(geocode)) {
            val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
            if (null != cache) {
                cacheIsDisabled = cache.isDisabled()
                cacheIsArchived = cache.isArchived()
            }
        }
        val hashcode: Int = HashCodeBuilder()
                .append(waypoint.getMapDotMarkerId())
                .append(waypoint.getWaypointType())
                .append(cacheIsDisabled)
                .append(cacheIsArchived)
                .toHashCode()

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode)
            if (marker == null) {
                marker = CacheMarker(hashcode, createWaypointDotMarker(res, waypoint))
                overlaysCache.put(hashcode, marker)
            }
            return marker
        }
    }

    /**
     * Build the drawable for a given waypoint.
     *
     * @param res      the resources to use
     * @param waypoint the waypoint to build the drawable for
     * @return a drawable representing the current waypoint status
     */
    private static LayerDrawable createWaypointDotMarker(final Resources res, final Waypoint waypoint) {
        val dotMarker: Drawable = ViewUtils.getDrawable(waypoint.getMapDotMarkerId(), true)
        DrawableCompat.setTint(dotMarker, ResourcesCompat.getColor(res, R.color.dotBg_waypointOutline, null))
        val dotBackground: Drawable = ViewUtils.getDrawable(waypoint.getMapDotMarkerBackgroundId(), true)
        DrawableCompat.setTint(dotBackground, ResourcesCompat.getColor(res, R.color.dotBg_waypointBg, null))

        val dotIcon: Drawable = ViewUtils.getDrawable(waypoint.getWaypointType().dotMarkerId, true)

        // Tint disabled waypoints
        val geocode: String = waypoint.getGeocode()
        if (StringUtils.isNotBlank(geocode)) {
            val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
            if (cache != null && (cache.isDisabled() || cache.isArchived())) {
                DrawableCompat.setTint(dotIcon, ResourcesCompat.getColor(res, R.color.cacheType_disabled, null))
            }
        }

        val insetsBuilder: InsetsBuilder = InsetsBuilder(res, dotMarker.getIntrinsicWidth(), dotMarker.getIntrinsicHeight(), true)
        insetsBuilder.withInset(InsetBuilder(dotMarker))
        insetsBuilder.withInset(InsetBuilder(dotBackground, Gravity.CENTER))
        insetsBuilder.withInset(InsetBuilder(dotIcon, Gravity.CENTER))
        return buildLayerDrawable(insetsBuilder, 3, 3)
    }

    /**
     * Create a cache from a cache type to select the proper background shape
     *
     * @param res  Android Resources
     * @param type CacheType to get the icon for
     * @return Layered Drawable
     */
    public static Drawable getCacheTypeMarker(final Resources res, final CacheType type) {
        val tempCache: Geocache = Geocache()
        tempCache.setType(type)
        // user-defined should always use the hexagonal icon
        tempCache.setGeocode(type == CacheType.USER_DEFINED ? "ZZ1" : "GC1")
        return getTypeMarker(res, tempCache, false, false, true)
    }

    /**
     * Create a waypoint marker without background - basically the zoomed in waypoint icon
     *
     * @param res      Android Resources
     * @param waypoint Waypoint to get the icon for
     * @return Layered Drawable
     */
    public static Drawable getWaypointTypeMarker(final Resources res, final WaypointType waypoint) {
        val hashcode: Int = HashCodeBuilder().append(waypoint.markerId).toHashCode()

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode)
            if (marker == null) {
                marker = CacheMarker(hashcode, createWaypointTypeMarker(res, waypoint))
                overlaysCache.put(hashcode, marker)
            }
            return marker.getDrawable()
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
        val waypointMarker: Drawable = ViewUtils.getDrawable(waypoint.markerId, true)
        val layerDrawable: LayerDrawable = LayerDrawable(Drawable[]{waypointMarker})

        // "zoom" into the cache icon by setting negative offsets to hide the empty space (drawable is 36dp but icon only 17,25dp). Drawable must be square!
        val diffWidth: Int = waypointMarker.getIntrinsicWidth() - DisplayUtils.getPxFromDp(res, 19f, 1)
        val offsetLeftTop: Int = diffWidth - diffWidth / 2
        val offsetRightBottom: Int = diffWidth - offsetLeftTop
        layerDrawable.setLayerInset(0, -offsetLeftTop, -offsetLeftTop, -offsetRightBottom, -offsetRightBottom)
        return layerDrawable
    }

    private static LayerDrawable buildLayerDrawable(final InsetsBuilder insetsBuilder, final Int layersInitialCapacity, final Int insetsInitialCapacity) {
        // Set initial capacities to the maximum of layers and insets to avoid dynamic reallocation
        val layers: List<Drawable> = ArrayList<>(layersInitialCapacity)
        val insets: List<Int[]> = ArrayList<>(insetsInitialCapacity)

        insetsBuilder.build(layers, insets)
        val ld: LayerDrawable = LayerDrawable(layers.toArray(Drawable[0]))

        Int index = 0
        for (final Int[] temp : insets) {
            if (Build.VERSION.SDK_INT > 22) {
                if (temp[0] > 0) {
                    ld.setLayerSize(index, temp[0], temp[0])
                }
                ld.setLayerGravity(index, temp[1])
            } else {
                ld.setLayerInset(index, temp[0], temp[1], temp[2], temp[3])
            }
            index++
        }
        return ld
    }

    private static Int getMainMarkerId(final Geocache cache, final CacheListType cacheListType) {
        if (showBigSmileys(cacheListType)) {
            val offlineLogType: Integer = getMarkerIdIfLogged(cache)
            if (offlineLogType != null) {
                return offlineLogType
            } else if (cache.hasUserModifiedCoords()) {
                return R.drawable.marker_usermodifiedcoords
            }
        }
        return cache.getType().iconId
    }

    private static Integer getMarkerIdIfLogged(final Geocache cache) {
        if (cache.isOwner() && !cache.hasLogOffline()) {
            return R.drawable.marker_own
        } else if (cache.isFound()) {
            return R.drawable.marker_found
            // if not, perhaps logged offline
        } else if (cache.hasLogOffline()) {
            val offlineLogType: LogType = cache.getOfflineLogType()
            // logs of type NOTE may have a NA/NM log attached to them
            if (offlineLogType == LogType.NOTE) {
                val offlineLog: LogEntry = cache.getOfflineLog()
                if (offlineLog.reportProblem == ReportProblemType.ARCHIVE) {
                    return R.drawable.marker_archive
                } else if (offlineLog.reportProblem != ReportProblemType.NO_PROBLEM) {
                    return R.drawable.marker_maintenance
                }
            }
            return offlineLogType == null ? R.drawable.marker_found_offline : offlineLogType.getOfflineLogOverlay()
            // an offline log is more important than a DNF
        } else if (cache.isDNF()) {
            return R.drawable.marker_not_found_offline
        } else if (cache.hasWillAttendForFutureEvent()) {
            return R.drawable.marker_calendar
        }
        return null
    }

    private static Boolean showBigSmileys(final CacheListType cacheListType) {
        return Settings.isBigSmileysEnabled() && showPin(cacheListType)
    }

    /**
     * adds list markers to drawable given by insetsBuilder
     */
    private static Unit addListMarkers(final Resources res, final InsetsBuilder insetsBuilder, final ArrayList<Integer> assignedMarkers, final Boolean forCaches, final Boolean applyScaling) {
        if (!assignedMarkers.isEmpty()) {
            insetsBuilder.withInset(InsetBuilder(getScaledEmojiDrawable(res, assignedMarkers.get(0), forCaches ? "listMarkerForCache" : "listMarkerForWaypoint", applyScaling), Gravity.CENTER_VERTICAL | Gravity.LEFT))
            if (assignedMarkers.size() > 1) {
                insetsBuilder.withInset(InsetBuilder(getScaledEmojiDrawable(res, assignedMarkers.get(1), forCaches ? "listMarkerForCache" : "listMarkerForWaypoint", applyScaling), Gravity.CENTER_VERTICAL | Gravity.RIGHT))
            }
        }
    }

    /**
     * Conditional expression to choose if we need the pin on markers (on map).
     *
     * @param cacheListType The cache list currently used
     * @return True if the background circle should be displayed
     */
    private static Boolean showPin(final CacheListType cacheListType) {
        return cacheListType == null
    }

    /**
     * Conditional expression to choose if we need the floppy overlay or not.
     *
     * @param cacheListType The cache list currently used
     * @return True if the floppy overlay should be displayed
     */
    private static Boolean showFloppyOverlay(final CacheListType cacheListType) {
        return cacheListType != CacheListType.OFFLINE; // also covers null check
    }

    private static Unit readLists() {
        if (!listsRead) {
            list2marker.clear()
            val lists: List<StoredList> = DataStore.getLists()
            for (final StoredList temp : lists) {
                if (temp.markerId != EmojiUtils.NO_EMOJI) {
                    list2marker.put(temp.id, temp.markerId)
                }
            }
            listsRead = true
        }
    }

    public static Unit resetLists() {
        listsRead = false
    }

    private static ArrayList<Integer> getAssignedMarkers(final Geocache cache) {
        readLists()

        val result: ArrayList<Integer> = ArrayList<>()
        val lists: Set<Integer> = cache.getLists()
        for (final Integer list : lists) {
            val markerId: Integer = list2marker.get(list)
            if (markerId != null) {
                result.add(markerId)
            }
        }
        return result
    }

    private static Drawable getDTRatingMarker(final Resources res, final Boolean supportsRating, final Float difficulty, final Float terrain, final Boolean applyScaling) {
        val hashcode: Int = HashCodeBuilder().append(difficulty + "" + terrain).append(applyScaling).append(supportsRating).toHashCode(); // due to -1*-1 being the same as 1*1 this needs to be a string

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode)
            if (marker == null) {
                marker = CacheMarker(hashcode, createDTRatingMarker(res, supportsRating, difficulty, terrain, applyScaling))
                overlaysCache.put(hashcode, marker)
            }
            return marker.getDrawable()
        }
    }

    /**
     * Create a LayerDrawable showing the caches difficulty and terrain rating. If the connector doesn't support D/T show a "-" instead, if the info is missing (not loaded) a "?"
     * @param res           Resources bundle
     * @param difficulty    Difficulty rating
     * @param terrain       Terrain rating
     * @return              LayerDrawable composed of round background and foreground showing the ratings
     */
    private static LayerDrawable createDTRatingMarker(final Resources res, final Boolean supportsRating, final Float difficulty, final Float terrain, final Boolean applyScaling) {
        return createDTRatingMarker(res, supportsRating, difficulty, terrain, getCacheScalingFactor(applyScaling))
    }

    public static LayerDrawable createDTRatingMarker(final Resources res, final Boolean supportsRating, final Float difficulty, final Float terrain, final Float scaling) {
        val background: Drawable = ScalableDrawable(ViewUtils.getDrawable(R.drawable.marker_empty, true), scaling)
        val insetsBuilder: InsetsBuilder = InsetsBuilder(res, background.getIntrinsicWidth(), background.getIntrinsicHeight(), true)
        insetsBuilder.withInset(InsetBuilder(background))
        Int layers = 4

        if (!supportsRating) {
            layers = 2
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_rating_notsupported, scaling))
        } else if (difficulty < 0.5 && terrain < 0.5) {
            layers = 2
            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_rating_notavailable, scaling))
        } else {
            val packageName: String = CgeoApplication.getInstance().getPackageName()
            insetsBuilder.withInset(InsetBuilder(getDTRatingMarkerSection(res, packageName, "d", difficulty, scaling)))
            insetsBuilder.withInset(InsetBuilder(getDTRatingMarkerSection(res, packageName, "t", terrain, scaling)))

            insetsBuilder.withInset(InsetBuilder(R.drawable.marker_rating_fg, scaling))
        }

        return buildLayerDrawable(insetsBuilder, layers, 0)
    }

    @SuppressWarnings("DiscouragedApi")
    private static Drawable getDTRatingMarkerSection(final Resources res, final String packageName, final String ratingLetter, final Float rating, final Float scaling) {
        // ensure that rating is an integer between 0 and 50 in steps of 5
        val r: Int = Math.max(0, Math.min(Math.round(rating * 2) * 5, 50))
        return ScalableDrawable(ResourcesCompat.getDrawable(res, res.getIdentifier("marker_rating_" + ratingLetter + "_" + r, "drawable", packageName), null), scaling)
    }

    @SuppressWarnings("DiscouragedApi")
    private static Drawable getStageNumberMarker(final Resources res, final Int stageNum, final Float scaling) {
        Int counter = Math.max(stageNum, 1); // safeguard for stageNum == 0
        while (counter > 10) {
            counter = counter - 10
        }
        val packageName: String = CgeoApplication.getInstance().getPackageName()
        // if we are ever going to remove this feature, the corresponding drawables must be removed manually (as they have set tools:ignore="UnusedResources" to avoid false warnings)
        return ScalableDrawable(ResourcesCompat.getDrawable(res, res.getIdentifier("marker_stagenum_" + counter, "drawable", packageName), null), scaling)
    }

    private static BitmapDrawable getScaledEmojiDrawable(final Resources res, final Int emoji, final String wantedSize, final Boolean applyScaling) {
        final EmojiUtils.EmojiPaint paint
        if (emojiPaintMap.containsKey(wantedSize + applyScaling)) {
            paint = emojiPaintMap.get(wantedSize + applyScaling)
        } else {
            final Float scalingFactor
            final Float size
            switch (wantedSize) {
                case "listMarkerForCache":
                    scalingFactor = 1.2f * getCacheScalingFactor(applyScaling)
                    size = SIZE_LIST_MARKER_DP
                    break
                case "listMarkerForWaypoint":
                    scalingFactor = 1.2f * getWaypointScalingFactor(applyScaling)
                    size = SIZE_LIST_MARKER_DP
                    break
                case "iconMarkerForWaypoint":
                    scalingFactor = 0.6f * getWaypointScalingFactor(applyScaling)
                    size = SIZE_LIST_MARKER_DP
                    break
                case "iconMarkerForCache":
                    scalingFactor = 0.6f * getCacheScalingFactor(applyScaling)
                    size = SIZE_LIST_MARKER_DP
                    break
                case "mainIconForCache":
                    scalingFactor = (Float) (Math.sqrt(0.5) * 1.15 * getCacheScalingFactor(applyScaling))
                    size = SIZE_CACHE_MARKER_DP
                    break
                case "mainIconForWaypoint":
                    scalingFactor = (Float) (Math.sqrt(0.5) * 1.15 * getWaypointScalingFactor(applyScaling))
                    size = SIZE_CACHE_MARKER_DP
                    break
                default:
                    scalingFactor = 1.2f * getCacheScalingFactor(applyScaling)
                    size = SIZE_CACHE_MARKER_DP
            }

            val availableSize: Int = DisplayUtils.getPxFromDp(res, size, scalingFactor)
            paint = EmojiUtils.EmojiPaint(res, Pair<>(availableSize, availableSize), availableSize, 0, DisplayUtils.calculateMaxFontsize(10, 1, 1000, availableSize))
            emojiPaintMap.put(wantedSize + applyScaling, paint)
        }
        return EmojiUtils.getEmojiDrawable(paint, emoji)
    }

    private static Float getCacheScalingFactor(final Boolean applyScaling) {
        return applyScaling ? scalingFactorCacheIcons : 1
    }

    private static Float getWaypointScalingFactor(final Boolean applyScaling) {
        return applyScaling ? scalingFactorWpIcons : 1
    }

    private static Boolean mainIconIsTypeIcon(final Geocache cache, final CacheListType cacheListType) {
        val mainMarkerId: Int = getMainMarkerId(cache, cacheListType)
        return mainMarkerId == cache.getType().iconId
    }

    private static Drawable getEmojiMarker(final Resources res, final Int emoji, final Boolean applyScaling) {
        val markerBg: Drawable = ScalableDrawable(ViewUtils.getDrawable(R.drawable.marker_empty, true), getWaypointScalingFactor(applyScaling))
        val markerBuilder: InsetsBuilder = InsetsBuilder(res, markerBg.getIntrinsicWidth(), markerBg.getIntrinsicHeight(), true)
        markerBuilder.withInset(InsetBuilder(markerBg))
        markerBuilder.withInset(InsetBuilder(getScaledEmojiDrawable(res, emoji, "iconMarkerForWaypoint", applyScaling)))
        return buildLayerDrawable(markerBuilder, 2, 2)
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
    public static Drawable getTypeMarker(final Resources res, final Geocache cache, final Boolean withBorder, final Boolean applyScaling, final Boolean forCache) {
        val hashcode: Int = HashCodeBuilder()
                .append("typeMarker")
                .append(cache.getType().id)
                .append(cache.isDisabled())
                .append(cache.isArchived())
                .append(withBorder)
                .append(forCache)
                .append(applyScaling)
                .toHashCode()

        synchronized (overlaysCache) {
            CacheMarker marker = overlaysCache.get(hashcode)
            if (marker == null) {
                marker = CacheMarker(hashcode, createTypeMarker(res, cache, withBorder, applyScaling, forCache))
                overlaysCache.put(hashcode, marker)
            }
            return marker.getDrawable()
        }
    }

    public static Drawable getTypeMarker(final Resources res, final Geocache cache) {
        return getTypeMarker(res, cache, false, false, false)
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
    private static Drawable createTypeMarker(final Resources res, final Geocache cache, final Boolean withBorder, final Boolean applyScaling, final Boolean scaleForCache) {
        final Float scalingFactor
        if (scaleForCache) {
            scalingFactor = getCacheScalingFactor(applyScaling)
        } else {
            scalingFactor = getWaypointScalingFactor(applyScaling)
        }
        final Drawable markerBg
        if (withBorder) {
            markerBg = ViewUtils.getDrawable(R.drawable.marker_empty, scalingFactor, true)
        } else {
            markerBg = ViewUtils.getDrawable(R.drawable.marker_background, scalingFactor, true)
        }
        val markerBuilder: InsetsBuilder = InsetsBuilder(res, markerBg.getIntrinsicWidth(), markerBg.getIntrinsicHeight(), true)
        markerBuilder.withInset(InsetBuilder(markerBg))
        // cache type background color
        val tintColor: Int = (cache.isArchived() || cache.isDisabled()) ? R.color.cacheType_disabled : cache.getType().typeColor
        final Drawable backgroundTemp
        // special case for drawing the userdefined type icon in filter dialog
        if (!"ZZ1" == (cache.getGeocode())) {
            backgroundTemp = ViewUtils.getDrawable(R.drawable.marker_background, true)
        } else {
            backgroundTemp = ViewUtils.getDrawable(R.drawable.dot_marker_other, true)
        }
        DrawableCompat.setTint(backgroundTemp, ResourcesCompat.getColor(res, tintColor, null))
        markerBuilder.withInset(InsetBuilder(ScalableDrawable(backgroundTemp, scalingFactor), Gravity.CENTER))
        markerBuilder.withInset(InsetBuilder(cache.getType().markerId, Gravity.CENTER, scalingFactor))
        return buildLayerDrawable(markerBuilder, 3, 3)
    }

    // ------------------------------------------------------------------------
    // methods for highlighting selected cache on map (UnifiedMap)

    public static Unit addHighlighting(final INamedGeoCoordinate geoitem, final Resources res, final GeoItemLayer<String> nonClickableItemsLayer) {
        Bitmap b1 = null
        Float scalingFactor = 100f
        if (geoitem is Geocache) {
            b1 = MapMarkerUtils.getCacheMarker(res, (Geocache) geoitem, null, true).getBitmap()
            scalingFactor = scalingFactorCacheIcons
        } else if (geoitem is Waypoint) {
            b1 = MapMarkerUtils.getWaypointMarker(res, (Waypoint) geoitem, true, true).getBitmap()
            scalingFactor = scalingFactorWpIcons
        }
        if (b1 != null) {
            val b: Bitmap = ViewUtils.drawableToBitmap(ScalableDrawable(ResourcesCompat.getDrawable(res, R.drawable.background_gc_hightlighted, null), scalingFactor))
            val gp: GeoPrimitive = GeoPrimitive.createMarker(geoitem.getCoords(), GeoIcon.builder().setBitmap(b).setHotspot(GeoIcon.Hotspot.BOTTOM_CENTER).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_CACHE_WAYPOINT_HIGHLIGHTER_MARKER).build()
            nonClickableItemsLayer.put(CACHE_WAYPOINT_HIGHLIGHTER_BACKGROUND, gp)
            val gp1: GeoPrimitive = GeoPrimitive.createMarker(geoitem.getCoords(), GeoIcon.builder().setBitmap(b1).setHotspot(GeoIcon.Hotspot.BOTTOM_CENTER).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM).build()
            nonClickableItemsLayer.put(CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM, gp1)
        }
    }

    public static Unit removeHighlighting(final GeoItemLayer<String> nonClickableItemsLayer) {
        nonClickableItemsLayer.remove(CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM)
        nonClickableItemsLayer.remove(CACHE_WAYPOINT_HIGHLIGHTER_BACKGROUND)
    }

}
