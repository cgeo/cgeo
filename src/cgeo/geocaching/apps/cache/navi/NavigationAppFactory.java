package cgeo.geocaching.apps.cache.navi;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractAppFactory;

public final class NavigationAppFactory extends AbstractAppFactory {
	private static NavigationApp[] apps = new NavigationApp[] {};

	private static NavigationApp[] getNavigationApps(Resources res) {
		if (null == apps || 0 == apps.length) {
			apps = new NavigationApp[] {
					// compass
					new RadarApp(res), 
					new InternalMap(res), 
					new StaticMapApp(res),
					new LocusApp(res),
					new RMapsApp(res),
					new GoogleMapsApp(res),
					new GoogleNavigationApp(res),
					new StreetviewApp(res)};
		}
		return apps;
	}

	public static void addMenuItems(Menu menu, Activity activity,
			Resources res) {
		for (NavigationApp app : getNavigationApps(res)) {
			if (app.isInstalled(activity)) {
				menu.add(0, app.getId(), 0, app.getName());
			}
		}
	}

	public static boolean onMenuItemSelected(final MenuItem item,
			final cgGeo geo, Activity activity, Resources res,
			cgCache cache,
			Long searchId, cgWaypoint waypoint, ArrayList<Double> destination) {
		NavigationApp app = (NavigationApp) getAppFromMenuItem(item, apps);
		if (app != null) {
			Double latitude = null;
			Double longitude = null;
			if (destination != null && destination.size() >= 2) {
				latitude = destination.get(0);
				longitude = destination.get(1);
			}
			try {
				return app.invoke(geo, activity, res, cache,
						searchId, waypoint, latitude, longitude);
			} catch (Exception e) {
				Log.e(cgSettings.tag, "NavigationAppFactory.onMenuItemSelected: " + e.toString());
			}
		}
		return false;
	}

}
