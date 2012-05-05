package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;

class DownloadStaticMapsApp extends AbstractStaticMapsApp {

    DownloadStaticMapsApp() {
        super(getString(R.string.cache_menu_download_map_static));
    }

    @Override
    public boolean invoke(IGeoData geo, Activity activity, cgCache cache, cgWaypoint waypoint, final Geopoint coords) {
        return invoke(activity, cache, waypoint, true);
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        return !hasStaticMap(cache);
    }

    @Override
    public boolean isEnabled(cgWaypoint waypoint) {
        return !hasStaticMap(waypoint);
    }
}
