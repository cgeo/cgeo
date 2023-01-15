package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.location.GeoObjectStyle;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapLineUtils;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class IndividualRoute extends Route implements Parcelable {

    private enum ToggleItemState {
        ADDED,
        REMOVED,
        ERROR_NO_POINT
    }

    private boolean loadingRoute = false;
    private SetTarget setTarget = null;

    public IndividualRoute(@Nullable final SetTarget setTarget) {
        super(true);
        this.setTarget = setTarget;
    }

    public interface UpdateIndividualRoute {
        void updateIndividualRoute(Route route);
    }

    public interface SetTarget {
        void setTarget(@Nullable Geopoint geopoint, String geocode);
    }

    public void toggleItem(final Context context, final RouteItem item, final UpdateIndividualRoute routeUpdater) {
        toggleItem(context, item, routeUpdater, false);
    }

    public void toggleItem(final Context context, final RouteItem item, final UpdateIndividualRoute routeUpdater, final boolean addToRouteStart) {
        if (loadingRoute) {
            Log.d("[RouteTrackDebug] Individual route: Cannot toggle item, route still loading");
            return;
        }

        if (item.getType() == RouteItem.RouteItemType.WAYPOINT && item.getWaypointId() == -1) {
            Toast.makeText(context, R.string.individual_route_error_single_waypoint_mode, Toast.LENGTH_SHORT).show();
            return;
        }

        final ToggleItemState result = toggleItemInternal(item, addToRouteStart);
        if (result == ToggleItemState.REMOVED) {
            Log.d("[RouteTrackDebug] Individual route: Removed first element from route (" + item.getIdentifier() + ")");
        }
        Toast.makeText(context, result == ToggleItemState.ADDED ? R.string.individual_route_added : result == ToggleItemState.REMOVED ? R.string.individual_route_removed : R.string.individual_route_error_toggling_waypoint, Toast.LENGTH_SHORT).show();
        updateRoute(routeUpdater);
        saveRoute();
    }

    public void reloadRoute(final UpdateIndividualRoute updateRoute) {
        clearRouteInternal(updateRoute, false);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), this::loadRouteInternal, () -> updateRoute(updateRoute));
    }

    public void updateRoute(final UpdateIndividualRoute routeUpdater) {
        if (loadingRoute) {
            return;
        }
        if (null != routeUpdater) {
            routeUpdater.updateIndividualRoute(this);
            triggerTargetUpdate(false);
        }
    }

    public void clearRoute(final UpdateIndividualRoute routeUpdater) {
        clearRouteInternal(routeUpdater, true);
    }

    public void triggerTargetUpdate(final boolean resetTarget) {
        if (setTarget == null) {
            Log.d("[RouteTrackDebug] Individual route: Cannot set target, setTarget is null");
            return;
        }
        if (resetTarget) {
            Log.d("[RouteTrackDebug] Individual route: Reset target to null");
            setTarget.setTarget(null, "");
        } else if (Settings.isAutotargetIndividualRoute()) {
            if (getNumSegments() == 0) {
                Log.d("[RouteTrackDebug] Individual route: Reset target to null");
                setTarget.setTarget(null, "");
            } else {
                final RouteItem firstItem = segments.get(0).getItem();
                Log.d("[RouteTrackDebug] Individual route: Reset target to " + firstItem.getIdentifier());
                setTarget.setTarget(firstItem.getPoint(), firstItem.getGeocode());
            }
        }
    }

    private synchronized void loadRouteInternal() {
        loadingRoute = true;
        Log.d("[RouteTrackDebug] Individual route: Start loading from database");
        final ArrayList<RouteItem> routeItems = DataStore.loadIndividualRoute();
        for (int i = 0; i < routeItems.size(); i++) {
            Log.d("[RouteTrackDebug] Individual route: Add item #" + i + " (" + routeItems.get(i).getIdentifier() + ")");
            toggleItemInternal(routeItems.get(i), false);
        }
        Log.d("[RouteTrackDebug] Individual route: Finished loading from database");
        loadingRoute = false;
    }

    private synchronized void saveRoute() {
        if (segments != null) {
            Schedulers.io().scheduleDirect(() -> DataStore.saveIndividualRoute(this));
        }
    }

    private void clearRouteInternal(final UpdateIndividualRoute routeUpdater, final boolean deleteInDatabase) {
        distance = 0.0f;
        if (deleteInDatabase) {
            Schedulers.io().scheduleDirect(DataStore::clearIndividualRoute);
        }
        segments = null;
        if (null != routeUpdater) {
            routeUpdater.updateIndividualRoute(this);
        }
    }

    /**
     * @param item item to be added or removed
     * @return ToggleItemState
     */
    private ToggleItemState toggleItemInternal(final RouteItem item, final boolean addToRouteStart) {
        if (segments == null) {
            segments = new ArrayList<>();
        }
        final int pos = pos(item);
        if (pos == -1) {
            final RouteSegment segment = new RouteSegment(item, null, true);
            if (segment.hasPoint()) {
                if (addToRouteStart) {
                    segments.add(0, segment);
                } else {
                    segments.add(segment);
                }
                calculateNavigationRoute(segments.size() - 1);
                return ToggleItemState.ADDED;
            } else {
                return ToggleItemState.ERROR_NO_POINT;
            }
        } else {
            distance -= segments.get(pos).getDistance();
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

    @Override
    protected GeoObjectStyle getLineStyle() {
        return new GeoObjectStyle(MapLineUtils.getRouteColor(), MapLineUtils.getRawRouteLineWidth(), -1);
    }

    // Parcelable methods

    public static final Creator<IndividualRoute> CREATOR = new Creator<IndividualRoute>() {

        @Override
        public IndividualRoute createFromParcel(final Parcel source) {
            return new IndividualRoute(source);
        }

        @Override
        public IndividualRoute[] newArray(final int size) {
            return new IndividualRoute[size];
        }

    };

    protected IndividualRoute(final Parcel parcel) {
        super(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
    }
}
