package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.EditWaypointActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.downloader.BRouterTileDownloader;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimplePopupMenu;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.FilterUtils;
import static cgeo.geocaching.brouter.BRouterConstants.BROUTER_TILE_FILEEXTENSION;

import android.app.Activity;
import android.graphics.Point;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MapUtils {

    private MapUtils() {
        // should not be instantiated
    }

    // filter waypoints from owned caches or certain wp types if requested.
    public static void filter(final Set<Waypoint> waypoints, final GeocacheFilterContext filterContext) {

        final GeocacheFilter filter = filterContext.get();

        final boolean excludeWpOriginal = Settings.isExcludeWpOriginal();
        final boolean excludeWpParking = Settings.isExcludeWpParking();
        final boolean excludeWpVisited = Settings.isExcludeWpVisited();

        final List<Waypoint> removeList = new ArrayList<>();
        for (final Waypoint wp : waypoints) {
            final Geocache cache = DataStore.loadCache(wp.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            final WaypointType wpt = wp.getWaypointType();
            if (cache == null ||
                    !filter.filter(cache) ||
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
        FilterUtils.updateFilterBar(activity, getActiveMapFilterName(filterContext));
    }

    @Nullable
    private static String getActiveMapFilterName(final GeocacheFilterContext filterContext) {
        final GeocacheFilter filter = filterContext.get();
        if (filter.isFiltering()) {
            return filter.toUserDisplayableString();
        }
        return null;
    }

    // one-time messages to be shown for maps
    public static void showMapOneTimeMessages(final Activity activity, final MapMode mapMode) {
        Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_QUICK_SETTINGS);
        Dialogs.basicOneTimeMessage(activity, Settings.isLongTapOnMapActivated() ? OneTimeDialogs.DialogType.MAP_LONG_TAP_ENABLED : OneTimeDialogs.DialogType.MAP_LONG_TAP_DISABLED);
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
        ActivityMixin.showToast(activity, "Checking available routing data...");

        final HashMap<String, String> missingTiles = new HashMap<>();
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
                    missingTiles.put(filenameBase, filenameBase);
                    curLon += 5;
                }
                curLat += 5;
            }
            checkTiles(missingTiles, missingDownloads, hasUnsupportedTiles);
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
    }

    @WorkerThread
    private static void checkTiles(final HashMap<String, String> missingTiles, final ArrayList<Download> missingDownloads, final AtomicBoolean hasUnsupportedTiles) {
        // read tiles already stored
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.ROUTING_TILES.getFolder());
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.endsWith(BROUTER_TILE_FILEEXTENSION)) {
                missingTiles.remove(fi.name);
            }
        }

        // read list of available tiles from the server, if necessary
        if (!missingTiles.isEmpty()) {
            final HashMap<String, Download> tiles = BRouterTileDownloader.getInstance().getAvailableTiles();
            final ArrayList<String> filenames = new ArrayList<>(missingTiles.values()); // work on copy to avoid concurrent modification
            for (String filename : filenames) {
                if (tiles.containsKey(filename)) {
                    missingDownloads.add(tiles.get(filename));
                } else {
                    missingTiles.remove(filename);
                    hasUnsupportedTiles.set(true);
                }
            }
        }
    }

    /**
     * @return the complete popup builder without dismiss listener specified
     */
    public static SimplePopupMenu createMapLongClickPopupMenu(final Activity activity, final Geopoint longClickGeopoint, final int tapX, final int tapY, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility, final Geocache currentTargetCache, final MapOptions mapOptions) {
        final int offset = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.map_pin, null).getIntrinsicHeight() / 2;

        return SimplePopupMenu.of(activity)
                .setMenuContent(R.menu.map_longclick)
                .setPosition(new Point(tapX, tapY - offset), (int) (offset * 1.25))
                .setOnCreatePopupMenuListener(menu -> {
                    menu.findItem(R.id.menu_add_waypoint).setVisible(currentTargetCache != null);
                    menu.findItem(R.id.menu_add_to_route_start).setVisible(individualRoute.getNumPoints() > 0);
                })
                .addItemClickListener(R.id.menu_udc, item -> InternalConnector.interactiveCreateCache(activity, longClickGeopoint, mapOptions.fromList, true))
                .addItemClickListener(R.id.menu_add_waypoint, item -> EditWaypointActivity.startActivityAddWaypoint(activity, currentTargetCache, longClickGeopoint))
                .addItemClickListener(R.id.menu_coords, item -> {
                    final AtomicReference<TextView> textview = new AtomicReference<>();
                    final AlertDialog dialog = Dialogs.newBuilder(activity)
                            .setTitle(R.string.waypoint_coordinates)
                            .setMessage("") // set a dummy message so that the textview gets created
                            .setPositiveButton(R.string.ok, null)
                            .setNegativeButton(android.R.string.copy, (d, which) -> {
                                ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(textview.get().getText()));
                                Toast.makeText(activity, R.string.clipboard_copy_ok, Toast.LENGTH_SHORT).show();
                            })
                            .show();
                    textview.set(dialog.findViewById(android.R.id.message));
                    new CoordinatesFormatSwitcher().setView(textview.get()).setCoordinate(longClickGeopoint);
                })
                .addItemClickListener(R.id.menu_add_to_route, item -> {
                    individualRoute.toggleItem(activity, new RouteItem(longClickGeopoint), routeUpdater, false);
                    updateRouteTrackButtonVisibility.run();
                })
                .addItemClickListener(R.id.menu_add_to_route_start, item -> {
                    individualRoute.toggleItem(activity, new RouteItem(longClickGeopoint), routeUpdater, true);
                    updateRouteTrackButtonVisibility.run();
                })
                .addItemClickListener(R.id.menu_navigate, item -> NavigationAppFactory.showNavigationMenu(activity, null, null, longClickGeopoint, false, true));
    }
}
