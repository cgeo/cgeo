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
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.location.GeopointFormatter.Format
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.utils.Log

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.StringRes

abstract class OtherMapsApp : AbstractPointNavigationApp() {

    final Boolean withLabel

    OtherMapsApp(@StringRes final Int title, final Boolean withLabel) {
        super(getString(title), null)
        this.withLabel = withLabel
    }

    override     public Boolean isInstalled() {
        return true
    }

    override     public Unit navigate(final Context context, final Geopoint point) {
        navigate(context, point, context.getString(R.string.waypoint))
    }

    private Unit navigate(final Context context, final Geopoint point, final String label) {
        try {
            val latitude: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, point)
            val longitude: String = GeopointFormatter.format(Format.LON_DECDEGREE_RAW, point)
            val geoLocation: String = "geo:" + latitude + "," + longitude
            val query: String = latitude + "," + longitude + (withLabel ? "(" + label + ")" : "")
            val uriString: String = geoLocation + "?q=" + Uri.encode(query)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
            return
        } catch (final RuntimeException ignored) {
            // nothing
        }
        Log.i("OtherMapsApp.navigate: No maps application available.")

        ActivityMixin.showToast(context, getString(R.string.err_application_no))
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        navigate(context, cache.getCoords(), cache.getName())
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        navigate(context, waypoint.getCoords(), waypoint.getName())
    }

    static class OtherMapsAppWithLabel : OtherMapsApp() {
        OtherMapsAppWithLabel() {
            super(R.string.cache_menu_map_ext, true)
        }
    }

    static class OtherMapsAppWithoutLabel : OtherMapsApp() {
        OtherMapsAppWithoutLabel() {
            super(R.string.cache_menu_map_ext_nolabel, false)
        }
    }
}
