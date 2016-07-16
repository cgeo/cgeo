package cgeo.geocaching.apps.navi;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Waypoint;

import android.support.annotation.NonNull;

import android.app.Activity;

class DownloadStaticMapsApp extends AbstractStaticMapsApp {

    DownloadStaticMapsApp() {
        super(getString(R.string.cache_menu_download_map_static));
    }

    @Override
    public boolean isEnabled(@NonNull final Geocache cache) {
        return cache.isOffline() && !cache.hasStaticMap();
    }

    @Override
    public boolean isEnabled(@NonNull final Waypoint waypoint) {
        return !hasStaticMap(waypoint);
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geocache cache) {
        invokeStaticMaps(activity, cache, null, true);
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Waypoint waypoint) {
        invokeStaticMaps(activity, null, waypoint, true);
    }
}
