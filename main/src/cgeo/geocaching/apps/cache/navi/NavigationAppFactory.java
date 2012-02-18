package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.StaticMapsProvider;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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
    }

    /**
     * Default way to handle selection of navigation tool.<br />
     * A dialog is created for tool selection and the selected tool is started afterwards.
     * <p />
     * Delegates to
     * {@link #showNavigationMenu(cgGeo, Activity, cgCache, SearchResult, cgWaypoint, Geopoint, boolean, boolean)} with
     * <code>showInternalMap = true</code> and <code>showDefaultNavigation = false</code>
     *
     * @param geo
     * @param activity
     * @param cache
     * @param waypoint
     * @param destination
     */
    public static void showNavigationMenu(final cgGeo geo, final Activity activity,
            final cgCache cache, final cgWaypoint waypoint, final Geopoint destination) {
        showNavigationMenu(geo, activity, cache, waypoint, destination, true, false);
    }

    /**
     * Specialized way to handle selection of navigation tool.<br />
     * A dialog is created for tool selection and the selected tool is started afterwards.
     * 
     * @param geo
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
     * @see #showNavigationMenu(cgGeo, Activity, cgCache, cgWaypoint, Geopoint)
     */
    public static void showNavigationMenu(final cgGeo geo, final Activity activity,
            final cgCache cache, final cgWaypoint waypoint, final Geopoint destination,
            final boolean showInternalMap, final boolean showDefaultNavigation) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cache_menu_navigate);
        builder.setIcon(android.R.drawable.ic_menu_mapmode);
        final List<NavigationAppsEnum> items = new ArrayList<NavigationAppFactory.NavigationAppsEnum>();
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();
        final boolean hasStaticMaps = hasStaticMap(cache, waypoint);
        for (NavigationAppsEnum navApp : getInstalledNavigationApps(activity)) {
            if (NavigationAppsEnum.STATIC_MAP.id == navApp.id) {
                if (hasStaticMaps) {
                    items.add(navApp);
                }
            } else if ((showInternalMap || !(navApp.app instanceof InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != navApp.id)) {
                items.add(navApp);
            }
        }
        /*
         * Using an ArrayAdapter with list of NavigationAppsEnum items avoids
         * handling between mapping list positions allows us to do dynamic filtering of the list based on usecase.
         */
        final ArrayAdapter<NavigationAppsEnum> adapter = new ArrayAdapter<NavigationAppsEnum>(activity, android.R.layout.select_dialog_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(getItem(position).app.getName());
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setText(getItem(position).app.getName());
                return textView;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.select_dialog_item);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                NavigationAppsEnum selectedItem = adapter.getItem(item);
                selectedItem.app.invoke(geo, activity, cache, waypoint, destination);
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private static boolean hasStaticMap(cgCache cache, cgWaypoint waypoint) {
        if (waypoint != null) {
            String geocode = waypoint.getGeocode();
            int id = waypoint.getId();
            if (StringUtils.isNotEmpty(geocode) && cgeoapplication.getInstance().isOffline(geocode, null)) {
                return StaticMapsProvider.doesExistStaticMapForWaypoint(geocode, id);
            }
        }
        if (cache != null) {
            String geocode = cache.getGeocode();
            if (StringUtils.isNotEmpty(geocode) && cgeoapplication.getInstance().isOffline(geocode, null)) {
                return StaticMapsProvider.doesExistStaticMapForCache(geocode);
            }
        }
        return false;
    }

    /**
     * Returns all installed navigation apps.
     *
     * @param activity
     * @return
     */
    public static List<NavigationAppsEnum> getInstalledNavigationApps(final Activity activity) {
        final List<NavigationAppsEnum> installedNavigationApps = new ArrayList<NavigationAppsEnum>();
        for (NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled(activity)) {
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
     * Use {@link #onMenuItemSelected(MenuItem, cgGeo, Activity, cgCache, SearchResult, cgWaypoint, Geopoint)} on
     * selection event to start the selected navigation tool.
     *
     * <b>Only use this way if {@link #showNavigationMenu(cgGeo, Activity, cgCache, SearchResult, cgWaypoint, Geopoint)}
     * is not suitable for the given usecase.</b>
     *
     * @param menu
     * @param activity
     */
    public static void addMenuItems(final Menu menu, final Activity activity) {
        addMenuItems(menu, activity, true, false);
    }

    /**
     * Adds the installed navigation tools to the given menu.
     * Use {@link #onMenuItemSelected(MenuItem, cgGeo, Activity, cgCache, cgWaypoint, Geopoint)} on
     * selection event to start the selected navigation tool.
     *
     * <b>Only use this way if
     * {@link #showNavigationMenu(cgGeo, Activity, cgCache, cgWaypoint, Geopoint, boolean, boolean)} is
     * not suitable for the given usecase.</b>
     *
     * @param menu
     * @param activity
     * @param showInternalMap
     * @param showDefaultNavigation
     */
    public static void addMenuItems(final Menu menu, final Activity activity,
            final boolean showInternalMap, final boolean showDefaultNavigation) {
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();
        for (NavigationAppsEnum navApp : getInstalledNavigationApps(activity)) {
            if ((showInternalMap || !(navApp.app instanceof InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != navApp.id)) {
                menu.add(0, MENU_ITEM_OFFSET + navApp.id, 0, navApp.app.getName());
            }
        }
    }

    /**
     * Handles menu selections for menu entries created with {@link #addMenuItems(Menu, Activity)} or
     * {@link #addMenuItems(Menu, Activity, boolean, boolean)}.
     *
     * @param item
     * @param geo
     * @param activity
     * @param cache
     * @param waypoint
     * @param destination
     * @return
     */
    public static boolean onMenuItemSelected(final MenuItem item,
            final cgGeo geo, Activity activity, cgCache cache, cgWaypoint waypoint, final Geopoint destination) {
        if (cache == null && waypoint == null && destination == null) {
            return false;
        }

        final NavigationApp app = getAppFromMenuItem(item);
        if (app != null) {
            try {
                return app.invoke(geo, activity, cache, waypoint, destination);
            } catch (Exception e) {
                Log.e(Settings.tag, "NavigationAppFactory.onMenuItemSelected: " + e.toString());
            }
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
     * @param geo
     * @param activity
     * @param cache
     * @param search
     * @param waypoint
     * @param destination
     */
    public static void startDefaultNavigationApplication(final cgGeo geo, Activity activity, cgCache cache,
            cgWaypoint waypoint, final Geopoint destination) {
        final NavigationApp app = getDefaultNavigationApplication(activity);

        if (app != null) {
            try {
                app.invoke(geo, activity, cache, waypoint, destination);
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
