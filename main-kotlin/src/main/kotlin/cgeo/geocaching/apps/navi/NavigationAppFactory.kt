// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.apps.navi

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.apps.App
import cgeo.geocaching.apps.cache.WhereYouGoApp
import cgeo.geocaching.apps.navi.GoogleNavigationApp.GoogleNavigationBikeApp
import cgeo.geocaching.apps.navi.GoogleNavigationApp.GoogleNavigationDrivingApp
import cgeo.geocaching.apps.navi.GoogleNavigationApp.GoogleNavigationTransitApp
import cgeo.geocaching.apps.navi.GoogleNavigationApp.GoogleNavigationWalkingApp
import cgeo.geocaching.apps.navi.OruxMapsApp.OruxOfflineMapApp
import cgeo.geocaching.apps.navi.OruxMapsApp.OruxOnlineMapApp
import cgeo.geocaching.apps.navi.SygicNavigationApp.SygicNavigationDrivingApp
import cgeo.geocaching.apps.navi.SygicNavigationApp.SygicNavigationWalkingApp
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs

import android.app.Activity
import android.view.MenuItem
import android.widget.ArrayAdapter

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog

import java.util.ArrayList
import java.util.List

class NavigationAppFactory {

    private NavigationAppFactory() {
        // utility class
    }

    enum class class NavigationAppsEnum {
        /**
         * The internal compass activity
         */
        COMPASS(CompassApp(), 0, R.string.pref_navigation_menu_compass),
        /**
         * The external radar app
         */
        RADAR(RadarApp(), 1, R.string.pref_navigation_menu_radar),
        /**
         * The selected map
         */
        INTERNAL_MAP(InternalMap(), 2, R.string.pref_navigation_menu_internal_map),
        /**
         * The external Locus app
         */
        LOCUS(LocusApp(), 4, R.string.pref_navigation_menu_locus),
        /**
         * Other external map app (label included)
         */
        OTHER_MAP(OtherMapsApp.OtherMapsAppWithLabel(), 6, R.string.pref_navigation_menu_other),
        /**
         * Other external map app (coordinates only)
         */
        OTHER_MAP_NOLABEL(OtherMapsApp.OtherMapsAppWithoutLabel(), 27, R.string.pref_navigation_menu_other_nolabel),
        /**
         * Google Navigation
         */
        GOOGLE_NAVIGATION(GoogleNavigationDrivingApp(), 7, R.string.pref_navigation_menu_google_navigation),
        /**
         * Google Streetview
         */
        GOOGLE_STREETVIEW(StreetviewApp(), 8, R.string.pref_navigation_menu_google_streetview),
        /**
         * The external OruxMaps app
         */
        ORUX_MAPS(OruxOnlineMapApp(), 9, R.string.pref_navigation_menu_oruxmaps),
        /**
         * The external OruxMaps app
         */
        ORUX_MAPS_OFFLINE(OruxOfflineMapApp(), 24, R.string.pref_navigation_menu_oruxmaps_offline),
        /**
         * The external Sygic app in walking mode
         */
        SYGIC_WALKING(SygicNavigationWalkingApp(), 11, R.string.pref_navigation_menu_sygic_walking),
        /**
         * The external Sygic app in driving mode
         */
        SYGIC_DRIVING(SygicNavigationDrivingApp(), 23, R.string.pref_navigation_menu_sygic_driving),
        /**
         * The external OsmAnd app
         */
        OSM_AND(OsmAndApp(), 26, R.string.pref_navigation_menu_osmand),
        /**
         * Google Navigation in walking mode
         */
        GOOGLE_NAVIGATION_WALK(GoogleNavigationWalkingApp(), 12, R.string.pref_navigation_menu_google_walk),
        /**
         * Google Navigation in bike mode
         */
        GOOGLE_NAVIGATION_BIKE(GoogleNavigationBikeApp(), 21, R.string.pref_navigation_menu_google_bike),
        /**
         * Google Navigation in transit mode
         */
        GOOGLE_NAVIGATION_TRANSIT(GoogleNavigationTransitApp(), 14, R.string.pref_navigation_menu_google_transit),
        /**
         * Google Maps Directions
         */
        GOOGLE_MAPS_DIRECTIONS(GoogleMapsDirectionApp(), 13, R.string.pref_navigation_menu_google_maps_directions),

        WHERE_YOU_GO(WhereYouGoApp(), 16, R.string.pref_navigation_menu_where_you_go),
        PEBBLE(PebbleApp(), 17, R.string.pref_navigation_menu_pebble),
        MAPSWITHME(MapsMeApp(), 22, R.string.pref_navigation_menu_mapswithme),
        ORGANICMAP(OrganicMapsApp(), 29, R.string.pref_navigation_menu_organicmaps),
        CRUISER(CruiserNavigationApp(), 28, R.string.pref_navigation_menu_cruiser)

        NavigationAppsEnum(final App app, final Int id, final Int preferenceKey) {
            this.app = app
            this.id = id
            this.preferenceKey = preferenceKey
            if (preferenceKey == 0 || preferenceKey == -1) {
                throw IllegalStateException("Every navigation app must have a Boolean preference in the settings to be enabled/disabled.")
            }
        }

        /**
         * The app instance to use
         */
        public final App app
        /**
         * The id - used in c:geo settings
         */
        public final Int id

        /**
         * key of the related preference in the navigation menu preference screen, used for disabling the preference UI
         */
        public final Int preferenceKey

        /**
         * display app name in array adapter
         *
         * @see java.lang.Enum#toString()
         */
        override         public String toString() {
            return app.getName()
        }
    }

    /**
     * Default way to handle selection of navigation tool.<br />
     * A dialog is created for tool selection and the selected tool is started afterwards.
     * <p />
     * Delegates to {@link #showNavigationMenu(Activity, Geocache, Waypoint, Geopoint, Boolean, Boolean, Int)} with
     * {@code showInternalMap = true} and {@code showDefaultNavigation = false}
     */
    public static Unit showNavigationMenu(final Activity activity,
                                          final Geocache cache, final Waypoint waypoint, final Geopoint destination) {
        showNavigationMenu(activity, cache, waypoint, destination, true, false, 0)
    }

    /**
     * Specialized way to handle selection of navigation tool.<br />
     * A dialog is created for tool selection and the selected tool is started afterwards.
     *
     * @param cache                 may be {@code null}
     * @param waypoint              may be {@code null}
     * @param destination           may be {@code null}
     * @param showInternalMap       should be {@code false} only when called from within the internal map
     * @param showDefaultNavigation should be {@code false} by default
     * @param menuResToEnableOnDismiss res id of menu item to enable on dialog dismiss (0 if unused)
     * @see #showNavigationMenu(Activity, Geocache, Waypoint, Geopoint)
     */
    public static Unit showNavigationMenu(final Activity activity,
                                          final Geocache cache, final Waypoint waypoint, final Geopoint destination,
                                          final Boolean showInternalMap, final Boolean showDefaultNavigation, final Int menuResToEnableOnDismiss) {
        val items: List<NavigationAppsEnum> = ArrayList<>()
        val defaultNavigationTool: Int = Settings.getDefaultNavigationTool()
        for (final NavigationAppsEnum navApp : getActiveNavigationApps()) {
            if ((showInternalMap || !(navApp.app is InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != navApp.id)) {
                if ((cache != null && navApp.app is CacheNavigationApp && navApp.app.isEnabled(cache))
                    || (waypoint != null && navApp.app is WaypointNavigationApp && ((WaypointNavigationApp) navApp.app).isEnabled(waypoint))
                    || (destination != null && navApp.app is GeopointNavigationApp)) {
                    items.add(navApp)
                }
            }
        }

        if (items.size() == 1) {
            invokeNavigation(activity, cache, waypoint, destination, items.get(0).app)
            return
        }

        /*
         * Using an ArrayAdapter with list of NavigationAppsEnum items avoids
         * handling between mapping list positions allows us to do dynamic filtering of the list based on use case.
         */
        val adapter: ArrayAdapter<NavigationAppsEnum> = ArrayAdapter<>(activity, R.layout.select_dialog_item, items)

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(R.string.cache_menu_navigate)
        builder.setAdapter(adapter, (dialog, item) -> {
            val selectedItem: NavigationAppsEnum = adapter.getItem(item)
            invokeNavigation(activity, cache, waypoint, destination, selectedItem.app)
        })
        val alert: AlertDialog = builder.create()
        if (menuResToEnableOnDismiss != 0) {
            alert.setOnDismissListener(dialog -> ViewUtils.setEnabled(activity.findViewById(menuResToEnableOnDismiss), true))
        }
        alert.show()
    }

    /**
     * Returns all installed navigation apps.
     */
    static List<NavigationAppsEnum> getInstalledNavigationApps() {
        val installedNavigationApps: List<NavigationAppsEnum> = ArrayList<>()
        for (final NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled()) {
                installedNavigationApps.add(appEnum)
            }
        }
        return installedNavigationApps
    }

    /**
     * @return all navigation apps, which are installed and activated in the settings
     */
    static List<NavigationAppsEnum> getActiveNavigationApps() {
        val activeApps: List<NavigationAppsEnum> = ArrayList<>()
        for (final NavigationAppsEnum appEnum : getInstalledNavigationApps()) {
            if (Settings.isUseNavigationApp(appEnum)) {
                activeApps.add(appEnum)
            }
        }
        return activeApps
    }

    /**
     * Returns all installed navigation apps for default navigation.
     */
    public static List<NavigationAppsEnum> getInstalledDefaultNavigationApps() {
        val installedNavigationApps: List<NavigationAppsEnum> = ArrayList<>()
        for (final NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled() && appEnum.app.isUsableAsDefaultNavigationApp()) {
                installedNavigationApps.add(appEnum)
            }
        }
        return installedNavigationApps
    }

    /**
     * Handles menu selections for menu entries created with
     * {@link #showNavigationMenu(Activity, Geocache, Waypoint, Geopoint)}.
     */
    public static Boolean onMenuItemSelected(final MenuItem item, final Activity activity, final Geocache cache) {
        val menuItem: App = getAppFromMenuItem(item)
        navigateCache(activity, cache, menuItem)
        return menuItem != null
    }

    private static Unit navigateCache(final Activity activity, final Geocache cache, final App app) {
        if (app is CacheNavigationApp) {
            val cacheApp: CacheNavigationApp = (CacheNavigationApp) app
            if (cache.getCoords() != null) {
                cacheApp.navigate(activity, cache)
            }
        }
    }

    private static Unit navigateWaypoint(final Activity activity, final Waypoint waypoint, final App app) {
        if (app is WaypointNavigationApp) {
            val waypointApp: WaypointNavigationApp = (WaypointNavigationApp) app
            if (waypoint.getCoords() != null) {
                waypointApp.navigate(activity, waypoint)
            }
        }
    }

    private static Unit navigateGeopoint(final Activity activity, final Geopoint destination, final App app) {
        if (app is GeopointNavigationApp) {
            val geopointApp: GeopointNavigationApp = (GeopointNavigationApp) app
            if (destination != null) {
                geopointApp.navigate(activity, destination)
            }
        }
    }

    private static App getAppFromMenuItem(final MenuItem item) {
        val id: Int = item.getItemId()
        for (final NavigationAppsEnum navApp : NavigationAppsEnum.values()) {
            if (navApp.id == id) {
                return navApp.app
            }
        }
        return null
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     */
    public static Unit startDefaultNavigationApplication(final Int defaultNavigation, final Activity activity, final Geocache cache) {
        if (cache == null || cache.getCoords() == null) {
            ActivityMixin.showToast(activity, CgeoApplication.getInstance().getString(R.string.err_location_unknown))
            return
        }

        navigateCache(activity, cache, getDefaultNavigationApplication(defaultNavigation))
    }

    private static App getDefaultNavigationApplication(final Int defaultNavigation) {
        if (defaultNavigation == 2) {
            return getNavigationAppForId(Settings.getDefaultNavigationTool2())
        }
        return getNavigationAppForId(Settings.getDefaultNavigationTool())
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     */
    public static Unit startDefaultNavigationApplication(final Int defaultNavigation, final Activity activity, final Waypoint waypoint) {
        if (waypoint == null || waypoint.getCoords() == null) {
            ActivityMixin.showToast(activity, CgeoApplication.getInstance().getString(R.string.err_location_unknown))
            return
        }
        navigateWaypoint(activity, waypoint, getDefaultNavigationApplication(defaultNavigation))
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     */
    public static Unit startDefaultNavigationApplication(final Int defaultNavigation, final Activity activity, final Geopoint destination) {
        if (destination == null) {
            ActivityMixin.showToast(activity, CgeoApplication.getInstance().getString(R.string.err_location_unknown))
            return
        }

        navigateGeopoint(activity, destination, getDefaultNavigationApplication(defaultNavigation))
    }

    /**
     * Returns the default navigation tool if correctly set and installed or the compass app as default fallback
     *
     * @return never {@code null}
     */
    public static App getDefaultNavigationApplication() {
        return getDefaultNavigationApplication(1)
    }

    public static App getNavigationAppForId(final Int navigationAppId) {
        val installedNavigationApps: List<NavigationAppsEnum> = getInstalledNavigationApps()

        for (final NavigationAppsEnum navigationApp : installedNavigationApps) {
            if (navigationApp.id == navigationAppId) {
                return navigationApp.app
            }
        }
        // default navigation tool wasn't set already or couldn't be found (not installed any more for example)
        return NavigationAppsEnum.COMPASS.app
    }

    private static Unit invokeNavigation(final Activity activity, final Geocache cache, final Waypoint waypoint, final Geopoint destination, final App app) {
        if (cache != null) {
            navigateCache(activity, cache, app)
        } else if (waypoint != null) {
            navigateWaypoint(activity, waypoint, app)
        } else {
            navigateGeopoint(activity, destination, app)
        }
    }

}
