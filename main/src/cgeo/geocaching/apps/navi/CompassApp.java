package cgeo.geocaching.apps.navi;

import cgeo.geocaching.CompassActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.content.Context;

import androidx.annotation.NonNull;

class CompassApp extends AbstractPointNavigationApp {

    CompassApp() {
        super(getString(R.string.compass_title), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        CompassActivity.startActivityPoint(context, coords, getString(R.string.navigation_direct_navigation));
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        CompassActivity.startActivityWaypoint(context, waypoint);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        CompassActivity.startActivityCache(context, cache);
    }

}
