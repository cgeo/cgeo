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

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu

import androidx.annotation.NonNull
import androidx.core.view.ActionProvider
import androidx.core.view.MenuItemCompat

/**
 * Action provider that lists all waypoints of a cache. Can be used to select a certain waypoint out of multiple ones in
 * navigation related activities.
 */
class WaypointSelectionActionProvider : AbstractMenuActionProvider() {

    private Callback callback
    private Geocache geocache

    interface Callback {
        Unit onWaypointSelected(Waypoint waypoint)

        Unit onGeocacheSelected(Geocache geocache)
    }

    /**
     * Creates a instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public WaypointSelectionActionProvider(final Context context) {
        super(context)
    }

    public Unit setCallback(final Callback callback) {
        this.callback = callback
    }

    override     public Unit onPrepareSubMenu(final SubMenu subMenu) {
        if (callback == null) {
            return
        }
        addWaypoints(subMenu)
    }

    private Unit addWaypoints(final SubMenu subMenu) {
        subMenu.clear()
        for (final Waypoint waypoint : geocache.getWaypoints()) {
            if (waypoint.getCoords() != null) {
                subMenu.add(Menu.NONE, waypoint.getId(), Menu.NONE, waypoint.getName()).setOnMenuItemClickListener(item -> {
                    callback.onWaypointSelected(waypoint)
                    return true
                })
            }
        }
        subMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, getContext().getString(R.string.cache)).setOnMenuItemClickListener(item -> {
            callback.onGeocacheSelected(geocache)
            return true
        })
    }

    public static Unit initialize(final MenuItem menuItem, final Geocache cache, final Callback callback) {
        val actionProvider: ActionProvider = MenuItemCompat.getActionProvider(menuItem)
        if (actionProvider is WaypointSelectionActionProvider) {
            val waypointsAction: WaypointSelectionActionProvider = (WaypointSelectionActionProvider) actionProvider
            waypointsAction.setCallback(callback)
            waypointsAction.setCache(cache)
            menuItem.setVisible(hasTargets(cache))
        }
    }

    private static Boolean hasTargets(final Geocache cache) {
        for (final Waypoint waypoint : cache.getWaypoints()) {
            if (waypoint.getCoords() != null) {
                return true
            }
        }
        return false
    }

    private Unit setCache(final Geocache cache) {
        geocache = cache
    }

}
