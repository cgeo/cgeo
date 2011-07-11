package cgeo.geocaching;

import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class cgMapImg {
	/**
	 * in my tests the "no image available" image had 5470 bytes, while "street only" maps had at least 20000 bytes
	 */
	private static final int MIN_MAP_IMAGE_BYTES = 6000;
	private cgSettings settings = null;
	private String geocode = null;

	public cgMapImg(cgSettings settingsIn, String geocodeIn) {
		geocode = geocodeIn;
		settings = settingsIn;

		if (geocode != null && geocode.length() > 0) {
			final String dirName = settings.getStorage() + geocode + "/";

			File dir = null;
			dir = new File(settings.getStorage());
			if (dir.exists() == false) {
				dir.mkdirs();
			}
			dir = new File(dirName);
			if (dir.exists() == false) {
				dir.mkdirs();
			}
			dir = null;
		}
	}

	public void getDrawable(String url, int level) {
		if (url == null || url.length() == 0) {
			return;
		}

		if (geocode == null || geocode.length() == 0) {
			return;
		}

		final String fileName = settings.getStorage() + geocode + "/map_" + level;
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
						ok = true;
					} catch (IOException e) {
						Log.e(cgSettings.tag, "cgMapImg.getDrawable (saving to cache): " + e.toString());
					} finally {
						is.close();
						fos.flush();
						fos.close();
					}

					bufferedEntity = null;

					// delete image if it has no contents
					if (ok && fileSize < MIN_MAP_IMAGE_BYTES) {
						(new File(fileName)).delete();
					}
				}

				if (ok == true) {
					break;
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgMapImg.getDrawable (downloading from web): " + e.toString());
			}
		}
	}
}
