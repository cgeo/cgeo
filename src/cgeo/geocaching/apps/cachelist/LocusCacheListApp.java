package cgeo.geocaching.apps.cachelist;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.AbstractLocusApp;

class LocusCacheListApp extends AbstractLocusApp implements CacheListApp {

	LocusCacheListApp(Resources res) {
		super(res);
	}

	@Override
	public boolean invoke(cgGeo geo, List<cgCache> cacheList, Activity activity, Resources res, final UUID searchId) {
		if (cacheList == null || cacheList.isEmpty()) {
			return false;
		}

		try {
			final List<cgCache> cacheListCoord = new ArrayList<cgCache>();
			for (cgCache cache : cacheList) {
				if (cache.coords != null) {
					cacheListCoord.add(cache);
				}
			}

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final DataOutputStream dos = new DataOutputStream(baos);

			dos.writeInt(1); // not used
			dos.writeInt(cacheListCoord.size()); // cache and waypoints

			// cache waypoints
			for (cgCache cache : cacheListCoord) {
				final int wpIcon = cgBase.getMarkerIcon(true, cache.type, cache.own, cache.found, cache.disabled);

				if (wpIcon > 0) {
					// load icon
					Bitmap bitmap = BitmapFactory.decodeResource(res, wpIcon);
					ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos2);
					byte[] image = baos2.toByteArray();

					dos.writeInt(image.length);
					dos.write(image);
				} else {
					// no icon
					dos.writeInt(0); // no image
				}

				// name
				if (StringUtils.isNotBlank(cache.geocode)) {
					dos.writeUTF(cache.geocode.toUpperCase());
				} else {
					dos.writeUTF("");
				}

				// description
				if (StringUtils.isNotBlank(cache.name)) {
					dos.writeUTF(cache.name);
				} else {
					dos.writeUTF("");
				}

				// additional data :: keyword, button title, package, activity, data name, data content
				if (StringUtils.isNotBlank(cache.geocode)) {
					dos.writeUTF("intent;c:geo;cgeo.geocaching;cgeo.geocaching.cgeodetail;geocode;" + cache.geocode);
				} else {
					dos.writeUTF("");
				}

				dos.writeDouble(cache.coords.getLatitude()); // latitude
				dos.writeDouble(cache.coords.getLongitude()); // longitude
			}

			final Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("menion.points:data"));
			intent.putExtra("data", baos.toByteArray());

			activity.startActivity(intent);
		} catch (Exception e) {
			// nothing
		}
		return true;
	}

}
