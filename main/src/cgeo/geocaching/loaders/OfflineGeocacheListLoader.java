package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.Context;

public class OfflineGeocacheListLoader extends AbstractSearchLoader {

    private int listId;
    private Geopoint searchCenter;

    public OfflineGeocacheListLoader(Context context, Geopoint searchCenter, int listId) {
        super(context);
        this.searchCenter = searchCenter;
        this.listId = listId;
    }

    @Override
    public SearchResult runSearch() {
        return DataStore.getBatchOfStoredCaches(searchCenter, Settings.getCacheType(), listId);
    }

    public void setListId(int listId) {
        this.listId = listId;
    }

    public void setSearchCenter(Geopoint searchCenter) {
        this.searchCenter = searchCenter;
    }

}
