package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.CGeoMap;

import android.app.Activity;
import android.content.res.Resources;

import java.util.UUID;

class InternalMap extends AbstractInternalMap implements
        NavigationApp {

    InternalMap(Resources res) {
        super(res.getString(R.string.cache_menu_map), null);
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final UUID searchId, cgWaypoint waypoint, final Geopoint coords) {
        if (searchId != null) {
            CGeoMap.startActivitySearch(activity, searchId, cache != null ? cache.geocode : null, true);
        }
        else if (cache != null) {
            CGeoMap.startActivityGeoCode(activity, cache.geocode);
        }
        else if (waypoint != null) {
            CGeoMap.startActivityCoords(activity, waypoint.coords, waypoint.type);
        }
        else if (coords != null) {
            CGeoMap.startActivityCoords(activity, coords, WaypointType.WAYPOINT);
        }

        return true;
    }

}
