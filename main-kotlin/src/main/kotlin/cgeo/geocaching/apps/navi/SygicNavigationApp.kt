// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.apps.navi

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.ProcessUtils

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.StringRes

/**
 * <a href="http://developers.sygic.com/documentation.php?action=customurl_android">...</a>
 */
abstract class SygicNavigationApp : AbstractPointNavigationApp() {

    private final String mode

    private static val PACKAGE_NORMAL: String = "com.sygic.aura"
    /**
     * there is a secondary edition of this app
     */
    private static val PACKAGE_VOUCHER: String = "com.sygic.aura_voucher"

    private SygicNavigationApp(@StringRes final Int nameResourceId, final String mode) {
        super(getString(nameResourceId), null, PACKAGE_NORMAL)
        this.mode = mode
    }

    override     public Boolean isInstalled() {
        return ProcessUtils.isLaunchable(PACKAGE_NORMAL) || ProcessUtils.isLaunchable(PACKAGE_VOUCHER)
    }

    override     public Unit navigate(final Context context, final Geopoint coords) {
        val str: String = "com.sygic.aura://coordinate|" + coords.getLongitude() + "|" + coords.getLatitude() + "|" + mode
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(str)))
    }

    static class SygicNavigationWalkingApp : SygicNavigationApp() {
        SygicNavigationWalkingApp() {
            super(R.string.cache_menu_sygic_walk, "walk")
        }
    }

    static class SygicNavigationDrivingApp : SygicNavigationApp() : TargetSelectorNavigationApp {
        SygicNavigationDrivingApp() {
            super(R.string.cache_menu_sygic_drive, "drive")
        }

        override         public Unit navigate(final Context context, final Geocache cache) {
            navigateWithTargetSelector(context, cache)
        }
    }
}
