package cgeo.geocaching;

import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;

public class StaticMapsProvider {
    private static final String MARKERS_URL = "http://cgeo.carnero.cc/_markers/";
    /**
     * in my tests the "no image available" image had 5470 bytes, while "street only" maps had at least 20000 bytes
     */
    private static final int MIN_MAP_IMAGE_BYTES = 6000;

    public static File getMapFile(final String geocode, String prefix, final int level, final boolean createDirs) {
        return LocalStorage.getStorageFile(geocode, "map_" + prefix + level, false, createDirs);
    }

    private static void downloadMapsInThread(final cgCache cache, String markerUrl, String prefix, String latlonMap, int edge, String waypoints) {
        downloadMap(cache, 20, "satellite", markerUrl, prefix, 1, latlonMap, edge, waypoints);
        downloadMap(cache, 18, "satellite", markerUrl, prefix, 2, latlonMap, edge, waypoints);
        downloadMap(cache, 16, "roadmap", markerUrl, prefix, 3, latlonMap, edge, waypoints);
        downloadMap(cache, 14, "roadmap", markerUrl, prefix, 4, latlonMap, edge, waypoints);
        downloadMap(cache, 11, "roadmap", markerUrl, prefix, 5, latlonMap, edge, waypoints);
    }

    private static void downloadMap(cgCache cache, int zoom, String mapType, String markerUrl, String prefix, int level, String latlonMap, int edge, String waypoints) {
        final String mapUrl = "http://maps.google.com/maps/api/staticmap?center=" + latlonMap;
        final String url = mapUrl + "&zoom=" + zoom + "&size=" + edge + "x" + edge + "&maptype=" + mapType + "&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints + "&sensor=false";
        final File file = getMapFile(cache.getGeocode(), prefix, level, true);
        final HttpResponse httpResponse = cgBase.request(url, null, false);

        if (httpResponse != null) {
            if (LocalStorage.saveEntityToFile(httpResponse.getEntity(), file)) {
                // Delete image if it has no contents
                final long fileSize = file.length();
                if (fileSize < MIN_MAP_IMAGE_BYTES) {
                    file.delete();
                }
            }
        }
    }

    public static void downloadMaps(cgCache cache, Activity activity) {
        if (!Settings.isStoreOfflineMaps() || cache.getCoords() == null || StringUtils.isBlank(cache.getGeocode())) {
            return;
        }

        final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int maxWidth = display.getWidth() - 25;
        final int maxHeight = display.getHeight() - 25;
        int edge = 0;
        if (maxWidth > maxHeight) {
            edge = maxWidth;
        } else {
            edge = maxHeight;
        }

        final StringBuilder waypoints = new StringBuilder();
        if (cache.hasWaypoints()) {
            for (cgWaypoint waypoint : cache.getWaypoints()) {
                if (waypoint.getCoords() == null) {
                    continue;
                }
                String wpMarkerUrl = getWpMarkerUrl(waypoint);
                waypoints.append("&markers=icon%3A");
                waypoints.append(wpMarkerUrl);
                waypoints.append("%7C");
                waypoints.append(waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA));
            }
        }

        // download map images in separate background thread for higher performance
        final String cacheMarkerUrl = getCacheMarkerUrl(cache);
        downloadMaps(cache, cacheMarkerUrl, "", latlonMap, edge, waypoints.toString());

        // download static map for current waypoints
        if (!Settings.isStoreOfflineWpMaps()) {
            return;
        }
        if (CollectionUtils.isNotEmpty(cache.getWaypoints())) {
            for (cgWaypoint waypoint : cache.getWaypoints()) {
                if (waypoint.getCoords() == null) {
                    continue;
                }
                String wpLatlonMap = waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
                String wpMarkerUrl = getWpMarkerUrl(waypoint);
                downloadMaps(cache, wpMarkerUrl, "wp" + waypoint.getId() + "_", wpLatlonMap, edge, "");
            }
        }
    }

    private static void downloadMaps(final cgCache cache, final String markerUrl, final String prefix, final String latlonMap, final int edge,
            final String waypoints) {
        Thread staticMapsThread = new Thread("getting static map") {
            @Override
            public void run() {
                downloadMapsInThread(cache, markerUrl, prefix, latlonMap, edge, waypoints);
            }
        };
        staticMapsThread.setPriority(Thread.MIN_PRIORITY);
        staticMapsThread.start();
    }

    private static String getCacheMarkerUrl(final cgCache cache) {
        String type = cache.getType().id;
        if (cache.isFound()) {
            type += "_found";
        } else if (cache.isDisabled()) {
            type += "_disabled";
        }

        return cgBase.urlencode_rfc3986(MARKERS_URL + "marker_cache_" + type + ".png");
    }

    private static String getWpMarkerUrl(final cgWaypoint waypoint) {
        String type = waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null;
        return cgBase.urlencode_rfc3986(MARKERS_URL + "marker_waypoint_" + type + ".png");
    }
}
