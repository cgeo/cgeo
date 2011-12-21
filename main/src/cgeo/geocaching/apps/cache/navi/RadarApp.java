package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;

class RadarApp extends AbstractNavigationApp {

    private static final String INTENT = "com.google.android.radar.SHOW_RADAR";
    private static final String PACKAGE_NAME = "com.eclipsim.gpsstatus2";

    RadarApp(final Resources res) {
        super(res.getString(R.string.cache_menu_radar), INTENT, PACKAGE_NAME);
    }

    private static boolean navigateTo(Activity activity, final Geopoint coords) {
        if (coords == null) {
            return false;
        }
        Intent radarIntent = new Intent(INTENT);
        radarIntent.putExtra("latitude", (float) coords.getLatitude());
        radarIntent.putExtra("longitude", (float) coords.getLongitude());
        activity.startActivity(radarIntent);
        return true;
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint coords) {
        if (cache != null) {
            return navigateTo(activity, cache.getCoords());
        }
        if (waypoint != null) {
            return navigateTo(activity, waypoint.getCoords());
        }
        return navigateTo(activity, coords);
    }
}
