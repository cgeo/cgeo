package cgeo.geocaching.apps.cache;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.Settings;
import cgeo.geocaching.apps.AbstractAppFactory;

import org.apache.commons.lang3.ArrayUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public final class GeneralAppsFactory extends AbstractAppFactory {
    private static GeneralApp[] apps = new GeneralApp[] {};

    private static GeneralApp[] getGeneralApps(Resources res) {
        if (ArrayUtils.isEmpty(apps)) {
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
            try {
                return app.invoke(activity, cache);
            } catch (Exception e) {
                Log.e(Settings.tag, "GeneralAppsFactory.onMenuItemSelected: " + e.toString());
            }
        }
        return false;
    }

}
