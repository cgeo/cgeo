package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;

public class PocketGeocacheListLoader extends AbstractSearchLoader {
    private final String shortGuid;
    private final String pqHash;

    public PocketGeocacheListLoader(final Activity activity, final String shortGuid, final String pqHash) {
        super(activity);
        this.shortGuid = shortGuid;
        this.pqHash = pqHash;
    }

    @Override
    public SearchResult runSearch() {

        if (Settings.isGCConnectorActive()) {
            return GCParser.searchByPocketQuery(GCConnector.getInstance(), shortGuid, pqHash);
        }

        return new SearchResult();

    }

}
