package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.maps.routing.Route;
import cgeo.geocaching.maps.routing.RouteSortActivity;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

public class IndividualRouteUtils {

    private static final int REQUEST_SORT_INDIVIDUAL_ROUTE = 4712;

    private IndividualRouteUtils() {
        // utility class
    }

    /**
     * Enable/disable track related menu entries
     *
     * @param menu menu to be configured
     */
    public static void onPrepareOptionsMenu(final Menu menu, final Route route) {
        menu.findItem(R.id.menu_individual_route).setVisible(route != null && !route.isEmpty());
    }

    /**
     * Check if selected menu entry is regarding individual routes
     *
     * @param activity calling activity
     * @param id       menu entry id
     * @return true, if selected menu entry is individual route related and consumed / false else
     */
    public static boolean onOptionsItemSelected(final Activity activity, final int id, final Runnable clearIndividualRoute) {
        switch (id) {
            case R.id.menu_clear_individual_route:
                Dialogs.confirm(activity, R.string.map_clear_individual_route, R.string.map_clear_individual_route_confirm, (dialog, which) -> {
                    clearIndividualRoute.run();
                    ActivityMixin.invalidateOptionsMenu(activity);
                });
                return true;
            case R.id.menu_sort_individual_route:
                activity.startActivityForResult(new Intent(activity, RouteSortActivity.class), REQUEST_SORT_INDIVIDUAL_ROUTE);
                return true;
            default:
                return false;
        }
    }

    public static boolean onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data, final Runnable reloadIndividualRoute) {
        if (requestCode == REQUEST_SORT_INDIVIDUAL_ROUTE) {
            reloadIndividualRoute.run();
            return true;
        }
        return false;
    }

}
