package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;

/**
 * navigation app for simple point navigation (no differentiation between cache/waypoint/point)
 *
 * @author bananeweizen
 *
 */
abstract class AbstractPointNavigationApp extends AbstractNavigationApp {

    protected AbstractPointNavigationApp(String name, String intent) {
        super(name, intent);
    }

    protected AbstractPointNavigationApp(String name, String intent, String packageName) {
        super(name, intent, packageName);
    }

    @Override
    public final boolean invoke(cgGeo geo, Activity activity, cgCache cache, SearchResult search, cgWaypoint waypoint, Geopoint coords) {
        if (cache == null && waypoint == null && coords == null) {
            return false;
        }

        try {
            if (isInstalled(activity)) {
                final Geopoint point = getCoordinates(cache, waypoint, coords);
                if (point != null) {
                    navigate(activity, point);
                    return true;
                }
            }
        } catch (Exception e) {
            // nothing
        }

        return false;
    }

    protected abstract void navigate(Activity activity, Geopoint point);

    private static Geopoint getCoordinates(cgCache cache, cgWaypoint waypoint, Geopoint coords) {
        if (cache != null && cache.getCoords() != null) {
            return cache.getCoords();
        }
        else if (waypoint != null && waypoint.getCoords() != null) {
            return waypoint.getCoords();
        }
        return coords;
    }
}