package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;

/**
 * navigation app for simple point navigation (no differentiation between cache/waypoint/point)
 */
abstract class AbstractPointNavigationApp extends AbstractNavigationApp {

    protected AbstractPointNavigationApp(String name, String intent) {
        super(name, intent);
    }

    protected AbstractPointNavigationApp(String name, String intent, String packageName) {
        super(name, intent, packageName);
    }

    @Override
    public final boolean invoke(Activity activity, cgCache cache, cgWaypoint waypoint, Geopoint coords) {
        if (cache == null && waypoint == null && coords == null) {
            return false;
        }

        try {
            if (isInstalled()) {
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

    /**
     * Return the first of the cache coordinates, the waypoint coordinates or the extra coordinates. <code>null</code>
     * entities are skipped.
     *
     * @param cache a cache
     * @param waypoint a waypoint
     * @param coords extra coordinates
     * @return the first non-null coordinates, or null if none are set
     */
    private static Geopoint getCoordinates(final cgCache cache, final cgWaypoint waypoint, final Geopoint coords) {
        if (cache != null && cache.getCoords() != null) {
            return cache.getCoords();
        }

        if (waypoint != null && waypoint.getCoords() != null) {
            return waypoint.getCoords();
        }

        return coords;
    }
}