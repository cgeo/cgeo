package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.activity.IAbstractActivity;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import java.util.ArrayList;
import java.util.List;

public final class CacheListAppFactory extends AbstractAppFactory {
    private static class LazyHolder {
        public static final CacheListApp[] apps = {
                new InternalCacheListMap(),
                new LocusCacheListApp(false),
                new LocusCacheListApp(true)
        };
    }

    /**
     * @param menu
     * @param activity
     * @param res
     * @return the added menu item (also for a sub menu, then the menu item in the parent menu is returned)
     */
    public static MenuItem addMenuItems(final Menu menu, final Activity activity, final Resources res) {
        final List<CacheListApp> activeApps = new ArrayList<CacheListApp>(LazyHolder.apps.length);
        for (final CacheListApp app : LazyHolder.apps) {
            if (app.isInstalled()) {
                activeApps.add(app);
            }
        }
        // use a new sub menu, if more than one app is available
        switch (activeApps.size()) {
            case 0:
                return null;
            case 1:
                return menu.add(0, activeApps.get(0).getId(), 0,
                        activeApps.get(0).getName()).setIcon(R.drawable.ic_menu_mapmode);
            default:
                final SubMenu subMenu = menu.addSubMenu(0, 101, 0,
                        res.getString(R.string.caches_on_map)).setIcon(R.drawable.ic_menu_mapmode);
                for (final CacheListApp app : activeApps) {
                    subMenu.add(0, app.getId(), 0, app.getName());
                }
                return subMenu.getItem();
        }
    }

    public static boolean onMenuItemSelected(final MenuItem item, final List<cgCache> caches, final IAbstractActivity activity,
            final SearchResult search) {
        final CacheListApp app = (CacheListApp) getAppFromMenuItem(item, LazyHolder.apps);
        if (app != null) {
            try {
                boolean result = app.invoke(caches, (Activity) activity, search);
                activity.invalidateOptionsMenuCompatible();
                return result;
            } catch (Exception e) {
                Log.e("CacheListAppFactory.onMenuItemSelected: " + e.toString());
            }
        }
        return false;
    }

}
