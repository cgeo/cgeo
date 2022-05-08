package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * http://developers.sygic.com/documentation.php?action=customurl_android
 */
abstract class SygicNavigationApp extends AbstractPointNavigationApp {

    private final String mode;

    private static final String PACKAGE_NORMAL = "com.sygic.aura";
    /**
     * there is a secondary edition of this app
     */
    private static final String PACKAGE_VOUCHER = "com.sygic.aura_voucher";

    private SygicNavigationApp(@StringRes final int nameResourceId, final String mode) {
        super(getString(nameResourceId), null, PACKAGE_NORMAL);
        this.mode = mode;
    }

    @Override
    public boolean isInstalled() {
        return ProcessUtils.isLaunchable(PACKAGE_NORMAL) || ProcessUtils.isLaunchable(PACKAGE_VOUCHER);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        final String str = "com.sygic.aura://coordinate|" + coords.getLongitude() + "|" + coords.getLatitude() + "|" + mode;
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
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
