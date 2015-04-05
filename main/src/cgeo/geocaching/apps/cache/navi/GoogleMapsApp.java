package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

class GoogleMapsApp extends AbstractPointNavigationApp {

    GoogleMapsApp() {
        super(getString(R.string.cache_menu_map_ext), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(final Activity activity, final Geopoint point) {
        navigate(activity, point, activity.getString(R.string.waypoint));
    }

    private static void navigate(final Activity activity, final Geopoint point, final String label) {
        try {
            final String latitude = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, point);
            final String longitude = GeopointFormatter.format(Format.LON_DECDEGREE_RAW, point);
            final String geoLocation = "geo:" + latitude + "," + longitude;
            final String query = latitude + "," + longitude + "(" + label + ")";
            final String uriString = geoLocation + "?q=" + Uri.encode(query);
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uriString)));
            return;
        } catch (final RuntimeException ignored) {
            // nothing
        }
        Log.i("GoogleMapsApp.navigate: No maps application available.");

        ActivityMixin.showToast(activity, getString(R.string.err_application_no));
    }

    @Override
    public void navigate(final Activity activity, final Geocache cache) {
        navigate(activity, cache.getCoords(), cache.getName());
    }

    @Override
    public void navigate(final Activity activity, final Waypoint waypoint) {
        navigate(activity, waypoint.getCoords(), waypoint.getName());
    }
}
