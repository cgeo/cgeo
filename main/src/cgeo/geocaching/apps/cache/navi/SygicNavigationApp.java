package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * http://help.sygic.com/entries/22207668-developers-only-sygic-implementation-to-your-app-android-sdk-iphone-sdk-url-
 * handler
 *
 */
public class SygicNavigationApp extends AbstractPointNavigationApp {

    private static final String PACKAGE = "com.sygic.aura";

    SygicNavigationApp() {
        super(getString(R.string.cache_menu_sygic), null, PACKAGE);
    }

    @Override
    public void navigate(Activity activity, Geopoint coords) {
        String str = "http://com.sygic.aura/coordinate|" + coords.getLongitude() + "|" + coords.getLatitude() + "|show";
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
    }

}