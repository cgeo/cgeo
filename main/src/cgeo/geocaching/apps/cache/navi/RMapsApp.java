package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;

import android.app.Activity;
import android.content.Intent;

import java.util.ArrayList;

class RMapsApp extends AbstractNavigationApp {

    private static final String INTENT = "com.robert.maps.action.SHOW_POINTS";

    RMapsApp() {
        super(getString(R.string.cache_menu_rmaps), INTENT);
    }

    @Override
    public boolean invoke(Activity activity, cgCache cache, cgWaypoint waypoint, final Geopoint coords) {
        try {
            final ArrayList<String> locations = new ArrayList<String>();
            if (cache != null && cache.getCoords() != null) {
                locations.add(cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA)
                        + ";" + cache.getGeocode()
                        + ";" + cache.getName());
            } else if (waypoint != null && waypoint.getCoords() != null) {
                locations.add(waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA)
                        + ";" + waypoint.getLookup()
                        + ";" + waypoint.getName());
            }

            if (!locations.isEmpty()) {
                final Intent intent = new Intent(INTENT);
                intent.putStringArrayListExtra("locations", locations);
                activity.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            // nothing
        }

        return false;
    }
}
