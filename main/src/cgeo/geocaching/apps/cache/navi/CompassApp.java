package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.cgeonavigate;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.ui.Formatter;

import android.app.Activity;

class CompassApp extends AbstractApp implements CacheNavigationApp, WaypointNavigationApp, GeopointNavigationApp {

    CompassApp() {
        super(getString(R.string.compass_title), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(Activity activity, Geopoint coords) {
        cgeonavigate.startActivity(activity, getString(R.string.navigation_direct_navigation), getString(R.string.navigation_target), coords, null);
    }

    @Override
    public void navigate(Activity activity, Waypoint waypoint) {
        cgeonavigate.startActivity(activity, waypoint.getPrefix() + "/" + waypoint.getLookup(), waypoint.getName(), waypoint.getCoords(), null,
                waypoint.getWaypointType().getL10n());
    }

    @Override
    public boolean isEnabled(Waypoint waypoint) {
        return waypoint.getCoords() != null;
    }

    @Override
    public void navigate(Activity activity, Geocache cache) {
        cgeonavigate.startActivity(activity, cache.getGeocode(), cache.getName(), cache.getCoords(), null,
                Formatter.formatCacheInfoShort(cache));
    }

    @Override
    public boolean isEnabled(Geocache cache) {
        return cache.getGeocode() != null;
    }

}