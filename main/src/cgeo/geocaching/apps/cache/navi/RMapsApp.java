package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Locale;

class RMapsApp extends AbstractNavigationApp implements NavigationApp {

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
                if (cache != null && cache.coords != null) {
                    locations.add(String.format((Locale) null, "%.6f",
                            cache.coords.getLatitude())
                            + ","
                            + String.format((Locale) null, "%.6f",
                                    cache.coords.getLongitude())
                            + ";"
                            + cache.geocode
                            + ";" + cache.name);
                } else if (waypoint != null && waypoint.coords != null) {
                    locations.add(String.format((Locale) null, "%.6f",
                            waypoint.coords.getLatitude())
                            + ","
                            + String.format((Locale) null, "%.6f",
                                    waypoint.coords.getLongitude())
                            + ";"
                            + waypoint.lookup
                            + ";" + waypoint.name);
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
