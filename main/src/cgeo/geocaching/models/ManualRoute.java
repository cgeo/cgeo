package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class ManualRoute extends Route {

    private enum ToggleItemState {
        ADDED,
        REMOVED,
        ERROR_NO_POINT
    }

    private boolean loadingRoute = false;

    public ManualRoute() {
        super(true);
    }

    public interface UpdateManualRoute {
        void updateManualRoute(Route route);
    }

    public void toggleItem(final Context context, final RouteItem item, final UpdateManualRoute routeUpdater) {
        if (loadingRoute) {
            return;
        }

        if (item.getType() == RouteItem.RouteItemType.WAYPOINT && item.getWaypointId() == -1) {
            Toast.makeText(context, R.string.individual_route_error_single_waypoint_mode, Toast.LENGTH_SHORT).show();
            return;
        }

        final ToggleItemState result = toggleItemInternal(item);
        Toast.makeText(context, result == ToggleItemState.ADDED ? R.string.individual_route_added : result == ToggleItemState.REMOVED ? R.string.individual_route_removed : R.string.individual_route_error_toggling_waypoint, Toast.LENGTH_SHORT).show();
        updateRoute(routeUpdater);
        saveRoute();
    }

    public void reloadRoute(final UpdateManualRoute updateRoute) {
        clearRouteInternal(updateRoute, false);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), this::loadRouteInternal, () -> updateRoute(updateRoute));
    }

    public void updateRoute(final UpdateManualRoute routeUpdater) {
        if (loadingRoute) {
            return;
        }
        if (null != routeUpdater) {
            routeUpdater.updateManualRoute(this);
        }
    }

    public void clearRoute(final UpdateManualRoute routeUpdater) {
        clearRouteInternal(routeUpdater, true);
    }

    private synchronized void loadRouteInternal() {
        loadingRoute = true;
        final ArrayList<RouteItem> routeItems = DataStore.loadIndividualRoute();
        for (int i = 0; i < routeItems.size(); i++) {
            toggleItemInternal(routeItems.get(i));
        }
        loadingRoute = false;
    }

    private synchronized void saveRoute() {
        if (segments != null) {
            Schedulers.io().scheduleDirect(() -> DataStore.saveIndividualRoute(this));
        }
    }
    private void clearRouteInternal(final UpdateManualRoute routeUpdater, final boolean deleteInDatabase) {
        if (deleteInDatabase) {
            Schedulers.io().scheduleDirect(DataStore::clearIndividualRoute);
        }
        segments = null;
        if (null != routeUpdater) {
            routeUpdater.updateManualRoute(this);
        }
    }

    /**
     * @param item item to be added or removed
     * @return ToggleItemState
     */
    private ToggleItemState toggleItemInternal(final RouteItem item) {
        if (segments == null) {
            segments = new ArrayList<>();
        }
        final int pos = pos(item);
        if (pos == -1) {
            final RouteSegment segment = new RouteSegment(item, null);
            if (segment.hasPoint()) {
                segments.add(segment);
                calculateNavigationRoute(segments.size() - 1);
                return ToggleItemState.ADDED;
            } else {
                return ToggleItemState.ERROR_NO_POINT;
            }
        } else {
            segments.remove(pos);
            calculateNavigationRoute(pos);
            if (pos < segments.size()) {
                calculateNavigationRoute(pos + 1);
            }
            return ToggleItemState.REMOVED;
        }
    }

    private int pos(final RouteItem item) {
        if (segments == null || segments.size() == 0) {
            return -1;
        }
        final String identifier = item.getIdentifier();
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getItem().getIdentifier().equals(identifier)) {
                return i;
            }
        }
        return -1;
    }

}
