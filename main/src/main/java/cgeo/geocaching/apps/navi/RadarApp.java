package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;

import android.content.Intent;

class RadarApp extends AbstractRadarApp {

    private static final String INTENT = "com.google.android.radar.SHOW_RADAR";
    private static final String PACKAGE_NAME = "com.eclipsim.gpsstatus2";

    RadarApp() {
        super(getString(R.string.cache_menu_radar), INTENT, PACKAGE_NAME);
    }

    @Override
    protected void addCoordinates(final Intent intent, final Geopoint coords) {
        intent.putExtra(RADAR_EXTRA_LATITUDE, (float) coords.getLatitude());
        intent.putExtra(RADAR_EXTRA_LONGITUDE, (float) coords.getLongitude());
    }

}
