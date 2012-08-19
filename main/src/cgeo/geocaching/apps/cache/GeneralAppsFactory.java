package cgeo.geocaching.apps.cache;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.ArrayUtils;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

public final class GeneralAppsFactory extends AbstractAppFactory {
    private static GeneralApp[] apps = new GeneralApp[] {};

    private static GeneralApp[] getGeneralApps() {
        if (ArrayUtils.isEmpty(apps)) {
            apps = new GeneralApp[] {
                    new CacheBeaconApp(),
                    new GccApp(),
                    new WhereYouGoApp()
            };
        }
        return apps;
    }

    public static void addMenuItems(Menu menu, cgCache cache) {
        for (GeneralApp app : getGeneralApps()) {
            if (app.isInstalled() && app.isEnabled(cache)) {
                menu.add(0, app.getId(), 0, app.getName());
            }
        }
    }

    public static boolean onMenuItemSelected(final MenuItem item, Activity activity, cgCache cache) {
        final GeneralApp app = (GeneralApp) getAppFromMenuItem(item, apps);
        if (app == null) {
            return false;
        }
        try {
            app.invoke(activity, cache);
        } catch (Exception e) {
            Log.e("GeneralAppsFactory.onMenuItemSelected: " + e.toString());
        }
        return true;
    }

}
