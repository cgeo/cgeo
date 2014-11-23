package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

abstract class GoogleNavigationApp extends AbstractPointNavigationApp {

    private final String mode;

    private GoogleNavigationApp(final int nameResourceId, final int id, final String mode) {
        super(getString(nameResourceId), id, null);
        this.mode = mode;
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(final Activity activity, final Geopoint coords) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("google.navigation:ll=" + coords.getLatitude() + ","
                            + coords.getLongitude() + "&mode=" + mode)));

        } catch (final Exception e) {
            Log.i("GoogleNavigationApp.navigate: No navigation application available.", e);
        }
    }

    static class GoogleNavigationWalkingApp extends GoogleNavigationApp {
        GoogleNavigationWalkingApp() {
            super(R.string.cache_menu_navigation_walk, R.id.cache_app_google_navigation_walk, "w");
        }
    }

    static class GoogleNavigationDrivingApp extends GoogleNavigationApp {
        GoogleNavigationDrivingApp() {
            super(R.string.cache_menu_navigation_drive, R.id.cache_app_google_navigation_drive, "d");
        }
    }

    static class GoogleNavigationBikeApp extends GoogleNavigationApp {
        GoogleNavigationBikeApp() {
            super(R.string.cache_menu_navigation_bike, R.id.cache_app_google_navigation_bike, "b");
        }
    }
}