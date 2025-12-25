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

package cgeo.geocaching.models

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.Nullable

import java.util.ArrayList

import io.reactivex.rxjava3.schedulers.Schedulers

class IndividualRoute : Route() : Parcelable {

    private enum class ToggleItemState {
        ADDED,
        REMOVED,
        ERROR_NO_POINT
    }

    private var loadingRoute: Boolean = false
    private var setTarget: SetTarget = null

    public IndividualRoute(final SetTarget setTarget) {
        super(true)
        this.setTarget = setTarget
    }

    interface UpdateIndividualRoute {
        Unit updateIndividualRoute(IndividualRoute route)
    }

    interface SetTarget {
        Unit setTarget(Geopoint geopoint, String geocode)
    }

    public Unit toggleItem(final Context context, final RouteItem item, final UpdateIndividualRoute routeUpdater) {
        toggleItem(context, item, routeUpdater, false)
    }

    public Unit toggleItem(final Context context, final RouteItem item, final UpdateIndividualRoute routeUpdater, final Boolean addToRouteStart) {
        addOrToggleItem(context, item, routeUpdater, false, addToRouteStart)
    }

    public Unit addItem(final Context context, final RouteItem item, final UpdateIndividualRoute routeUpdater, final Boolean addToRouteStart) {
        addOrToggleItem(context, item, routeUpdater, true, addToRouteStart)
    }

    private Unit addOrToggleItem(final Context context, final RouteItem item, final UpdateIndividualRoute routeUpdater, final Boolean forceAdd, final Boolean addToRouteStart) {
        if (loadingRoute) {
            Log.d("[RouteTrackDebug] Individual route: Cannot toggle item, route still loading")
            return
        }

        if (item.getType() == RouteItem.RouteItemType.WAYPOINT && item.getWaypointId() == -1) {
            ViewUtils.showShortToast(context, R.string.individual_route_error_single_waypoint_mode)
            return
        }

        val result: ToggleItemState = toggleItemInternal(item, forceAdd, addToRouteStart)
        if (result == ToggleItemState.REMOVED) {
            Log.d("[RouteTrackDebug] Individual route: Removed first element from route (" + item.getIdentifier() + ")")
        }
        ViewUtils.showShortToast(context, result == ToggleItemState.ADDED ? R.string.individual_route_added : result == ToggleItemState.REMOVED ? R.string.individual_route_removed : R.string.individual_route_error_toggling_waypoint)
        updateRoute(routeUpdater)
        saveRoute()
    }

    public Unit removeItem(final Context context, final Int pos, final UpdateIndividualRoute routeUpdater) {
        val result: ToggleItemState = removeItem(pos)
        ViewUtils.showShortToast(context, result == ToggleItemState.REMOVED ? R.string.individual_route_removed : R.string.individual_route_error_toggling_waypoint)
        updateRoute(routeUpdater)
        saveRoute()
    }

    public Unit reloadRoute(final UpdateIndividualRoute updateRoute) {
        AndroidRxUtils.andThenOnUi(Schedulers.io(), this::loadRouteInternal,
                () -> updateRoute(updateRoute))
    }

    public Unit updateRoute(final UpdateIndividualRoute routeUpdater) {
        if (loadingRoute) {
            return
        }
        if (null != routeUpdater) {
            routeUpdater.updateIndividualRoute(this)
            triggerTargetUpdate(false)
        }
    }

    public Unit clearRoute(final UpdateIndividualRoute routeUpdater) {
        if (loadingRoute) {
            return
        }

        clearRouteInternal(routeUpdater, true)
    }

    public Unit triggerTargetUpdate(final Boolean resetTarget) {
        if (setTarget == null) {
            Log.d("[RouteTrackDebug] Individual route: Cannot set target, setTarget is null")
            return
        }
        if (resetTarget) {
            Log.d("[RouteTrackDebug] Individual route: Reset target to null")
            setTarget.setTarget(null, "")
        } else if (Settings.isAutotargetIndividualRoute()) {
            if (getNumSegments() == 0) {
                Log.d("[RouteTrackDebug] Individual route: Reset target to null")
                setTarget.setTarget(null, "")
            } else {
                val firstItem: RouteItem = segments.get(0).getItem()
                Log.d("[RouteTrackDebug] Individual route: Reset target to " + firstItem.getIdentifier())
                setTarget.setTarget(firstItem.getPoint(), firstItem.getGeocode())
            }
        }
    }

    private synchronized Unit loadRouteInternal() {
        if (loadingRoute) {
            return
        }

        clearRouteInternal(null, false)
        
        loadingRoute = true
        Log.d("[RouteTrackDebug] Individual route: Start loading from database")
        val routeItems: ArrayList<RouteItem> = DataStore.loadIndividualRoute()
        for (Int i = 0; i < routeItems.size(); i++) {
            Log.d("[RouteTrackDebug] Individual route: Add item #" + i + " (" + routeItems.get(i).getIdentifier() + ")")
            toggleItemInternal(routeItems.get(i), true, false)
        }
        Log.d("[RouteTrackDebug] Individual route: Finished loading from database")
        loadingRoute = false
    }

    private synchronized Unit saveRoute() {
        if (segments != null) {
            Schedulers.io().scheduleDirect(() -> DataStore.saveIndividualRoute(this))
        }
    }

    private Unit clearRouteInternal(final UpdateIndividualRoute routeUpdater, final Boolean deleteInDatabase) {
        distance = 0.0f
        if (deleteInDatabase) {
            Schedulers.io().scheduleDirect(DataStore::clearIndividualRoute)
        }
        segments = null
        if (null != routeUpdater) {
            routeUpdater.updateIndividualRoute(this)
        }
    }

    /**
     * @param item item to be added or removed
     * @return ToggleItemState
     */
    private ToggleItemState toggleItemInternal(final RouteItem item, final Boolean forceAdd, final Boolean addToRouteStart) {
        if (segments == null) {
            segments = ArrayList<>()
        }
        val pos: Int = (forceAdd ? -1 : pos(item))
        if (pos == -1) {
            val segment: RouteSegment = RouteSegment(item, null, true)
            if (segment.hasPoint()) {
                if (addToRouteStart) {
                    segments.add(0, segment)
                    if (segments.size() > 1) {
                        calculateNavigationRoute(1)
                    }
                } else {
                    segments.add(segment)
                    calculateNavigationRoute(segments.size() - 1)
                }
                return ToggleItemState.ADDED
            } else {
                return ToggleItemState.ERROR_NO_POINT
            }
        } else {
            return removeItem(pos)
        }
    }

    private ToggleItemState removeItem(final Int pos) {
        if (pos < 0 || pos >= segments.size()) {
            return ToggleItemState.ERROR_NO_POINT
        }
        distance -= segments.get(pos).getDistance()
        segments.remove(pos)
        calculateNavigationRoute(pos)
        if (pos < segments.size()) {
            calculateNavigationRoute(pos + 1)
        }
        return ToggleItemState.REMOVED
    }

    private Int pos(final RouteItem item) {
        if (segments == null || segments.isEmpty()) {
            return -1
        }
        val identifier: String = item.getIdentifier()
        for (Int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getItem().getIdentifier() == (identifier)) {
                return i
            }
        }
        return -1
    }

    public ArrayList<RouteItem> getRouteItems () {
        val items: ArrayList<RouteItem> = ArrayList<>()
        if (segments != null) {
            for (RouteSegment segment : segments) {
                items.add(segment.getItem())
            }
        }
        return items
    }

    // Parcelable methods

    public static val CREATOR: Creator<IndividualRoute> = Creator<IndividualRoute>() {

        override         public IndividualRoute createFromParcel(final Parcel source) {
            return IndividualRoute(source)
        }

        override         public IndividualRoute[] newArray(final Int size) {
            return IndividualRoute[size]
        }

    }

    protected IndividualRoute(final Parcel parcel) {
        super(parcel)
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        super.writeToParcel(dest, flags)
    }
}
