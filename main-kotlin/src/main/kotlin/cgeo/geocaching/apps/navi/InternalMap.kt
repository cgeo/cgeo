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
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.DefaultMap
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings

import android.content.Context

import androidx.annotation.NonNull

class InternalMap : AbstractPointNavigationApp() {

    private final Class<?> cls

    InternalMap(final Class<?> cls, final Int name) {
        super(getString(name), null)
        this.cls = cls
    }

    InternalMap() {
        super(getString(R.string.cache_menu_map), null)
        cls = null
    }

    override     public Boolean isInstalled() {
        return true
    }

    override     public Unit navigate(final Context context, final Geopoint coords) {
        DefaultMap.startActivityCoords(context, cls != null ? cls : Settings.getMapProvider().getMapClass(), coords, WaypointType.WAYPOINT)
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        DefaultMap.startActivityCoords(context, cls != null ? cls : Settings.getMapProvider().getMapClass(), waypoint)
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        DefaultMap.startActivityGeoCode(context, cls != null ? cls : Settings.getMapProvider().getMapClass(), cache.getGeocode())
    }

}
