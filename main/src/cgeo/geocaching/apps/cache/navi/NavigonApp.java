package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;

import android.app.Activity;
import android.content.Intent;

class NavigonApp extends AbstractPointNavigationApp {

    private static final String INTENT = "android.intent.action.navigon.START_PUBLIC";
    private static final String INTENT_EXTRA_KEY_LATITUDE = "latitude";
    private static final String INTENT_EXTRA_KEY_LONGITUDE = "longitude";

    NavigonApp() {
        super(getString(R.string.cache_menu_navigon), R.id.cache_app_navigon, INTENT);
    }

    @Override
    public void navigate(Activity activity, Geopoint point) {
        final Intent intent = new Intent(INTENT);

        /*
         * Long/Lat are float values in decimal degree format (+-DDD.DDDDD).
         * Example:
         * intent.putExtra(INTENT_EXTRA_KEY_LATITUDE, 46.12345f)
         * intent.putExtra(INTENT_EXTRA_KEY_LONGITUDE, 23.12345f)
         */
        intent.putExtra(INTENT_EXTRA_KEY_LATITUDE, (float) point.getLatitude());
        intent.putExtra(INTENT_EXTRA_KEY_LONGITUDE, (float) point.getLongitude());

        activity.startActivity(intent);
    }
}