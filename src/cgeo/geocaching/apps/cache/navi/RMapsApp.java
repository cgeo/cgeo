package cgeo.geocaching.apps.cache.navi;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWarning;
import cgeo.geocaching.cgWaypoint;

class RMapsApp extends AbstractNavigationApp implements NavigationApp {

	private static final String INTENT = "com.robert.maps.action.SHOW_POINTS";

	RMapsApp(final Resources res) {
		super(res.getString(R.string.cache_menu_rmaps), INTENT);
	}

	@Override
	public boolean invoke(cgGeo geo, Activity activity, Resources res,
			cgWarning warning, cgCache cache,
			Long searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
		if (cache == null && waypoint == null && latitude == null
				&& longitude == null) {
			return false;
		}

		try {
			if (isInstalled(activity)) {
				final ArrayList<String> locations = new ArrayList<String>();
				if (cache != null && cache.latitude != null
						&& cache.longitude != null) {
					locations.add(String.format((Locale) null, "%.6f",
							cache.latitude)
							+ ","
							+ String.format((Locale) null, "%.6f",
									cache.longitude)
							+ ";"
							+ cache.geocode
							+ ";" + cache.name);
				} else if (waypoint != null && waypoint.latitude != null
						&& waypoint.longitude != null) {
					locations.add(String.format((Locale) null, "%.6f",
							waypoint.latitude)
							+ ","
							+ String.format((Locale) null, "%.6f",
									waypoint.longitude)
							+ ";"
							+ waypoint.lookup
							+ ";" + waypoint.name);
				}

				final Intent intent = new Intent(
						"com.robert.maps.action.SHOW_POINTS");

				intent.putStringArrayListExtra("locations", locations);

				activity.startActivity(intent);

				return true;
			}
		} catch (Exception e) {
			// nothing
		}

		return false;
	}
}
