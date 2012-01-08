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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public final class NavigationAppFactory extends AbstractAppFactory {
    private static NavigationApp[] apps = new NavigationApp[] {};

    private static NavigationApp[] getNavigationApps() {
        if (ArrayUtils.isEmpty(apps)) {
            apps = new NavigationApp[] {
                    new CompassApp(),
                    new RadarApp(),
                    new InternalMap(),
                    new StaticMapApp(),
                    new LocusApp(),
                    new RMapsApp(),
                    new GoogleMapsApp(),
                    new GoogleNavigationApp(),
                    new StreetviewApp(),
                    new OruxMapsApp(),
                    new NavigonApp() };
        }
        return apps;
    }

    public static void addMenuItems(final Menu menu, final Activity activity) {
        addMenuItems(menu, activity, true, false);
    }

    public static void addMenuItems(final Menu menu, final Activity activity,
            final boolean showInternalMap, final boolean showDefaultNavigation) {
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();
        for (NavigationApp app : getInstalledNavigationApps(activity)) {
            if ((showInternalMap || !(app instanceof InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != app.getId())) {
                menu.add(0, app.getId(), 0, app.getName());
            }
        }
    }

    public static List<NavigationApp> getInstalledNavigationApps(final Activity activity) {
        final List<NavigationApp> installedNavigationApps = new ArrayList<NavigationApp>();
        for (NavigationApp app : getNavigationApps()) {
            if (app.isInstalled(activity)) {
                installedNavigationApps.add(app);
            }
        }
        return installedNavigationApps;
    }

    public static int getOrdinalFromId(final Activity activity, final int id) {
        int ordinal = 0;
        for (NavigationApp app : getInstalledNavigationApps(activity)) {
            if (app.getId() == id) {
                return ordinal;
            }
            ordinal++;
        }
        return 0;
    }

    public static boolean onMenuItemSelected(final MenuItem item,
            final cgGeo geo, Activity activity, cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint destination) {
        if (cache == null && waypoint == null && destination == null) {
            return false;
        }

        final NavigationApp app = (NavigationApp) getAppFromMenuItem(item, apps);
        if (app != null) {
            try {
                return app.invoke(geo, activity, cache,
                        search, waypoint, destination);
            } catch (Exception e) {
                Log.e(Settings.tag, "NavigationAppFactory.onMenuItemSelected: " + e.toString());
            }
        }
        return false;
    }

    public static void startDefaultNavigationApplication(final cgGeo geo, Activity activity, cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint destination) {
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();

        NavigationApp app = null;
        final List<NavigationApp> installedNavigationApps = getInstalledNavigationApps(activity);

        if (defaultNavigationTool == 0) {
            // assume that 0 is the compass-app
            app = installedNavigationApps.get(0);
        } else {
            for (NavigationApp navigationApp : installedNavigationApps) {
                if (navigationApp.getId() == defaultNavigationTool) {
                    app = navigationApp;
                    break;
                }
            }
        }

        if (app != null) {
            try {
                app.invoke(geo, activity, cache, search, waypoint, destination);
            } catch (Exception e) {
                Log.e(Settings.tag, "NavigationAppFactory.startDefaultNavigationApplication: " + e.toString());
            }
        }
    }

}
