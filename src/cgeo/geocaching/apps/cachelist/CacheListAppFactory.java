package cgeo.geocaching.apps.cachelist;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.AbstractAppFactory;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public final class CacheListAppFactory extends AbstractAppFactory {
	private static CacheListApp[] apps = new CacheListApp[] {};

	private static CacheListApp[] getMultiPointNavigationApps(
			Resources res) {
		if (null == apps || 0 == apps.length) {
			apps = new CacheListApp[] {
					new InternalCacheListMap(res),
					new LocusCacheListApp(res) };
		}
		return apps;
	}

	public static void addMenuItems(Menu menu,
			Activity activity, Resources res) {
		ArrayList<CacheListApp> activeApps = new ArrayList<CacheListApp>();
		for (CacheListApp app : getMultiPointNavigationApps(res)) {
			if (app.isInstalled(activity)) {
				activeApps.add(app);
			}
		}
		// use a new sub menu, if more than one app is available
		if (activeApps.size() > 1) {
			Menu subMenu = menu.addSubMenu(0, 101, 0,
					res.getString(R.string.caches_on_map)).setIcon(
					android.R.drawable.ic_menu_mapmode);
			for (CacheListApp app : activeApps) {
				subMenu.add(0, app.getId(), 0, app.getName());
			}
		} else if (activeApps.size() == 1) {
			menu.add(0, activeApps.get(0).getId(), 0,
					activeApps.get(0).getName()).setIcon(android.R.drawable.ic_menu_mapmode);
		}
	}

	public static boolean onMenuItemSelected(final MenuItem item,
			final cgGeo geo, final List<cgCache> caches, final Activity activity, final Resources res,
			final GoogleAnalyticsTracker tracker, final Integer searchId) {
		CacheListApp app = (CacheListApp) getAppFromMenuItem(
				item, apps);
		if (app != null) {
			return app.invoke(geo, caches, activity, res, tracker, searchId);
		}
		return false;
	}

}
