package cgeo.geocaching.loaders;

import cgeo.geocaching.Intents;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;
import android.os.Bundle;

public class OfflineGeocacheListLoader extends AbstractSearchLoader {

    private final int listId;
    private final Geopoint searchCenter;
    private final GeocacheFilter filter;

    private final CacheComparator sort;
    private final boolean sortInverse;
    private final int limit;

    public OfflineGeocacheListLoader(final Activity activity, final Geopoint searchCenter, final int listId, final GeocacheFilter filter, final CacheComparator sort, final boolean sortInverse, final int limit) {
        super(activity);
        this.searchCenter = searchCenter;
        this.listId = listId;
        this.filter = filter;
        this.sort = sort;
        this.sortInverse = sortInverse;
        this.limit = limit;
    }

    @Override
    public SearchResult runSearch() {
        return DataStore.getBatchOfStoredCaches(searchCenter, listId, filter, sort, sortInverse, limit);
    }

    /**
     * @return the bundle needed for querying the LoaderManager for the offline list with the given id
     */
    public static Bundle getBundleForList(final int listId) {
        final Bundle bundle = new Bundle();
        bundle.putInt(Intents.EXTRA_LIST_ID, listId);
        return bundle;
    }

}
