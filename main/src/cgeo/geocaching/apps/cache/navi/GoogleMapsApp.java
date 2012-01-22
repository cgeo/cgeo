package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

class GoogleMapsApp extends AbstractPointNavigationApp {

    GoogleMapsApp() {
        super(getString(R.string.cache_menu_map_ext), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    @Override
    protected void navigate(Activity activity, Geopoint point) {
        // INFO: q parameter works with Google Maps, but breaks cooperation with all other apps
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:" + point.getLatitude() + "," + point.getLongitude())));
            return;
        } catch (Exception e) {
            // nothing
        }
        Log.i(Settings.tag, "cgBase.runExternalMap: No maps application available.");

        ActivityMixin.showToast(activity, getString(R.string.err_application_no));
    }

}
