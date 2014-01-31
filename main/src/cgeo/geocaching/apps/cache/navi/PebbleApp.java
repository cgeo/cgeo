package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;

/**
 * Application for communication with the Pebble watch.
 *
 */
class PebbleApp extends AbstractRadarApp {

    private static final String INTENT = "com.webmajstr.pebble_gc.NAVIGATE_TO";
    private static final String PACKAGE_NAME = "com.webmajstr.pebble_gc";

    PebbleApp() {
        super(getString(R.string.cache_menu_pebble), R.id.cache_app_pebble, INTENT, PACKAGE_NAME);
    }

}