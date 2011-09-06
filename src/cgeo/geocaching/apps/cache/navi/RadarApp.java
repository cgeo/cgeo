package cgeo.geocaching.apps.cache.navi;

import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;

class RadarApp extends AbstractNavigationApp implements NavigationApp {

	private static final String INTENT = "com.google.android.radar.SHOW_RADAR";
	private static final String PACKAGE_NAME = "com.eclipsim.gpsstatus2";

	RadarApp(final Resources res) {
		super(res.getString(R.string.cache_menu_radar), INTENT, PACKAGE_NAME);
	}

	private static void navigateTo(Activity activity, Double latitude, Double longitude) {
		Intent radarIntent = new Intent(INTENT);
		radarIntent.putExtra("latitude", Float.valueOf(latitude.floatValue()));
		radarIntent.putExtra("longitude", Float.valueOf(longitude.floatValue()));
		activity.startActivity(radarIntent);
	}

	@Override
	public boolean invoke(cgGeo geo, Activity activity, Resources res,
			cgCache cache,
			final UUID searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
		if (cache != null) {
			if (cache.latitude != null && cache.longitude != null) {
				navigateTo(activity, cache.latitude, cache.longitude);
				return true;
			}
		}
		if (waypoint != null) {
			if (waypoint.latitude != null && waypoint.longitude != null) {
				navigateTo(activity, waypoint.latitude, waypoint.longitude);
				return true;
			}
		}
		if (latitude != null && longitude != null) {
			navigateTo(activity, latitude, longitude);
			return true;
		}
		return false;
	}
}
