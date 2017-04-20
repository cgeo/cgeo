package cgeo.geocaching.apps.navi;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

/**
 * For use with any Android Wear geocaching apps which can handle the intent action below.
 */
class AndroidWearApp extends AbstractPointNavigationApp {
    private static final String INTENT_ACTION = "cgeo.geocaching.wear.NAVIGATE_TO";
    private static final String INTENT_PACKAGE = "com.javadog.cgeowear";

    AndroidWearApp() {
        super(getString(R.string.cache_menu_android_wear), INTENT_ACTION, null);
    }

    @Override
    public boolean isInstalled() {
        return ProcessUtils.isIntentAvailable(INTENT_ACTION);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        navigate(context, null, null, coords);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        navigate(context, cache.getName(), cache.getGeocode(), cache.getCoords());
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        navigate(context, waypoint.getName(), waypoint.getGeocode(), waypoint.getCoords());
    }

    private static void navigate(final Context context, final String destName,
                                 final String destCode, final Geopoint coords) {
        final Intent launchIntent = new Intent(INTENT_ACTION);
        launchIntent.setPackage(INTENT_PACKAGE);
        launchIntent.putExtra(Intents.EXTRA_NAME, destName)
                .putExtra(Intents.EXTRA_GEOCODE, destCode)
                .putExtra(Intents.EXTRA_LATITUDE, coords.getLatitude())
                .putExtra(Intents.EXTRA_LONGITUDE, coords.getLongitude());
        context.startService(launchIntent);
    }
}
