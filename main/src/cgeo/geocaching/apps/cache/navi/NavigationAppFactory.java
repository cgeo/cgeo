package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.ArrayUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public final class NavigationAppFactory extends AbstractAppFactory {
    private static NavigationApp[] apps = new NavigationApp[] {};

    private static NavigationApp[] getNavigationApps(Resources res) {
        if (ArrayUtils.isEmpty(apps)) {
            apps = new NavigationApp[] {
                    new CompassApp(res),
                    new RadarApp(res),
                    new InternalMap(res),
                    new StaticMapApp(res),
                    new LocusApp(res),
                    new RMapsApp(res),
                    new GoogleMapsApp(res),
                    new GoogleNavigationApp(res),
                    new StreetviewApp(res),
                    new OruxMapsApp(res) };
        }
        return apps;
    }

    public static void addMenuItems(final Menu menu, final Activity activity,
            final Resources res) {
        addMenuItems(menu, activity, res, true, false);
    }

    public static void addMenuItems(final Menu menu, final Activity activity,
            final Resources res, final boolean showInternalMap, final boolean showDefaultNavigation) {
        int defaultNavigationTool = Settings.getDefaultNavigationTool();
        for (NavigationApp app : getInstalledNavigationApps(activity, res)) {
            if ((showInternalMap || !(app instanceof InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != app.getId())) {
                menu.add(0, app.getId(), 0, app.getName());
            }
        }
    }

    public static List<NavigationApp> getInstalledNavigationApps(final Activity activity, final Resources res) {
        List<NavigationApp> installedNavigationApps = new ArrayList<NavigationApp>();
        for (NavigationApp app : getNavigationApps(res)) {
            if (app.isInstalled(activity)) {
                installedNavigationApps.add(app);
            }
        }
        return installedNavigationApps;
    }

    public static int getOrdinalFromId(final Activity activity, final Resources res, final int id) {
        int ordinal = 0;
        for (NavigationApp app : getInstalledNavigationApps(activity, res)) {
            if (app.getId() == id) {
                return ordinal;
            }
            ordinal++;
        }
        return 0;
    }

    public static boolean onMenuItemSelected(final MenuItem item,
            final cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint destination) {
        NavigationApp app = (NavigationApp) getAppFromMenuItem(item, apps);
        if (app != null) {
            try {
                return app.invoke(geo, activity, res, cache,
                        search, waypoint, destination);
            } catch (Exception e) {
                Log.e(Settings.tag, "NavigationAppFactory.onMenuItemSelected: " + e.toString());
            }
        }
        return false;
    }

    public static void startDefaultNavigationApplication(final cgGeo geo, Activity activity, Resources res, cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint destination) {
        int defaultNavigationTool = Settings.getDefaultNavigationTool();
        if (defaultNavigationTool == 0) {
            return;
        }
        NavigationApp app = null;
        List<NavigationApp> installedNavigationApps = getInstalledNavigationApps(activity, res);
        for (NavigationApp navigationApp : installedNavigationApps) {
            if (navigationApp.getId() == defaultNavigationTool) {
                app = navigationApp;
                break;
            }
        }
        if (app != null) {
            try {
                app.invoke(geo, activity, res, cache, search, waypoint, destination);
            } catch (Exception e) {
                Log.e(Settings.tag, "NavigationAppFactory.startDefaultNavigationApplication: " + e.toString());
            }
        }
    }

}
