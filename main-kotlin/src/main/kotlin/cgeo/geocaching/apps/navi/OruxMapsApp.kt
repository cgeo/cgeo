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
import cgeo.geocaching.models.Waypoint

import android.content.Context
import android.content.Intent

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import org.apache.commons.lang3.StringUtils

abstract class OruxMapsApp : AbstractPointNavigationApp() {

    private static val ORUXMAPS_EXTRA_LONGITUDE: String = "targetLon"
    private static val ORUXMAPS_EXTRA_LATITUDE: String = "targetLat"
    private static val ORUXMAPS_EXTRA_NAME: String = "targetName"
    private static val INTENT_ONLINE: String = "com.oruxmaps.VIEW_MAP_ONLINE"
    private static val INTENT_OFFLINE: String = "com.oruxmaps.VIEW_MAP_OFFLINE"

    private OruxMapsApp(@StringRes final Int nameResourceId, final String intent) {
        super(getString(nameResourceId), intent)
    }

    private Unit navigate(final Context context, final Geopoint point, final String name) {
        val intent: Intent = Intent(this.intent)
        final Double[] targetLat = {point.getLatitude()}
        final Double[] targetLon = {point.getLongitude()}
        intent.putExtra(ORUXMAPS_EXTRA_LATITUDE, targetLat); //latitude, wgs84 datum
        intent.putExtra(ORUXMAPS_EXTRA_LONGITUDE, targetLon); //longitude, wgs84 datum
        if (StringUtils.isNotBlank(name)) {
            final String[] targetName = {name}
            intent.putExtra(ORUXMAPS_EXTRA_NAME, targetName)
        }

        context.startActivity(intent)
    }

    override     public Unit navigate(final Context context, final Geopoint point) {
        navigate(context, point, "Waypoint")
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        val coords: Geopoint = cache.getCoords()
        assert coords != null; // guaranteed by caller
        navigate(context, coords, cache.getName())
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        val coords: Geopoint = waypoint.getCoords()
        assert coords != null; // guaranteed by caller
        navigate(context, coords, waypoint.getName())
    }

    static class OruxOnlineMapApp : OruxMapsApp() {
        OruxOnlineMapApp() {
            super(R.string.cache_menu_oruxmaps_online, INTENT_ONLINE)
        }
    }

    static class OruxOfflineMapApp : OruxMapsApp() {
        OruxOfflineMapApp() {
            super(R.string.cache_menu_oruxmaps_offline, INTENT_OFFLINE)
        }
    }
}
