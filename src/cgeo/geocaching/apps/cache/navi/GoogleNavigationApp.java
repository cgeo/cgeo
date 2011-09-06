package cgeo.geocaching.apps.cache.navi;

import java.util.UUID;

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
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.activity.ActivityMixin;

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
	public boolean invoke(final cgGeo geo, final Activity activity, final Resources res,
			final cgCache cache,
			final UUID searchId, final cgWaypoint waypoint, final Double latitude, final Double longitude) {
		if (activity == null) {
			return false;
		}

		boolean navigationResult = false;
		if (latitude != null && longitude != null) {
			navigationResult = navigateToCoordinates(geo, activity, latitude, longitude);
		}
		else if (waypoint != null) {
			navigationResult = navigateToCoordinates(geo, activity, waypoint.latitude, waypoint.longitude);
		}
		else if (cache != null) {
			navigationResult = navigateToCoordinates(geo, activity, cache.latitude, cache.longitude);
		}

		if (!navigationResult) {
			if (res != null) {
				ActivityMixin.showToast(activity, res.getString(R.string.err_navigation_no));
			}
			return false;
		}

		return true;
	}

	private static boolean navigateToCoordinates(cgGeo geo, Activity activity, Double latitude,
			Double longitude) {
		Double latitudeNow = null;
		Double longitudeNow = null;
		if (geo != null) {
			latitudeNow = geo.latitudeNow;
			longitudeNow = geo.longitudeNow;
		}

		cgSettings settings = getSettings(activity);

		// Google Navigation
		if (settings.useGNavigation == 1) {
			try {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("google.navigation:ll=" + latitude + ","
								+ longitude)));

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

			return true;
		} catch (Exception e) {
			// nothing
		}

		Log.i(cgSettings.tag,
				"cgBase.runNavigation: No navigation application available.");
		return false;
	}

}