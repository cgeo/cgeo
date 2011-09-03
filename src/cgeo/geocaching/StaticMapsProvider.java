package cgeo.geocaching;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import cgeo.geocaching.utils.CollectionUtils;

public class StaticMapsProvider {
	private static final String MARKERS_URL = "http://cgeo.carnero.cc/_markers/";
	/**
	 * in my tests the "no image available" image had 5470 bytes, while "street only" maps had at least 20000 bytes
	 */
	private static final int MIN_MAP_IMAGE_BYTES = 6000;

	private static void downloadMapsInThread(final cgCache cache, String latlonMap, int edge, String waypoints) {
		createStorageDirectory(cache);

		downloadMap(cache, 20, "satellite", 1, latlonMap, edge, waypoints);
		downloadMap(cache, 18, "satellite", 2, latlonMap, edge, waypoints);
		downloadMap(cache, 16, "roadmap", 3, latlonMap, edge, waypoints);
		downloadMap(cache, 14, "roadmap", 4, latlonMap, edge, waypoints);
		downloadMap(cache, 11 ,"roadmap", 5, latlonMap, edge, waypoints);
	}

	private static void createStorageDirectory(final cgCache cache) {
		File dir = new File(cgSettings.getStorage());
		if (dir.exists() == false) {
			dir.mkdirs();
		}
		dir = new File(getStaticMapsDirectory(cache));
		if (dir.exists() == false) {
			dir.mkdirs();
		}
	}

	private static String getStaticMapsDirectory(final cgCache cache) {
		return cgSettings.getStorage() + cache.geocode;
	}

	private static void downloadMap(cgCache cache, int zoom, String mapType, int level, String latlonMap, int edge, String waypoints) {
		String mapUrl = "http://maps.google.com/maps/api/staticmap?center=" + latlonMap;
		String markerUrl = getMarkerUrl(cache);

		String url = mapUrl + "&zoom=" + zoom + "&size=" + edge + "x" + edge + "&maptype=" + mapType + "&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints + "&sensor=false";

		final String fileName = getStaticMapsDirectory(cache) + "/map_" + level;
		HttpClient client = null;
		HttpGet getMethod = null;
		HttpResponse httpResponse = null;
		HttpEntity entity = null;
		BufferedHttpEntity bufferedEntity = null;

		boolean ok = false;

		for (int i = 0; i < 3; i ++) {
			if (i > 0) Log.w(cgSettings.tag, "cgMapImg.getDrawable: Failed to download data, retrying. Attempt #" + (i + 1));

			try {
				client = new DefaultHttpClient();
				getMethod = new HttpGet(url);
				httpResponse = client.execute(getMethod);
				entity = httpResponse.getEntity();

				// if image is to small, don't download and save, there is no map data for this zoom level
				long contentSize = entity.getContentLength();
				if (contentSize > 0 && contentSize <= MIN_MAP_IMAGE_BYTES) {
					break;
				}

				bufferedEntity = new BufferedHttpEntity(entity);
				if (bufferedEntity != null) {
					InputStream is = (InputStream)bufferedEntity.getContent();
					FileOutputStream fos = new FileOutputStream(fileName);

					int fileSize = 0;
					try {
						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = is.read(buffer)) != -1) {
							fos.write(buffer, 0, bytesRead);
							fileSize += bytesRead;
						}
						fos.flush();
						ok = true;
					} catch (IOException e) {
						Log.e(cgSettings.tag, "cgMapImg.getDrawable (saving to cache): " + e.toString());
					} finally {
						is.close();
						fos.close();
					}

					bufferedEntity = null;

					// delete image if it has no contents
					if (ok && fileSize < MIN_MAP_IMAGE_BYTES) {
						(new File(fileName)).delete();
					}
				}

				if (ok) {
					break;
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgMapImg.getDrawable (downloading from web): " + e.toString());
			}
		}
	}

	public static void downloadMaps(cgCache cache, cgSettings settings, Activity activity) {
		if (settings.storeOfflineMaps != 1 || cache.latitude == null
				|| cache.longitude == null || StringUtils.isNotBlank(cache.geocode)) {
			return;
		}

		final String latlonMap = String.format((Locale) null, "%.6f", cache.latitude) + "," + String.format((Locale) null, "%.6f", cache.longitude);
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
				if (waypoint.latitude == null && waypoint.longitude == null) {
					continue;
				}

				waypoints.append("&markers=icon%3A");
				waypoints.append(MARKERS_URL);
				waypoints.append("marker_waypoint_");
				waypoints.append(waypoint.type);
				waypoints.append(".png%7C");
				waypoints.append(String.format((Locale) null, "%.6f", waypoint.latitude));
				waypoints.append(',');
				waypoints.append(String.format((Locale) null, "%.6f", waypoint.longitude));
			}
		}

		// download map images in separate background thread for higher performance
		downloadMaps(cache, latlonMap, edge, waypoints.toString());
	}

	private static void downloadMaps(final cgCache cache, final String latlonMap, final int edge,
			final String waypoints) {
		Thread staticMapsThread = new Thread("getting static map") {@Override
		public void run() {
			downloadMapsInThread(cache, latlonMap, edge, waypoints);
		}};
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
