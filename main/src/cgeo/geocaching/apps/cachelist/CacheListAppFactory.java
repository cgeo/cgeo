package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.ActivityMixin;
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
                new LocusShowCacheListApp(),
                new LocusExportCacheListApp(),
                new MapsWithMeCacheListApp()
        };
    }

    /**
     * @param menu
     * @param activity
     * @param res
     */
    public static void addMenuItems(final Menu menu, final Activity activity, final Resources res) {
        final List<CacheListApp> activeApps = getActiveApps();
        if (activeApps.isEmpty()) {
            return;
        }
        if (activeApps.size() == 1) {
            final MenuItem subItem = menu.findItem(R.id.menu_cache_list_app);
            subItem.setVisible(true);
            subItem.setTitle(activeApps.get(0).getName());
        } else {
            final MenuItem subItem = menu.findItem(R.id.submenu_cache_list_app);
            subItem.setVisible(true);
            final SubMenu subMenu = subItem.getSubMenu();
            for (final CacheListApp app : activeApps) {
                subMenu.add(0, app.getId(), 0, app.getName());
            }
        }
    }

    private static List<CacheListApp> getActiveApps() {
        final List<CacheListApp> activeApps = new ArrayList<CacheListApp>(LazyHolder.apps.length);
        for (final CacheListApp app : LazyHolder.apps) {
            if (app.isInstalled()) {
                activeApps.add(app);
            }
        }
        return activeApps;
    }

    public static boolean onMenuItemSelected(final MenuItem item, final List<Geocache> caches, final Activity activity,
            final SearchResult search) {
        CacheListApp app;
        if (item.getItemId() == R.id.menu_cache_list_app) {
            app = getActiveApps().get(0);
        }
        else {
            app = (CacheListApp) getAppFromMenuItem(item, LazyHolder.apps);
        }
        if (app != null) {
            try {
                boolean result = app.invoke(caches, activity, search);
                ActivityMixin.invalidateOptionsMenu(activity);
                return result;
            } catch (Exception e) {
                Log.e("CacheListAppFactory.onMenuItemSelected", e);
            }
        }
        return false;
    }

}
