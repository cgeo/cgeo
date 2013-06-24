package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.OldSettings;
import cgeo.geocaching.connector.gc.GCParser;

import android.content.Context;

public class NextPageGeocacheListLoader extends AbstractSearchLoader {
    private final SearchResult search;

    public NextPageGeocacheListLoader(Context context, SearchResult search) {
        super(context);
        this.search = search;
    }

    @Override
    public SearchResult runSearch() {
        return GCParser.searchByNextPage(search, OldSettings.isShowCaptcha(), this);
    }

}
