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
import cgeo.geocaching.geopoint.Geopoint;

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
			final UUID searchId, final cgWaypoint waypoint, final Geopoint coords) {
		if (activity == null) {
			return false;
		}

		boolean navigationResult = false;
		if (coords != null) {
			navigationResult = navigateToCoordinates(geo, activity, coords);
		}
		else if (waypoint != null) {
			navigationResult = navigateToCoordinates(geo, activity, waypoint.coords);
		}
		else if (cache != null) {
			navigationResult = navigateToCoordinates(geo, activity, cache.coords);
		}

		if (!navigationResult) {
			if (res != null) {
				ActivityMixin.showToast(activity, res.getString(R.string.err_navigation_no));
			}
			return false;
		}

		return true;
	}

	private static boolean navigateToCoordinates(cgGeo geo, Activity activity, final Geopoint coords) {
		final Geopoint coordsNow = geo == null ? null : geo.coordsNow;

		cgSettings settings = getSettings(activity);

		// Google Navigation
		if (settings.useGNavigation == 1) {
			try {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("google.navigation:ll=" + coords.getLatitude() + ","
								+ coords.getLongitude())));

				return true;
			} catch (Exception e) {
				// nothing
			}
		}

		// Google Maps Directions
		try {
			if (coordsNow != null) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://maps.google.com/maps?f=d&saddr="
								+ coordsNow.getLatitude() + "," + coordsNow.getLongitude() + "&daddr="
								+ coords.getLatitude() + "," + coords.getLongitude())));
			} else {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://maps.google.com/maps?f=d&daddr="
								+ coords.getLatitude() + "," + coords.getLongitude())));
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