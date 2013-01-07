package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.cgCache;

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
    public boolean isEnabled(Waypoint waypoint) {
        return hasStaticMap(waypoint);
    }

    @Override
    public void navigate(Activity activity, cgCache cache) {
        invokeStaticMaps(activity, cache, null, false);
    }

    @Override
    public void navigate(Activity activity, Waypoint waypoint) {
        invokeStaticMaps(activity, null, waypoint, false);
    }
}
