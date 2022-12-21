package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;

import android.app.Activity;

public class NullGeocacheListLoader extends AbstractSearchLoader {
    private final SearchResult search;

    public NullGeocacheListLoader(final Activity activity, final SearchResult search) {
        super(activity);
        this.search = search;
    }

    @Override
    public SearchResult runSearch() {
        return search;
    }

}
