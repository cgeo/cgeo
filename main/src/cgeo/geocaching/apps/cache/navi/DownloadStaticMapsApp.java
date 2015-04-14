package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

class DownloadStaticMapsApp extends AbstractStaticMapsApp {

    DownloadStaticMapsApp() {
        super(getString(R.string.cache_menu_download_map_static));
    }

    @Override
    public boolean isEnabled(final @NonNull Geocache cache) {
        return cache.isOffline() && !cache.hasStaticMap();
    }

    @Override
    public boolean isEnabled(final @NonNull Waypoint waypoint) {
        return !hasStaticMap(waypoint);
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geocache cache) {
        invokeStaticMaps(activity, cache, null, true);
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Waypoint waypoint) {
        invokeStaticMaps(activity, null, waypoint, true);
    }
}
