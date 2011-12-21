package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.CGeoMap;

import android.app.Activity;
import android.content.res.Resources;

class InternalMap extends AbstractInternalMap {

    InternalMap(Resources res) {
        super(res.getString(R.string.cache_menu_map), null);
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final cgSearch search, cgWaypoint waypoint, final Geopoint coords) {
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

}
