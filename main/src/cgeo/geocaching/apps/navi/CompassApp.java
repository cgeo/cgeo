package cgeo.geocaching.apps.navi;

import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.location.Geopoint;

import android.support.annotation.NonNull;

import android.app.Activity;

class CompassApp extends AbstractPointNavigationApp {

    CompassApp() {
        super(getString(R.string.compass_title), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geopoint coords) {
        CompassActivity.startActivityPoint(activity, coords, getString(R.string.navigation_direct_navigation));
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Waypoint waypoint) {
        CompassActivity.startActivityWaypoint(activity, waypoint);
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geocache cache) {
        CompassActivity.startActivityCache(activity, cache);
    }

}
