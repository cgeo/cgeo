package cgeo.geocaching.connector;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.bettercacher.BetterCacherConnector;
import cgeo.geocaching.connector.capability.ICacheAmendment;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Viewport;

import androidx.annotation.Nullable;

public class
AmendmentUtils {

    private static final ICacheAmendment[] AMENDERS = new ICacheAmendment[] {
            BetterCacherConnector.INSTANCE
    };

    private AmendmentUtils() {
        //no instance
    }

    public static void amendCaches(final SearchResult searchResult) {
        amendCachesInternal(searchResult, null, null);
    }

    public static void amendCachesForFilter(final SearchResult searchResult, @Nullable final GeocacheFilter filter) {
        amendCachesInternal(searchResult, null, filter);
    }

    public static void amendCachesForViewport(final SearchResult searchResult, final Viewport viewport, final GeocacheFilter filter) {
        if (searchResult == null) {
            return;
        }
        amendCachesInternal(searchResult, viewport, filter);
    }

    private static void amendCachesInternal(final SearchResult searchResult, final Viewport viewport, final GeocacheFilter filter) {
        if (searchResult == null || searchResult.isEmpty()) {
            return;
        }

        for (ICacheAmendment amender : AMENDERS) {
            if (!amender.isActive() || (filter != null && !amender.relevantForFilter(filter))) {
                continue;
            }
            if (viewport == null) {
                amender.amendCaches(searchResult);
            } else {
                amender.amendCachesForViewport(searchResult, viewport);
            }
        }
    }
}
