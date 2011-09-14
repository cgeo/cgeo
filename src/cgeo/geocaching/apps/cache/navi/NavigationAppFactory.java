package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.ArrayUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.UUID;

public final class NavigationAppFactory extends AbstractAppFactory {
    private static NavigationApp[] apps = new NavigationApp[] {};

    private static NavigationApp[] getNavigationApps(Resources res) {
        if (ArrayUtils.isEmpty(apps)) {
            apps = new NavigationApp[] {
                    // compass
                    new RadarApp(res),
                    new InternalMap(res),
                    new StaticMapApp(res),
                    new LocusApp(res),
                    new RMapsApp(res),
                    new GoogleMapsApp(res),
                    new GoogleNavigationApp(res),
                    new StreetviewApp(res) };
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
            final UUID searchId, cgWaypoint waypoint, final Geopoint destination) {
        NavigationApp app = (NavigationApp) getAppFromMenuItem(item, apps);
        if (app != null) {
            try {
                return app.invoke(geo, activity, res, cache,
                        searchId, waypoint, destination);
            } catch (Exception e) {
                Log.e(cgSettings.tag, "NavigationAppFactory.onMenuItemSelected: " + e.toString());
            }
        }
        return false;
    }

}
