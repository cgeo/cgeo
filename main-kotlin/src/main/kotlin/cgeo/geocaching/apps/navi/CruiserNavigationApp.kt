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
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.LocationDataProvider

import android.content.Context
import android.content.Intent

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

class CruiserNavigationApp : AbstractPointNavigationApp() {

    static val ACTION: String = "com.devemux86.intent.action.NAVIGATION"

    protected CruiserNavigationApp() {
        super(getString(R.string.cache_menu_cruiser), null, getString(R.string.package_cruiser))
    }

    override     public Unit navigate(final Context context, final Geopoint coords) {
        navigate(context, coords, null)
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        val coords: Geopoint = cache.getCoords()
        assert coords != null; // asserted by caller
        navigate(context, coords, cache.getName())
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        val coords: Geopoint = waypoint.getCoords()
        assert coords != null; // asserted by caller
        navigate(context, coords, waypoint.getName())
    }

    private Unit navigate(final Context context, final Geopoint coords, final String info) {
        val geo: GeoData = LocationDataProvider.getInstance().currentGeo()
        val intent: Intent = Intent()
        intent.setAction(ACTION)
        intent.setPackage(getString(R.string.package_cruiser))
        intent.putExtra("LATITUDE", Double[]{geo.getLatitude(), coords.getLatitude()})
        intent.putExtra("LONGITUDE", Double[]{geo.getLongitude(), coords.getLongitude()})
        if (StringUtils.isNotBlank(info)) {
            intent.putExtra("NAME", String[] {info})
        }
        context.startActivity(intent)
    }
}
