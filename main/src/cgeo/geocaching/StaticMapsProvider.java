package cgeo.geocaching;

import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.utils.CollectionUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.util.Locale;

public class StaticMapsProvider {
    private static final String MARKERS_URL = "http://cgeo.carnero.cc/_markers/";
    /**
     * in my tests the "no image available" image had 5470 bytes, while "street only" maps had at least 20000 bytes
     */
    private static final int MIN_MAP_IMAGE_BYTES = 6000;

    public static File getMapFile(final String geocode, final int level) {
        return LocalStorage.getStorageFile(geocode, "map_" + level, false);
    }

    private static void downloadMapsInThread(final cgCache cache, String latlonMap, int edge, String waypoints) {
        downloadMap(cache, 20, "satellite", 1, latlonMap, edge, waypoints);
        downloadMap(cache, 18, "satellite", 2, latlonMap, edge, waypoints);
        downloadMap(cache, 16, "roadmap", 3, latlonMap, edge, waypoints);
        downloadMap(cache, 14, "roadmap", 4, latlonMap, edge, waypoints);
        downloadMap(cache, 11, "roadmap", 5, latlonMap, edge, waypoints);
    }

    private static void downloadMap(cgCache cache, int zoom, String mapType, int level, String latlonMap, int edge, String waypoints) {
        final String mapUrl = "http://maps.google.com/maps/api/staticmap?center=" + latlonMap;
        final String markerUrl = getMarkerUrl(cache);

        final String url = mapUrl + "&zoom=" + zoom + "&size=" + edge + "x" + edge + "&maptype=" + mapType + "&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints + "&sensor=false";

        final File file = getMapFile(cache.geocode, level);
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
        if (!Settings.isStoreOfflineMaps() || cache.coords == null || StringUtils.isBlank(cache.geocode)) {
            return;
        }

        final String latlonMap = String.format((Locale) null, "%.6f", cache.coords.getLatitude()) + "," +
                String.format((Locale) null, "%.6f", cache.coords.getLongitude());
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
        if (CollectionUtils.isNotEmpty(cache.waypoints)) {
            for (cgWaypoint waypoint : cache.waypoints) {
                if (waypoint.coords == null) {
                    continue;
                }

                waypoints.append("&markers=icon%3A");
                waypoints.append(MARKERS_URL);
                waypoints.append("marker_waypoint_");
                waypoints.append(waypoint.typee != null ? waypoint.typee.id : null);
                waypoints.append(".png%7C");
                waypoints.append(String.format((Locale) null, "%.6f", waypoint.coords.getLatitude()));
                waypoints.append(',');
                waypoints.append(String.format((Locale) null, "%.6f", waypoint.coords.getLongitude()));
            }
        }

        // download map images in separate background thread for higher performance
        downloadMaps(cache, latlonMap, edge, waypoints.toString());
    }

    private static void downloadMaps(final cgCache cache, final String latlonMap, final int edge,
            final String waypoints) {
        Thread staticMapsThread = new Thread("getting static map") {
            @Override
            public void run() {
                downloadMapsInThread(cache, latlonMap, edge, waypoints);
            }
        };
        staticMapsThread.setPriority(Thread.MIN_PRIORITY);
        staticMapsThread.start();
    }

    private static String getMarkerUrl(final cgCache cache) {
        String type = "mystery";
        if (cache.found) {
            type = cache.type + "_found";
        } else if (cache.disabled) {
            type = cache.type + "_disabled";
        } else {
            type = cache.type;
        }

        return cgBase.urlencode_rfc3986(MARKERS_URL + "marker_cache_" + type + ".png");
    }
}
