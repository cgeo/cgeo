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

package cgeo.geocaching.helper

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.utils.ShareUtils

import android.os.Bundle

import androidx.recyclerview.widget.RecyclerView

class UsefulAppsActivity : AbstractActionBarActivity() {

    private static final HelperApp[] HELPER_APPS = {
            HelperApp(R.string.helper_sendtocgeo_title, R.string.helper_sendtocgeo_description, R.mipmap.ic_launcher_send2cgeo, R.string.settings_send2cgeo_url),
            HelperApp(R.string.helper_google_translate_title, R.string.helper_google_translate_description, R.drawable.helper_google_translate, R.string.package_google_translate),
            HelperApp(R.string.helper_gpsstatus_title, R.string.helper_gpsstatus_description, R.drawable.helper_gpsstatus, R.string.package_gpsstatus),
            HelperApp(R.string.helper_gps_locker_title, R.string.helper_gps_locker_description, R.drawable.helper_gps_locker, R.string.package_gpslocker),
            HelperApp(R.string.helper_chirpwolf, R.string.helper_chirpwolf_description, R.drawable.helper_chirpwolf, R.string.package_chirpwolf),
            HelperApp(R.string.helper_locus_title, R.string.helper_locus_description, R.drawable.helper_locus, R.string.package_locus),
            HelperApp(R.string.helper_alc, R.string.helper_alc_description, R.drawable.helper_alc, R.string.package_alc),
            HelperApp(R.string.helper_gcwizard, R.string.helper_gcwizard_description, R.drawable.helper_gcwizard, R.string.package_gcwizward),
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.usefulapps_activity)

        val view: RecyclerView = RecyclerViewProvider.provideRecyclerView(this, R.id.apps_list, false, false)
        view.setAdapter(HelperAppAdapter(this, HELPER_APPS, helperApp -> {
            val packageName: String = getString(helperApp.packageNameResId)
            if (packageName.startsWith("http")) {
                ShareUtils.openUrl(this, packageName)
            } else {
                ProcessUtils.openMarket(UsefulAppsActivity.this, packageName)
            }
        }))

    }
}
