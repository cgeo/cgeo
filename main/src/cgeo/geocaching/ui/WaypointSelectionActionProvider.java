package cgeo.geocaching.ui;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;

/**
 * Action provider that lists all waypoints of a cache. Can be used to select a certain waypoint out of multiple ones in
 * navigation related activities.
 */
public class WaypointSelectionActionProvider extends AbstractMenuActionProvider {

    public static interface Callback {
        void onWaypointSelected(final Waypoint waypoint);

        void onGeocacheSelected(final Geocache geocache);
    }

    private Callback callback;
    private Geocache geocache;

    public WaypointSelectionActionProvider(final Context context) {
        super(context);
    }

    public void setCallback(final Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onPrepareSubMenu(final SubMenu subMenu) {
        if (callback == null) {
            return;
        }
        addWaypoints(subMenu);
    }

    private void addWaypoints(final SubMenu subMenu) {
        subMenu.clear();
        for (final Waypoint waypoint : geocache.getWaypoints()) {
            if (waypoint.getCoords() != null) {
                subMenu.add(Menu.NONE, waypoint.getId(), Menu.NONE, waypoint.getName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        callback.onWaypointSelected(waypoint);
                        return true;
                    }
                });
            }
        }
        subMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, getContext().getString(R.string.cache)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                callback.onGeocacheSelected(geocache);
                return true;
            }
        });
    }

    public static void initialize(@NonNull final MenuItem menuItem, @NonNull final Geocache cache, @NonNull final Callback callback) {
        final ActionProvider actionProvider = MenuItemCompat.getActionProvider(menuItem);
        if (actionProvider instanceof WaypointSelectionActionProvider) {
            final WaypointSelectionActionProvider waypointsAction = (WaypointSelectionActionProvider) actionProvider;
            waypointsAction.setCallback(callback);
            waypointsAction.setCache(cache);
            menuItem.setVisible(hasTargets(cache));
        }
    }

    private static boolean hasTargets(final Geocache cache) {
        for (final Waypoint waypoint : cache.getWaypoints()) {
            if (waypoint.getCoords() != null) {
                return true;
            }
        }
        return false;
    }

    private void setCache(final Geocache cache) {
        geocache = cache;
    }

}
