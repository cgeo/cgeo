package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeonavigate;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;

class CompassApp extends AbstractNavigationApp {

    CompassApp(final Resources res) {
        super(res.getString(R.string.compass_title), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final cgSearch search, cgWaypoint waypoint, final Geopoint coords) {

        if (cache.getGeocode() != null) {
            cgeonavigate.startActivity(activity, cache.getGeocode(), cache.getName(), coords, null);
            return true;
        }
        if (waypoint != null && waypoint.getCoords() != null) {
            cgeonavigate.startActivity(activity, waypoint.getPrefix().trim() + "/" + waypoint.getLookup().trim(), waypoint.getName(), waypoint.getCoords(), null);
            return true;
        }
        if (coords != null) {
            cgeonavigate.startActivity(activity, res.getString(R.string.navigation_direct_navigation), res.getString(R.string.navigation_target), coords, null);
            return true;
        }
        return false;
    }

}
