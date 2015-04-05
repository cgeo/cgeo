package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.ProcessUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * http://help.sygic.com/entries/22207668-developers-only-sygic-implementation-to-your-app-android-sdk-iphone-sdk-url-
 * handler
 *
 */
class SygicNavigationApp extends AbstractPointNavigationApp {

    private static final String PACKAGE_NORMAL = "com.sygic.aura";
    /**
     * there is a secondary edition of this app
     */
    private static final String PACKAGE_VOUCHER = "com.sygic.aura_voucher";

    SygicNavigationApp() {
        super(getString(R.string.cache_menu_sygic), null, PACKAGE_NORMAL);
    }

    @Override
    public boolean isInstalled() {
        return ProcessUtils.isLaunchable(PACKAGE_NORMAL) || ProcessUtils.isLaunchable(PACKAGE_VOUCHER);
    }

    @Override
    public void navigate(final Activity activity, final Geopoint coords) {
        final String str = "http://com.sygic.aura/coordinate|" + coords.getLongitude() + "|" + coords.getLatitude() + "|show";
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
    }

}