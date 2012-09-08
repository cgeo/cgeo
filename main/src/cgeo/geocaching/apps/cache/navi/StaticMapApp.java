package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;

import android.app.Activity;

class StaticMapApp extends AbstractStaticMapsApp {

    StaticMapApp() {
        super(getString(R.string.cache_menu_map_static));
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        return cache.hasStaticMap();
    }

    @Override
    public boolean isEnabled(cgWaypoint waypoint) {
        return hasStaticMap(waypoint);
    }

    @Override
    public void navigate(Activity activity, cgCache cache) {
        invokeStaticMaps(activity, cache, null, false);
    }

    @Override
    public void navigate(Activity activity, cgWaypoint waypoint) {
        invokeStaticMaps(activity, null, waypoint, false);
    }
}
