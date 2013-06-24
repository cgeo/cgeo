package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.OldSettings;
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
        return GCParser.searchByKeyword(keyword, OldSettings.getCacheType(), OldSettings.isShowCaptcha(), this);
    }

}
