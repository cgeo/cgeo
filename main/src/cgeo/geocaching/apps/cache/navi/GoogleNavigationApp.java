package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

class GoogleNavigationApp extends AbstractNavigationApp {

    GoogleNavigationApp(final Resources res) {
        super(res.getString(R.string.cache_menu_tbt), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    @Override
    public boolean invoke(final cgGeo geo, final Activity activity, final Resources res,
            final cgCache cache,
            final cgSearch search, final cgWaypoint waypoint, final Geopoint coords) {
        if (activity == null) {
            return false;
        }

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
            if (res != null) {
                ActivityMixin.showToast(activity, res.getString(R.string.err_navigation_no));
            }
            return false;
        }

        return true;
    }

    private static boolean navigateToCoordinates(cgGeo geo, Activity activity, final Geopoint coords) {
        final Geopoint coordsNow = geo == null ? null : geo.coordsNow;

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

        Log.i(Settings.tag,
                "cgBase.runNavigation: No navigation application available.");
        return false;
    }

}