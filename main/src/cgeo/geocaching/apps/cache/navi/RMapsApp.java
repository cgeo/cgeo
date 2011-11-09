package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;

import java.util.ArrayList;

class RMapsApp extends AbstractNavigationApp {

    private static final String INTENT = "com.robert.maps.action.SHOW_POINTS";

    RMapsApp(final Resources res) {
        super(res.getString(R.string.cache_menu_rmaps), INTENT);
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final cgSearch search, cgWaypoint waypoint, final Geopoint coords) {
        if (cache == null && waypoint == null && coords == null) {
            return false;
        }

        try {
            if (isInstalled(activity)) {
                final ArrayList<String> locations = new ArrayList<String>();
                if (cache != null && cache.getCoords() != null) {
                    locations.add(cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA)
                            + ";"
                            + cache.getGeocode()
                            + ";" + cache.getName());
                } else if (waypoint != null && waypoint.getCoords() != null) {
                    locations.add(waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA)
                            + ";"
                            + waypoint.getLookup()
                            + ";" + waypoint.getName());
                }

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
