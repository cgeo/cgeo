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

package cgeo.geocaching.connector

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.bettercacher.BetterCacherConnector
import cgeo.geocaching.connector.capability.ICacheAmendment
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.location.Viewport

import androidx.annotation.Nullable

class AmendmentUtils {

    private static final ICacheAmendment[] AMENDERS = ICacheAmendment[] {
            BetterCacherConnector.INSTANCE
    }

    private AmendmentUtils() {
        //no instance
    }

    public static Unit amendCaches(final SearchResult searchResult) {
        amendCachesInternal(searchResult, null, null)
    }

    public static Unit amendCachesForFilter(final SearchResult searchResult, final GeocacheFilter filter) {
        amendCachesInternal(searchResult, null, filter)
    }

    public static Unit amendCachesForViewport(final SearchResult searchResult, final Viewport viewport, final GeocacheFilter filter) {
        if (searchResult == null) {
            return
        }
        amendCachesInternal(searchResult, viewport, filter)
    }

    private static Unit amendCachesInternal(final SearchResult searchResult, final Viewport viewport, final GeocacheFilter filter) {
        if (searchResult == null || searchResult.isEmpty()) {
            return
        }

        for (ICacheAmendment amender : AMENDERS) {
            if (!amender.isActive() || (filter != null && !amender.relevantForFilter(filter))) {
                continue
            }
            if (viewport == null) {
                amender.amendCaches(searchResult)
            } else {
                amender.amendCachesForViewport(searchResult, viewport)
            }
        }
    }
}
