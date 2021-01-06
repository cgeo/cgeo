package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.export.IndividualRouteExport;
import cgeo.geocaching.files.GPXIndividualRouteImporter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.RouteSortActivity;
import cgeo.geocaching.models.ManualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ConfigurableFolder;
import cgeo.geocaching.storage.ConfigurableFolderStorageActivityHelper;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action2;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

public class IndividualRouteUtils {

    private static final int REQUEST_SORT_INDIVIDUAL_ROUTE = 4712;

    private final Activity activity;

    private final ConfigurableFolderStorageActivityHelper fileSelector;

    public IndividualRouteUtils(final Activity activity) {
        this.activity = activity;
        this.fileSelector = new ConfigurableFolderStorageActivityHelper(activity);
    }

    /**
     * Enable/disable track related menu entries
     *
     * @param menu menu to be configured
     */
    public void onPrepareOptionsMenu(final Menu menu, final ManualRoute route, final boolean targetIsSet) {
        final boolean isVisible = route != null && route.getNumSegments() > 0;
        menu.findItem(R.id.menu_sort_individual_route).setVisible(isVisible);
        menu.findItem(R.id.menu_center_on_route).setVisible(isVisible);
        menu.findItem(R.id.menu_export_individual_route).setVisible(isVisible);
        menu.findItem(R.id.menu_clear_individual_route).setVisible(isVisible);
        menu.findItem(R.id.menu_clear_targets).setVisible(targetIsSet || Settings.isAutotargetIndividualRoute());
        menu.findItem(R.id.menu_autotarget_individual_route).setVisible(true).setChecked(Settings.isAutotargetIndividualRoute());
    }

    /**
     * Check if selected menu entry is regarding individual routes
     *
     * @param activity calling activity
     * @param id       menu entry id
     * @return true, if selected menu entry is individual route related and consumed / false else
     */
    public boolean onOptionsItemSelected(final int id, final ManualRoute route, final Runnable clearIndividualRoute, final Runnable reloadIndividualRoute, final Route.CenterOnPosition centerOnPosition, final Action2<Geopoint, String> setTarget) {
        if (id == R.id.menu_load_individual_route) {
            if (null == route || route.getNumSegments() == 0) {
                startIndividualRouteFileSelector(reloadIndividualRoute);
            } else {
                Dialogs.confirm(activity, R.string.map_load_individual_route, R.string.map_load_individual_route_confirm, (dialog, which) -> startIndividualRouteFileSelector(reloadIndividualRoute));
            }
        } else if (id == R.id.menu_sort_individual_route) {
            activity.startActivityForResult(new Intent(activity, RouteSortActivity.class), REQUEST_SORT_INDIVIDUAL_ROUTE);
        } else if (id == R.id.menu_center_on_route) {
            route.setCenter(centerOnPosition);
        } else if (id == R.id.menu_export_individual_route) {
            new IndividualRouteExport(activity, route);
        } else if (id == R.id.menu_clear_individual_route) {
            Dialogs.confirm(activity, R.string.map_clear_individual_route, R.string.map_clear_individual_route_confirm, (dialog, which) -> {
                clearIndividualRoute.run();
                ActivityMixin.invalidateOptionsMenu(activity);
            });
        } else if (id == R.id.menu_autotarget_individual_route) {
            setAutotargetIndividualRoute(activity, route, !Settings.isAutotargetIndividualRoute());
        } else if (id == R.id.menu_clear_targets) {
            if (setTarget != null) {
                setTarget.call(null, null);
            }
            Settings.setAutotargetIndividualRoute(false);
            route.triggerTargetUpdate(true);
            ActivityMixin.invalidateOptionsMenu(activity);
            ActivityMixin.showToast(activity, R.string.map_manual_targets_cleared);
        } else {
            return false;
        }
        return true;
    }

    private void startIndividualRouteFileSelector(final Runnable reloadIndividualRoute) {

        fileSelector.selectFile(null, ConfigurableFolder.GPX.getUri(), uri -> {
            if (uri != null) {
                GPXIndividualRouteImporter.doImport(activity, uri);
                reloadIndividualRoute.run();
            }
        });
    }

    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data, final Runnable reloadIndividualRoute) {
        if (fileSelector.onActivityResult(requestCode, resultCode, data)) {
            return true;
        }
        if (requestCode == REQUEST_SORT_INDIVIDUAL_ROUTE) {
            reloadIndividualRoute.run();
            return true;
        }
        return false;
    }

    public static void setAutotargetIndividualRoute(final Activity activity, final ManualRoute route, final boolean newValue) {
        Settings.setAutotargetIndividualRoute(newValue);
        route.triggerTargetUpdate(!Settings.isAutotargetIndividualRoute());
        ActivityMixin.invalidateOptionsMenu(activity);
    }

}
