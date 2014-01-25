package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;

class RadarApp extends AbstractPointNavigationApp {

    private static final String INTENT = "com.google.android.radar.SHOW_RADAR";
    private static final String PACKAGE_NAME = "com.eclipsim.gpsstatus2";

    RadarApp() {
        super(getString(R.string.cache_menu_radar), R.id.cache_app_radar, INTENT, PACKAGE_NAME);
    }

    @Override
    public void navigate(final Activity activity, final Geopoint point) {
        final Intent radarIntent = createRadarIntent(point);
        activity.startActivity(radarIntent);
    }

    private static Intent createRadarIntent(final Geopoint point) {
        final Intent radarIntent = new Intent(INTENT);
        radarIntent.putExtra("latitude", (float) point.getLatitude());
        radarIntent.putExtra("longitude", (float) point.getLongitude());
        return radarIntent;
    }

    @Override
    public void navigate(final Activity activity, final Geocache cache) {
        final Intent radarIntent = createRadarIntent(cache.getCoords());
        addIntentExtras(cache, radarIntent);
        activity.startActivity(radarIntent);
    }
}