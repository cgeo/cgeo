package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

class GoogleNavigationApp extends AbstractPointNavigationApp {

    GoogleNavigationApp() {
        super(getString(R.string.cache_menu_tbt), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    private static boolean navigateToCoordinates(Activity activity, final Geopoint coords) {
        IGeoData geo = cgeoapplication.getInstance().currentGeo();
        final Geopoint coordsNow = geo == null ? null : geo.getCoords();

        // Google Navigation
        if (Settings.isUseGoogleNavigation()) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("google.navigation:ll=" + coords.getLatitude() + ","
                                + coords.getLongitude())));

                return true;
            } catch (Exception e) {
                // nothing
            }
        }

        // Google Maps Directions
        try {
            if (coordsNow != null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://maps.google.com/maps?f=d&saddr="
                                + coordsNow.getLatitude() + "," + coordsNow.getLongitude() + "&daddr="
                                + coords.getLatitude() + "," + coords.getLongitude())));
            } else {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://maps.google.com/maps?f=d&daddr="
                                + coords.getLatitude() + "," + coords.getLongitude())));
            }

            return true;
        } catch (Exception e) {
            // nothing
        }

        Log.i("cgBase.runNavigation: No navigation application available.");
        return false;
    }

    @Override
    public void navigate(Activity activity, Geopoint coords) {
        if (!navigateToCoordinates(activity, coords)) {
            ActivityMixin.showToast(activity, getString(R.string.err_navigation_no));
        }
    }
}