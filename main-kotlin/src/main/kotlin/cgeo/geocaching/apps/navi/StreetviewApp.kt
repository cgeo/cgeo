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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.utils.ProcessUtils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.annotation.NonNull

class StreetviewApp : AbstractPointNavigationApp() {

    private static val PACKAGE_NAME_STREET_VIEW: String = "com.google.android.street"
    private static val INSTALLED: Boolean = ProcessUtils.isInstalled(PACKAGE_NAME_STREET_VIEW)

    StreetviewApp() {
        super(getString(R.string.cache_menu_streetview), null)
    }

    override     public Boolean isInstalled() {
        return INSTALLED
    }

    override     public Unit navigate(final Context context, final Geopoint point) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("google.streetview:cbll=" + point.getLatitude() + "," + point.getLongitude())))
        } catch (final ActivityNotFoundException ignored) {
            ActivityMixin.showToast(context, CgeoApplication.getInstance().getString(R.string.err_application_no))
        }
    }
}
