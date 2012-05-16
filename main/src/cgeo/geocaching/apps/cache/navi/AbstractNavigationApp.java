package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.geopoint.Geopoint;

abstract class AbstractNavigationApp extends AbstractApp implements NavigationApp {

    protected AbstractNavigationApp(String name, String intent, String packageName) {
        super(name, intent, packageName);
    }

    protected AbstractNavigationApp(String name, String intent) {
        super(name, intent);
    }

    @Override
    public boolean isEnabled(cgWaypoint waypoint) {
        return true;
    }

    @Override
    public boolean isEnabled(Geopoint geopoint) {
        return geopoint != null;
    }
}
