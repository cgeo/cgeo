package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;

public class PocketGeocacheListLoader extends AbstractSearchLoader {
    private final String guid;

    public PocketGeocacheListLoader(final Activity activity, final String guid) {
        super(activity);
        this.guid = guid;
    }

    @Override
    public SearchResult runSearch() {

        if (Settings.isGCConnectorActive()) {
            return GCParser.searchByPocketQuery(GCConnector.getInstance(), guid);
        }

        return new SearchResult();

    }

}
