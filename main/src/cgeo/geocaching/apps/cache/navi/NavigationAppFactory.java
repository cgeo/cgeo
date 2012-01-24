package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public final class NavigationAppFactory extends AbstractAppFactory {

    public enum NavigationAppsEnum {
        COMPASS(new CompassApp(), 0),
        RADAR(new RadarApp(), 1),
        INTERNAL_MAP(new InternalMap(), 2),
        STATIC_MAP(new StaticMapApp(), 3),
        LOCUS(new LocusApp(), 4),
        RMAPS(new RMapsApp(), 5),
        GOOGLE_MAPS(new GoogleMapsApp(), 6),
        GOOGLE_NAVIGATION(new GoogleNavigationApp(), 7),
        GOOGLE_STREETVIEW(new StreetviewApp(), 8),
        ORUX_MAPS(new OruxMapsApp(), 9),
        NAVIGON(new NavigonApp(), 10);

        NavigationAppsEnum(NavigationApp app, int id) {
            this.app = app;
            this.id = id;
        }

        /**
         * The app instance to use
         */
        public final NavigationApp app;
        /**
         * The id - used in c:geo settings
         */
        public final int id;
    }

    public static void addMenuItems(final Menu menu, final Activity activity) {
        addMenuItems(menu, activity, true, false);
    }

    public static void addMenuItems(final Menu menu, final Activity activity,
            final boolean showInternalMap, final boolean showDefaultNavigation) {
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();
        for (NavigationAppsEnum navApp : getInstalledNavigationApps(activity)) {
            if ((showInternalMap || !(navApp.app instanceof InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != navApp.id)) {
                menu.add(0, navApp.id, 0, navApp.app.getName());
            }
        }
    }

    public static void showNavigationMenu(final cgGeo geo, final Activity activity, final cgCache cache, final SearchResult search) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cache_menu_navigate);
        builder.setIcon(android.R.drawable.ic_menu_mapmode);
        final List<NavigationAppsEnum> installed = getInstalledNavigationApps(activity);
        final String[] items = new String[installed.size()];
        for (int i = 0; i < installed.size(); i++) {
            items[i] = installed.get(i).app.getName();
        }
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                installed.get(item).app.invoke(geo, activity, cache, search, null, null);
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();

    }

    public static List<NavigationAppsEnum> getInstalledNavigationApps(final Activity activity) {
        final List<NavigationAppsEnum> installedNavigationApps = new ArrayList<NavigationAppsEnum>();
        for (NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled(activity)) {
                installedNavigationApps.add(appEnum);
            }
        }
        return installedNavigationApps;
    }

    public static boolean onMenuItemSelected(final MenuItem item,
            final cgGeo geo, Activity activity, cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint destination) {
        if (cache == null && waypoint == null && destination == null) {
            return false;
        }

        final NavigationApp app = getAppFromMenuItem(item);
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

    public static NavigationApp getAppFromMenuItem(MenuItem item) {
        final int id = item.getItemId();
        for (NavigationAppsEnum navApp : NavigationAppsEnum.values()) {
            if (navApp.id == id) {
                return navApp.app;
            }
        }
        return null;
    }

    public static void startDefaultNavigationApplication(final cgGeo geo, Activity activity, cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint destination) {
        final NavigationApp app = getDefaultNavigationApplication(activity);

        if (app != null) {
            try {
                app.invoke(geo, activity, cache, search, waypoint, destination);
            } catch (Exception e) {
                Log.e(Settings.tag, "NavigationAppFactory.startDefaultNavigationApplication: " + e.toString());
            }
        }
    }

    /**
     * Returns the default navigation tool if correctly set and installed or the compass app as default fallback
     *
     * @param activity
     * @return never <code>null</code>
     */
    public static NavigationApp getDefaultNavigationApplication(Activity activity) {
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();

        final List<NavigationAppsEnum> installedNavigationApps = getInstalledNavigationApps(activity);

        for (NavigationAppsEnum navigationApp : installedNavigationApps) {
            if (navigationApp.id == defaultNavigationTool) {
                return navigationApp.app;
            }
        }
        // default navigation tool wasn't set already or couldn't be found (not installed any more for example)
        return NavigationAppsEnum.COMPASS.app;
    }

}
