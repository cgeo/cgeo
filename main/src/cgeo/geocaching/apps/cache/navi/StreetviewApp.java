package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

class StreetviewApp extends AbstractNavigationApp implements NavigationApp {

    StreetviewApp(final Resources res) {
        super(res.getString(R.string.cache_menu_streetview), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final cgSearch search, cgWaypoint waypoint, final Geopoint coords) {
        if (cache == null && waypoint == null && coords == null) {
            return false;
        }

        try {
            if (cache != null && cache.getCoords() != null) {
                startActivity(activity, cache.getCoords());
            } else if (waypoint != null && waypoint.getCoords() != null) {
                startActivity(activity, waypoint.getCoords());
            } else if (coords != null) {
                startActivity(activity, coords);
            }

            return true;
        } catch (ActivityNotFoundException e) {
            if (res != null) {
                ActivityMixin.showToast(activity, res.getString(R.string.err_application_no));
            }
        }

        return false;
    }

    private static void startActivity(Activity activity, final Geopoint coords) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("google.streetview:cbll=" + coords.getLatitude() + "," + coords.getLongitude())));
    }
}
