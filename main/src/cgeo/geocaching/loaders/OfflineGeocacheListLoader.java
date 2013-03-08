package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgData;
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
        return cgData.getBatchOfStoredCaches(searchCenter, Settings.getCacheType(), listId);
    }

}
