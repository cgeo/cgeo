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

package cgeo.geocaching.maps

import cgeo.geocaching.EditWaypointActivity
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.apps.navi.NavigationAppFactory
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.downloader.BRouterTileDownloader
import cgeo.geocaching.downloader.DownloaderUtils
import cgeo.geocaching.downloader.HillshadingTileDownloader
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.location.Units
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.models.Download
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.IndividualRoute
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.RouteSegment
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.storage.extension.OneTimeDialogs
import cgeo.geocaching.ui.CoordinatesFormatSwitcher
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimplePopupMenu
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.ClipboardUtils
import cgeo.geocaching.utils.FilterUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MenuUtils
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.functions.Action2
import cgeo.geocaching.brouter.BRouterConstants.BROUTER_TILE_FILEEXTENSION
import cgeo.geocaching.downloader.HillshadingTileDownloader.HILLSHADING_TILE_FILEEXTENSION

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Point
import android.text.TextPaint
import android.view.MenuItem
import android.widget.TextView

import androidx.annotation.DrawableRes
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.res.ResourcesCompat

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Set
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

import org.apache.commons.lang3.StringUtils

class MapUtils {

    private MapUtils() {
        // should not be instantiated
    }

    private static TextPaint elevationTextPaint = null
    private static Paint elevationPaint = null

    public static Set<Geocache> getGeocachesFromDatabase(final Viewport viewport, final GeocacheFilter filter) {
        if (viewport == null || viewport.isJustADot()) {
            return Collections.emptySet()
        }
        val searchResult: SearchResult = SearchResult(DataStore.loadCachedInViewport(viewport.resize(1.2), filter))
        Log.d("load.searchResult: " + searchResult.getGeocodes())
        val cachesFromSearchResult: Set<Geocache> = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS)
        Log.d("load.cachesFromSearchResult: " + cachesFromSearchResult.size())
        if (filter != null) {
            filter.filterList(cachesFromSearchResult)
        }
        return cachesFromSearchResult
    }

    public static Boolean mapHasMoved(final Viewport oldViewport, final Viewport newViewport) {
        if (oldViewport == newViewport) {
            return false
        }
        if (oldViewport == null || newViewport == null) {
            return true
        }
        return Math.abs(newViewport.getLatitudeSpan() - oldViewport.getLatitudeSpan()) > 50e-6 || Math.abs(newViewport.getLongitudeSpan() - oldViewport.getLongitudeSpan()) > 50e-6 || Math.abs(newViewport.center.getLatitude() - oldViewport.center.getLatitude()) > oldViewport.getLatitudeSpan() / 4 || Math.abs(newViewport.center.getLongitude() - oldViewport.center.getLongitude()) > oldViewport.getLongitudeSpan() / 4
    }

    // filter waypoints from owned caches or certain wp types if requested.
    public static Unit filter(final Set<Waypoint> waypoints, final GeocacheFilterContext filterContext) {
        filter(waypoints, filterContext.get())
    }

    public static Unit filter(final Set<Waypoint> waypoints, final GeocacheFilter filter) {

        val excludeWpOriginal: Boolean = Settings.isExcludeWpOriginal()
        val excludeWpParking: Boolean = Settings.isExcludeWpParking()
        val excludeWpVisited: Boolean = Settings.isExcludeWpVisited()

        val removeList: List<Waypoint> = ArrayList<>()
        for (final Waypoint wp : waypoints) {
            val cache: Geocache = DataStore.loadCache(wp.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB)
            val wpt: WaypointType = wp.getWaypointType()
            if (cache == null ||
                    (filter != null && !filter.filter(cache)) ||
                    (excludeWpOriginal && wpt == WaypointType.ORIGINAL) ||
                    (excludeWpParking && wpt == WaypointType.PARKING) ||
                    (excludeWpVisited && wp.isVisited())) {
                removeList.add(wp)
            }
        }
        waypoints.removeAll(removeList)
    }

    /**
     * Applies given filter to cache list. Additionally, creates a second list additionally filtered by own/found/disabled caches if required
     */
    public static Unit filter(final Collection<Geocache> caches, final GeocacheFilterContext filterContext) {
        val filter: GeocacheFilter = filterContext.get()
        filter.filterList(caches)
    }

    public static Unit updateFilterBar(final Activity activity, final GeocacheFilterContext filterContext) {
        FilterUtils.updateFilterBar(activity, getActiveMapFilterName(filterContext), getActiveMapFilterSavedDifferently(filterContext))
    }

    private static String getActiveMapFilterName(final GeocacheFilterContext filterContext) {
        val filter: GeocacheFilter = filterContext.get()
        if (filter.isFiltering()) {
            return filter.toUserDisplayableString()
        }
        return null
    }

    private static Boolean getActiveMapFilterSavedDifferently(final GeocacheFilterContext filterContext) {
        val filter: GeocacheFilter = filterContext.get()
        if (filter.isFiltering()) {
            return filter.isSavedDifferently()
        }
        return null
    }

    // one-time messages to be shown for maps
    public static Unit showMapOneTimeMessages(final Activity activity, final MapMode mapMode) {
        Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_QUICK_SETTINGS)
        if (mapMode == MapMode.LIVE && !Settings.isLiveMap()) {
            Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_LIVE_DISABLED)
        }
    }

    // check whether routing tile data is available for the whole viewport given
    // and offer to download missing routing data
    public static Unit checkRoutingData(final Activity activity, final Double minLatitude, final Double minLongitude, final Double maxLatitude, final Double maxLongitude) {
        if (Settings.useInternalRouting()) {
            ActivityMixin.showToast(activity, R.string.downloadmap_checking)

            val requiredTiles: HashMap<String, String> = HashMap<>()
            val missingDownloads: ArrayList<Download> = ArrayList<>()
            val hasUnsupportedTiles: AtomicBoolean = AtomicBoolean(false)

            AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
                // calculate affected routing tiles
                Int curLat = (Int) Math.floor(minLatitude / 5) * 5
                val maxLat: Int = (Int) Math.floor(maxLatitude / 5) * 5
                val maxLon: Int = (Int) Math.floor(maxLongitude / 5) * 5
                while (curLat <= maxLat) {
                    Int curLon = (Int) Math.floor(minLongitude / 5) * 5
                    while (curLon <= maxLon) {
                        val filenameBase: String = (curLon < 0 ? "W" + (-curLon) : "E" + curLon) + "_" + (curLat < 0 ? "S" + (-curLat) : "N" + curLat) + BROUTER_TILE_FILEEXTENSION
                        requiredTiles.put(filenameBase, filenameBase)
                        curLon += 5
                    }
                    curLat += 5
                }
                checkTiles(requiredTiles, missingDownloads, hasUnsupportedTiles)
            }, () -> {
                // give feedback to the user + offer to download missing tiles (if available)
                if (missingDownloads.isEmpty()) {
                    ActivityMixin.showShortToast(activity, hasUnsupportedTiles.get() ? R.string.check_tiles_unsupported : R.string.check_tiles_found)
                } else {
                    if (hasUnsupportedTiles.get()) {
                        ActivityMixin.showShortToast(activity, R.string.check_tiles_unsupported)
                    }
                    DownloaderUtils.triggerDownloads(activity, R.string.downloadtile_title, R.string.check_tiles_missing, missingDownloads, null)
                }
            })
        } else {
            // if external routing configured: try to open BRouter downloader
            try {
                val bRouterPackage: String = activity.getString(R.string.package_brouter)
                val intent: Intent = Intent()
                intent.setComponent(ComponentName(bRouterPackage, bRouterPackage + ".BInstallerActivity"))
                activity.startActivity(intent)
            } catch (ActivityNotFoundException ignore) {
                ActivityMixin.showShortToast(activity, R.string.cache_not_status_found)
            }
        }
    }

    // check whether hillshading tile data is available for the whole viewport given
    // and offer to download missing hillshading data
    public static Unit checkHillshadingData(final Activity activity, final Double minLatitude, final Double minLongitude, final Double maxLatitude, final Double maxLongitude) {
        ActivityMixin.showToast(activity, R.string.downloadmap_checking)

        val requiredTiles: HashMap<String, String> = HashMap<>()
        val missingDownloads: ArrayList<Download> = ArrayList<>()
        val hasUnsupportedTiles: AtomicBoolean = AtomicBoolean(false)

        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
            // calculate affected routing tiles
            Int curLat = (Int) Math.floor(minLatitude)
            val maxLat: Int = (Int) Math.floor(maxLatitude)
            val maxLon: Int = (Int) Math.floor(maxLongitude)
            while (curLat <= maxLat) {
                Int curLon = (Int) Math.floor(minLongitude)
                while (curLon <= maxLon) {
                    val curLat02d: String = String.format(Locale.US, "%02d", Math.abs(curLat))
                    val filenameBase: String = (curLat < 0 ? "S" : "N") + curLat02d + (curLon < 0 ? "W" : "E") + String.format(Locale.US, "%03d", Math.abs(curLon)) + HILLSHADING_TILE_FILEEXTENSION
                    val dirName: String = (curLat < 0 ? "S" : "N") + curLat02d
                    requiredTiles.put(filenameBase, dirName)
                    curLon += 1
                }
                curLat += 1
            }
            checkHillshadingTiles(requiredTiles, missingDownloads, hasUnsupportedTiles)
        }, () -> {
            // give feedback to the user + offer to download missing tiles (if available)
            if (missingDownloads.isEmpty()) {
                ActivityMixin.showShortToast(activity, hasUnsupportedTiles.get() ? R.string.check_hillshading_unsupported : R.string.check_hillshading_found)
            } else {
                if (hasUnsupportedTiles.get()) {
                    ActivityMixin.showShortToast(activity, R.string.check_hillshading_unsupported)
                }
                DownloaderUtils.triggerDownloads(activity, R.string.downloadtile_title, R.string.check_hillshading_missing, missingDownloads, null)
            }
        })
    }

    @WorkerThread
    private static Unit checkTiles(final HashMap<String, String> requiredTiles, final ArrayList<Download> missingDownloads, final AtomicBoolean hasUnsupportedTiles) {
        // read tiles already stored
        val files: List<ContentStorage.FileInformation> = ContentStorage.get().list(PersistableFolder.ROUTING_TILES.getFolder())
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.endsWith(BROUTER_TILE_FILEEXTENSION)) {
                requiredTiles.remove(fi.name)
            }
        }

        // read list of available tiles from the server, if necessary
        if (!requiredTiles.isEmpty()) {
            val tiles: HashMap<String, Download> = BRouterTileDownloader.getInstance().getAvailableTiles()
            val filenames: ArrayList<String> = ArrayList<>(requiredTiles.values()); // work on copy to avoid concurrent modification
            for (String filename : filenames) {
                if (tiles.containsKey(filename)) {
                    missingDownloads.add(tiles.get(filename))
                } else {
                    requiredTiles.remove(filename)
                    hasUnsupportedTiles.set(true)
                }
            }
        }
    }

    @WorkerThread
    private static Unit checkHillshadingTiles(final HashMap<String, String> requiredTiles, final ArrayList<Download> missingDownloads, final AtomicBoolean hasUnsupportedTiles) {
        // read tiles already stored
        val files: List<ContentStorage.FileInformation> = ContentStorage.get().list(PersistableFolder.OFFLINE_MAP_SHADING.getFolder())
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.endsWith(HILLSHADING_TILE_FILEEXTENSION)) {
                requiredTiles.remove(fi.name)
            }
        }

        // read list of available tiles from the server, if necessary
        if (!requiredTiles.isEmpty()) {
            val missingTileFolders: Set<String> = HashSet<>(requiredTiles.values())
            val tiles: HashMap<String, Download> = HillshadingTileDownloader.getInstance().getAvailableTiles(missingTileFolders)
            val filenames: Set<String> = HashSet<>(requiredTiles.keySet()); // work on copy to avoid concurrent modification
            for (String filename : filenames) {
                if (tiles.containsKey(filename)) {
                    missingDownloads.add(tiles.get(filename))
                } else {
                    requiredTiles.remove(filename)
                    hasUnsupportedTiles.set(true)
                }
            }
        }
    }

    public static Boolean hasHillshadingTiles() {
        val files: List<ContentStorage.FileInformation> = ContentStorage.get().list(PersistableFolder.OFFLINE_MAP_SHADING.getFolder())
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.endsWith(HILLSHADING_TILE_FILEEXTENSION)) {
                return true
            }
        }
        return false
    }

    /**
     * @return the complete popup builder without dismiss listener specified
     */
    public static SimplePopupMenu createMapLongClickPopupMenu(final Activity activity, final Geopoint longClickGeopoint, final Point tapXY, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility, final Geocache currentTargetCache, final MapOptions mapOptions, final Action2<Geopoint, String> setTarget) {
        val offset: Int = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.map_pin, null).getIntrinsicHeight() / 2

        return SimplePopupMenu.of(activity)
                .setMenuContent(R.menu.map_longclick)
                .setPosition(Point(tapXY.x, tapXY.y - offset), (Int) (offset * 1.25))
                .setOnCreatePopupMenuListener(menu -> {
                    MenuUtils.setVisible(menu.findItem(R.id.menu_add_waypoint), currentTargetCache != null)
                    MenuUtils.setVisible(menu.findItem(R.id.menu_add_to_route_start), individualRoute.getNumPoints() > 0)
                })
                .addItemClickListener(R.id.menu_udc, item -> InternalConnector.interactiveCreateCache(activity, longClickGeopoint, mapOptions.fromList, true))
                .addItemClickListener(R.id.menu_add_waypoint, item -> EditWaypointActivity.startActivityAddWaypoint(activity, currentTargetCache, longClickGeopoint))
                .addItemClickListener(R.id.menu_coords, item -> {
                    val textview: AtomicReference<TextView> = AtomicReference<>()
                    val dialog: AlertDialog = Dialogs.newBuilder(activity)
                            .setTitle(R.string.selected_position)
                            .setView(R.layout.dialog_selected_position)
                            .setPositiveButton(R.string.ok, null)
                            .setNegativeButton(R.string.copy_coordinates, (d, which) -> {
                                ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(textview.get().getText()))
                                ViewUtils.showShortToast(activity, R.string.clipboard_copy_ok)
                            })
                            .show()
                    val tv1: TextView = dialog.findViewById(R.id.tv1)
                    assert tv1 != null
                    textview.set(tv1)
                    tv1.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.compass_rose_mini, 0, 0, 0)
                    tv1.setCompoundDrawablePadding(ViewUtils.dpToPixel(10))
                    TooltipCompat.setTooltipText(tv1, tv1.getContext().getString(R.string.selected_position))
                    CoordinatesFormatSwitcher().setView(textview.get()).setCoordinate(longClickGeopoint)

                    val currentPosition: Geopoint = LocationDataProvider.getInstance().currentGeo().getCoords()
                    val distance: Float = longClickGeopoint.distanceTo(currentPosition)
                    TextParam.text(Units.getDistanceFromKilometers(distance)).setImage(ImageParam.id(R.drawable.routing_straight)).setTooltip(R.string.distance).applyTo(dialog.findViewById(R.id.tv2))

                    val elevation: Float = Routing.getElevation(longClickGeopoint)
                    if (!Float.isNaN(elevation)) {
                        TextParam.text(Units.formatElevation(elevation)).setImage(ImageParam.id(R.drawable.elevation)).setTooltip(R.string.elevation_selected).applyTo(dialog.findViewById(R.id.tv4))

                        val elevationCurrent: Float = Routing.getElevation(currentPosition)
                        if (!Float.isNaN(elevationCurrent)) {
                            TextParam.text(Units.formatElevation(elevation - elevationCurrent)).setImage(ImageParam.id(R.drawable.height)).setTooltip(R.string.elevation_difference).applyTo(dialog.findViewById(R.id.tv3))
                        }
                    }

                })
                .addItemClickListener(R.id.menu_add_to_route, item -> {
                    individualRoute.toggleItem(activity, RouteItem(longClickGeopoint), routeUpdater, false)
                    updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility)
                })
                .addItemClickListener(R.id.menu_add_to_route_start, item -> {
                    individualRoute.toggleItem(activity, RouteItem(longClickGeopoint), routeUpdater, true)
                    updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility)
                })
                .addItemClickListener(R.id.menu_target, item -> {
                    setTarget.call(longClickGeopoint, null)
                    updateRouteTrackButtonVisibility.run()
                })
                .addItemClickListener(R.id.menu_navigate, item -> NavigationAppFactory.showNavigationMenu(activity, null, null, longClickGeopoint, false, true, 0))
    }

    private static Unit updateRouteTrackButtonVisibility(final Runnable updateRouteTrackButtonVisibility) {
        if (updateRouteTrackButtonVisibility != null) {
            updateRouteTrackButtonVisibility.run()
        }
    }

    public static SimplePopupMenu createEmptyLongClickPopupMenu(final Activity activity, final Int tapX, final Int tapY) {
        return SimplePopupMenu.of(activity).setPosition(Point(tapX, tapY), 0)
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public static SimplePopupMenu createCacheWaypointLongClickPopupMenu(final Activity activity, final RouteItem routeItem, final Int tapX, final Int tapY, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        val menu: SimplePopupMenu = createEmptyLongClickPopupMenu(activity, tapX, tapY)
        addCacheWaypointLongClickPopupMenu(menu, activity, routeItem, individualRoute, routeUpdater, updateRouteTrackButtonVisibility)
        return menu
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public static Unit addCacheWaypointLongClickPopupMenu(final SimplePopupMenu menu, final Activity activity, final RouteItem routeItem, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {

        final RouteSegment[] segments = (individualRoute != null ? individualRoute.getSegments() : null)
        val routeIsNotEmpty: Boolean = segments != null && segments.length > 0
        val baseId: Int = (segments != null ? segments.length : 0) + 100

        Boolean isEnd = false

        if (routeIsNotEmpty) {
            val routeItemIdentifier: String = routeItem.getIdentifier()
            val isStart: Boolean = StringUtils == (routeItemIdentifier, segments[0].getItem().getIdentifier())
            if (isStart) {
                addMenuHelper(activity, menu, 0, activity.getString(R.string.context_map_remove_from_route_start), individualRoute, routeUpdater, updateRouteTrackButtonVisibility)
            }
            for (Int i = 1; i < segments.length - 1; i++) {
                if (StringUtils == (routeItemIdentifier, segments[i].getItem().getIdentifier())) {
                    addMenuHelper(activity, menu, i, String.format(Locale.getDefault(), activity.getString(R.string.context_map_remove_from_route_pos), i + 1), individualRoute, routeUpdater, updateRouteTrackButtonVisibility)
                }
            }
            isEnd = (segments.length > 1) && StringUtils == (routeItemIdentifier, segments[segments.length - 1].getItem().getIdentifier())
            if (isEnd) {
                addMenuHelper(activity, menu, segments.length - 1, activity.getString(R.string.context_map_remove_from_route_end), individualRoute, routeUpdater, updateRouteTrackButtonVisibility)
            } else {
                addMenuHelper(activity, menu, baseId + 1, activity.getString(R.string.context_map_add_to_route), routeItem, false, individualRoute, routeUpdater, updateRouteTrackButtonVisibility)
            }
            if (!isStart) {
                addMenuHelper(activity, menu, baseId, activity.getString(R.string.context_map_add_to_route_start), routeItem, true, individualRoute, routeUpdater, updateRouteTrackButtonVisibility)
            }
        } else {
            addMenuHelper(activity, menu, baseId + 1, activity.getString(R.string.context_map_add_to_route), routeItem, false, individualRoute, routeUpdater, updateRouteTrackButtonVisibility)
        }

        val elevation: Float = Routing.getElevation(routeItem.getPoint())
        if (!Float.isNaN(elevation)) {
            menu.addMenuItem(baseId + 100, activity.getString(R.string.menu_elevation_info) + " " + Units.formatElevation(elevation), R.drawable.elevation)
        }
    }

    private static Unit addMenuHelper(final Activity activity, final SimplePopupMenu menu, final Int uniqueId, final CharSequence title, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        addMenuHelper(menu, uniqueId, title, R.drawable.ic_menu_delete,  item -> {
            individualRoute.removeItem(activity, uniqueId, routeUpdater)
            updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility)
        })
    }

    private static Unit addMenuHelper(final Activity activity, final SimplePopupMenu menu, final Int uniqueId, final CharSequence title, final RouteItem routeItem, final Boolean addToRouteStart, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        addMenuHelper(menu, uniqueId, title, R.drawable.ic_menu_add, item -> {
            individualRoute.addItem(activity, routeItem, routeUpdater, addToRouteStart)
            updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility)
        })
    }

    private static Unit addMenuHelper(final SimplePopupMenu menu, final Int uniqueId, final CharSequence title, @DrawableRes final Int drawable, final Action1<MenuItem> clickAction) {
        menu.addMenuItem(uniqueId, title, drawable)
        menu.addItemClickListener(uniqueId, clickAction::call)
    }

    // elevation handling -------------------------------------------------------------------------------------------

    public static Bitmap getElevationBitmap(final Resources res, final Int markerHeight, final Double altitude) {
        val textSizeInPx: Float = ViewUtils.dpToPixel(14f)
        val width: Int = (Int) (8 * textSizeInPx)
        val height: Int = markerHeight + (Int) (2 * textSizeInPx) + 2

        if (elevationTextPaint == null) {
            elevationTextPaint = TextPaint()
            elevationTextPaint.setColor(0xff007ae3); // same as in heading indicator
            elevationTextPaint.setTextSize(textSizeInPx)
            elevationTextPaint.setAntiAlias(true)
            elevationTextPaint.setTextAlign(Paint.Align.CENTER)
        }

        if (elevationPaint == null) {
            elevationPaint = Paint()
            elevationPaint.setColor(res.getColor(R.color.osm_zoomcontrolbackground))
            elevationPaint.setStrokeCap(Paint.Cap.ROUND)
            elevationPaint.setStrokeWidth(textSizeInPx)
        }

        val bm: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        final android.graphics.Canvas canvas = android.graphics.Canvas(bm)
        val info: String = Units.formatElevation((Float) altitude)

        val textWidth: Float = elevationTextPaint.measureText(info) + 10
        val yPos: Float = height - 0.45f * textSizeInPx
        canvas.drawLine((Float) (width - textWidth) / 2, yPos, (Float) (width + textWidth) / 2, yPos, elevationPaint)

        canvas.drawText(info, (Int) (width / 2), height - 4, elevationTextPaint)

        return bm
    }
}
