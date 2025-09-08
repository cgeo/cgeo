package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;

import java.util.List;

public class PocketGeocacheListLoader extends AbstractSearchLoader {
    private final List<GCList> pocketQueries;

    public PocketGeocacheListLoader(final Activity activity, final List<GCList> pocketQueries) {
        super(activity);
        this.pocketQueries = pocketQueries;
    }

    @Override
    public SearchResult runSearch() {

        if (Settings.isGCConnectorActive()) {
            final SearchResult combinedResult = new SearchResult();
            for (final GCList pocketQuery : pocketQueries) {
                if (pocketQuery.isBookmarkList()) {
                    // final SearchResult bmResult = GCParser.searchByBookmarkList(GCConnector.getInstance(), pocketQuery.getGuid());
                    // combinedResult.addSearchResult(bmResult);
                } else {
                    final SearchResult pqResult = GCParser.searchByPocketQuery(GCConnector.getInstance(), pocketQuery.getShortGuid(), pocketQuery.getPqHash());
                    combinedResult.addSearchResult(pqResult);
                }
            }
            return combinedResult;
        }

        return new SearchResult();
    }
}
