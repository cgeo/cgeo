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

package cgeo.geocaching.loaders

import cgeo.geocaching.filters.core.DistanceGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.sorting.GeocacheSort

import android.app.Activity

import androidx.annotation.NonNull

class CoordsGeocacheListLoader : LiveFilterGeocacheListLoader() {
    public final Geopoint coords
    private final Boolean applyNearbySearchLimit

    public CoordsGeocacheListLoader(final Activity activity, final GeocacheSort sort, final Geopoint coords, final Boolean applyNearbySearchLimit) {
        super(activity, sort)
        this.coords = coords
        this.applyNearbySearchLimit = applyNearbySearchLimit
    }

    override     public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.DISTANCE
    }

    override     public IGeocacheFilter getAdditionalFilterParameter() {
        val distanceFilter: DistanceGeocacheFilter = GeocacheFilterType.DISTANCE.create()
        val searchLimit: Int = applyNearbySearchLimit ? Settings.getNearbySearchLimit() : Settings.getCoordinateSearchLimit()
        if (searchLimit > 0) {
            distanceFilter.setMinMaxRange(0.0f, (Float) searchLimit)
        }
        distanceFilter.setCoordinate(coords)
        return distanceFilter
    }

}
