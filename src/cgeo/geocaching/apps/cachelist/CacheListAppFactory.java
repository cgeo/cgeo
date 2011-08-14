package cgeo.geocaching.apps.cachelist;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.apps.AbstractAppFactory;

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

	/**
	 * @param menu
	 * @param activity
	 * @param res
	 * @return the added menu item (also for a sub menu, then the menu item in the parent menu is returned)
	 */
	public static MenuItem addMenuItems(Menu menu,
			Activity activity, Resources res) {
		ArrayList<CacheListApp> activeApps = new ArrayList<CacheListApp>();
		for (CacheListApp app : getMultiPointNavigationApps(res)) {
			if (app.isInstalled(activity)) {
				activeApps.add(app);
			}
		}
		// use a new sub menu, if more than one app is available
		if (activeApps.size() > 1) {
			SubMenu subMenu = menu.addSubMenu(0, 101, 0,
					res.getString(R.string.caches_on_map)).setIcon(
					android.R.drawable.ic_menu_mapmode);
			for (CacheListApp app : activeApps) {
				subMenu.add(0, app.getId(), 0, app.getName());
			}
			return subMenu.getItem();
		} else if (activeApps.size() == 1) {
			return menu.add(0, activeApps.get(0).getId(), 0,
					activeApps.get(0).getName()).setIcon(android.R.drawable.ic_menu_mapmode);
		}
		return null;
	}

	public static boolean onMenuItemSelected(final MenuItem item,
			final cgGeo geo, final List<cgCache> caches, final Activity activity, final Resources res,
			final Integer searchId) {
		CacheListApp app = (CacheListApp) getAppFromMenuItem(
				item, apps);
		if (app != null) {
			try {
				return app.invoke(geo, caches, activity, res, searchId);
			} catch (Exception e) {
				Log.e(cgSettings.tag, "CacheListAppFactory.onMenuItemSelected: " + e.toString());
			}
		}
		return false;
	}

}
