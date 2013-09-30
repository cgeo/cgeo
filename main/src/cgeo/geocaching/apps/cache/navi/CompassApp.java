package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.ui.Formatter;

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
    public void navigate(Activity activity, Geopoint coords) {
        CompassActivity.startActivity(activity, getString(R.string.navigation_direct_navigation), getString(R.string.navigation_target), coords, null);
    }

    @Override
    public void navigate(Activity activity, Waypoint waypoint) {
        CompassActivity.startActivity(activity, waypoint.getPrefix() + "/" + waypoint.getLookup(), waypoint.getName(), waypoint.getCoords(), null,
                waypoint.getWaypointType().getL10n());
    }

    @Override
    public void navigate(Activity activity, Geocache cache) {
        CompassActivity.startActivity(activity, cache.getGeocode(), cache.getName(), cache.getCoords(), null,
                Formatter.formatCacheInfoShort(cache));
    }

}