package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;

import java.util.UUID;

class RadarApp extends AbstractNavigationApp implements NavigationApp {

    private static final String INTENT = "com.google.android.radar.SHOW_RADAR";
    private static final String PACKAGE_NAME = "com.eclipsim.gpsstatus2";

    RadarApp(final Resources res) {
        super(res.getString(R.string.cache_menu_radar), INTENT, PACKAGE_NAME);
    }

    private static void navigateTo(Activity activity, final Geopoint coords) {
        Intent radarIntent = new Intent(INTENT);
        radarIntent.putExtra("latitude", (float) coords.getLatitude());
        radarIntent.putExtra("longitude", (float) coords.getLongitude());
        activity.startActivity(radarIntent);
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final UUID searchId, cgWaypoint waypoint, final Geopoint coords) {
        if (cache != null) {
            if (cache.coords != null) {
                navigateTo(activity, cache.coords);
                return true;
            }
        }
        if (waypoint != null) {
            if (waypoint.coords != null) {
                navigateTo(activity, waypoint.coords);
                return true;
            }
        }
        if (coords != null) {
            navigateTo(activity, coords);
            return true;
        }
        return false;
    }
}
