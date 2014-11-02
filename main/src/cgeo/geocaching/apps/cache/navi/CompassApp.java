package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.location.Geopoint;

import android.app.Activity;

class CompassApp extends AbstractPointNavigationApp {

    CompassApp() {
        super(getString(R.string.compass_title), R.id.cache_app_compass, null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(final Activity activity, final Geopoint coords) {
        CompassActivity.startActivityPoint(activity, coords, getString(R.string.navigation_direct_navigation));
    }

    @Override
    public void navigate(final Activity activity, final Waypoint waypoint) {
        CompassActivity.startActivityWaypoint(activity, waypoint);
    }

    @Override
    public void navigate(final Activity activity, final Geocache cache) {
        CompassActivity.startActivityCache(activity, cache);
    }

}