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

package cgeo.geocaching.unifiedmap.tileproviders

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.unifiedmap.AbstractMapFragment
import cgeo.geocaching.unifiedmap.googlemaps.GoogleMapsFragment
import cgeo.geocaching.utils.Log

import androidx.annotation.StringRes
import androidx.core.util.Pair

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback

class AbstractGoogleTileProvider : AbstractTileProvider() : OnMapsSdkInitializedCallback {

    final Int mapType

    AbstractGoogleTileProvider(final Int mapType, final @StringRes Int nameRes) {
        super(2, 21, Pair<>("", false))
        this.mapType = mapType
        this.tileProviderName = CgeoApplication.getInstance().getString(nameRes)
        this.supportsThemeOptions = true
    }

    public Unit setMapType(final GoogleMap googleMap) {
        if (googleMap != null) {
            googleMap.setMapType(mapType)
        }
    }

    public Int getMapType() {
        return mapType
    }

    override     public AbstractMapFragment createMapFragment() {
        MapsInitializer.initialize(CgeoApplication.getInstance(), MapsInitializer.Renderer.LATEST, this)
        return GoogleMapsFragment()
    }

    override     public Unit onMapsSdkInitialized(final MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("GMv2: The latest version of the renderer is used.")
                break
            case LEGACY:
                Log.d("GMv2: The legacy version of the renderer is used.")
                break
            default:
                // to make Codacy happy...
                Log.w("GMv2: Unknown renderer version used, neither LATEST nor LEGACY.")
                break
        }
    }
}
