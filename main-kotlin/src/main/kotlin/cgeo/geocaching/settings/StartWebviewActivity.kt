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

package cgeo.geocaching.settings

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.ProcessUtils.isChromeLaunchable

import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle

import java.util.List


class StartWebviewActivity : AbstractActivity() {

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        val url: String = getIntent().getDataString()
        if (url != null) {
            if (isChromeLaunchable()) {
                ShareUtils.openCustomTab(this, url)
            } else {
                val browsers: List<ResolveInfo> = ProcessUtils.getInstalledBrowsers(this)
                if (!browsers.isEmpty()) {
                    val resolveInfo: ResolveInfo = browsers.get(0)

                    val launchIntent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    launchIntent.setPackage(resolveInfo.activityInfo.packageName)

                    startActivity(launchIntent)
                } else {
                    ActivityMixin.showShortToast(this, R.string.no_browser_found)
                }
            }
        }
        finish()
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }
}
