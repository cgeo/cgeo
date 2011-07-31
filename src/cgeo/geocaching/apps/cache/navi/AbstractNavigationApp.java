package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.apps.AbstractApp;

abstract class AbstractNavigationApp extends AbstractApp implements NavigationApp {

	AbstractNavigationApp(String name, String intent, String packageName) {
		super(name, intent, packageName);
	}

	AbstractNavigationApp(String name, String intent) {
		super(name, intent);
	}

}
