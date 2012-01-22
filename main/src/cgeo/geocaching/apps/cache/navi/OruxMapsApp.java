package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;

class OruxMapsApp extends AbstractPointNavigationApp {

    private static final String INTENT = "com.oruxmaps.VIEW_MAP_ONLINE";

    OruxMapsApp() {
        super(getString(R.string.cache_menu_oruxmaps), INTENT);
    }

    @Override
    protected void navigate(Activity activity, Geopoint point) {
        final Intent intent = new Intent(INTENT);
        intent.putExtra("latitude", point.getLatitude());//latitude, wgs84 datum
        intent.putExtra("longitude", point.getLongitude());//longitude, wgs84 datum

        activity.startActivity(intent);
    }

}
