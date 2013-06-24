package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.OldSettings;
import cgeo.geocaching.cgData;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.Context;

public class HistoryGeocacheListLoader extends AbstractSearchLoader {
    private final Geopoint coords;

    public HistoryGeocacheListLoader(Context context, Geopoint coords) {
        super(context);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        return cgData.getHistoryOfCaches(true, coords != null ? OldSettings.getCacheType() : CacheType.ALL);
    }

}
