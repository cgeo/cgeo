package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;

import java.util.Collections;

/**
 * Offline list loader for {@link PseudoList#OWN_UNPUBLISHED_LIST} which, unlike a normal offline list, first
 * refreshes its membership from geocaching.com before showing the (now up to date) stored caches.
 */
public class OwnUnpublishedGeocacheListLoader extends OfflineGeocacheListLoader {

    public OwnUnpublishedGeocacheListLoader(final Activity activity, final Geopoint searchCenter, final GeocacheFilter filter, final CacheComparator sort, final boolean sortInverse, final int limit) {
        super(activity, searchCenter, PseudoList.OWN_UNPUBLISHED_LIST.id, filter, sort, sortInverse, limit);
    }

    @Override
    public SearchResult runSearch() {
        final SearchResult online = GCParser.searchOwnUnpublishedGeocaches();
        DataStore.syncOwnUnpublishedCachesList(online == null ? Collections.emptySet() : online.getCachesFromSearchResult());
        return super.runSearch();
    }
}
