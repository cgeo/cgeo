package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CGeoMap;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

class InternalMap extends AbstractPointNavigationApp {

    InternalMap() {
        super(getString(R.string.cache_menu_map), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geopoint coords) {
        CGeoMap.startActivityCoords(activity, coords, WaypointType.WAYPOINT, null);
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Waypoint waypoint) {
        CGeoMap.startActivityCoords(activity, waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName());
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geocache cache) {
        CGeoMap.startActivityGeoCode(activity, cache.getGeocode());
    }

}
