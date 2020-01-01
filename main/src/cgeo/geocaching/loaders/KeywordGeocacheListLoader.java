package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;

import android.app.Activity;

import androidx.annotation.NonNull;

public class KeywordGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final String keyword;

    public KeywordGeocacheListLoader(final Activity activity, @NonNull final String keyword) {
        super(activity);
        this.keyword = keyword;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByKeywordConnectors(),
                connector -> connector.searchByKeyword(keyword));
    }

}
