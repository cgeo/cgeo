package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.BookmarkListActivity;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

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
                // Handle artificial ignore list entry specially
                if (BookmarkListActivity.isArtificialIgnoreList(gcList)) {
                    final SearchResult ignoreListResult = loadArtificialIgnoreList();
                    combinedResult.addSearchResult(ignoreListResult);
                } else if (gcList.isBookmarkList()) {
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

    /**
     * Load caches from the artificial ignore list entry.
     * Fetches from the online ignore list using the web interface.
     */
    private SearchResult loadArtificialIgnoreList() {
        final SearchResult result = new SearchResult();
        try {
            final List<Geocache> ignoreListCaches = GCParser.fetchCachesFromPlanList("ignored");
            if (ignoreListCaches != null) {
                result.addCaches(ignoreListCaches);
            }
        } catch (final Exception e) {
            Log.e("GCListLoader.loadArtificialIgnoreList: Error loading ignore list", e);
        }
        return result;
    }
}
