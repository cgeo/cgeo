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
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter
import cgeo.geocaching.sorting.GeocacheSort

import android.app.Activity

import androidx.annotation.NonNull

class FinderGeocacheListLoader : LiveFilterGeocacheListLoader() {

    public final String username

    public FinderGeocacheListLoader(final Activity activity, final GeocacheSort sort, final String username) {
        super(activity, sort)
        this.username = username
    }

    override     public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.LOG_ENTRY
    }

    override     public IGeocacheFilter getAdditionalFilterParameter() {
        val foundByFilter: LogEntryGeocacheFilter = GeocacheFilterType.LOG_ENTRY.create()
        foundByFilter.setFoundByUser(username)
        return foundByFilter
    }
}
