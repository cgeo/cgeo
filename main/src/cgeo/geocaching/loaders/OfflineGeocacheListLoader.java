package cgeo.geocaching.loaders;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Intents;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.os.Bundle;

public class OfflineGeocacheListLoader extends AbstractSearchLoader {

    private final int listId;
    private final Geopoint searchCenter;

    public OfflineGeocacheListLoader(final Context context, final Geopoint searchCenter, final int listId) {
        super(context);
        this.searchCenter = searchCenter;
        this.listId = listId;
    }

    @Override
    public SearchResult runSearch() {
        return DataStore.getBatchOfStoredCaches(searchCenter, Settings.getCacheType(), listId);
    }

    /**
     * @param listId
     * @return the bundle needed for querying the LoaderManager for the offline list with the given id
     */
    public static Bundle getBundleForList(final int listId) {
        final Bundle bundle = new Bundle();
        bundle.putInt(Intents.EXTRA_LIST_ID, listId);
        return bundle;
    }

}
