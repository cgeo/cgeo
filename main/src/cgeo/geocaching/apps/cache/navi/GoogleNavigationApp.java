package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

class GoogleNavigationApp extends AbstractNavigationApp {

    GoogleNavigationApp() {
        super(getString(R.string.cache_menu_tbt), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public boolean invoke(final Activity activity, final cgCache cache, final cgWaypoint waypoint, final Geopoint coords) {
        if (activity == null) {
            return false;
        }

        IGeoData geo = cgeoapplication.getInstance().currentGeo();
        boolean navigationResult = false;
        if (coords != null) {
            navigationResult = navigateToCoordinates(geo, activity, coords);
        }
        else if (waypoint != null) {
            navigationResult = navigateToCoordinates(geo, activity, waypoint.getCoords());
        }
        else if (cache != null) {
            navigationResult = navigateToCoordinates(geo, activity, cache.getCoords());
        }

        if (!navigationResult) {
            ActivityMixin.showToast(activity, getString(R.string.err_navigation_no));
            return false;
        }

        return true;
    }

    private static boolean navigateToCoordinates(IGeoData geo, Activity activity, final Geopoint coords) {
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

}