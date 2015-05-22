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
abstract class SygicNavigationApp extends AbstractPointNavigationApp {

    private final String mode;

    private static final String PACKAGE_NORMAL = "com.sygic.aura";
    /**
     * there is a secondary edition of this app
     */
    private static final String PACKAGE_VOUCHER = "com.sygic.aura_voucher";

    private SygicNavigationApp(final int nameResourceId, final String mode) {
        super(getString(nameResourceId), null, PACKAGE_NORMAL);
        this.mode = mode;
    }

    @Override
    public boolean isInstalled() {
        return ProcessUtils.isLaunchable(PACKAGE_NORMAL) || ProcessUtils.isLaunchable(PACKAGE_VOUCHER);
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geopoint coords) {
        final String str = "com.sygic.aura://coordinate|" + coords.getLongitude() + "|" + coords.getLatitude() + "|" + mode;
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
    }

    static class SygicNavigationWalkingApp extends SygicNavigationApp {
        SygicNavigationWalkingApp() {
            super(R.string.cache_menu_sygic_walk, "walk");
        }
    }

    static class SygicNavigationDrivingApp extends SygicNavigationApp {
        SygicNavigationDrivingApp() {
            super(R.string.cache_menu_sygic_drive, "drive");
        }
    }
}