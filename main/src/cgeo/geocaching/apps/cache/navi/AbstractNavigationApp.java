package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.apps.AbstractApp;

abstract class AbstractNavigationApp extends AbstractApp implements NavigationApp {

    protected AbstractNavigationApp(String name, String intent, String packageName) {
        super(name, intent, packageName);
    }

    protected AbstractNavigationApp(String name, String intent) {
        super(name, intent);
    }

}
