package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.ProcessUtils;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * http://developers.sygic.com/documentation.php?action=customurl_android
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
    public void navigate(final @NonNull Activity activity, final @NonNull Geopoint coords) {
        final String str = "com.sygic.aura://coordinate|" + coords.getLongitude() + "|" + coords.getLatitude() + "|show";
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
    }

}