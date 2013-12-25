package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;

/**
 * Application for communication with the Pebble watch.
 * 
 */
class PebbleApp extends AbstractPointNavigationApp {

    private static final String INTENT = "com.webmajstr.pebble_gc.NAVIGATE_TO";
    private static final String PACKAGE_NAME = "com.webmajstr.pebble_gc";

    PebbleApp() {
        super(getString(R.string.cache_menu_pebble), R.id.cache_app_pebble, INTENT, PACKAGE_NAME);
    }

    @Override
    public void navigate(Activity activity, Geopoint point) {
        final Intent pebbleIntent = new Intent(INTENT);
        pebbleIntent.putExtra("latitude", point.getLatitude());
        pebbleIntent.putExtra("longitude", point.getLongitude());
        activity.startActivity(pebbleIntent);
    }

    @Override
    public void navigate(Activity activity, Geocache cache) {
        final Intent pebbleIntent = new Intent(INTENT);
        pebbleIntent.putExtra("latitude", cache.getCoords().getLatitude());
        pebbleIntent.putExtra("longitude", cache.getCoords().getLongitude());
        pebbleIntent.putExtra("difficulty", cache.getDifficulty());
        pebbleIntent.putExtra("terrain", cache.getTerrain());
        pebbleIntent.putExtra("name", cache.getName());
        pebbleIntent.putExtra("code", cache.getGeocode());
        pebbleIntent.putExtra("size", cache.getSize().getL10n());
        activity.startActivity(pebbleIntent);
    }

}