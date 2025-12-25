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
import cgeo.geocaching.maps.MapProviderFactory
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.utils.Log

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.annotation.NonNull

class GoogleMapsDirectionApp : AbstractPointNavigationApp() {

    protected GoogleMapsDirectionApp() {
        super(getString(R.string.cache_menu_maps_directions), null)
    }

    override     public Boolean isInstalled() {
        return MapProviderFactory.isGoogleMapsInstalled()
    }

    override     public Unit navigate(final Context context, final Geopoint coords) {
        try {
            val geo: GeoData = LocationDataProvider.getInstance().currentGeo()
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri
                    .parse("https://maps.google.com/maps?f=d&saddr="
                            + geo.getCoords().getLatitude() + "," + geo.getCoords().getLongitude() + "&daddr="
                            + coords.getLatitude() + "," + coords.getLongitude())))

        } catch (final Exception e) {
            Log.i("GoogleMapsDirectionApp: application not available.", e)
        }

    }

}
