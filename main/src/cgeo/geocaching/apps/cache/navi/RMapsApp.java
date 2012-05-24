package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;

import android.app.Activity;
import android.content.Intent;

import java.util.ArrayList;

class RMapsApp extends AbstractApp implements CacheNavigationApp, WaypointNavigationApp, GeopointNavigationApp {

    private static final String INTENT = "com.robert.maps.action.SHOW_POINTS";

    RMapsApp() {
        super(getString(R.string.cache_menu_rmaps), INTENT);
    }

    @Override
    public void navigate(Activity activity, cgWaypoint waypoint) {
        navigate(activity, waypoint.getCoords(), waypoint.getLookup(), waypoint.getName());
    }

    private static void navigate(Activity activity, Geopoint coords, String code, String name) {
        final ArrayList<String> locations = new ArrayList<String>();
        locations.add(coords.format(Format.LAT_LON_DECDEGREE_COMMA) + ";" + code + ";" + name);
        final Intent intent = new Intent(INTENT);
        intent.putStringArrayListExtra("locations", locations);
        activity.startActivity(intent);
    }

    @Override
    public boolean isEnabled(cgWaypoint waypoint) {
        return waypoint.getCoords() != null;
    }

    @Override
    public void navigate(Activity activity, cgCache cache) {
        navigate(activity, cache.getCoords(), cache.getGeocode(), cache.getName());
    }

    @Override
    public void navigate(Activity activity, Geopoint coords) {
        navigate(activity, coords, "", "");
    }
}
