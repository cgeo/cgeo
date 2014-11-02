package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CGeoMap;

import android.app.Activity;

class InternalMap extends AbstractPointNavigationApp {

    InternalMap() {
        super(getString(R.string.cache_menu_map), R.id.cache_app_internal_map, null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(Activity activity, Geopoint coords) {
        CGeoMap.startActivityCoords(activity, coords, WaypointType.WAYPOINT, null);
    }

    @Override
    public void navigate(Activity activity, Waypoint waypoint) {
        CGeoMap.startActivityCoords(activity, waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName());
    }

    @Override
    public void navigate(Activity activity, Geocache cache) {
        CGeoMap.startActivityGeoCode(activity, cache.getGeocode());
    }

}
