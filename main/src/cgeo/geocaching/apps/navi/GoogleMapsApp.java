package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

class GoogleMapsApp extends AbstractPointNavigationApp {

    GoogleMapsApp() {
        super(getString(R.string.cache_menu_map_ext), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint point) {
        navigate(context, point, context.getString(R.string.waypoint));
    }

    private static void navigate(final Context context, final Geopoint point, final String label) {
        try {
            final String latitude = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, point);
            final String longitude = GeopointFormatter.format(Format.LON_DECDEGREE_RAW, point);
            final String geoLocation = "geo:" + latitude + "," + longitude;
            final String query = latitude + "," + longitude + "(" + label + ")";
            final String uriString = geoLocation + "?q=" + Uri.encode(query);
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uriString)));
            return;
        } catch (final RuntimeException ignored) {
            // nothing
        }
        Log.i("GoogleMapsApp.navigate: No maps application available.");

        ActivityMixin.showToast(context, getString(R.string.err_application_no));
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        navigate(context, cache.getCoords(), cache.getName());
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        navigate(context, waypoint.getCoords(), waypoint.getName());
    }
}
