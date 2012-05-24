package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;

class RadarApp extends AbstractPointNavigationApp {

    private static final String INTENT = "com.google.android.radar.SHOW_RADAR";
    private static final String PACKAGE_NAME = "com.eclipsim.gpsstatus2";

    RadarApp() {
        super(getString(R.string.cache_menu_radar), INTENT, PACKAGE_NAME);
    }

    @Override
    public void navigate(Activity activity, Geopoint point) {
        final Intent radarIntent = new Intent(INTENT);
        radarIntent.putExtra("latitude", (float) point.getLatitude());
        radarIntent.putExtra("longitude", (float) point.getLongitude());
        activity.startActivity(radarIntent);
    }
}