package cgeo.geocaching;

import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import rx.Observable;
import rx.functions.Action0;
import rx.util.async.Async;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public final class StaticMapsProvider {
    static final int MAPS_LEVEL_MAX = 5;
    private static final String PREFIX_PREVIEW = "preview";
    private static final String GOOGLE_STATICMAP_URL = "http://maps.google.com/maps/api/staticmap";
    private static final int GOOGLE_MAX_ZOOM = 20;
    private static final String SATELLITE = "satellite";
    private static final String ROADMAP = "roadmap";
    private static final String WAYPOINT_PREFIX = "wp";
    private static final String MAP_FILENAME_PREFIX = "map_";
    private static final String MARKERS_URL = "http://status.cgeo.org/assets/markers/";

    /** We assume there is no real usable image with less than 1k. */
    private static final int MIN_MAP_IMAGE_BYTES = 1000;

    /**
     * max size in free API version: https://developers.google.com/maps/documentation/staticmaps/#Imagesizes
     */
    private static final int GOOGLE_MAPS_MAX_SIZE = 640;

    private StaticMapsProvider() {
        // utility class
    }

    private static File getMapFile(final String geocode, final String prefix, final boolean createDirs) {
        return LocalStorage.getStorageFile(geocode, MAP_FILENAME_PREFIX + prefix, false, createDirs);
    }

    private static Observable<String> downloadDifferentZooms(final String geocode, final String markerUrl, final String prefix, final String latlonMap, final int width, final int height, final Parameters waypoints) {
        return Observable.merge(downloadMap(geocode, 20, SATELLITE, markerUrl, prefix + '1', "", latlonMap, width, height, waypoints),
                downloadMap(geocode, 18, SATELLITE, markerUrl, prefix + '2', "", latlonMap, width, height, waypoints),
                downloadMap(geocode, 16, ROADMAP, markerUrl, prefix + '3', "", latlonMap, width, height, waypoints),
                downloadMap(geocode, 14, ROADMAP, markerUrl, prefix + '4', "", latlonMap, width, height, waypoints),
                downloadMap(geocode, 11, ROADMAP, markerUrl, prefix + '5', "", latlonMap, width, height, waypoints));
    }

    private static Observable<String> downloadMap(final String geocode, final int zoom, final String mapType, final String markerUrl, final String prefix, final String shadow, final String latlonMap, final int width, final int height, final Parameters waypoints) {
        int scale = 1;
        if (width > GOOGLE_MAPS_MAX_SIZE) {
            scale = 2;
        }
        final float aspectRatio = width / (float) height;
        final int requestWidth = Math.min(width / scale, GOOGLE_MAPS_MAX_SIZE);
        final int requestHeight = (aspectRatio > 1) ? Math.round(requestWidth / aspectRatio) : requestWidth;
        final int requestScale = scale;
        final int requestZoom = Math.min((scale == 2) ? zoom + 1 : zoom, GOOGLE_MAX_ZOOM);
        return Async.fromAction(new Action0() {
            @Override
            public void call() {
                final Parameters params = new Parameters(
                        "center", latlonMap,
                        "zoom", String.valueOf(requestZoom),
                        "size", String.valueOf(requestWidth) + 'x' + String.valueOf(requestHeight),
                        "scale", String.valueOf(requestScale),
                        "maptype", mapType,
                        "markers", "icon:" + markerUrl + '|' + shadow + latlonMap,
                        "sensor", "false");
                if (waypoints != null) {
                    params.addAll(waypoints);
                }
                final HttpResponse httpResponse = Network.getRequest(GOOGLE_STATICMAP_URL, params);

                if (httpResponse == null) {
                    Log.e("StaticMapsProvider.downloadMap: httpResponse is null");
                    return;
                }
                if (httpResponse.getStatusLine().getStatusCode() != 200) {
                    Log.d("StaticMapsProvider.downloadMap: httpResponseCode = " + httpResponse.getStatusLine().getStatusCode());
                    return;
                }
                final File file = getMapFile(geocode, prefix, true);
                if (LocalStorage.saveEntityToFile(httpResponse, file)) {
                    // Delete image if it has no contents
                    final long fileSize = file.length();
                    if (fileSize < MIN_MAP_IMAGE_BYTES) {
                        FileUtils.deleteIgnoringFailure(file);
                    }
                }
            }
        }, prefix, RxUtils.networkScheduler);
    }

    public static Observable<String> downloadMaps(final Geocache cache) {
        if ((!Settings.isStoreOfflineMaps() && !Settings.isStoreOfflineWpMaps()) || StringUtils.isBlank(cache.getGeocode())) {
            return Observable.empty();
        }
        // TODO Check if this is also OK, was width -25
        final Point displaySize = Compatibility.getDisplaySize();

        final List<Observable<String>> downloaders = new LinkedList<>();

        if (Settings.isStoreOfflineMaps() && cache.getCoords() != null) {
            downloaders.add(storeCachePreviewMap(cache));
            downloaders.add(storeCacheStaticMap(cache, displaySize.x, displaySize.y));
        }

        // clean old and download static maps for waypoints if one is missing
        if (Settings.isStoreOfflineWpMaps()) {
            for (final Waypoint waypoint : cache.getWaypoints()) {
                if (!hasAllStaticMapsForWaypoint(cache.getGeocode(), waypoint)) {
                    downloaders.add(refreshAllWpStaticMaps(cache, displaySize.x, displaySize.y));
                }
            }

        }

        return Observable.merge(downloaders);
    }

    /**
     * Deletes and download all Waypoints static maps.
     */
    private static Observable<String> refreshAllWpStaticMaps(final Geocache cache, final int width, final int height) {
        LocalStorage.deleteFilesWithPrefix(cache.getGeocode(), MAP_FILENAME_PREFIX + WAYPOINT_PREFIX);
        final List<Observable<String>> downloaders = new LinkedList<>();
        for (final Waypoint waypoint : cache.getWaypoints()) {
            downloaders.add(storeWaypointStaticMap(cache.getGeocode(), width, height, waypoint));
        }
        return Observable.merge(downloaders);
    }

    public static Observable<String> storeWaypointStaticMap(final Geocache cache, final Waypoint waypoint) {
        // TODO Check if this is also OK, was width -25
        final Point displaySize = Compatibility.getDisplaySize();
        return storeWaypointStaticMap(cache.getGeocode(), displaySize.x, displaySize.y, waypoint);
    }

    private static Observable<String> storeWaypointStaticMap(final String geocode, final int width, final int height, final Waypoint waypoint) {
        if (geocode == null) {
            Log.e("storeWaypointStaticMap - missing input parameter geocode");
            return Observable.empty();
        }
        if (waypoint == null) {
            Log.e("storeWaypointStaticMap - missing input parameter waypoint");
            return Observable.empty();
        }
        if (waypoint.getCoords() == null) {
            return Observable.empty();
        }
        final String wpLatlonMap = waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final String wpMarkerUrl = getWpMarkerUrl(waypoint);
        if (!hasAllStaticMapsForWaypoint(geocode, waypoint)) {
            // download map images in separate background thread for higher performance
            return downloadMaps(geocode, wpMarkerUrl, WAYPOINT_PREFIX + waypoint.getId() + '_' + waypoint.getStaticMapsHashcode() + "_", wpLatlonMap, width, height, null);
        }
        return Observable.empty();
    }

    public static Observable<String> storeCacheStaticMap(final Geocache cache) {
        // TODO Check if this is also OK, was width -25
        final Point displaySize = Compatibility.getDisplaySize();
        return storeCacheStaticMap(cache, displaySize.x, displaySize.y);
    }

    private static Observable<String> storeCacheStaticMap(final Geocache cache, final int width, final int height) {
        final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final Parameters waypoints = new Parameters();
        for (final Waypoint waypoint : cache.getWaypoints()) {
            if (waypoint.getCoords() == null) {
                continue;
            }
            final String wpMarkerUrl = getWpMarkerUrl(waypoint);
            waypoints.put("markers", "icon:" + wpMarkerUrl + '|' + waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA));
        }
        // download map images in separate background thread for higher performance
        final String cacheMarkerUrl = getCacheMarkerUrl(cache);
        return downloadMaps(cache.getGeocode(), cacheMarkerUrl, "", latlonMap, width, height, waypoints);
    }

    public static Observable<String> storeCachePreviewMap(final Geocache cache) {
        final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final Point displaySize = Compatibility.getDisplaySize();
        final int minSize = Math.min(displaySize.x, displaySize.y);
        final String markerUrl = MARKERS_URL + "my_location_mdpi.png";
        return downloadMap(cache.getGeocode(), 15, ROADMAP, markerUrl, PREFIX_PREVIEW, "shadow:false|", latlonMap, minSize, minSize, null);
    }

    private static Observable<String> downloadMaps(final String geocode, final String markerUrl, final String prefix,
            final String latlonMap, final int width, final int height,
                                                   final Parameters waypoints) {
        return downloadDifferentZooms(geocode, markerUrl, prefix, latlonMap, width, height, waypoints);
    }

    private static String getCacheMarkerUrl(final Geocache cache) {
        final StringBuilder url = new StringBuilder(MARKERS_URL);
        url.append("marker_cache_").append(cache.getType().id);
        if (cache.isFound()) {
            url.append("_found");
        } else if (cache.isDisabled() || cache.isArchived()) {
            url.append("_disabled");
        }
        url.append(".png");
        return url.toString();
    }

    private static String getWpMarkerUrl(final Waypoint waypoint) {
        final String type = waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null;
        return MARKERS_URL + "marker_waypoint_" + type + ".png";
    }

    public static void removeWpStaticMaps(final Waypoint waypoint, final String geocode) {
        if (waypoint == null) {
            return;
        }
        final int waypointId = waypoint.getId();
        final int waypointMapHash = waypoint.getStaticMapsHashcode();
        for (int level = 1; level <= MAPS_LEVEL_MAX; level++) {
            final File mapFile = StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + waypointId + "_" + waypointMapHash + '_' + level, false);
            if (!FileUtils.delete(mapFile)) {
                Log.e("StaticMapsProvider.removeWpStaticMaps failed for " + mapFile.getAbsolutePath());
            }
        }
    }

    /**
     * Check if at least one map file exists for the given cache.
     *
     * @param cache
     * @return <code>true</code> if at least one map file exists; <code>false</code> otherwise
     */
    public static boolean hasStaticMap(@NonNull final Geocache cache) {
       final String geocode = cache.getGeocode();
        if (StringUtils.isBlank(geocode)) {
            return false;
        }
        for (int level = 1; level <= MAPS_LEVEL_MAX; level++) {
            final File mapFile = StaticMapsProvider.getMapFile(geocode, String.valueOf(level), false);
            if (mapFile.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if at least one map file exists for the given geocode and waypoint ID.
     *
     * @param geocode
     * @param waypoint
     * @return <code>true</code> if at least one map file exists; <code>false</code> otherwise
     */
    public static boolean hasStaticMapForWaypoint(final String geocode, final Waypoint waypoint) {
        final int waypointId = waypoint.getId();
        final int waypointMapHash = waypoint.getStaticMapsHashcode();
        for (int level = 1; level <= MAPS_LEVEL_MAX; level++) {
            final File mapFile = StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + waypointId + "_" + waypointMapHash + "_" + level, false);
            if (mapFile.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if all map files exist for the given geocode and waypoint ID.
     *
     * @param geocode
     * @param waypoint
     * @return <code>true</code> if all map files exist; <code>false</code> otherwise
     */
    public static boolean hasAllStaticMapsForWaypoint(final String geocode, final Waypoint waypoint) {
        final int waypointId = waypoint.getId();
        final int waypointMapHash = waypoint.getStaticMapsHashcode();
        for (int level = 1; level <= MAPS_LEVEL_MAX; level++) {
            final File mapFile = StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + waypointId + "_" + waypointMapHash + "_" + level, false);
            final boolean mapExists = mapFile.exists();
            if (!mapExists) {
                return false;
            }
        }
        return true;
    }

    public static Bitmap getPreviewMap(final Geocache cache) {
        return decodeFile(StaticMapsProvider.getMapFile(cache.getGeocode(), PREFIX_PREVIEW, false));
    }

    public static Bitmap getWaypointMap(final String geocode, final Waypoint waypoint, final int level) {
        final int waypointId = waypoint.getId();
        final int waypointMapHash = waypoint.getStaticMapsHashcode();
        return decodeFile(StaticMapsProvider.getMapFile(geocode, WAYPOINT_PREFIX + waypointId + "_" + waypointMapHash + "_" + level, false));
    }

    public static Bitmap getCacheMap(final String geocode, final int level) {
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
