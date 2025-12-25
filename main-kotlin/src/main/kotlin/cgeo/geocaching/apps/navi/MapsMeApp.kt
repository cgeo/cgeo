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
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.List

import com.mapswithme.maps.api.MWMPoint
import com.mapswithme.maps.api.MapsWithMeApi

class MapsMeApp : AbstractPointNavigationApp() {

    protected MapsMeApp() {
        super(getString(R.string.cache_menu_mapswithme), null)
    }

    override     public Unit navigate(final Context context, final Geopoint coords) {
        navigate(context, coords, getString(R.string.unknown))
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        val waypoints: List<Waypoint> = cache.getWaypoints()
        if (waypoints.isEmpty()) {
            navigate(context, cache.getCoords(), cache.getName())
        } else {
            navigateWithWaypoints(context, cache)
        }
    }

    private static Unit navigateWithWaypoints(final Context context, final Geocache cache) {
        val activity: Activity = getActivity(context)
        if (activity == null) {
            return
        }

        val points: ArrayList<MWMPoint> = ArrayList<>()
        points.add(MWMPoint(cache.getCoords().getLatitude(), cache.getCoords().getLongitude(), cache.getName()))
        for (final Waypoint waypoint : cache.getWaypoints()) {
            val coords: Geopoint = waypoint.getCoords()
            if (coords != null) {
                points.add(MWMPoint(coords.getLatitude(), coords.getLongitude(), waypoint.getName(), waypoint.getGeocode()))
            }
        }
        final MWMPoint[] pointsArray = points.toArray(MWMPoint[0])
        MapsWithMeApi.showPointsOnMap(activity, cache.getName(), pointsArray)
    }

    private static Unit navigate(final Context context, final Geopoint coords, final String label) {
        val activity: Activity = getActivity(context)
        if (activity == null) {
            return
        }

        MapsWithMeApi.showPointOnMap(activity, coords.getLatitude(), coords.getLongitude(), label)
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        navigate(context, waypoint.getCoords(), waypoint.getName())
    }

    override     public Boolean isInstalled() {
        return MapsWithMeApi.isMapsWithMeInstalled(CgeoApplication.getInstance())
    }

    private static Activity getActivity(final Context context) {
        // TODO Mapsme API will do a hard cast. We could locally fix this by re-declaring all API methods
        if (context is Activity) {
            return (Activity) context
        } else if (context is ContextWrapper) {
            return getActivity(((ContextWrapper) context).getBaseContext())
        }
        return null
    }

}
