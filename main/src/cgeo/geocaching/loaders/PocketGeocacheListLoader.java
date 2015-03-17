package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.settings.Settings;

import android.content.Context;

public class PocketGeocacheListLoader extends AbstractSearchLoader {
    private final String guid;

    public PocketGeocacheListLoader(final Context context, final String guid) {
        super(context);
        this.guid = guid;
    }

    @Override
    public SearchResult runSearch() {

        if (Settings.isGCConnectorActive()) {
            return GCParser.searchByPocketQuery(guid, Settings.getCacheType(), Settings.isShowCaptcha(), this);
        }

        return new SearchResult();

    }

}
