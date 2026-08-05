package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.EditWaypointActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.downloader.BRouterTileDownloader;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.downloader.HillshadingTileDownloader;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimplePopupMenu;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MenuUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;
import static cgeo.geocaching.brouter.BRouterConstants.BROUTER_TILE_FILEEXTENSION;
import static cgeo.geocaching.downloader.HillshadingTileDownloader.HILLSHADING_TILE_FILEEXTENSION;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

public class MapUtils {

    private MapUtils() {
        // should not be instantiated
    }

    private static TextPaint elevationTextPaint = null;
    private static Paint elevationPaint = null;

    public static Set<Geocache> getGeocachesFromDatabase(final Viewport viewport, final GeocacheFilter filter) {
        if (viewport == null || viewport.isJustADot()) {
            return Collections.emptySet();
        }
        final SearchResult searchResult = new SearchResult(DataStore.loadCachedInViewport(viewport.resize(1.2), filter));
        Log.d("load.searchResult: " + searchResult.getGeocodes());
        final Set<Geocache> cachesFromSearchResult = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_WAYPOINTS);
        Log.d("load.cachesFromSearchResult: " + cachesFromSearchResult.size());
        if (filter != null) {
            filter.filterList(cachesFromSearchResult);
        }
        return cachesFromSearchResult;
    }

    public static boolean mapHasMoved(final Viewport oldViewport, final Viewport newViewport) {
        if (oldViewport == newViewport) {
            return false;
        }
        if (oldViewport == null || newViewport == null) {
            return true;
        }
        return Math.abs(newViewport.getLatitudeSpan() - oldViewport.getLatitudeSpan()) > 50e-6 || Math.abs(newViewport.getLongitudeSpan() - oldViewport.getLongitudeSpan()) > 50e-6 || Math.abs(newViewport.center.getLatitude() - oldViewport.center.getLatitude()) > oldViewport.getLatitudeSpan() / 4 || Math.abs(newViewport.center.getLongitude() - oldViewport.center.getLongitude()) > oldViewport.getLongitudeSpan() / 4;
    }

    // filter waypoints from owned caches or certain wp types if requested.
    public static void filter(final Set<Waypoint> waypoints, final GeocacheFilterContext filterContext) {
        filter(waypoints, filterContext.get());
    }

    public static void filter(final Set<Waypoint> waypoints, @Nullable final GeocacheFilter filter) {

        final boolean excludeWpOriginal = Settings.isExcludeWpOriginal();
        final boolean excludeWpParking = Settings.isExcludeWpParking();
        final boolean excludeWpVisited = Settings.isExcludeWpVisited();

        final List<Waypoint> removeList = new ArrayList<>();
        for (final Waypoint wp : waypoints) {
            final Geocache cache = DataStore.loadCache(wp.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            final WaypointType wpt = wp.getWaypointType();
            if (cache == null ||
                    (filter != null && !filter.filter(cache)) ||
                    (excludeWpOriginal && wpt == WaypointType.ORIGINAL) ||
                    (excludeWpParking && wpt == WaypointType.PARKING) ||
                    (excludeWpVisited && wp.isVisited())) {
                removeList.add(wp);
            }
        }
        waypoints.removeAll(removeList);
    }

    /**
     * Applies given filter to cache list. Additionally, creates a second list additionally filtered by own/found/disabled caches if required
     */
    public static void filter(final Collection<Geocache> caches, final GeocacheFilterContext filterContext) {
        final GeocacheFilter filter = filterContext.get();
        filter.filterList(caches);
    }

    public static void updateFilterBar(final Activity activity, final GeocacheFilterContext filterContext) {
        FilterUtils.updateFilterBar(activity, getActiveMapFilterName(filterContext), getActiveMapFilterSavedDifferently(filterContext));
    }

    @Nullable
    private static String getActiveMapFilterName(final GeocacheFilterContext filterContext) {
        final GeocacheFilter filter = filterContext.get();
        if (filter.isFiltering()) {
            return filter.toUserDisplayableString();
        }
        return null;
    }

    @Nullable
    private static Boolean getActiveMapFilterSavedDifferently(final GeocacheFilterContext filterContext) {
        final GeocacheFilter filter = filterContext.get();
        if (filter.isFiltering()) {
            return filter.isSavedDifferently();
        }
        return null;
    }

    // one-time messages to be shown for maps
    public static void showMapOneTimeMessages(final Activity activity, final MapMode mapMode) {
        Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_QUICK_SETTINGS);
        if (mapMode == MapMode.LIVE && !Settings.isLiveMap()) {
            Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_LIVE_DISABLED);
        }
    }

    // workaround for colored ActionBar titles/subtitles
    // @todo remove after switching map ActionBar to Toolbar
    public static Spanned getColoredValue(final String value) {
        return Html.fromHtml("<font color=\"" + String.format("#%06X", 0xFFFFFF & CgeoApplication.getInstance().getResources().getColor(R.color.colorTextActionBar)) + "\">" + value + "</font>");
    }

    // check whether routing tile data is available for the whole viewport given
    // and offer to download missing routing data
    public static void checkRoutingData(final Activity activity, final double minLatitude, final double minLongitude, final double maxLatitude, final double maxLongitude) {
        if (Settings.useInternalRouting()) {
            ActivityMixin.showToast(activity, R.string.downloadmap_checking);

            final HashMap<String, String> requiredTiles = new HashMap<>();
            final ArrayList<Download> missingDownloads = new ArrayList<>();
            final AtomicBoolean hasUnsupportedTiles = new AtomicBoolean(false);

            AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
                // calculate affected routing tiles
                int curLat = (int) Math.floor(minLatitude / 5) * 5;
                final int maxLat = (int) Math.floor(maxLatitude / 5) * 5;
                final int maxLon = (int) Math.floor(maxLongitude / 5) * 5;
                while (curLat <= maxLat) {
                    int curLon = (int) Math.floor(minLongitude / 5) * 5;
                    while (curLon <= maxLon) {
                        final String filenameBase = (curLon < 0 ? "W" + (-curLon) : "E" + curLon) + "_" + (curLat < 0 ? "S" + (-curLat) : "N" + curLat) + BROUTER_TILE_FILEEXTENSION;
                        requiredTiles.put(filenameBase, filenameBase);
                        curLon += 5;
                    }
                    curLat += 5;
                }
                checkTiles(requiredTiles, missingDownloads, hasUnsupportedTiles);
            }, () -> {
                // give feedback to the user + offer to download missing tiles (if available)
                if (missingDownloads.isEmpty()) {
                    ActivityMixin.showShortToast(activity, hasUnsupportedTiles.get() ? R.string.check_tiles_unsupported : R.string.check_tiles_found);
                } else {
                    if (hasUnsupportedTiles.get()) {
                        ActivityMixin.showShortToast(activity, R.string.check_tiles_unsupported);
                    }
                    DownloaderUtils.triggerDownloads(activity, R.string.downloadtile_title, R.string.check_tiles_missing, missingDownloads, null);
                }
            });
        } else {
            // if external routing configured: try to open BRouter downloader
            try {
                final String bRouterPackage = activity.getString(R.string.package_brouter);
                final Intent intent = new Intent();
                intent.setComponent(new ComponentName(bRouterPackage, bRouterPackage + ".BInstallerActivity"));
                activity.startActivity(intent);
            } catch (ActivityNotFoundException ignore) {
                ActivityMixin.showShortToast(activity, R.string.cache_not_status_found);
            }
        }
    }

    // check whether hillshading tile data is available for the whole viewport given
    // and offer to download missing hillshading data
    public static void checkHillshadingData(final Activity activity, final double minLatitude, final double minLongitude, final double maxLatitude, final double maxLongitude) {
        ActivityMixin.showToast(activity, R.string.downloadmap_checking);

        final HashMap<String, String> requiredTiles = new HashMap<>();
        final ArrayList<Download> missingDownloads = new ArrayList<>();
        final AtomicBoolean hasUnsupportedTiles = new AtomicBoolean(false);

        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
            // calculate affected routing tiles
            int curLat = (int) Math.floor(minLatitude);
            final int maxLat = (int) Math.floor(maxLatitude);
            final int maxLon = (int) Math.floor(maxLongitude);
            while (curLat <= maxLat) {
                int curLon = (int) Math.floor(minLongitude);
                while (curLon <= maxLon) {
                    final String curLat02d = String.format(Locale.US, "%02d", Math.abs(curLat));
                    final String filenameBase = (curLat < 0 ? "S" : "N") + curLat02d + (curLon < 0 ? "W" : "E") + String.format(Locale.US, "%03d", Math.abs(curLon)) + HILLSHADING_TILE_FILEEXTENSION;
                    final String dirName = (curLat < 0 ? "S" : "N") + curLat02d;
                    requiredTiles.put(filenameBase, dirName);
                    curLon += 1;
                }
                curLat += 1;
            }
            checkHillshadingTiles(requiredTiles, missingDownloads, hasUnsupportedTiles);
        }, () -> {
            // give feedback to the user + offer to download missing tiles (if available)
            if (missingDownloads.isEmpty()) {
                ActivityMixin.showShortToast(activity, hasUnsupportedTiles.get() ? R.string.check_hillshading_unsupported : R.string.check_hillshading_found);
            } else {
                if (hasUnsupportedTiles.get()) {
                    ActivityMixin.showShortToast(activity, R.string.check_hillshading_unsupported);
                }
                DownloaderUtils.triggerDownloads(activity, R.string.downloadtile_title, R.string.check_hillshading_missing, missingDownloads, null);
            }
        });
    }

    @WorkerThread
    private static void checkTiles(final HashMap<String, String> requiredTiles, final ArrayList<Download> missingDownloads, final AtomicBoolean hasUnsupportedTiles) {
        // read tiles already stored
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.ROUTING_TILES.getFolder());
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.endsWith(BROUTER_TILE_FILEEXTENSION)) {
                requiredTiles.remove(fi.name);
            }
        }

        // read list of available tiles from the server, if necessary
        if (!requiredTiles.isEmpty()) {
            final HashMap<String, Download> tiles = BRouterTileDownloader.getInstance().getAvailableTiles();
            final ArrayList<String> filenames = new ArrayList<>(requiredTiles.values()); // work on copy to avoid concurrent modification
            for (String filename : filenames) {
                if (tiles.containsKey(filename)) {
                    missingDownloads.add(tiles.get(filename));
                } else {
                    requiredTiles.remove(filename);
                    hasUnsupportedTiles.set(true);
                }
            }
        }
    }

    @WorkerThread
    private static void checkHillshadingTiles(final HashMap<String, String> requiredTiles, final ArrayList<Download> missingDownloads, final AtomicBoolean hasUnsupportedTiles) {
        // read tiles already stored
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.OFFLINE_MAP_SHADING.getFolder());
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.endsWith(HILLSHADING_TILE_FILEEXTENSION)) {
                requiredTiles.remove(fi.name);
            }
        }

        // read list of available tiles from the server, if necessary
        if (!requiredTiles.isEmpty()) {
            final Set<String> missingTileFolders = new HashSet<>(requiredTiles.values());
            final HashMap<String, Download> tiles = HillshadingTileDownloader.getInstance().getAvailableTiles(missingTileFolders);
            final Set<String> filenames = new HashSet<>(requiredTiles.keySet()); // work on copy to avoid concurrent modification
            for (String filename : filenames) {
                if (tiles.containsKey(filename)) {
                    missingDownloads.add(tiles.get(filename));
                } else {
                    requiredTiles.remove(filename);
                    hasUnsupportedTiles.set(true);
                }
            }
        }
    }

    public static boolean hasHillshadingTiles() {
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.OFFLINE_MAP_SHADING.getFolder());
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.endsWith(HILLSHADING_TILE_FILEEXTENSION)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the complete popup builder without dismiss listener specified
     */
    public static SimplePopupMenu createMapLongClickPopupMenu(final Activity activity, final Geopoint longClickGeopoint, final Point tapXY, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility, final Geocache currentTargetCache, final MapOptions mapOptions, final Action2<Geopoint, String> setTarget) {
        final int offset = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.map_pin, null).getIntrinsicHeight() / 2;

        return SimplePopupMenu.of(activity)
                .setMenuContent(R.menu.map_longclick)
                .setPosition(new Point(tapXY.x, tapXY.y - offset), (int) (offset * 1.25))
                .setOnCreatePopupMenuListener(menu -> {
                    MenuUtils.setVisible(menu.findItem(R.id.menu_add_waypoint), currentTargetCache != null);
                    MenuUtils.setVisible(menu.findItem(R.id.menu_add_to_route_start), individualRoute.getNumPoints() > 0);
                })
                .addItemClickListener(R.id.menu_udc, item -> InternalConnector.interactiveCreateCache(activity, longClickGeopoint, mapOptions.fromList, true))
                .addItemClickListener(R.id.menu_add_waypoint, item -> EditWaypointActivity.startActivityAddWaypoint(activity, currentTargetCache, longClickGeopoint))
                .addItemClickListener(R.id.menu_coords, item -> {
                    final AtomicReference<TextView> textview = new AtomicReference<>();
                    final AlertDialog dialog = Dialogs.newBuilder(activity)
                            .setTitle(R.string.selected_position)
                            .setView(R.layout.dialog_selected_position)
                            .setPositiveButton(R.string.ok, null)
                            .setNegativeButton(R.string.copy_coordinates, (d, which) -> {
                                ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(textview.get().getText()));
                                ViewUtils.showShortToast(activity, R.string.clipboard_copy_ok);
                            })
                            .show();
                    final TextView tv1 = dialog.findViewById(R.id.tv1);
                    assert tv1 != null;
                    textview.set(tv1);
                    tv1.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.compass_rose_mini, 0, 0, 0);
                    tv1.setCompoundDrawablePadding(ViewUtils.dpToPixel(10));
                    TooltipCompat.setTooltipText(tv1, tv1.getContext().getString(R.string.selected_position));
                    new CoordinatesFormatSwitcher().setView(textview.get()).setCoordinate(longClickGeopoint);

                    final Geopoint currentPosition = LocationDataProvider.getInstance().currentGeo().getCoords();
                    final float distance = longClickGeopoint.distanceTo(currentPosition);
                    TextParam.text(Units.getDistanceFromKilometers(distance)).setImage(ImageParam.id(R.drawable.routing_straight)).setTooltip(R.string.distance).applyTo(dialog.findViewById(R.id.tv2));

                    final float elevation = Routing.getElevation(longClickGeopoint);
                    if (!Float.isNaN(elevation)) {
                        TextParam.text(Units.formatElevation(elevation)).setImage(ImageParam.id(R.drawable.elevation)).setTooltip(R.string.elevation_selected).applyTo(dialog.findViewById(R.id.tv4));

                        final float elevationCurrent = Routing.getElevation(currentPosition);
                        if (!Float.isNaN(elevationCurrent)) {
                            TextParam.text(Units.formatElevation(elevation - elevationCurrent)).setImage(ImageParam.id(R.drawable.height)).setTooltip(R.string.elevation_difference).applyTo(dialog.findViewById(R.id.tv3));
                        }
                    }

                })
                .addItemClickListener(R.id.menu_add_to_route, item -> {
                    individualRoute.toggleItem(activity, new RouteItem(longClickGeopoint), routeUpdater, false);
                    updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility);
                })
                .addItemClickListener(R.id.menu_add_to_route_start, item -> {
                    individualRoute.toggleItem(activity, new RouteItem(longClickGeopoint), routeUpdater, true);
                    updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility);
                })
                .addItemClickListener(R.id.menu_target, item -> {
                    setTarget.call(longClickGeopoint, null);
                    updateRouteTrackButtonVisibility.run();
                })
                .addItemClickListener(R.id.menu_navigate, item -> NavigationAppFactory.showNavigationMenu(activity, null, null, longClickGeopoint, false, true, 0));
    }

    private static void updateRouteTrackButtonVisibility(final Runnable updateRouteTrackButtonVisibility) {
        if (updateRouteTrackButtonVisibility != null) {
            updateRouteTrackButtonVisibility.run();
        }
    }

    public static SimplePopupMenu createEmptyLongClickPopupMenu(final Activity activity, final int tapX, final int tapY) {
        return SimplePopupMenu.of(activity).setPosition(new Point(tapX, tapY), 0);
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public static SimplePopupMenu createCacheWaypointLongClickPopupMenu(final Activity activity, final RouteItem routeItem, final int tapX, final int tapY, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        final SimplePopupMenu menu = createEmptyLongClickPopupMenu(activity, tapX, tapY);
        addCacheWaypointLongClickPopupMenu(menu, activity, routeItem, individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
        return menu;
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public static void addCacheWaypointLongClickPopupMenu(final SimplePopupMenu menu, final Activity activity, final RouteItem routeItem, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {

        final RouteSegment[] segments = (individualRoute != null ? individualRoute.getSegments() : null);
        final boolean routeIsNotEmpty = segments != null && segments.length > 0;
        final int baseId = (segments != null ? segments.length : 0) + 100;

        boolean isEnd = false;

        if (routeIsNotEmpty) {
            final String routeItemIdentifier = routeItem.getIdentifier();
            final boolean isStart = StringUtils.equals(routeItemIdentifier, segments[0].getItem().getIdentifier());
            if (isStart) {
                addMenuHelper(activity, menu, 0, activity.getString(R.string.context_map_remove_from_route_start), individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
            }
            for (int i = 1; i < segments.length - 1; i++) {
                if (StringUtils.equals(routeItemIdentifier, segments[i].getItem().getIdentifier())) {
                    addMenuHelper(activity, menu, i, String.format(Locale.getDefault(), activity.getString(R.string.context_map_remove_from_route_pos), i + 1), individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
                }
            }
            isEnd = (segments.length > 1) && StringUtils.equals(routeItemIdentifier, segments[segments.length - 1].getItem().getIdentifier());
            if (isEnd) {
                addMenuHelper(activity, menu, segments.length - 1, activity.getString(R.string.context_map_remove_from_route_end), individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
            } else {
                addMenuHelper(activity, menu, baseId + 1, activity.getString(R.string.context_map_add_to_route), routeItem, false, individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
            }
            if (!isStart) {
                addMenuHelper(activity, menu, baseId, activity.getString(R.string.context_map_add_to_route_start), routeItem, true, individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
            }
        } else {
            addMenuHelper(activity, menu, baseId + 1, activity.getString(R.string.context_map_add_to_route), routeItem, false, individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
        }

        final float elevation = Routing.getElevation(routeItem.getPoint());
        if (!Float.isNaN(elevation)) {
            menu.addMenuItem(baseId + 100, activity.getString(R.string.menu_elevation_info) + " " + Units.formatElevation(elevation), R.drawable.elevation);
        }
    }

    private static void addMenuHelper(final Activity activity, final SimplePopupMenu menu, final int uniqueId, final CharSequence title, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        addMenuHelper(menu, uniqueId, title, R.drawable.ic_menu_delete,  item -> {
            individualRoute.removeItem(activity, uniqueId, routeUpdater);
            updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility);
        });
    }

    private static void addMenuHelper(final Activity activity, final SimplePopupMenu menu, final int uniqueId, final CharSequence title, final RouteItem routeItem, final boolean addToRouteStart, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        addMenuHelper(menu, uniqueId, title, R.drawable.ic_menu_add, item -> {
            individualRoute.addItem(activity, routeItem, routeUpdater, addToRouteStart);
            updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility);
        });
    }

    private static void addMenuHelper(final SimplePopupMenu menu, final int uniqueId, final CharSequence title, @DrawableRes final int drawable, final Action1<MenuItem> clickAction) {
        menu.addMenuItem(uniqueId, title, drawable);
        menu.addItemClickListener(uniqueId, clickAction::call);
    }

    // elevation handling -------------------------------------------------------------------------------------------

    public static Bitmap getElevationBitmap(final Resources res, final int markerHeight, final double altitude) {
        final float textSizeInPx = ViewUtils.dpToPixel(14f);
        final int width = (int) (8 * textSizeInPx);
        final int height = markerHeight + (int) (2 * textSizeInPx) + 2;

        if (elevationTextPaint == null) {
            elevationTextPaint = new TextPaint();
            elevationTextPaint.setColor(0xff007ae3); // same as in heading indicator
            elevationTextPaint.setTextSize(textSizeInPx);
            elevationTextPaint.setAntiAlias(true);
            elevationTextPaint.setTextAlign(Paint.Align.CENTER);
        }

        if (elevationPaint == null) {
            elevationPaint = new Paint();
            elevationPaint.setColor(res.getColor(R.color.osm_zoomcontrolbackground));
            elevationPaint.setStrokeCap(Paint.Cap.ROUND);
            elevationPaint.setStrokeWidth(textSizeInPx);
        }

        final Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final android.graphics.Canvas canvas = new android.graphics.Canvas(bm);
        final String info = Units.formatElevation((float) altitude);

        final float textWidth = elevationTextPaint.measureText(info) + 10;
        final float yPos = height - 0.45f * textSizeInPx;
        canvas.drawLine((float) (width - textWidth) / 2, yPos, (float) (width + textWidth) / 2, yPos, elevationPaint);

        canvas.drawText(info, (int) (width / 2), height - 4, elevationTextPaint);

        return bm;
    }
}
