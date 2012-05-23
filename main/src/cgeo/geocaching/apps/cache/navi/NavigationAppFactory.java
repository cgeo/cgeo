package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public final class NavigationAppFactory extends AbstractAppFactory {

    public enum NavigationAppsEnum {
        /** The internal compass activity */
        COMPASS(new CompassApp(), 0),
        /** The external radar app */
        RADAR(new RadarApp(), 1),
        /** The selected map */
        INTERNAL_MAP(new InternalMap(), 2),
        /** The internal static map activity */
        STATIC_MAP(new StaticMapApp(), 3),
        /** The external Locus app */
        DOWNLOAD_STATIC_MAPS(new DownloadStaticMapsApp(), 20),
        /** The external Locus app */
        LOCUS(new LocusApp(), 4),
        /** The external RMaps app */
        RMAPS(new RMapsApp(), 5),
        /** Google Maps */
        GOOGLE_MAPS(new GoogleMapsApp(), 6),
        /** Google Navigation */
        GOOGLE_NAVIGATION(new GoogleNavigationApp(), 7),
        /** Google Streetview */
        GOOGLE_STREETVIEW(new StreetviewApp(), 8),
        /** The external OruxMaps app */
        ORUX_MAPS(new OruxMapsApp(), 9),
        /** The external navigon app */
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

        /*
         * display app name in array adapter
         *
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return app.getName();
        }
    }

    /**
     * Default way to handle selection of navigation tool.<br />
     * A dialog is created for tool selection and the selected tool is started afterwards.
     * <p />
     * Delegates to {@link #showNavigationMenu(Activity, cgCache, cgWaypoint, Geopoint, boolean, boolean)} with
     * <code>showInternalMap = true</code> and <code>showDefaultNavigation = false</code>
     *
     * @param activity
     * @param cache
     * @param waypoint
     * @param destination
     */
    public static void showNavigationMenu(final Activity activity,
            final cgCache cache, final cgWaypoint waypoint, final Geopoint destination) {
        showNavigationMenu(activity, cache, waypoint, destination, true, false);
    }

    /**
     * Specialized way to handle selection of navigation tool.<br />
     * A dialog is created for tool selection and the selected tool is started afterwards.
     *
     * @param activity
     * @param cache
     *            may be <code>null</code>
     * @param waypoint
     *            may be <code>null</code>
     * @param destination
     *            may be <code>null</code>
     * @param showInternalMap
     *            should be <code>false</code> only when called from within the internal map
     * @param showDefaultNavigation
     *            should be <code>false</code> by default
     *
     * @see #showNavigationMenu(Activity, cgCache, cgWaypoint, Geopoint)
     */
    public static void showNavigationMenu(final Activity activity,
            final cgCache cache, final cgWaypoint waypoint, final Geopoint destination,
            final boolean showInternalMap, final boolean showDefaultNavigation) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cache_menu_navigate);
        builder.setIcon(R.drawable.ic_menu_mapmode);
        final List<NavigationAppsEnum> items = new ArrayList<NavigationAppFactory.NavigationAppsEnum>();
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();
        for (NavigationAppsEnum navApp : getInstalledNavigationApps()) {
            if ((showInternalMap || !(navApp.app instanceof InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != navApp.id)) {
                if (navApp.app.isEnabled(cache) || navApp.app.isEnabled(waypoint) || navApp.app.isEnabled(destination)) {
                    items.add(navApp);
                }
            }
        }
        /*
         * Using an ArrayAdapter with list of NavigationAppsEnum items avoids
         * handling between mapping list positions allows us to do dynamic filtering of the list based on use case.
         */
        final ArrayAdapter<NavigationAppsEnum> adapter = new ArrayAdapter<NavigationAppsEnum>(activity, android.R.layout.select_dialog_item, items);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                NavigationAppsEnum selectedItem = adapter.getItem(item);
                selectedItem.app.invoke(activity, cache, waypoint, destination);
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Returns all installed navigation apps.
     *
     * @return
     */
    public static List<NavigationAppsEnum> getInstalledNavigationApps() {
        final List<NavigationAppsEnum> installedNavigationApps = new ArrayList<NavigationAppsEnum>();
        for (NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled()) {
                installedNavigationApps.add(appEnum);
            }
        }
        return installedNavigationApps;
    }

    /**
     * This offset is used to build unique menu ids to avoid collisions of ids in menus
     */
    private static final int MENU_ITEM_OFFSET = 12345;


    /**
     * Adds the installed navigation tools to the given menu.
     * Use {@link #onMenuItemSelected(MenuItem, Activity, cgCache)} on
     * selection event to start the selected navigation tool.
     *
     * <b>Only use this way if {@link #showNavigationMenu(Activity, cgCache, cgWaypoint, Geopoint, boolean, boolean)} is
     * not suitable for the given usecase.</b>
     *
     * @param menu
     */
    public static void addMenuItems(final Menu menu, final cgCache cache) {
        for (NavigationAppsEnum navApp : getInstalledNavigationApps()) {
            if (navApp.app.isEnabled(cache)) {
                menu.add(0, MENU_ITEM_OFFSET + navApp.id, 0, navApp.app.getName());
            }
        }
    }

    public static void addMenuItems(final Menu menu, final cgWaypoint waypoint) {
        for (NavigationAppsEnum navApp : getInstalledNavigationApps()) {
            if (navApp.app.isEnabled(waypoint)) {
                menu.add(0, MENU_ITEM_OFFSET + navApp.id, 0, navApp.app.getName());
            }
        }
    }

    /**
     * Handles menu selections for menu entries created with {@link #addMenuItems(Menu, cgCache)}.
     *
     * @param item
     * @param activity
     * @param cache
     * @return
     */
    public static boolean onMenuItemSelected(final MenuItem item, Activity activity, cgCache cache) {
        return invokeApp(activity, cache, null, null, getAppFromMenuItem(item));
    }

    public static boolean onMenuItemSelected(final MenuItem item, Activity activity, cgWaypoint waypoint) {
        return invokeApp(activity, null, waypoint, null, getAppFromMenuItem(item));
    }

    private static boolean invokeApp(Activity activity, cgCache cache, cgWaypoint waypoint, final Geopoint destination, final NavigationApp app) {
        if (app == null) {
            return false;
        }
        if (cache == null && waypoint == null && destination == null) {
            return false;
        }
        try {
            return app.invoke(activity, cache, waypoint, destination);
        } catch (Exception e) {
            Log.e("NavigationAppFactory.onMenuItemSelected: " + e.toString());
        }
        return false;
    }

    private static NavigationApp getAppFromMenuItem(MenuItem item) {
        final int id = item.getItemId();
        for (NavigationAppsEnum navApp : NavigationAppsEnum.values()) {
            if (MENU_ITEM_OFFSET + navApp.id == id) {
                return navApp.app;
            }
        }
        return null;
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     *
     * @param defaultNavigation
     *
     * @param activity
     * @param cache
     */
    public static void startDefaultNavigationApplication(int defaultNavigation, Activity activity, cgCache cache) {
        if (cache == null || cache.getCoords() == null) {
            ActivityMixin.showToast(activity, cgeoapplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }

        invokeApp(activity, cache, null, null, getDefaultNavigationApplication(defaultNavigation));
    }

    private static NavigationApp getDefaultNavigationApplication(int defaultNavigation) {
        if (defaultNavigation == 2) {
            return getNavigationAppFromSetting(Settings.getDefaultNavigationTool2());
        }
        return getNavigationAppFromSetting(Settings.getDefaultNavigationTool());
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     *
     * @param activity
     * @param waypoint
     */
    public static void startDefaultNavigationApplication(int defaultNavigation, Activity activity, cgWaypoint waypoint) {
        if (waypoint == null || waypoint.getCoords() == null) {
            ActivityMixin.showToast(activity, cgeoapplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }
        invokeApp(activity, null, waypoint, null, getDefaultNavigationApplication(defaultNavigation));
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     *
     * @param activity
     * @param destination
     */
    public static void startDefaultNavigationApplication(int defaultNavigation, Activity activity, final Geopoint destination) {
        if (destination == null) {
            ActivityMixin.showToast(activity, cgeoapplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }

        invokeApp(activity, null, null, destination, getDefaultNavigationApplication(defaultNavigation));
    }

    /**
     * Returns the default navigation tool if correctly set and installed or the compass app as default fallback
     *
     * @return never <code>null</code>
     */
    public static NavigationApp getDefaultNavigationApplication() {
        return getNavigationAppFromSetting(Settings.getDefaultNavigationTool());
    }

    private static NavigationApp getNavigationAppFromSetting(final int defaultNavigationTool) {
        final List<NavigationAppsEnum> installedNavigationApps = getInstalledNavigationApps();

        for (NavigationAppsEnum navigationApp : installedNavigationApps) {
            if (navigationApp.id == defaultNavigationTool) {
                return navigationApp.app;
            }
        }
        // default navigation tool wasn't set already or couldn't be found (not installed any more for example)
        return NavigationAppsEnum.COMPASS.app;
    }

}
