package cgeo.geocaching;

import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.concurrent.BlockingThreadPool;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class StaticMapsProvider {
    private static final String PREFIX_PREVIEW = "preview";
    private static final String GOOGLE_STATICMAP_URL = "http://maps.google.com/maps/api/staticmap";
    private static final String SATELLITE = "satellite";
    private static final String ROADMAP = "roadmap";
    private static final String WAYPOINT_PREFIX = "wp";
    private static final String MAP_FILENAME_PREFIX = "map_";
    private static final String MARKERS_URL = "http://status.cgeo.org/assets/markers/";
    /** We assume there is no real usable image with less than 1k */
    private static final int MIN_MAP_IMAGE_BYTES = 1000;
    /** ThreadPool restricting this to 1 Thread. **/
    private static final BlockingThreadPool pool = new BlockingThreadPool(1, Thread.MIN_PRIORITY);

    private static File getMapFile(final String geocode, String prefix, final boolean createDirs) {
        return LocalStorage.getStorageFile(geocode, MAP_FILENAME_PREFIX + prefix, false, createDirs);
    }

    private static void downloadDifferentZooms(final String geocode, String markerUrl, String prefix, String latlonMap, int edge, final Parameters waypoints) {
        downloadMap(geocode, 20, SATELLITE, markerUrl, prefix + '1', "", latlonMap, edge, edge, waypoints);
        downloadMap(geocode, 18, SATELLITE, markerUrl, prefix + '2', "", latlonMap, edge, edge, waypoints);
        downloadMap(geocode, 16, ROADMAP, markerUrl, prefix + '3', "", latlonMap, edge, edge, waypoints);
        downloadMap(geocode, 14, ROADMAP, markerUrl, prefix + '4', "", latlonMap, edge, edge, waypoints);
        downloadMap(geocode, 11, ROADMAP, markerUrl, prefix + '5', "", latlonMap, edge, edge, waypoints);
    }

    private static void downloadMap(String geocode, int zoom, String mapType, String markerUrl, String prefix, String shadow, String latlonMap, int width, int height, final Parameters waypoints) {
        final Parameters params = new Parameters(
                "center", latlonMap,
                "zoom", String.valueOf(zoom),
                "size", String.valueOf(width) + 'x' + String.valueOf(height),
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
        if (cache == null) {
            Log.e("downloadMaps - missing input parameter cache");
            return;
        }
        if ((!Settings.isStoreOfflineMaps() && !Settings.isStoreOfflineWpMaps()) || StringUtils.isBlank(cache.getGeocode())) {
            return;
        }
        int edge = guessMaxDisplaySide();

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

    public static void storeWaypointStaticMap(cgCache cache, cgWaypoint waypoint, boolean waitForResult) {
        int edge = StaticMapsProvider.guessMaxDisplaySide();
        storeWaypointStaticMap(cache.getGeocode(), edge, waypoint, waitForResult);
    }

    private static void storeWaypointStaticMap(final String geocode, int edge, cgWaypoint waypoint, final boolean waitForResult) {
        if (geocode == null) {
            Log.e("storeWaypointStaticMap - missing input parameter geocode");
            return;
        }
        if (waypoint == null) {
            Log.e("storeWaypointStaticMap - missing input parameter waypoint");
            return;
        }
        if (waypoint.getCoords() == null) {
            return;
        }
        String wpLatlonMap = waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        String wpMarkerUrl = getWpMarkerUrl(waypoint);
        // download map images in separate background thread for higher performance
        downloadMaps(geocode, wpMarkerUrl, WAYPOINT_PREFIX + waypoint.getId() + '_', wpLatlonMap, edge, null, waitForResult);
    }

    public static void storeCacheStaticMap(cgCache cache, final boolean waitForResult) {
        int edge = guessMaxDisplaySide();
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
            waypoints.put("markers", "icon:" + wpMarkerUrl + '|' + waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA));
        }
        // download map images in separate background thread for higher performance
        final String cacheMarkerUrl = getCacheMarkerUrl(cache);
        downloadMaps(cache.getGeocode(), cacheMarkerUrl, "", latlonMap, edge, waypoints, waitForResult);
    }

    public static void storeCachePreviewMap(final cgCache cache) {
        if (cache == null) {
            Log.e("storeCachePreviewMap - missing input parameter cache");
            return;
        }
        final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final String markerUrl = MARKERS_URL + "my_location_mdpi.png";
        final Display display = ((WindowManager) cgeoapplication.getInstance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        final int width = metrics.widthPixels;
        final int height = (int) (110 * metrics.density);
        downloadMap(cache.getGeocode(), 15, ROADMAP, markerUrl, PREFIX_PREVIEW, "shadow:false|", latlonMap, width, height, null);
    }

    private static int guessMaxDisplaySide() {
        Point displaySize = Compatibility.getDisplaySize();
        final int maxWidth = displaySize.x - 25;
        final int maxHeight = displaySize.y - 25;
        if (maxWidth > maxHeight) {
            return maxWidth;
        }
        return maxHeight;
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
        StringBuilder url = new StringBuilder(MARKERS_URL);
        url.append("marker_cache_").append(cache.getType().id);
        if (cache.isFound()) {
            url.append("_found");
        } else if (cache.isDisabled()) {
            url.append("_disabled");
        }
        url.append(".png");
        return url.toString();
    }

    private static String getWpMarkerUrl(final cgWaypoint waypoint) {
        String type = waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null;
        return MARKERS_URL + "marker_waypoint_" + type + ".png";
    }

    public static void removeWpStaticMaps(int wp_id, final String geocode) {
        if (wp_id <= 0) {
            return;
        }
        for (int level = 1; level <= 5; level++) {
            try {
                StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + wp_id + '_' + level, false).delete();
            } catch (Exception e) {
                Log.e("StaticMapsProvider.removeWpStaticMaps: " + e.toString());
            }
        }
    }

    /**
     * Check if at least one map file exists for the given cache.
     *
     * @param cache
     * @return <code>true</code> if at least one mapfile exists; <code>false</code> otherwise
     */
    public static boolean hasStaticMap(final cgCache cache) {
        if (cache == null) {
            return false;
        }
        final String geocode = cache.getGeocode();
        if (StringUtils.isBlank(geocode)) {
            return false;
        }
        for (int level = 1; level <= 5; level++) {
            File mapFile = StaticMapsProvider.getMapFile(geocode, String.valueOf(level), false);
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
    public static boolean hasStaticMapForWaypoint(String geocode, int waypointId) {
        for (int level = 1; level <= 5; level++) {
            File mapFile = StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + waypointId + "_" + level, false);
            if (mapFile != null && mapFile.exists()) {
                return true;
            }
        }
        return false;
    }

    public static Bitmap getPreviewMap(final String geocode) {
        return decodeFile(StaticMapsProvider.getMapFile(geocode, PREFIX_PREVIEW, false));
    }

    public static Bitmap getWaypointMap(final String geocode, int waypoint_id, int level) {
        return decodeFile(StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + waypoint_id + "_" + level, false));
    }

    public static Bitmap getCacheMap(final String geocode, int level) {
        return decodeFile(StaticMapsProvider.getMapFile(geocode, String.valueOf(level), false));
    }

    private static Bitmap decodeFile(final File mapFile) {
        // avoid exception in system log, if we got nothing back from Google.
        if (mapFile.exists()) {
            return BitmapFactory.decodeFile(mapFile.getPath());
        }
        return null;
    }

}
