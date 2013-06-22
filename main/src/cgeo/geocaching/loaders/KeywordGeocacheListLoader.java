package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.connector.gc.GCParser;

import android.content.Context;

public class KeywordGeocacheListLoader extends AbstractSearchLoader {

    private final String keyword;

    public KeywordGeocacheListLoader(Context context, String keyword) {
        super(context);
        this.keyword = keyword;
    }

    @Override
    public SearchResult runSearch() {
        return GCParser.searchByKeyword(keyword, Settings.getCacheType(), Settings.isShowCaptcha(), this);
    }

}
