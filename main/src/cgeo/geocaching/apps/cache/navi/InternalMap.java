package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
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
    public boolean invoke(cgGeo geo, Activity activity, cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint coords) {
        if (search != null) {
            CGeoMap.startActivitySearch(activity, search, cache != null ? cache.getGeocode() : null, true);
        }
        else if (cache != null) {
            CGeoMap.startActivityGeoCode(activity, cache.getGeocode());
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
