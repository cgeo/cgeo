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

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

class GoogleNavigationApp extends AbstractNavigationApp implements
		NavigationApp {

	GoogleNavigationApp(final Resources res) {
		super(res.getString(R.string.cache_menu_tbt), null);
	}

	@Override
	public boolean isInstalled(Context context) {
		return true;
	}

	@Override
	public boolean invoke(cgGeo geo, Activity activity, Resources res,
			cgWarning warning, GoogleAnalyticsTracker tracker, cgCache cache,
			Long searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
		if (activity == null) {
			return false;
		}
		cgSettings settings = getSettings(activity);

		Double latitudeNow = null;
		Double longitudeNow = null;
		if (geo != null) {
			latitudeNow = geo.latitudeNow;
			longitudeNow = geo.longitudeNow;
		}

		// Google Navigation
		if (settings.useGNavigation == 1) {
			try {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("google.navigation:ll=" + latitude + ","
								+ longitude)));

				sendAnal(activity, tracker, "/external/native/navigation");

				return true;
			} catch (Exception e) {
				// nothing
			}
		}

		// Google Maps Directions
		try {
			if (latitudeNow != null && longitudeNow != null) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://maps.google.com/maps?f=d&saddr="
								+ latitudeNow + "," + longitudeNow + "&daddr="
								+ latitude + "," + longitude)));
			} else {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://maps.google.com/maps?f=d&daddr="
								+ latitude + "," + longitude)));
			}

			sendAnal(activity, tracker, "/external/native/maps");

			return true;
		} catch (Exception e) {
			// nothing
		}

		Log.i(cgSettings.tag,
				"cgBase.runNavigation: No navigation application available.");

		if (warning != null && res != null) {
			warning.showToast(res.getString(R.string.err_navigation_no));
		}

		return false;
	}

}
