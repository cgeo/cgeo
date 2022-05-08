package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;

import android.content.Intent;

/**
 * Application for communication with the Pebble watch.
 */
class PebbleApp extends AbstractRadarApp {

    private static final String INTENT = "com.webmajstr.pebble_gc.NAVIGATE_TO";
    private static final String PACKAGE_NAME = "com.webmajstr.pebble_gc";

    PebbleApp() {
        super(getString(R.string.cache_menu_pebble), INTENT, PACKAGE_NAME);
    }

    @Override
    protected void addCoordinates(final Intent intent, final Geopoint coords) {
        intent.putExtra(RADAR_EXTRA_LATITUDE, coords.getLatitude());
        intent.putExtra(RADAR_EXTRA_LONGITUDE, coords.getLongitude());
    }
}
