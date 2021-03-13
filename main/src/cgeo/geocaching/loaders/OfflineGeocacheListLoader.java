package cgeo.geocaching.loaders;

import cgeo.geocaching.Intents;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;
import android.os.Bundle;

public class OfflineGeocacheListLoader extends AbstractSearchLoader {

    private final int listId;
    private final Geopoint searchCenter;
    private final IGeocacheFilter filter;

    public OfflineGeocacheListLoader(final Activity activity, final Geopoint searchCenter, final int listId, final IGeocacheFilter filter) {
        super(activity);
        this.searchCenter = searchCenter;
        this.listId = listId;
        this.filter = filter;
    }

    @Override
    public SearchResult runSearch() {
        return DataStore.getBatchOfStoredCaches(searchCenter, Settings.getCacheType(), listId, filter);
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
