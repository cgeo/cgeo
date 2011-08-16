package cgeo.geocaching.apps.cachelist;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.AbstractApp;

class InternalCacheListMap extends AbstractApp implements CacheListApp {

	InternalCacheListMap(Resources res) {
		super(res.getString(R.string.cache_menu_map), null);
	}

	@Override
	public boolean isInstalled(Context context) {
		return true;
	}

	@Override
	public boolean invoke(cgGeo geo, List<cgCache> caches, Activity activity, Resources res, Long searchId) {
		Intent mapIntent = new Intent(activity, getSettings(activity).getMapFactory()
				.getMapClass());
		mapIntent.putExtra("detail", false); // this is the main difference to the activity for a single point
		mapIntent.putExtra("searchid", searchId);

		activity.startActivity(mapIntent);
		return true;
	}
}
