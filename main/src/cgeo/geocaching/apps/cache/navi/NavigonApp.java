package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;

public class NavigonApp extends AbstractNavigationApp {

    private static final String INTENT = "android.intent.action.navigon.START_PUBLIC";
    private static final String INTENT_EXTRA_KEY_LATITUDE = "latitude";
    private static final String INTENT_EXTRA_KEY_LONGITUDE = "longitude";

    NavigonApp() {
        super("Navigon", INTENT);
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res, cgCache cache, SearchResult search, cgWaypoint waypoint, Geopoint coords) {
        if (cache == null && waypoint == null && coords == null) {
            return false;
        }

        try {
            if (isInstalled(activity)) {
                Geopoint usedCoords = getCoords(cache, waypoint, coords);

                final Intent intent = new Intent(INTENT);

                /*
                 * Long/Lat are float values in decimal degree format (+-DDD.DDDDD).
                 * Example:
                 * intent.putExtra(INTENT_EXTRA_KEY_LATITUDE, 46.12345f);
                 * intent.putExtra(INTENT_EXTRA_KEY_LONGITUDE, 23.12345f);
                 */
                intent.putExtra(INTENT_EXTRA_KEY_LATITUDE, (float) usedCoords.getLatitude());
                intent.putExtra(INTENT_EXTRA_KEY_LONGITUDE, (float) usedCoords.getLongitude());

                activity.startActivity(intent);

                return true;
            }
        } catch (Exception e) {
            // nothing
        }

        return false;
    }

    private static Geopoint getCoords(cgCache cache, cgWaypoint waypoint, Geopoint coords) {
        if (cache != null) {
            return cache.getCoords();
        }
        else if (waypoint != null) {
            return waypoint.getCoords();
        }
        return coords;
    }
}