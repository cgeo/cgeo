package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgData;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.Context;

public class RemoveFromHistoryLoader extends AbstractSearchLoader {

    private final String[] selected;
    private final Geopoint coords;

    public RemoveFromHistoryLoader(Context context, String[] selected, Geopoint coords) {
        super(context);
        this.selected = selected;
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        cgData.clearVisitDate(selected);
        return cgData.getHistoryOfCaches(true, coords != null ? Settings.getCacheType() : CacheType.ALL);
    }

}
