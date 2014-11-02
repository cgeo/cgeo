package cgeo.geocaching.loaders;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;

import android.content.Context;

public class HistoryGeocacheListLoader extends AbstractSearchLoader {
    private final Geopoint coords;

    public HistoryGeocacheListLoader(Context context, Geopoint coords) {
        super(context);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        return DataStore.getHistoryOfCaches(true, coords != null ? Settings.getCacheType() : CacheType.ALL);
    }

}
