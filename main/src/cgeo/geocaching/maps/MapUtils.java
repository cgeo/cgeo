package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.downloader.BRouterTileDownloader;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.brouter.BRouterConstants.BROUTER_TILE_FILEEXTENSION;

import android.app.Activity;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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

    /** Applies given filter to cache list. Additionally, creates a second list additionally filtered by own/found/disabled caches if required */
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

        final HashMap<String, String> existingTiles = new HashMap<>();
        final HashMap<String, String> missingTiles = new HashMap<>();
        final ArrayList<Download> missingDownloads = new ArrayList<>();

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

            // read tiles already stored
            final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.ROUTING_TILES.getFolder());
            for (ContentStorage.FileInformation fi : files) {
                if (fi.name.endsWith(BROUTER_TILE_FILEEXTENSION) && missingTiles.containsKey(fi.name)) {
                    existingTiles.put(fi.name, fi.name);
                    missingTiles.remove(fi.name);
                }
            }

            // read list of available tiles from the server, if necessary
            if (!missingTiles.isEmpty()) {
                final HashMap<String, Download> tiles = BRouterTileDownloader.getInstance().getAvailableTiles();
                for (String filename : missingTiles.values()) {
                    Log.e("checking " + filename + ": " + (tiles.containsKey(filename) ? "available for download" : "not available!"));
                    if (tiles.containsKey(filename)) {
                        missingDownloads.add(tiles.get(filename));
                    } else {
                        missingTiles.remove(filename);
                    }
                }
            }

        }, () -> {
            if (missingDownloads.isEmpty()) {
                ActivityMixin.showShortToast(activity, R.string.check_tiles_found);
            } else {
                DownloaderUtils.triggerDownloads(activity, R.string.downloadtile_title, R.string.check_tiles_missing, missingDownloads);
            }
        });
    }
}
