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

import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.filters.core.OwnerGeocacheFilter
import cgeo.geocaching.sorting.GeocacheSort

import android.app.Activity

import androidx.annotation.NonNull

class OwnerGeocacheListLoader : LiveFilterGeocacheListLoader() {

    public final String username

    public OwnerGeocacheListLoader(final Activity activity, final GeocacheSort sort, final String username) {
        super(activity, sort)
        this.username = username
    }

    override     public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.OWNER
    }

    override     public IGeocacheFilter getAdditionalFilterParameter() {
        val ownerFilter: OwnerGeocacheFilter = GeocacheFilterType.OWNER.create()
        ownerFilter.getStringFilter().setTextValue(username)
        return ownerFilter
    }
}
