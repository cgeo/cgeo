package cgeo.geocaching;

import android.util.Log;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class cgAddressImg {
	public static BitmapDrawable getDrawable(String url) {
		Bitmap imagePre = null;

		if (url == null || url.length() == 0) return null;

		HttpClient client = null;
		HttpGet getMethod = null;
		HttpResponse httpResponse = null;
		HttpEntity entity = null;
		BufferedHttpEntity bufferedEntity = null;

		for (int i = 0; i < 2; i ++) {
			if (i > 0) Log.w(cgSettings.tag, "cgAddressImg.getDrawable: Failed to download data, retrying. Attempt #" + (i + 1));

			try {
				client = new DefaultHttpClient();
				getMethod = new HttpGet(url);
				httpResponse = client.execute(getMethod);
				entity = httpResponse.getEntity();
				bufferedEntity = new BufferedHttpEntity(entity);

				Log.i(cgSettings.tag, "[" + entity.getContentLength() + "B] Downloading address map " + url);

				if (bufferedEntity != null) imagePre = BitmapFactory.decodeStream(bufferedEntity.getContent(), null, null);
				if (imagePre != null) break;
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgAddressImg.getDrawable (downloading from web): " + e.toString());
			}
		}

		if (imagePre == null) {
			Log.d(cgSettings.tag, "cgAddressImg.getDrawable: Failed to obtain image");

			return null;
		}

		final BitmapDrawable image = new BitmapDrawable(imagePre);
		image.setBounds(new Rect(0, 0, imagePre.getWidth(), imagePre.getHeight()));

		// imagePre.recycle();
		imagePre = null;

		return image;
	}
}
