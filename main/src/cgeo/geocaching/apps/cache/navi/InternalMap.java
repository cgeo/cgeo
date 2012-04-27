package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.CGeoMap;

import android.app.Activity;
import android.content.Context;

class InternalMap extends AbstractNavigationApp {

    InternalMap() {
        super(getString(R.string.cache_menu_map), null);
    }

    @Override
    public boolean invoke(IGeoData geo, Activity activity, cgCache cache,
            cgWaypoint waypoint, final Geopoint coords) {
        if (cache != null) {
            CGeoMap.startActivityGeoCode(activity, cache.getGeocode());
            // may need some code from CGeoMap.startActivitySearch(activity, search, cache != null ? cache.getGeocode() : null, true);
        }
        else if (waypoint != null) {
            CGeoMap.startActivityCoords(activity, waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName());
        }
        else if (coords != null) {
            CGeoMap.startActivityCoords(activity, coords, WaypointType.WAYPOINT, null);
        }

        return true;
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

}
