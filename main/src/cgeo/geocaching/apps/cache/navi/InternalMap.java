package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.CGeoMap;

import android.app.Activity;

class InternalMap extends AbstractApp implements CacheNavigationApp, WaypointNavigationApp, GeopointNavigationApp {

    InternalMap() {
        super(getString(R.string.cache_menu_map), null);
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
    public void navigate(Activity activity, cgWaypoint waypoint) {
        CGeoMap.startActivityCoords(activity, waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName());
    }

    @Override
    public boolean isEnabled(cgWaypoint waypoint) {
        return waypoint.getCoords() != null;
    }

    @Override
    public void navigate(Activity activity, cgCache cache) {
        CGeoMap.startActivityGeoCode(activity, cache.getGeocode());
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        return cache.getCoords() != null;
    }
}
