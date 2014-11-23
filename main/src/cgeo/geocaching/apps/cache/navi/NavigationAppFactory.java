package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.apps.cache.WhereYouGoApp;
import cgeo.geocaching.apps.cache.navi.GoogleNavigationApp.GoogleNavigationBikeApp;
import cgeo.geocaching.apps.cache.navi.GoogleNavigationApp.GoogleNavigationDrivingApp;
import cgeo.geocaching.apps.cache.navi.GoogleNavigationApp.GoogleNavigationWalkingApp;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;

import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public final class NavigationAppFactory extends AbstractAppFactory {

    private NavigationAppFactory() {
        // utility class
    }

    public enum NavigationAppsEnum {
        /** The internal compass activity */
        COMPASS(new CompassApp(), 0, R.string.pref_navigation_menu_compass),
        /** The external radar app */
        RADAR(new RadarApp(), 1, R.string.pref_navigation_menu_radar),
        /** The selected map */
        INTERNAL_MAP(new InternalMap(), 2, R.string.pref_navigation_menu_internal_map),
        /** The internal static map activity, when stored */
        STATIC_MAP(new StaticMapApp(), 3, R.string.pref_navigation_menu_static_map),
        /** The internal static map activity, when not yet stored */
        DOWNLOAD_STATIC_MAPS(new DownloadStaticMapsApp(), 20, R.string.pref_navigation_menu_static_map_download),
        /** The external Locus app */
        LOCUS(new LocusApp(), 4, R.string.pref_navigation_menu_locus),
        /** The external RMaps app */
        RMAPS(new RMapsApp(), 5, R.string.pref_navigation_menu_rmaps),
        /** Google Maps */
        GOOGLE_MAPS(new GoogleMapsApp(), 6, R.string.pref_navigation_menu_google_maps),
        /** Google Navigation */
        GOOGLE_NAVIGATION(new GoogleNavigationDrivingApp(), 7, R.string.pref_navigation_menu_google_navigation),
        /** Google Streetview */
        GOOGLE_STREETVIEW(new StreetviewApp(), 8, R.string.pref_navigation_menu_google_streetview),
        /** The external OruxMaps app */
        ORUX_MAPS(new OruxMapsApp(), 9, R.string.pref_navigation_menu_oruxmaps),
        /** The external navigon app */
        NAVIGON(new NavigonApp(), 10, R.string.pref_navigation_menu_navigon),
        /** The external Sygic app */
        SYGIC(new SygicNavigationApp(), 11, R.string.pref_navigation_menu_sygic),
        /**
         * Google Navigation in walking mode
         */
        GOOGLE_NAVIGATION_WALK(new GoogleNavigationWalkingApp(), 12, R.string.pref_navigation_menu_google_walk),
        /**
         * Google Navigation in walking mode
         */
        GOOGLE_NAVIGATION_BIKE(new GoogleNavigationBikeApp(), 21, R.string.pref_navigation_menu_google_bike),
        /**
         * Google Maps Directions
         */
        GOOGLE_MAPS_DIRECTIONS(new GoogleMapsDirectionApp(), 13, R.string.pref_navigation_menu_google_maps_directions),

        WHERE_YOU_GO(new WhereYouGoApp(), 16, R.string.pref_navigation_menu_where_you_go),
        PEBBLE(new PebbleApp(), 17, R.string.pref_navigation_menu_pebble),
        ANDROID_WEAR(new AndroidWearApp(), 18, R.string.pref_navigation_menu_android_wear),
        MAPSWITHME(new MapsWithMeApp(), 22, R.string.pref_navigation_menu_mapswithme);

        NavigationAppsEnum(final App app, final int id, final int preferenceKey) {
            this.app = app;
            this.id = id;
            this.preferenceKey = preferenceKey;
        }

        /**
         * The app instance to use
         */
        public final App app;
        /**
         * The id - used in c:geo settings
         */
        public final int id;

        /**
         * key of the related preference in the navigation menu preference screen, used for disabling the preference UI
         */
        public final int preferenceKey;

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
     * Delegates to {@link #showNavigationMenu(Activity, cgeo.geocaching.Geocache, cgeo.geocaching.Waypoint, Geopoint, boolean, boolean)} with
     * <code>showInternalMap = true</code> and <code>showDefaultNavigation = false</code>
     *
     * @param activity
     * @param cache
     * @param waypoint
     * @param destination
     */
    public static void showNavigationMenu(final Activity activity,
            final Geocache cache, final Waypoint waypoint, final Geopoint destination) {
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
     * @see #showNavigationMenu(Activity, cgeo.geocaching.Geocache, cgeo.geocaching.Waypoint, Geopoint)
     */
    public static void showNavigationMenu(final Activity activity,
            final Geocache cache, final Waypoint waypoint, final Geopoint destination,
            final boolean showInternalMap, final boolean showDefaultNavigation) {
        final List<NavigationAppsEnum> items = new ArrayList<>();
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();
        for (final NavigationAppsEnum navApp : getActiveNavigationApps()) {
            if ((showInternalMap || !(navApp.app instanceof InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != navApp.id)) {
                boolean add = false;
                if (cache != null && navApp.app instanceof CacheNavigationApp && navApp.app.isEnabled(cache)) {
                    add = true;
                }
                if (waypoint != null && navApp.app instanceof WaypointNavigationApp && ((WaypointNavigationApp) navApp.app).isEnabled(waypoint)) {
                    add = true;
                }
                if (destination != null && navApp.app instanceof GeopointNavigationApp) {
                    add = true;
                }
                if (add) {
                    items.add(navApp);
                }
            }
        }

        if (items.size() == 1) {
            invokeNavigation(activity, cache, waypoint, destination, items.get(0).app);
            return;
        }

        /*
         * Using an ArrayAdapter with list of NavigationAppsEnum items avoids
         * handling between mapping list positions allows us to do dynamic filtering of the list based on use case.
         */
        final ArrayAdapter<NavigationAppsEnum> adapter = new ArrayAdapter<>(activity, android.R.layout.select_dialog_item, items);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cache_menu_navigate);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int item) {
                final NavigationAppsEnum selectedItem = adapter.getItem(item);
                invokeNavigation(activity, cache, waypoint, destination, selectedItem.app);
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
    static List<NavigationAppsEnum> getInstalledNavigationApps() {
        final List<NavigationAppsEnum> installedNavigationApps = new ArrayList<>();
        for (final NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled()) {
                installedNavigationApps.add(appEnum);
            }
        }
        return installedNavigationApps;
    }

    /**
     * @return all navigation apps, which are installed and activated in the settings
     */
    static List<NavigationAppsEnum> getActiveNavigationApps() {
        final List<NavigationAppsEnum> activeApps = new ArrayList<>();
        for (final NavigationAppsEnum appEnum : getInstalledNavigationApps()) {
            if (Settings.isUseNavigationApp(appEnum)) {
                activeApps.add(appEnum);
            }
        }
        return activeApps;
    }

    /**
     * Returns all installed navigation apps for default navigation.
     *
     * @return
     */
    public static List<NavigationAppsEnum> getInstalledDefaultNavigationApps() {
        final List<NavigationAppsEnum> installedNavigationApps = new ArrayList<>();
        for (final NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled() && appEnum.app.isUsableAsDefaultNavigationApp()) {
                installedNavigationApps.add(appEnum);
            }
        }
        return installedNavigationApps;
    }

    /**
     * Handles menu selections for menu entries created with
     * {@link #showNavigationMenu(Activity, Geocache, Waypoint, Geopoint)}.
     *
     * @param item
     * @param activity
     * @param cache
     * @return
     */
    public static boolean onMenuItemSelected(final MenuItem item, final Activity activity, final Geocache cache) {
        final App menuItem = getAppFromMenuItem(item);
        navigateCache(activity, cache, menuItem);
        return menuItem != null;
    }

    private static void navigateCache(final Activity activity, final Geocache cache, @Nullable final App app) {
        if (app instanceof CacheNavigationApp) {
            final CacheNavigationApp cacheApp = (CacheNavigationApp) app;
            cacheApp.navigate(activity, cache);
        }
    }

    private static void navigateWaypoint(final Activity activity, final Waypoint waypoint, @Nullable final App app) {
        if (app instanceof WaypointNavigationApp) {
            final WaypointNavigationApp waypointApp = (WaypointNavigationApp) app;
            waypointApp.navigate(activity, waypoint);
        }
    }

    private static void navigateGeopoint(final Activity activity, final Geopoint destination, final App app) {
        if (app instanceof GeopointNavigationApp) {
            final GeopointNavigationApp geopointApp = (GeopointNavigationApp) app;
            geopointApp.navigate(activity, destination);
        }
    }

    @Nullable
    private static App getAppFromMenuItem(final MenuItem item) {
        final int id = item.getItemId();
        for (final NavigationAppsEnum navApp : NavigationAppsEnum.values()) {
            if (navApp.id == id) {
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
    public static void startDefaultNavigationApplication(final int defaultNavigation, final Activity activity, final Geocache cache) {
        if (cache == null || cache.getCoords() == null) {
            ActivityMixin.showToast(activity, CgeoApplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }

        navigateCache(activity, cache, getDefaultNavigationApplication(defaultNavigation));
    }

    private static App getDefaultNavigationApplication(final int defaultNavigation) {
        if (defaultNavigation == 2) {
            return getNavigationAppForId(Settings.getDefaultNavigationTool2());
        }
        return getNavigationAppForId(Settings.getDefaultNavigationTool());
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     *
     * @param activity
     * @param waypoint
     */
    public static void startDefaultNavigationApplication(final int defaultNavigation, final Activity activity, final Waypoint waypoint) {
        if (waypoint == null || waypoint.getCoords() == null) {
            ActivityMixin.showToast(activity, CgeoApplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }
        navigateWaypoint(activity, waypoint, getDefaultNavigationApplication(defaultNavigation));
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     *
     * @param activity
     * @param destination
     */
    public static void startDefaultNavigationApplication(final int defaultNavigation, final Activity activity, final Geopoint destination) {
        if (destination == null) {
            ActivityMixin.showToast(activity, CgeoApplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }

        navigateGeopoint(activity, destination, getDefaultNavigationApplication(defaultNavigation));
    }

    /**
     * Returns the default navigation tool if correctly set and installed or the compass app as default fallback
     *
     * @return never <code>null</code>
     */
    public static App getDefaultNavigationApplication() {
        return getDefaultNavigationApplication(1);
    }

    private static App getNavigationAppForId(final int navigationAppId) {
        final List<NavigationAppsEnum> installedNavigationApps = getInstalledNavigationApps();

        for (final NavigationAppsEnum navigationApp : installedNavigationApps) {
            if (navigationApp.id == navigationAppId) {
                return navigationApp.app;
            }
        }
        // default navigation tool wasn't set already or couldn't be found (not installed any more for example)
        return NavigationAppsEnum.COMPASS.app;
    }

    private static void invokeNavigation(final Activity activity, final Geocache cache, final Waypoint waypoint, final Geopoint destination, final App app) {
        if (cache != null) {
            navigateCache(activity, cache, app);
        }
        else if (waypoint != null) {
            navigateWaypoint(activity, waypoint, app);
        }
        else {
            navigateGeopoint(activity, destination, app);
        }
    }

}
