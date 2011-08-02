package cgeo.geocaching.apps.cache.navi;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWarning;
import cgeo.geocaching.cgWaypoint;

class InternalMap extends AbstractInternalMap implements
		NavigationApp {

	InternalMap(Resources res) {
		super(res.getString(R.string.caches_map_cgeo), null);
	}

	@Override
	public boolean invoke(cgGeo geo, Activity activity, Resources res,
			cgWarning warning, cgCache cache,
			Long searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
		cgSettings settings = getSettings(activity);
		Intent mapIntent = new Intent(activity, settings.getMapFactory().getMapClass());
		if (searchId != null) {
			mapIntent.putExtra("detail", true);
			mapIntent.putExtra("searchid", searchId);
		}
		if (waypoint != null) {
			mapIntent.putExtra("latitude", waypoint.latitude);
			mapIntent.putExtra("longitude", waypoint.longitude);
			mapIntent.putExtra("wpttype", waypoint.type);
		}
		if (cache != null) {
			mapIntent.putExtra("latitude", cache.latitude);
			mapIntent.putExtra("longitude", cache.longitude);
		}

		activity.startActivity(mapIntent);
		return true;
	}

}
