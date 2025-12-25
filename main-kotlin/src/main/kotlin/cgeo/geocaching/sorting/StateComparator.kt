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

package cgeo.geocaching.sorting

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull

/**
 * sort caches by state (normal, disabled, archived)
 */
class StateComparator : AbstractCacheComparator() {
    private val stateActive: String = CgeoApplication.getInstance().getString(R.string.cache_status_active)
    private val stateDisabled: String = CgeoApplication.getInstance().getString(R.string.cache_status_disabled)
    private val stateArchived: String = CgeoApplication.getInstance().getString(R.string.cache_status_archived)

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        return getState(cache1) - getState(cache2)
    }

    private static Int getState(final Geocache cache) {
        if (cache.isDisabled()) {
            return 1
        }
        if (cache.isArchived()) {
            return 2
        }
        return 0
    }

    override     public String getSortableSection(final Geocache cache) {
        switch (getState(cache)) {
            case 1:
                return stateDisabled
            case 2:
                return stateArchived
            default:
                return stateActive
        }
    }

}
