package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;

import android.app.Activity;

class StaticMapApp extends AbstractStaticMapsApp {

    StaticMapApp() {
        super(getString(R.string.cache_menu_map_static), R.id.cache_app_show_static_maps);
    }

    @Override
    public boolean isEnabled(Geocache cache) {
        return cache.hasStaticMap();
    }

    @Override
    public boolean isEnabled(Waypoint waypoint) {
        return hasStaticMap(waypoint);
    }

    @Override
    public void navigate(Activity activity, Geocache cache) {
        invokeStaticMaps(activity, cache, null, false);
    }

    @Override
    public void navigate(Activity activity, Waypoint waypoint) {
        invokeStaticMaps(activity, null, waypoint, false);
    }
}
