package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeonavigate;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Context;

class CompassApp extends AbstractNavigationApp {

    CompassApp() {
        super(getString(R.string.compass_title), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    @Override
    public boolean invoke(IGeoData geo, Activity activity, cgCache cache,
            cgWaypoint waypoint, final Geopoint coords) {

        if (cache != null && cache.getGeocode() != null) {
            cgeonavigate.startActivity(activity, cache.getGeocode(), cache.getName(), cache.getCoords(), null);
            return true;
        }
        if (waypoint != null && waypoint.getCoords() != null) {
            cgeonavigate.startActivity(activity, waypoint.getPrefix() + "/" + waypoint.getLookup(), waypoint.getName(), waypoint.getCoords(), null);
            return true;
        }
        if (coords != null) {
            cgeonavigate.startActivity(activity, getString(R.string.navigation_direct_navigation), getString(R.string.navigation_target), coords, null);
            return true;
        }
        // search is not handled here
        return false;
    }

}