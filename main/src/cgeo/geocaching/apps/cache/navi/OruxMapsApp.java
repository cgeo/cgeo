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

class OruxMapsApp extends AbstractNavigationApp {

    private static final String INTENT = "com.oruxmaps.VIEW_MAP_ONLINE";

    OruxMapsApp(final Resources res) {
        super(res.getString(R.string.cache_menu_oruxmaps), INTENT);
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final SearchResult search, cgWaypoint waypoint, final Geopoint coords) {
        if (cache == null && waypoint == null && coords == null) {
            return false;
        }

        try {
            if (isInstalled(activity)) {
                Geopoint usedCoords = getCoords(cache, waypoint, coords);

                final Intent intent = new Intent(INTENT);
                intent.putExtra("latitude", usedCoords.getLatitude());//latitude, wgs84 datum
                intent.putExtra("longitude", usedCoords.getLongitude());//longitude, wgs84 datum

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
