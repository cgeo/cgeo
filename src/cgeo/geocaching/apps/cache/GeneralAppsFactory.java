package cgeo.geocaching.apps.cache;

import android.app.Activity;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.apps.AbstractAppFactory;

public final class GeneralAppsFactory extends AbstractAppFactory {
	private static GeneralApp[] apps = new GeneralApp[] {};

	private static GeneralApp[] getGeneralApps(Resources res) {
		if (null == apps || 0 == apps.length) {
			apps = new GeneralApp[] { new GccApp(res),
					new WhereYouGoApp(res) };
		}
		return apps;
	}

	public static void addMenuItems(Menu menu, Activity activity,
			Resources res, cgCache cache) {
		for (GeneralApp app : getGeneralApps(res)) {
			if (app.isInstalled(activity) && app.isEnabled(cache)) {
				menu.add(0, app.getId(), 0, app.getName());
			}
		}
	}

	public static boolean onMenuItemSelected(final MenuItem item,
			Activity activity, cgCache cache) {
		GeneralApp app = (GeneralApp) getAppFromMenuItem(item, apps);
		if (app != null) {
			return app.invoke(activity, cache);
		}
		return false;
	}

}
