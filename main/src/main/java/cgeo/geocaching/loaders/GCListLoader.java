package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;

import java.util.List;

public class GCListLoader extends AbstractSearchLoader {
    private final List<GCList> gcLists;

    public GCListLoader(final Activity activity, final List<GCList> gcLists) {
        super(activity);
        this.gcLists = gcLists;
    }

    @Override
    public SearchResult runSearch() {
        if (Settings.isGCConnectorActive()) {
            final SearchResult combinedResult = new SearchResult();
            for (final GCList gcList : gcLists) {
                if (gcList.isBookmarkList()) {
                    final SearchResult bmResult = GCParser.searchByBookmarkList(GCConnector.getInstance(), gcList.getGuid(), 0);
                    combinedResult.addSearchResult(bmResult);
                } else {
                    final SearchResult pqResult = GCParser.searchByPocketQuery(GCConnector.getInstance(), gcList.getShortGuid(), gcList.getPqHash());
                    combinedResult.addSearchResult(pqResult);
                }
            }
            return combinedResult;
        }

        return new SearchResult();
    }
}
