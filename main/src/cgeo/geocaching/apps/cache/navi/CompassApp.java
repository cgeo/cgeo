package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
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
    public boolean invoke(cgGeo geo, Activity activity, cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint coords) {

        if (cache != null && cache.getGeocode() != null) {
            cgeonavigate.startActivity(activity, cache.getGeocode(), cache.getName(), cache.getCoords(), null);
            return true;
        }
        if (waypoint != null && waypoint.getCoords() != null) {
            cgeonavigate.startActivity(activity, waypoint.getPrefix().trim() + "/" + waypoint.getLookup().trim(), waypoint.getName(), waypoint.getCoords(), null);
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