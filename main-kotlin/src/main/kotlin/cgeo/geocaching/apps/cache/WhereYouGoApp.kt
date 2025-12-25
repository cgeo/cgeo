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

package cgeo.geocaching.apps.cache

import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.wherigo.WherigoUtils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.annotation.NonNull

import java.util.List

class WhereYouGoApp : AbstractGeneralApp() {

    public WhereYouGoApp() {
        super(getString(R.string.cache_menu_whereyougo), "menion.android.whereyougo")
    }

    override     public Boolean isEnabled(final Geocache cache) {
        return cache.getType() == CacheType.WHERIGO && !WherigoUtils.getWherigoGuids(cache).isEmpty()
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        val guids: List<String> = WherigoUtils.getWherigoGuids(cache)
        if (!guids.isEmpty()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WherigoUtils.getWherigoDownloadUrl(guids.get(0)))))
        }
    }

    public static Boolean isWhereYouGoInstalled() {
        return null != ProcessUtils.getLaunchIntent(getString(R.string.package_whereyougo))
    }

    public static Unit openWherigo(final Activity activity, final String guid) {
        // re-check installation state, might have changed since creating the view
        if (isWhereYouGoInstalled()) {
            val intent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(WherigoUtils.getWherigoDownloadUrl(guid)))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } else {
            ProcessUtils.openMarket(activity, activity.getString(R.string.package_whereyougo))
        }
    }

}
