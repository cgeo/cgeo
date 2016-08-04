package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.app.Activity;
import android.support.annotation.NonNull;

class InternalMapNew extends AbstractPointNavigationApp {

    InternalMapNew() {
        super(getString(R.string.cache_menu_mfbeta), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geopoint coords) {
        NewMap.startActivityCoords(activity, coords, WaypointType.WAYPOINT, null);
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Waypoint waypoint) {
        NewMap.startActivityCoords(activity, waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName());
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geocache cache) {
        NewMap.startActivityGeoCode(activity, cache.getGeocode());
    }

}
