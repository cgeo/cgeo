package cgeo.geocaching;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class cgHtmlImg implements Html.ImageGetter {

	private Activity activity = null;
	private String geocode = null;
	private boolean placement = true;
	private int reason = 0;
	private boolean onlySave = false;
	private boolean save = true;
	private BitmapFactory.Options bfOptions = new BitmapFactory.Options();
	private Display display = null;
	private int maxWidth = 0;
	private int maxHeight = 0;
	private double ratio = 1.0d;
	private int width = 0;
	private int height = 0;

	public cgHtmlImg(Activity activityIn, String geocodeIn, boolean placementIn, int reasonIn, boolean onlySaveIn) {
		this(activityIn, geocodeIn, placementIn, reasonIn, onlySaveIn, true);
	}

	public cgHtmlImg(Activity activityIn, String geocodeIn, boolean placementIn, int reasonIn, boolean onlySaveIn, boolean saveIn) {
		activity = activityIn;
		geocode = geocodeIn;
		placement = placementIn;
		reason = reasonIn;
		onlySave = onlySaveIn;
		save = saveIn;

		bfOptions.inTempStorage = new byte[16 * 1024];

		display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		maxWidth = display.getWidth() - 25;
		maxHeight = display.getHeight() - 25;
	}

	@Override
	public BitmapDrawable getDrawable(String url) {
		Bitmap imagePre = null;
		String dirName = null;
		String fileName = null;
		String fileNameSec = null;

		if (StringUtils.isNotBlank(url)) {
			return null;
		}

		final String[] urlParts = url.split("\\.");
		String urlExt = null;
		if (urlParts.length > 1) {
			urlExt = "." + urlParts[(urlParts.length - 1)];
			if (urlExt.length() > 5) {
				urlExt = "";
			}
		} else {
			urlExt = "";
		}

		if (StringUtils.isNotBlank(geocode)) {
			dirName = cgSettings.getStorage() + geocode + "/";
			fileName = cgSettings.getStorage() + geocode + "/" + cgBase.md5(url) + urlExt;
			fileNameSec = cgSettings.getStorageSec() + geocode + "/" + cgBase.md5(url) + urlExt;
		} else {
			dirName = cgSettings.getStorage() + "_others/";
			fileName = cgSettings.getStorage() + "_others/" + cgBase.md5(url) + urlExt;
			fileNameSec = cgSettings.getStorageSec() + "_others/" + cgBase.md5(url) + urlExt;
		}

		File dir = null;
		dir = new File(cgSettings.getStorage());
		if (dir.exists() == false) {
			dir.mkdirs();
		}
		dir = new File(dirName);
		if (dir.exists() == false) {
			dir.mkdirs();
		}
		dir = null;

		// load image from cache
		if (onlySave == false) {
			try {
				final Date now = new Date();

				final File file = new File(fileName);
				if (file.exists()) {
					final long imageSize = file.length();

					// large images will be downscaled on input to save memory
					if (imageSize > (6 * 1024 * 1024)) {
						bfOptions.inSampleSize = 48;
					} else if (imageSize > (4 * 1024 * 1024)) {
						bfOptions.inSampleSize = 16;
					} else if (imageSize > (2 * 1024 * 1024)) {
						bfOptions.inSampleSize = 10;
					} else if (imageSize > (1 * 1024 * 1024)) {
						bfOptions.inSampleSize = 6;
					} else if (imageSize > (0.5 * 1024 * 1024)) {
						bfOptions.inSampleSize = 2;
					}

					if (reason > 0 || file.lastModified() > (now.getTime() - (24 * 60 * 60 * 1000))) {
						imagePre = BitmapFactory.decodeFile(fileName, bfOptions);
					}
				}

				if (imagePre == null) {
					final File fileSec = new File(fileNameSec);
					if (fileSec.exists()) {
						final long imageSize = fileSec.length();

						// large images will be downscaled on input to save memory
						if (imageSize > (6 * 1024 * 1024)) {
							bfOptions.inSampleSize = 48;
						} else if (imageSize > (4 * 1024 * 1024)) {
							bfOptions.inSampleSize = 16;
						} else if (imageSize > (2 * 1024 * 1024)) {
							bfOptions.inSampleSize = 10;
						} else if (imageSize > (1 * 1024 * 1024)) {
							bfOptions.inSampleSize = 6;
						} else if (imageSize > (0.5 * 1024 * 1024)) {
							bfOptions.inSampleSize = 2;
						}

						if (reason > 0 || file.lastModified() > (now.getTime() - (24 * 60 * 60 * 1000))) {
							imagePre = BitmapFactory.decodeFile(fileNameSec, bfOptions);
						}
					}
				}
			} catch (Exception e) {
				Log.w(cgSettings.tag, "cgHtmlImg.getDrawable (reading cache): " + e.toString());
			}
		}

		// download image and save it to the cache
		if ((imagePre == null && reason == 0) || onlySave) {
			Uri uri = null;
			HttpClient client = null;
			HttpGet getMethod = null;
			HttpResponse httpResponse = null;
			HttpEntity entity = null;
			BufferedHttpEntity bufferedEntity = null;

			try {
				// check if uri is absolute or not, if not attach geocaching.com hostname and scheme
				uri = Uri.parse(url);

				if (uri.isAbsolute() == false) {
					url = "http://www.geocaching.com" + url;
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgHtmlImg.getDrawable (parse URL): " + e.toString());
			}

			if (uri != null) {
				for (int i = 0; i < 2; i++) {
					if (i > 0) {
						Log.w(cgSettings.tag, "cgHtmlImg.getDrawable: Failed to download data, retrying. Attempt #" + (i + 1));
					}

					try {
						client = new DefaultHttpClient();
						getMethod = new HttpGet(url);
						httpResponse = client.execute(getMethod);
						entity = httpResponse.getEntity();
						bufferedEntity = new BufferedHttpEntity(entity);

						final long imageSize = bufferedEntity.getContentLength();

						// large images will be downscaled on input to save memory
						if (imageSize > (6 * 1024 * 1024)) {
							bfOptions.inSampleSize = 48;
						} else if (imageSize > (4 * 1024 * 1024)) {
							bfOptions.inSampleSize = 16;
						} else if (imageSize > (2 * 1024 * 1024)) {
							bfOptions.inSampleSize = 10;
						} else if (imageSize > (1 * 1024 * 1024)) {
							bfOptions.inSampleSize = 6;
						} else if (imageSize > (0.5 * 1024 * 1024)) {
							bfOptions.inSampleSize = 2;
						}

						if (bufferedEntity != null) {
							imagePre = BitmapFactory.decodeStream(bufferedEntity.getContent(), null, bfOptions);
						}
						if (imagePre != null) {
							break;
						}
					} catch (Exception e) {
						Log.e(cgSettings.tag, "cgHtmlImg.getDrawable (downloading from web): " + e.toString());
					}
				}
			}

			if (save) {
				try {
					// save to memory/SD cache
					if (bufferedEntity != null) {
						final InputStream is = (InputStream) bufferedEntity.getContent();
						final FileOutputStream fos = new FileOutputStream(fileName);
						try {
							final byte[] buffer = new byte[4096];
							int l;
							while ((l = is.read(buffer)) != -1) {
								fos.write(buffer, 0, l);
							}
							fos.flush();
						} catch (IOException e) {
							Log.e(cgSettings.tag, "cgHtmlImg.getDrawable (saving to cache): " + e.toString());
						} finally {
							is.close();
							fos.close();
						}
					}
				} catch (Exception e) {
					Log.e(cgSettings.tag, "cgHtmlImg.getDrawable (saving to cache): " + e.toString());
				}
			}

			entity = null;
			bufferedEntity = null;
		}

		if (onlySave) {
			return null;
		}

		// get image and return
		if (imagePre == null) {
			Log.d(cgSettings.tag, "cgHtmlImg.getDrawable: Failed to obtain image");

			if (placement == false) {
				imagePre = BitmapFactory.decodeResource(activity.getResources(), R.drawable.image_no_placement);
			} else {
				imagePre = BitmapFactory.decodeResource(activity.getResources(), R.drawable.image_not_loaded);
			}
		}

		final int imgWidth = imagePre.getWidth();
		final int imgHeight = imagePre.getHeight();

		if (imgWidth > maxWidth || imgHeight > maxHeight) {
			if ((maxWidth / imgWidth) > (maxHeight / imgHeight)) {
				ratio = (double) maxHeight / (double) imgHeight;
			} else {
				ratio = (double) maxWidth / (double) imgWidth;
			}

			width = (int) Math.ceil(imgWidth * ratio);
			height = (int) Math.ceil(imgHeight * ratio);

			try {
				imagePre = Bitmap.createScaledBitmap(imagePre, width, height, true);
			} catch (Exception e) {
				Log.d(cgSettings.tag, "cgHtmlImg.getDrawable: Failed to scale image");
				return null;
			}
		} else {
			width = imgWidth;
			height = imgHeight;
		}

		final BitmapDrawable image = new BitmapDrawable(imagePre);
		image.setBounds(new Rect(0, 0, width, height));

		return image;
	}
}
