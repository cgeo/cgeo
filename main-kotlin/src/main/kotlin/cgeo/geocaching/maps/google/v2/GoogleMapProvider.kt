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

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.maps.AbstractMapProvider
import cgeo.geocaching.maps.AbstractMapSource
import cgeo.geocaching.maps.interfaces.MapItemFactory
import cgeo.geocaching.maps.interfaces.MapProvider
import cgeo.geocaching.maps.interfaces.MapSource

import android.content.res.Resources

import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.maps.GoogleMap

class GoogleMapProvider : AbstractMapProvider() {

    public static val GOOGLE_MAP_ID: String = "GOOGLE_MAP"

    private final MapItemFactory mapItemFactory

    private GoogleMapProvider() {
        val resources: Resources = CgeoApplication.getInstance().getResources()

        registerMapSource(GoogleMapSource(this, resources.getString(R.string.map_source_google_map)))
        registerMapSource(GoogleSatelliteSource(this, resources.getString(R.string.map_source_google_satellite)))
        registerMapSource(GoogleTerrainSource(this, resources.getString(R.string.map_source_google_terrain)))

        mapItemFactory = GoogleMapItemFactory()
    }

    private static class Holder {
        private static val INSTANCE: GoogleMapProvider = GoogleMapProvider()
    }

    public static GoogleMapProvider getInstance() {
        return Holder.INSTANCE
    }

    override     public Class<? : AppCompatActivity()> getMapClass() {
        return GoogleMapActivity.class
    }

    override     public Int getMapViewId() {
        return R.id.map
    }

    override     public MapItemFactory getMapItemFactory() {
        return mapItemFactory
    }

    override     public Boolean isSameActivity(final MapSource source1, final MapSource source2) {
        return true
    }

    public abstract static class AbstractGoogleMapSource : AbstractMapSource() {
        public final Boolean indoorEnabled
        public final Boolean supportsTheming
        public final Int mapType

        protected AbstractGoogleMapSource(final MapProvider mapProvider, final String name, final Int mapType, final Boolean supportsTheming, final Boolean indoorEnabled) {
            super(mapProvider, name)
            this.mapType = mapType
            this.supportsTheming = supportsTheming
            this.indoorEnabled = indoorEnabled
        }

    }

    private static class GoogleMapSource : AbstractGoogleMapSource() {

        GoogleMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, GoogleMap.MAP_TYPE_NORMAL, true, true)
        }

    }

    private static class GoogleSatelliteSource : AbstractGoogleMapSource() {

        GoogleSatelliteSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, GoogleMap.MAP_TYPE_HYBRID, false, false)
        }

    }

    private static class GoogleTerrainSource : AbstractGoogleMapSource() {

        GoogleTerrainSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, GoogleMap.MAP_TYPE_TERRAIN, false, false)
        }

    }

}
