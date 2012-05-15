package cgeo.geocaching;

import cgeo.geocaching.concurrent.BlockingThreadPool;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class StaticMapsProvider {
    private static final String GOOGLE_STATICMAP_URL = "http://maps.google.com/maps/api/staticmap";
    private static final String SATELLITE = "satellite";
    private static final String ROADMAP = "roadmap";
    private static final String WAYPOINT_PREFIX = "wp";
    private static final String MAP_FILENAME_PREFIX = "map_";
    private static final String MARKERS_URL = "http://cgeo.carnero.cc/_markers/";
    /** In my tests the "no image available" image had 5470 bytes, while "street only" maps had at least 20000 bytes */
    private static final int MIN_MAP_IMAGE_BYTES = 6000;
    /** ThreadPool restricting this to 1 Thread. **/
    private static final BlockingThreadPool pool = new BlockingThreadPool(1, Thread.MIN_PRIORITY);

    public static File getMapFile(final String geocode, String prefix, final boolean createDirs) {
        return LocalStorage.getStorageFile(geocode, MAP_FILENAME_PREFIX + prefix, false, createDirs);
    }

    private static void downloadDifferentZooms(final String geocode, String markerUrl, String prefix, String latlonMap, int edge, final Parameters waypoints) {
        downloadMap(geocode, 20, SATELLITE, markerUrl, prefix + "_1", "", latlonMap, edge, edge, waypoints);
        downloadMap(geocode, 18, SATELLITE, markerUrl, prefix + "_2", "", latlonMap, edge, edge, waypoints);
        downloadMap(geocode, 16, ROADMAP, markerUrl, prefix + "_3", "", latlonMap, edge, edge, waypoints);
        downloadMap(geocode, 14, ROADMAP, markerUrl, prefix + "_4", "", latlonMap, edge, edge, waypoints);
        downloadMap(geocode, 11, ROADMAP, markerUrl, prefix + "_5", "", latlonMap, edge, edge, waypoints);
    }

    private static void downloadMap(String geocode, int zoom, String mapType, String markerUrl, String prefix, String shadow, String latlonMap, int width, int height, final Parameters waypoints) {
        final Parameters params = new Parameters(
                "center", latlonMap,
                "zoom", String.valueOf(zoom),
                "size", width + "x" + height,
                "maptype", mapType,
                "markers", "icon:" + markerUrl + '|' + shadow + latlonMap,
                "sensor", "false");
        if (waypoints != null) {
            params.addAll(waypoints);
        }
        final HttpResponse httpResponse = Network.getRequest(GOOGLE_STATICMAP_URL, params);

        if (httpResponse != null) {
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                final File file = getMapFile(geocode, prefix, true);
                if (LocalStorage.saveEntityToFile(httpResponse, file)) {
                    // Delete image if it has no contents
                    final long fileSize = file.length();
                    if (fileSize < MIN_MAP_IMAGE_BYTES) {
                        file.delete();
                    }
                }
            } else {
                Log.d("StaticMapsProvider.downloadMap: httpResponseCode = " + httpResponse.getStatusLine().getStatusCode());
            }
        } else {
            Log.e("StaticMapsProvider.downloadMap: httpResponse is null");
        }
    }

    public static void downloadMaps(cgCache cache) {
        final Display display = ((WindowManager) cgeoapplication.getInstance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        downloadMaps(cache, display);
    }

    public static void downloadMaps(cgCache cache, Display display) {
        if ((!Settings.isStoreOfflineMaps() && !Settings.isStoreOfflineWpMaps()) || StringUtils.isBlank(cache.getGeocode())) {
            return;
        }

        int edge = guessMaxDisplaySide(display);

        if (Settings.isStoreOfflineMaps() && cache.getCoords() != null) {
            storeCachePreviewMap(cache);
            storeCacheStaticMap(cache, edge, false);
        }

        // download static map for current waypoints
        if (Settings.isStoreOfflineWpMaps() && CollectionUtils.isNotEmpty(cache.getWaypoints())) {
            // remove all waypoint static map files due to origin cache waypoint id changed on saveCache
            LocalStorage.deleteFilesWithPrefix(cache.getGeocode(), MAP_FILENAME_PREFIX + WAYPOINT_PREFIX);
            for (cgWaypoint waypoint : cache.getWaypoints()) {
                storeWaypointStaticMap(cache.getGeocode(), edge, waypoint, false);
            }
        }
    }

    public static void storeWaypointStaticMap(cgCache cache, Activity activity, cgWaypoint waypoint, boolean waitForResult) {
        int edge = StaticMapsProvider.guessMaxDisplaySide(activity);
        storeWaypointStaticMap(cache.getGeocode(), edge, waypoint, waitForResult);
    }

    private static void storeWaypointStaticMap(final String geocode, int edge, cgWaypoint waypoint, final boolean waitForResult) {
        if (waypoint.getCoords() == null) {
            return;
        }
        String wpLatlonMap = waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        String wpMarkerUrl = getWpMarkerUrl(waypoint);
        // download map images in separate background thread for higher performance
        downloadMaps(geocode, wpMarkerUrl, WAYPOINT_PREFIX + waypoint.getId() + "_", wpLatlonMap, edge, null, waitForResult);
    }

    public static void storeCacheStaticMap(cgCache cache, Activity activity, final boolean waitForResult) {
        int edge = guessMaxDisplaySide(activity);
        storeCacheStaticMap(cache, edge, waitForResult);
    }

    private static void storeCacheStaticMap(final cgCache cache, final int edge, final boolean waitForResult) {
        final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final Parameters waypoints = new Parameters();
        for (final cgWaypoint waypoint : cache.getWaypoints()) {
            if (waypoint.getCoords() == null) {
                continue;
            }
            final String wpMarkerUrl = getWpMarkerUrl(waypoint);
            waypoints.put("markers", "icon:" + wpMarkerUrl + "|" + waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA));
        }
        // download map images in separate background thread for higher performance
        final String cacheMarkerUrl = getCacheMarkerUrl(cache);
        downloadMaps(cache.getGeocode(), cacheMarkerUrl, "", latlonMap, edge, waypoints, waitForResult);
    }

    public static void storeCachePreviewMap(final cgCache cache) {
        final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final String markerUrl = MARKERS_URL + "my_location_mdpi.png";
        final Display display = ((WindowManager) cgeoapplication.getInstance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        final int width = metrics.widthPixels;
        final int height = (int) (110 * metrics.density);
        downloadMap(cache.getGeocode(), 15, ROADMAP, markerUrl, "preview", "shadow:false|", latlonMap, width, height, null);
    }

    private static int guessMaxDisplaySide(Display display) {
        final int maxWidth = display.getWidth() - 25;
        final int maxHeight = display.getHeight() - 25;
        if (maxWidth > maxHeight) {
            return maxWidth;
        }
        return maxHeight;
    }

    private static int guessMaxDisplaySide(Activity activity) {
        return guessMaxDisplaySide(((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
    }

    private static void downloadMaps(final String geocode, final String markerUrl, final String prefix, final String latlonMap, final int edge,
            final Parameters waypoints, boolean waitForResult) {
        if (waitForResult) {
            downloadDifferentZooms(geocode, markerUrl, prefix, latlonMap, edge, waypoints);
        }
        else {
            final Runnable currentTask = new Runnable() {
                @Override
                public void run() {
                    downloadDifferentZooms(geocode, markerUrl, prefix, latlonMap, edge, waypoints);
                }
            };
            try {
                pool.add(currentTask, 20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.e("StaticMapsProvider.downloadMaps error adding task: " + e.toString());
            }
        }
    }

    private static String getCacheMarkerUrl(final cgCache cache) {
        String type = cache.getType().id;
        if (cache.isFound()) {
            type += "_found";
        } else if (cache.isDisabled()) {
            type += "_disabled";
        }

        return MARKERS_URL + "marker_cache_" + type + ".png";
    }

    private static String getWpMarkerUrl(final cgWaypoint waypoint) {
        String type = waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null;
        return MARKERS_URL + "marker_waypoint_" + type + ".png";
    }

    public static void removeWpStaticMaps(int wp_id, final String geocode) {
        for (int level = 1; level <= 5; level++) {
            try {
                if (wp_id > 0) {
                    StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + wp_id + "_" + level, false).delete();
                }
            } catch (Exception e) {
                Log.e("StaticMapsProvider.removeWpStaticMaps: " + e.toString());
            }
        }
    }

    /**
     * Check if at least one map file exists for the given geocode.
     *
     * @param geocode
     * @return <code>true</code> if at least one mapfile exists; <code>false</code> otherwise
     */
    public static boolean doesExistStaticMapForCache(String geocode) {
        for (int level = 1; level <= 5; level++) {
            File mapFile = StaticMapsProvider.getMapFile(geocode, "" + level, false);
            if (mapFile != null && mapFile.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if at least one map file exists for the given geocode and waypoint ID.
     *
     * @param geocode
     * @param waypointId
     * @return <code>true</code> if at least one mapfile exists; <code>false</code> otherwise
     */
    public static boolean doesExistStaticMapForWaypoint(String geocode, int waypointId) {
        for (int level = 1; level <= 5; level++) {
            File mapFile = StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + waypointId + "_" + level, false);
            if (mapFile != null && mapFile.exists()) {
                return true;
            }
        }
        return false;
    }
}
