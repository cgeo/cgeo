package cgeo.geocaching.apps.cache.navi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWarning;
import cgeo.geocaching.cgWaypoint;

class GoogleMapsApp extends AbstractNavigationApp implements NavigationApp {

	GoogleMapsApp(final Resources res) {
		super(res.getString(R.string.cache_menu_map_ext), null);
	}

	@Override
	public boolean isInstalled(Context context) {
		return true;
	}

	public boolean invoke(cgGeo geo, Activity activity, Resources res,
			cgWarning warning, cgCache cache,
			Long searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
		if (cache == null && waypoint == null && latitude == null && longitude == null) {
			return false;
		}

		try {
			if (cache != null && cache.latitude != null && cache.longitude != null) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + cache.latitude + "," + cache.longitude)));
				// INFO: q parameter works with Google Maps, but breaks cooperation with all other apps
			} else if (waypoint != null && waypoint.latitude != null && waypoint.longitude != null) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + waypoint.latitude + "," + waypoint.longitude)));
				// INFO: q parameter works with Google Maps, but breaks cooperation with all other apps
			}

			return true;
		} catch (Exception e) {
			// nothing
		}

		Log.i(cgSettings.tag, "cgBase.runExternalMap: No maps application available.");

		if (warning != null && res != null) {
			warning.showToast(res.getString(R.string.err_application_no));
		}

		return false;
	}

}
