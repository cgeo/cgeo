package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

class GoogleMapsApp extends AbstractPointNavigationApp {

    GoogleMapsApp() {
        super(getString(R.string.cache_menu_map_ext), R.id.cache_app_google_maps, null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(Activity activity, Geopoint point) {
        navigate(activity, point, activity.getString(R.string.waypoint));
    }

    private static void navigate(Activity activity, Geopoint point, String label) {
        try {
            final String geoLocation = "geo:" + point.getLatitude() + "," + point.getLongitude();
            final String query = point.getLatitude() + "," + point.getLongitude() + "(" + label + ")";
            final String uriString = geoLocation + "?q=" + Uri.encode(query);
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uriString)));
            return;
        } catch (RuntimeException e) {
            // nothing
        }
        Log.i("GoogleMapsApp.navigate: No maps application available.");

        ActivityMixin.showToast(activity, getString(R.string.err_application_no));
    }

    @Override
    public void navigate(Activity activity, Geocache cache) {
        navigate(activity, cache.getCoords(), cache.getName());
    }

    @Override
    public void navigate(Activity activity, Waypoint waypoint) {
        navigate(activity, waypoint.getCoords(), waypoint.getName());
    }
}
