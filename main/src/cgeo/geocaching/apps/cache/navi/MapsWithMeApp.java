package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.location.Geopoint;

import com.mapswithme.maps.api.MapsWithMeApi;

import android.app.Activity;

class MapsWithMeApp extends AbstractPointNavigationApp {

    protected MapsWithMeApp() {
        super(getString(R.string.cache_menu_mapswithme), R.id.cache_app_mapswithme, null);
    }

    @Override
    public void navigate(final Activity activity, final Geopoint coords) {
        navigate(activity, coords, getString(R.string.unknown));
    }

    @Override
    public void navigate(final Activity activity, final Geocache cache) {
        navigate(activity, cache.getCoords(), cache.getName());
    }

    private static void navigate(final Activity activity, final Geopoint coords, final String label) {
        MapsWithMeApi.showPointOnMap(activity, coords.getLatitude(), coords.getLongitude(), label);
    }

    @Override
    public void navigate(final Activity activity, final Waypoint waypoint) {
        navigate(activity, waypoint.getCoords(), waypoint.getName());
    }

    @Override
    public boolean isInstalled() {
        // the library can handle the app not being installed
        return true;
    }

}
