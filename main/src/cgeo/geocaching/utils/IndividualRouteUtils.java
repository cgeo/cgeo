package cgeo.geocaching.utils;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectIndividualRouteFileActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.export.IndividualRouteExport;
import cgeo.geocaching.files.GPXIndividualRouteImporter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.routing.Route;
import cgeo.geocaching.maps.routing.RouteSegment;
import cgeo.geocaching.maps.routing.RouteSortActivity;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

import java.io.File;

public class IndividualRouteUtils {

    private static final int REQUEST_SORT_INDIVIDUAL_ROUTE = 4712;
    private static final int REQUEST_CODE_GET_INDIVIDUALROUTEFILE = 47122;

    private IndividualRouteUtils() {
        // utility class
    }

    /**
     * Enable/disable track related menu entries
     *
     * @param menu menu to be configured
     */
    public static void onPrepareOptionsMenu(final Menu menu, final Route route) {
        final boolean isVisible = route != null && !route.isEmpty();
        menu.findItem(R.id.menu_sort_individual_route).setVisible(isVisible);
        menu.findItem(R.id.menu_center_on_route).setVisible(isVisible);
        menu.findItem(R.id.menu_export_individual_route).setVisible(isVisible);
        menu.findItem(R.id.menu_clear_individual_route).setVisible(isVisible);
    }

    /**
     * Check if selected menu entry is regarding individual routes
     *
     * @param activity calling activity
     * @param id       menu entry id
     * @return true, if selected menu entry is individual route related and consumed / false else
     */
    public static boolean onOptionsItemSelected(final Activity activity, final int id, final Route route, final Runnable clearIndividualRoute, final MapUtils.CenterOnPosition centerOnPosition) {
        switch (id) {
            case R.id.menu_load_individual_route:
                if (null == route || route.isEmpty()) {
                    startIndividualRouteFileSelector(activity);
                } else {
                    Dialogs.confirm(activity, R.string.map_load_individual_route, R.string.map_load_individual_route_confirm, (dialog, which) -> startIndividualRouteFileSelector(activity));
                }
                return true;
            case R.id.menu_sort_individual_route:
                activity.startActivityForResult(new Intent(activity, RouteSortActivity.class), REQUEST_SORT_INDIVIDUAL_ROUTE);
                return true;
            case R.id.menu_center_on_route:
                if (null != route && !route.isEmpty()) {
                    final RouteSegment[] segments = route.getSegments();

                    // calculate center and boundaries of individual route
                    final Geopoint first = segments[0].getPoint();
                    double minLat = first.getLatitude();
                    double maxLat = first.getLatitude();
                    double minLon = first.getLongitude();
                    double maxLon = first.getLongitude();

                    double latitude = 0.0d;
                    double longitude = 0.0d;
                    for (RouteSegment segment : segments) {
                        final Geopoint point = segment.getPoint();
                        final double lat = point.getLatitude();
                        final double lon = point.getLongitude();

                        latitude += point.getLatitude();
                        longitude += point.getLongitude();

                        minLat = Math.min(minLat, lat);
                        maxLat = Math.max(maxLat, lat);
                        minLon = Math.min(minLon, lon);
                        maxLon = Math.max(maxLon, lon);
                    }
                    centerOnPosition.centerOnPosition(latitude / segments.length, longitude / segments.length, new Viewport(new Geopoint(minLat, minLon), new Geopoint(maxLat, maxLon)));
                }
                return true;
            case R.id.menu_export_individual_route:
                new IndividualRouteExport(activity, route);
                return true;
            case R.id.menu_clear_individual_route:
                Dialogs.confirm(activity, R.string.map_clear_individual_route, R.string.map_clear_individual_route_confirm, (dialog, which) -> {
                    clearIndividualRoute.run();
                    ActivityMixin.invalidateOptionsMenu(activity);
                });
                return true;
            default:
                return false;
        }
    }

    private static void startIndividualRouteFileSelector(final Activity activity) {
        final Intent intent = new Intent(activity, SelectIndividualRouteFileActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_GET_INDIVIDUALROUTEFILE);
    }

    public static boolean onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data, final Runnable reloadIndividualRoute) {
        if (requestCode == REQUEST_SORT_INDIVIDUAL_ROUTE) {
            reloadIndividualRoute.run();
            return true;
        } else if (requestCode == REQUEST_CODE_GET_INDIVIDUALROUTEFILE) {
            final String filename = data.getStringExtra(Intents.EXTRA_GPX_FILE);
            if (null != filename) {
                final File file = new File(filename);
                if (!file.isDirectory()) {
                    GPXIndividualRouteImporter.doImport(activity, file);
                    reloadIndividualRoute.run();
                }
            }
            return true;
        }
        return false;
    }

}
