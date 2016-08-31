package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;

public class HistoryGeocacheListLoader extends AbstractSearchLoader {
    private final Geopoint coords;

    public HistoryGeocacheListLoader(final Activity activity, final Geopoint coords) {
        super(activity);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        return DataStore.getHistoryOfCaches(coords != null ? Settings.getCacheType() : CacheType.ALL);
    }

}
