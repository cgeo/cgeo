package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.NonNull;
import androidx.core.view.ActionProvider;
import androidx.core.view.MenuItemCompat;

/**
 * Action provider that lists all waypoints of a cache. Can be used to select a certain waypoint out of multiple ones in
 * navigation related activities.
 */
public class WaypointSelectionActionProvider extends AbstractMenuActionProvider {

    private Callback callback;
    private Geocache geocache;

    public interface Callback {
        void onWaypointSelected(Waypoint waypoint);

        void onGeocacheSelected(Geocache geocache);
    }

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
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
                subMenu.add(Menu.NONE, waypoint.getId(), Menu.NONE, waypoint.getName()).setOnMenuItemClickListener(item -> {
                    callback.onWaypointSelected(waypoint);
                    return true;
                });
            }
        }
        subMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, getContext().getString(R.string.cache)).setOnMenuItemClickListener(item -> {
            callback.onGeocacheSelected(geocache);
            return true;
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
